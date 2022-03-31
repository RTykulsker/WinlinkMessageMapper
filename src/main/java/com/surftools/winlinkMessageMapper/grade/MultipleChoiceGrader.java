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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class MultipleChoiceGrader implements IGrader {

  private static final String CORRECT = "correct";
  private static final String INCORRECT = "incorrect";
  private static final String NOT_VALID = "not valid";

  private static final Character[] STOP_CHARS_ARRAY = new Character[] { '.', ',', ';', ':' };

  final Set<Character> STOP_CHARS = new HashSet<Character>(Arrays.asList(STOP_CHARS_ARRAY));

  private String validResponseString;
  private boolean doDequote = true;
  private boolean doStopChars = false;

  private String NOT_VALID_EXPLANATION = "Your response of %s is not valid, because it is not one of the valid responses: %s that are defined for this exercise.";
  private String INCORRECT_EXPLANATION = "Your response of %s is incorrect. %s";
  private String CORRECT_EXPLANATION = "Your response of %s is correct.";

  private final MessageType messageType;

  private Set<String> correctResponseSet;
  private String correctResponseString;
  private Set<String> incorrectResponseSet;

  public MultipleChoiceGrader(MessageType messageType) {
    this.messageType = messageType;

    correctResponseSet = new HashSet<>();
    incorrectResponseSet = new HashSet<>();
  }

  @Override
  public GradeResult grade(String response) {
    var grade = "";
    var explanation = "";

    if (doDequote) {
      if (response.startsWith("\"") && response.endsWith("\"")) {
        response = response.substring(1, response.length() - 1);
      }
    }

    if (doStopChars) {
      if ((response.length() == 2) && STOP_CHARS.contains(response.charAt(1))) {
        response = response.substring(0, 1);
      }
    }

    if (correctResponseSet.contains(response)) {
      grade = CORRECT;
      explanation = String.format(CORRECT_EXPLANATION, response);
    } else if (incorrectResponseSet.contains(response)) {
      grade = INCORRECT;
      explanation = String.format(INCORRECT_EXPLANATION, response, correctResponseString);
    } else {
      grade = NOT_VALID;
      explanation = String.format(NOT_VALID_EXPLANATION, response, validResponseString);
    }

    var gradeResult = new GradeResult(grade, explanation);
    return gradeResult;
  }

  @Override
  public String getPostProcessReport(List<ExportedMessage> messages) {

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
    sb.append(messageType.toString() + ": " + totalGraded + " messages" + "\n");
    for (String grade : gradeCountMap.keySet()) {
      int count = gradeCountMap.getOrDefault(grade, Integer.valueOf(0));
      double percent = 100d * count / totalGraded;
      var percentString = FORMAT.format(percent) + "%";
      sb.append(INDENT + grade.toString() + ": " + count + " (" + percentString + ")\n");
    }
    var s = sb.toString();
    return s;
  }

  @Override
  public GradeResult grade(GradableMessage m) {
    return null;
  }

  public void setDoDequote(boolean b) {
    this.doDequote = b;
  }

  public void setDoStopChars(boolean b) {
    this.doStopChars = b;
  }

  public void setCorrectResponseSet(HashSet<String> hashSet) {
    this.correctResponseSet = hashSet;

    if (correctResponseSet.size() == 1) {
      correctResponseString = "The correct response is: " + correctResponseSet.iterator().next();
    } else {
      var list = new ArrayList<>(correctResponseSet);
      Collections.sort(list);
      correctResponseString = "The correct responses are: " + String.join(",", list);
    }

  }

  public void setIncorrectResponseSet(HashSet<String> hashSet) {
    this.incorrectResponseSet = hashSet;
  }

  public void setValidResponseString(String string) {
    this.validResponseString = string;
  }

}
