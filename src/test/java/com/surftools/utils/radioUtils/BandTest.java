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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class BandTest {

  @Test
  public void test_range() {
    for (var band : Band.values()) {
      System.out.println("band: " + band + ", range: " + band.range());
    }
  }

  @Test
  public void test_range_with_units() {
    for (var unit : FreqUnit.values()) {
      for (var band : Band.values()) {
        System.out.println("unit: " + unit + ", band: " + band + ", range: " + band.range(unit));
      }
    }
  }

  @Test
  public void test_isHF() {
    for (var band : Band.values()) {
      System.out.println("band: " + band + ", isHF: " + band.isHF());
    }
  }

  @Test
  public void test_isVHF() {
    for (var band : Band.values()) {
      System.out.println("band: " + band + ", isVHF: " + band.isVHF());
    }
  }

  @Test
  public void test_isUHF() {
    for (var band : Band.values()) {
      System.out.println("band: " + band + ", isUHF: " + band.isUHF());
    }
  }

  @Test
  public void test_of() {
    for (var freq : List.of(1d, 2.7818d, 3.14159d, 5d, 10d, 20d, 100d, 1000d)) {
      var band = Band.of(freq);
      assertNull("freq: " + freq, band);
    }

    var map = Map.of(3.573d, Band.B_80M, 7.074d, Band.B_40M, 10.136d, Band.B_30M, 14.074d, Band.B_20M);
    for (var freq : map.keySet()) {
      var band = Band.of(freq);
      assertNotNull("freq: " + freq, band);
      assertEquals("freq: " + freq, map.get(freq), band);
    }
  }
}
