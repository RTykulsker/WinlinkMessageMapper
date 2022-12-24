/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.formField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FormFieldManager {

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private Map<String, FormField> map = new LinkedHashMap<>();
  private List<String> explanations;
  private boolean isEnabled = true;
  private int points = 0;

  public void add(String key, FormField field) {
    map.put(key, field);
  }

  public void reset(List<String> explanations) {
    this.explanations = explanations;
    points = 0;
  }

  public int getPoints() {
    return points;
  }

  public void setIsEnabled(boolean isEnabled) {
    this.isEnabled = isEnabled;
  }

  public double size() {
    return map.size();
  }

  public Set<String> keySet() {
    return map.keySet();
  }

  public FormField get(String key) {
    return map.get(key);
  }

  /**
   * test if the supplied value scores points, or not
   *
   * @param key
   * @param value
   * @return
   */
  public int test(String key, String value) {
    if (!isEnabled) {
      return 0;
    }

    var field = map.get(key);
    if (field == null) {
      throw new RuntimeException("no FormField found for key: " + key);
    }

    var isOk = false;
    var explanation = "";
    var label = field.label;
    var placeholderValue = field.placeholderValue;

    switch (field.type) {
    case DATE_TIME:
      if (value == null) {
        explanation = label + " must be supplied";
      } else {
        try {
          LocalDateTime.parse(value, FORMATTER);
          isOk = true;
        } catch (Exception e) {
          explanation = label + "(" + value + ") is not a valid Date/Time";
        }
      }
      break;

    case DATE_TIME_NOT:
      if (value == null) {
        explanation = label + " must be supplied";
      } else if (value.equalsIgnoreCase(placeholderValue)) {
        explanation = label + "(" + value + ") must not be " + placeholderValue;
      } else {
        try {
          LocalDateTime.parse(value, FORMATTER);
          isOk = true;
        } catch (Exception e) {
          explanation = label + "(" + value + ") is not a valid Date/Time";
        }
      }
      break;

    case EMPTY:
      isOk = value == null || value.isBlank();
      if (!isOk) {
        explanation = label + "(" + value + ") must be blank";
      }
      break;

    case OPTIONAL:
      isOk = true;
      break;

    case OPTIONAL_NOT:
      if (value != null && value.equalsIgnoreCase(placeholderValue)) {
        explanation = label + "(" + value + ") must not be " + placeholderValue;
      } else {
        isOk = true;
      }
      break;

    case REQUIRED:
      if (value == null) {
        explanation = label + " must be supplied";
      } else if (value.isBlank()) {
        explanation = label + "(" + value + ") must not be blank";
      } else {
        isOk = true;
      }
      break;

    case REQUIRED_NOT:
      if (value == null) {
        explanation = label + " must be supplied";
      } else if (value.isBlank()) {
        explanation = label + "(" + value + ") must not be blank";
      } else if (value.equalsIgnoreCase(placeholderValue)) {
        isOk = false;
        explanation = label + "(" + value + ") must not be " + placeholderValue;
      } else {
        isOk = true;
      }

      break;

    case SPECIFIED:
      if (value == null) {
        explanation = label + " must be " + placeholderValue;
      } else if (!value.equalsIgnoreCase(placeholderValue)) {
        explanation = label + "(" + value + ") must be " + placeholderValue;
      } else {
        isOk = true;
      }
      break;

    default:
      throw new RuntimeException("unhandled type: " + field.type.toString());
    }

    var returnPoints = 0;
    if (isOk) {
      ++field.count;
      returnPoints = field.points;
    } else {
      explanations.add(explanation);
    }

    points += returnPoints;
    return returnPoints;
  }

}
