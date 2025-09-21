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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surftools.wimp.core.MessageType;

public class Ics309Message extends ExportedMessage {

  private static int nDisplayActivities = 30;

  public final String organization;
  public final String taskNumber;
  public final String dateTimePrepared;
  public final String operationalPeriod;
  public final String taskName;
  public final String operatorName;
  public final String stationId;
  public final String incidentName;
  public final String page;
  public final String version;
  public final List<Activity> activities;

  public static record Activity(String dateTimeString, String from, String to, String subject) {
    public boolean isValid() {
      if (dateTimeString != null && !dateTimeString.isEmpty()) {
        return true;
      }

      if (from != null && !from.isEmpty()) {
        return true;
      }

      if (to != null && !to.isEmpty()) {
        return true;
      }

      if (subject != null && !subject.isEmpty()) {
        return true;
      }

      return false;
    }
  };

  public Ics309Message(ExportedMessage exportedMessage, String organization, String taskNumber, String dateTimePrepared,
      String operationalPeriod, String taskName, String operatorName, String stationId, String incidentName,
      String page, String version, List<Activity> activities) {
    super(exportedMessage);
    this.organization = organization;
    this.taskNumber = taskNumber;
    this.dateTimePrepared = dateTimePrepared;
    this.operationalPeriod = operationalPeriod;
    this.taskName = taskName;
    this.operatorName = operatorName;
    this.stationId = stationId;
    this.incidentName = incidentName;
    this.page = page;
    this.version = version;
    this.activities = activities;
  }

  public static int getNDisplayActivities() {
    return nDisplayActivities;
  }

  public static void setNDisplayActivities(int nDisplayActivities) {
    Ics309Message.nDisplayActivities = nDisplayActivities;
  }

  public static String[] getStaticHeaders() {
    final var fixedHeaders = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "Organization", "Task Number", "Date/Time Prepared", "Operational Period", //
        "Task Name", "Operator Id", "Station Id", "Incident Name", "Page", "Version", "# of Activities", "File Name" };

    final var activityHeaders = new String[] { "DateTime", "From", "To", "Subject" };

    var resultList = new ArrayList<String>(fixedHeaders.length + (nDisplayActivities * 4));
    Collections.addAll(resultList, fixedHeaders);

    for (var i = 1; i <= nDisplayActivities; ++i) {
      for (var header : activityHeaders) {
        resultList.add(header + String.valueOf(i));
      }
    }

    return resultList.toArray(new String[resultList.size()]);
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    var lat = mapLocation == null ? "0" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "0" : mapLocation.getLongitude();

    var fixedValues = new String[] { messageId, from, to, subject, date, time, //
        lat, lon, organization, taskNumber, dateTimePrepared, operationalPeriod, taskName, operatorName, stationId,
        incidentName, page, version, String.valueOf(activities.size()), fileName };

    var resultList = new ArrayList<String>(fixedValues.length + (nDisplayActivities * 4));

    Collections.addAll(resultList, fixedValues);

    for (var i = 0; i < nDisplayActivities; ++i) {
      if (i < activities.size()) {
        var r = activities.get(i);
        if (r != null) {
          resultList.add(r.dateTimeString == null ? "" : r.dateTimeString);
          resultList.add(r.from == null ? "" : r.from);
          resultList.add(r.to == null ? "" : r.to);
          resultList.add(r.subject == null ? "" : r.subject);
        } else {
          resultList.add("");
          resultList.add("");
          resultList.add("");
          resultList.add("");
        }
      } else {
        resultList.add("");
        resultList.add("");
        resultList.add("");
        resultList.add("");
      }
    }

    return resultList.toArray(new String[resultList.size()]);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ICS_309;
  }

  @Override
  public String getMultiMessageComment() {

    var sb = new StringBuilder();
    sb
        .append("name: " + operatorName + ", task#: " + taskNumber + "(" + taskName + ")" + ", op period: "
            + operationalPeriod);
    sb.append(",  activities: " + activities.size());
    return sb.toString();
  }

}
