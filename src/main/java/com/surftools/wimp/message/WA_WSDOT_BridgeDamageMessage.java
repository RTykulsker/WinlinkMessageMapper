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

import com.surftools.wimp.core.MessageType;

public class WA_WSDOT_BridgeDamageMessage extends ExportedMessage {
  public final boolean isExercise;
  public final String formDate;
  public final String formTime;
  public final String status;
  public final String region;
  public final String county;
  public final String route;
  public final String milepost;
  public final String bridgeNumber;
  public final String location;
  public final String inspectorName;
  public final String remarks;

  public final boolean damageApproaches;
  public final boolean damageWingWalls;
  public final boolean damageAbutments;
  public final boolean damageBentCapsAndColumns;
  public final boolean damageBearings;
  public final boolean damageBeamsGirders;
  public final boolean damageDeck;
  public final boolean damageHingeExpansionJoints;
  public final boolean damageHandRails;
  public final boolean damageUtilitiesSurroundingAreas;
  public final boolean damageSeismicRestraintDevices;

  public final String commLogSendingStation;
  public final String commLogReceivingStation;
  public final String commLogFrequencyMHz;
  public final String commLogReceivedLocal;

  public final String version;

  public WA_WSDOT_BridgeDamageMessage(ExportedMessage exportedMessage, //
      boolean isExercise, String formDate, String formTime, String status, //
      String region, String county, String route, String milepost, String bridgeNumber, //
      String location, String inspectorName, //
      String remarks,

      boolean damageApproaches, //
      boolean damageWingWalls, //
      boolean damageAbutments, //
      boolean damageBentCapsAndColumns, //
      boolean damageBearings, //
      boolean damageBeamsGirders, //
      boolean damageDeck, //
      boolean damageHingeExpansionJoints, //
      boolean damageHandRails, //
      boolean damageUtilitiesSurroundingAreas, //
      boolean damageSeismicRestraintDevices, //

      String commLogSendingStation, String commLogReceivingStation, String commLogFrequencyMHz,
      String commLogReceivedLocal,

      String version) {
    super(exportedMessage);

    this.isExercise = isExercise;
    this.formDate = formDate;
    this.formTime = formTime;
    this.status = status;
    this.region = region;
    this.county = county;
    this.route = route;
    this.milepost = milepost;
    this.bridgeNumber = bridgeNumber;
    this.location = location;
    this.inspectorName = inspectorName;
    this.remarks = remarks;

    this.damageApproaches = damageApproaches;
    this.damageWingWalls = damageWingWalls;
    this.damageAbutments = damageAbutments;
    this.damageBentCapsAndColumns = damageBentCapsAndColumns;
    this.damageBearings = damageBearings;
    this.damageBeamsGirders = damageBeamsGirders;
    this.damageDeck = damageDeck;
    this.damageHingeExpansionJoints = damageHingeExpansionJoints;
    this.damageHandRails = damageHandRails;
    this.damageUtilitiesSurroundingAreas = damageUtilitiesSurroundingAreas;
    this.damageSeismicRestraintDevices = damageSeismicRestraintDevices;

    this.commLogSendingStation = commLogSendingStation;
    this.commLogReceivingStation = commLogReceivingStation;
    this.commLogFrequencyMHz = commLogFrequencyMHz;
    this.commLogReceivedLocal = commLogReceivedLocal;

    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Is Exercise", "FormDate", "FormTime", "Status", //
        "Region", "County", "Route", "Milepost", "Bridge Number", //
        "Location", "Inspector's Name", //
        "Remarks", //

        "Bridge Approaches", //
        "Wing Walls", //
        "Bridge Abutments", //
        "Bent Caps and Columns", //
        "Bearings", //
        "Beams or Girders", //
        "Bridge Deck", //
        "Hinge or Expansion Joints", //
        "Hand Rails", //
        "Utilities and Surrounding Areas", //
        "Seismic Restraint Devices", //

        "Sending Station", "Receiving Station", "Radio Frequency", "Sent/Received", //
        "Version", "File Name" };
  }

  private String b(boolean b) {
    return b ? "Yes" : "No";
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        b(isExercise), formDate, formTime, status, //
        region, county, route, milepost, bridgeNumber, //
        location, inspectorName, //
        remarks, //

        b(damageApproaches), //
        b(damageWingWalls), //
        b(damageAbutments), //
        b(damageBentCapsAndColumns), //
        b(damageBearings), //
        b(damageBeamsGirders), //
        b(damageDeck), //
        b(damageHingeExpansionJoints), //
        b(damageHandRails), //
        b(damageUtilitiesSurroundingAreas), //
        b(damageSeismicRestraintDevices), //

        commLogSendingStation, commLogReceivingStation, commLogFrequencyMHz, commLogReceivedLocal, //
        version, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_WSDOT_BRIDGE_DAMAGE;
  }

  @Override
  public String getMultiMessageComment() {
    return remarks;
  }
}
