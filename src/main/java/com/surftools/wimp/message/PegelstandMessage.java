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
import java.time.format.DateTimeFormatter;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

public class PegelstandMessage extends ExportedMessage {

  public final String formDateTime;
  public final String reportStatus;

  public final String sender;
  public final String senderIsObserver;

  public final String observerEmail;
  public final String observerPhone;

  public final String city;
  public final String region;
  public final String stateOfFederal;

  public final String state;

  public final String formLatitude;
  public final String formLongitude;
  public final String formMGRS;
  public final String formGrid;

  public final String measuredValues;
  public final String measurementLocationNumber;

  public final String speed;
  public final String speedUnits;
  public final String volume;
  public final String volumeUnits;
  public final String trend;

  public final String water;
  public final String waterUnits;

  public final String comments;

  public final String formVersion;

  public PegelstandMessage(ExportedMessage exportedMessage, //
      String formDateTime, String reportStatus, //
      String sender, String senderIsObserver, //
      String observerEmail, String observerPhone, //
      String city, String region, String stateOfFederal, //
      String state, //
      String formLatitude, String formLongitude, String formMGRS, String formGrid, //
      String measuredValues, String measurementLocationNumber, //
      String speed, String speedUnits, String volume, String volumeUnits, String trend, //
      String water, String waterUnits, //
      String comments, //
      String formVersion) {
    super(exportedMessage);

    this.formDateTime = formDateTime;
    this.reportStatus = reportStatus;

    this.sender = sender;
    this.senderIsObserver = senderIsObserver;

    this.observerEmail = observerEmail;
    this.observerPhone = observerPhone;

    this.city = city;
    this.region = region;
    this.stateOfFederal = stateOfFederal;
    this.state = state;

    this.formLatitude = formLatitude;
    this.formLongitude = formLongitude;
    this.formMGRS = formMGRS;
    this.formGrid = formGrid;

    this.measuredValues = measuredValues;
    this.measurementLocationNumber = measurementLocationNumber;

    this.speed = speed;
    this.speedUnits = speedUnits;
    this.volume = volume;
    this.volumeUnits = volumeUnits;
    this.trend = trend;

    this.water = water;
    this.waterUnits = waterUnits;

    this.comments = comments;

    this.formVersion = formVersion;

    var formLocation = new LatLongPair(formLatitude, formLongitude);
    if (formLocation.isValid()) {
      mapLocation = formLocation;
    } else {
      mapLocation = msgLocation;
    }

    if (formDateTime != null) {
      try {
        final var DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss'Z'");
        var dt = LocalDateTime.parse(formDateTime, DTF);
        sortDateTime = dt;
      } catch (Exception e) {
        ;
      }
    }

  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //

        "Form Date/Time", "Report Status", //

        "Sender", "IsSenderObserver", //

        "ObserverEmail", "ObserverPhone", //

        "City", "Region", "StateOfFederal", "State", //

        "Form LAT", "Form LON", "MGRS", "Grid", //

        "Measured Values", "Measurement location/number", //

        "Speed", "Speed Units", "Volume", "Volume Units", "Trend", //

        "Water Level", "Water Level Units", //

        "Comments", "Form Version", "File Name"//
    };

  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var lat = mapLocation == null ? "" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, //
        lat, lon, //
        formDateTime, reportStatus, //
        sender, senderIsObserver, //
        observerEmail, observerPhone, //
        city, region, stateOfFederal, state, //
        formLatitude, formLongitude, formMGRS, formGrid, //
        measuredValues, measurementLocationNumber, //
        speed, speedUnits, volume, volumeUnits, trend, //
        water, waterUnits, //
        comments, formVersion, fileName };

  }

  @Override
  public MessageType getMessageType() {
    return MessageType.PEGELSTAND;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
