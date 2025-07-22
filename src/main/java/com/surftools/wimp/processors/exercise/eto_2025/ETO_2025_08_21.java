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

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Hics259Message.CasualtyEntry;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * HICS 259 Hospital Casualty Report
 *
 * @author bobt
 *
 */
public class ETO_2025_08_21 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_08_21.class);

  public Map<String, CasualtyEntry> casualtyMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    messageType = MessageType.HICS_259;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

    casualtyMap = Map
        .of( //
            Hics259Message.CASUALTY_KEYS.get(0),
            new CasualtyEntry("32", "9",
                "Emergency Room is at capacity. Less critical patients are being placed in the hallways."), //
            Hics259Message.CASUALTY_KEYS.get(1), new CasualtyEntry("14", "4", ""), //
            Hics259Message.CASUALTY_KEYS.get(2), new CasualtyEntry("22", "2", ""), //
            Hics259Message.CASUALTY_KEYS.get(3), new CasualtyEntry("3", "2", ""), //
            Hics259Message.CASUALTY_KEYS.get(4), new CasualtyEntry("0", "0", ""), //
            Hics259Message.CASUALTY_KEYS.get(5), new CasualtyEntry("0", "0", ""), //
            Hics259Message.CASUALTY_KEYS.get(6), new CasualtyEntry("7", "1", ""), //
            Hics259Message.CASUALTY_KEYS.get(7),
            new CasualtyEntry("3", "0", "Adults to University Medical Center. More transfers are needed."), //
            Hics259Message.CASUALTY_KEYS.get(8), new CasualtyEntry("0", "0", "") //
        );
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    Hics259Message m = (Hics259Message) message;

    count(sts.test("Incident Names should be #EV", "HEATWAVE ADAM", m.incidentName));

    final DateTimeFormatter DATE_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final DateTimeFormatter TIME_DTF = DateTimeFormatter.ofPattern("HH:mm");
    count(sts.test("Form Date should be #EV", "2025-08-21", DATE_DTF.format(m.formDateTime)));
    count(sts.test("Form Time should be #EV", "13:48", TIME_DTF.format(m.formDateTime)));
    count(sts.test("Operational Period # should be #EV", "1", m.operationalPeriod));
    count(sts.test("Operational Date From Should be #EV", "2025-08-21", DATE_DTF.format(m.opFrom)));
    count(sts.test("Operational Date To Should be #EV", "2025-08-21", DATE_DTF.format(m.opTo)));
    count(sts.test("Operational Time From Should be #EV", "00:00", TIME_DTF.format(m.opFrom)));
    count(sts.test("Operational Time To Should be #EV", "23:59", TIME_DTF.format(m.opTo)));

    var lineNumber = 0;
    for (var key : Hics259Message.CASUALTY_KEYS) {
      ++lineNumber;
      var exp = casualtyMap.get(key);
      var act = m.casualtyMap.get(key);
      count(sts.test("Line " + lineNumber + ": Adult " + key + " should be #EV", exp.adultCount(), act.adultCount()));
      count(sts.test("Line " + lineNumber + ": Child " + key + " should be #EV", exp.childCount(), act.childCount()));

      if (!exp.comment().isEmpty()) {
        count(sts
            .test_2line("Line " + lineNumber + ": " + key + " comments should be #EV", exp.comment(), act.comment()));
      } else {
        count(sts.testIfEmpty("Line " + lineNumber + ": " + key + " comments should be #EV", act.comment()));
      }
    }
    count(sts.test("Prepared by should be #EV", "Gloria Samstone", m.patientTrackingManager));
    count(sts.test("Facility should be #EV", "Smith County Hospital", m.facilityName));
  }
}