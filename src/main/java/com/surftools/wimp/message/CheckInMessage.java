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
 * the favorite message type for ETO WLT exercises
 *
 * @author bobt
 *
 */
public class CheckInMessage extends ExportedMessage {
  public final String organization;
  public final LatLongPair formLocation;
  public final LocalDateTime formDateTime;

  public final String status;
  public final String band;
  public final String mode;
  public final String comments;
  public final String version;

  public CheckInMessage(ExportedMessage exportedMessage, String organization, //
      LatLongPair formLocation, LocalDateTime formDateTime, //
      String status, String band, String mode, String comments, String version) {
    super(exportedMessage);
    this.organization = organization;
    this.formLocation = formLocation;
    this.formDateTime = formDateTime;

    this.status = status;
    this.band = band;
    this.mode = mode;
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
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", "Organization", //
        "Status", "Band", "Mode", "Comments", "Version" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, //
        date, time, latitude, longitude, organization, //
        status, band, mode, comments, version };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.CHECK_IN;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
