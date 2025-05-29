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

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.database.IDatabaseService;

/**
 * one row per participant
 */
public record ParticipantSummary(//
    String call, //
    LatLongPair location, //
    LocalDate firstDate, //
    LocalDate lastDate, //
    int exerciseCount, //
    int messageCount) implements IWritableTable {

  public ParticipantSummary update(ParticipantDetail pd) {
    var _call = call == null ? pd.call() : call;
    var _location = location == null ? pd.location() : location;
    var pd_date = pd.exerciseId().date();
    var _firstDate = firstDate == null ? pd_date : pd_date.isBefore(firstDate) ? pd_date : firstDate;
    var _lastDate = lastDate == null ? pd_date : pd_date.isAfter(lastDate) ? pd_date : lastDate;
    return new ParticipantSummary(_call, _location, _firstDate, _lastDate, exerciseCount + 1,
        messageCount + pd.messageCount());
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "Call", "Latitude", "Longitude", //
        "First Date", "Last Date", "Exercise Count", "Message Count" };
  }

  @Override
  public String[] getValues() {
    final var DB_DTF = IDatabaseService.DB_DTF;
    return new String[] { call, location.getLatitude(), location.getLongitude(), //
        DB_DTF.format(firstDate), DB_DTF.format(lastDate), s(exerciseCount), s(messageCount) };
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ParticipantSummary) other;
    return call.compareTo(o.call);
  }
}
