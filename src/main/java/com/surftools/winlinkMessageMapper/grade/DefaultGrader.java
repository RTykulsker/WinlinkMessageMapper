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

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;

public class DefaultGrader implements IGrader {

  private final String gradeKey;

  public DefaultGrader(String gradeKey) {
    this.gradeKey = gradeKey;
  }

  @Override
  public GradeResult grade(String s) {
    String grade = "unknown";
    String explanation = "unsupported gradeKey: " + gradeKey;
    return new GradeResult(grade, explanation);
  }

  @Override
  public GradeResult grade(GradableMessage m) {
    String grade = "unknown";
    String explanation = "unsupported gradeKey: " + gradeKey;
    return new GradeResult(grade, explanation);
  }

  @Override
  public String getPostProcessReport(List<ExportedMessage> messages) {
    return "";
  }

  public static String defaultPostProcessReport(List<ExportedMessage> messages) {

    Map<String, Integer> gradeCountMap = new HashMap<>();
    int totalGraded = 0;
    for (ExportedMessage message : messages) {
      GradableMessage m = (GradableMessage) message;
      if (m.isGraded()) {
        String grade = m.getGrade();
        int count = gradeCountMap.getOrDefault(grade, Integer.valueOf(0));
        ++count;
        ++totalGraded;
        gradeCountMap.put(grade, count);
      }
    }

    if (gradeCountMap.size() == 0) {
      return "";
    }

    final var INDENT = "   ";
    final var FORMAT = new DecimalFormat("0.00");
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append(messages.get(0).getMessageType().toString() + ": " + totalGraded + " messages" + "\n");
    for (String grade : gradeCountMap.keySet()) {
      int count = gradeCountMap.getOrDefault(grade, Integer.valueOf(0));
      double percent = 100d * count / totalGraded;
      var percentString = FORMAT.format(percent) + "%";
      sb.append(INDENT + grade.toString() + ": " + count + " (" + percentString + ")\n");
    }
    var s = sb.toString();
    return s;
  }

}
