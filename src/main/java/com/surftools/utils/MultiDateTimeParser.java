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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MultiDateTimeParser {
  private List<DateTimeFormatter> formatters;

  public MultiDateTimeParser(List<String> patterns) {
    formatters = new ArrayList<>(patterns.size());
    for (var pattern : patterns) {
      var formatter = DateTimeFormatter.ofPattern(pattern);
      formatters.add(formatter);
    }
  }

  public LocalDateTime parse(String s) {
    for (var formatter : formatters) {
      try {
        return LocalDateTime.parse(s.trim(), formatter);
      } catch (Exception e) {
        ;
      }
    }
    throw new RuntimeException("could not parse DateTime: " + s);
  }

  public LocalDateTime parseDateTime(String s) {
    for (var formatter : formatters) {
      try {
        return LocalDateTime.parse(s.trim(), formatter);
      } catch (Exception e) {
        ;
      }
    }
    return LocalDateTime.MIN;
  }

  public LocalDate parseDate(String s) {
    for (var formatter : formatters) {
      try {
        return LocalDate.parse(s.trim(), formatter);
      } catch (Exception e) {
        ;
      }
    }
    return LocalDate.MIN;
  }

  public LocalTime parseTime(String s) {
    for (var formatter : formatters) {
      try {
        return LocalTime.parse(s.trim(), formatter);
      } catch (Exception e) {
        ;
      }
    }
    return LocalTime.MIN;
  }
}
