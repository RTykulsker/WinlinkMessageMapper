/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

package com.surftools.wimp.processors.exercise.eto_2026;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * an ICS-213 with message body representing an exported Contacts file
 *
 * @author bobt
 *
 */
public class ETO_2026_03_19 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_03_19.class);

  private static final String referenceMessage = """
      Name,Address,Notes,
      FEMA REGION 01,ETO-01,ETO-01 Exercise Clearing House,
      FEMA_REGION 02,ETO-02,ETO-02 Exercise Clearing House,
      FEMA_REGION 03,ETO-03,ETO-03 Exercise Clearing House,
      FEMA_REGION 04,ETO-04,ETO-04 Exercise Clearing House,
      FEMA_REGION 05,ETO-05,ETO-05 Exercise Clearing House,
      FEMA_REGION 06,ETO-06,ETO-06 Exercise Clearing House,
      FEMA_REGION 07,ETO-07,ETO-07 Exercise Clearing House,
      FEMA_REGION 08,ETO-08,ETO-08 Exercise Clearing House,
      FEMA_REGION 09,ETO-09,ETO-09 Exercise Clearing House,
      FEMA_REGION 10,ETO-10,ETO-10 Exercise Clearing House,
      CANADIAN_PROVINCES,ETO-CAN,ETO-CAN Exercise Clearing House,
      OTHER COUNTRIES,ETO-DX,ETO-DX Exercise Clearing House,
      Practice Ex To Entry,ETO-PRACTICE,To Field for Practice,
      Training Ex To Entry,ETO-BK,ETO-BK CC Field for Training,
                              """;

  private static List<String> refLines;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

    refLines = Arrays.asList(referenceMessage.split("\n"));
  }

  @Override
  protected void specificProcessing(ExportedMessage exportedMessage) {
    var m = (Ics213Message) exportedMessage;

    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", "03/19/2026 Training Exercise", m.incidentName));
    count(sts.test("Form To should be #EV", "EmComm Training Organization", m.formTo));
    count(sts.test("Form From should contain '/ Participant'", m.formFrom.contains("/ Participant")));
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));

    var msgLines = Arrays.asList(m.formMessage.split("\n"));
    for (var i = 0; i < refLines.size(); ++i) {
      var line = (i < msgLines.size()) ? msgLines.get(i) : "";
      count(sts.test_2line("Message line " + (i + 1) + " should be #EV", refLines.get(i), line));
    }
    count(sts.test("Message should not have more lines than instructions", msgLines.size() <= refLines.size()));

    count(sts.testIfPresent("Approved by should be present", m.approvedBy));
    count(sts.test("Position/Title should match call sign", m.position.equals(m.from)));
  }

  @Override
  public void postProcess() {
    super.postProcess();
  }
}