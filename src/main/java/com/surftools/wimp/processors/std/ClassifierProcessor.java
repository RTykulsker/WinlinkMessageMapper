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

package com.surftools.wimp.processors.std;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IDetailableMessage;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IParser;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.ExportedMessage.ExportedKey;
import com.surftools.wimp.parser.PdfIcs309Parser;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * classify message by type
 *
 * @author bobt
 *
 */
public class ClassifierProcessor extends AbstractBaseProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ClassifierProcessor.class);

  private Map<MessageType, IParser> parserMap = new HashMap<>();
  private Map<ExportedKey, Map<String, String>> formDataMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    final var ignoreTypes = List.of(MessageType.EXPORTED, MessageType.REJECTS, MessageType.EYEWARN_DETAIL);
    for (var type : MessageType.values()) {
      if (ignoreTypes.contains(type)) {
        continue;
      }
      var parserName = "com.surftools.wimp.parser." + type.makeParserName() + "Parser";
      try {
        var parserClass = Class.forName(parserName);
        var parser = (IParser) parserClass.getDeclaredConstructor().newInstance();
        parser.initialize(cm, mm);
        parserMap.put(type, parser);
      } catch (Exception e) {
        logger.error("Couldn't create parser for: " + type.toString() + ", " + e.getLocalizedMessage());
      }
    }
  }

  @Override
  public void process() {
    var messages = mm.getOriginalMessages();

    if (messages != null) {
      var tmpMessageMap = new HashMap<MessageType, List<ExportedMessage>>();

      for (var message : messages) {
        var messageType = findMessageType(message);
        var parser = parserMap.get(messageType);
        ExportedMessage parsedMessage = message;
        if (parser != null) {
          parsedMessage = parser.parse(message);
        }

        var parsedMessageType = parsedMessage.getMessageType();
        var list = tmpMessageMap.getOrDefault(parsedMessageType, new ArrayList<ExportedMessage>());
        list.add(parsedMessage);
        tmpMessageMap.put(parsedMessageType, list);

        if (parsedMessage instanceof IDetailableMessage) {
          var detailableMessage = (IDetailableMessage) parsedMessage;
          var detailType = detailableMessage.getDetailMessageType();
          var existingDetailList = tmpMessageMap.getOrDefault(detailType, new ArrayList<ExportedMessage>());
          var newDetailList = detailableMessage.getDetailMessages();
          existingDetailList.addAll(newDetailList);
          tmpMessageMap.put(detailType, existingDetailList);
        }

      } // end loop over messages

      mm.load(tmpMessageMap);
    }
  }

  /**
   * determine the messageType of the ExportedMessage
   *
   * this is the "heart and soul" of classification
   *
   * @param message
   * @return
   */
  public MessageType findMessageType(ExportedMessage message) {
    // First choice: for source-of-truth is the RMS viewer (aka XML blob) attachment
    var messageType = getMessageTypeFromRmsViewerData(message);
    if (messageType != null) {
      return messageType;
    }

    // Second choice: FormData.txt attachment
    messageType = getMessageTypeFromFormData(message, null);
    if (messageType != null) {
      return messageType;
    }

    // Third choice: message subject
    messageType = getMessageTypeFromSubject(message);
    if (messageType != null) {
      return messageType;
    }

    // Last choice is message content
    if (PdfIcs309Parser.isPdfIcs309(message)) {
      return MessageType.PDF_ICS_309;
    }

    // default
    return MessageType.PLAIN;
  }

  private MessageType getMessageTypeFromRmsViewerData(ExportedMessage message) {
    var attachments = message.attachments;
    if (attachments == null || attachments.size() == 0) {
      return null;
    }

    for (var attachmentName : attachments.keySet()) {
      if (attachmentName == null) {
        continue;
      }

      for (var messageType : MessageType.values()) {
        if (messageType.rmsViewerName() == null) {
          continue;
        }

        if (attachmentName.startsWith(messageType.rmsViewerName())) {
          return messageType;
        }

      } // end loop over messageTypes
    } // end loop over attachment names

    return null;
  }

  private MessageType getMessageTypeFromFormData(ExportedMessage m, MessageType subjectMessageType) {
    var attachments = m.attachments;
    if (attachments != null && attachments.size() > 0) {
      var attachmentNames = attachments.keySet();
      final var formDataKey = "FormData.txt";
      if (attachmentNames.contains(formDataKey)) {
        var valueMap = new HashMap<String, String>();
        var formDataString = new String(attachments.get(formDataKey));
        var lines = formDataString.split("\n");
        for (var line : lines) {
          if (line == null || line.strip().isEmpty() || line.startsWith("*")) {
            continue;
          }
          var fields = line.split("=");
          if (fields.length == 2) {
            var mapKey = fields[0].split(":")[0].strip();
            var mapValue = fields[1].strip();
            valueMap.put(mapKey, mapValue);
          } else if (fields.length == 1) {
            var mapKey = fields[0].split(":")[0].strip();
            var mapValue = "";
            valueMap.put(mapKey, mapValue);
          } else {
            logger
                .warn(String
                    .format("###  wrong number of fields in FormData: %s for sender: %s, mId: %s", //
                        line, m.from, m.messageId));
          }
        } // end loop over lines

        if (valueMap.size() > 0) {
          if (subjectMessageType == null) {
            var mapFileName = valueMap.get("MapFileName");
            if (mapFileName == null) {
              logger
                  .warn(String
                      .format("###  couldn't find MapFileName in FormData: for sender: %s, mId: %s", //
                          m.from, m.messageId));
              return null;
            }
            for (var messageType : MessageType.values()) {
              if (messageType.formDataName() != null && messageType.formDataName().equals(mapFileName)) {
                var messageKey = new ExportedKey(m.from, m.messageId);
                formDataMap.put(messageKey, valueMap);
                mm.putContextObject("formDataMap", formDataMap);
                return messageType;
              }
            }
          } else {
            // FormData won't be containing a map file name, we must rely on messageType set by subject
            var messageKey = new ExportedKey(m.from, m.messageId);
            formDataMap.put(messageKey, valueMap);
            mm.putContextObject("formDataMap", formDataMap);
            return subjectMessageType;
          } // end if subjectMessageType != null
        } // end if parsed map.size() > 0
      } // end if attachments contains FormData.txt
    } // end if attachments != null

    return null;
  }

  private MessageType getMessageTypeFromSubject(ExportedMessage message) {
    var subject = message.subject;
    for (var messageType : MessageType.values()) {
      if (messageType.testSubject(subject)) {
        return messageType;
      }
    }
    return null;
  }

}
