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

import com.surftools.wimp.core.MessageType;

public class CustomizableFormMessage extends ExportedMessage {

  private static int nDisplayEntries = 4;
  public static int MAX_ENTRIES = 30;

  public final String organization;
  public final String incidentName;
  public final String formDateTimeString;
  public final String description;
  public final String columnAName;
  public final String columnBName;
  public final String columnCName;
  public final Entry[] entryArray;
  public final String comments;
  public final String version;

  public static record Entry(int lineNumber, String assignment, String name, String method) {

    public boolean isEmpty() {
      if (assignment.isEmpty() && name.isEmpty() && method.isEmpty()) {
        return true;
      }
      return false;
    }
  }

  public CustomizableFormMessage(ExportedMessage exportedMessage, String organization, String incidentName, //
      String formDateTimeString, String description, //
      String columnAName, String columnBName, String columnCName, //
      Entry[] entryArray, String comments, String version) {
    super(exportedMessage);
    this.organization = organization;
    this.incidentName = incidentName;
    this.formDateTimeString = formDateTimeString;
    this.description = description;
    this.columnAName = columnAName;
    this.columnBName = columnBName;
    this.columnCName = columnCName;
    this.entryArray = entryArray;
    this.comments = comments;
    this.version = version;
  }

  public static int getNDisplayEntreis() {
    return nDisplayEntries;
  }

  public static void setNDisplayEntries(int nDisplayEntries) {
    CustomizableFormMessage.nDisplayEntries = nDisplayEntries;
  }

  @Override
  public String[] getHeaders() {
    final var fixedHeaders = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "Organization", "IncidentName", "Form Date/Time", "Description", //
        "Comments", "Version", "File Name" };
    final var headers = new String[] { columnAName, columnBName, columnCName };

    var resultList = new ArrayList<String>(fixedHeaders.length + (nDisplayEntries * 3));

    Collections.addAll(resultList, fixedHeaders);
    for (var i = 1; i <= nDisplayEntries; ++i) {
      for (var header : headers) {
        resultList.add(header + String.valueOf(i));
      }
    }

    return resultList.toArray(new String[resultList.size()]);
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    var lat = mapLocation == null ? "" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "" : mapLocation.getLongitude();

    var fixedValues = new String[] { messageId, from, to, subject, date, time, //
        lat, lon, organization, incidentName, formDateTimeString, description, //
        comments, version, fileName };

    var resultList = new ArrayList<String>(fixedValues.length + (nDisplayEntries * 3));

    Collections.addAll(resultList, fixedValues);

    for (var i = 1; i < nDisplayEntries; ++i) {
      var entry = entryArray[i];
      resultList.add(entry.assignment);
      resultList.add(entry.name);
      resultList.add(entry.method);
    }

    return resultList.toArray(new String[resultList.size()]);

  }

  @Override
  public MessageType getMessageType() {
    return MessageType.CUSTOMIZABLE_FORM;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
