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

import com.surftools.winlinkMessageMapper.dto.other.Grade;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class GradedCheckInMessage extends GisMessage {
  public final String comments;
  public final String status;
  public final String band;
  public final String mode;
  public final String version;
  public final String response;
  public final Grade grade;

  public GradedCheckInMessage(ExportedMessage xmlMessage, String latitude, String longitude, String organization, //
      String comments, String status, String band, String mode, String version, String response, Grade grade) {
    super(xmlMessage, latitude, longitude, organization);
    this.comments = comments;
    this.status = status;
    this.band = band;
    this.mode = mode;
    this.version = version;
    this.response = response;
    this.grade = grade;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", "Organization",
        "Comments", "Status", "Band", "Mode", "Version", "Response", "Grade" };
  }

  @Override
  public String[] getValues() {
    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, organization, comments, status,
        band, mode, version, response, grade.toString() };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.GRADABLE_CHECK_IN;
  }

}
