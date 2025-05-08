/**

The MIT License (MIT)

Copyright (c) 2022-2025, Robert Tykulsker

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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * "dups and sups" or remove extra messages
 *
 * duplicates: "identical" messages. Can arise from exporting message multiple times to different files. In theory,
 * every field must be identical. In practice, only sender/from and messageId
 *
 * superceded: "replaced" by better message. Typically we want only the last/most recent message of a given type for a
 * sender. But sometimes we want more than one, or we want only the first message(s).
 *
 * NOTE WELL: this processing is designed for documenting which messages get removed and why, processing efficiency is
 * not the goal here.
 *
 * @author bobt
 *
 */
public class DeduplicationProcessor extends AbstractBaseProcessor {
  private final Logger logger = LoggerFactory.getLogger(DeduplicationProcessor.class);
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("MM-dd HH:mm");

  // rule format is number of items, most recent first, negative from beginning, 0 for all
  private static Map<MessageType, Integer> typeRuleMap;

  record DupEntry(ExportedMessage m, List<ExportedMessage> dups) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable o) {
      var other = (DupEntry) o;
      return m.from.compareTo(other.m.from);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "MessageId", "Date/Time", "FileName", "Dups" };
    }

    @Override
    public String[] getValues() {
      var dupsString = String.join(";", dups.stream().map(d -> d.fileName).toList());
      return new String[] { m.from, m.messageId, DTF.format(m.sortDateTime), m.fileName, dupsString };
    }

  }

  record SupEntry(List<ExportedMessage> retainedList, List<ExportedMessage> supercededList) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable o) {
      var other = (SupEntry) o;
      return retainedList.get(0).from.compareTo(other.retainedList.get(0).from);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "MessageType", "Max Retain", "Direction", "Retained", "Superceded" };
    }

    @Override
    public String[] getValues() {
      var m = retainedList.get(0);
      var rule = typeRuleMap.getOrDefault(m.getMessageType(), Integer.valueOf(1));
      var direction = rule > 0 ? "Descending" : "Ascending";
      var ruleLimit = String.valueOf(Math.abs(rule));

      var retainedStringList = retainedList
          .stream()
            .map(s -> String.format("(%s,%s)", s.messageId, DTF.format(s.sortDateTime)))
            .toList();
      var retainedString = String.join(";", retainedStringList);

      var supercededStringList = supercededList
          .stream()
            .map(s -> String.format("(%s,%s)", s.messageId, DTF.format(s.sortDateTime)))
            .toList();
      var supercededString = String.join(";", supercededStringList);

      return new String[] { m.from, m.getMessageType().toString(), ruleLimit, direction, retainedString,
          supercededString };
    }

  }

  private List<DupEntry> dupEntries = new ArrayList<>(); // for generating CSV file
  private List<SupEntry> supEntries = new ArrayList<>(); // for generating CSV file

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

      var ruleString = typeRuleMap
          .keySet()
            .stream()
            .map(k -> String.format("(%s:%s)", k.toString(), typeRuleMap.get(k)))
            .toList();
      logger.info("typeRuleMap has " + typeRuleMap.size() + " explicit rules: " + String.join(";", ruleString));
    }
  }

  @Override
  public void process() {
    record DupKey(String sender, String messageId) {
    }
    var uniqueMap = new HashMap<DupKey, ExportedMessage>(); // first message for DupKey; keep this
    var dupListMap = new HashMap<DupKey, List<ExportedMessage>>(); // subsequent messages for DupKey; don't keep

    var it = mm.getSenderIterator();
    var dedupeCount = 0;
    while (it.hasNext()) {
      var sender = it.next();
      var map = mm.getMessagesForSender(sender);
      for (var messageType : map.keySet()) {

        var inputList = map.get(messageType);
        var tmpList = new ArrayList<ExportedMessage>(inputList.size()); // unique messages; how many to retain?

        var dupKey = (DupKey) null;
        for (var m : inputList) {
          dupKey = new DupKey(m.from, m.messageId);
          if (uniqueMap.containsKey(dupKey)) {
            var dupList = dupListMap.getOrDefault(dupKey, new ArrayList<ExportedMessage>());
            dupList.add(m);
            dupListMap.put(dupKey, dupList);
            if (logger.isInfoEnabled()) {
              logger
                  .debug("Duplicate message from sender: " + m.from + ", mId: " + m.messageId + ", type: "
                      + m.getMessageType().toString() + ", file: " + m.fileName + ", first file: "
                      + uniqueMap.get(dupKey).fileName);
            }
            continue; // do NOT continue to use this message!
          }
          uniqueMap.put(dupKey, m);
          tmpList.add(m);
        } // end loop over inputList

        // for aggregate reporting
        var dupList = dupListMap.get(dupKey);
        if (dupList != null) {
          var dupEntry = new DupEntry(uniqueMap.get(dupKey), dupList);
          dupEntries.add(dupEntry);
        }

        Collections.sort(tmpList); // ascending order based on sortDateTime
        var rule = typeRuleMap.getOrDefault(messageType, Integer.valueOf(1)); // by default, we only want last message;
        if (rule > 0) {
          Collections.reverse(tmpList);
        }
        var ruleLimit = Math.abs(rule);

        var outputList = new ArrayList<ExportedMessage>(tmpList.size());
        // all messages for messageType or just/not enough messages? retain 'em all
        if (rule == 0 || tmpList.size() <= ruleLimit) {
          outputList.addAll(tmpList);
        } else {
          var retainedList = new ArrayList<ExportedMessage>(); // will use those
          var supercededList = new ArrayList<ExportedMessage>(); // won't use these
          for (var m : tmpList) {
            if (outputList.size() < ruleLimit) {
              retainedList.add(m);
              outputList.add(m);
            } else {
              supercededList.add(m);
              if (logger.isInfoEnabled()) {
                var direction = rule > 0 ? "Descending" : "Ascending";
                var retainedStringList = retainedList
                    .stream()
                      .map(s -> String.format("(%s,%s)", s.messageId, DTF.format(s.sortDateTime)))
                      .toList();
                var retainedString = String.join(";", retainedStringList);

                var supercededStringList = supercededList
                    .stream()
                      .map(s -> String.format("(%s,%s)", s.messageId, DTF.format(s.sortDateTime)))
                      .toList();
                var supercededString = String.join(";", supercededStringList);
                logger
                    .debug("Superceded message from sender: " + m.from + " type: " + m.getMessageType().toString()
                        + ", maxRetain:" + ruleLimit + ", direction:" + direction + ", retained: " + retainedString
                        + ", superceded" + supercededString);
              } // end block for logging
            } // else this message is superceded
            // outputList.add(m);
          } // end loop over messages in tmpList
          if (supercededList.size() > 0) {
            var supEntry = new SupEntry(retainedList, supercededList);
            supEntries.add(supEntry);
          }
        } // end loop over messages for type and sender

        if (inputList.size() != outputList.size()) {
          logger
              .info("sender: " + sender + ", type: " + messageType + ", deduped/superceded from: " + inputList.size()
                  + " to " + outputList.size());
          dedupeCount += (inputList.size() - outputList.size());
        }
        map.put(messageType, outputList);
      } // end loop over type for sender
      mm.putMessagesForSender(sender, map);
    } // end loop over sender
    logger.info("removed: " + dedupeCount + " duplicate or superceded messages");
    mm.putContextObject("dedupeCount", dedupeCount); // for SummaryProcessor
  } // end process()

  @Override
  public void postProcess() {
    WriteProcessor.writeTable("DuplicateMesages.csv", dupEntries);
    WriteProcessor.writeTable("SupercededMesages.csv", supEntries);
  }

}
