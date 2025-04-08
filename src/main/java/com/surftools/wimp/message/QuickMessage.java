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

import java.time.LocalDateTime;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

/**
 * the message type for General/Quick
 *
 * @author bobt
 *
 */
public class QuickMessage extends ExportedMessage {
  public final LatLongPair formLocation;
  public final LocalDateTime formDateTime;

  public final String attention;
  public final String sendToAddress;
  public final String fromNameGroup;
  public final String dateTimeString;
  public final String subject;

  public final String messageText;
  public final String version;

  public QuickMessage(ExportedMessage exportedMessage, //
      LatLongPair formLocation, LocalDateTime formDateTime, //
      String attention, String sendToAddress, String fromNameGroup, String dateTimeString, //
      String subject, String messageText, String version) {
    super(exportedMessage);
    this.formLocation = formLocation;
    this.formDateTime = formDateTime;

    this.attention = attention;
    this.sendToAddress = sendToAddress;
    this.fromNameGroup = fromNameGroup;
    this.dateTimeString = dateTimeString;
    this.subject = subject;
    this.messageText = messageText;
    this.version = version;

    if (formDateTime != null) {
      setSortDateTime(formDateTime);
    }

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "Attention", "SendTo", "From (Name/Group)", "Date/Time", "Subject", "Message", "Version", "File Name" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, //
        date, time, latitude, longitude, //
        attention, sendToAddress, fromNameGroup, dateTimeString, subject, messageText, version, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.QUICK;
  }

  @Override
  public String getMultiMessageComment() {
    return messageText;
  }

}
