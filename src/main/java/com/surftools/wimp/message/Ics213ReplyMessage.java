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

package com.surftools.wimp.message;

import com.surftools.wimp.core.MessageType;

public class Ics213ReplyMessage extends ExportedMessage {
  public final String organization;
  public final String message;
  public final String reply;
  public final String replyBy;
  public final String replyPosition;
  public final String replyDateTime;

  public Ics213ReplyMessage(ExportedMessage xmlMessage, String organization, String message, //
      String reply, String replyBy, String replyPosition, String replyDateTime) {
    super(xmlMessage);
    this.organization = organization;
    this.message = message;
    this.reply = reply;
    this.replyBy = replyBy;
    this.replyPosition = replyPosition;
    this.replyDateTime = replyDateTime;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", "Organization", "Message", //
        "Reply", "ReplyBy", "ReplyPosition", "ReplyDateTime", "File Name" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, //
        latitude, longitude, organization, message, //
        reply, replyBy, replyPosition, replyDateTime, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ICS_213_REPLY;
  }

  @Override
  public String getMultiMessageComment() {
    return message;
  }
}
