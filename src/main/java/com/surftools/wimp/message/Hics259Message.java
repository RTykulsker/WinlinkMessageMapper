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
import java.util.List;
import java.util.Map;

import com.surftools.wimp.core.MessageType;

// support HICS 259 message

public class Hics259Message extends ExportedMessage {

  public static final List<String> CASUALTY_KEYS = List
      .of("Patients seen", "Waiting to be seen", "Admitted", "Critical care bed", "Medical/surgical bed",
          "Pediatric Bed", "Discharged", "Transferred", "Expired");

  public record CasualtyEntry(String adultCount, String childCount, String comment) {
  };

  public final String incidentName;
  public final String formDate;
  public final String formTime;

  public final String operationalPeriod;
  public final String opFromDate;
  public final String opFromTime;
  public final String opToDate;
  public final String opToTime;

  public final Map<String, CasualtyEntry> casualtyMap;

  public final String patientTrackingManager;
  public final String facilityName;

  public final String version;

  public Hics259Message(ExportedMessage exportedMessage, //
      String incidentName, String formDate, String formTime, //
      String operationalPeriod, String opFromDate, String opFromTime, String opToDate, String opToTime, //
      Map<String, CasualtyEntry> casualtyMap, //
      String patientTrackingManager, String facilityName, String version) {

    super(exportedMessage);

    this.incidentName = incidentName;
    this.formDate = formDate;
    this.formTime = formTime;

    this.operationalPeriod = operationalPeriod;
    this.opFromDate = opFromDate;
    this.opFromTime = opFromTime;
    this.opToDate = opToDate;
    this.opToTime = opToTime;

    this.casualtyMap = casualtyMap;

    this.patientTrackingManager = patientTrackingManager;
    this.facilityName = facilityName;

    this.version = version;
  }

  public static String[] getStaticHeaders() {

    var firstList = List
        .of("MessageId", "From", "To", "Subject", "Date", "Time", "Msg Location", //
            "Incident Name", "Form Date/Time", //
            "Op Period #", "Op From", "Op To" //
        );

    var casualtyList = new ArrayList<String>();
    for (var key : CASUALTY_KEYS) {
      casualtyList.add(key + ": Adult");
      casualtyList.add(key + ": Pediatric");
      casualtyList.add(key + ": Comments");
    }

    var lastList = List
        .of("Patient Tracking Manager", "Facility Name", //
            "Version", "File Name");

    var resultList = new ArrayList<String>(firstList.size() + casualtyList.size() + lastList.size());
    resultList.addAll(firstList);
    resultList.addAll(casualtyList);
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
            incidentName, formDate + " " + formTime, //
            operationalPeriod, opFromDate + " " + opFromTime, //
            opToDate + " " + opToTime);//

    var casualtyList = new ArrayList<String>();
    for (var key : CASUALTY_KEYS) {
      var entry = casualtyMap.getOrDefault(key, new CasualtyEntry("", "", ""));
      casualtyList.add(entry.adultCount);
      casualtyList.add(entry.childCount);
      casualtyList.add(entry.comment);
    }

    var lastList = List
        .of(patientTrackingManager, facilityName, //
            version, fileName);

    var resultList = new ArrayList<String>(firstList.size() + casualtyList.size() + lastList.size());
    resultList.addAll(firstList);
    resultList.addAll(casualtyList);
    resultList.addAll(lastList);
    return resultList.toArray(new String[0]);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HICS_259;
  }

  @Override
  public String getMultiMessageComment() {
    return "";
  }
}
