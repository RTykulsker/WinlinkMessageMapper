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

public class DyfiMessage extends ExportedMessage {

  public final String exerciseId;
  public final boolean isRealEvent;
  public final boolean isFelt;

  public final LocalDateTime formDateTime;
  public final String location;
  public final LatLongPair formLocation;

  // LOW DetailLevel
  public final String response;
  public final String comments;
  public final String intensity;
  public final String formVersion;

  // MEDIUM DetailLevel;

  // HIGH DetailLevel

  public DyfiMessage(ExportedMessage exportedMessage, //
      String exerciseId, boolean isRealEvent, boolean isFelt, //
      LocalDateTime formDateTime, String location, LatLongPair formLocation, //
      String response, String comments, String intensity, String formVersion) {
    super(exportedMessage);
    this.exerciseId = exerciseId;
    this.isRealEvent = isRealEvent;
    this.isFelt = isFelt;

    this.formDateTime = formDateTime;
    this.location = location;
    this.formLocation = formLocation;

    this.response = response;
    this.comments = comments;
    this.intensity = intensity;
    this.formVersion = formVersion;

    if (formLocation.isValid()) {
      mapLocation = formLocation;
    } else {
      mapLocation = msgLocation;
    }

    if (formDateTime != null) {
      sortDateTime = formDateTime;
    }
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "ExerciseId", "Location", "IsRealEvent", "IsFelt", "Response", "Comments", "Intensity", "FormVersion" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var lat = mapLocation == null ? "" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, //
        lat, lon, //
        exerciseId, location, Boolean.toString(isRealEvent), Boolean.toString(isFelt), response, comments, intensity,
        formVersion };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.DYFI;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
