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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

/**
 * Winlink Check In, looking for specific text in comments, plus setup
 *
 * @author bobt
 *
 */
public class ETO_2022_07_28 implements IGrader {
  // for post processing
  private int ppCount;
  private int ppOrgOk;
  private int ppCommentsContainsOk;
  private int ppExactCommentMatch;

  private Set<String> dumpIds;

  public ETO_2022_07_28() throws Exception {
    dumpIds = new HashSet<>();
  }

  @Override
  public GradeResult grade(GradableMessage gm) {
    if (!(gm instanceof CheckInMessage)) {
      return null;
    }

    var m = (CheckInMessage) gm;
    ++ppCount;

    if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
      // System.err.println("ETO_2022_07_28 grader: " + m);
    }

    /**
     * https://emcomm-training.org/ETO%20US%20Message%20Viewer%20Exercise%20for%207-28-2022.pdf
     *
     * SCORING
     *
     * 25 points for getting “Setup” information correct
     *
     * 25 points for getting the correct sender’s call sign
     *
     * 50 additional points for correct sender’s call sign on the first line and all upper case letters with no
     * punctuation or spaces preceding or following the call sign
     *
     */
    var points = 0;
    var explanations = new ArrayList<String>();

    var org = m.organization;
    final var requiredOrg = "ETO Winlink Thursday";
    if (org != null && org.equalsIgnoreCase(requiredOrg)) {
      points += 25;
      ++ppOrgOk;
    } else {
      explanations.add("agency/group name not " + requiredOrg);
    }

    var comments = m.comments;
    var requiredComment = "W7YAM";

    if (comments != null) {
      if (comments.toUpperCase().contains(requiredComment)) {
        points += 25;
        ++ppCommentsContainsOk;
      } else {
        explanations.add("comments do not contain required text: " + requiredComment);
      }

      var commentLines = comments.split("\n");
      if (commentLines.length >= 1) {
        var line1 = commentLines[0];
        if (line1.equals(requiredComment)) {
          points += 50;
          ++ppExactCommentMatch;
        } else {
          explanations.add("first line of comments does not exactly match required text: " + requiredComment);
        }
      } else {
        explanations.add("comments do not contain required text: " + requiredComment);
      }
    } else {
      explanations.add("no comments provided");
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

    return String.format("%.2f", 100d * d) + "%";
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
    sb.append("\nETO-2022-07-28 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    sb.append(formatPP("agency/group name", ppOrgOk));
    sb.append(formatPP("comment contains required", ppCommentsContainsOk));
    sb.append(formatPP("comment exact match", ppExactCommentMatch));
    sb.append("\n");

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

  @Override
  public void setDumpIds(Set<String> dumpIds) {
    this.dumpIds = dumpIds;
  }

  @Override
  public void setConfigurationManager(IConfigurationManager cm) {
  }

}