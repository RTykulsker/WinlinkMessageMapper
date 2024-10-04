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

import java.util.HashMap;
import java.util.Map;

import com.surftools.wimp.core.MessageType;

public class WA_EyewarnMessage extends ExportedMessage {
  public enum ResourceType {
    BRIDGES("Bridges"), //
    CELL_TOWERS("Cell Towers"), //
    HOSPITALS("Hospitals"), //
    POWER("Power Lines/Towers"), //
    ROADS("Roads"), //
    SCHOOLS("Schools"), //
    OTHER("Other Local Damage"), //
    ;

    private final String key;

    private ResourceType(String key) {
      this.key = key;
    }

    @Override
    public String toString() {
      return key;
    }
  };

  public record Resource(ResourceType type, String statusstatus) {
  };

  public final String precedence;
  public final String isExercise;
  public final String ncs;
  public final String location;
  public final String formDateTime;
  public final String reportType;
  public final String activationType;
  public final String missionNumber;
  public final String incidentType;
  public final String numberOfZips;
  public final String totalCheckIns;
  public final String questions;
  public final String bridges;
  public final String cellTowers;
  public final String hospitals;
  public final String powerLinesTowers;
  public final String roads;
  public final String schools;
  public final String otherLocalDamage;
  public final String relayOperator;
  public final String relayReceived;
  public final String relaySent;
  public final String radioOperator;
  public final String radioReceived;

  public final Map<ResourceType, Resource> resourceMap;

  public WA_EyewarnMessage(ExportedMessage exportedMessage, //
      String precedence, String isExercise, //
      String ncs, String location, //
      String formDateTime, String reportType, String activationType, String missionNumber, //
      String incidentType, //
      String numberOfZips, String totalCheckIns, //
      String questions, //
      String bridges, String cellTowers, String hospitals, String powerLinesTowers, String roads, String schools,
      String otherLocalDamage, //
      String relayOperator, String relayReceived, String relaySent, //
      String radioOperator, String radioReceived //
  ) {
    super(exportedMessage);

    this.precedence = precedence;
    this.isExercise = isExercise;

    this.ncs = ncs;
    this.location = location;

    this.formDateTime = formDateTime;
    this.reportType = reportType;
    this.activationType = activationType;
    this.missionNumber = missionNumber;

    this.incidentType = incidentType;

    this.numberOfZips = numberOfZips;
    this.totalCheckIns = totalCheckIns;

    this.questions = questions;

    this.bridges = bridges;
    this.cellTowers = cellTowers;
    this.hospitals = hospitals;
    this.powerLinesTowers = powerLinesTowers;
    this.roads = roads;
    this.schools = schools;
    this.otherLocalDamage = otherLocalDamage;

    this.relayOperator = relayOperator;
    this.relayReceived = relayReceived;
    this.relaySent = relaySent;

    this.radioOperator = radioOperator;
    this.radioReceived = radioReceived;

    resourceMap = new HashMap<>();
    add(ResourceType.BRIDGES, bridges);
    add(ResourceType.CELL_TOWERS, cellTowers);
    add(ResourceType.HOSPITALS, hospitals);
    add(ResourceType.POWER, powerLinesTowers);
    add(ResourceType.ROADS, roads);
    add(ResourceType.SCHOOLS, schools);
    add(ResourceType.OTHER, otherLocalDamage);
  }

  private void add(ResourceType type, String status) {
    var resource = new Resource(type, status);
    resourceMap.put(type, resource);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date/Time", "Latitude", "Longitude", //
        "Precedence", "Is Exercise", //
        "NCS", "Location", //
        "Form Date/Time", "Report Type", "Activation Type", "Mission Number", //
        "Incident Type", //
        "Number of Zips", "Total Check-ins", //
        "Questions", //
        "Bridges", "Cell Towers", "Hospitals", "Power Lines/Towers", "Roads", "Schools", "Other Local Damage", //
        "Relay Operator", "Relay Received", "Relay Sent", //
        "Radio Operator", "Radio Received", //
    };
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, formDateTime, latitude, longitude, //
        precedence, isExercise, //
        ncs, location, //
        formDateTime, reportType, activationType, missionNumber, //
        incidentType, //
        numberOfZips, totalCheckIns, //
        questions, //
        bridges, cellTowers, hospitals, powerLinesTowers, roads, schools, otherLocalDamage, //
        relayOperator, relayReceived, relaySent, //
        radioOperator, radioReceived };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_EYEWARN;
  }

  @Override
  public String getMultiMessageComment() {
    return questions;
  }

  @Override
  public String toString() {
    var headers = getHeaders();
    var values = getValues();
    var sb = new StringBuilder();
    sb.append(" + {");
    var n = Math.min(headers.length, values.length);
    for (var i = 0; i < n; ++i) {
      sb.append(headers[i] + ": " + values[i] + ", ");
    }
    sb.append("}");
    return super.toString() + sb.toString();
  }
}
