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

package com.surftools.wimp.processors.exercise.eto_2026;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Hics259Message.CasualtyEntry;
import com.surftools.wimp.message.Hics259Message.CasualtyType;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * HICS 259 Hospital Casualty Report
 *
 * see: https://docs.google.com/document/d/1P5llM757bcsw5nF7_uPHyPjLVeq5ejoZ8-GPq3Cvbhs/edit?tab=t.0
 *
 * @author bobt
 *
 */
public class ETO_2026_07_16 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_07_16.class);

  public Map<CasualtyType, CasualtyEntry> casualtyMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    messageType = MessageType.HICS_259;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

    casualtyMap = Map
        .of( //
            Hics259Message.CasualtyType.PATIENTS_SEEN,
            new CasualtyEntry("35", "11",
                "Emergency Room is at capacity. Less critical patients are being placed in the hallways."), //
            Hics259Message.CasualtyType.WAITING_TO_BE_SEEN, new CasualtyEntry("12", "2", ""), //
            Hics259Message.CasualtyType.ADMITTED, new CasualtyEntry("19", "2", ""), //
            Hics259Message.CasualtyType.CRITICAL_CARE_BED, new CasualtyEntry("18", "0", ""), //
            Hics259Message.CasualtyType.MEDICAL_SURGICAL_BED, new CasualtyEntry("4", "0", ""), //
            Hics259Message.CasualtyType.PEDIATRIC_BED, new CasualtyEntry("0", "2", ""), //
            Hics259Message.CasualtyType.DISCHARGED, new CasualtyEntry("10", "9", ""), //
            Hics259Message.CasualtyType.TRANSFERRED,
            new CasualtyEntry("3", "0", "Adults to University Medical Center. More transfers are needed."), //
            Hics259Message.CasualtyType.EXPIRED, new CasualtyEntry("3", "0", "") //
        );
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    Hics259Message m = (Hics259Message) message;

    count(sts.test_2line("Incident Name should be #EV", "Medical Response and Surge Exercise", m.incidentName));

    // rely on gateways to filter to window
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));
    count(sts.testIfPresent("Operational Date From should be present", m.opFromDate));
    count(sts.testIfPresent("Operational Date To should be present", m.opToDate));
    count(sts.testIfPresent("Operational Time From should be present", m.opFromTime));
    count(sts.testIfPresent("Operational Time To should be present", m.opToTime));

    var lineNumber = 0;
    for (var type : Hics259Message.CasualtyType.values()) {
      ++lineNumber;
      var exp = casualtyMap.get(type);
      var key = type.toString();
      var act = m.casualtyMap.get(key);
      count(sts.test("Line " + lineNumber + ": Adult " + key + " should be #EV", exp.adultCount(), act.adultCount()));
      count(sts.test("Line " + lineNumber + ": Child " + key + " should be #EV", exp.childCount(), act.childCount()));

      if (!exp.comment().isEmpty()) {
        count(sts
            .test_2line("Line " + lineNumber + ": " + key + " comments should be #EV", exp.comment(), act.comment()));
      } else {
        count(sts.testIfEmpty("Line " + lineNumber + ": " + key + " comments should be empty", act.comment()));
      }
    }

    count(sts.testStartsWith("Prepared by should start with #EV", "Julia Winter", m.patientTrackingManager));
    count(sts.test_2line("Facility should be #EV", "Jackson Memorial Hospital", m.facilityName));
  }
}