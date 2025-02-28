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

import java.util.LinkedHashSet;
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
      if (word.strip().toUpperCase().startsWith(phrase.strip().toUpperCase())) {
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
   * extract all group affiliations
   *
   * @param label
   * @param comments
   * @param lineNumber
   */
  // protected String processGroupAffiliation(String label, String comments, Integer lineNumber) {
  // var groupSet = new LinkedHashSet<String>();
  // if (comments != null) {
  // if (lineNumber == null) {
  // // DYFI has only a single line. Everything from 2nd word to first with starting with number is assumed to be a
  // // group affiliation
  // var fields = comments.split(",");
  // if (fields.length >= 2) {
  // for (var index = 1; index < fields.length; ++index) {
  // var field = fields[index];
  // if (isNull(field)) {
  // continue;
  // }
  // var c = field.charAt(0);
  // if (Character.isDigit(c)) {
  // break;
  // }
  // groupSet.add(field.trim().toUpperCase());
  // }
  // }
  // } else {
  // var lines = comments.split("\n");
  // if (lines.length >= lineNumber) {
  // var line = lines[lineNumber - 1];
  // if (line != null) {
  // var fields = line.split(",");
  // for (var field : fields) {
  // if (!isNull(field)) {
  // groupSet.add(field.trim().toUpperCase());
  // } // endif non-null and not empty
  // } // end for over fields in line
  // } else { // end if line != null
  // count(sts.test("Group Affiliation(s) parsed", false));
  // } // end if line == null
  // } else { // end if lineNumber in range
  // count(sts.test("Group Affiliation(s) parsed", false));
  // } // end if lineNumber not in range
  // } // end if lineNumber specified
  //
  // for (var groupName : groupSet) {
  // getCounter(label + " Group Affiliation").increment(groupName);
  // }
  // count(sts.test("Group Affiliation(s) parsed", true));
  // } else { // comment are null
  // count(sts.test("Group Affiliation(s) parsed", false));
  // } // end if comments are null
  //
  // return String.join(",", groupSet);
  // }
}
