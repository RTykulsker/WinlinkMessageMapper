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

package com.surftools.wimp.persistence.dto;

import java.time.LocalDate;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;

/**
 * a User who has participated in only one previous exercise
 */
public record OneAndDone(//
    String call, //
    String name, //
    LocalDate date, //
    LatLongPair location, //
    String exerciseType, //
    String exerciseName, //
    String exerciseDescription, //
    int feedbackCount, //
    String feedback, //
    long userIx, //
    long exerciseIdx, //
    long eventIdx) implements IWritableTable {

  public static OneAndDone FromJoinedUser(JoinedUser j) {
    var u = j.user;
    var ev = j.events.get(0);
    var ex = j.exercises.get(0);
    return new OneAndDone(u.call(), u.name(), ex.date(), ev.location(), ex.type(), ex.name(), ex.description(),
        ev.feedbackCount(), ev.feedback(), u.id(), ex.id(), ev.id());
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "Call", "Name", "Date", "Latitude", "Longitude", "Type", "Ex Name", "Description",
        "FeedbackCount", "Feedback" };
  }

  @Override
  public String[] getValues() {
    return new String[] { call, name, date.toString(), location.getLatitude(), location.getLongitude(), exerciseType,
        exerciseName, exerciseDescription, String.valueOf(feedbackCount), feedback };
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (OneAndDone) other;
    var cmp = date.compareTo(o.date);
    if (cmp != 0) {
      return cmp;
    }

    cmp = exerciseType.compareTo(o.exerciseType);
    if (cmp != 0) {
      return cmp;
    }

    return call.compareTo(o.call);
  }
}
