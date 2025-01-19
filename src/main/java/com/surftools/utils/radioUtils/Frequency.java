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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * abstraction for a radio frequency
 */
public class Frequency {
  private static Logger logger = LoggerFactory.getLogger(Frequency.class);

  private static final boolean ALLOW_TWO_DECIMAL_POINTS = true;
  private static final boolean ALLOW_SMASHED_UNITS = true;
  private static final boolean ALLOW_COMMAS_INSTEAD_OF_PERIODS = true;

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

      var multiplier = new BigDecimal(unit.multiplier());

      if (ALLOW_TWO_DECIMAL_POINTS) {
        var count = freqString.length() - freqString.replaceAll("\\.", "").length();
        if (count == 2) {
          var lastIndex = freqString.lastIndexOf(".");
          var sb = new StringBuilder(freqString);
          freqString = sb.deleteCharAt(lastIndex).toString();
        }
      }

      if (ALLOW_SMASHED_UNITS) {
        if (freqString.endsWith("MHz") || freqString.endsWith("MHZ")) {
          freqString = freqString.replace("MHz", "");
          freqString = freqString.replace("MHZ", "");
        }

        if (freqString.endsWith("kHz")) {
          freqString = freqString.replace("kHz", "");
          unit = FreqUnit.HZ;
        }
      }

      if (ALLOW_COMMAS_INSTEAD_OF_PERIODS) {
        freqString = freqString.replaceAll(",", "\\.");
      }

      freqString = freqString.trim();

      try {
        frequencyHz = new BigDecimal(freqString).multiply(multiplier);
      } catch (Exception e) {
        logger.warn("could not parse frequency: " + freqString);
        frequencyHz = new BigDecimal("-1");
      }

      this.freqString = freqString;
      this.unit = unit;
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

  public boolean isAmateurFrequency() {
    return isHF() || isVHF() || isUHF();
  }

  public boolean isHF() {
    return isHF(true);
  }

  public boolean isHF(boolean hamBandsOnly) {
    if (!isValid()) {
      return false;
    }

    if (hamBandsOnly) {
      var band = Band.of(getFrequencyHz(), FreqUnit.HZ);
      if (band == null) {
        return false;
      }
    }

    return getFrequencyHz() < 30_000_000d;
  }

  public boolean isVHF() {
    return isVHF(true);
  }

  public boolean isVHF(boolean hamBandsOnly) {
    if (!isValid()) {
      return false;
    }

    if (hamBandsOnly) {
      var band = Band.of(getFrequencyHz(), FreqUnit.HZ);
      if (band == null) {
        return false;
      }
    }

    return 30_000_000d <= getFrequencyHz() && getFrequencyHz() < 300_000_000d;
  }

  public boolean isUHF() {
    return isUHF(true);
  }

  public boolean isUHF(boolean hamBandsOnly) {
    if (!isValid()) {
      return false;
    }

    if (hamBandsOnly) {
      var band = Band.of(getFrequencyHz(), FreqUnit.HZ);
      if (band == null) {
        return false;
      }
    }

    return getFrequencyHz() >= 300_000_000d;
  }

  public boolean isWX() {
    if (!isValid()) {
      return false;
    }

    final List<Double> WX_FREQUENCIES_MHZ = List.of(162.400, 162.425, 162.450, 162.475, 162.500, 162.525, 162.550);
    final List<Double> WX_FREQUENCIES_HZ = WX_FREQUENCIES_MHZ.stream().map(d -> d * 1_000_000d).toList();
    final Set<Double> WX_FREQUENCY_SET = new HashSet<Double>(WX_FREQUENCIES_HZ);

    Double doubleFreq = Double.valueOf(getFrequencyHz());

    if (!WX_FREQUENCY_SET.contains(doubleFreq)) {
      return false;
    }

    return true;
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
