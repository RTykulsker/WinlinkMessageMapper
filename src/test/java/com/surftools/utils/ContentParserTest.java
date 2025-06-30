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

package com.surftools.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class ContentParserTest {
  @Test
  public void test_isExerciseFirstWord_empty_comments() {

    var cp = new ContentParser("");

    var result = cp.isExerciseFirstWord("Comments", "exercise");
    assertNotNull(result);
    assertEquals("NO", result.value());
    assertNull(result.error());
    assertEquals("", result.context());
  }

  @Test
  public void test_isExerciseFirstWord_happyPath() {

    var content = """
        EXERCISE,ETO,LAXNORTHEAST
        """;

    var cp = new ContentParser(content);

    var result = cp.isExerciseFirstWord("Comments", "exercise");
    assertNotNull(result);
    assertEquals("YES", result.value());
    assertNull(result.error());
    assertEquals("EXERCISE", result.context());
  }

  @Test
  public void test_isExerciseFirstWord_happyPath_mixed_case() {

    var content = """
        Exercise,ETO,LAXNORTHEAST,2 meter
        """;

    var cp = new ContentParser(content);

    var result = cp.isExerciseFirstWord("Comments", "eXeRcIsE");
    assertNotNull(result);
    assertEquals("YES", result.value());
    assertNull(result.error());
    assertEquals("Exercise", result.context());
  }

  @Test
  public void test_getDYFIGroupAffiliation_empty_comments() {
    var cp = new ContentParser("");
    var result = cp.getDYFIGroupAffiliation(",", 2, "\\d+.");

    assertNotNull(result);
    assertNull(result.value());
    assertEquals("not enough words", result.error());
    assertEquals("only 1 word(s)", result.context());
  }

  @Test
  public void test_getDYFIGroupAffiliation_happyPath() {

    var comments = """
        Exercise,ETO,LAXNORTHEAST,2 meter
        """;
    var cp = new ContentParser(comments);
    var result = cp.getDYFIGroupAffiliation(",", 2, "\\d+.");

    assertNotNull(result);
    assertNotNull(result.value());
    assertNull(result.error());
    assertNotNull(result.context());

    @SuppressWarnings("rawtypes")
    var groupSet = (Set) result.context();
    assertEquals(2, groupSet.size());
    assertTrue(groupSet.contains("ETO"));
    assertTrue(groupSet.contains("LAXNORTHEAST"));
  }

  @Test
  public void test_getDYFIGroupAffiliation_happyPath_no_end_delimiter() {

    var comments = """
        Exercise,ETO,LAXNORTHEAST,LAXNORTHEAST,LAXNORTHEAST,ETO,Two meter
        """;
    var cp = new ContentParser(comments);
    var result = cp.getDYFIGroupAffiliation(",", 2, "\\d+.");

    assertNotNull(result);
    assertNotNull(result.value());
    assertNull(result.error());
    assertNotNull(result.context());

    @SuppressWarnings("rawtypes")
    var groupSet = (Set) result.context();
    assertEquals(3, groupSet.size());
    assertTrue(groupSet.contains("ETO"));
    assertTrue(groupSet.contains("LAXNORTHEAST"));
    assertTrue(groupSet.contains("TWO METER"));
  }

  @Test
  public void testFindFirstNumberInString() {
    String result = "";
    ContentParser cp;

    cp = new ContentParser(null);
    result = cp.findFirstNumber();
    assertEquals(null, result);

    cp = new ContentParser("");
    result = cp.findFirstNumber();
    assertEquals(null, result);

    cp = new ContentParser("abc");
    result = cp.findFirstNumber();
    assertEquals(null, result);

    cp = new ContentParser("abc123eex 987");
    result = cp.findFirstNumber();
    assertEquals("123", result);

    cp = new ContentParser("abc123eex 987");
    result = cp.findFirstNumber(Set.of("123"));
    assertEquals("987", result);
  }
}