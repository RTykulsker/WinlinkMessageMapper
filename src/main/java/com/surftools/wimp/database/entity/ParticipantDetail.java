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

package com.surftools.wimp.database.entity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.database.IDatabaseService;

/**
 * one row per participant, per exercise
 */
public record ParticipantDetail(//
    String call, //
    LatLongPair location, //
    ExerciseId exerciseId, //
    int messageCount, //
    String messageIds) implements IWritableTable {

  static final DateTimeFormatter DB_DTF = IDatabaseService.DB_DTF;

  public static ParticipantDetail make(String[] fields) {
    return new ParticipantDetail(fields[0], new LatLongPair(fields[1], fields[2]),
        new ExerciseId(LocalDate.parse(fields[3], DB_DTF), fields[4]), Integer.valueOf(fields[5]), fields[6]);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "Call", "Latitude", "Longitude", //
        "Date", "Name", //
        "Message Count", "Message Ids" };
  }

  @Override
  public String[] getValues() {
    return new String[] { call, location.getLatitude(), location.getLongitude(), //
        DB_DTF.format(exerciseId.date()), exerciseId.name(), //
        s(messageCount), messageIds };
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ParticipantDetail) other;
    var cmp = call.compareTo(o.call);
    if (cmp != 0) {
      return cmp;
    }
    return exerciseId.compareTo(o.exerciseId);
  }

}
