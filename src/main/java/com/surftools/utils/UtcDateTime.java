/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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

package com.surftools.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * to get time in UTC
 */
public class UtcDateTime {

  public static LocalDateTime ofNow() {
    var fields = Instant.now().toString().split("T"); // 2025-01-22T02:27:38.304917131Z
    var dateFields = fields[0].split("-");
    var utcDate = LocalDate.of(atoi(dateFields[0]), atoi(dateFields[1]), atoi(dateFields[2]));
    var timeFields = fields[1].split("\\.")[0].split(":");
    var utcTime = LocalTime.of(atoi(timeFields[0]), atoi(timeFields[1]), atoi(timeFields[2]));
    var utcDateTime = LocalDateTime.of(utcDate, utcTime);
    return utcDateTime;
  }

  private static int atoi(String s) {
    return Integer.valueOf(s);
  }
}
