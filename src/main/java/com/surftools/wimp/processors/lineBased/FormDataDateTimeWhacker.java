/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.wimp.processors.lineBased;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.HospitalBedMessage;
import com.surftools.wimp.message.WxLocalMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * slam a DateTime value into FormData; necessary because of a bug in (at least) Hospital Bed where msgDateTime, not
 * formDateTime is showing up in FormData.txt, which makes it hard to effectively map/filter on Date/Time in Winlink
 * Express
 */
public class FormDataDateTimeWhacker extends AbstractBaseProcessor {
  private Set<MessageType> expectededMessageTypes = new LinkedHashSet<MessageType>();

  private List<String> rewrittenMessageStrings = new ArrayList<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm);
    var expectedMessageTypesStrings = cm.getAsString(Key.EXPECTED_MESSAGE_TYPES).split(",");
    for (var typeString : expectedMessageTypesStrings) {
      var messageType = MessageType.fromString(typeString);
      if (messageType == null) {
        logger
            .error("unknown messageType: " + messageType + " from configuration: "
                + Key.EXPECTED_MESSAGE_TYPES.toString() + ": " + String.join(",", expectedMessageTypesStrings));
      } else {
        expectededMessageTypes.add(messageType);
      }
    }
    logger.info("Expected messageTypes: " + String.join(",", expectedMessageTypesStrings));
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) {
      var sender = senderIterator.next();
      var map = mm.getMessagesForSender(sender);
      for (var messageType : map.keySet()) {
        if (!expectededMessageTypes.contains(messageType)) {
          logger.warn("### UNSUPPORTED messageType: " + messageType.toString() + " from: " + sender);
          continue;
        }
        var messages = map.get(messageType);
        final var fsrDtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'");
        for (var m : messages) {
          var trueFormDateTimeString = (String) null;
          switch (messageType) {
          case HOSPITAL_BED:
            trueFormDateTimeString = makeDateTime(((HospitalBedMessage) m).formDateTime);
            break;
          case FIELD_SITUATION:
            trueFormDateTimeString = makeDateTime(fsrDtFormatter, ((FieldSituationMessage) m).formDateTime);
            break;
          case WX_LOCAL:
            trueFormDateTimeString = makeDateTime(((WxLocalMessage) m).formDateTime);
            break;

          default:
            continue;
          } // end switch over messages
          var newMessageString = stompTheStamp(m.lines, trueFormDateTimeString);
          rewrittenMessageStrings.add(newMessageString);
        } // end for over messages of a given type
      } // end for over messageTypes
    } // end for over senders
  }

  /**
   * want a String like: DateTime:T=20250225191109
   *
   * @param formDateTime
   * @return
   */
  private String makeDateTime(LocalDateTime formDateTime) {
    final var dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    final var prefix = "DateTime:T=";
    return prefix + dtf.format(formDateTime);
  }

  private String makeDateTime(DateTimeFormatter formatter, String formDateTimeString) {
    var formDateTime = LocalDateTime.parse(formDateTimeString, formatter);
    return makeDateTime(formDateTime);
  }

  private String stompTheStamp(List<String> oldLines, String newDateTimeString) {
    var newLines = new ArrayList<String>();
    var base64Blob = new StringBuilder();
    var inFormData = false;
    for (var line : oldLines) {
      if (!inFormData) {
        newLines.add(line);
      }

      if (line.equals("Content-Type: text/plain; name=\"FormData.txt\"")) {
        inFormData = true;
        continue;
      }

      if (inFormData) {
        if (line.strip().length() == 0 || line.equals("Content-Transfer-Encoding: base64")) {
          newLines.add(line);
          continue;
        }

        if (line.startsWith("--boundary")) { // terminator
          inFormData = false;
          var newBase64Lines = slam(base64Blob.toString(), newDateTimeString);
          newLines.addAll(newBase64Lines);
          newLines.add(line);
          continue;
        }

        base64Blob.append(line + "\n");
      }
    }

    return String.join("\n", newLines);
  }

  private List<String> slam(String base64Blob, String dateTime) {
    var oldContent = new String(Base64.getMimeDecoder().decode(base64Blob), StandardCharsets.UTF_8).split("\n");
    var newContent = new StringBuilder();
    for (var line : oldContent) {
      if (line.startsWith("DateTime")) {
        newContent.append(dateTime + "\r\n");
        logger.info("replacing " + line + ", with " + dateTime);
      } else {
        newContent.append(line + "\n");
      }
    }

    var encoded = Base64.getMimeEncoder().encodeToString(newContent.toString().getBytes());
    var list = Arrays.asList(encoded.split("\n"));
    return list;
  }

  @Override
  public void postProcess() {
    final String messagesTemplate = """
        <?xml version="1.0"?>
        <Winlink_Express_message_export>
          <export_parameters>
          </export_parameters>
          <message_list>
            #MESSAGES#
          </message_list>
        </Winlink_Express_message_export>
              """;

    FileUtils.makeDirIfNeeded(Path.of(outputPathName));
    var content = messagesTemplate.replace("#MESSAGES", String.join("\n", rewrittenMessageStrings));
    var fileOutputPath = Path.of(outputPath.toString(), "formDataRewrittenDateTimes.xml");
    try {
      Files.writeString(fileOutputPath, content);
    } catch (IOException e) {
      logger.error("Could not write " + fileOutputPath.toString() + ", " + e.getLocalizedMessage());
    }
    logger.info("wrote " + rewrittenMessageStrings.size() + " messages to file: " + fileOutputPath.toString());

  }

}
