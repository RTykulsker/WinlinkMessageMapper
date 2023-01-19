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

public class SpotRepMessage extends ExportedMessage {
  public final LatLongPair formLocation;
  public final String locationString;
  public final String landlineStatus;
  public final String landlineComments;
  public final String cellPhoneStatus;
  public final String cellPhoneComments;
  public final String radioStatus;
  public final String radioComments;
  public final String tvStatus;
  public final String tvComments;
  public final String waterStatus;
  public final String waterComments;
  public final String powerStatus;
  public final String powerComments;
  public final String internetStatus;
  public final String internetComments;
  public final String additionalComments;
  public final String poc;

  public SpotRepMessage(ExportedMessage exportedMessage, LatLongPair formLocation, //
      String locationString, //
      String landlineStatus, String landlineComments, //
      String cellPhoneStatus, String cellPhoneComments, //
      String radioStatus, String radioComments, //
      String tvStatus, String tvComments, //
      String waterStatus, String waterComments, //
      String powerStatus, String powerComments, //
      String internetStatus, String internetComments, //
      String additionalComments, String poc) {
    super(exportedMessage);
    this.formLocation = formLocation;
    this.locationString = locationString;
    this.landlineStatus = landlineStatus;
    this.landlineComments = landlineComments;
    this.cellPhoneStatus = cellPhoneStatus;
    this.cellPhoneComments = cellPhoneComments;
    this.radioStatus = radioStatus;
    this.radioComments = radioComments;
    this.tvStatus = tvStatus;
    this.tvComments = tvComments;
    this.waterStatus = waterStatus;
    this.waterComments = waterComments;
    this.powerStatus = powerStatus;
    this.powerComments = powerComments;
    this.internetStatus = internetStatus;
    this.internetComments = internetComments;
    this.additionalComments = additionalComments;
    this.poc = poc;

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Location", "LandlineStatus", "LandlineComments", "CellPhoneStatus", "CellPhoneComments", "RadioStatus",
        "RadioComments", "TvStatus", "TvComments", "WaterStatus", "WaterComments", "PowerStatus", "PowerComments",
        "InternetStatus", "InternetComments", "AdditionalComments", "POC" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        locationString, landlineStatus, landlineComments, cellPhoneStatus, cellPhoneComments, radioStatus,
        radioComments, tvStatus, tvComments, waterStatus, waterComments, powerStatus, powerComments, internetStatus,
        internetComments, additionalComments, poc };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.SPOTREP;
  }

  @Override
  public String getMultiMessageComment() {
    return additionalComments;
  }
}
