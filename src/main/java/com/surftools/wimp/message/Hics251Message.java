/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

// support HICS 251 Facility System Status Report message

public class Hics251Message extends ExportedMessage {

  public static final List<String> SYSTEM_NAMES = List
      .of("Power", "Lighting", "Water", "Sewer/Toilet", "Nurse Call", "Medical Gas", "Communications/IT");

  public enum StatusType {
    FULL("Fully functional", "Fully Functional"), //
    PART("Partially functional", "Partially Functional"), //
    NON("Nonfunctional", "Non Functional"), //
    NA("N/A", "N/A");

    private String input; // what we parse from exported messages
    private String output; // what we present to output

    StatusType(String input, String output) {
      this.input = input;
      this.output = output;
    }

    @Override
    public String toString() {
      return output;
    }

    public static List<String> toList() {
      var enumList = Arrays.asList(values());
      var stringList = enumList.stream().map(s -> s.toString()).toList();
      return stringList;
    }

    public static StatusType parse(String string) {
      for (var key : StatusType.values()) {
        if (key.input.equals(string)) {
          return key;
        }
      }
      return null;
    }
  };

  public record StatusEntry(String system, StatusType status, String comments) {
  };

  public final String incidentName;
  public final String pageNumber;
  public final String pageTotal;

  public final String operationalPeriod;
  public final String opFromDate;
  public final String opFromTime;
  public final String opToDate;
  public final String opToTime;

  public final String departmentName;
  public final String contactNumber;

  public final String streetAddress;
  public final String city;
  public final String state;
  public final String zip;

  public final List<StatusEntry> statusEntries;

  public final String remarks;

  public final String preparedBy;
  public final String formDateTime;
  public final String facilityName;

  public final String radioOperator;
  public final LatLongPair formLocation;

  // TODO exportedMessage? especially in EtoPractice
  public final String formVersion;
  public final String expressVersion;

  public Hics251Message(ExportedMessage exportedMessage, //
      String incidentName, String pageNumber, String pageTotal, //
      String operationalPeriod, String opFromDate, String opFromTime, String opToDate, String opToTime, //
      String departmentName, String contactNumber, //
      String streetAddress, String city, String state, String zip, //
      List<StatusEntry> statusEntries, //
      String preparedBy, String formDateTime, String facilityName, //
      String remarks, //
      String radioOperator, LatLongPair formLocation, //
      String formVersion, String expressVersion) {

    super(exportedMessage);

    this.incidentName = incidentName;
    this.pageNumber = pageNumber;
    this.pageTotal = pageTotal;

    this.operationalPeriod = operationalPeriod;
    this.opFromDate = opFromDate;
    this.opFromTime = opFromTime;
    this.opToDate = opToDate;
    this.opToTime = opToTime;

    this.departmentName = departmentName;
    this.contactNumber = contactNumber;

    this.streetAddress = streetAddress;
    this.city = city;
    this.state = state;
    this.zip = zip;

    this.statusEntries = statusEntries;

    this.preparedBy = preparedBy;
    this.formDateTime = formDateTime;
    this.facilityName = facilityName;

    this.remarks = remarks;

    this.radioOperator = radioOperator;
    this.formLocation = formLocation;

    this.formVersion = formVersion;
    this.expressVersion = expressVersion;
  }

  public static String[] getStaticHeaders() {

    var firstList = List
        .of("MessageId", "From", "To", "Subject", "Date", "Time", "Msg Location", //
            "Incident Name", "Page #", "Page Total", //
            "Op Period #", "Op From Date", "Op From Time", "Op To Date", "Op To Time", //
            "Department Name", "Contact Number", //
            "Street Address", "City", "State", "Zip" //
        );

    var statusList = new ArrayList<String>();
    for (var systemName : SYSTEM_NAMES) {
      statusList.add(systemName + " Status");
      statusList.add(systemName + " Comments");
    }

    var lastList = List
        .of("Remarks", //
            "Prepared By", "Form Date/Time", "Facility Name", //
            "Radio Operator", "Form Latitude", "Form Longitude", //
            "Form Version", "Express Version", "File Name");

    var resultList = new ArrayList<String>(firstList.size() + statusList.size() + lastList.size());
    resultList.addAll(firstList);
    resultList.addAll(statusList);
    resultList.addAll(lastList);
    return resultList.toArray(new String[0]);
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    var firstList = List
        .of(messageId, from, to, subject, date, time, msgLocation == null ? "" : msgLocation.toString(), //
            incidentName, pageNumber, pageTotal, //
            operationalPeriod, opFromDate, opFromTime, opToDate, opToTime, //
            departmentName, contactNumber, //
            streetAddress, city, state, zip //
        );

    var statusList = new ArrayList<String>();
    for (var statusEntry : statusEntries) {
      statusList.add(statusEntry.status.toString());
      statusList.add(statusEntry.comments);
    }

    var lastList = List
        .of(remarks, //
            preparedBy, formDateTime, facilityName, //
            radioOperator, formLocation.getLatitude(), formLocation.getLongitude(), //
            formVersion, expressVersion, fileName);

    var resultList = new ArrayList<String>(firstList.size() + statusList.size() + lastList.size());
    resultList.addAll(firstList);
    resultList.addAll(statusList);
    resultList.addAll(lastList);
    return resultList.toArray(new String[0]);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HICS_251;
  }

  @Override
  public String getMultiMessageComment() {
    return remarks;
  }
}
