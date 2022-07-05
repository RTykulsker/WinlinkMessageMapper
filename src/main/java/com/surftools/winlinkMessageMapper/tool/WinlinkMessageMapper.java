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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.winlinkMessageMapper.aggregation.AggregatorProcessor;
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
import com.surftools.winlinkMessageMapper.multiMessageMapper.SimpleMultiMessageCommentProcessor;
import com.surftools.winlinkMessageMapper.processor.message.AbstractBaseProcessor;
import com.surftools.winlinkMessageMapper.processor.message.AckProcessor;
import com.surftools.winlinkMessageMapper.processor.message.CheckInProcessor;
import com.surftools.winlinkMessageMapper.processor.message.DyfiProcessor;
import com.surftools.winlinkMessageMapper.processor.message.EtoCheckInProcessor;
import com.surftools.winlinkMessageMapper.processor.message.FieldSituationProcessor;
import com.surftools.winlinkMessageMapper.processor.message.FieldSituationProcessor_23;
import com.surftools.winlinkMessageMapper.processor.message.HospitalBedProcessor;
import com.surftools.winlinkMessageMapper.processor.message.IProcessor;
import com.surftools.winlinkMessageMapper.processor.message.Ics213Processor;
import com.surftools.winlinkMessageMapper.processor.message.Ics213ReplyProcessor;
import com.surftools.winlinkMessageMapper.processor.message.PositionProcessor;
import com.surftools.winlinkMessageMapper.processor.message.SpotRepProcessor;
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

  @Option(name = "--path", usage = "path to message files", required = true)
  private String pathName = null;

  @Option(name = "--databasePath", usage = "path to input database summary files", required = false)
  private String databasePathName = "/home/bobt/Documents/eto/database";

  @Option(name = "--requiredMessageType", usage = "to ONLY process messages of a given type", required = false)
  private String requiredMessageTypeString = null;
  private MessageType requiredMessageType = null;

  @Option(name = "--dumpIds", usage = "comma-delimited list of messageIds or call signs to dump message contents for", required = false)
  private String dumpIdsString = null;
  private Set<String> dumpIdsSet = null;

  @Option(name = "--preferredPrefixes", usage = "comma-delimited preferred prefixes for To: addresses")
  private String preferredPrefixes = "ETO";

  @Option(name = "--preferredSuffixes", usage = "comma-delimited preferred suffixes for To: addresses")
  private String preferredSuffixes = "Winlink.org,winlink.org";

  @Option(name = "--notPreferredPrefixes", usage = "comma-delimited NOT preferred prefixes for To: addresses")
  private String notPreferredPrefixes = "QTH,SMTP";

  @Option(name = "--notPreferredSuffixes", usage = "comma-delimited NOT preferred suffixes for To: addresses.")
  private String notPreferredSuffixes = null;

  @Option(name = "--deduplicationThresholdMeters", usage = "threshold distance in meters to avoid being considered a duplicate location, negative to skip")
  private int deduplicationThresholdMeters = 100;

  @Option(name = "--saveAttachments", usage = "save ALL attachments in exported message")
  private boolean saveAttachments = false;

  @Option(name = "--gradeKey", usage = "exercise/processor-specific key to identify grading method, if any")
  private String gradeKey = null;

  @Option(name = "--aggregatorName", usage = "exercise-specific name of multi-message aggregator, if any")
  private String aggregatorName = null;

  @Option(name = "--mmCommentKey", usage = "MultiMessageComment key, if any")
  private String mmCommentKey = null;

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

      Path path = Paths.get(pathName);
      if (!Files.exists(path)) {
        logger.error("specified path: " + pathName + " does not exist");
        System.exit(1);
      } else {
        logger.debug("path: " + path);
      }

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
      var graderMap = makeGraders(gradeKey);

      logger.info("WinlinkMessageMapper, starting with input path: " + path);

      if (requiredMessageTypeString != null) {
        requiredMessageType = MessageType.valueOf(requiredMessageTypeString);
      }

      // read all ExportedMessages from the files
      var exportedMessages = readAllExportedMessages(path, preferredPrefixes, preferredSuffixes, notPreferredPrefixes,
          notPreferredSuffixes);

      // transform ExportedMessages into type-specific messages
      var messageMap = processAllExportedMessages(exportedMessages);

      // explicit rejections
      var rejectionProcessor = new ExplicitRejectionProcessor(path);
      rejectionProcessor.processExplicitRejections(messageMap);

      // deduplication
      var deduplicator = new Deduplicator(deduplicationThresholdMeters);
      deduplicator.deduplicate(messageMap);

      // grade individual messages after deduplication, grade summary and processor summary
      gradeAndPostProcesss(messageMap, graderMap);

      // output
      var writer = new ExportedMessageWriter(pathName);
      writer.writeAll(messageMap);

      // summary
      if (databasePathName != null) {
        var summarizer = new Summarizer(databasePathName, pathName);
        summarizer.setDumpIds(dumpIdsSet);
        summarizer.summarize(messageMap);
      }

      if (aggregatorName != null) {
        var aggregatorProcessor = new AggregatorProcessor(aggregatorName);
        aggregatorProcessor.aggregate(messageMap, pathName);
      }

      if (mmCommentKey != null) {
        var mmCommentProcessor = new SimpleMultiMessageCommentProcessor(mmCommentKey);
        mmCommentProcessor.aggregate(messageMap, pathName);
        mmCommentProcessor.output(pathName);
      }

      logger.info("exiting");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
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
    var processorMap = makeProcessorMap();

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
   * @param path
   * @param preferredPrefixes
   * @param preferredSuffixes
   * @param notPreferredPrefixes
   * @param notPreferredSuffixes
   * @return
   */
  private List<ExportedMessage> readAllExportedMessages(Path path, String preferredPrefixes, String preferredSuffixes,
      String notPreferredPrefixes, String notPreferredSuffixes) {

    ExportedMessageReader reader = new ExportedMessageReader();
    reader.setPreferredPrefixes(preferredPrefixes);
    reader.setPreferredSuffixes(preferredPrefixes);
    reader.setNotPreferredPrefixes(notPreferredPrefixes);
    reader.setNotPreferredSuffixes(notPreferredSuffixes);

    if (dumpIdsSet == null) {
      dumpIdsSet = makeDumpIds(dumpIdsString);
      AbstractBaseProcessor.setDumpIds(dumpIdsSet);
      reader.setDumpIds(dumpIdsSet);
    }

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

    processorMap.put(MessageType.FIELD_SITUATION_REPORT, new FieldSituationProcessor());
    processorMap.put(MessageType.FIELD_SITUATION_REPORT_23, new FieldSituationProcessor_23());

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

    if (dumpIdsSet == null) {
      dumpIdsSet = makeDumpIds(dumpIdsString);
      AbstractBaseProcessor.setDumpIds(dumpIdsSet);
    }

    AbstractBaseProcessor.setPath(Paths.get(pathName));
    AbstractBaseProcessor.setSaveAttachments(saveAttachments);

    this.processorMap = processorMap;
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
          final var prefixes = new String[] { "com.surftools.winlinkMessageMapper.grade.named.", "" };

          for (var prefix : prefixes) {
            var className = prefix + fields[1];
            try {
              var clazz = Class.forName(className);
              if (clazz != null) {
                grader = (IGrader) clazz.getDeclaredConstructor().newInstance();
                graderMap.put(messageType, grader);
              }
            } catch (Exception e) {
              ;// explanations.add("could not find grader class for name: " + className);
            }
          } // end loop over prefixes
          if (grader == null) {
            explanations.add("could not find grader class for name: " + fields[1]);
          } else {
            grader.setDumpIds(dumpIdsSet);
            graderMap.put(messageType, grader);
          }
          break;

        case EXPECT:
          var expectGrader = new ExpectGrader(messageType, fields[1]);
          expectGrader.setDumpIds(dumpIdsSet);
          graderMap.put(messageType, expectGrader);
          break;

        case MULTIPLE_CHOICE:
          var mcGrader = new MultipleChoiceGrader(messageType);
          mcGrader.parse(messageTypeName + ":" + value);
          mcGrader.setDumpIds(dumpIdsSet);
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
      for (String string : fields) {
        set.add(string);
      }
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
  public MessageType getMessageType(ExportedMessage message) {
    var subject = message.subject;

    /**
     * mime based
     */
    var attachments = message.attachments;
    var attachmentNames = attachments.keySet();

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
    } else if (attachmentNames.contains(MessageType.FIELD_SITUATION_REPORT.attachmentName())) {
      return MessageType.FIELD_SITUATION_REPORT;
    } else if (attachmentNames.contains(MessageType.FIELD_SITUATION_REPORT_23.attachmentName())) {
      return MessageType.FIELD_SITUATION_REPORT_23;
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
      /**
       * subject-based
       */
    } else if (subject.startsWith("DYFI Automatic Entry")) {
      return MessageType.DYFI;
    } else if (subject.startsWith("Hurricane Report")) {
      return MessageType.WX_HURRICANE;
    } else if (subject.startsWith("Winlink Thursday Net Check-In")
        || subject.startsWith("Re: Winlink Thursday Net Check-In")) {
      return MessageType.ETO_CHECK_IN;
    } else if (subject.equals("Position Report")) {
      return MessageType.POSITION;
    } else if (subject.startsWith("ACK:")) {
      return MessageType.ACK;
    } else {
      return MessageType.UNKNOWN;
    }
  }
}
