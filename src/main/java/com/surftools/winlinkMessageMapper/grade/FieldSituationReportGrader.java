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

package com.surftools.winlinkMessageMapper.grade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.FieldSituationMessage;

public class FieldSituationReportGrader implements IGrader {

  private final String gradeKey;

  public FieldSituationReportGrader(String gradeKey) {
    this.gradeKey = gradeKey;
  }

  @Override
  public GradeResult grade(GradableMessage m) {
    if (gradeKey.equals("fsr:ETO-2022-04-14")) {
      return grade_ETO_2022_04_14((FieldSituationMessage) m);
    } else if (gradeKey.equals("fsr:ETO-2022-05-14")) {
      return grade_ETO_2022_05_14((FieldSituationMessage) m);
    }

    return null;
  }

  /**
   * Operation Ashfall
   *
   * @param m
   * @return
   */
  private GradeResult grade_ETO_2022_05_14(FieldSituationMessage m) {

    // participation points
    var points = 20;
    var explanations = new ArrayList<String>();

    var expected = "";
    var automaticFail = false;

    // AGENCY or GROUP NAME: Must match exactly EmComm Training Organization 0514 THIS IS A DRILL
    expected = "EmComm Training Organization 0514 THIS IS A DRILL";
    if (m.organization != null && m.organization.equalsIgnoreCase(expected)) {
      points += 5;
    } else {
      explanations.add("Agency/Group name not: " + expected + ".");
    }

    // Precedence: MUST be Priority
    expected = "(P) - Priority";
    if (m.precedence != null && m.precedence.equals(expected)) {
      points += 5;
    } else {
      automaticFail = true;
      explanations.add("Precedence must be: " + expected + ".");
    }

    // TASK # must be: 0514
    expected = "0514";
    var task = m.task;
    if (task != null && task.equals(expected)) {
      points += 5;
    } else {
      explanations.add("TASK # must be: " + expected + ".");
    }

    // Box1: MUST be checked No and contain no comments
    if (m.isHelpNeeded.equals("NO")) {
      points += 5;
    } else {
      automaticFail = true;
      explanations.add("Box 1 must be NO.");
    }

    if (m.neededHelp == null) {
      points += 5;
    } else {
      automaticFail = true;
      explanations.add("Box 1 comments must be empty.");
    }

    // Box 2: CITY must be filled in along with COUNTY and STATE for the RMS gateway part of the drill. NO TERRITORY
    // should be specified. For international participants sending to ETO-CAN or ETO-DX your information under Territory
    // should be specified. For the P2P part of the drill, CITY must be filled in.
    var isCity = m.city != null && m.city.length() > 0;
    var isCounty = m.county != null && m.county.length() > 0;
    var isState = m.state != null && m.state.length() > 0;
    var isTerritory = m.territory != null && m.territory.length() > 0;
    var isRMS = m.to.startsWith("ETO");
    if (isRMS) {
      var isDomestic = m.to.startsWith("ETO-0") || m.to.equals("ETO-10");
      if (isDomestic) {
        if (isCity && isCounty && isState) {
          points += 5;
        } else {
          explanations.add("must supply City, County and State for US RMS.");
        } // endif fail domestic
      } else { // endif domestic
        if (isCity && isTerritory) {
          points += 5;
        } else {
          explanations.add("must supply City and Territory for non-US RMS.");
        }
      } // endif not domestic
    } else { // endif RMS
      if (isCity) {
        points += 5;
      } else {
        explanations.add("must supply City for P2P.");
      }
    } // endif not RMS

    // Boxes 4-11 must contain comments if the answer is “NO”, and must not contain comments if the answer is “YES” or
    // “Unknown - N/A.”

    var fieldsList = Arrays
        .asList(Arrays.asList(m.landlineStatus, m.landlineComments, "landline"), //
            Arrays.asList(m.cellPhoneStatus, m.cellPhoneComments, "cellPhone"), //
            Arrays.asList(m.radioStatus, m.radioComments, "radio"), //
            Arrays.asList(m.tvStatus, m.tvComments, "tv"), //
            Arrays.asList(m.waterStatus, m.waterComments, "water"), //
            Arrays.asList(m.powerStatus, m.powerComments, "power"), //
            Arrays.asList(m.internetStatus, m.internetComments, "Internet"), //
            Arrays.asList(m.noaaStatus, m.noaaComments, "NOAA/Weather")//
        );//

    var boxIndex = 3;
    for (var fields : fieldsList) {
      ++boxIndex;
      var status = fields.get(0);
      var comments = fields.get(1);
      var label = fields.get(2);

      if (status.equals("YES") || status.equals("Unknown - N/A")) {
        if (comments == null || comments.equals("")) {
          points += 5;
        } else {
          explanations.add("box " + boxIndex + " must NOT contain comments for " + label + ".");
        }
      } else {
        if (comments == null || comments.equals("")) {
          explanations.add("box " + boxIndex + " must contain comments for " + label + ".");
        } else {
          points += 5;
        }
      }
    }

    // Box 12 must contain ashfall mounts in inches if present or “NO ASHFALL” if none is present
    if (m.additionalComments != null) {
      var comments = m.additionalComments.toLowerCase();
      if (comments.contains("NO ASHFALL".toLowerCase())) {
        points += 5;
      } else {
        if (comments.contains("inch") || comments.matches(".*\\d.*")) {
          points += 5;
        } else {
          explanations
              .add("Box 12 must contain ashfall amounts in inches if present or “NO ASHFALL” if none is present.");
        }
      }

      // additional activity option -- no original credit, so now it's extra credit
      {
        final var jetSet = Set.of("jet stream over", "jet stream near", "jetstream over", "jetstream near");
        for (var string : jetSet) {
          if (comments.contains(string)) {
            points += 5;
            explanations.add("extra credit for Jet Stream");
          } // end if contains
        } // end for
      } // end block additional activity
    } else {
      explanations.add("Box 12 must contain ashfall mounts in inches if present or “NO ASHFALL” if none is present.");
    }

    // Box 13 must be filled.
    if (m.poc != null) {
      points += 5;
    } else {
      explanations.add("Box 13 must be filled.");
    }

    points = Math.min(100, points);
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

    if (automaticFail) {
      grade = "Automatic Fail";
    }
    return new GradeResult(grade, explanation);
  }

  private GradeResult grade_ETO_2022_04_14(FieldSituationMessage m) {
    var points = 0;
    var explanations = new ArrayList<String>();

    // automatic fail with 0 grade if not Routine precedence
    var precedence = m.precedence;
    if (!precedence.equals("(R) - Routine")) {
      return new GradeResult("0", "non-Routine precedence is automatic failure");
    } else {
      points += 10;
    }

    // require task == WLT-001
    var task = m.task;
    if (task != null && !task.equals("WLT-001")) {
      explanations.add("expected task: WLT-001, got: " + task);
    } else {
      points += 10;
    }

    // require NO isHelpNeeded
    var isHelpNeeded = m.isHelpNeeded;
    if (!isHelpNeeded.equals("NO")) {
      explanations.add("expected ishelpNeeded: NO, got " + isHelpNeeded);
    } else {
      points += 10;
    }

    // require null neededHelp
    var neededHelp = m.neededHelp;
    if (neededHelp == null || neededHelp.length() == 0) {
      points += 10;
    } else {
      explanations.add("expected neededHelp: (null), got: " + neededHelp);
    }

    // loop through {tv,water,internet}Status, rotating the expected status
    final var statuses = Arrays.asList(m.tvStatus, m.waterStatus, m.internetStatus);
    final var labels = Arrays.asList("tv", "water", "internet");
    final var expecteds = Arrays.asList("YES", "Unknown - N/A", "NO");
    List<Integer> indices = null;

    // set up "regional" rotations of expected values
    if (new HashSet<String>(Arrays.asList("ETO-01", "ETO-02", "ETO-03", "ETO-04")).contains(m.to)) {
      indices = Arrays.asList(0, 1, 2);
    } else if (new HashSet<String>(Arrays.asList("ETO-05", "ETO-06", "ETO-07", "ETO-08")).contains(m.to)) {
      indices = Arrays.asList(1, 2, 0);
    } else if (new HashSet<String>(Arrays.asList("ETO-09", "ETO-10", "ETO-DX")).contains(m.to)) {
      indices = Arrays.asList(2, 0, 1);
    } else {
      explanations.add("unexpected To: " + m.to);
    }

    if (indices != null) {
      for (int i = 0; i < statuses.size(); ++i) {
        var status = statuses.get(i);
        // rotate through the expected value based on "region"
        var expected = expecteds.get(indices.get(i));
        if (status.equals(expected)) {
          points += 10;
        } else {
          explanations.add("expected " + labels.get(i) + "Status: " + expected + ", got: " + status);
        }
      }
    }

    // loop through {tv,water,internet}Comments, expect all to be: "this is an exercise"
    boolean isStrict = false;
    if (isStrict) {
      final var THIS_IS_AN_EXERCISE = "this is an exercise";
      final var comments = Arrays.asList(m.tvComments, m.waterComments, m.internetComments);
      for (int i = 0; i < comments.size(); ++i) {
        var comment = comments.get(i);
        if (comment != null && comment.equals(THIS_IS_AN_EXERCISE)) {
          points += 10;
        } else {
          explanations.add("expected " + labels.get(i) + "Comments: " + THIS_IS_AN_EXERCISE + ", got: " + comment);
        }
      }
    } else {
      final var THIS_IS_AN_EXERCISE = "this is an exercise";
      final var comments = Arrays.asList(m.tvComments, m.waterComments, m.internetComments);
      for (int i = 0; i < comments.size(); ++i) {
        var comment = comments.get(i);
        if (comment != null && comment.toLowerCase().startsWith(THIS_IS_AN_EXERCISE)) {
          points += 10;
        } else {
          explanations.add("expected " + labels.get(i) + "Comments: " + THIS_IS_AN_EXERCISE + ", got: " + comment);
        }
      }
    }

    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "perfect score" : String.join("\n", explanations);

    return new GradeResult(grade, explanation);
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<ExportedMessage> messages) {
    return DefaultGrader.defaultPostProcessReport(messages);
  }
}