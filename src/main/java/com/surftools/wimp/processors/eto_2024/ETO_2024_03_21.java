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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.message.Ics309Message.Activity;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-03-21 Exercise: ICS-309 from WLE-generated CSV
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_03_21 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_03_21.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Ics309Message.setNDisplayActivities(8);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics309Message) message;
    getCounter("versions").increment(m.version);

    sts.test("Agency/Group name should be #EV", "EmComm Training Organization", m.organization);
    sts.test("Task # should be #EV", "240321", m.taskNumber);
    sts
        .testOnOrAfter("Date/Time Prepared should be on or after #EV", windowOpenDT,
            LocalDateTime.from(DTF.parse(m.dateTimePrepared)), DTF);
    sts
        .testOnOrBefore("Date/Time Prepared should be on or after #EV", windowCloseDT,
            LocalDateTime.from(DTF.parse(m.dateTimePrepared)), DTF);
    sts.test("Operational Periold should be #EV", "03/19-03/22", m.operationalPeriod);
    sts.test("Task Name should be #EV", "ICS 309 Form Via Winlink Express Generated CSV", m.taskName);
    sts.testIfPresent("Operator Name should be provided", m.operatorName);
    sts.test("Station Id should be #EV", sender, m.stationId);
    sts.test("Page # should be #EV", "1", m.page);

    // push into function
    validateActivities(sender, sts, m.activities, windowOpenDT, windowCloseDT);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  public void validateActivities(String sender, SimpleTestService sts, List<Activity> activities,
      LocalDateTime windowOpenDT, LocalDateTime windowCloseDT) {
    if (sender == null) {
      logger.warn("null sender");
      return;
    }

    if (sts == null) {
      logger.warn("null sts");
      return;
    }

    if (activities == null || activities.size() == 0) {
      logger.warn("null or empty sts");
      return;
    }

    var validActivities = activities.stream().filter(a -> a != null && a.isValid()).collect(Collectors.toList());
    var validctivitiesString = validActivities.stream().map((a) -> Objects.toString(a, null)).toList();
    logger.debug("Activities: \n" + validctivitiesString);

    sts.test("Should have only 6 activities", validActivities.size() == 6, String.valueOf(validActivities.size()));

    var testMessageCount = 0;
    var testMessage1Count = 0;
    var testMessage2Count = 0;
    var testMessage3Count = 0;
    var serviceMessageCount = 0;

    var lastDT = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    LocalDateTime dt = null;
    var lineNumber = 0;
    for (var a : activities) {
      ++lineNumber;

      if (a == null || !a.isValid()) {
        continue;
      }

      if (isNull(a.dateTimeString())) {
        sts.test("Should have non-empty activity Date/Time value", false, "on line: " + lineNumber);
        continue;
      } else {
        sts.test("Should have non-empty activity Date/Time value", true, "on line: " + lineNumber);
      }

      if (isNull(a.from())) {
        sts.test("Should have non-empty activity From value", false, "on line: " + lineNumber);
        continue;
      } else {
        sts.test("Should have non-empty activity From value", true, "on line: " + lineNumber);
      }

      if (isNull(a.from())) {
        sts.test("Should have non-empty activity From value", false, "on line: " + lineNumber);
        continue;
      } else {
        sts.test("Should have non-empty activity From value", true, "on line: " + lineNumber);
      }

      if (isNull(a.to())) {
        sts.test("Should have non-empty activity To value", false, "on line: " + lineNumber);
        continue;
      } else {
        sts.test("Should have non-empty activity To value", true, "on line: " + lineNumber);
      }

      if (isNull(a.subject())) {
        sts.test("Should have non-empty activity Subject value", false, "on line: " + lineNumber);
        continue;
      } else {
        sts.test("Should have non-empty activity Subject value", true, "on line: " + lineNumber);
      }

      try {
        dt = parse(a.dateTimeString().trim());
        sts.test("Should have valid activity Date/Time" + lineNumber, true);

        if (lineNumber > 1) {
          sts.testOnOrAfter("Should be ascending Date/Time on line: " + lineNumber, lastDT, dt, DTF);
        }

      } catch (Exception e) {
        sts.test("Should have valid activity Date/Time", false, "'" + a.dateTimeString() + "', on line: " + lineNumber);
      }

      if (dt != null) {
        lastDT = dt;
      }

      final var testSubject = "ETO Exercise, Winlink Simulated Emergency Message ";
      var isTestMessage1 = !isNull(a.from()) && a.from().equals(sender) //
          && !isNull(a.to()) && a.to().equals("TEST") //
          && !isNull(a.subject()) && a.subject().equalsIgnoreCase(testSubject + "One");
      var isTestMessage2 = !isNull(a.from()) && a.from().equals(sender) //
          && !isNull(a.to()) && a.to().equals("TEST") //
          && !isNull(a.subject()) && a.subject().equalsIgnoreCase(testSubject + "Two");
      var isTestMessage3 = !isNull(a.from()) && a.from().equals(sender) //
          && !isNull(a.to()) && a.to().equals("TEST") //
          && !isNull(a.subject()) && a.subject().equalsIgnoreCase(testSubject + "Three");

      var isTestMessage = isTestMessage1 || isTestMessage2 || isTestMessage3;
      var isServiceMessage = !isNull(a.from()) && a.from().equals("SERVICE") //
          && !isNull(a.to()) && a.to().equals(sender) //
          && !isNull(a.subject()) && a.subject().equals("Test Message");

      sts
          .test("Should have a TEST or SERVICE message", isTestMessage || isServiceMessage,
              a.toString() + ", on line: " + lineNumber);

      if (windowOpenDT != null) {
        sts
            .testOnOrAfter("Activity Date/Time should be on or after #EV", windowOpenDT, dt, DTF,
                ", not " + DTF.format(dt) + ", on line: " + lineNumber);
      }

      if (windowCloseDT != null) {
        sts
            .testOnOrBefore("Activity Date/Time should be on or before #EV", windowCloseDT, dt, DTF,
                ", not " + DTF.format(dt) + ", on line: " + lineNumber);
      }

      testMessageCount += isTestMessage ? 1 : 0;
      testMessage1Count += isTestMessage1 ? 1 : 0;
      testMessage2Count += isTestMessage2 ? 1 : 0;
      testMessage3Count += isTestMessage3 ? 1 : 0;
      serviceMessageCount += isServiceMessage ? 1 : 0;

      if (isServiceMessage) {
        sts
            .test("Should have more TEST messages than SERVICE messages at line: " + lineNumber,
                testMessageCount >= serviceMessageCount);
      }

      if (isTestMessage3) {
        sts
            .test("Should have at least one TEST One or TEST Two message before TEST Three",
                testMessage3Count <= testMessage2Count || testMessage3Count < -testMessage1Count,
                "(" + testMessage1Count + " or " + testMessage2Count + ") on line: " + lineNumber);
      }

      if (isTestMessage2) {
        sts
            .test("Should have at least one TEST One message before TEST Two", testMessage2Count <= testMessage1Count,
                "" + testMessage1Count + ", on line: " + lineNumber);
      }

    } // end loop over lines

    sts
        .test("Should have exactly 3 messages from SERVICE", serviceMessageCount == 3,
            String.valueOf(serviceMessageCount));
    sts
        .test("Should have exactly 1 message to TEST with Subject ... One", testMessage1Count == 1,
            String.valueOf(testMessage1Count));
    sts
        .test("Should have exactly 1 message to TEST with Subject ... Two", testMessage2Count == 1,
            String.valueOf(testMessage2Count));
    sts
        .test("Should have exactly 1 message to TEST with Subject ... Three", testMessage3Count == 1,
            String.valueOf(testMessage3Count));
  }

}
