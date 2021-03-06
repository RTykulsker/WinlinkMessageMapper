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

import com.surftools.winlinkMessageMapper.dto.other.LatLongPair;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;

public class CheckInMessage extends GisMessage implements GradableMessage {
  public final String comments;
  public final String status;
  public final String band;
  public final String mode;
  public final String version;

  public final String formDate;
  public final String formTime;

  private boolean isGraded;
  private String grade;
  private String explanation;

  public CheckInMessage(ExportedMessage xmlMessage, LatLongPair latlong, String organization, //
      String comments, String status, String band, String mode, String version, //
      String formDate, String formTime, MessageType messageType) {
    super(xmlMessage, latlong, organization);
    this.comments = comments;
    this.status = status;
    this.band = band;
    this.mode = mode;
    this.version = version;

    this.formDate = formDate;
    this.formTime = formTime;
  }

  @Override
  public String[] getHeaders() {
    if (isGraded) {
      return new String[] { "MessageId", "From", "To", "ToList", "CcList", "Subject", "Date", "Time", "Latitude",
          "Longitude", "Organization", "Comments", "Status", "Band", "Mode", "Grade", "Explanation", "Version",
          "FormDate", "FormTime" };
    } else {
      return new String[] { "MessageId", "From", "To", "ToList", "CcList", "Subject", "Date", "Time", "Latitude",
          "Longitude", "Organization", "Comments", "Status", "Band", "Mode", "Version", "FormDate", "FormTime" };
    }
  }

  @Override
  public String[] getValues() {
    if (isGraded) {
      return new String[] { messageId, from, to, toList, ccList, subject, date, time, latitude, longitude, organization,
          comments, status, band, mode, grade, explanation, version, formDate, formTime };
    } else {
      return new String[] { messageId, from, to, toList, ccList, subject, date, time, latitude, longitude, organization,
          comments, status, band, mode, version, formDate, formTime };
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.CHECK_IN;
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

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
