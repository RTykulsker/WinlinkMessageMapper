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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * remove extra messages
 *
 * @author bobt
 *
 */
public class DeduplicationProcessor extends AbstractBaseProcessor {
  private final Logger logger = LoggerFactory.getLogger(DeduplicationProcessor.class);

  // rule format is number of items, most recent first, negative from beginning, 0 for all
  private Map<MessageType, Integer> typeRuleMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    typeRuleMap = new HashMap<>();

    // override default rule of only last
    typeRuleMap.put(MessageType.PLAIN, 0);
    typeRuleMap.put(MessageType.REJECTS, 0);
    typeRuleMap.put(MessageType.EYEWARN_DETAIL, 0);

    var typeRuleString = cm.getAsString(Key.DEDUPLICATION_RULES);
    if (typeRuleString != null) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        var jsonMap = mapper.readValue(typeRuleString, Map.class);
        for (var messageTypeName : jsonMap.keySet()) {
          var messageType = MessageType.fromString((String) messageTypeName);
          if (messageType == null) {
            throw new RuntimeException("unsupported message type: " + messageTypeName + " in rules: " + typeRuleString);
          }

          Object value = jsonMap.get(messageTypeName);
          try {
            var intValue = (Integer) jsonMap.get(messageTypeName);
            typeRuleMap.put(messageType, intValue);
            logger.info("type: " + messageType + ", rule: " + intValue);
          } catch (Exception e) {
            throw new RuntimeException(
                "Couldn't parse value from " + value + " for " + messageTypeName + " in rules: " + typeRuleString);
          }

        }
      } catch (Exception e) {
        throw new RuntimeException("Exception parsing " + typeRuleString + ", " + e.getLocalizedMessage());
      }
      logger.info("typeRuleMap has " + typeRuleMap.size() + " explicit rules");
    }
  }

  @Override
  public void process() {
    var it = mm.getSenderIterator();
    var dedupeCount = 0;
    while (it.hasNext()) {
      var sender = it.next();

      if (dumpIds != null && dumpIds.contains(sender)) {
        logger.debug("sender: " + sender);
      }

      var map = mm.getMessagesForSender(sender);
      for (var messageType : map.keySet()) {

        // by default, we only want last message;
        var rule = typeRuleMap.getOrDefault(messageType, Integer.valueOf(1));
        var inputList = map.get(messageType);

        // do we have any work to do?
        if (rule == 0 || inputList.size() == 1) {
          continue;
        }

        // ExportedMessage implements Comparable, by sortTime

        Collections.sort(inputList);

        if (rule > 0) {
          Collections.reverse(inputList);
        }
        var ruleLimit = Math.abs(rule);

        // implicit de-duplicate by messageId
        var messageIdSet = new HashSet<String>();
        var outputList = new ArrayList<ExportedMessage>();

        for (var message : inputList) {
          if (messageIdSet.contains(message.messageId)) {
            logger
                .info("Duplicate message from sender: " + message.from + ", mId: " + message.messageId + ", type: "
                    + message.getMessageType().toString());
            continue;
          }

          messageIdSet.add(message.messageId);
          outputList.add(message);

          if (outputList.size() == ruleLimit) {
            break;
          }
        } // end loop over messages for type and sender
        Collections.sort(outputList);
        if (inputList.size() != outputList.size()) {
          logger
              .debug("sender: " + sender + ", type: " + messageType + ", deduped from: " + inputList.size() + " to "
                  + outputList.size());
          dedupeCount += (inputList.size() - outputList.size());
        }
        map.put(messageType, outputList);
      } // end loop over type for sender
      mm.putMessagesForSender(sender, map);
    } // end loop over sender
    logger.info("removed: " + dedupeCount + " duplicate messages");
    mm.putContextObject("dedupeCount", dedupeCount);
  } // end process()

}
