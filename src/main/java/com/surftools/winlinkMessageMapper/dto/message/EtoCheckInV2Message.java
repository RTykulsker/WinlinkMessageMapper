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

package com.surftools.winlinkMessageMapper.dto.message;

import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;

public class EtoCheckInV2Message extends GisMessage implements GradableMessage {
  public final String comments;
  public final String formDate;
  public final String formTime;
  public final String formName;
  public final String version;

  private boolean isGraded;
  private String grade;
  private String explanation;

  public EtoCheckInV2Message(ExportedMessage xmlMessage, String latitude, String longitude, String comments,
      String formDate, String formTime, String formName, String version) {
    super(xmlMessage, latitude, longitude, null);
    this.comments = comments;
    this.formDate = formDate;
    this.formTime = formTime;
    this.formName = formName;
    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Comments", "Grade", "Explanation", "Version", "FormDate", "FormTime" };
  }

  @Override
  public String[] getValues() {
    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        comments, grade, explanation, version, formDate, formTime };
  }

  @Override
  public boolean isGraded() {
    return isGraded;
  }

  @Override
  public void setIsGraded(boolean isGraded) {
    this.isGraded = isGraded;
  }

  @Override
  public String getGrade() {
    return grade;
  }

  @Override
  public void setGrade(String grade) {
    this.grade = grade;
  }

  @Override
  public String getExplanation() {
    return explanation;
  }

  @Override
  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public String getComments() {
    return comments;
  }

  public String getFormDate() {
    return formDate;
  }

  public String getFormTime() {
    return formTime;
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ETO_CHECK_IN_V2;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
