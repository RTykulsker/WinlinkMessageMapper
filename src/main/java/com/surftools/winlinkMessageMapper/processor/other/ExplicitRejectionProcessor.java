/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.processor.other;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;
import com.surftools.winlinkMessageMapper.dto.message.RejectionMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;
import com.surftools.winlinkMessageMapper.dto.other.RejectTypeContextPair;

public class ExplicitRejectionProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ExplicitRejectionProcessor.class);

  protected Map<String, RejectTypeContextPair> rejectMap = null;

  public ExplicitRejectionProcessor(Path path) {
    rejectMap = makeExplicitRejectMap(path);
  }

  /**
   * read explicit rejection messageIds
   *
   * @param path
   * @return
   */
  protected Map<String, RejectTypeContextPair> makeExplicitRejectMap(Path path) {
    Map<String, RejectTypeContextPair> map = new HashMap<>();
    Path rejectsPath = Path.of(path.toString(), "rejects.txt");
    List<String> lines = new ArrayList<>();
    try {
      lines = Files.readAllLines(rejectsPath);
      for (String line : lines) {
        String[] fields = line.split("\t");
        String messageId = null;
        int id = -1;
        RejectType reason = RejectType.EXPLICIT_LOCATION;
        String context = "";
        int nFields = fields.length;

        if (nFields >= 1) {
          messageId = fields[0];
        }

        if (nFields >= 2) {
          try {
            id = Integer.parseInt(fields[1]);
            reason = RejectType.fromId(id);
            if (reason == null) {
              reason = RejectType.EXPLICIT_OTHER;
            }
          } catch (Exception e) {
            logger.warn("couldn't find ReasonType: " + line);
            reason = RejectType.EXPLICIT_OTHER;
          }
        }

        if (nFields >= 3) {
          context = fields[2];
        }
        map.put(messageId, new RejectTypeContextPair(reason, context));
      } // end for over lines
    } catch (Exception e) {
      logger.info("couldn't read rejection file: " + rejectsPath + ", " + e.getLocalizedMessage());
    }
    logger.info("read: " + map.size() + " rejects from file: " + rejectsPath);

    return map;
  }

  /**
   * explicitly reject messages
   *
   * @param path
   * @param messageMap
   */
  public void processExplicitRejections(Map<MessageType, List<ExportedMessage>> messageMap) {
    var explicitRejections = new ArrayList<RejectionMessage>();

    for (MessageType messageType : messageMap.keySet()) {
      var inputMessages = messageMap.get(messageType);
      if (inputMessages == null) {
        continue;
      }
      var outputMessages = new ArrayList<ExportedMessage>(inputMessages.size());

      int rejectCount = 0;
      for (ExportedMessage message : inputMessages) {
        var pair = rejectMap.get(message.messageId);
        if (pair == null) {
          outputMessages.add(message);
        } else {
          ++rejectCount;
          var rejectType = pair.reason();
          var context = pair.context();
          if (rejectType == RejectType.EXPLICIT_LOCATION) {
            GisMessage gisMessage = (GisMessage) message;
            context = "{latitude: " + gisMessage.latitude + ", longitude: " + gisMessage.longitude + "}";
          }
          RejectionMessage rejection = new RejectionMessage(message, rejectType, context);
          explicitRejections.add(rejection);
        } // end if rejecting
      } // end loop over messages for type
      logger
          .info("messageType: " + messageType + ", in: " + inputMessages.size() + " messages, out: "
              + outputMessages.size() + ", rejected: " + rejectCount);
      messageMap.put(messageType, outputMessages);
    } // end loop over type

    // merge the explicit rejections with prior rejections
    var implicitRejections = messageMap.getOrDefault(MessageType.REJECTS, new ArrayList<>());
    var implicitRejectionCount = implicitRejections.size();
    implicitRejections.addAll(explicitRejections);
    messageMap.put(MessageType.REJECTS, implicitRejections);

    logger
        .info("implicit rejections: " + implicitRejectionCount + ", explicitRejections: " + explicitRejections.size());
    return;
  }

}
