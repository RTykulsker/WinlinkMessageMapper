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
 * encapsulate the RRI Quick Welfare message
 *
 * @author bobt
 *
 */
public class RRIQuickWelfareMessage extends ExportedMessage {
  public final String formFrom;
  public final String formDateTime;
  public final String incidentName; // event location or region/area name
  public final String text;
  public final String version;

  public RRIQuickWelfareMessage(ExportedMessage exportedMessage, //
      String formFrom, String formDateTime, String incidentName, String text, String version) {
    super(exportedMessage);
    this.formFrom = formFrom;
    this.formDateTime = formDateTime;
    this.incidentName = incidentName;
    this.text = text;
    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "(Form) From", "(Form) Date/Time", "Incident Name", "Message Text", "Version", "File Name" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = msgLocation != null && msgLocation.isValid() ? msgLocation.getLatitude() : "0.0";
    var longitude = msgLocation != null && msgLocation.isValid() ? msgLocation.getLongitude() : "0.0";
    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        formFrom, formDateTime, incidentName, text, version, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.RRI_QUICK_WELFARE;
  }

  @Override
  public String getMultiMessageComment() {
    return "n/a";
  }
}
