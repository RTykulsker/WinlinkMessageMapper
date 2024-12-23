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

package com.surftools.wimp.processors.exercise.eto_2022;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.HospitalBedMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Winlink Hospital Bed
 *
 *
 * agency/group name set to ETO Winlink Thursday. 10 points.
 *
 * contact phone specified. 10 points
 *
 * contact email specified. 10 points
 *
 * Medical/Surgical available filled in. 20 points
 *
 * Critical Care available filled in. 20 points (0 value permitted if
 *
 * six other Available fields blank. 10 points if all are blank.
 *
 * Additional Comments set to "This is an exercise" (case sensitive? or case-insensitive?) 20 points
 *
 *
 *
 * @author bobt
 *
 */
public class ETO_2022_06_16 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_06_16.class);

  private FormFieldManager ffm = new FormFieldManager();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    ffm.add("organization", new FormField(FFType.SPECIFIED, "Agency/Group name", "ETO Winlink Thursday", 10));
    ffm.add("phone", new FormField(FFType.SPECIFIED, "Contact Phone", "555-555-5555", 10));
    ffm.add("comments", new FormField(FFType.SPECIFIED, "Additional Comments", "This is an exercise", 20));

  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    int ppCount = 0;
    int ppCountEmailOk = 0;
    int ppCountMedicalBedOk = 0;
    int ppCountCriticalBedOk = 0;
    int ppCountOtherBedOk = 0;

    var scoreCounter = new Counter();
    var gradedResults = new ArrayList<IWritableTable>();
    for (var message : mm.getMessagesForType(MessageType.HOSPITAL_BED)) {
      HospitalBedMessage m = (HospitalBedMessage) message;

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      ffm.test("organization", m.organization);
      ffm.test("phone", m.contactPhone);
      ffm.test("comments", m.additionalComments);

      var contactEmail = m.contactEmail;
      if (contactEmail != null) {
        var atIndex = contactEmail.indexOf("@");
        if (atIndex >= 0) {
          contactEmail = contactEmail.substring(0, atIndex);
        }
        if (contactEmail != null && contactEmail.equalsIgnoreCase(m.from)) {
          ++ppCountEmailOk;
          points += 10;
        } else {
          explanations.add("contactEmail not set to your Winlink address");
        }
      } else {
        explanations.add("contactEmail not provided");
      }

      var explanation = checkBedCountsAndNotes(m.medicalBedCount, m.medicalBedNotes, "medical");
      if (explanation != null) {
        explanations.add(explanation);
      } else {
        points += 20;
        ++ppCountMedicalBedOk;
      }

      explanation = checkBedCountsAndNotes(m.criticalBedCount, m.criticalBedNotes, "critical");
      if (explanation != null) {
        explanations.add(explanation);
      } else {
        points += 20;
        ++ppCountCriticalBedOk;
      }

      var areOtherCountPresent = false;
      var others = new String[] { m.emergencyBedCount, m.pediatricsBedCount, m.psychiatryBedCount, m.burnBedCount,
          m.other1BedCount, m.other2BedCount };
      for (var other : others) {
        if (other != null && other.length() > 0) {
          areOtherCountPresent = true;
          break;
        }
      }
      if (!areOtherCountPresent) {
        ++ppCountOtherBedOk;
        points += 10;
      } else {
        explanations.add("other bed counts specified, contrary to instructions");
      }

      points += ffm.getPoints();
      points = Math.min(100, points);
      var grade = String.valueOf(points);
      scoreCounter.increment(points);
      explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);
      var gradedResult = new GradedResult(m, grade, explanation);
      gradedResults.add(gradedResult);
    }

    var sb = new StringBuilder();
    sb.append("\nETO-2022-06-16 Grading Report: graded " + ppCount + " Hospital Bed messages\n");

    for (var key : ffm.keySet()) {
      var af = ffm.get(key);
      sb.append(formatPP(af.label, af.count, ppCount));
    }

    sb.append(formatPP("contact email", ppCountEmailOk, ppCount));
    sb.append(formatPP("medical/surgical beds", ppCountMedicalBedOk, ppCount));
    sb.append(formatPP("critical car beds", ppCountCriticalBedOk, ppCount));
    sb.append(formatPP("other beds", ppCountOtherBedOk, ppCount));

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());
    writeTable("graded-hospital_bed.csv", gradedResults);
  }

  private String checkBedCountsAndNotes(String counts, String notes, String label) {
    if (counts == null && notes == null) {
      return label + " bed count missing or zero without explanation";
    } else if (counts != null && notes != null) {
      return label + " bed count has unneeded explanation";
    } else {
      return null;
    }
  }

}