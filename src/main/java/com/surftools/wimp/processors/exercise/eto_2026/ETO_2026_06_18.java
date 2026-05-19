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

package com.surftools.wimp.processors.exercise.eto_2026;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-213, Field Day warm-up
 *
 * @author bobt
 *
 */
public class ETO_2026_06_18 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_06_18.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213Message) message;
    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", "ARRL Field Day 2026", m.incidentName));

    var fromPredicate = m.formFrom.toLowerCase().endsWith("ETO Winlink Thursday Participant".toLowerCase());
    count(sts.test("Form From should end with ETO Winlink Thursday Participant", fromPredicate, m.formFrom));

    count(sts.test("Form Subject should be #EV", "ARRL Field Day 2026 Participation", m.formSubject));
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));

    var msg = m.formMessage;
    var msgOk = msg != null;
    if (msgOk) {
      var lines = msg.split("\n");
      if (lines.length >= 4) {
        var clubLine = lines[0];
        var testResult = sts.testStartsWith("Message line #1 should start with #EV", "Club:", clubLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        var clubName = clubLine.substring(clubLine.indexOf(" ") + 1);
        getCounter("Club Name").increment(clubName);

        var participantsLine = lines[1];
        testResult = sts.testStartsWith("Message line #2 should start with #EV", "Participants:", participantsLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        var participantCountString = participantsLine.substring(participantsLine.indexOf(" ") + 1).strip();
        try {
          var participantCount = Integer.parseInt(participantCountString);
          count(sts.test("Message line #2 should end with a number", true));
          getCounter("Participant Count").increment(participantCount);
        } catch (Exception e) {
          count(sts.test("Message line #2 should end with a number", false, participantCountString));
        }

        var locationLine = lines[2];
        testResult = sts.testStartsWith("Message line #3 should start with #EV", "Field Day Location:", locationLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        var locationFields = locationLine.split(":");
        if (locationFields.length >= 2) {
          var location = locationFields[1].strip();
          getCounter("Field Day Location").increment(location);
          count(sts.test("Message line 3 should be parsable", true, locationLine));
        } else {
          count(sts.test("Message line 3 should be parsable", false, locationLine));
        }

        var aresLine = lines[3];
        testResult = sts.testStartsWith("Message line #4 should start with #EV", "ARES Participants:", aresLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        var aresFields = aresLine.split(":");
        if (aresFields.length >= 2) {
          var aresCountString = aresFields[1].strip();
          try {
            var aresCount = Integer.parseInt(aresCountString);
            getCounter("ARES Count").increment(aresCount);
            count(sts.test("Message line #4 should end with a number", true));
          } catch (Exception e) {
            count(sts.test("Message line #4 should end with a number", false, aresCountString));
          }
          count(sts.test("Message line 4 should be parsable", true, aresLine));
        } else {
          count(sts.test("Message line 4 should be parsable", false, aresLine));
        }

      } else {
        msgOk = false;
      }
    }
    count(sts.test("Message text should be correctly formatted", msgOk, m.formMessage));

    count(sts.testIfPresent("Approved by should be present", m.approvedBy));

    var posTitlePredicate = m.position.toLowerCase().endsWith("ETO participant".toLowerCase());
    count(sts.test("Position/Title should be ETO participant", posTitlePredicate, m.position));
  }

}