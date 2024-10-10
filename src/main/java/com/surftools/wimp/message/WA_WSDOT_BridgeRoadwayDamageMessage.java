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
  public static enum DataType {
    IS_EXERCISE("Is Exercise", "isexercise"), //
    FORM_DATE("Form Date", "inspectdate"), //
    FORM_TIME("Form Time", "inspecttime"), //
    STATUS("Status of Bridge or Roadway", "status"), //
    REGION("Region", "region"), //
    COUNTY("County", "county"), //
    ROUTE("Route", "route"), //
    MILEPOST("Milepost", "milepost"), //
    BRIDGE_NUMBER("Bridge Number (if applicable", "direction"), //
    LOCATION("Location", "location"), //
    INSPECTOR("Inspector's Name", "inspector"), //
    REMARKS("Remarks", "remarks"), //

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
    SINKHOLE("Sinkhole", "btna40"), //

    SENDING_STATION("Sending Station", "sendingstation"), //
    RECEIVING_STATION("Receiving Station", "receiving station"), //
    RADIO_FREQUENCY("Radio frequency", "freq"), //
    SEND_RECEIVE_TIME("Sent/Received", "timesend"), //

    VERSION("Version", "templateversion") //
    ;

    private final String label;
    private final String fieldName;

    private DataType(String label) {
      this.label = label;
      this.fieldName = null;
    }

    private DataType(String label, String fieldName) {
      this.label = label;
      this.fieldName = fieldName;
    }

    public String getFieldName() {
      return fieldName;
    }

    @Override
    public String toString() {
      return label;
    }
  };

  private final LinkedHashMap<DataType, String> dataMap;

  public WA_WSDOT_BridgeRoadwayDamageMessage(ExportedMessage exportedMessage, //
      LinkedHashMap<DataType, String> dataMap) {
    super(exportedMessage);
    this.dataMap = dataMap;
  }

  @Override
  public String[] getHeaders() {
    var prefix = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude" };

    var dataHeaders = Arrays
        .stream(DataType.values())
          .map(dt -> dt.toString())
          .collect(Collectors.toList())
          .toArray(new String[0]);

    return Stream.of(prefix, dataHeaders).flatMap(Stream::of).toArray(String[]::new);
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    var prefix = new String[] { messageId, from, to, subject, date, time, latitude, longitude };

    var dataValues = dataMap.values().toArray(new String[0]);

    return Stream.of(prefix, dataValues).flatMap(Stream::of).toArray(String[]::new);
  }

  public String getDataAsString(DataType key) {
    return dataMap.get(key);
  }

  public boolean getDataAsBoolean(DataType key) {
    var value = dataMap.get(key);
    if (value != null && value.equals("checkbox")) {
      return true;
    }
    return false;
  }

  public boolean isExercise() {
    return dataMap.get(DataType.IS_EXERCISE) != null
        && dataMap.get(DataType.IS_EXERCISE).equalsIgnoreCase("** THIS IS AN EXERCISE **");
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_WSDOT_BRIDGE_ROADWAY_DAMAGE;
  }

  @Override
  public String getMultiMessageComment() {
    return dataMap.get(DataType.REMARKS);
  }
}
