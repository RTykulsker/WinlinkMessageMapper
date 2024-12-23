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

package com.surftools.wimp.processors.exercise.eto_2024;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.message.Ics309Message.Activity;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;

public class ETO_2024_03_21_Test {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_03_21_Test.class);

  private static final LocalDateTime WINDOW_OPEN_DT = LocalDateTime.of(1970, 1, 01, 0, 0, 0);
  private static final LocalDateTime WINDOW_CLOSE_DT = LocalDateTime.of(2099, 12, 31, 0, 0, 0);

  @Test
  public void test_null() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    List<Activity> activities = new ArrayList<>();

    processor.validateActivities(null, sts, activities, null, null);
    assertEquals(0, sts.getExplanations().size());

    processor.validateActivities(sender, null, activities, null, null);
    assertEquals(0, sts.getExplanations().size());

    processor.validateActivities(sender, sts, null, null, null);
    assertEquals(0, sts.getExplanations().size());

    processor.validateActivities(sender, sts, activities, null, null);
    assertEquals(0, sts.getExplanations().size());

    activities.add(null);
    processor.validateActivities(sender, sts, activities, null, null);
    assertEquals(0, sts.getExplanations().size());

    sts.reset("unit test");
    activities.clear();
    activities.add(new Activity(null, null, null, null));
    processor.validateActivities(sender, sts, activities, null, null);
    assertEquals(0, sts.getExplanations().size());
  }

  @Test
  public void test_activityTestMessageNulls() {
    final var sender = "UNIT TEST";
    final var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    List<Activity> activities = new ArrayList<>();

    final var dt = "1970-01-01 00:00";
    final var testSubject = "ETO Exercise, Winlink Simulated Emergency Message One";

    activities.add(new Activity(null, sender, "TEST", testSubject));
    activities.add(new Activity(dt, null, "TEST", testSubject));
    activities.add(new Activity(dt, sender, null, testSubject));
    activities.add(new Activity(dt, sender, "TEST", null));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(7, sts.getExplanations().size());
  }

  @Test
  public void test_activityServiceMessageNulls() {
    final var sender = "UNIT TEST";
    final var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    List<Activity> activities = new ArrayList<>();

    final var dt = "1970-01-01 00:00";
    final var testSubject = "Test Message";

    activities.add(new Activity(null, "SERVICE", sender, testSubject));
    activities.add(new Activity(dt, null, sender, testSubject));
    activities.add(new Activity(dt, "SERVICE", null, testSubject));
    activities.add(new Activity(dt, "SERVICE", sender, null));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(8, sts.getExplanations().size());
  }

  @Test
  public void test_invalid_date() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity("a date", sender, sender, sender));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(6, explanations.size());
    assertEquals("Should have valid activity Date/Time, not 'a date', on line: 1", explanations.get(0));
  }

  @Test
  public void test_happyPath() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "2024-03-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "6", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(0, explanations.size());
  }

  @Test
  public void test_windowBefore() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "1969-01-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "6", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(6, explanations.size());
    for (var i = 1; i <= 6; ++i) {
      assertEquals("Activity Date/Time should be on or after " + ETO_2024_03_21.DTF.format(WINDOW_OPEN_DT) + ", not "
          + dateBase + i + ", on line: " + i, explanations.get(i - 1));
    }
  }

  @Test
  public void test_windowAfter() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "2100-03-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "6", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(6, explanations.size());
    for (var i = 1; i < 6; ++i) {
      assertEquals("Activity Date/Time should be on or before " + ETO_2024_03_21.DTF.format(WINDOW_CLOSE_DT) + ", not "
          + dateBase + i + ", on line: " + i, explanations.get(i - 1));
    }
  }

  @Test
  public void test_wrongTimeSequence() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "2024-03-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "6", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "5", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "4", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "3", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "2", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "1", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(5, explanations.size());
    assertEquals("Should be ascending Date/Time on line: 2, not 2024-03-01 00:05", explanations.get(0));
    assertEquals("Should be ascending Date/Time on line: 3, not 2024-03-01 00:04", explanations.get(1));
    assertEquals("Should be ascending Date/Time on line: 4, not 2024-03-01 00:03", explanations.get(2));
    assertEquals("Should be ascending Date/Time on line: 5, not 2024-03-01 00:02", explanations.get(3));
    assertEquals("Should be ascending Date/Time on line: 6, not 2024-03-01 00:01", explanations.get(4));
  }

  @Test
  public void test_tooFewMessages() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "2024-03-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(2, explanations.size());
    assertEquals("Should have exactly 3 messages from SERVICE, not 2", explanations.get(0));
    assertEquals("Should have exactly 1 message to TEST with Subject ... Three, not 0", explanations.get(1));
  }

  @Test
  public void test_tooManyMessages() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "2024-03-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "6", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "7", sender, "TEST", testBase + "Four"));
    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(1, explanations.size());
    assertTrue(explanations.get(0).startsWith("Should have a TEST or SERVICE message"));
  }

  @Test
  public void test_serviceBeforeTest() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "2024-03-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();

    activities.add(new Activity(dateBase + "0", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));

    processor.validateActivities(sender, sts, activities, WINDOW_OPEN_DT, WINDOW_CLOSE_DT);
    var explanations = sts.getExplanations();
    logger.debug("explanations:\n" + String.join("\n", explanations) + "\n");
    assertEquals(0, explanations.size());
  }
}
