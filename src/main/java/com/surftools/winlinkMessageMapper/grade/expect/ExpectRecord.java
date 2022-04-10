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

package com.surftools.winlinkMessageMapper.grade.expect;

public record ExpectRecord(String variable, boolean isId, ExpectType expect, String value, double points) {

  public static ExpectRecord parse(String[] fields) {
    if (fields.length < 5) {
      throw new RuntimeException("not enough fields: " + String.join(",", fields));
    }

    try {
      var variable = fields[0];
      var isId = Boolean.valueOf(fields[1]);
      var expect = parseExpectType(fields[2]);
      var value = fields[3];
      var points = Double.valueOf(fields[4]);
      return new ExpectRecord(variable, isId, expect, value, points);
    } catch (Exception e) {
      throw new RuntimeException("exception parsing: " + String.join(",", fields) + ", " + e.getLocalizedMessage());
    }
  }

  public static ExpectType parseExpectType(String field) {
    var expectType = ExpectType.fromName(field);
    if (expectType != null) {
      return expectType;
    }

    int id = Integer.valueOf(field);
    expectType = ExpectType.fromId(id);
    return expectType;
  }

}
