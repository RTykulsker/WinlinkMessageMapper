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

/**
 * encapsulate the reply to a RRI Welfare Radiogram message
 *
 * @author bobt
 *
 */
public class RRIReplyWelfareRadiogramMessage extends ExportedMessage {
  public final String reply;
  public final String replyDateTime;
  public final String sourceMessage;

  public RRIReplyWelfareRadiogramMessage(ExportedMessage exportedMessage, //
      String reply, String replyDateTime, String sourceMessage) {
    super(exportedMessage);
    this.reply = reply;
    this.replyDateTime = replyDateTime;
    this.sourceMessage = sourceMessage;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Reply", "Reply Date/Time", "Source Msg" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = msgLocation != null && msgLocation.isValid() ? msgLocation.getLatitude() : "0.0";
    var longitude = msgLocation != null && msgLocation.isValid() ? msgLocation.getLongitude() : "0.0";
    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        reply, replyDateTime, source };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.RRI_REPLY_WELFARE_RADIOGRRAM;
  }

  @Override
  public String getMultiMessageComment() {
    return "n/a";
  }
}
