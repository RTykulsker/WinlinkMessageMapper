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
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.winlinkMessageMapper.dto.message.EtoCheckInV2Message;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

/**
 * ETO Check In V2 (custom form)
 *
 * @author bobt
 *
 */
public class ETO_2022_09_08 implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_09_08.class);

  private IConfigurationManager cm;

  // for post processing
  private int ppCount;
  private int ppCommentOk;
  private Map<String, Integer> commentCountMap;

  private Set<String> dumpIds;

  public ETO_2022_09_08() {
    commentCountMap = new TreeMap<>();
  }

  @Override

  /**
   * Grading Message is from the ETO Check-In form 75%
   *
   * Correct Version Number entered in comment and is 5 characters long. 25%
   */
  public GradeResult grade(GradableMessage gm) {
    if (!(gm instanceof EtoCheckInV2Message)) {
      return null;
    }

    EtoCheckInV2Message m = (EtoCheckInV2Message) gm;

    if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
      // logger.info("ETO_2022_09_08 grader: " + m);
    }

    ++ppCount;
    var points = 75;
    var explanations = new ArrayList<String>();

    var comments = m.comments;
    var expectedComments = "1.0.4";
    if (comments != null) {
      comments = comments.trim();
      if (comments.equals(expectedComments)) {
        points += 25;
      } else {
        explanations
            .add("Comments (" + comments + ") doesn't matchexpected Comments: '" + expectedComments + "' not found");
      }
    } else {
      explanations.add("expected Comments: '" + expectedComments + "' not found");
      comments = "(null)";
    }
    var count = commentCountMap.getOrDefault(comments, Integer.valueOf(0));
    ++count;
    commentCountMap.put(comments, count);

    points = Math.min(100, points);
    points = Math.max(0, points);
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
    if (messages == null || messages.size() == 0) {
      return null;
    }

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);
    sb.append("\nETO-2022-09-08 Grading Report: graded " + ppCount + " ETO Check In V2 messages\n");
    sb.append(formatPP("Correct form version", ppCommentOk));

    sb.append("\nCounts by comment:\n");
    for (var comment : commentCountMap.keySet()) {
      var count = commentCountMap.get(comment);
      sb.append(formatPP(comment, count));
    }

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
    this.cm = cm;
  }

  public static record PositionReport(String from, String to, String nauticalMiles, String bearing, String latitude,
      String longitude, String date, String time, String comments) {

    public static String[] getHeaders() {
      return new String[] { //
          "From", "To", "NauticalMiles", "Bearing", "Latitude", "Longitude", //
          "Date", "Time", "Comments" };
    }

    public String[] getValues() {
      return new String[] { //
          from, to, nauticalMiles, bearing, latitude, longitude, //
          date, time, comments };
    }
  }

}