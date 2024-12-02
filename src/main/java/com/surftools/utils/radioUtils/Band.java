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

package com.surftools.utils.radioUtils;

import java.text.DecimalFormat;

public enum Band {
  B_160M(1800, 2000, "160m"), //
  B_80M(3500, 4000, "80m"), //
  B_60M(5330.5, 5403.5, "60m"), //
  B_40M(7000, 7300, "40m"), //
  B_30M(10100, 10150, "30m"), //
  B_20M(14000, 14350, "20m"), //
  B_17M(18068, 18168, "17m"), //
  B_15M(21000, 21450, "15m"), //
  B_12M(24890, 24990, "12m"), //
  B_10M(28000, 29700, "10m"), //
  B_6M(50000, 54000, "6m"), //
  B_2M(140000, 148000, "2m"), //
  B_125CM(222000, 225000, "1.25m"), //
  B_70CM(420000, 450000, "70cm"), //
  B_33CM(902000, 928000, "33cm"), //
  B_23CM(1240000, 1300000, "23cm"), //
  B_13CM(2300000, 2450000, "13cm"); //

  private int lowerHz;
  private int upperHz;
  private String name; // with space!

  private Band(double lowerKHz, double upperKHz, String name) {
    this.lowerHz = (int) (1_000 * lowerKHz);
    this.upperHz = (int) (1_000 * upperKHz);
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public boolean isHF() {
    return this.ordinal() <= B_10M.ordinal();
  }

  public boolean isVHF() {
    return this.ordinal() >= B_6M.ordinal() && this.ordinal() <= B_125CM.ordinal();
  }

  public boolean isUHF() {
    return this.ordinal() >= B_70CM.ordinal();
  }

  public static Band of(double freq) {
    return of(freq, FreqUnit.MHZ);
  }

  public static Band of(double freq, FreqUnit unit) {
    if (unit == null) {
      throw new IllegalArgumentException("null FreqUnit");
    }

    var multiplier = unit.multiplier();
    var freqHz = freq * multiplier;

    for (var band : values()) {
      if (band.lowerHz <= freqHz && freqHz <= band.upperHz) {
        return band;
      }
    }

    return null;
  }

  public boolean isHF(int freqHz) {
    if (freqHz <= B_10M.upperHz) {
      return true;
    }
    return false;
  }

  public String range(FreqUnit unit) {
    final var defaultDecimalFormat = new DecimalFormat("#.000");
    final var longDecimalFormat = new DecimalFormat("#.0000");
    var df = !is4Digit() ? defaultDecimalFormat : longDecimalFormat;

    var multiplier = unit.multiplier();
    var lo = lowerHz / multiplier;
    var hi = upperHz / multiplier;

    return "(" + df.format(lo) + " " + unit + " to " + df.format(hi) + " " + unit + ")";
  }

  public String range() {
    return range(FreqUnit.MHZ);
  }

  private boolean is4Digit() {
    return this.equals(B_60M);
  }

}
