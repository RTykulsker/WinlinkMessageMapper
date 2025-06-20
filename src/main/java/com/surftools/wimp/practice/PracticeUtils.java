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

package com.surftools.wimp.practice;

import java.time.LocalDate;

public class PracticeUtils {

  /**
   * return the ordinal (1, 2, 3, 4 or 5) for the occurrence of this day's day-of-week within a month
   *
   * @param date
   * @return
   */
  public static int getOrdinalDayOfWeek(LocalDate date) {
    var day = date.getDayOfMonth(); // from 1 to 31
    return (day / 7) + 1 + ((day % 7) == 0 ? -1 : 0);
  }

  public static String getOrdinalLabel(int ord) {
    var s = String.valueOf(ord);
    var unit = Integer.parseInt(s.substring(s.length() - 1));
    switch (unit) {
    case 1:
      return ord + "st";
    case 2:
      return ord + "nd";
    case 3:
      return ord + "rd";
    default:
      return ord + "th";
    }
  }

  // public static void main(String[] args) {
  // var date = LocalDate.of(2025, 1, 1);
  // while (true) {
  // var ordinal = getOrdinalDayOfWeek(date);
  // var dow = date.getDayOfWeek();
  // if (ordinal == 5) {
  // System.out.println("date: " + date + ", dow: " + dow.toString() + ", ordinal: " + ordinal);
  // }
  // date = date.plusDays(1);
  // if (date.getYear() == 2026) {
  // break;
  // }
  // }
  // }
}
