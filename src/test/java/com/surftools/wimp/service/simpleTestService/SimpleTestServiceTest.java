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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    var sts = new SimpleTestService();
    var s = sts.toAlphaNumericString(null);
    assertNull(s);

    s = sts.toAlphaNumericString("");
    assertEquals("", s);

    s = "abc ABC123 !@#";
    s = sts.toAlphaNumericString(s);
    assertEquals("abcabc123", s);
  }

  @Test
  public void test_toAlphaNumericWords() {
    var sts = new SimpleTestService();
    var expected = "Hello, world";
    var actual1 = "+++he---llo, World!";

    var expectedString = sts.toAlphaNumericString(expected);
    var expectedWords = sts.toAlphaNumericWords(expected);
    var actual1String = sts.toAlphaNumericString(actual1);
    var actual1Words = sts.toAlphaNumericWords(actual1);
    assertTrue(expectedString.equalsIgnoreCase(actual1String));
    assertFalse(expectedWords.equalsIgnoreCase(actual1Words));

    var actual2 = "+++hello, @World!";
    var actual2String = sts.toAlphaNumericString(actual2);
    var actual2Words = sts.toAlphaNumericWords(actual2);
    assertTrue(expectedString.equalsIgnoreCase(actual2String));
    assertTrue(expectedWords.equalsIgnoreCase(actual2Words));

    var actual3 = "212 S Ocean Blvd Myrtle Beach,SC";
    var actual3Words = sts.toAlphaNumericWords(actual3);
    var expected3Words = "212 S Ocean Blvd Myrtle Beach SC";
    assertEquals(expected3Words.toLowerCase(), actual3Words.toLowerCase());
  }

  @Test
  public void test_test_2line() {
    {
      var sts = new SimpleTestService();
      var expected = "greeting should be hello, world,\n               not hello, warld";
      var result = sts.test_2line("greeting should be #EV", "hello, world", "hello, warld");
      var actual = result.explanation();
      System.out.println(actual);
      assertEquals(expected, actual);
    }

    {
      var sts = new SimpleTestService();
      var prefix = "blah ";
      sts.setExplanationPrefix(prefix);
      var expected = prefix + "greeting should be hello, world,\n                    not hello, warld";
      var result = sts.test_2line("greeting should be #EV", "hello, world", "hello, warld");
      var actual = result.explanation();
      System.out.println(actual);
      assertEquals(expected, actual);
    }
  }
}
