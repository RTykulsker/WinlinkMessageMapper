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

package com.surftools.wimp.processors.eto_2024;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.message.PdfIcs309Message;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-11-09: Semi-annual drill: Winlink Check, 4 sequential FSR and a generated ICS-309
 *
 * @author bobt
 *
 */
public class ETO_2024_11_09 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_11_09.class);

  private class Summary implements IWritableTable {
    public String sender;
    public LatLongPair location;

    public List<String> explanations;

    // we want these three messages
    public CheckInMessage checkInMessage;
    public FieldSituationMessage fsrDay1Message;
    public FieldSituationMessage fsrDay2Message;
    public FieldSituationMessage fsrDay3Message;
    public FieldSituationMessage fsrDay4Message;
    public PdfIcs309Message pdfIcs309Message;

    public Summary(String sender) {
      this.sender = sender;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Summary) other;
      return sender.compareTo(o.sender);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "Latitude", "Longitude", "Feedback Count", "Feedback", //
          "CheckIn", "FSR Day 1", "FSR Day 2", "FSR Day 3", "FSR Day 4", "PDF ICS-309", //
      };
    }

    private String mId(ExportedMessage m) {
      return m == null ? "" : m.messageId;
    }

    @Override
    public String[] getValues() {
      var latitude = (location != null && location.isValid()) ? location.getLatitude() : "0.0";
      var longitude = (location != null && location.isValid()) ? location.getLongitude() : "0.0";

      var feedback = (explanations.size() == 0) ? "Perfect messages!" : String.join("\n", explanations);
      return new String[] { sender, latitude, longitude, //
          String.valueOf(explanations.size()), feedback, //
          mId(checkInMessage), mId(fsrDay1Message), mId(fsrDay2Message), mId(fsrDay3Message), mId(fsrDay4Message),
          mId(pdfIcs309Message), //
      };
    }
  }

  private Map<String, Summary> summaryMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Ics309Message.setNDisplayActivities(12);

    var acceptableMessageTypesList = List
        .of( // order matters, last location wins,
            MessageType.CHECK_IN, MessageType.FIELD_SITUATION, MessageType.PDF_ICS_309);
    acceptableMessageTypesSet.addAll(acceptableMessageTypesList);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var summary = summaryMap.getOrDefault(sender, new Summary(sender));

    var type = message.getMessageType();
    if (type == MessageType.CHECK_IN) {
      handle_CheckInMessage(summary, (CheckInMessage) message);
    } else if (type == MessageType.FIELD_SITUATION) {
      handle_FieldSituationMessage(summary, (FieldSituationMessage) message);
    } else if (type == MessageType.PDF_ICS_309) {
      handle_PdfIcs309Message(summary, (PdfIcs309Message) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }

    // last valid location wins; order of messageTypes matters
    summary.location = (message.mapLocation != null && message.mapLocation.isValid()) ? message.mapLocation
        : LatLongPair.INVALID;
    summary.explanations.addAll(sts.getExplanations());
    summaryMap.put(sender, summary);
  }

  private void handle_PdfIcs309Message(Summary summary, PdfIcs309Message m) {
    summary.pdfIcs309Message = m;
    count(sts.test("ICS-309 task number should be #EV", "01 Sep", m.taskNumber));
    count(sts.test("ICS-309 task name should be #EV", "RRI Welfare Message Exercise", m.taskName));
    count(sts.test("ICS-309 operationsal period should be #EV", "191500-201500 UTC Sep 24", m.operationalPeriod));
    count(sts.testIfPresent("Operator Name should be present", m.operatorName));
    count(sts.testIfPresent("Station ID should be present", m.stationId));

    var activitiesSubjectSet = m.activities.stream().map(a -> a.subject()).collect(Collectors.toSet());

    writePdf(m);
  }

  private void writePdf(PdfIcs309Message m) {
    var attachmentIndices = m.pdfAttachmentIndices;
    if (attachmentIndices == null || attachmentIndices.length() == 0) {
      logger.warn("can't write pdf for " + m.from + ", mid: " + m.messageId + ", no attachments");
      return;
    }

    var fields = attachmentIndices.split(",");
    if (fields == null || fields.length == 0) {
      logger.warn("can't write pdf for " + m.from + ", mid: " + m.messageId + ", no attachments");
      return;
    }

    var firstIndex = Integer.valueOf(fields[0]);
    var keyList = new ArrayList<String>(m.attachments.keySet());
    var attachmentName = keyList.get(firstIndex);
    var bytes = m.attachments.get(attachmentName);
    var filePath = Path.of(outputPath.toString(), "extractedPdfs", m.from + "-" + m.messageId + "-" + "ics309.pdf");
    try {
      FileUtils.makeDirIfNeeded(filePath.toString());
      Files.write(filePath, bytes);
    } catch (IOException e) {
      logger.error("couldn't write pdf for " + m.from + ", mId: " + m.messageId + ", " + e.getLocalizedMessage());
    }
  }

  private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
    summary.checkInMessage = m;

    // TODO fixme
  }

  private void handle_FieldSituationMessage(Summary summary, FieldSituationMessage m) {
    // TODO get message "day"
    // TODO deduplicate, saving only latest message for given day

    var formDateTimeString = m.formDateTime;
  }

  @Override
  protected void beforeCommonProcessing(String sender, ExportedMessage message) {
    var messageType = message.getMessageType();
    if (messageType == MessageType.CHECK_IN) {
      sts.setExplanationPrefix("CheckIn: ");
    } else if (messageType == MessageType.FIELD_SITUATION) {
      sts.setExplanationPrefix("FSR: ");
    } else if (messageType == MessageType.PDF_ICS_309) {
      sts.setExplanationPrefix("ICS-309: ");
    }
  }

  @Override
  protected void endProcessingForSender(String sender) {
    var summary = summaryMap.get(sender);

    if (summary == null) {
      return;
    }

    if (summary.checkInMessage == null) {
      summary.explanations.add("No CheckIn message received.");
    }

    if (summary.fsrDay1Message == null) {
      summary.explanations.add("No FSR Day 1 message received.");
    }

    if (summary.fsrDay2Message == null) {
      summary.explanations.add("No FSR Day 2 message received.");
    }

    if (summary.fsrDay3Message == null) {
      summary.explanations.add("No FSR Day 3 message received.");
    }

    if (summary.fsrDay4Message == null) {
      summary.explanations.add("No FSR Day 4 message received.");
    }

    if (summary.pdfIcs309Message == null) {
      summary.explanations.add("No ICS-309 message received.");
    }

    // TODO other inter-message relationships

    // TODO histograms for this? VirtualMessage for histograms? globalCounter exerciseCounter, ExerciseMessage
    // SummaryMessage

    summaryMap.put(sender, summary);
  }

  @Override
  public void postProcess() {
    // don't do any outbound messaging for individual messageTypes
    var cachedOutboundMessaging = doOutboundMessaging;
    doOutboundMessaging = false;
    outboundMessageList.clear();

    super.postProcess();

    // fix bad locations
    var badLocationSenders = new ArrayList<String>();
    var summaries = summaryMap.values();
    for (var summary : summaries) {
      if (!summary.location.isValid()) {
        badLocationSenders.add(summary.sender);
      }
    }

    if (badLocationSenders.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationSenders.size() + " summaries: "
              + String.join(",", badLocationSenders));
      var newLocations = LocationUtils.jitter(badLocationSenders.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationSenders.size(); ++i) {
        var sender = badLocationSenders.get(i);
        var summary = summaryMap.get(sender);
        summary.location = newLocations.get(i);
        summaryMap.put(sender, summary);
      }
    }

    // write outbound messages, but only for summary; change subject
    if (cachedOutboundMessaging) {
      setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
      for (var summary : summaryMap.values()) {
        var outboundMessageFeedback = (summary.explanations.size() == 0) ? "Perfect messages!"
            : String.join("\n", summary.explanations) + OB_DISCLAIMER;
        var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
            "Feedback on ETO " + cm.getAsString(Key.EXERCISE_DATE) + " exercise", //
            outboundMessageFeedback, null);
        outboundMessageList.add(outboundMessage);

        var service = new OutboundMessageService(cm);
        outboundMessageList = service.sendAll(outboundMessageList);
        writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
      }
    }

    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(summaryMap.values()), Path.of(outputPathName, "drill-summary.csv"));
  }
}
