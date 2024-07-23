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

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-06-24: an ICS-213 from Field Day and an ICS-214 to describe it
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_06_24 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_06_24.class);

  private class Summary implements IWritableTable {
    public String sender;
    public LatLongPair location;

    public int feedbackCount;
    public String feedback;

    public int ics213Count; // number of ICS-213 messages received (0 or 1)
    public int ics214Count; // number of ICS-214 messages received (0 or 1)
    public int ics214ACount; // number of ICS-214A messages received (0 or 1)

    @SuppressWarnings("unused")
    public Ics213Message ics213Message;

    public Ics214Message ics214Message;

    @SuppressWarnings("unused")
    public Ics214Message ics214AMessage;

    public List<PlainMessage> plainMessages = new ArrayList<>();

    public Summary() {
      feedback = "";
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Summary) other;
      return sender.compareTo(o.sender);
    }

    @Override
    public String[] getHeaders() {

      var list = new ArrayList<String>(List
          .of("From", "Latitude", "Longitude", "Feedback Count", "Feedback", //
              "Ics213 Count", "Ics214 Count", "Ics214A Count", "Required Count", //
              "Plain Count", "Resource Count", "Activity Count"));

      return list.toArray(new String[list.size()]);
    }

    @Override
    public String[] getValues() {
      var latitude = (location != null && location.isValid()) ? location.getLatitude() : "0.0";
      var longitude = (location != null && location.isValid()) ? location.getLongitude() : "0.0";

      var assignedResourcesCount = ics214Message == null ? 0 : 1 + ics214Message.assignedResources.size();
      var activitesCount = ics214Message == null ? 0 : ics214Message.activities.size();

      var list = new ArrayList<String>(List
          .of(sender, latitude, longitude, String.valueOf(feedbackCount), feedback.trim(), //
              String.valueOf(ics213Count), String.valueOf(ics214Count), String.valueOf(ics214ACount),
              String.valueOf(ics213Count + ics214Count), //
              String.valueOf(plainMessages.size()), String.valueOf(assignedResourcesCount),
              String.valueOf(activitesCount)));

      return list.toArray(new String[list.size()]);
    }

    public void aggregate(Ics213Message m, SimpleTestService sts) {

      // last wins
      sender = m.from;
      location = m.mapLocation;

      ++ics213Count;
      ics213Message = m;

      if (sts.getExplanations().size() > 0) {
        feedbackCount += sts.getExplanations().size();
        feedback += "\n" + String.join("\n", sts.getExplanations());
      }

    }

    public void aggregate(Ics214Message m, SimpleTestService sts) {
      // last wins
      sender = m.from;
      location = m.mapLocation;

      if (m.getMessageType() == MessageType.ICS_214) {
        ++ics214Count;
        ics214Message = m;
      } else {
        ++ics214ACount;
        ics214AMessage = m;
      }

      if (sts.getExplanations().size() > 0) {
        feedbackCount += sts.getExplanations().size();
        feedback += "\n" + String.join("\n", sts.getExplanations());
      }

      if (sts.getExplanations().size() > 0) {
        feedbackCount += sts.getExplanations().size();
        feedback += "\n" + String.join("\n", sts.getExplanations());
      }
    }

    public void aggregate(PlainMessage m, SimpleTestService sts) {
      // last wins
      sender = m.from;
      location = m.mapLocation;

      plainMessages.add(m);

      if (sts.getExplanations().size() > 0) {
        feedbackCount += sts.getExplanations().size();
        feedback += "\n" + String.join("\n", sts.getExplanations());
      }

      if (sts.getExplanations().size() > 0) {
        feedbackCount += sts.getExplanations().size();
        feedback += "\n" + String.join("\n", sts.getExplanations());
      }
    }
  }

  private Map<String, Summary> summaryMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    doStsFieldValidation = true;

    messageTypesRequiringSecondaryAddress = Set.of(MessageType.ICS_213, MessageType.ICS_214);

    // different exercise windows
    setWindowsForType(MessageType.ICS_213, LocalDateTime.of(2024, 6, 22, 18, 0, 0),
        LocalDateTime.of(2024, 6, 23, 20, 59, 59));
    setWindowsForType(MessageType.ICS_214, LocalDateTime.of(2024, 6, 25, 18, 0, 0),
        LocalDateTime.of(2024, 6, 28, 7, 59, 0));

    Ics214Message.setNDisplayAdditionalResouces(0);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var type = message.getMessageType();
    if (type == MessageType.PLAIN) {
      handlePlain((PlainMessage) message);
    } else if (type == MessageType.ICS_213) {
      handleIcs213((Ics213Message) message);
    } else if (type == MessageType.ICS_214) {
      handleIcs214((Ics214Message) message);
    } else if (type == MessageType.ICS_214A) {
      handleIcs214((Ics214Message) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }
  }

  private void handlePlain(PlainMessage m) {
    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(m, sts);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  protected void handleIcs213(Ics213Message m) {
    outboundMessageSubject = "Feedback on your Field Day ICS-213 message, mId: ";
    sts.setExplanationPrefix("");

    if (dumpIds.contains(m.messageId) || dumpIds.contains(m.from)) {
      logger.info("### call: " + m.from + "\n" + sts.toString());
    }

    // box 0: meta
    getCounter("ICS_213 versions").increment(m.version);
    sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization);

    // box 1: incident name
    sts.test("Box 1 Incident Name should be #EV", "ARRL Field Day 2024", m.incidentName);

    // box 2: to

    // var source = m.source;
    // var sender = m.from;
    // var sourceNotSender = !source.equalsIgnoreCase(sender);
    // if (sourceNotSender) {
    // System.err.println("source: " + source + ", sender: " + sender);
    // }

    // box 3: from
    // var box3Predicate = sts
    // .toAlphaNumericWords(m.from + " / ETO Winlink Thursday Participant")
    // .equalsIgnoreCase(sts.toAlphaNumericWords(m.formFrom));
    // sts.test("Box 3 From should be <YOURCALL> / ETO Winlink Thursday Participant", box3Predicate, m.formFrom);

    sts
        .test("Box 3 From should contain ETO Winlink Thursday Participant",
            m.formFrom.toUpperCase().contains("ETO Winlink Thursday Participant".toUpperCase()), m.formFrom);

    // box 4: subject
    sts.test("Box 4 Subject should be #EV", "ARRL Field Day 2024 Participation", m.formSubject);

    // box 5: date
    // box 6: time

    // box 7: message
    var feedback = evaluateMessage(m.formMessage);
    sts.test("Box 7 Message should be properly formatted", feedback == null, feedback);

    // box 8: approved by
    sts.test("Box 8 Approved by should contain callsign", m.approvedBy.contains(m.from), m.approvedBy);

    // box 8b: Position/ Title
    sts.test("Box 8b should be #EV", "Winlink Thursday Participant", m.position);

    // form location
    sts.test("Form location should be specified", m.formLocation.isValid(), m.formLocation.toString());

    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(m, sts);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);

    getCounter("Feedback Count").increment(sts.getExplanations().size());
    getCounter("Clearinghouse Count").increment(m.to);
  }

  private String evaluateMessage(String message) {
    var details = new ArrayList<String>();

    if (message == null) {
      return "null message";
    }

    var lines = message.split("\n");
    if (lines.length != 4) {
      details.add("number of lines should be 4");
    }

    if (lines.length >= 2) {
      var line2 = lines[1];
      try {
        Integer.valueOf(line2);
      } catch (Exception e) {
        details.add("line 2 should be a number");
      }
    }

    if (lines.length >= 4) {
      var line4 = lines[3];
      try {
        Integer.valueOf(line4);
      } catch (Exception e) {
        details.add("line 4 should be a number");
      }
    }

    return details.size() > 0 ? String.join(",", details) : null;
  }

  protected void handleIcs214(Ics214Message m) {
    outboundMessageSubject = "Feedback on your Field Day ICS-214 message, mId: ";
    sts.setExplanationPrefix("");

    if (dumpIds.contains(m.messageId) || dumpIds.contains(m.from)) {
      logger.info("### call: " + m.from + "\n" + sts.toString());
    }

    getCounter("ICS-214 versions").increment(m.version);

    sts.test("Agency/Group name should be #EV", "EmComm Training Organization", m.organization);
    var messageTypePredicate = m.getMessageType() == MessageType.ICS_214;
    sts.test("Form ICS-214 should be used", messageTypePredicate);

    // box 1: incident name and page
    sts.test("Box 1 Incident Name should be #EV", "ARRL Field Day 2024", m.incidentName);
    sts.test("Page # should be #EV", "1", m.page);

    // box 2: operational period
    // final var OP_DTF = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm");
    // sts.testIsDateTime("Box 2 Date Time From should be a valid date/time", (Object) m.opFrom, OP_DTF);
    // sts.testIsDateTime("Box 2 Date Time To should be a valid date/time", (Object) m.opTo, OP_DTF);

    // box 3: name
    sts.test("Box 3 Name should end with callsign", m.selfResource.name().endsWith(m.from), m.selfResource.name());

    // box 4: ICS position
    sts.test("Box 4 ICS Position should be #EV", "ETO Winlink Thursday Participant", m.selfResource.icsPosition());

    // box 5: Home Agency
    // sts.test(outboundMessageSender, doOutboundMessaging)

    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(m, sts);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
    getCounter("Feedback Count").increment(sts.getExplanations().size());
    getCounter("Clearinghouse Count").increment(m.to);
  }

  @Override
  public void postProcess() {
    super.postProcess();

    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(summaryMap.values()),
            Path.of(outputPathName, "aggregate-summary.csv"));
  }

}
