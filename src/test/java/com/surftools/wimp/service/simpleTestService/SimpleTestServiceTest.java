/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

package com.surftools.wimp.service.simpleTestService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class SimpleTestServiceTest {

  static final double DELTA = 0d;

  @Test
  public void test_constructor() {
    var sts = new SimpleTestService();
    assertNotNull(sts);

    assertEquals("SimpleTestService", sts.getName());
    assertEquals(0, sts.getResetCount());
    assertEquals(0, sts.getTestCount());
    assertEquals(0, sts.getAddCount());
    assertEquals(0, sts.getTotalPoints(), DELTA);

    assertEquals(0, sts.getPoints(), DELTA);
    assertEquals(0, sts.getExplanations().size());
  }

  @Test
  public void test_toAlphanumericString() {
    String s = "";
    s = SimpleTestService.toAlphaNumericString(null);
    assertNull(s);

    s = SimpleTestService.toAlphaNumericString("");
    assertEquals("", s);

    s = "abc ABC123 !@#";
    s = SimpleTestService.toAlphaNumericString(s);
    assertEquals("abcabc123", s);
  }
}
