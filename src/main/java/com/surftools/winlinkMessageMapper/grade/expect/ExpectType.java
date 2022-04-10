/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.grade.expect;

import java.util.ArrayList;

public enum ExpectType {

  ANY(0, "any", "no expectation, anything and everything matches"), //
  NULL(1, "null", "must be empty"), //
  NOT_NULL(2, "non null", "must not be empty"), //
  EXACT(3, "exact", "must match exactly"), //
  NOT_EXACT(4, "not exact", "must not match exactly"), //
  CI_EXACT(5, "uncased_exact", "case-independent exact"), //
  NOT_CI_EXACT(6, "not uncased exact", "must not match case-independent exact"), //
  RE(5, "re", "must match regular expression"), //
  LIST(6, "list", "must be in list"), //
  NOT_LIST(7, "not list", "must not be in list"), //
  INTEGER(8, "number", "must be an integer"), //
  INTEGER_RANGE(9, "number range", "must be an integer in range"), //
  DATE_TIME(10, "date time", "must be a date and/or time"), //
  CONTAINS(11, "contains", "must contain"), //
  STARTS_WITH(12, "starts with", "starts with"), //
  ;

  /**
   * id serves NO purpose other than to discourage re-ordering of values
   *
   * this will be needed when reading/writing counts by type
   */
  private final int id;
  private final String name;
  private final String description;

  private ExpectType(int id, String name, String description) {
    this.id = id;
    this.name = name;
    this.description = description;
  }

  public static final String getAllNames() {
    var strings = new ArrayList<String>();
    for (ExpectType type : values()) {
      strings.add(type.toString());
    }
    return String.join(", ", strings);
  }

  public static ExpectType fromName(String string) {
    for (ExpectType key : ExpectType.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  public static ExpectType fromId(int id) {
    for (ExpectType key : ExpectType.values()) {
      if (key.id == id) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return name;
  }

  public int id() {
    return id;
  }

  public String description() {
    return description;
  }

}
