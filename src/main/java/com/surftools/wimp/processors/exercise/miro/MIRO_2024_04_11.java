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

package com.surftools.wimp.processors.exercise.miro;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.message.Ics309Message.Activity;
import com.surftools.wimp.processors.std.baseExercise.FeedbackProcessor;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-04-10 Exercise: ICS-309 from WLE-generated CSV
 *
 *
 * @author bobt
 *
 */
public class MIRO_2024_04_11 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(MIRO_2024_04_11.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Ics309Message.setNDisplayActivities(8);
    doStsFieldValidation = false;
    messageTypesRequiringSecondaryAddress = Set.of(MessageType.MIRO_CHECK_IN);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics309Message) message;
    getCounter("versions").increment(m.version);

    if (dumpIds.contains(m.messageId) || dumpIds.contains(m.from)) {
      logger.info("### call: " + m.from + "\n" + sts.toString());
    }

    sts.test("Agency/Group name should be #EV", "MIRO Exercise", m.organization);
    sts.test("Task # should be #EV", "240411", m.taskNumber);

    try {
      sts.test("Date/Time Prepared properly formatted", true);
      sts
          .testOnOrAfter("Date/Time Prepared should be on or after #EV", windowOpenDT,
              LocalDateTime.from(DTF.parse(m.dateTimePrepared)), DTF);

      sts
          .testOnOrBefore("Date/Time Prepared should be on or before #EV", windowCloseDT,
              LocalDateTime.from(DTF.parse(m.dateTimePrepared)), DTF);
    } catch (Exception e) {
      sts.test("Date/Time Prepared properly formatted", false);
    }

    sts.test("Operational Period should be #EV", "Today", m.operationalPeriod);
    sts.test("Task Name should be #EV", "ICS 309 Form Via Winlink Express Generated CSV", m.taskName);
    sts.testIfPresent("Operator Name should be provided", m.operatorName);
    sts.test("Station Id should be #EV", m.from, m.stationId);
    sts.test("Page # should be #EV", "1", m.page);

    // push into function
    validateActivities(sender, sts, m.activities, windowOpenDT, windowCloseDT);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  /**
   * @param sender
   * @param sts
   * @param activities
   * @param windowOpenDT
   * @param windowCloseDT
   */
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

    if (windowOpenDT == null) {
      logger.warn("null windowOpenDT");
      return;
    }

    if (windowCloseDT == null) {
      logger.warn("null windowCloseDT");
      return;
    }

    // per exercise instructions:
    // https://emcomm-training.org/Create%20ICS%20309%20Form%20Via%20WLE%20Generated%20CSV%20From%20WLE%20Folder%20Content%20Rev.%201.01.pdf
    // 1. The three sent simulated traffic messages should have the subject as specified.
    // 2. There should be three simulated traffic messages sent. Each addressed to TEST.
    // 3. There should be three received simulated traffic messages, each from SERVICE.
    // 4. Dates should be in yyyy-mm-dd format and the values should be within the exercise submission window.
    // 5. Message line items are in Ascending Chronological order (latest entries last).

    var testMessage1Count = 0;
    var testMessage2Count = 0;
    var testMessage3Count = 0;
    var serviceMessageCount = 0;

    var lastDT = LocalDateTime.of(2024, 4, 1, 0, 0, 0);
    LocalDateTime dt = null;
    var lineNumber = 0;
    for (var a : activities) {
      ++lineNumber;

      if (a == null || !a.isValid()) {
        continue;
      }

      try {
        dt = parse(a.dateTimeString().trim());
        sts.test("Should have valid activity Date/Time" + lineNumber, true);

        if (lineNumber > 1) {
          sts.testOnOrAfter("Should be ascending Date/Time on line: " + lineNumber, lastDT, dt, DTF);
        }

        sts
            .testOnOrAfter("Activity Date/Time should be on or after #EV", windowOpenDT, dt, DTF,
                ", not " + DTF.format(dt) + ", on line: " + lineNumber);

        sts
            .testOnOrBefore("Activity Date/Time should be on or before #EV", windowCloseDT, dt, DTF,
                ", not " + DTF.format(dt) + ", on line: " + lineNumber);

      } catch (Exception e) {
        sts.test("Should have valid activity Date/Time", false, "'" + a.dateTimeString() + "', on line: " + lineNumber);
      }

      if (dt != null) {
        lastDT = dt;
      }

      final var testSubject = "MIRO Exercise, Winlink Simulated Emergency Message ";
      var isTestMessage1 = !isNull(a.from()) && a.from().equals(sender) //
          && !isNull(a.to()) && a.to().equals("TEST") //
          && !isNull(a.subject()) && sts.compareWords(a.subject(), testSubject + "One");
      var isTestMessage2 = !isNull(a.from()) && a.from().equals(sender) //
          && !isNull(a.to()) && a.to().equals("TEST") //
          && !isNull(a.subject()) && sts.compareWords(a.subject(), testSubject + "Two");
      var isTestMessage3 = !isNull(a.from()) && a.from().equals(sender) //
          && !isNull(a.to()) && a.to().equals("TEST") //
          && !isNull(a.subject()) && sts.compareWords(a.subject(), testSubject + "Three");

      var isTestMessage = isTestMessage1 || isTestMessage2 || isTestMessage3;
      var isServiceMessage = !isNull(a.from()) && a.from().equals("SERVICE") //
          && !isNull(a.to()) && a.to().equals(sender) //
          && !isNull(a.subject()) && a.subject().equals("Test Message");

      sts
          .test("Should have a TEST or SERVICE message", isTestMessage || isServiceMessage,
              a.toString() + ", on line: " + lineNumber);

      testMessage1Count += isTestMessage1 ? 1 : 0;
      testMessage2Count += isTestMessage2 ? 1 : 0;
      testMessage3Count += isTestMessage3 ? 1 : 0;
      serviceMessageCount += isServiceMessage ? 1 : 0;

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
