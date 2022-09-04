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

public class DyfiMessage extends GisMessage implements GradableMessage {
  public final String location;
  public final boolean isRealEvent;
  public final boolean isFelt;
  public final String response;
  public final String comments;
  public final String intensity;
  public final String formVersion;

  private boolean isGraded;
  private String grade;
  private String explanation;

  public DyfiMessage(ExportedMessage xmlMessage, String latitude, String longitude, String organization, //
      String location, boolean isRealEvent, boolean isFelt, String response, String comments, String intensity,
      String formVersion) {
    super(xmlMessage, latitude, longitude, organization);
    this.location = location;
    this.isRealEvent = isRealEvent;
    this.isFelt = isFelt;
    this.response = response;
    this.comments = comments;
    this.intensity = intensity;
    this.formVersion = formVersion;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", "ExerciseId", //
        "Location", "IsRealEvent", "IsFelt", "Response", "Comments", "Intensity", "FormVersion", "Grade",
        "Explanation" };
  }

  @Override
  public String[] getValues() {
    return new String[] { messageId, from, to, subject, date, time, //
        latitude, longitude, organization, location, //
        Boolean.toString(isRealEvent), Boolean.toString(isFelt), response, comments, intensity, formVersion, grade,
        explanation };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.DYFI;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
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
}
