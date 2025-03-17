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

package com.surftools.wimp.core;

/**
 * reasons for rejecting a message
 *
 * @author bobt
 *
 */
public enum RejectType {

  WRONG_MESSAGE_TYPE(0, "wrong message type"), //
  EXPLICIT_LOCATION(1, "explicit bad location"), //
  PROCESSING_ERROR(2, "unknown server error"), //
  EXPLICIT_OTHER(3, "explicit other reasons"), //
  SAME_LOCATION(4, "same location"), //
  CANT_PARSE_LATLONG(5, "can't parse lat/lon"), //
  CANT_PARSE_DYFI_JSON(6, "can't parse DYFI json"), //
  NO_RECIPIENT(7, "no recipient"), //
  CANT_PARSE_MIME(8, "can't parse MIME"), //
  SAME_CALL(9, "same call, earlier time"), //
  UNSUPPORTED_TYPE(10, "unsupported type"), //
  CANT_PARSE_ETO_JSON(11, "can't parse ETO json"), //
  CANT_PARSE_DATE_TIME(12, "can't parse date/time"), //
  CANT_FIND_FORMDATA(13, "can't find FormData in context");

  /**
   * id serves NO purpose other than to discourage re-ordering of values
   *
   * this will be needed when reading/writing counts by type
   */
  private final int id;
  private final String text;

  private RejectType(int id, String text) {
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

  public static RejectType fromId(int id) {
    for (RejectType type : RejectType.values()) {
      if (type.id == id) {
        return type;
      }
    }
    return null;
  }
}
