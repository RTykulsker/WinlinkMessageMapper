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
import java.io.FileWriter;
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

import com.opencsv.CSVWriter;
import com.surftools.winlinkMessageMapper.dao.ExportedMessageReader;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;
import com.surftools.winlinkMessageMapper.dto.message.IMessage;
import com.surftools.winlinkMessageMapper.dto.message.RejectionMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;
import com.surftools.winlinkMessageMapper.dto.other.RejectTypeContextPair;
import com.surftools.winlinkMessageMapper.processor.message.AbstractBaseProcessor;
import com.surftools.winlinkMessageMapper.processor.message.CheckInProcessor;
import com.surftools.winlinkMessageMapper.processor.message.DyfiProcessor;
import com.surftools.winlinkMessageMapper.processor.message.EtoCheckInProcessor;
import com.surftools.winlinkMessageMapper.processor.message.HospitalBedProcessor;
import com.surftools.winlinkMessageMapper.processor.message.IProcessor;
import com.surftools.winlinkMessageMapper.processor.message.Ics213Processor;
import com.surftools.winlinkMessageMapper.processor.message.PositionProcessor;
import com.surftools.winlinkMessageMapper.processor.message.SpotRepProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WxHurricaneProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WxLocalProcessor;
import com.surftools.winlinkMessageMapper.processor.message.WxSevereProcessor;
import com.surftools.winlinkMessageMapper.processor.other.Deduplicator;

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

  @Option(name = "--path", usage = "path to message files", required = true)
  private String pathName = null;

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

      logger.info("WinlinkMessageMapper, starting with input path: " + path);

      if (requiredMessageTypeString != null) {
        requiredMessageType = MessageType.valueOf(requiredMessageTypeString);
      }

      // read all ExportedMessages from the files
      var exportedMessages = readAllExportedMessages(path, preferredPrefixes, preferredSuffixes, notPreferredPrefixes,
          notPreferredSuffixes);

      // transform ExportedMessages into type-specific messages
      var processResults = processAllExportedMessages(exportedMessages);

      // explicitly reject, deduplicate and out the results
      outputResults(path, processResults);

      logger.info("exiting");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  protected ExportedMessage reject(ExportedMessage message, RejectType reason, String context) {
    return new RejectionMessage(message, reason, context);
  }

  /**
   * process all ExportedMessages, transforming them into a more specific Message
   *
   * @param exportedMessages
   * @return
   */
  private Map<MessageType, List<ExportedMessage>> processAllExportedMessages(List<ExportedMessage> exportedMessages) {
    // private MessageMapRejectionsResult processAllExportedMessages(List<ExportedMessage> exportedMessages) {
    var processorMap = makeProcessorMap();

    Map<MessageType, List<ExportedMessage>> messageMap = new HashMap<>();

    for (ExportedMessage exportedMessage : exportedMessages) {
      var messageType = getMessageType(exportedMessage);
      var processor = processorMap.get(messageType);
      ExportedMessage processedMessage = null;

      if (requiredMessageType != null && messageType != requiredMessageType) {
        processedMessage = reject(exportedMessage, RejectType.WRONG_MESSAGE_TYPE, messageType.toString());
        messageType = MessageType.REJECTIONS;
      } else if (messageType == MessageType.UNKNOWN || processor == null) {
        processedMessage = reject(exportedMessage, RejectType.UNSUPPORTED_TYPE, exportedMessage.subject);
        messageType = MessageType.REJECTIONS;
      } else {
        processedMessage = processor.process(exportedMessage);
        if (processedMessage instanceof RejectionMessage) {
          messageType = MessageType.REJECTIONS;
        }
      }

      List<ExportedMessage> list = messageMap.getOrDefault(messageType, new ArrayList<ExportedMessage>());
      list.add(processedMessage);
      messageMap.put(messageType, list);

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
   * output all processed messages, along with rejections
   *
   * @param path
   * @param processResults
   */
  private void outputResults(Path path, Map<MessageType, List<ExportedMessage>> messageMap) {

    // explicit rejections
    processExplicitRejections(path, messageMap);

    // deduplication
    var deduplicator = new Deduplicator(deduplicationThresholdMeters);
    deduplicator.deduplicate(messageMap);

    for (MessageType messageType : messageMap.keySet()) {
      List<ExportedMessage> messages = messageMap.getOrDefault(messageType, new ArrayList<ExportedMessage>());

      if (messages.size() > 0) {
        writeOutput(messages, pathName, messageType);
      }
    }
  }

  /**
   * write out the type-specific messages
   *
   * @param messages
   * @param pathName
   * @param messageType
   */
  private void writeOutput(List<ExportedMessage> messages, String pathName, MessageType messageType) {
    Path outputPath = Path.of(pathName, "output", messageType.toString() + ".csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      if (messages.size() > 0) {
        IMessage aMessage = messages.get(0);
        writer.writeNext(aMessage.getHeaders());
        for (IMessage m : messages) {
          writer.writeNext(m.getValues());
        }
      } else {
        writer.writeNext(new String[] { "MessageId" });
      }

      writer.close();
      logger.info("wrote " + messages.size() + " messages to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * instantiate all IProcessors, put into a map
   *
   * @return
   */
  private Map<MessageType, IProcessor> makeProcessorMap() {
    Map<MessageType, IProcessor> processorMap = new HashMap<>();
    processorMap.put(MessageType.CHECK_IN, new CheckInProcessor(true));
    processorMap.put(MessageType.CHECK_OUT, new CheckInProcessor(false));
    processorMap.put(MessageType.DYFI, new DyfiProcessor());
    processorMap.put(MessageType.POSITION, new PositionProcessor());
    processorMap.put(MessageType.ETO_CHECK_IN, new EtoCheckInProcessor());
    processorMap.put(MessageType.ICS_213, new Ics213Processor(MessageType.ICS_213));
    processorMap.put(MessageType.GIS_ICS_213, new Ics213Processor(MessageType.GIS_ICS_213));
    processorMap.put(MessageType.WX_LOCAL, new WxLocalProcessor());
    processorMap.put(MessageType.WX_SEVERE, new WxSevereProcessor());
    processorMap.put(MessageType.WX_HURRICANE, new WxHurricaneProcessor());
    processorMap.put(MessageType.HOSPITAL_BED, new HospitalBedProcessor());
    processorMap.put(MessageType.SPOTREP, new SpotRepProcessor());

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

    return processorMap;
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

    // if (dumpIdsSet.contains(message.messageId) || dumpIdsSet.contains(message.from)) {
    // logger.info("getMessageType: " + message);
    // }

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
    } else if (attachmentNames.contains(MessageType.WX_LOCAL.attachmentName())) {
      return MessageType.WX_LOCAL;
    } else if (attachmentNames.contains(MessageType.WX_SEVERE.attachmentName())) {
      return MessageType.WX_SEVERE;
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
    } else {
      return MessageType.UNKNOWN;
    }
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
  private void processExplicitRejections(Path path, Map<MessageType, List<ExportedMessage>> messageMap) {

    Map<String, RejectTypeContextPair> rejectMap = makeExplicitRejectMap(path);
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
    var implicitRejections = messageMap.getOrDefault(MessageType.REJECTIONS, new ArrayList<>());
    var implicitRejectionCount = implicitRejections.size();
    implicitRejections.addAll(explicitRejections);
    messageMap.put(MessageType.REJECTIONS, implicitRejections);

    logger
        .info("implicit rejections: " + implicitRejectionCount + ", explicitRejections: " + explicitRejections.size());
    return;
  }

}
