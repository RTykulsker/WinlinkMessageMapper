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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * parse "fields" out of a String like Comments or Message
 *
 *
 */
public class ContentParser {
  private final String content;
  private String[] lines;

  /**
   * how we return all results
   */
  public static record ParseResult(String value, String error, Object context) {
  };

  public ContentParser(String content) {
    this.content = (content != null) ? content : "";

    lines = this.content.split("\n");
    lines = (lines == null) ? new String[] {} : lines;
  }

  public String[] getLines() {
    return lines;
  }

  /**
   * return "YES" if first "word" is phrase (typically, EXERCISE (case-independent)), "NO" if not
   *
   * @return
   */
  public ParseResult isExerciseFirstWord(String label, String phrase) {
    var words = content.split(",");
    if (words != null && words.length >= 1) {
      var word = words[0];
      if (word.strip().toUpperCase().replaceAll("[^A-Za-z0-9]", "").startsWith(phrase.strip().toUpperCase())) {
        return new ParseResult("YES", null, word);
      } else {
        return new ParseResult("NO", null, word);
      }
    } else {
      return new ParseResult(null, "could not parse " + phrase + " from " + label, "");
    }
  }

  /**
   * DYFI has only one line of comment
   *
   * @param delimiter,
   *          typically a comma,
   * @param startWordIndex,
   *          1-based, typically 2, since 1 is for Exercise
   * @param endWordRegex
   *          typically "\\d+.", to stop on first word that starts with digit
   * @return a upper-cased set in context
   */
  public ParseResult getDYFIGroupAffiliation(String delimiter, int startWordIndex, String endWordRegex) {
    var words = content.split(delimiter);

    if (words.length < startWordIndex) {
      return new ParseResult(null, "not enough words", "only " + words.length + " word(s)");
    }

    var pattern = Pattern.compile(endWordRegex);
    var groupSet = new LinkedHashSet<String>();
    for (var i = startWordIndex - 1; i < words.length; ++i) {
      var word = words[i].strip();
      if (word == null || word.length() == 0) {
        continue;
      }
      var matcher = pattern.matcher(word);
      if (matcher.find()) {
        break;
      }
      groupSet.add(word.toUpperCase());
    }

    var resultString = String.join(",", groupSet);
    return new ParseResult(resultString, null, groupSet);
  }

  /**
   * find first Number in a String
   *
   * @param line
   * @return
   */
  public String findFirstNumber() {
    return findFirstNumber(new HashSet<String>());
  }

  /**
   * find first Number in a String
   *
   * @param line
   * @param ignoreSet
   *          set of strings, representing numbers that shold be ignored
   * @return
   */
  public String findFirstNumber(Set<String> ignoreSet) {
    if (content == null) {
      return null;
    }

    final var pattern = Pattern.compile("\\d+");
    String ret = null;
    var matcher = pattern.matcher(content);
    while (true) {
      var found = matcher.find();
      if (found) {
        var countString = matcher.group();
        if (ignoreSet.contains(countString)) {
          continue;
        } else {
          found = true;
          ret = countString;
          break;
        }
      } else {
        return ret;
      }
    }

    return ret;
  }
}
