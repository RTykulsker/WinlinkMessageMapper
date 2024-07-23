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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * parse a String into an ordered, deduplicated List of Integers such as would be used to print a set of pages, select a
 * set of columns from a spreadsheet, etc.
 */
public class PageParser {
  private final String FIELD_DELIMITER = ",";
  private final String RANGE_DELIMITER = "-";
  private Set<Integer> indexSet = new HashSet<>();

  public List<Integer> parse(String inputString) {
    if (inputString == null || inputString.trim().length() == 0) {
      return new ArrayList<Integer>();
    }

    var fields = inputString.split(FIELD_DELIMITER);
    for (var field : fields) {
      var intList = parseField(field.trim());
      if (intList != null) {
        indexSet.addAll(intList);
      }
    }

    return toString(indexSet);
  }

  private List<Integer> parseField(String inputString) {
    if (inputString.contains("-")) {
      return parseFieldAsRange(inputString);
    } else {
      return parseFieldAsSingleValue(inputString);
    }
  }

  private List<Integer> parseFieldAsSingleValue(String inputString) {
    var isAllDigits = inputString.matches("\\d+");
    if (isAllDigits) {
      var intValue = Integer.parseInt(inputString);
      return List.of(intValue);
    }

    var isAllAlphabetic = inputString.matches("^[a-zA-Z]*$");
    if (isAllAlphabetic) {
      var intValue = 0;
      for (var i = 0; i < inputString.length(); ++i) {
        var c = inputString.toUpperCase().charAt(i);
        var charIntValue = (c - 'A') + 1;
        intValue = (26 * intValue) + charIntValue;
      }
      return List.of(intValue);
    }

    throw new IllegalArgumentException("can't parse " + inputString + ", must be either all alphabetic or all numeric");
  }

  private List<Integer> parseFieldAsRange(String inputString) {
    int count = inputString.length() - inputString.replace(RANGE_DELIMITER, "").length();
    if (count != 1) {
      throw new IllegalArgumentException(
          "can't parse " + inputString + ", too many " + RANGE_DELIMITER + " characters");
    }

    var rangeIndex = inputString.indexOf(RANGE_DELIMITER);
    var startIndex = parseFieldAsSingleValue(inputString.substring(0, rangeIndex)).get(0);
    var endIndex = parseFieldAsSingleValue(inputString.substring(rangeIndex + 1)).get(0);

    var intList = new ArrayList<Integer>();
    for (var value = startIndex; value <= endIndex; ++value) {
      intList.add(value);
    }

    return intList;
  }

  private List<Integer> toString(Set<Integer> set) {
    var list = new ArrayList<Integer>(set);
    Collections.sort(list);
    return list;
  }
}
