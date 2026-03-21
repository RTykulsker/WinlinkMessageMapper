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
      Name	Address	Notes
      CANADIAN_PROVINCES	ETO-CAN@winlink.org	ETO-CAN Exercise Clearing House
      FEMA_REGION_01	ETO-01@winlink.org	ETO-01 Exercise Clearing House
      FEMA_REGION_02	ETO-02@winlink.org	ETO-02 Exercise Clearing House
      FEMA_REGION_03	ETO-03@winlink.org	ETO-03 Exercise Clearing House
      FEMA_REGION_04	ETO-04@winlink.org	ETO-04 Exercise Clearing House
      FEMA_REGION_05	ETO-05@winlink.org	ETO-05 Exercise Clearing House
      FEMA_REGION_06	ETO-06@winlink.org	ETO-06 Exercise Clearing House
      FEMA_REGION_07	ETO-07@winlink.org	ETO-07 Exercise Clearing House
      FEMA_REGION_08	ETO-08@winlink.org	ETO-08 Exercise Clearing House
      FEMA_REGION_09	ETO-09@winlink.org	ETO-09 Exercise Clearing House
      FEMA_REGION_10	ETO-10@winlink.org	ETO-10 Exercise Clearing House
      OTHER_COUNTRIES	ETO-DX@winlink.org	ETO-DX Exercise Clearing House
      PRACTICE_EX_TO_ENTRY	ETO-PRACTICE@winlink.org	To Field for Practice
      TRAINING_EX_CC_ENTRY	ETO-BK@winlink.org	ETO-BK CC Field for Training
                                          """;

  private static List<String> refLines;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213;
    doStsFieldValidation = false;
    var extraOutboundMessageText = """

        -----------------------------------------------------------------------------

        ETO is working with several other teams to raise awareness about flooding and tsunami risks
        as part of a joint Tsunami 2026 Exercise!

        This exercise is currently running through March 30 for Full and authoritative instructions
        can be found at https://www.laxnortheast.org/exercises/tsunami.

        ETO-specific guidance can be found at https://emcomm-training.org/Non-ETO_Exercises.html

        """;
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

    var nAttachments = exportedMessage.attachments.size();
    count(sts.test("Number of attachments should be #EV", String.valueOf(3), String.valueOf(nAttachments)));
    if (nAttachments == 3) {
      var standardAttachmentNames = List.of("RMS_Express_Form_ICS213_Initial_Viewer.xml", "FormData.txt");
      for (var attachmentKey : exportedMessage.attachments.keySet()) {
        if (standardAttachmentNames.contains(attachmentKey)) {
          continue;
        }
        var attachmentContent = new String(exportedMessage.attachments.get(attachmentKey));
        var attachmentLines = Arrays.asList(attachmentContent.split("\n"));
        count(sts
            .test("Attachments lines should be #EV", String.valueOf(refLines.size()),
                String.valueOf(attachmentLines.size())));
        for (var i = 0; i < refLines.size(); ++i) {
          var lineNo = i + 1;
          var refLine = refLines.get(i);
          if (attachmentLines.size() > i) {
            var attachLine = attachmentLines.get(i);
            count(sts.test_2line("Attachment line: " + lineNo + " should be #EV", refLine, attachLine));
          } else {
            count(sts.test("Attachment line" + lineNo + " should be #EV", refLine, "(empty"));
          }
        }
      }
    }
  }

  @Override
  public void postProcess() {
    super.postProcess();
  }
}