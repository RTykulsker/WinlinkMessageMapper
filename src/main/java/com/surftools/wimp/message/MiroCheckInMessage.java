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

public class MiroCheckInMessage extends ExportedMessage {

  public final LocalDateTime formDateTime;
  public final LatLongPair formLocation;

  public final String power;
  public final String band;
  public final String mode;
  public final String radio;
  public final String antenna;
  public final String portable;

  public final String comments;
  public final String version;

  public MiroCheckInMessage(ExportedMessage exportedMessage, LocalDateTime formDateTime, LatLongPair formLocation,
      String power, String band, String mode, String radio, String antenna, String portable, String comments,
      String version) {
    super(exportedMessage);

    this.formDateTime = formDateTime;
    this.formLocation = formLocation;

    this.power = power;
    this.band = band;
    this.mode = mode;
    this.radio = radio;
    this.antenna = antenna;
    this.portable = portable;

    this.comments = comments;
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
    return new String[] { "MessageId", "From", "Date", "Time", "Latitude", "Longitude", //
        "Power", "Band", "Mode", "Radio", "Antenna", "Portable", //
        "Comments", "Version" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, date, time, latitude, longitude, //
        power, band, mode, radio, antenna, portable, comments, //
        version };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.MIRO_CHECK_IN;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
