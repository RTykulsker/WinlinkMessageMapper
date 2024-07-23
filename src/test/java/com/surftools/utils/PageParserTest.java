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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PageParserTest {

  @SuppressWarnings("deprecation")
  @Rule
  public ExpectedException exceptionRule = ExpectedException.none();

  @Test
  public void test_nullInput() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse(null);
    assertNotNull(list);
    assertEquals(0, list.size());
  }

  @Test
  public void test_emptyInput() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("");
    assertNotNull(list);
    assertEquals(0, list.size());
  }

  @Test
  public void test_singleValue() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("42");
    assertNotNull(list);
    assertEquals(1, list.size());
    assertTrue(list.get(0) == 42);
  }

  @Test
  public void test_ordering() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("3,2,1");
    assertNotNull(list);
    assertEquals(3, list.size());
    assertTrue(list.get(0) == 1);
    assertTrue(list.get(1) == 2);
    assertTrue(list.get(2) == 3);
  }

  @Test
  public void test_dedupe() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("3,2,1,3,2,1,1,2,3");
    assertNotNull(list);
    assertEquals(3, list.size());
    assertTrue(list.get(0) == 1);
    assertTrue(list.get(1) == 2);
    assertTrue(list.get(2) == 3);
  }

  @Test
  public void test_embeddedSpaces() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("3, 2, 1 , 3 ,    2 , 1,1,2,3");
    assertNotNull(list);
    assertEquals(3, list.size());
    assertTrue(list.get(0) == 1);
    assertTrue(list.get(1) == 2);
    assertTrue(list.get(2) == 3);
  }

  @Test
  public void test_range() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("1-30");
    assertNotNull(list);
    assertEquals(30, list.size());
    assertTrue(list.get(0) == 1);
    assertTrue(list.get(1) == 2);
    assertTrue(list.get(2) == 3);
    assertTrue(list.get(29) == 30);
  }

  @Test
  public void test_simpleAlphabetic() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("c");
    assertNotNull(list);
    assertEquals(1, list.size());
    assertTrue(list.get(0) == 3);
  }

  @Test
  public void test_2CharAlphabetic() {
    var pp = new PageParser();
    assertNotNull(pp);
    var list = pp.parse("AA");
    assertNotNull(list);
    assertEquals(1, list.size());
    assertTrue(list.get(0) == 27);
  }

  @Test
  public void test_doubleHyphen() {
    var pp = new PageParser();
    assertNotNull(pp);
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage("can't parse 1--3, too many - characters");
    pp.parse("1--3");
  }

  @Test
  public void testMixedAlphaNumbic() {
    var pp = new PageParser();
    assertNotNull(pp);
    exceptionRule.expect(IllegalArgumentException.class);
    exceptionRule.expectMessage("can't parse A1, must be either all alphabetic or all numeric");
    pp.parse("A1");
  }
}
