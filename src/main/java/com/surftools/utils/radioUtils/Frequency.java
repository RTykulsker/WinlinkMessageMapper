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

import java.math.BigDecimal;

/**
 * abstraction for a radio frequency
 */
public class Frequency {

  private BigDecimal frequencyHz;
  private String freqString;
  private FreqUnit unit;

  public boolean isValid() {
    return frequencyHz != null;
  }

  public Frequency(String freqString) {
    this(freqString, FreqUnit.MHZ);
  }

  public Frequency(String freqString, FreqUnit unit) {
    if (freqString != null && !freqString.isEmpty() && unit != null) {
      this.freqString = freqString;
      this.unit = unit;
      var multiplier = new BigDecimal(unit.multiplier());
      frequencyHz = new BigDecimal(freqString).multiply(multiplier);
    }
  }

  public int getFrequencyHz() {
    return frequencyHz.intValue();
  }

  public Frequency add(Frequency other) {
    if (other == null || !other.isValid()) {
      throw new IllegalArgumentException("other value null");
    }

    var bdValue = frequencyHz.add(other.frequencyHz);
    return new Frequency(String.valueOf(bdValue.doubleValue() / unit.multiplier()), unit);
  }

  public Frequency subtract(Frequency other) {
    if (other == null || !other.isValid()) {
      throw new IllegalArgumentException("other value null");
    }

    var bdValue = frequencyHz.subtract(other.frequencyHz);
    return new Frequency(String.valueOf(bdValue.doubleValue() / unit.multiplier()), unit);
  }

  public Band bandOf() {
    return Band.of(frequencyHz.doubleValue() / unit.multiplier(), unit);
  }

  @Override
  public String toString() {
    return freqString + " " + unit;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((frequencyHz == null) ? 0 : frequencyHz.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Frequency other = (Frequency) obj;
    if (frequencyHz == null) {
      if (other.frequencyHz != null) {
        return false;
      }
    } else if (frequencyHz.intValue() != other.frequencyHz.intValue()) {
      return false;
    }
    return true;
  }

}
