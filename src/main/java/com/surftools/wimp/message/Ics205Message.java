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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

/**
 * the ICS 205 Radio Plan
 *
 * NOTE WELL that dateTimePrepared, dateTimeApproved can and should be of type LocalDateTime
 *
 * NOTE WELL that iapPage can and should be of type int
 *
 * they are NOT because I don't want failures in the message parser to reject the message, I'll leave that to the
 * grader, if any
 *
 * @author bobt
 *
 */
public class Ics205Message extends ExportedMessage {

  public final String organization;
  public final String incidentName;
  public final String dateTimePrepared;
  public final String dateFrom;
  public final String dateTo;
  public final String timeFrom;
  public final String timeTo;
  public final String specialInstructions;
  public final String approvedBy;
  public final String approvedDateTime;
  public final String iapPage;
  public final String version;

  public final List<RadioEntry> radioEntries;

  public static final int MAX_RADIO_ENTRIES = 10;
  public static int radioEntriesToDisplay = MAX_RADIO_ENTRIES;

  // because Google MyMaps has a limit of 50 columns -- who knew!
  private static boolean outputRadioEntriesAsSingleValue = true;

  // because Google MyMaps doesn't deal well will blanks or 360
  private static boolean mapBadLocationsToZeroZero = true;

  public Ics205Message(ExportedMessage exportedMessage, String organization, String incidentName,
      String dateTimePrepared, String dateFrom, String dateTo, String timeFrom, String timeTo,
      String specialInstructions, String approvedBy, String approvedDateTime, String iapPage,
      List<RadioEntry> radioEntries, String version) {
    super(exportedMessage);

    this.organization = organization;
    this.incidentName = incidentName;
    this.dateTimePrepared = dateTimePrepared;
    this.dateFrom = dateFrom;
    this.dateTo = dateTo;
    this.timeFrom = timeFrom;
    this.timeTo = timeTo;
    this.specialInstructions = specialInstructions;
    this.approvedBy = approvedBy;
    this.approvedDateTime = approvedDateTime;
    this.iapPage = iapPage;
    this.radioEntries = radioEntries;
    this.version = version;
  }

  public static String[] getStaticHeaders() {
    var headers = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "Organization", "Incident Name", "Date/Time Prepared", //
        "OP Date From", "OP Date To", "OP Time From", "OP Time To", //
        "Special Instructions", "Approved By", //
        "Approved Date/Time", "IAP Page", //
        "Version", "File Name" };

    var radioHeaders = new ArrayList<String>();
    for (int i = 1; i <= radioEntriesToDisplay; i++) {
      if (outputRadioEntriesAsSingleValue) {
        radioHeaders.add("line " + i);
      } else {
        for (var entry : RadioEntry.getNames()) {
          radioHeaders.add(entry + "-" + i);
        }
      }
    }

    return Stream.concat(Arrays.stream(headers), radioHeaders.stream()).toArray(String[]::new);
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    var latitude = "";
    var longitude = "";
    if (mapBadLocationsToZeroZero) {
      if (mapLocation == null || !mapLocation.isValid()) {
        mapLocation = LatLongPair.ZERO_ZERO;
      }
    }
    latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    var values = new String[] { messageId, from, to, subject, date, time, //
        latitude, longitude, //
        organization, incidentName, dateTimePrepared, //
        dateFrom, dateTo, timeFrom, timeTo, //
        specialInstructions, approvedBy, approvedDateTime, //
        iapPage, version, fileName };

    var radioValues = new ArrayList<String>();
    for (int i = 0; i < radioEntriesToDisplay; i++) {
      if (outputRadioEntriesAsSingleValue) {
        radioValues.add(radioEntries.get(i).toString());
      } else {
        radioValues.addAll(radioEntries.get(i).getValuesAsList());
      }
    }

    return Stream.concat(Arrays.stream(values), radioValues.stream()).toArray(String[]::new);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ICS_205;
  }

  @Override
  public String getMultiMessageComment() {
    return specialInstructions;
  };

  public static int getRadioEntriesToDisplay() {
    return radioEntriesToDisplay;
  }

  public static void setRadioEntriesToDisplay(int radioEntriesToDisplay) {
    Ics205Message.radioEntriesToDisplay = radioEntriesToDisplay;
  }

  public record RadioEntry(int rowNumber, //
      String zoneGroup, String channelNumber, String function, String channelName, String assignment, //
      String rxFrequency, String rxNarrowWide, String rxTone, //
      String txFrequency, String txNarrowWide, String txTone, //
      String mode, String remarks) {

    public static RadioEntry EMPTY = new RadioEntry(0, "", "", "", "", "", "", "", "", "", "", "", "", "");

    public String[] getValues() {
      return new String[] { String.valueOf(rowNumber), //
          zoneGroup, channelNumber, function, channelName, assignment, //
          rxFrequency, rxNarrowWide, rxTone, //
          txFrequency, txNarrowWide, txTone, //
          mode, remarks };
    }

    public List<String> getValuesAsList() {
      return Arrays.asList(getValues());
    }

    public boolean isEmpty() {
      var values = getValues();
      for (var i = 1; i < values.length; ++i) {
        var field = values[i];
        if (field != null && !field.isEmpty()) {
          return false;
        }
      }
      return true;
    }

    private static String[] names = { "Row", //
        "Zone/Group", "Channel", "Function", "Channel Name", "Assignment", //
        "Rx Freq", "Rx N/W", "Rx Tone", //
        "Tx Freq", "Tx N/W", "Tx Tone", //
        "Mode(A/D/M)", "Remarks" };

    public static List<String> getNames() {
      return new ArrayList<String>(Arrays.asList(names));
    }

    public static String getName(int index) {
      if (index < 0 || index >= names.length) {
        throw new IllegalArgumentException("index: " + index + " out of range");
      }
      return names[index];
    }
  }
}
