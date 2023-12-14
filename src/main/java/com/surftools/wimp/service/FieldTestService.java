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

package com.surftools.wimp.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.counter.ICounter;

public class FieldTestService implements IService {
  private enum DateTimeRelop {
    EQ, GE, LE
  };

  private class Entry {
    final String label;
    final Object data;
    final int points;
    int passCount;
    int totalCount;
    final Counter counter;

    public Entry(String label, Object data, int points) {
      this.label = label;
      this.data = data;
      this.points = points;
      this.passCount = 0;
      this.totalCount = 0;
      this.counter = new Counter();
    }

    public Entry(String label, Object data) {
      this(label, data, 0);
    }

  }; // end class Entry

  private final Map<String, Entry> entryMap = new LinkedHashMap<>();
  private List<String> explanations = new ArrayList<>();
  private int points = 0;

  /**
   * must be called at the beginning of process() for each call
   */
  public void reset() {
    this.explanations = new ArrayList<>();
    this.points = 0;
  }

  /**
   * add a new Entry for later testing
   *
   * @param key
   * @param label
   * @param expectedValue
   */
  public void add(String key, String label, Object data) {
    if (data instanceof String s && s != null && label.contains("#EV")) {
      label = label.replaceAll("#EV", s);
    }

    var entry = new Entry(label, data);
    entryMap.put(key, entry);
  }

  /**
   * add a new Entry for later testing
   *
   * @param key
   * @param label
   */
  public void add(String key, String label) {
    add(key, label, null);
  }

  /**
   * fail with the given message added to the explanations
   *
   * @param key
   * @param message
   * @return
   */
  public int fail(String key, String message) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    ++entry.totalCount;
    entry.counter.incrementNullSafe(message);
    explanations.add(message);

    entryMap.put(key, entry);

    return 0;
  }

  public int fail(String key) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    ++entry.totalCount;

    explanations.add(entry.label);

    entryMap.put(key, entry);

    return 0;
  }

  /**
   * always pass the test
   *
   * @param key
   * @return
   */
  public int pass(String key) {
    return testInternal(key, null, true);
  }

  /**
   * generic test method
   *
   * @param key
   * @param value
   * @param predicate
   * @return
   */
  public int test(String key, String value, boolean predicate) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    return testInternal(key, value, predicate);
  }

  public int test(String key, boolean predicate) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    return testInternal(key, null, predicate);
  }

  /**
   * internal test method
   *
   * @param key
   * @param value
   * @param predicate
   * @return
   */
  private int testInternal(String key, String value, boolean predicate) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    var possiblePoints = 0;
    ++entry.totalCount;
    entry.counter.incrementNullSafe(value);
    if (predicate) {
      ++entry.passCount;
      possiblePoints = entry.points;
      points += entry.points;
    } else {
      explanations.add(entry.label + ", not " + wrap(value));
    }
    entryMap.put(key, entry);

    return possiblePoints;
  }

  /**
   * test if string ends with data
   *
   * @param string
   * @param activityDateTime
   */
  public int testEndsWith(String key, String value, boolean doCaseIndependent) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    var expectedValue = (String) entry.data;
    var predicate = false;
    if (doCaseIndependent) {
      predicate = value.toLowerCase().endsWith(expectedValue.toLowerCase());
    } else {
      predicate = value.endsWith(expectedValue);
    }
    return testInternal(key, value, predicate);
  }

  /**
   * test for non-null and non-empty values
   *
   * @param string
   * @param activityDateTime
   */
  public int testIfPresent(String key, String value) {
    return testInternal(key, value, value != null && !value.isEmpty());
  }

  /**
   * test for null or empty values
   *
   * @param string
   * @param activityDateTime
   */
  public int testIfEmpty(String key, String value) {
    return testInternal(key, value, value == null || value.isEmpty());
  }

  /**
   * this is how we do a case-independent alpha-numeric only string comparison
   *
   * @param value
   * @param expectedValue
   * @return
   */
  public boolean defaultStringCompare(String value, String expectedValue) {
    if (value == null) {
      return false;
    }

    var predicate = value.replaceAll("[^A-Za-z0-9]", "").equalsIgnoreCase(expectedValue.replaceAll("[^A-Za-z0-9]", ""));
    return predicate;
  }

  /**
   * our default test is a case-independent alpha-numeric only string comparison
   *
   * @param key
   * @param value
   * @return
   */
  public int test(String key, String value) {
    if (value == null) {
      return testInternal(key, null, false);
    }

    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    var expectedValue = (String) entry.data;
    var predicate = defaultStringCompare(value, expectedValue);
    return testInternal(key, value, predicate);
  }

  /**
   * test against a set of strings
   *
   * @param key
   * @param value
   * @return
   */
  public int testSetOfStrings(String key, String value) {
    if (value == null) {
      return testInternal(key, null, false);
    }

    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }

    @SuppressWarnings("unchecked")
    var expectedValues = (Set<String>) entry.data;
    for (var expectedValue : expectedValues) {
      // determine if we'll pass first, then execute test to accumulate statistics and return
      var predicate = defaultStringCompare(value, expectedValue);
      if (predicate) {
        return testInternal(key, value, predicate);
      }
    }

    var message = entry.label + " , not " + value;

    return fail(key, message);
  }

  private int testDateTime(String key, LocalDateTime value, DateTimeFormatter formatter, DateTimeRelop relop) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }
    var expectedValue = (LocalDateTime) entry.data;
    var predicate = false;
    switch (relop) {
    case EQ:
      predicate = value.compareTo(expectedValue) == 0;
      break;
    case GE:
      predicate = value.compareTo(expectedValue) >= 0;
      break;
    case LE:
      predicate = value.compareTo(expectedValue) <= 0;
      break;
    default:
      throw new RuntimeException("DateTimeRelop: " + relop + " not supported");
    }
    return testInternal(key, formatter.format(value), predicate);
  }

  public int testDtEquals(String key, LocalDateTime value, DateTimeFormatter formatter) {
    return testDateTime(key, value, formatter, DateTimeRelop.EQ);
  }

  public int testOnOrAfter(String key, LocalDateTime value, DateTimeFormatter formatter) {
    return testDateTime(key, value, formatter, DateTimeRelop.GE);
  }

  public int testOnOrBefore(String key, LocalDateTime value, DateTimeFormatter formatter) {
    return testDateTime(key, value, formatter, DateTimeRelop.LE);
  }

  private String wrap(String value) {
    if (value == null) {
      return "(null)";
    } else if (value.isEmpty()) {
      return "(empty)";
    } else {
      return value;
    }
  }

  /**
   * get list of all accumulated explanations
   *
   * @return
   */
  public List<String> getExplanations() {
    return explanations;
  }

  /**
   * if we are actually scoring instead of just providing feedback
   *
   * @return
   */
  public int getPoints() {
    return points;
  }

  /**
   * for iterating over all Entries
   *
   * @return
   */
  public Iterator<String> iterator() {
    return entryMap.keySet().iterator();
  }

  @SuppressWarnings("rawtypes")
  public ICounter getCounter(String key) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }
    return entry.counter;
  }

  /**
   * return true if at least some failures
   *
   * @param key
   * @return
   */
  public boolean hasContent(String key) {
    var entry = entryMap.get(key);
    if (entry == null) {
      throw new IllegalArgumentException("no entry found for key: " + key);
    }
    return entry.totalCount != entry.passCount;
  }

  public String format(String key) {
    var entry = entryMap.get(key);
    var sb = new StringBuilder();
    sb.append("    ");
    sb.append(entry.label);
    var failCount = entry.totalCount - entry.passCount;
    var passPercent = formatPercent(entry.passCount, entry.totalCount);
    var failPercent = formatPercent(failCount, entry.totalCount);
    sb
        .append(", correct: " + entry.passCount + "(" + passPercent + "), incorrect: " + failCount + "(" + failPercent
            + ")");
    sb.append("\n");
    return sb.toString();
  }

  private String formatPercent(int numerator, int denominator) {
    if (denominator == 0) {
      return "0%";
    }

    var d = (double) numerator / (double) denominator;
    return String.format("%.2f", 100d * d) + "%";
  }

  @Override
  public String getName() {
    return "FieldTestService";
  }

} // end class FieldTestService
