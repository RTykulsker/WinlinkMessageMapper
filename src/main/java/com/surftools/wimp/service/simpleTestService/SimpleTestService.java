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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.ICounter;
import com.surftools.wimp.service.IService;

import me.xdrop.fuzzywuzzy.FuzzySearch;

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
  private static final Logger logger = LoggerFactory.getLogger(SimpleTestService.class);

  private final Map<String, TestEntry> entryMap = new LinkedHashMap<>();
  private List<String> explanations = new ArrayList<>();

  private double totalPoints = 0; // for all entries
  private double points = 0; // accumulated points since last reset();
  private int resetCount = 0; // number of times reset() has been called
  private int addCount = 0; // number of times internalAdd() has been called
  private int testCount = 0; // number of times internalTest() has been called

  private String explanationPrefix = "";
  private String caller;

  /**
   * this is a hack to enable two line output
   */
  private boolean doTwoLineOutput = false;

  /**
   * must, Must, MUST be called at the beginning of process() for each call
   */
  public void reset(String caller) {
    this.explanations = new ArrayList<>();
    this.points = 0;
    ++resetCount;

    this.caller = caller;
  }

  /**
   * convenience method to unwrap a TestResult
   *
   * @param testResult
   * @return
   */
  public boolean isOk(TestResult testResult) {
    if (testResult == null) {
      return false;
    }

    return testResult.ok();
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
   * our most common use case, with two-line output
   *
   * @param rawLabel
   * @param expectedValue
   * @param value
   * @return
   */
  public TestResult test_2line(String rawLabel, String expectedValue, String value) {
    doTwoLineOutput = true;
    var ret = test(rawLabel, expectedValue, value);
    doTwoLineOutput = false;
    return ret;
  }

  public TestResult testAsDouble(String rawLabel, String expectedValueString, String valueString) {
    var label = rawLabel.contains("#EV") && expectedValueString != null //
        ? rawLabel.replaceAll("#EV", expectedValueString)
        : rawLabel;

    var expectedValue = Double.parseDouble(expectedValueString);
    try {
      var value = Double.valueOf(Double.parseDouble(valueString));
      var predicate = value.equals(expectedValue);
      return test(label, predicate, valueString);
    } catch (Exception e) {
      return test(label, false, valueString);
    }
  }

  public TestResult testStartsWith(String rawLabel, String expectedValue, String value) {
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
    var predicate = value != null && toAlphaNumericWords(value).startsWith(expectedValue);

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

  public TestResult test(String label, boolean predicate, String value, String altExplanation) {
    if (label == null) {
      throw new IllegalArgumentException("null label or expectedValue");
    }

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
      entryMap.put(label, entry);
    }

    return internalTest(entry, predicate, wrapEmpty(value), altExplanation);
  }

  public TestResult testList(String label, List<String> list, String value) {
    return testList(label, list, value, null);
  }

  public TestResult testList(String rawLabel, List<String> list, String value, String altExplanation) {
    if (rawLabel == null) {
      throw new IllegalArgumentException("null label or expectedValue");
    }

    var label = rawLabel.contains("#EV") && list != null //
        ? rawLabel.replaceAll("#EV", "[" + String.join(",", list) + "]")
        : rawLabel;

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
      entryMap.put(label, entry);
    }

    var predicate = list.contains(value);
    return internalTest(entry, predicate, wrapEmpty(value), altExplanation);
  }

  public TestResult testRegex(String label, String regexString, String value) {
    return testRegex(label, regexString, value, null);
  }

  /**
   *
   * @param label
   * @param regexString
   * @param value
   * @param altExplanation
   * @return
   */
  public TestResult testRegex(String label, String regexString, String value, String altExplanation) {
    if (label == null) {
      throw new IllegalArgumentException("null label or expectedValue");
    }

    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
      entryMap.put(label, entry);
    }

    var predicate = Pattern.compile(regexString).matcher(value).find();

    return internalTest(entry, predicate, wrapEmpty(value), altExplanation);
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
    return testOnOrAfter(rawLabel, expectedValue, value, formatter, null);
  }

  /**
   * test[LocalDateTime]OnOrAfter
   *
   * @param rawLabel
   * @param expectedValue
   * @param value
   * @param formatter
   * @param extraExplanation
   * @return
   */
  public TestResult testOnOrAfter(String rawLabel, LocalDateTime expectedValue, LocalDateTime value,
      DateTimeFormatter formatter, String extraExplanation) {
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

    String altExplanation = null;
    if (extraExplanation != null) {
      altExplanation = label + extraExplanation;
    }

    return internalTest(entry, predicate, formatter.format(value), altExplanation);
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
    return testOnOrBefore(rawLabel, expectedValue, value, formatter, null);

  }

  /**
   * test[LocalDateTime]OnOrBefore
   *
   * @param rawLabel
   * @param expectedValue
   * @param value
   * @param formatter
   * @param extraExplanation
   * @return
   */
  public TestResult testOnOrBefore(String rawLabel, LocalDateTime expectedValue, LocalDateTime value,
      DateTimeFormatter formatter, String extraExplanation) {
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

    String altExplanation = null;
    if (extraExplanation != null) {
      altExplanation = label + extraExplanation;
    }

    return internalTest(entry, predicate, formatter.format(value), altExplanation);
  }

  /**
   * test if value could be a LocalDate, LocalTime or LocalDateTime, according to formatter
   *
   * @param label
   * @param value
   * @param formatter
   * @return
   */
  public TestResult testIsDateTime(String label, String value, DateTimeFormatter formatter) {
    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
    }
    var predicate = false;
    try {
      formatter.parse(value);
      predicate = true;
    } catch (Exception e) {
      ; // could not parse
    }
    return internalTest(entry, predicate, value, null);
  }

  /**
   * test if value could be a LocalDate, LocalTime or LocalDateTime, according to formatter
   *
   * @param label
   * @param value
   * @param formatter
   * @return
   */
  public TestResult testIsDateTime(String label, Object value, DateTimeFormatter formatter) {
    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, null);
    }

    if (value == null) {
      return internalTest(entry, false, "(null)", null);
    }

    var predicate = false;
    var string = "";

    try {
      switch (value) {
      case LocalDateTime ldt:
        string = formatter.format(ldt);
        formatter.parse(string);
        predicate = true;
        break;

      case LocalDate ld:
        string = formatter.format(ld);
        formatter.parse(string);
        predicate = true;
        break;

      case LocalTime lt:
        string = formatter.format(lt);
        formatter.parse(string);
        predicate = true;
        break;

      default:
        break;
      }
    } catch (Exception e) {
      ; // could not parse
    }

    return internalTest(entry, predicate, string, null);
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
   * fuzzy match; see: https://github.com/xdrop/fuzzywuzzy
   *
   * @param fuzzyQuery
   * @param rawLabel
   * @param expectedValue
   * @param value
   * @return
   */
  public TestResult testFuzzy(FuzzyQuery fuzzyQuery, String rawLabel, String expectedValue, String value) {
    if (fuzzyQuery == null) {
      throw new IllegalArgumentException("null fuzzyQuery");
    }

    var fuzzyType = fuzzyQuery.type();
    if (fuzzyType == null) {
      throw new IllegalArgumentException("null fuzzyQuery.type");
    }

    if (fuzzyQuery.threshhold() < 0 || fuzzyQuery.threshhold() > 100) {
      throw new IllegalArgumentException("invalid fuzzy threshhold");
    }

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

    var fuzzyResult = -1;
    var predicate = value != null;
    if (predicate) {
      expectedValue = entry.expectedValue;
      var cookedValue = toAlphaNumericWords(value);

      switch (fuzzyType) {
      case FuzzyType.Simple:
        fuzzyResult = FuzzySearch.ratio(expectedValue, cookedValue);
        break;
      case FuzzyType.Partial:
        fuzzyResult = FuzzySearch.partialRatio(expectedValue, cookedValue);
        break;
      case FuzzyType.TokenSort:
        fuzzyResult = FuzzySearch.tokenSortRatio(expectedValue, cookedValue);
        break;
      case FuzzyType.TokenSet:
        fuzzyResult = FuzzySearch.tokenSetRatio(expectedValue, cookedValue);
        break;
      case FuzzyType.Weighted:
        fuzzyResult = FuzzySearch.weightedRatio(expectedValue, cookedValue);
        break;
      default:
        break;
      }

      logger
          .debug("fuzzyQuery: " + fuzzyQuery + ", ev: " + expectedValue + ", value:" + toAlphaNumericString(value)
              + ", result: " + fuzzyResult);

      predicate = fuzzyResult >= fuzzyQuery.threshhold();
    }

    var internalResult = internalTest(entry, predicate, wrap(value), null);
    return TestResult.withExtraData(internalResult, String.valueOf(fuzzyResult));
  }

  public TestResult testNotNull(String label, Object object) {
    var entry = entryMap.get(label);
    if (entry == null) {
      ++addCount;
      entry = new TestEntry(label, "");
      entryMap.put(label, entry);
    }

    return internalTest(entry, object != null, null, null);
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
      if (altExplanation != null && !altExplanation.isEmpty()) {
        explanation += altExplanation;
      } else if (value == null) {
        explanation += entry.label;
      } else {
        explanation += entry.label + ", not " + value;
      }
      if (doTwoLineOutput) {
        explanation = reformat_ShouldBe_Not(explanation);
      }
      explanations.add(explanation);
    }

    return new TestResult(predicate, entry.label, possiblePoints, explanation, null);
  }

  private String reformat_ShouldBe_Not(String s) {
    if (s == null) {
      return s;
    }

    final var SHOULD_BE = " should be ";
    var shouldBeIndex = s.indexOf(SHOULD_BE);

    final var NOT = ", not";
    var notIndex = s.indexOf(NOT);
    if (shouldBeIndex == -1 || notIndex == -1 || notIndex <= shouldBeIndex) {
      return s;
    }

    var nSpaces = shouldBeIndex + SHOULD_BE.length() - NOT.length();
    var spaces = new String(new char[nSpaces]).replace('\0', ' ');

    var ret = s.substring(0, notIndex + 1) + "\n" + spaces + s.substring(notIndex + 1);

    return ret;
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
    s = toAlphaNumericString(s);

    // Some people, when confronted with a problem, think "I know, I'll use regular expressions."
    // Now they have two problems.
    var word = "";
    for (var i = 0; i < s.length(); ++i) {
      var ch = s.charAt(i);
      if (Character.isLetterOrDigit(ch)) {
        word += ch;
      } else {
        if (word.length() > 0) {
          list.add(word);
          word = "";
        }
      }
    }

    // is there still a word pending?
    if (word.length() > 0) {
      list.add(word);
    }

    return String.join(" ", list);
  }

  /**
   * compare two strings, as case-independent alphanumeric words
   *
   * @param s1
   * @param s2
   * @return
   */
  public boolean compareWords(String s1, String s2) {
    if (s1 == null && s2 != null) {
      return false;
    } else if (s1 != null && s2 == null) {
      return false;
    } else if (s1 == null && s2 == null) {
      return true;
    }

    return toAlphaNumericWords(s1).equalsIgnoreCase(toAlphaNumericWords(s2));
  }

  public String stripEmbeddedSpaces(String s) {
    if (s == null) {
      return null;
    }
    return s.replaceAll("\\s", "");
  }

  @Override
  public String toString() {
    var sb = new StringBuilder();

    sb.append("### instance: " + caller + "\n");
    sb.append("points: " + points + "\n");
    sb.append("explanations.size(): " + explanations.size() + "\n");
    sb.append("explanations: " + String.join("\n", explanations) + "\n");

    sb.append("### class\n");
    sb.append("totalPoints (all entries): " + totalPoints + "\n");
    sb.append("reset count: " + resetCount + "\n");
    sb.append("add count: " + addCount + "\n");
    sb.append("testCount: " + testCount + "\n");

    var it = this.iterator();
    while (it.hasNext()) {
      var key = it.next();
      if (this.hasContent(key)) {
        sb.append("   " + this.format(key));
      }
    }

    return sb.toString();
  }

  public void setExplanationPrefix(String explanationPrefix) {
    this.explanationPrefix = explanationPrefix;
  }

  public String getPrefix() {
    return explanationPrefix;
  }

} // end class SimpleTestService
