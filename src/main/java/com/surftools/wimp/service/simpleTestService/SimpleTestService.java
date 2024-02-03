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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.surftools.utils.counter.ICounter;
import com.surftools.wimp.service.IService;

/**
 * Support testing and accumulating statistics on whether or not a value matches an expected value.
 *
 * Most common use case should be trivial: test("Greeting should be #EV", "hello, world", value) or test("Message should
 * be on or after #EV", formatted(expectedDT), value.compareTo(expectedDT) >= 0);
 *
 * Previous versions used separate add(...) and test(...) methods, tied together with a "key". Now let's use the label
 * as the key!
 *
 * Type: Strings, DateTimes, Doubles, etc. First version had explicit type as an enum and used in add(), second version
 * inferred type from testXXX() names. Let's go with testXXX, as it may be slightly shorter.
 *
 * String comparison: Text is entered by humans. Humans make mistakes. I found using case-independent, alphanumeric only
 * comparison to be the most friendly.
 *
 * YAGNI: You Ain't Gonna Need It! The initial version (FormFieldManager), was built around the notion of a test being
 * worth "points". The second version (FieldTestService), supported points, but was never used. So, I'm not going to
 * explicitly support points for now, but I won't preclude.
 *
 * fail()/pass(): I found this to be EXTREMELY dangerous. It's easy to code the fail() path and forget the pass() path,
 * thus screwing up the statistics.
 *
 *
 */
public class SimpleTestService implements IService {

  private final Map<String, TestEntry> entryMap = new LinkedHashMap<>();
  private List<String> explanations = new ArrayList<>();

  private double totalPoints = 0; // for all entries
  private double points = 0; // accumulated points since last reset();
  private int resetCount = 0; // number of times reset() has been called
  private int addCount = 0; // number of times internalAdd() has been called
  private int testCount = 0; // number of times internalTest() has been called

  private String explanationPrefix = "";

  /**
   * must, Must, MUST be called at the beginning of process() for each call
   */
  public void reset() {
    this.explanations = new ArrayList<>();
    this.points = 0;
    ++resetCount;
  }

  /**
   * our most common use case, case-independent, alphanumeric String comparison
   *
   * @param rawLabel
   * @param expectedValue
   * @param value
   * @return
   */
  public TestResult test(String rawLabel, String expectedValue, String value) {
    if (rawLabel == null) {
      throw new IllegalArgumentException("null label");
    }

    var label = rawLabel.contains("#EV") && expectedValue != null //
        ? rawLabel.replaceAll("#EV", expectedValue)
        : rawLabel;

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, toAlphaNumericWords(expectedValue));
      entryMap.put(label, entry);
    }

    expectedValue = entry.expectedValue;
    var predicate = value != null && toAlphaNumericWords(value).equalsIgnoreCase(expectedValue);
    return internalTest(entry, predicate, wrap(value), null);
  }

  /**
   * another common use case, predicate evaluated by caller
   *
   * @param label
   * @param predicate
   * @param value
   * @return
   */
  public TestResult test(String label, boolean predicate, String value) {
    if (label == null) {
      throw new IllegalArgumentException("null label or expectedValue");
    }

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
      entryMap.put(label, entry);
    }

    return internalTest(entry, predicate, wrapEmpty(value), null);
  }

  /**
   * test for null or empty String value
   *
   * @param label
   * @param value
   * @return
   */
  public TestResult testIfEmpty(String label, String value) {
    if (label == null) {
      throw new IllegalArgumentException("null label or expectedValue");
    }

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
      entryMap.put(label, entry);
    }

    var predicate = value == null || value.isEmpty();
    return internalTest(entry, predicate, wrapEmpty(value), null);
  }

  /**
   * test if present and not null
   *
   * @param label
   * @param value
   * @return
   */
  public TestResult testIfPresent(String label, String value) {
    if (label == null) {
      throw new IllegalArgumentException("null label or expectedValue");
    }

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
      entryMap.put(label, entry);
    }

    var predicate = value != null && !value.isEmpty();
    return internalTest(entry, predicate, wrapEmpty(value), null);
  }

  /**
   * test[LocalDateTime]OnOrAfter
   *
   * @param rawLabel
   * @param expectedValue
   * @param value
   * @param formatter
   * @return
   */
  public TestResult testOnOrAfter(String rawLabel, LocalDateTime expectedValue, LocalDateTime value,
      DateTimeFormatter formatter) {
    var predicate = value.compareTo(expectedValue) >= 0;

    var label = rawLabel.contains("#EV") && expectedValue != null //
        ? rawLabel.replaceAll("#EV", formatter.format(expectedValue))
        : rawLabel;

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, formatter.format(expectedValue));
      entryMap.put(label, entry);
    }

    return internalTest(entry, predicate, formatter.format(value), null);
  }

  /**
   * test[LocalDateTime]OnOrBefore
   *
   * @param rawLabel
   * @param expectedValue
   * @param value
   * @param formatter
   * @return
   */
  public TestResult testOnOrBefore(String rawLabel, LocalDateTime expectedValue, LocalDateTime value,
      DateTimeFormatter formatter) {
    var predicate = value.compareTo(expectedValue) <= 0;

    var label = rawLabel.contains("#EV") && expectedValue != null //
        ? rawLabel.replaceAll("#EV", formatter.format(expectedValue))
        : rawLabel;

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, formatter.format(expectedValue));
      entryMap.put(label, entry);
    }

    return internalTest(entry, predicate, formatter.format(value), null);
  }

  /**
   * for testing booleans/predicates
   *
   * @param entry
   * @param predicate
   * @return
   */
  public TestResult test(String label, boolean predicate) {
    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, "");
      entryMap.put(label, entry);
    }

    return internalTest(entry, predicate, null, null);
  }

  /**
   * our package-private test method, used by ALL testXXX(...) calls
   *
   * @param entry
   * @param predicate
   * @param value
   * @param altExplanation
   * @return
   */
  private TestResult internalTest(TestEntry entry, boolean predicate, String value, String altExplanation) {
    ++testCount;

    var possiblePoints = 0d;
    var explanation = "";
    ++entry.totalCount;
    entry.counter.incrementNullSafe(value);
    if (predicate) {
      ++entry.passCount;
      possiblePoints = entry.points;
      points += entry.points;
    } else {
      explanation = explanationPrefix;
      if (altExplanation != null) {
        explanation += altExplanation;
      } else if (value == null) {
        explanation = entry.label;
      } else {
        explanation = entry.label + ", not " + value;
      }
      explanations.add(explanation);
    }

    return new TestResult(predicate, entry.label, possiblePoints, explanation);
  }

  @SuppressWarnings("rawtypes")
  private String wrap(Comparable value) {
    if (value == null) {
      return "(null)";
    } else if (value instanceof String s && s.isEmpty()) {
      return "(empty)";
    } else {
      return value.toString();
    }
  }

  private String wrapEmpty(String value) {
    if (value == null) {
      return null;
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
  public double getPoints() {
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
    return "SimpleTestService";
  }

  public int getResetCount() {
    return resetCount;
  }

  public int getAddCount() {
    return addCount;
  }

  public int getTestCount() {
    return testCount;
  }

  public double getTotalPoints() {
    return totalPoints;
  }

  /**
   * to make sum of points equal to any desired amount
   *
   * @param desiredTotalPoints
   * @return
   */
  public double normalizePoints(double desiredTotalPoints) {

    double factor = desiredTotalPoints / totalPoints;
    totalPoints = 0;
    for (String key : entryMap.keySet()) {
      var entry = entryMap.get(key);
      entry.points *= factor;
      totalPoints += entry.points;
      entryMap.put(key, entry);
    }

    return getTotalPoints();
  }

  public String validate() {
    var explanations = new ArrayList<String>();

    var expectedEntryCount = resetCount;
    for (String key : entryMap.keySet()) {
      var entry = entryMap.get(key);
      if (entry.totalCount == 0) {
        explanations.add("key: " + key + ", called 0 times");
      }
      if (entry.totalCount != expectedEntryCount) {
        explanations.add("key: " + key + ", called " + entry.totalCount + " times, expected: " + expectedEntryCount);
      }
      entryMap.put(key, entry);
    }

    return String.join("\n", explanations);
  }

  public String toAlphaNumericString(String s) {
    if (s == null) {
      return null;
    }
    return s.toLowerCase().replaceAll("[^A-Za-z0-9]", "");
  }

  public String toAlphaNumericWords(String s) {
    if (s == null) {
      return null;
    }

    var list = new ArrayList<String>();
    var words = s.split("\\s");
    for (var word : words) {
      // https://stackoverflow.com/questions/24967089/java-remove-all-non-alphanumeric-character-from-beginning-and-end-of-string
      list.add(word.replaceAll("^[^\\p{L}^\\p{N}\\s%]+|[^\\p{L}^\\p{N}\\s%]+$", ""));
    }

    return String.join(" ", list);
  }

  public String stripEmbeddedSpaces(String s) {
    if (s == null) {
      return null;
    }
    return s.replaceAll("\\s", "");
  }

} // end class SimpleTestService
