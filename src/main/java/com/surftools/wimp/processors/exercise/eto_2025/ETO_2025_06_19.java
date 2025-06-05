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

package com.surftools.wimp.processors.exercise.eto_2025;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.MultiDateTimeParser;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics205RadioPlanMessage;
import com.surftools.wimp.message.Ics205RadioPlanMessage.RadioEntry;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-205
 *
 * @author bobt
 *
 */
public class ETO_2025_06_19 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_06_19.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    messageType = MessageType.ICS_205_RADIO_PLAN;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    Ics205RadioPlanMessage m = (Ics205RadioPlanMessage) message;

    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Incident Name should be #EV", "ETO ICS-205 Exercise", m.incidentName));

    var dateTimePrepared = parseDateTime(m.dateTimePrepared, List.of(DTF, ALT_DTF));
    if (dateTimePrepared == null) {
      count(sts.test("Date/Time Prepared should be present and in exercise window", false));
    } else {
      count(sts.test("Date/Time Prepared should be present and in exercise window", true));
      count(sts.testOnOrAfter("Date/Time Prepared should be on or after #EV", windowOpenDT, dateTimePrepared, DTF));
      count(sts.testOnOrBefore("Date/Time Prepared should be on or before #EV", windowCloseDT, dateTimePrepared, DTF));
    }

    var opDateFormatter = new MultiDateTimeParser(List
        .of("M/dd/yyyy", "MM/dd/yy", "MM/dd/yyyy", "yyyy-MM-dd", "yyyy-M-dd", "M-dd-yyyy", "yyyy/MM/dd", "M/dd/yy"));

    count(sts
        .test("Operational Period Date From should be 06/17/25",
            opDateFormatter.parseDate(m.dateFrom).equals(LocalDate.of(2025, 06, 17)), m.dateFrom));
    count(sts
        .test("Operational Period Date To should be 06/20/25",
            opDateFormatter.parseDate(m.dateTo).equals(LocalDate.of(2025, 06, 20)), m.dateTo));

    var opTimeFormatter = new MultiDateTimeParser(List.of("HH:mm 'UTC'", "HHmm 'UTC'", "HHmm", "HH:mm", "HH:mm'UTC'"));
    count(sts
        .test("Operational Period Time From should be 00:00 UTC",
            opTimeFormatter.parseTime(m.timeFrom).equals(LocalTime.of(0, 0)), m.timeFrom));
    count(sts
        .test("Operational Period Time To should be 15:00 UTC",
            opTimeFormatter.parseTime(m.timeTo).equals(LocalTime.of(15, 00)), m.timeTo));

    handleRadioEntries(m, m.radioEntries);
    sts.setExplanationPrefix("");

    count(sts.test("Special Instructions be #EV", "Winlink address: ETO-DRILL@winlink.org", m.specialInstructions));

    var approvedBy = m.approvedBy == null ? "" : m.approvedBy;
    var approvedByFields = Arrays
        .asList(approvedBy.split(",| ")) // comma OR space
          .stream()
          .filter(Predicate.not(String::isEmpty))
          .toList();
    var nApprovedByFields = approvedByFields.size();
    var areThere2Fields = approvedByFields.size() == 2;
    count(
        sts.test("Approved By should have two fields", areThere2Fields, String.valueOf(nApprovedByFields), approvedBy));
    if (areThere2Fields) {
      count(sts.test("Approved By Field #2 should match call sign", approvedByFields.get(1).equalsIgnoreCase(m.from)));
    } else {
      count(sts.test("Approved By Field #2 should match call sign", false));
    }

    var dateTimeApproved = parseDateTime(m.dateTimePrepared, List.of(DTF, ALT_DTF));
    if (dateTimeApproved == null) {
      count(sts.test("Date/Time Approved should be present and in exercise window", false));
    } else {
      count(sts.testOnOrAfter("Date/Time Approved should be on or after #EV", windowOpenDT, dateTimeApproved, DTF));
      count(sts.testOnOrBefore("Date/Time Approved should be on or before #EV", windowCloseDT, dateTimeApproved, DTF));
    }

    count(sts.test("IAP Page should be #EV", "5", m.iapPage));
  }

  private void handleRadioEntries(Ics205RadioPlanMessage m, List<RadioEntry> allEntries) {
    var entries = allEntries.stream().filter(s -> !s.isEmpty()).toList();
    count(sts.test("Number of Radio Channels defined should be #EV", "2", String.valueOf(entries.size())));

    {
      var entry = entries.get(0);
      sts.setExplanationPrefix("line 1: (VHF Repeater): ");
      count(sts.testIfEmpty("Zone/Group should be empty", entry.zoneGroup()));
      count(sts.test("Channel number should be #EV", "1", entry.channelNumber()));
      count(sts.test("Function should be #EV", "Coordination", entry.function()));
      count(sts.test("Channel name should be #EV", "VHF Repeater", entry.channelName()));
      count(sts.test("Assignment should be #EV", "amateur", entry.assignment()));
      count(sts.testAsDouble("RX Freq should be #EV", "147.160", entry.rxFrequency()));
      count(sts.test("RX N/W should be #EV", "W", entry.rxNarrowWide()));
      count(sts.testAsDouble("RX Tone should be #EV", "103.5", entry.rxTone()));
      count(sts.testAsDouble("TX Freq should be #EV", "147.760", entry.txFrequency()));
      count(sts.test("TX N/W should be #EV", "W", entry.txNarrowWide()));
      count(sts.testAsDouble("TX Tone should be #EV", "103.5", entry.txTone()));
      count(sts.test("Remarks should be #EV", "Primary repeater", entry.remarks()));
    }

    {
      var entry = entries.get(1);
      sts.setExplanationPrefix("line 2: (VHF Simplex): ");
      count(sts.testIfEmpty("Zone/Group should be empty", entry.zoneGroup()));
      count(sts.test("Channel number should be #EV", "2", entry.channelNumber()));
      count(sts.test("Function should be #EV", "Tactical;", entry.function()));
      count(sts.test("Channel name should be #EV", "VHF Simplex", entry.channelName()));
      count(sts.test("Assignment should be #EV", "amateur", entry.assignment()));
      count(sts.testAsDouble("RX Freq should be #EV", "147.440", entry.rxFrequency()));
      count(sts.test("RX N/W should be #EV", "W", entry.rxNarrowWide()));
      count(sts.test("RX Tone should be #EV", "CSQ", entry.rxTone()));
      count(sts.testAsDouble("TX Freq should be #EV", "147.440", entry.txFrequency()));
      count(sts.test("TX N/W should be #EV", "W", entry.txNarrowWide()));
      count(sts.test("TX Tone should be #EV", "CSQ", entry.txTone()));
      count(sts.test("Remarks should be #EV", "Primary simplex", entry.remarks()));
    }

  }

}