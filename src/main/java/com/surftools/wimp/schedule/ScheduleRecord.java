/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

package com.surftools.wimp.schedule;

import java.time.LocalDate;

import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;

public record ScheduleRecord(String name, LocalDate date, MessageType messageType, //
    boolean isPractice, String extraData, String comments) implements IWritableTable {

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ScheduleRecord) other;
    return this.date.compareTo(o.date);
  }

  @Override
  public String[] getHeaders() {
    final var headers = new String[] { "Date", "Name", "MessageType", "IsPractice", "Extra Data", "Ordinal",
        "Day Of Week", "Comments" };
    return headers;
  }

  @Override
  public String[] getValues() {
    return new String[] { date.toString(), name, messageType == null ? "" : messageType.name(),
        String.valueOf(isPractice), extraData == null ? "" : extraData, String.valueOf(getOrdinalDayOfWeek(date)),
        date.getDayOfWeek().toString(), comments };
  }

  public static ScheduleRecord from(String[] fields) {
    var messageType = MessageType.fromString(fields[2].toLowerCase());
    try {
      return new ScheduleRecord(fields[1], LocalDate.parse(fields[0]), messageType, //
          Boolean.parseBoolean(fields[3]), fields[4], fields[7]);
    } catch (Exception e) {
      return null;
    }
  }

  public static int getOrdinalDayOfWeek(LocalDate date) {
    var day = date.getDayOfMonth(); // from 1 to 31
    return (day / 7) + 1 + ((day % 7) == 0 ? -1 : 0);
  }

  public static String getOrdinalLabel(int ord) {
    var s = String.valueOf(ord);
    var unit = Integer.parseInt(s.substring(s.length() - 1));
    switch (unit) {
    case 1:
      return ord + "st";
    case 2:
      return ord + "nd";
    case 3:
      return ord + "rd";
    default:
      return ord + "th";
    }
  }

}