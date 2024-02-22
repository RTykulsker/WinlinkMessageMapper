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

package com.surftools.wimp.processors.eto_2024;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.surftools.wimp.message.Ics309Message.Activity;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;

public class ETO_2024_03_21_Test {

  @Test
  public void test_null() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    List<Activity> activities = new ArrayList<>();

    processor.validateActivities(null, sts, activities);
    assertEquals(0, sts.getExplanations().size());

    processor.validateActivities(sender, null, activities);
    assertEquals(0, sts.getExplanations().size());

    processor.validateActivities(sender, sts, null);
    assertEquals(0, sts.getExplanations().size());

    processor.validateActivities(sender, sts, activities);
    assertEquals(0, sts.getExplanations().size());

    activities.add(null);
    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(6, explanations.size());
    assertEquals("Should have only 6 activities, not 0", explanations.get(0));
    assertEquals("Should have exactly 3 messages to TEST, not 0", explanations.get(1));
    assertEquals("Should have exactly 3 messages from SERVICE, not 0", explanations.get(2));
    assertEquals("Should have exactly 1 message to TEST with Subject ... One, not 0", explanations.get(3));
    assertEquals("Should have exactly 1 message to TEST with Subject ... Two, not 0", explanations.get(4));
    assertEquals("Should have exactly 1 message to TEST with Subject ... Three, not 0", explanations.get(5));

    sts.reset();
    activities.clear();
    activities.add(new Activity(null, null, null, null));
    processor.validateActivities(sender, sts, activities);
    explanations = sts.getExplanations();
    assertEquals(6, sts.getExplanations().size());
    assertEquals("Should have only 6 activities, not 0", explanations.get(0));
    assertEquals("Should have exactly 3 messages to TEST, not 0", explanations.get(1));
    assertEquals("Should have exactly 3 messages from SERVICE, not 0", explanations.get(2));
    assertEquals("Should have exactly 1 message to TEST with Subject ... One, not 0", explanations.get(3));
    assertEquals("Should have exactly 1 message to TEST with Subject ... Two, not 0", explanations.get(4));
    assertEquals("Should have exactly 1 message to TEST with Subject ... Three, not 0", explanations.get(5));
  }

  @Test
  public void test_invalid_date() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity("a date", sender, sender, sender));
    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(8, explanations.size());
    assertEquals("Should have only 6 activities, not 1", explanations.get(0));
    assertEquals("Should have valid activity Date/Time, not 'a date', on line: 1", explanations.get(1));
  }

  @Test
  public void test_happyPath() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "1970-01-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "6", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(0, explanations.size());
  }

  @Test
  public void test_wrongTimeSequence() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "1970-01-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "6", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "5", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "4", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "3", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "2", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "1", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(5, explanations.size());
    assertEquals("Should be ascending Date/Time on line: 2, not 1970-01-01 00:05", explanations.get(0));
  }

  @Test
  public void test_tooFewMessages() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "1970-01-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(4, explanations.size());
    assertEquals("Should have only 6 activities, not 4", explanations.get(0));
    assertEquals("Should have exactly 3 messages to TEST, not 2", explanations.get(1));
    assertEquals("Should have exactly 3 messages from SERVICE, not 2", explanations.get(2));
    assertEquals("Should have exactly 1 message to TEST with Subject ... Three, not 0", explanations.get(3));
  }

  @Test
  public void test_tooManyMessages() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "1970-01-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "6", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "7", sender, "TEST", testBase + "Four"));
    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(2, explanations.size());
    assertEquals("Should have only 6 activities, not 7", explanations.get(0));
    assertTrue(explanations.get(1).startsWith("Should have a TEST or SERVICE message"));
  }

  @Test
  public void test_serviceBeforeTest() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "1970-01-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();

    activities.add(new Activity(dateBase + "0", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "Three"));

    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(3, explanations.size());
    assertEquals("Should have more TEST messages than SERVICE messages at line: 1", explanations.get(0));
    assertEquals("Should have more TEST messages than SERVICE messages at line: 3", explanations.get(1));
    assertEquals("Should have more TEST messages than SERVICE messages at line: 5", explanations.get(2));
  }

  @Test
  public void test_3_before_2_or_1() {
    var sender = "UNIT TEST";
    var processor = new ETO_2024_03_21();
    var sts = new SimpleTestService();
    var dateBase = "1970-01-01 00:0";
    var testBase = "ETO Exercise, Winlink Simulated Emergency Message ";
    List<Activity> activities = new ArrayList<>();
    activities.add(new Activity(dateBase + "1", sender, "TEST", testBase + "Three"));
    activities.add(new Activity(dateBase + "2", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "3", sender, "TEST", testBase + "Two"));
    activities.add(new Activity(dateBase + "4", "SERVICE", sender, "Test Message"));
    activities.add(new Activity(dateBase + "5", sender, "TEST", testBase + "One"));
    activities.add(new Activity(dateBase + "6", "SERVICE", sender, "Test Message"));
    processor.validateActivities(sender, sts, activities);
    var explanations = sts.getExplanations();
    System.err.println(String.join("\n", explanations));
    System.err.flush();
    assertEquals(2, explanations.size());
    assertEquals("Should have at least one TEST One or TEST Two message before TEST Three, not (0 or 0) on line: 1",
        explanations.get(0));
    assertEquals("Should have at least one TEST One message before TEST Two, not 0, on line: 3", explanations.get(1));
  }
}
