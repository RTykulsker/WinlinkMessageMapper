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

package com.surftools.wimp.processors.exercise.eto_2024;

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
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.message.PdfIcs309Message;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.message.RRIQuickWelfareMessage;
import com.surftools.wimp.message.RRIReplyWelfareRadiogramMessage;
import com.surftools.wimp.message.RRIWelfareRadiogramMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.FeedbackProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-09-19: "awareness" exercise for both RRI Welfare Radiogram and RRI Quick Welfare
 *
 * Specifically:
 *
 * send an Welfare Radiogram to clearinghouse, ETO-BK and personal email reply to Welfare Radiogram back to winlink
 * address send a Quick Welfare to clearinghouse and ETO-BK
 *
 * then generate an ICS-309 (PDF) with the above three messages and send to clearinghouse and ETO-BK
 *
 * There should *NOT* be a reply, (form-based) ICS-309 or any other plain text message
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_09_19 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_09_19.class);

  private class Summary implements IWritableTable {
    public String sender;
    public String to;
    public LatLongPair location;

    public List<String> explanations;

    // we want these three messages
    public RRIWelfareRadiogramMessage rriWelfareRadiogramMessage;
    public RRIQuickWelfareMessage rriQuickWelfareMessage;
    public PdfIcs309Message pdfIcs309Message;

    // we don't want these three messages
    public RRIReplyWelfareRadiogramMessage rriReplyWelfareRadiogramMessage;
    public PlainMessage plainMessage;
    public Ics309Message formIcs309Message; // we don't care about this either

    public Summary(String sender, String to) {
      this.sender = sender;
      this.to = to;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String toString() {
      var sb = new StringBuilder();
      sb.append("{");
      sb.append("from: " + sender);
      sb.append(", radiogram: " + rriWelfareRadiogramMessage == null ? "null" : rriWelfareRadiogramMessage.subject);
      sb
          .append(
              ", reply: " + rriReplyWelfareRadiogramMessage == null ? "null" : rriReplyWelfareRadiogramMessage.subject);
      sb.append(", quick: " + rriQuickWelfareMessage == null ? "null" : rriQuickWelfareMessage.subject);
      sb.append("}");
      return sb.toString();
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Summary) other;
      return sender.compareTo(o.sender);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "To", "Latitude", "Longitude", "Feedback Count", "Feedback", //
          "#Radiogram", "#Quick", "#PDF ICS-309", //
          "#Reply", "#Plain", "#Form ICS-309" };
    }

    private String count(ExportedMessage m) {
      return m == null ? "" : "1";
    }

    @Override
    public String[] getValues() {
      var latitude = (location != null && location.isValid()) ? location.getLatitude() : "0.0";
      var longitude = (location != null && location.isValid()) ? location.getLongitude() : "0.0";

      var feedback = (explanations.size() == 0) ? "Perfect messages!" : String.join("\n", explanations);
      return new String[] { sender, to, latitude, longitude, //
          String.valueOf(explanations.size()), feedback, //
          count(rriWelfareRadiogramMessage), count(rriQuickWelfareMessage), count(pdfIcs309Message), //
          count(rriReplyWelfareRadiogramMessage), count(plainMessage), count(formIcs309Message) };
    }
  }

  private Map<String, Summary> summaryMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Ics309Message.setNDisplayActivities(12);

    var acceptableMessageTypesList = List // order matters, last location wins,
        .of(MessageType.PLAIN, MessageType.ICS_309, MessageType.RRI_REPLY_WELFARE_RADIOGRRAM, //
            MessageType.RRI_QUICK_WELFARE, MessageType.RRI_WELFARE_RADIOGRAM, MessageType.PDF_ICS_309);
    acceptableMessageTypesSet.addAll(acceptableMessageTypesList);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var sender = (message.getMessageType() == MessageType.RRI_REPLY_WELFARE_RADIOGRRAM) ? message.to : message.from;
    var summary = summaryMap.getOrDefault(sender, new Summary(sender, message.to));

    var type = message.getMessageType();
    if (type == MessageType.RRI_QUICK_WELFARE) {
      handle_RriQuickWelfareMessage(summary, (RRIQuickWelfareMessage) message);
    } else if (type == MessageType.RRI_WELFARE_RADIOGRAM) {
      handle_RriRadiogramMessage(summary, (RRIWelfareRadiogramMessage) message);
    } else if (type == MessageType.RRI_REPLY_WELFARE_RADIOGRRAM) {
      handle_RriReplyWelfareMessage(summary, (RRIReplyWelfareRadiogramMessage) message);
    } else if (type == MessageType.PDF_ICS_309) {
      handle_PdfIcs309Message(summary, (PdfIcs309Message) message);
    } else if (type == MessageType.ICS_309) {
      handle_formIcs309Message(summary, (Ics309Message) message);
    } else if (type == MessageType.PLAIN) {
      handlePlain(summary, (PlainMessage) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }

    // last valid location wins; order of messageTypes matters
    summary.location = (message.mapLocation != null && message.mapLocation.isValid()) ? message.mapLocation
        : LatLongPair.INVALID;
    summary.explanations.addAll(sts.getExplanations());
    summaryMap.put(sender, summary);
  }

  private void handle_formIcs309Message(Summary summary, Ics309Message m) {
    summary.formIcs309Message = m;
  }

  // but we shouldn't receive these
  private void handle_RriReplyWelfareMessage(Summary summary, RRIReplyWelfareRadiogramMessage m) {
    sts.reset(m.to);
    summary.rriReplyWelfareRadiogramMessage = m;
    count(sts.test("Body should start with ACK", m.reply.contains("ACK")));
  }

  private void handlePlain(Summary summary, PlainMessage m) {
    summary.plainMessage = m;
  }

  private void handle_PdfIcs309Message(Summary summary, PdfIcs309Message m) {
    summary.pdfIcs309Message = m;
    count(sts.test("ICS-309 task number should be #EV", "01 Sep", m.taskNumber));
    count(sts.test("ICS-309 task name should be #EV", "RRI Welfare Message Exercise", m.taskName));
    count(sts.test("ICS-309 operational period should be #EV", "191500-201500 UTC Sep 24", m.operationalPeriod));
    count(sts.testIfPresent("Operator Name should be present", m.operatorName));
    count(sts.testIfPresent("Station ID should be present", m.stationId));

    var activitiesSubjectSet = m.activities.stream().map(a -> a.subject()).collect(Collectors.toSet());

    String welfareSubject = summary.rriWelfareRadiogramMessage == null ? ""
        : summary.rriWelfareRadiogramMessage.subject;
    /*
     * activitiesSubjectSet.contains(subject) should be sufficient, but it is not, because "some people" enter dates in
     * a format that is hard to parse "09 03 24 19:58", thereby messing up the subject
     */
    var welfarePredicate = activitiesSubjectSet.stream().anyMatch(x -> x.contains(welfareSubject));
    count(sts
        .test("ICS-309 activities should contain RRI Welfare Radiogram subject",
            welfareSubject == "" ? false : welfarePredicate));

    String welfareReplySubject = welfareSubject == null ? "" : "Re: " + welfareSubject;
    var welfareReplyPredicate = activitiesSubjectSet.stream().anyMatch(x -> x.contains(welfareReplySubject));
    count(sts
        .test("ICS-309 activities should contain RRI Welfare Radiogram reply subject",
            welfareReplySubject == "" ? false : welfareReplyPredicate));

    String quickSubject = summary.rriQuickWelfareMessage == null ? "" : summary.rriQuickWelfareMessage.subject;
    var quickPredicate = activitiesSubjectSet.stream().anyMatch(x -> x.contains(quickSubject));
    count(sts
        .test("ICS-309 activities should contain RRI Welfare Radiogram subject",
            quickSubject == "" ? false : quickPredicate));

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

  private void handle_RriRadiogramMessage(Summary summary, RRIWelfareRadiogramMessage m) {
    summary.rriWelfareRadiogramMessage = m;
    count(sts.test("Message Precedence should be TEST W", m.header.contains("TEST W")));

    var bodyOneLine = m.body.replaceAll("\n", " ").replaceAll("\r", " ");

    var ev = "EVACUATING TO A FAMILY MEMBER/FRIENDS HOUSE";
    var predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));

    ev = "WILL CONTACT YOU WHEN ABLE";
    predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));
  }

  private void handle_RriQuickWelfareMessage(Summary summary, RRIQuickWelfareMessage m) {
    summary.rriQuickWelfareMessage = m;
    var bodyOneLine = m.text.replaceAll("\n", "").replaceAll("\r", "");
    var ev = "I am safe and well.";
    var predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));

    ev = "All communications are down.";
    predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));
  }

  @Override
  protected void beforeCommonProcessing(String sender, ExportedMessage message) {
    var messageType = message.getMessageType();
    if (messageType == MessageType.RRI_QUICK_WELFARE) {
      sts.setExplanationPrefix("Quick: ");
    } else if (messageType == MessageType.RRI_WELFARE_RADIOGRAM) {
      sts.setExplanationPrefix("Radiogram: ");
    } else if (messageType == MessageType.PDF_ICS_309) {
      sts.setExplanationPrefix("ICS-309: ");
    } else if (messageType == MessageType.ICS_309) {
      sts.setExplanationPrefix("ICS-309: ");
    } else if (messageType == MessageType.PLAIN) {
      sts.setExplanationPrefix("");
    }
  }

  @Override
  protected void endProcessingForSender(String sender) {
    var summary = summaryMap.get(sender);

    if (summary == null) {
      return;
    }

    if (summary.plainMessage != null) {
      summary.explanations.add("Plain messages should NOT have been sent.");
    }

    if (summary.formIcs309Message != null) {
      summary.explanations.add("Standard Template ICS-309 messages should NOT have been sent");
    }

    if (summary.rriReplyWelfareRadiogramMessage != null) {
      summary.explanations.add("RRI Welfare Radiogram Reply messages should NOT have been sent");
    }

    if (summary.rriQuickWelfareMessage == null) {
      summary.explanations.add("No RRI Quick Welfare message received.");
    }

    if (summary.rriWelfareRadiogramMessage == null) {
      summary.explanations.add("No RRI Welfare Radiogram message received.");
    }

    if (summary.pdfIcs309Message == null) {
      summary.explanations.add("No ICS-309 message received.");
    }

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
        var outboundMessage = new OutboundMessage(outboundMessageSender, summary.sender,
            "Feedback on ETO " + cm.getAsString(Key.EXERCISE_DATE) + " exercise", //
            outboundMessageFeedback, null);
        outboundMessageList.add(outboundMessage);
      }
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }

    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(summaryMap.values()), Path.of(outputPathName, "rri-summary.csv"));
  }
}
