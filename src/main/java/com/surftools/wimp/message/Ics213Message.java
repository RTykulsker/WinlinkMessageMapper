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

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

public class Ics213Message extends ExportedMessage {
  public final String organization;
  public final String incidentName;

  public final String formFrom;
  public final String formTo;
  public final String formSubject;
  public final String formDate; // I'm not going to try to parse these
  public final String formTime; // I/m not going to try to parse this either
  public final String formMessage;
  public final String approvedBy;
  public final String position;

  public final boolean isExercise;
  public final LatLongPair formLocation;
  public final String version;
  public final String dataSource;

  public Ics213Message(ExportedMessage exportedMessage, String organization, String incidentName, //
      String formFrom, String formTo, String formSubject, String formDate, String formTime, //
      String formMessage, String approvedBy, String position, //
      boolean isExercise, LatLongPair formLocation, String version, String dataSource) {
    super(exportedMessage);
    this.organization = organization;
    this.incidentName = incidentName;

    this.formFrom = formFrom;
    this.formTo = formTo;
    this.formSubject = formSubject;
    this.formDate = formDate;
    this.formTime = formTime;

    this.formMessage = formMessage;
    this.approvedBy = approvedBy;
    this.position = position;

    this.isExercise = isExercise;
    this.formLocation = formLocation;
    this.version = version;
    this.dataSource = dataSource;

    this.mapLocation = (formLocation != null && formLocation.isValid()) ? formLocation : mapLocation;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", "Msg Location", "Form Location", //
        "Organization", "IncidentName", //
        "Form From", "Form To", "Form Subject", "Form Date", "Form Time", //
        "Form Message", "Approved By", "Position/Title", "Is Exercise", "Version", "Data Source", "File Name" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    // prefer form location to message location
    var location = formLocation;
    if (location == null || !location.isValid()) {
      location = mapLocation;
    }
    var lat = location == null ? "" : location.getLatitude();
    var lon = location == null ? "" : location.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, //
        lat, lon, mapLocation == null ? "" : mapLocation.toString(), //
        formLocation == null ? "" : formLocation.toString(), //
        organization, incidentName, //
        formFrom, formTo, formSubject, formDate, formTime, //
        formMessage, approvedBy, position, String.valueOf(isExercise), version, dataSource, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ICS_213;
  }

  @Override
  public String getMultiMessageComment() {
    return formMessage;
  }
}
