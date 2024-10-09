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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.surftools.wimp.core.MessageType;

public class WA_WSDOT_BridgeRoadwayDamageMessage extends ExportedMessage {
  public static enum DamageType {
    APPROACHES("Bridge Approaches", "btna14"), //
    WING_WALLS("Wing Walls", "btna15"), //
    ABUTMENTS("Bridge Abutments", "btna16"), //
    BENT_CAPS("Bent Caps, and Columns", "btna17"), //
    BEARINGS("Bearings", "btna18"), //
    BEAMS("Beams or Girders", "btna19"), //
    DECK("Deck (top and underside)", "btna20"), //
    HINGE("Hinge or Expansion Joints", "btna21"), //
    RAILINGS("Railings, Parapet and Curb", "btna22"), //
    UTILITIES("Utilities and Surrounding Areas", "btna23"), //
    DEVICES("Seismic Restraint Devices", "btna24"), //
    DEBRIS_ACCUMULATION("Debris Accumulation in Channel", "btna25"), //
    FOOTING("Undermined Footings", "btna26"), //

    CRACKING("Cracking", "btna27"), //
    BUCKLING("Buckling", "btna28"), //
    HEAVING("Heaving or rolling", "btna29"), //
    SEPARATION("Pavement separation", "btna30"), //
    CONFIGURATION("Change in configuration", "btna31"), //
    SURFACE("Change in roadway surface", "btna32"), //
    SIGNS("Signs, VMS and sign bridges", "btna33"), //

    SETTLEMENT("Shoulder settlement", "btna34"), //
    SAGS("Sags in roadway safety features", "btna35"), //
    MOVEMENT("Movement above or below tilted trees", "btna36"), //
    ROADWAY_DEBRIS("Debris on Roadway", "btna37"), //
    SLOPES("Sloughing slopes", "btna38"), //
    UNSTABLE("Unstable road blocks above roadway", "btna39"), //
    SINKHOLE("Sinkhole", "btna40");

    private final String label;
    private final String fieldName;

    private DamageType(String label) {
      this.label = label;
      this.fieldName = null;
    }

    private DamageType(String label, String fieldName) {
      this.label = label;
      this.fieldName = fieldName;
    }

    public String getLabel() {
      return label;
    }

    public String getFieldName() {
      return fieldName;
    }

    @Override
    public String toString() {
      return label;
    }
  };

  private final String isExercise;
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

  private final LinkedHashMap<DamageType, String> damageMap;

  public final String commLogSendingStation;
  public final String commLogReceivingStation;
  public final String commLogFrequencyMHz;
  public final String commLogReceivedLocal;

  public final String version;

  public WA_WSDOT_BridgeRoadwayDamageMessage(ExportedMessage exportedMessage, //
      String isExercise, String formDate, String formTime, String status, //
      String region, String county, String route, String milepost, String bridgeNumber, //
      String location, String inspectorName, //
      String remarks,

      LinkedHashMap<DamageType, String> damageMap, //

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

    this.damageMap = damageMap;

    this.commLogSendingStation = commLogSendingStation;
    this.commLogReceivingStation = commLogReceivingStation;
    this.commLogFrequencyMHz = commLogFrequencyMHz;
    this.commLogReceivedLocal = commLogReceivedLocal;

    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    var prefix = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Is Exercise", "FormDate", "FormTime", "Status", //
        "Region", "County", "Route", "Milepost", "Bridge Number", //
        "Location", "Inspector's Name", //
        "Remarks" }; //

    var damageHeaders = Arrays
        .stream(DamageType.values())
          .map(dt -> dt.getLabel())
          .collect(Collectors.toList())
          .toArray(new String[0]);

    var suffix = new String[] { "Sending Station", "Receiving Station", "Radio Frequency", "Sent/Received", //
        "Version" };

    var headers = Stream.of(prefix, damageHeaders, suffix).flatMap(Stream::of).toArray(String[]::new);
    return headers;
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    var prefix = new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        isExercise, formDate, formTime, status, //
        region, county, route, milepost, bridgeNumber, //
        location, inspectorName, //
        remarks };

    var damageValues = damageMap.values().toArray(new String[0]);

    var suffix = new String[] { commLogSendingStation, commLogReceivingStation, commLogFrequencyMHz,
        commLogReceivedLocal, //
        version };

    var values = Stream.of(prefix, damageValues, suffix).flatMap(Stream::of).toArray(String[]::new);
    return values;
  }

  public String getDamageAsString(DamageType key) {
    return damageMap.get(key);
  }

  public boolean getDamageAsBoolean(DamageType key) {
    var value = damageMap.get(key);
    if (value != null && value.equals("checkbox")) {
      return true;
    }
    return false;
  }

  public boolean isExercise() {
    return isExercise != null && isExercise.equalsIgnoreCase("** THIS IS AN EXERCISE **");
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_WSDOT_BRIDGE_ROADWAY_DAMAGE;
  }

  @Override
  public String getMultiMessageComment() {
    return remarks;
  }
}
