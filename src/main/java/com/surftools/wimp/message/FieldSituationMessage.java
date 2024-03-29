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

/**
 * necessary because the WDT made FieldSituation, FieldSituation23, FieldSituation25, etc.
 *
 * @author bobt
 *
 */
public class FieldSituationMessage extends ExportedMessage {
  public final String organization;
  public final LatLongPair formLocation;
  public final String precedence;
  public final String task;
  public final String formTo;
  public final String formFrom;
  public final String isHelpNeeded;
  public final String neededHelp;
  public final String city;
  public final String county;
  public final String state;
  public final String territory;
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
  public final String noaaStatus;
  public final String noaaComments;
  public final String additionalComments;
  public final String poc;
  public final String formVersion;

  public FieldSituationMessage(ExportedMessage exportedMessage, String organization, LatLongPair formLocation, //
      String precedence, String task, String formTo, String formFrom, //
      String isHelpNeeded, String neededHelp, //
      String city, String county, String state, String territory, //
      String landlineStatus, String landlineComments, //
      String cellPhoneStatus, String cellPhoneComments, //
      String radioStatus, String radioComments, //
      String tvStatus, String tvComments, //
      String waterStatus, String waterComments, //
      String powerStatus, String powerComments, //
      String internetStatus, String internetComments, //
      String noaaStatus, String noaaComments, //
      String additionalComments, String poc, String formVersion) {
    super(exportedMessage);
    this.organization = organization;
    this.formLocation = formLocation;
    this.precedence = precedence;
    this.task = task;
    this.formTo = formTo;
    this.formFrom = formFrom;
    this.isHelpNeeded = isHelpNeeded;
    this.neededHelp = neededHelp;
    this.city = city;
    this.county = county;
    this.state = state;
    this.territory = territory;
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
    this.noaaStatus = noaaStatus;
    this.noaaComments = noaaComments;
    this.additionalComments = additionalComments;
    this.poc = poc;
    this.formVersion = formVersion;

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Precedence", "Task", "FormTo", "FormFrom", "IsHelpNeeded", "NeededHelp", //
        "Organization", "City", "County", "State", "Territory", //
        "LandlineStatus", "LandlineComments", "CellPhoneStatus", "CellPhoneComments", "RadioStatus", "RadioComments",
        "TvStatus", "TvComments", "WaterStatus", "WaterComments", "PowerStatus", "PowerComments", "InternetStatus",
        "InternetComments", "NOAAStatus", "NOAAComments", "AdditionalComments", "POC", "FormVersion" };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        precedence, task, formTo, formFrom, isHelpNeeded, neededHelp, organization, city, county, state, territory, //
        landlineStatus, landlineComments, cellPhoneStatus, cellPhoneComments, radioStatus, radioComments, tvStatus,
        tvComments, waterStatus, waterComments, powerStatus, powerComments, internetStatus, internetComments,
        noaaStatus, noaaComments, additionalComments, poc, formVersion };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.FIELD_SITUATION;
  }

  @Override
  public String getMultiMessageComment() {
    return additionalComments;
  }
}
