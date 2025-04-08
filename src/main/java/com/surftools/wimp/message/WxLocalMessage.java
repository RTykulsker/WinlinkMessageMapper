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

public class WxLocalMessage extends ExportedMessage {
  public final String organization;
  public final LatLongPair formLocation;
  public final LocalDateTime formDateTime;
  public final String locationString;
  public final String city;
  public final String state;
  public final String county;

  public final String temperature;
  public final String windspeed;
  public final String range;
  public final String maxGusts;
  public final String warningType;
  public final String warningField;
  public final String comments;

  public WxLocalMessage(ExportedMessage exportedMessage, String organization, LatLongPair formLocation, //
      LocalDateTime formDateTime, String locationString, String city, String state, String county, //
      String temperature, String windspeed, String range, String maxGusts, String warningType, String warningField,
      String comments) {
    super(exportedMessage);

    this.organization = organization;
    this.formLocation = formLocation;
    this.formDateTime = formDateTime;

    this.locationString = locationString;
    this.city = city;
    this.state = state;
    this.county = county;

    this.temperature = temperature;
    this.windspeed = windspeed;
    this.range = range;
    this.maxGusts = maxGusts;

    this.warningType = warningType;
    this.warningField = warningField;

    this.comments = comments;

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
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", "Organization", //
        "Location", "City", "State", "County", //
        "Temperature", "Windspeed", "Range", "Max Gusts", "Warning Type", "Warning Field", "Comments", "File Name" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var lat = mapLocation == null ? "" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, lat, lon, organization, //
        locationString, city, state, county, //
        temperature, windspeed, range, maxGusts, warningType, warningField, comments, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WX_LOCAL;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }
}
