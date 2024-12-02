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

import org.junit.Test;

public class FrequencyTest {

  @Test
  public void constructor_test() {
    var f = new Frequency("7.074");
    System.out.println("freq: " + f);
    assertEquals("freq: " + f.toString(), "7.074 MHz", f.toString());
  }

  @Test
  public void band_test() {
    var f = new Frequency("7.074");
    var band = f.bandOf();
    System.out.println("band: " + band);
    assertEquals("freq: " + f.toString() + ", band: " + band, Band.B_40M, band);
  }

  @Test
  public void subtract_test() {
    var txFreq = new Frequency("147.76");
    var rxFreq = new Frequency("147.16");
    var offsetFreq = txFreq.subtract(rxFreq);
    var expectedOffset = new Frequency("600", FreqUnit.KHZ);
    var areEqual = expectedOffset.equals(offsetFreq);
    assertEquals("offset should be 600 kHz", expectedOffset, offsetFreq);
  }
}
