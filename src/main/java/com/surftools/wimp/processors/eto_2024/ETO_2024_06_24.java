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
              "Ics213 Count", "Ics214 Count"));

      return list.toArray(new String[list.size()]);
    }

    @Override
    public String[] getValues() {
      var latitude = (location != null && location.isValid()) ? location.getLatitude() : "0.0";
      var longitude = (location != null && location.isValid()) ? location.getLongitude() : "0.0";

      var list = new ArrayList<String>(List
          .of(sender, latitude, longitude, String.valueOf(feedbackCount), feedback.trim(), //
              String.valueOf(ics213Count), String.valueOf(ics214Count)));

      return list.toArray(new String[list.size()]);
    }

    // TODO if these two aggregators stay the same, make it one function!
    public void aggregate(Ics213Message m, SimpleTestService sts) {

      // last wins
      sender = m.from;
      location = m.mapLocation;

      ++ics213Count;

      if (sts.getExplanations().size() > 0) {
        feedbackCount += sts.getExplanations().size();
        feedback += "\n" + String.join("\n", sts.getExplanations());
      }

    }

    public void aggregate(Ics214Message m, SimpleTestService sts) {
      // last wins
      sender = m.from;
      location = m.mapLocation;

      ++ics213Count;

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
    setWindowsForType(MessageType.ICS_214, LocalDateTime.of(2024, 6, 22, 18, 0, 0),
        LocalDateTime.of(2024, 6, 28, 16, 0, 0));
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var type = message.getMessageType();
    if (type == MessageType.ICS_213) {
      handleIcs213((Ics213Message) message);
    } else if (type == MessageType.ICS_214) {
      handleIcs214((Ics214Message) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }
  }

  protected void handleIcs213(Ics213Message m) {
    outboundMessageSubject = "Feedback on your ETO Spring Drill 2024 Check In message, mId: ";

    // box 0: meta
    getCounter("ICS_213 versions").increment(m.version);
    sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization);

    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(m, sts);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  protected void handleIcs214(Ics214Message m) {
    outboundMessageSubject = "Feedback on your ETO Spring Drill 2024 ICS-309 message, mId: ";

    getCounter("ICS-214 versions").increment(m.version);

    if (dumpIds.contains(m.messageId) || dumpIds.contains(m.from)) {
      logger.info("### call: " + m.from + "\n" + sts.toString());
    }

    sts.test("Agency/Group name should be #EV", "EmComm Training Organization", m.organization);
    sts.test("Page # should be #EV", "1", m.page);

    sts.setExplanationPrefix("");
    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(m, sts);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  @Override
  public void postProcess() {
    super.postProcess();

    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(summaryMap.values()),
            Path.of(outputPathName, "aggregate-summary.csv"));
  }

}
