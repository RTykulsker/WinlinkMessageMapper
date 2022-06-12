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

package com.surftools.winlinkMessageMapper.grade.named;

import java.util.ArrayList;
import java.util.List;

import com.surftools.winlinkMessageMapper.dto.message.HospitalBedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

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
public class ETO_2022_06_16 implements IGrader {

  // for post processing
  private int ppCount;
  private int ppOrganizationOk;
  private int ppCountPhoneOk;
  private int ppCountEmailOk;
  private int ppCountMedicalBedOk;
  private int ppCountCriticalBedOk;
  private int ppCountOtherBedOk;
  private int ppCountCommentsOk;

  public ETO_2022_06_16() throws Exception {
  }

  @Override
  public GradeResult grade(GradableMessage gm) {
    if (!(gm instanceof HospitalBedMessage)) {
      return null;
    }

    HospitalBedMessage m = (HospitalBedMessage) gm;

    ++ppCount;
    var points = 0;
    var explanations = new ArrayList<String>();

    var organization = m.organization;
    if (organization != null && organization.equalsIgnoreCase("ETO Winlink Thursday")) {
      ++ppOrganizationOk;
      points += 10;
    } else {
      explanations.add("agency/group name not 'ETO Winlink Thursday'");
    }

    var contactPhone = m.contactPhone;
    if (contactPhone != null && contactPhone.equals("555-555-5555")) {
      ++ppCountPhoneOk;
      points += 10;
    } else {
      explanations.add("contact phone not set to '555-555-5555'");
    }

    var contactEmail = m.contactEmail;
    if (contactEmail != null && contactEmail.equalsIgnoreCase(m.from)) {
      ++ppCountEmailOk;
      points += 10;
    } else {
      explanations.add("email not set to your Winlink address");
    }

    var medCount = m.medicalBedCount;
    var medNotes = m.medicalBedNotes;
    if (medCount != null) {
      Integer count = null;
      try {
        count = Integer.parseInt(medCount);
        if (count == 0) {
          if (medNotes != null) {
            ++ppCountMedicalBedOk;
            points += 20;
          } else {
            explanations.add("medical bed count missing or zero without explanation");
          }
        }
      } catch (Exception e) {
        if (medNotes != null) {
          ++ppCountMedicalBedOk;
          points += 20;
        } else {
          explanations.add("medical bed count unreadable without explanation");
        }
      }
    } else {
      if (medNotes != null) {
        ++ppCountMedicalBedOk;
        points += 20;
      } else {
        explanations.add("medical bed count missing or zero without explanation");
      }
    }

    var critCount = m.criticalBedCount;
    var critNotes = m.criticalBedNotes;
    if (critCount != null) {
      Integer count = null;
      try {
        count = Integer.parseInt(critCount);
        if (count == 0) {
          if (critNotes != null) {
            ++ppCountCriticalBedOk;
            points += 20;
          } else {
            explanations.add("critical bed count missing or zero without explanation");
          }
        }
      } catch (Exception e) {
        if (critNotes != null) {
          ++ppCountCriticalBedOk;
          points += 20;
        } else {
          explanations.add("critical bed count unreadable without explanation");
        }
      }
    } else {
      if (critNotes != null) {
        ++ppCountCriticalBedOk;
        points += 20;
      } else {
        explanations.add("critical bed count missing or zero without explanation");
      }
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

    var additionalComments = m.additionalComments;
    if (additionalComments != null && !additionalComments.toLowerCase().contains("This is an exercise")) {
      ++ppCountCommentsOk;
      points += 20;
    } else {
      explanations.add("comments doesn't contain 'This is an exercise'");
    }

    points = Math.min(100, points);
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

    gm.setIsGraded(true);
    gm.setGrade(grade);
    gm.setExplanation(explanation);

    return new GradeResult(grade, explanation);
  }

  private String formatPercent(Double d) {
    if (d == null) {
      return "";
    }

    return String.format("%.0f", 100d * d) + "%";
  }

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    if (messages == null || messages.size() == 0 || messages.get(0).getMessageType() != MessageType.CHECK_IN) {
      return null;
    }

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);
    sb.append("\nETO-2022-06-16 Grading Report: graded " + ppCount + " Hospital Bed messages\n");
    sb.append(formatPP("agency/group name", ppOrganizationOk));
    sb.append(formatPP("contact phone", ppCountPhoneOk));
    sb.append(formatPP("contact email", ppCountEmailOk));
    sb.append(formatPP("medical/surgical beds", ppCountMedicalBedOk));
    sb.append(formatPP("critical car beds", ppCountCriticalBedOk));
    sb.append(formatPP("other beds", ppCountOtherBedOk));
    sb.append(formatPP("additional comments", ppCountCommentsOk));

    return sb.toString();
  }

  private String formatPP(String label, int okCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = (double) okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
  }

}