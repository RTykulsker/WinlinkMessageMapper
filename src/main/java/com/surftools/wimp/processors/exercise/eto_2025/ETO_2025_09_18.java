/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics259Message.CasualtyEntry;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-309, from a TSV/TDF file
 *
 * @author bobt
 *
 */
public class ETO_2025_09_18 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_09_18.class);

  public Map<String, CasualtyEntry> casualtyMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    messageType = MessageType.ICS_309;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    Ics309Message m = (Ics309Message) message;

    count(sts.test("Organization name shoulb be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Task # should be #EV", "091801", m.taskNumber));

    final var formDTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    var dateTimePrepared = LocalDateTime.parse(m.dateTimePrepared, formDTF);
    count(sts.testOnOrAfter("Date/Time Prepared should be on or after #EV", windowOpenDT, dateTimePrepared, formDTF));
    count(
        sts.testOnOrBefore("Date/Time Prepared should be on or before #EV", windowCloseDT, dateTimePrepared, formDTF));

    count(sts.test("Operational Period # should be #EV", "20250918", m.operationalPeriod));
    count(sts.test("Task Name should be #EV", "ETO 09/18/2025 ICS-309 Exercise", m.taskName));

    count(sts.test("Operator Name should be #EV", "Taylor Lane", m.operatorName));
    count(sts.test("Station ID should be #EV", "W0LTD", m.stationId));
    count(sts.test("Page # should be #EV", "1", m.page));

    var expectedActivities = new Ics309Message.Activity[] {
        new Ics309Message.Activity("2025-09-15 09:42", "K0XRD", "W0TLD",
            "ICS 213RR- Request for Animal Bedding: Wood Chips, Hay, Straw"), //
        new Ics309Message.Activity("2025-09-15 11:17", "W0TLD", "K0XRD",
            "ICS-213: Received Bedding Request - Processing and Allocation Underway"), //
        new Ics309Message.Activity("2025-09-15 13:02", "W0TLD", "K0XRD",
            "ICS 213RR- Bedding Delivery ETA: 3pm, Partial Fulfillment Confirmed"), //
        new Ics309Message.Activity("2025-09-15 15:45", "K0XRD", "W0TLD",
            "ICS-213: Request for Veterinary Supplies - Basic Medications and Bandages"), //
        new Ics309Message.Activity("2025-09-15 17:08", "W0TLD", "K0XRD",
            "ICS-213: Vet Supply Request Received - Inventory Check in Progress"), //
        new Ics309Message.Activity("2025-09-15 19:36", "W0TLD", "K0XRD",
            "ICS-213: Veterinary Supplies Delivery ETA: 8pm - Full Fulfillment Confirmed") };

    var activities = m.activities;
    for (var lineNumber = 1; lineNumber <= Ics309Message.getNDisplayActivities(); ++lineNumber) {
      var act = activities.get(lineNumber - 1);
      sts.setExplanationPrefix("(line " + lineNumber + ") ");

      if (lineNumber <= expectedActivities.length) {
        var exp = expectedActivities[lineNumber - 1];
        count(sts.test_2line("Date/Time should be #EV", exp.dateTimeString(), act.dateTimeString()));
        count(sts.test_2line("From should be #EV", exp.from(), act.from()));
        count(sts.test_2line("To should be #EV", exp.to(), act.to()));
        count(sts.test_2line("Subject should be #EV", exp.subject(), act.subject()));
      } else {
        count(sts.testIfEmpty("Date/Time should be empty", act.dateTimeString()));
        count(sts.testIfEmpty("From should be empty", act.from()));
        count(sts.testIfEmpty("To should be empty", act.to()));
        count(sts.testIfEmpty("Subject should be empty", act.subject()));
      }

    }
  }
}