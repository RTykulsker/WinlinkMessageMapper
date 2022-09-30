/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.tool;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.config.impl.PropertyFileConfigurationManager;
import com.surftools.winlinkMessageMapper.aggregation.AggregatorProcessor;
import com.surftools.winlinkMessageMapper.aggregation.common.NeighborAggregator;
import com.surftools.winlinkMessageMapper.aggregation.common.SimpleMultiMessageCommentAggregator;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dao.ExportedMessageReader;
import com.surftools.winlinkMessageMapper.dao.ExportedMessageWriter;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.RejectionMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GraderStrategy;
import com.surftools.winlinkMessageMapper.grade.IGrader;
import com.surftools.winlinkMessageMapper.grade.MultipleChoiceGrader;
import com.surftools.winlinkMessageMapper.grade.expect.ExpectGrader;
import com.surftools.winlinkMessageMapper.processor.message.AckProcessor;
import com.surftools.winlinkMessageMapper.processor.message.CheckInProcessor;
import com.surftools.winlinkMessageMapper.processor.message.DyfiProcessor;
import com.surftools.winlinkMessageMapper.processor.message.EtoCheckInProcessor;
import com.surftools.winlinkMessageMapper.processor.message.EtoCheckInV2Processor;
import com.surftools.winlinkMessageMapper.processor.message.HospitalBedProcessor;
import com.surftools.winlinkMessageMapper.processor.message.IProcessor;
import com.surftools.winlinkMessageMapper.processor.message.Ics213Processor;
import com.surftools.winlinkMessageMapper.processor.message.Ics213ReplyProcessor;
import com.surftools.winlinkMessageMapper.processor.message.PositionProcessor;
import com.surftools.winlinkMessageMapper.processor.message.SpotRepProcessor;
import com.surftools.winlinkMessageMapper.processor.message.UnifiedFieldSituationProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WaISnapProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WaResourceRequestProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WxHurricaneProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WxLocalProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WxSevereProcessor;
import com.surftools.winlinkMessageMapper.processor.other.Deduplicator;
import com.surftools.winlinkMessageMapper.processor.other.ExplicitRejectionProcessor;
import com.surftools.winlinkMessageMapper.summary.Summarizer;

/**
 * read a bunch of Winlink "Exported Message" files of messages, output single CSV message file
 *
 * @author bobt
 *
 */

public class WinlinkMessageMapper {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(WinlinkMessageMapper.class);

  private Map<MessageType, IProcessor> processorMap;
  private IConfigurationManager cm;

  @Option(name = "--configurationFile", usage = "path to configuration file", required = false)
  private String configurationFileName = "configuration.txt";

  private MessageType requiredMessageType = null;
  private Set<String> dumpIdsSet = null;

  public static void main(String[] args) {
    WinlinkMessageMapper app = new WinlinkMessageMapper();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  private void run() {
    try {

      cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
      dumpIdsSet = makeDumpIds(cm.getAsString(Key.DUMP_IDS));

      var pathName = cm.getAsString(Key.PATH);
      Path path = Paths.get(pathName);
      if (!Files.exists(path)) {
        logger.error("specified path: " + pathName + " does not exist");
        System.exit(1);
      } else {
        logger.debug("path: " + path);
      }

      var requiredMessageTypeString = cm.getAsString(Key.REQUIRED_MESSAGE_TYPE);
      if (requiredMessageTypeString != null) {
        requiredMessageType = MessageType.valueOf(requiredMessageTypeString);
        if (requiredMessageType == null) {
          logger.error("requireMessageType: " + requiredMessageTypeString + " does not exist");
          System.exit(1);
        } else {
          logger.info("requireMessageType: " + requiredMessageType.toString());
        }
      }

      // fail-fast if we can't make the graders
      var graderMap = makeGraders(cm.getAsString(Key.GRADE_KEY));

      logger.info("WinlinkMessageMapper, starting with input path: " + path);

      if (requiredMessageTypeString != null) {
        requiredMessageType = MessageType.valueOf(requiredMessageTypeString);
      }

      // read all ExportedMessages from the files
      var exportedMessages = readAllExportedMessages(cm);

      warnForLateMessages(exportedMessages);

      // transform ExportedMessages into type-specific messages
      var messageMap = processAllExportedMessages(exportedMessages);

      // explicit rejections
      var rejectionProcessor = new ExplicitRejectionProcessor(path);
      rejectionProcessor.processExplicitRejections(messageMap);

      // deduplication
      var deduplicator = new Deduplicator(cm.getAsInt(Key.DEDUPLICATION_THRESHOLD_Meters, 100));
      deduplicator.deduplicate(messageMap);

      // grade individual messages after deduplication, grade summary and processor summary
      gradeAndPostProcesss(messageMap, graderMap);

      // output
      var writer = new ExportedMessageWriter(pathName);
      writer.writeAll(messageMap);

      // summary
      var databasePathName = cm.getAsString(Key.DATABASE_PATH);
      if (databasePathName != null) {
        var summarizer = new Summarizer(cm, databasePathName, pathName);
        summarizer.setDumpIds(dumpIdsSet);
        summarizer.summarize(messageMap);
      }

      var aggregatorName = cm.getAsString(Key.AGGREGATOR_NAME);
      if (aggregatorName != null) {
        var aggregatorProcessor = new AggregatorProcessor(aggregatorName);
        aggregatorProcessor.setDumpIds(dumpIdsSet);
        aggregatorProcessor.aggregate(messageMap, pathName);
      }

      var mmCommentKey = cm.getAsString(Key.MM_COMMENT_KEY);
      if (mmCommentKey != null) {
        var mmCommentProcessor = new SimpleMultiMessageCommentAggregator(mmCommentKey);
        mmCommentProcessor.aggregate(messageMap, pathName);
        mmCommentProcessor.output(pathName);
      }

      var neighborAggregator = new NeighborAggregator(10, 10);
      neighborAggregator.aggregate(messageMap);
      neighborAggregator.output(pathName);

      missingDataReport(messageMap);

      logger.info("exiting");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * iterate through all read, but unclassified messages to see if any are "late", or more precisely, "too old"
   *
   * requires the following configuration items:
   *
   * EXERCISE_DATE -- date (YYYY-MM-DD) when exercise occurred
   *
   * MAX_DAYS_BEFORE_LATE -- number of days (default 7) to consider a message "too old"
   *
   * @param exportedMessages
   */
  private void warnForLateMessages(List<ExportedMessage> exportedMessages) {
    var exerciseDateString = cm.getAsString(Key.EXERCISE_DATE).replaceAll("/", "-");
    var exerciseDate = LocalDate.parse(exerciseDateString);
    var exerciseDateTime = LocalDateTime.of(exerciseDate, LocalTime.MIDNIGHT);
    var maxLateDays = cm.getAsInt(Key.MAX_DAYS_BEFORE_LATE, 7);

    var count = 0; // total late count
    var toCountMap = new HashMap<String, Integer>(); // count by destination (clearinghouse)
    var lateCountMap = new TreeMap<String, Integer>(); // count of late messages by clearinghouse
    for (var m : exportedMessages) {
      var messageDateTime = m.dateTime;
      var duration = Duration.between(messageDateTime, exerciseDateTime);
      var to = m.to; // destination/clearinghouse

      // always increment the count by destination
      var toCount = toCountMap.getOrDefault(to, Integer.valueOf(0));
      ++toCount;
      toCountMap.put(to, toCount);

      if (duration.toDays() > maxLateDays) {
        // only if late
        ++count;
        logger
            .warn("### late message: from " + m.from + ", to: " + m.to + ", mId: " + m.messageId + ", duration: "
                + duration.toDays());
        var lateCount = lateCountMap.getOrDefault(to, Integer.valueOf(0));
        ++lateCount;
        lateCountMap.put(to, lateCount);
      }
    }

    // output as needed
    if (count > 0) {
      logger.warn("### " + count + " total late messages");

      for (var to : lateCountMap.keySet()) {
        var lateCount = lateCountMap.get(to);
        var toCount = toCountMap.get(to);
        logger.warn("### to: " + to + ", late: " + lateCount + ", total: " + toCount);
      }
    }
  }

  /**
   * grade individual messages after deduplication, because grade lost in the de-dupe process
   *
   * both grader and processor have summaries;
   *
   * grader summary can be exercise-specific,
   *
   * processor summary is more general
   *
   * @param messageMap
   * @param graderMap
   */
  private void gradeAndPostProcesss(Map<MessageType, List<ExportedMessage>> messageMap,
      Map<MessageType, IGrader> graderMap) {

    for (MessageType messageType : processorMap.keySet()) {
      StringBuilder sb = new StringBuilder();
      var exportedMessages = messageMap.get(messageType);
      IGrader grader = graderMap.get(messageType);
      if (grader != null && exportedMessages != null) {
        var gradedMessages = new ArrayList<GradableMessage>(exportedMessages.size());
        for (var m : exportedMessages) {
          var message = (GradableMessage) m;
          grader.grade(message);
          gradedMessages.add(message);
        }
        sb.append(grader.getPostProcessReport(gradedMessages));

        exportedMessages.clear();
        for (var message : gradedMessages) {
          var m = (ExportedMessage) message;
          exportedMessages.add(m);
        }
        messageMap.put(messageType, exportedMessages);
      } // endif grader not null

      IProcessor processor = processorMap.get(messageType);
      if (processor != null) {
        sb.append(processor.getPostProcessReport(exportedMessages));
      }

      if (sb.length() > 0) {
        logger.info("Post Processing Report for MessageType: " + messageType.toString() + ":\n" + sb.toString());
      }
    } // end message type
  }

  /**
   * process all ExportedMessages, transforming them into a more specific Message
   *
   * @param exportedMessages
   * @return
   */
  private Map<MessageType, List<ExportedMessage>> processAllExportedMessages(List<ExportedMessage> exportedMessages) {
    processorMap = makeProcessorMap();

    Map<MessageType, List<ExportedMessage>> messageMap = new HashMap<>();

    for (ExportedMessage exportedMessage : exportedMessages) {
      var messageType = getMessageType(exportedMessage);
      var processor = processorMap.get(messageType);
      ExportedMessage processedMessage = null;

      if (requiredMessageType != null && messageType != requiredMessageType) {
        processedMessage = new RejectionMessage(exportedMessage, RejectType.WRONG_MESSAGE_TYPE, messageType.toString());
        messageType = MessageType.REJECTS;
      } else if (messageType == MessageType.UNKNOWN || processor == null) {
        processedMessage = new RejectionMessage(exportedMessage, RejectType.UNSUPPORTED_TYPE, exportedMessage.subject);
        messageType = MessageType.REJECTS;
      } else {
        processedMessage = processor.process(exportedMessage);
        if (processedMessage instanceof RejectionMessage) {
          messageType = MessageType.REJECTS;
        }
      }

      List<ExportedMessage> list = messageMap.getOrDefault(messageType, new ArrayList<ExportedMessage>());
      list.add(processedMessage);
      messageMap.put(messageType, list);
    }

    // integrity check
    var mapMessageCount = 0;
    for (MessageType messageType : messageMap.keySet()) {
      var list = messageMap.get(messageType);
      mapMessageCount += list.size();
    }

    if (mapMessageCount != exportedMessages.size()) {
      logger.warn("Exported messages: " + exportedMessages.size() + " != mapMessageCount: " + mapMessageCount);
    }

    return messageMap;
  }

  /**
   * extract all the ExportedMessages from the files
   *
   * @param cm
   * @return
   */
  private List<ExportedMessage> readAllExportedMessages(IConfigurationManager cm) {

    var pathName = cm.getAsString(Key.PATH);
    Path path = Paths.get(pathName);

    ExportedMessageReader reader = new ExportedMessageReader();
    reader.setPreferredPrefixes(cm.getAsString(Key.PREFERRED_PREFIXES, "ETO"));
    reader.setPreferredSuffixes(cm.getAsString(Key.PREFERRED_SUFFEXES, "Winlink.org,winlink.org"));
    reader.setNotPreferredPrefixes(cm.getAsString(Key.NOT_PREFERRED_PREFIXES, "QTH,SMTP"));
    reader.setNotPreferredSuffixes(cm.getAsString(Key.NOT_PREFERRED_SUFFIXES));
    reader.setDumpIds(dumpIdsSet);

    // read all Exported Messages from files
    List<ExportedMessage> exportedMessages = new ArrayList<>();
    for (File file : path.toFile().listFiles()) {
      if (file.isFile()) {
        if (!file.getName().toLowerCase().endsWith(".xml")) {
          continue;
        }
        var fileExportedMessages = reader.readAll(file.toPath());
        exportedMessages.addAll(fileExportedMessages);
      }
    }
    logger.info("read " + exportedMessages.size() + " exported messages from all files");

    return exportedMessages;
  }

  /**
   * instantiate all IProcessors, put into a map
   *
   * @return
   */
  private Map<MessageType, IProcessor> makeProcessorMap() {
    var processorMap = new HashMap<MessageType, IProcessor>();

    processorMap.put(MessageType.CHECK_IN, new CheckInProcessor(true));
    processorMap.put(MessageType.CHECK_OUT, new CheckInProcessor(false));
    processorMap.put(MessageType.SPOTREP, new SpotRepProcessor());
    processorMap.put(MessageType.DYFI, new DyfiProcessor());

    processorMap.put(MessageType.WX_LOCAL, new WxLocalProcessor());
    processorMap.put(MessageType.WX_SEVERE, new WxSevereProcessor());
    processorMap.put(MessageType.WX_HURRICANE, new WxHurricaneProcessor());

    processorMap.put(MessageType.HOSPITAL_BED, new HospitalBedProcessor());
    processorMap.put(MessageType.POSITION, new PositionProcessor());

    processorMap.put(MessageType.ICS_213, new Ics213Processor(MessageType.ICS_213));
    processorMap.put(MessageType.GIS_ICS_213, new Ics213Processor(MessageType.GIS_ICS_213));
    processorMap.put(MessageType.ETO_CHECK_IN, new EtoCheckInProcessor());
    processorMap.put(MessageType.ETO_CHECK_IN_V2, new EtoCheckInV2Processor());

    processorMap.put(MessageType.UNIFIED_FIELD_SITUATION, new UnifiedFieldSituationProcessor());
    // TODO delete me
    // processorMap.put(MessageType.FIELD_SITUATION_REPORT, new FieldSituationProcessor());
    // processorMap.put(MessageType.FIELD_SITUATION_REPORT_23, new FieldSituationProcessor_23());
    // processorMap.put(MessageType.FIELD_SITUATION_REPORT_25, null);

    processorMap.put(MessageType.WA_RR, new WaResourceRequestProcessor());
    processorMap.put(MessageType.WA_ISNAP, new WaISnapProcessor());

    processorMap.put(MessageType.ACK, new AckProcessor());
    processorMap.put(MessageType.ICS_213_REPLY, new Ics213ReplyProcessor());

    if (requiredMessageType != null) {
      for (MessageType messageType : processorMap.keySet()) {
        if (messageType != requiredMessageType) {
          processorMap.remove(messageType);
        } // end if not required type
      } // end for over types in map
    } // end if requiredMessageType != null

    for (IProcessor processor : processorMap.values()) {
      processor.setDumpIds(dumpIdsSet);
      processor.setPath(Paths.get(cm.getAsString(Key.PATH)));
    }

    return processorMap;
  }

  /**
   * parse the gradeKey, construct the appropriate grader
   *
   * @param gradeKey
   * @return
   */
  private Map<MessageType, IGrader> makeGraders(String gradeKey) {
    var graderMap = new HashMap<MessageType, IGrader>();
    var explanations = new ArrayList<String>();

    if (gradeKey == null) {
      return graderMap;
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

      var jsonMap = mapper.readValue(gradeKey, Map.class);
      for (var messageTypeName : jsonMap.keySet()) {
        var messageType = MessageType.fromString((String) messageTypeName);
        if (messageType == null) {
          explanations.add("messageType: " + messageTypeName + " not recognized");
          continue;
        }

        var value = (String) jsonMap.get(messageTypeName);
        if (value == null || value.length() == 0) {
          explanations.add("null or empty details for type: " + messageType);
          continue;
        }

        var fields = value.split(":");
        if (fields.length < 2) {
          explanations.add("can't parse details for type: " + messageType);
          continue;
        }

        var graderStrategy = GraderStrategy.fromString(fields[0]);
        if (graderStrategy == null) {
          explanations.add("unsupported strategy for type: " + messageType);
        }

        IGrader grader = null;
        switch (graderStrategy) {
        case CLASS:
          // if (fields[1].equals("ETO_2022_08_18")) {
          // grader = new ETO_2022_08_18();
          // grader.setDumpIds(dumpIdsSet);
          // grader.setConfigurationManager(cm);
          // graderMap.put(messageType, grader);
          // break;
          // }

          final var prefixes = new String[] { "com.surftools.winlinkMessageMapper.grade.named.", "" };

          for (var prefix : prefixes) {
            if (grader != null) {
              continue;
            }
            var className = prefix + fields[1];
            try {
              var clazz = Class.forName(className);
              if (clazz != null) {
                grader = (IGrader) clazz.getDeclaredConstructor().newInstance();
                break;
              }
            } catch (Exception e) {
              ;// explanations.add("could not find grader class for name: " + className);
            }
          } // end loop over prefixes
          if (grader == null) {
            explanations.add("could not find grader class for name: " + fields[1]);
          } else {
            grader.setDumpIds(dumpIdsSet);
            grader.setConfigurationManager(cm);
            graderMap.put(messageType, grader);
          }
          break;

        case EXPECT:
          var expectGrader = new ExpectGrader(messageType, fields[1]);
          expectGrader.setDumpIds(dumpIdsSet);
          expectGrader.setConfigurationManager(cm);
          graderMap.put(messageType, expectGrader);
          break;

        case MULTIPLE_CHOICE:
          var mcGrader = new MultipleChoiceGrader(messageType);
          mcGrader.parse(messageTypeName + ":" + value);
          mcGrader.setDumpIds(dumpIdsSet);
          mcGrader.setConfigurationManager(cm);
          graderMap.put(messageType, mcGrader);
          break;
        }
      }
    } catch (Exception e) {
      explanations.add(e.getLocalizedMessage());
    }

    if (explanations.size() == 0) {
      return graderMap;
    } else {
      throw new RuntimeException(
          "could not parse gradeKey: " + gradeKey + "\ndetails: " + String.join(",", explanations + "\n"));
    }
  }

  public Set<String> makeDumpIds(String dumpIdsString) {
    Set<String> set = new HashSet<>();
    if (dumpIdsString != null) {
      String[] fields = dumpIdsString.split(",");
      set.addAll(Arrays.asList(fields));
      logger.info("dumpIds: " + String.join(",", set));
    }
    return set;
  }

  /**
   * infer the specific type of an ExportedMessage
   *
   * @param message
   * @return
   */
  public static MessageType getMessageType(ExportedMessage message) {
    var subject = message.subject;

    /**
     * mime based
     */
    var attachments = message.attachments;
    if (attachments != null && attachments.size() > 0) {
      var attachmentNames = attachments.keySet();

      // because there is/was FSR, FSR_23, FSR_25, FSR_26 ...
      for (var name : attachmentNames) {
        if (name.startsWith(MessageType.UNIFIED_FIELD_SITUATION.attachmentName())) {
          return MessageType.UNIFIED_FIELD_SITUATION;
        }
      }

      if (attachmentNames.contains(MessageType.ICS_213.attachmentName())) {
        return MessageType.ICS_213;
      } else if (attachmentNames.contains(MessageType.CHECK_IN.attachmentName())) {
        return MessageType.CHECK_IN;
      } else if (attachmentNames.contains(MessageType.CHECK_OUT.attachmentName())) {
        return MessageType.CHECK_OUT;
      } else if (attachmentNames.contains(MessageType.HOSPITAL_BED.attachmentName())) {
        return MessageType.HOSPITAL_BED;
      } else if (attachmentNames.contains(MessageType.SPOTREP.attachmentName())) {
        return MessageType.SPOTREP;
      } else if (attachmentNames.contains(MessageType.WX_LOCAL.attachmentName())) {
        return MessageType.WX_LOCAL;
      } else if (attachmentNames.contains(MessageType.WX_SEVERE.attachmentName())) {
        return MessageType.WX_SEVERE;
      } else if (attachmentNames.contains(MessageType.WA_RR.attachmentName())) {
        return MessageType.WA_RR;
      } else if (attachmentNames.contains(MessageType.WA_ISNAP.attachmentName())) {
        return MessageType.WA_ISNAP;
      } else if (attachmentNames.contains(MessageType.ICS_213_REPLY.attachmentName())) {
        return MessageType.ICS_213_REPLY;
      }
    }
    /**
     * subject-based
     */
    if (subject.startsWith("DYFI Automatic Entry")) {
      return MessageType.DYFI;
    } else if (subject.startsWith("Hurricane Report")) {
      return MessageType.WX_HURRICANE;
    } else if (subject.startsWith("Winlink Thursday Net Check-In")
        || subject.startsWith("Re: Winlink Thursday Net Check-In")) {
      return MessageType.ETO_CHECK_IN;
    } else if (subject.startsWith("ETO Winlink Thursday Check-In")
        || subject.startsWith("Re: ETO Winlink Thursday Check-In")) {
      return MessageType.ETO_CHECK_IN_V2;
    } else if (subject.equals("Position Report")) {
      return MessageType.POSITION;
    } else if (subject.startsWith("ACK:")) {
      return MessageType.ACK;
    } else {
      return MessageType.UNKNOWN;
    }
  }

  private void missingDataReport(Map<MessageType, List<ExportedMessage>> messageMap) {
    var toMap = new TreeMap<String, Integer>();
    var messageType = MessageType.fromString(cm.getAsString(Key.EXERCISE_MESSAGE_TYPE));
    var messageList = messageMap.get(messageType);

    if (messageList != null) {
      for (var message : messageList) {
        var to = message.to;
        var count = toMap.getOrDefault(to, Integer.valueOf(0));
        ++count;
        toMap.put(to, count);
      }
    }

    final var defaultExpectedDestinations = "ETO-01,ETO-02,ETO-03,ETO-04,ETO-05,ETO-06,ETO-07,ETO-08,ETO-09,ETO-10,ETO-CAN,ETO-DX";
    var expectedToString = cm.getAsString(Key.EXPECTED_DESTINATIONS, defaultExpectedDestinations);
    var expectedSet = new TreeSet<String>(Arrays.asList(expectedToString.split(",")));

    var missingExpectedDestinations = new TreeSet<String>(expectedSet);
    var foundUnexpectedDestinations = new TreeSet<String>();

    for (var to : toMap.keySet()) {
      if (expectedSet.contains(to)) {
        missingExpectedDestinations.remove(to);
      } else {
        foundUnexpectedDestinations.add(to);
      }
    }

    if (missingExpectedDestinations.size() == 0) {
      logger.info("all expected destinations found");
    } else {
      logger.warn("missing destinations: " + String.join(",", missingExpectedDestinations));
    }

    if (foundUnexpectedDestinations.size() == 0) {
      logger.info("no unexpected destinations found");
    } else {
      var list = new ArrayList<String>();
      for (var to : foundUnexpectedDestinations) {
        list.add(to + "(" + toMap.get(to) + ")");
      }
      logger.warn("found unexpected destinations: " + String.join(",", list));
    }
  }

}
