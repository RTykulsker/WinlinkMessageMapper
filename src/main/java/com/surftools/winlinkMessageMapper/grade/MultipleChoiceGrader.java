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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
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
  private boolean doToUpper = true;
  private boolean doTrim = true;

  private String NOT_VALID_EXPLANATION = "Your response of %s is not valid, because it is not one of the valid responses: %s that are defined for this exercise.";
  private String INCORRECT_EXPLANATION = "Your response of %s is incorrect. %s";
  private String CORRECT_EXPLANATION = "Your response of %s is correct.";

  private Set<String> correctResponseSet;
  private String correctResponseString;
  private Set<String> incorrectResponseSet;

  @SuppressWarnings("unused")
  private Set<String> dumpIds;

  public MultipleChoiceGrader(MessageType messageType) {
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

    if (doToUpper) {
      response = response.toUpperCase();
    }

    if (doTrim) {
      response = response.trim();
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

  /**
   * parse something like this: --gradeKey="check_in:mc:correct:B:incorrect:A,C,D:doToUpper:true"
   *
   * @param gradeKey
   */
  public void parse(String gradeKey) {
    var fields = gradeKey.split(":");

    if (fields.length != 8) {
      throw new IllegalArgumentException("can't parse: " + gradeKey + ", wrong number of fields");
    }

    if (!fields[1].equals("mc")) {
      throw new IllegalArgumentException("can't parse: " + gradeKey + ", \"mc\" not found in field[1]");
    }

    if (!fields[2].equals("correct")) {
      throw new IllegalArgumentException("can't parse: " + gradeKey + ", \"correct\" not found in field[2]");
    }

    if (!fields[4].equals("incorrect")) {
      throw new IllegalArgumentException("can't parse: " + gradeKey + ", \"incorrect\" not found in field[4]");
    }

    if (!fields[6].equals("doToUpper")) {
      throw new IllegalArgumentException("can't parse: " + gradeKey + ", \"doToUpper\" not found in field[6]");
    }

    var correctString = fields[3];
    var incorrectString = fields[5];
    var doToUpper = Boolean.parseBoolean(fields[7]);
    setDoToUpper(doToUpper);

    if (doToUpper) {
      correctString = correctString.toUpperCase();
      incorrectString = incorrectString.toUpperCase();
    }

    var correctFields = correctString.split(",");
    var incorrectFields = incorrectString.split(",");

    setCorrectResponseSet(new HashSet<String>(Arrays.asList(correctFields)));
    setIncorrectResponseSet(new HashSet<String>(Arrays.asList(incorrectFields)));

    var intersection = new HashSet<String>(correctResponseSet);
    intersection.retainAll(incorrectResponseSet);

    if (intersection.size() != 0) {
      throw new IllegalArgumentException("can't parse: " + gradeKey + ", correct and incorrect intersect");
    }

    var list = new ArrayList<String>(correctResponseSet);
    list.addAll(incorrectResponseSet);
    Collections.sort(list);
    setValidResponseString(String.join(",", list));
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    return DefaultGrader.defaultPostProcessReport(messages);
  }

  @Override
  public GradeResult grade(GradableMessage m) {
    if (m instanceof CheckInMessage) {
      CheckInMessage message = (CheckInMessage) m;
      message.setIsGraded(true);
      var comments = message.comments;
      if (comments != null) {
        var result = grade(message.comments);
        message.setGrade(result.grade());
        message.setExplanation(result.explanation());
      } else {
        message.setGrade(NOT_VALID);
        message.setExplanation("No response provided");
      }
    }
    return null;
  }

  public void setDoDequote(boolean b) {
    this.doDequote = b;
  }

  public void setDoStopChars(boolean b) {
    this.doStopChars = b;
  }

  public void setDoToUpper(boolean b) {
    this.doToUpper = b;
  }

  public void setDoTrim(boolean b) {
    this.doTrim = b;
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

  @Override
  public GraderType getGraderType() {
    return GraderType.SINGLE_LINE_STRING;
  }

  @Override
  public void setDumpIds(Set<String> dumpIds) {
    this.dumpIds = dumpIds;
  }

  @Override
  public void setConfigurationManager(IConfigurationManager cm) {

  }

}
