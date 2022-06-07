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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.FieldSituationMessage;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

public class ETO_2022_04_14 implements IGrader {

  @Override
  public GradeResult grade(GradableMessage gm) {
    FieldSituationMessage m = (FieldSituationMessage) gm;
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

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }
}