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

public class HumanitarianNeedsMessage extends ExportedMessage {
  public final LatLongPair formLocation;
  public final String teamId;
  public final String formDate; // I'm not going to try to parse these
  public final String formTime; // I/m not going to try to parse this either
  public final String address;

  public final boolean needsHealth;
  public final boolean needsShelter;
  public final boolean needsFood;
  public final boolean needsWater;
  public final boolean needsLogistics;
  public final boolean needsOther;

  public final String description;
  public final String other;

  public final String approvedBy;
  public final String position;
  public final String version;

  public HumanitarianNeedsMessage(ExportedMessage message, LatLongPair formLocation, String teamId, //
      String formDate, String formTime, String address, //
      boolean needsHealth, boolean needsShelter, boolean needsFood, boolean needsWater, //
      boolean needsLogistics, boolean needsOther, //
      String description, String other, String approvedBy, String position, String version) {

    super(message);
    this.formLocation = formLocation;
    this.teamId = teamId;
    this.formDate = formDate;
    this.formTime = formTime;
    this.address = address;

    this.needsHealth = needsHealth;
    this.needsShelter = needsShelter;
    this.needsFood = needsFood;
    this.needsWater = needsWater;
    this.needsLogistics = needsLogistics;
    this.needsOther = needsOther;

    this.description = description;
    this.other = other;

    this.approvedBy = approvedBy;
    this.position = position;
    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", "Msg Location", "Form Location", //
        "TeamId", "Form Date", "Form Time", "Address", //
        "Needs Health", "Needs Shelter", "Needs Food", "Needs Water", //
        "Needs Logistics", "Needs Other", //
        "Description", "Other", //
        "Approved By", "Position/Title", "Version" };
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
        teamId, formDate, formTime, address, //
        String.valueOf(needsHealth), String.valueOf(needsShelter), String.valueOf(needsFood), //
        String.valueOf(needsWater), String.valueOf(needsLogistics), String.valueOf(needsOther), //
        description, other, approvedBy, position, version };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HUMANITARIAN_NEEDS;
  }

  @Override
  public String getMultiMessageComment() {
    return description + ", " + other;
  }

}
