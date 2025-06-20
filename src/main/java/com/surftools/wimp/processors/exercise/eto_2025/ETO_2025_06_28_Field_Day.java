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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-213, first of two parts
 *
 * @author bobt
 *
 */
public class ETO_2025_06_28_Field_Day extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_06_28_Field_Day.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213Message) message;
    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Incident Name should be #EV", "ARRL Field Day 2025", m.incidentName));

    var partText = "Winlink Thursday Participant";
    var etoPartText = " / ETO " + partText;
    var expectedFrom = m.from + etoPartText;
    var fromPredicate = sts.compareWords(expectedFrom, m.formFrom);
    count(sts.test("Form From should match <YOURCALL>" + etoPartText, fromPredicate, m.formFrom));

    count(sts.test("Form Subject should be #EV", "ARRL Field Day 2025 Participation", m.formSubject));
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));

    var msg = m.formMessage;
    var msgOk = msg != null;
    if (msgOk) {
      var lines = msg.split("\n");
      if (lines.length == 4) {
        var participantLine = lines[1];
        var aresLine = lines[3];
        try {
          Integer.parseInt(participantLine);
          Integer.parseInt(aresLine);
        } catch (Exception e) {
          msgOk = false;
        }
      } else {
        msgOk = false;
      }
    }
    count(sts.test("Message text should be correctly formatted", msgOk, m.formMessage));

    count(sts.testIfPresent("Approved by should be present", m.approvedBy));
    count(sts.test("Position/Title should be #EV", partText, m.position));
  }

}