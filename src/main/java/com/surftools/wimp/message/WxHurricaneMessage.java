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

public class WxHurricaneMessage extends ExportedMessage {
  LatLongPair formLocation;

  String status;
  String isObserver;
  String observerPhone;
  String observerEmail;

  String city;
  String county;
  String state;
  String country;

  String instrumentsUsed;
  String windSpeed;
  String gustSpeed;
  String windDirection;
  String barometricPressure;

  String comments;

  public WxHurricaneMessage(ExportedMessage exportedMessage, LatLongPair formLocation, //
      String status, String isObserver, String observerPhone, String observerEmail, //
      String city, String county, String state, String country, //
      String instrumentsUsed, String windSpeed, String gustSpeed, String windDirection, String barometricPressure, //
      String comments) {
    super(exportedMessage);
    this.formLocation = formLocation;
    this.status = status;
    this.isObserver = isObserver;
    this.observerPhone = observerPhone;
    this.observerEmail = observerEmail;

    this.city = city;
    this.county = county;
    this.state = state;
    this.country = country;

    this.instrumentsUsed = instrumentsUsed;
    this.windSpeed = windSpeed;
    this.gustSpeed = gustSpeed;
    this.windDirection = windDirection;
    this.barometricPressure = barometricPressure;

    this.comments = comments;

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Status", "IsObserver", "ObserverPhone", "ObserverEmail", //
        "City", "County", "State", "Country", //
        "InstrumentsUsed", "WindSpeed", "GustSpeed", "WindDirection", "BarometricPressure", //
        "Comments" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        status, isObserver, observerPhone, observerEmail, //
        city, county, state, country, //
        instrumentsUsed, windSpeed, gustSpeed, windDirection, barometricPressure, //
        comments };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WX_HURRICANE;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }
}
