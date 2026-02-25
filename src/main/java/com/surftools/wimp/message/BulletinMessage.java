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

public class BulletinMessage extends ExportedMessage {
  public final String organization;

  public final String forNameGroup; // to
  public final String fromNameGroup;
  public final String bulletinNumber;
  public final String formDateTime;
  public final String precedence;
  public final String formSubject;
  public final String bulletinText;
  public final String version;

  public BulletinMessage(ExportedMessage exportedMessage, String organization, //
      String forNameGroup, String fromNameGroup, String bulletinNumber, //
      String formDateTime, String precedence, String formSubject, String bulletinText, //
      String version) {
    super(exportedMessage);
    this.organization = organization;
    this.forNameGroup = forNameGroup; // to
    this.fromNameGroup = fromNameGroup;
    this.bulletinNumber = bulletinNumber;
    this.formDateTime = formDateTime;
    this.precedence = precedence;
    this.formSubject = subject;
    this.bulletinText = bulletinText;
    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "Organization", "For Name/Group", "From Name/Group", "Bulletin Number", "Form Date/Time", //
        "Precedence", "Bulletin Text", "Version", "File Name" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    // prefer form location to message location
    var location = mapLocation;
    var lat = location == null ? "" : location.getLatitude();
    var lon = location == null ? "" : location.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, //
        lat, lon, organization, forNameGroup, fromNameGroup, bulletinNumber, formDateTime, //
        precedence, bulletinText, version, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.BULLETIN;
  }

  @Override
  public String getMultiMessageComment() {
    return bulletinText;
  }
}
