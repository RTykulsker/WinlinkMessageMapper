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

import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.database.IDatabaseService;

/**
 * one record per exercise
 */
public record ExerciseSummary(//
    ExerciseId exerciseId, //
    int totalMessages, //
    int uniqueParticipants) implements IWritableTable {

  @Override
  public String[] getHeaders() {
    return new String[] { "Exercise Date", "Exercise Name", "Total Messages", "Unique Participants" };
  }

  @Override
  public String[] getValues() {
    final var DB_DTF = IDatabaseService.DB_DTF;
    return new String[] { DB_DTF.format(exerciseId.date()), exerciseId.name(), s(totalMessages),
        s(uniqueParticipants) };
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ExerciseSummary) other;
    return exerciseId.compareTo(o.exerciseId);
  }
}