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

package com.surftools.wimp.formField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
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

  private static final Set<FFType> needsPlaceholderSet = Set
      .of(FFType.DATE_TIME_NOT, FFType.OPTIONAL_NOT, FFType.REQUIRED_NOT, FFType.SPECIFIED, FFType.CONTAINS);

  private static final Set<FFType> noPlaceholderSet = Set
      .of(FFType.DATE_TIME, FFType.EMPTY, FFType.OPTIONAL, FFType.REQUIRED);

  public void add(String key, FormField field) {
    // fail fast if placeholder provided when none should be and vice-versa
    var placeholder = field.placeholderValue;
    if (needsPlaceholderSet.contains(field.type) && (placeholder == null || placeholder.isBlank())) {
      throw new RuntimeException("no placeholder provided for FormField: " + key);
    }
    if (noPlaceholderSet.contains(field.type) && (placeholder != null)) {
      throw new RuntimeException("placeholder provided for FormField: " + key);
    }

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
    var counter = field.counter;

    counter.incrementNullSafe(value);

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

    case CONTAINS:
      if (value == null) {
        explanation = label + " must contain " + placeholderValue;
      } else if (!value.toLowerCase().contains(placeholderValue.toLowerCase())) {
        explanation = label + " must contain " + placeholderValue;
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

  public String formatCounters() {
    var sb = new StringBuilder();

    for (var entry : map.entrySet()) {
      var formField = entry.getValue();
      var counter = formField.counter;
      var it = counter.getDescendingCountIterator();
      sb.append("\n" + formField.label + "\n");
      while (it.hasNext()) {
        var countEntry = it.next();
        var countKey = countEntry.getKey();
        var countValue = countEntry.getValue();
        sb.append("  value: " + countKey + ", count: " + countValue + "\n");
      } // end loop over values in counter
    } // end loop over formFields in manager

    return sb.toString();
  }

  public void merge(FormFieldManager[] ffms, String[] summableFieldKeys) {
    var summableFieldSet = new HashSet<String>(Arrays.asList(summableFieldKeys));
    for (var ffm : ffms) {
      var subMap = ffm.map;
      for (var entry : subMap.entrySet()) {
        var key = entry.getKey();
        var subField = entry.getValue();
        var field = map.get(key);
        if (field == null) {
          field = new FormField(subField.type, subField.label, subField.placeholderValue, subField.points);
        }
        if (summableFieldSet.contains(key)) {
          field.count += subField.count;
          field.counter.merge(subField.counter);
        } else {
          field.count = subField.count;
          field.counter = subField.counter;
        }
        map.put(key, field);
      }
    }

  }

}