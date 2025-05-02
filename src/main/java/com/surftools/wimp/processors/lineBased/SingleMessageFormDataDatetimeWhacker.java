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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
public class SingleMessageFormDataDatetimeWhacker extends AbstractBaseProcessor {
  private Path rewrittenMessageFilePath; // where we write files to
  private Set<MessageType> expectededMessageTypes = new LinkedHashSet<MessageType>();

  private record SenderKey(String sender, String messageId) {
  }

  private record SenderValue(MessageType messageType, String dateTime) {
  };

  private Map<SenderKey, SenderValue> messageMap = new LinkedHashMap<>();
  private Map<SenderKey, String> fileMap = new LinkedHashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm);
    var expectedMessageTypesStrings = cm.getAsString(Key.EXPECTED_MESSAGE_TYPES).split(",");
    for (var typeString : expectedMessageTypesStrings) {
      var messageType = MessageType.fromString(typeString);
      if (messageType == null) {
        logger
            .error("unknown messageType: " + messageType + " from configuration: "
                + Key.EXPECTED_MESSAGE_TYPES.toString() + ": " + expectedMessageTypesStrings);
      } else {
        expectededMessageTypes.add(messageType);
      }
    }
    logger.info("Expected messageTypes: " + expectedMessageTypesStrings);
    rewrittenMessageFilePath = Path.of(outputPathName, "rewrittenMessages");
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) {
      var sender = senderIterator.next();
      var map = mm.getMessagesForSender(sender);
      for (var messageType : map.keySet()) {
        if (!expectededMessageTypes.contains(messageType)) {
          logger.warn("### UNSUPPORTED messageType: " + messageType.toString());
          continue;
        }
        var messages = map.get(messageType);
        for (var m : messages) {
          var messageId = m.messageId;
          var senderKey = new SenderKey(sender, messageId);
          var value = (SenderValue) null;
          switch (messageType) {
          case HOSPITAL_BED:
            value = new SenderValue(messageType, makeDateTime(((HospitalBedMessage) m).formDateTime));
            messageMap.put(senderKey, value);
            break;
          case FIELD_SITUATION:
            final var fsrDtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'");
            value = new SenderValue(messageType,
                makeDateTime(fsrDtFormatter, ((FieldSituationMessage) m).formDateTime));
            break;
          case WX_LOCAL:
            value = new SenderValue(messageType, makeDateTime(((WxLocalMessage) m).formDateTime));
            break;

          default:
            continue;
          } // end switch over messages
          fileMap.put(senderKey, m.fileName);
          messageMap.put(senderKey, value);
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

  @Override
  public void postProcess() {
    var rewriteCount = 0;
    FileUtils.makeDirIfNeeded(rewrittenMessageFilePath);
    for (var key : fileMap.keySet()) {
      var fileName = fileMap.get(key);
      try {
        var fileLines = Files.readAllLines(Path.of(pathName, fileName));
        var messageCount = fileLines.stream().filter(s -> s.strip().equals("<message>")).count();
        if (messageCount != 1) {
          logger.error("multiple messages in file: " + fileName + ", skipping");
          System.exit(1);
        }

        var messageValue = messageMap.get(key);
        if (messageValue == null) {
          logger.error("did not find message value for key: " + key);
          System.exit(1);
        } else {
          var messageLines = Files.readAllLines(Path.of(pathName, fileName));
          var newMessage = stompTheStamp(messageLines, key, messageValue);
          var baseName = Path.of(fileName).getName(Path.of(fileName).getNameCount() - 1).toString();

          var fileOutputPath = Path.of(rewrittenMessageFilePath.toString(), baseName);
          Files.writeString(fileOutputPath, newMessage);
          logger.info("rewrote file to : " + fileOutputPath.toString());
          ++rewriteCount;
        }
      } catch (Exception e) {
        logger.error("Exception processing file: " + fileName + "; " + e.getMessage());
      }
    } // end for over keys in fileMap
    logger.info("re-wrote:" + rewriteCount + " files");
  }

  private String stompTheStamp(List<String> inputMessageLines, SenderKey key, SenderValue messageValue) {
    var sb = new StringBuilder();
    var base64Blob = new StringBuilder();
    var inFormData = false;
    for (var line : inputMessageLines) {
      if (!inFormData) {
        sb.append(line + "\n");
      }

      if (line.equals("Content-Type: text/plain; name=\"FormData.txt\"")) {
        inFormData = true;
        continue;
      }

      if (inFormData) {
        if (line.strip().length() == 0 || line.equals("Content-Transfer-Encoding: base64")) {
          sb.append(line + "\n");
          continue;
        }

        if (line.startsWith("--boundary")) { // terminator
          inFormData = false;
          var newBase64Lines = slam(base64Blob.toString(), messageValue.dateTime);
          sb.append(newBase64Lines + "\n");
          sb.append(line + "\n");
          continue;
        }

        base64Blob.append(line + "\n");
      }
    }

    return sb.toString();
  }

  private String slam(String base64Blob, String dateTime) {
    var oldContent = new String(Base64.getMimeDecoder().decode(base64Blob), StandardCharsets.UTF_8).split("\n");
    var newContent = new StringBuilder();
    for (var line : oldContent) {
      if (line.startsWith("DateTime")) {
        newContent.append(dateTime + "\r\n");
      } else {
        newContent.append(line + "\n");
      }
    }

    var encoded = Base64.getMimeEncoder().encodeToString(newContent.toString().getBytes());

    return encoded;
  }

}
