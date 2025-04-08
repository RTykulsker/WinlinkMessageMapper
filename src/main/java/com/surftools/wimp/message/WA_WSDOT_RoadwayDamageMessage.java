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

public class WA_WSDOT_RoadwayDamageMessage extends ExportedMessage {
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

  public final boolean damageCracking;
  public final boolean damageBuckling;
  public final boolean damageHeavingRolling;
  public final boolean damagePavementSeparation;
  public final boolean damageChangeInConfiguration;
  public final boolean damageChangeInSurface;
  public final boolean damageSignsVmsBridges;
  public final boolean damageDrainage;

  public final boolean geoShoulderSettlement;
  public final boolean geoSags;
  public final boolean geoMovement;
  public final boolean geoDebris;
  public final boolean geoSloughingSlopes;

  public final String commLogSendingStation;
  public final String commLogReceivingStation;
  public final String commLogFrequencyMHz;
  public final String commLogReceivedLocal;

  public final String version;

  public WA_WSDOT_RoadwayDamageMessage(ExportedMessage exportedMessage, //
      boolean isExercise, String formDate, String formTime, String status, //
      String region, String county, String route, String milepost, String bridgeNumber, //
      String location, String inspectorName, //
      String remarks,

      boolean damageCracking, //
      boolean damageBuckling, //
      boolean damageHeavingRolling, //
      boolean damagePavementSeparation, //
      boolean damageChangeInConfiguration, //
      boolean damageChangeInSurface, //
      boolean damageSignsVmsBridges, //
      boolean damageDrainage, //

      boolean geoShoulderSettlement, //
      boolean geoSags, //
      boolean geoMovement, //
      boolean geoDebris, //
      boolean geoSloughingSlopes, //

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

    this.damageCracking = damageCracking;
    this.damageBuckling = damageBuckling;
    this.damageHeavingRolling = damageHeavingRolling;
    this.damagePavementSeparation = damagePavementSeparation;
    this.damageChangeInConfiguration = damageChangeInConfiguration;
    this.damageChangeInSurface = damageChangeInSurface;
    this.damageSignsVmsBridges = damageSignsVmsBridges;
    this.damageDrainage = damageDrainage;

    this.geoShoulderSettlement = geoShoulderSettlement;
    this.geoSags = geoSags;
    this.geoMovement = geoMovement;
    this.geoDebris = geoDebris;
    this.geoSloughingSlopes = geoSloughingSlopes;

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

        "Cracking", //
        "Buckling", //
        "Heaving or rolling", //
        "Pavement separation", //
        "Change in configuration- striping or lane shift", //
        "Change in roadway surface", //
        "Signs, VMS and sign bridge", //
        "Drainage", //

        "Shoulder settlement", //
        "Sags in roadway safety features", //
        "Movement above or below tilted trees", //
        "Debris on roadway", //
        "Sloughing slopes", //

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

        b(damageCracking), //
        b(damageBuckling), //
        b(damageHeavingRolling), //
        b(damagePavementSeparation), //
        b(damageChangeInConfiguration), //
        b(damageChangeInSurface), //
        b(damageSignsVmsBridges), //
        b(damageDrainage), //

        b(geoShoulderSettlement), //
        b(geoSags), //
        b(geoMovement), //
        b(geoDebris), //
        b(geoSloughingSlopes), //

        commLogSendingStation, commLogReceivingStation, commLogFrequencyMHz, commLogReceivedLocal, //
        version, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_WSDOT_ROADWAY_DAMAGE;
  }

  @Override
  public String getMultiMessageComment() {
    return remarks;
  }
}
