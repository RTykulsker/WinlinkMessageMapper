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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surftools.utils.config.IConfigurationManager;

public abstract class DefaultGrader implements IGrader {

  private final String gradeKey;

  protected static Comparator<String> gradeComparator = new DefaultGradeComparator();

  protected IConfigurationManager cm;
  protected Set<String> dumpIds;

  public DefaultGrader(String gradeKey) {
    this.gradeKey = gradeKey;
    gradeComparator = new DefaultGradeComparator();
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
  public String getPostProcessReport(List<GradableMessage> messages) {
    return "";
  }

  public static String defaultPostProcessReport(List<GradableMessage> messages) {
    if (messages == null) {
      return "";
    }

    Map<String, Integer> gradeCountMap = new HashMap<>();
    int totalGraded = 0;
    for (var m : messages) {
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

    // sort keys, based on grader criteria
    var keySet = new ArrayList<String>(gradeCountMap.keySet());
    Collections.sort(keySet, gradeComparator);
    for (String grade : keySet) {
      int count = gradeCountMap.getOrDefault(grade, Integer.valueOf(0));
      double percent = 100d * count / totalGraded;
      var percentString = FORMAT.format(percent) + "%";
      sb.append(INDENT + grade.toString() + ": " + count + " (" + percentString + ")\n");
    }
    var s = sb.toString();
    return s;
  }

  @Override
  public void setDumpIds(Set<String> dumpIds) {
    this.dumpIds = dumpIds;
  }

  @Override
  public void setConfigurationManager(IConfigurationManager cm) {
    this.cm = cm;
  }

  protected static class DefaultGradeComparator implements Comparator<String> {

    @Override
    public int compare(String o1, String o2) {
      // numeric descending, alpha ascending
      Double d1 = null;
      Double d2 = null;
      try {
        d1 = Double.parseDouble(o1);
      } catch (Exception e) {
        ;
      }
      try {
        d2 = Double.parseDouble(o2);
      } catch (Exception e) {
        ;
      }

      if (d1 == null && d2 == null) {
        return o1.compareTo(o2);
      } else if (d1 == null) {
        return 1;
      } else if (d2 == null) {
        return -1;
      } else {
        double diff = d2 - d1;
        if (diff > 0) {
          return 1;
        } else if (diff < 0) {
          return -1;
        } else {
          return 0;
        }
      }
    } // end compare()
  } // end class DefaultGradeComparator

}
