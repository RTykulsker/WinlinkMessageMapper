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

package com.surftools.winlinkMessageMapper.summary;

public enum HistoryCategory {
  UNDEFINED(0, "undefined"), //
  ONE_AND_DONE(1, "one and done"), //
  FIRST_TIME_LAST_TIME(2, "first time, last time"), //
  NEEDS_ENCOURAGEMENT(3, "needs encouragement"), //
  GOING_STRONG(4, "going strong"), //
  HEAVEY_HITTER(5, "heavy hitter"), //
  HUNDRED_PERCENT(6, "100%"), //
  ;

  /**
   * id serves NO purpose other than to discourage re-ordering of values
   *
   * this will be needed when reading/writing counts by type
   */
  private final int id;
  private final String text;

  private HistoryCategory(int id, String text) {
    this.id = id;
    this.text = text;
  }

  public int id() {
    return id;
  }

  @Override
  public String toString() {
    return text;
  }

  public static HistoryCategory fromId(int id) {
    for (HistoryCategory type : HistoryCategory.values()) {
      if (type.id == id) {
        return type;
      }
    }
    return null;
  }
}
