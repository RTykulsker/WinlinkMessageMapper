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

public class WelfareBulletinBoardMessage extends ExportedMessage {
  public static enum DataType {
    INCIDENT_NAME("Incident Name", "incidentname"), //
    FORM_TO("To", "msgto"), //
    FORM_FROM("From", "from"), //
    MESSAGE_TYPE("Message Type", "msgtype"), //
    DATE_TIME_LOCAL("Date/Time Local", "datetime"), //
    DATE_TIME_UTC("Date/Time UTC", "utctime"), //

    MY_STATUS("My Status", "msgstatus"), //
    FORM_MESSAGE("Message", "message"), //
    MESSAGE_STATUS("Message Status", "message_status"), //
    NEXT_DATE("Next Date", "nextdate"), //
    NEXT_TIME("Next Time", "nexttime"), //

    STREET("Street", "street"), //
    CITY("City", "city"), //
    STATE("State", "state"), //
    ZIP("Zip", "zip"), //
    COUNTRY("Country", "country"), //

    RADIO_OPERATOR("Radio Operator", "callsign"), //
    COORDINATES_SAME("Coordinates are same as address", "addresssame"), //
    FORM_LATITUDE("Latitude", "maplat"), //
    FORM_LONGITUDE("Longitude", "maplon"), //
    WHAT_THREE_WORDS("What3words location", "w3w"), //

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

  public WelfareBulletinBoardMessage(ExportedMessage exportedMessage, LinkedHashMap<DataType, String> dataMap) {
    super(exportedMessage);
    this.dataMap = dataMap;
  }

  @Override
  public String[] getHeaders() {
    var prefix = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude",
        "File Name" };

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

    var prefix = new String[] { messageId, from, to, subject, date, time, latitude, longitude, fileName };

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
    return dataMap.get(DataType.MESSAGE_TYPE) != null
        && dataMap.get(DataType.MESSAGE_TYPE).equalsIgnoreCase("EXERCISE");
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WELFARE_BULLETIN_BOARD;
  }

  @Override
  public String getMultiMessageComment() {
    return dataMap.get(DataType.FORM_MESSAGE);
  }
}
