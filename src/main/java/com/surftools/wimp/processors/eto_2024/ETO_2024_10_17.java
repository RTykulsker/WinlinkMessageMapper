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

package com.surftools.wimp.processors.eto_2024;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * DYFI for Shakeout 2024
 *
 * @author bobt
 *
 */
public class ETO_2024_10_17 extends FeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2024_10_17.class);

  public static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";

  protected static final DateTimeFormatter DYFI_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter DYFI_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static final String EXPECTED_DATE = "10/17/2024";
  protected static final String EXPECTED_TIME = "10:17";

  static record Email(String from, String address) implements IWritableTable {
    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Email" };
    }

    @Override
    public String[] getValues() {
      return new String[] { from, address };
    }

    @Override
    public int compareTo(IWritableTable o) {
      var other = (Email) o;
      return address.compareTo(other.address());
    }
  };

  private List<Email> emailAddresses = new ArrayList<Email>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var detailLevelString = cm.getAsString(Key.DYFI_DETAIL_LEVEL, "LOW");
    var detailLevel = DyfiMessage.DetailLevel.fromString(detailLevelString);
    if (detailLevel == null) {
      throw new RuntimeException("Unsupported " + Key.DYFI_DETAIL_LEVEL.toString() + " value: " + detailLevelString);
    } else {
      DyfiMessage.setDetailLevel(detailLevel);
      logger.info("### using DYFI detailLevel: " + detailLevel);
    }
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    DyfiMessage m = (DyfiMessage) message;

    var hasUSGSAddress = (m.toList + "," + m.ccList).toUpperCase().contains(REQUIRED_USGS_ADDRESS.toUpperCase());
    count(sts.test("To and/or CC addresses must contain " + REQUIRED_USGS_ADDRESS, hasUSGSAddress));
    count(sts.test("Event Type must be: EXERCISE", !m.isRealEvent));
    count(sts.test("Exercise Id must be: #EV", "ETO Winlink Thursday SHAKEOUT 2024", m.exerciseId));
    count(sts.test("Did You feel it must be: Yes", m.isFelt));
    var response = m.response == null ? "Not specified" : m.response;
    count(sts.test("How did you respond must be: Dropped and covered", "duck", response));

    // date and time should be 10/17 and 10:17
    count(sts.test("Date of Earthquake should be #EV", EXPECTED_DATE, m.formDateTime.format(DYFI_DATE_FORMATTER)));
    count(sts.test("Time of Earthquake should be #EV", EXPECTED_TIME, m.formDateTime.format(DYFI_TIME_FORMATTER)));

    // All questions starting following the Your Location up to the Additional Comments section are populated.
    if (DyfiMessage.getDetailLevel() == DyfiMessage.DetailLevel.MEDIUM) {
      count(sts.testIfPresent("Situation value should be present", m.situation));
      testPresentAbsent("If Situation/Other checked, other should be specified, else empty", m.situation,
          m.situationOther);
      count(sts.testIfPresent("Inside/Floor value should be present", m.situationFloor));
      testPresentAbsent("If Inside/Floor/Other checked, other should be specified", m.situationFloor,
          m.situationFloorOther);
      count(sts.testIfPresent("Inside/How Tall value should be present", m.structureStories));
      testPresentAbsent("If Inside/How Tall/Other checked, other should be specified", m.structureStories,
          m.structureStoriesOther);
      count(sts.testIfPresent("Where you asleep value should be present", m.situationSleep));
      count(sts.testIfPresent("Did others feel it value should be present", m.experienceResponseOther));
      count(sts.testIfPresent("Describe the Shaking value should be present", m.experienceShaking));
      count(sts.testIfPresent("Your Reaction value should be present", m.experienceReaction));
      testPresentAbsent("If Response/Other, other should be specified", m.experienceResponse,
          m.experienceResponseOther);
      count(sts.testIfPresent("Difficult to Stand value should be present", m.experienceStand));
      count(sts.testIfPresent("Swinging Doors value should be present", m.effectsDoors));
      count(sts.testIfPresent("Creaking/Noise value should be present", m.effectsSounds));
      count(sts.testIfPresent("Objects/Rattle value should be present", m.effectsShelves));
      count(sts.testIfPresent("Pictures/Move value should be present", m.effectsPictures));
      count(sts.testIfPresent("Furniture(Appliances)/slide value should be present", m.effectsFurniture));
      count(sts.testIfPresent("Heavy Appliance Affected value should be present", m.effectsAppliances));
      count(sts.testIfPresent("Walls/Fences damaged value should be present", m.effectsWalls));
    } // endif detailLevel == MEDIUM

    getCounter("Intensity").increment(m.intensity);
    getCounter("Version").increment(m.formVersion);

    if (m.comments != null) {
      var words = m.comments.split(" ");
      if (words[0].contains("@")) { // no attempt at validation, let someone else deal with email bounces
        var emailEntry = new Email(m.from, words[0]);
        emailAddresses.add(emailEntry);
      } // endif words[0] contains @
    } // endif comments != null

  }

  /**
   * convenience method to test if a variable is "present" or "absent", based on a predicate variable equally a specific
   * value. Assumed use case is when predVariable == "other"
   *
   * @param label
   * @param predVariable
   * @param predValue
   * @param variable
   */
  private void testPresentAbsent(String label, String predVariable, String predValue, String variable) {
    if (predVariable.equals(predValue)) {
      count(sts.testIfPresent(label, variable));
    } else {
      count(sts.testIfEmpty(label, variable));
    }
  }

  /**
   * convenience method to test if a variable is "present" or "absent", based on a predicate variable equally a specific
   * value. Assumed use case is when predVariable == "other"
   *
   * @param label
   * @param predVariable
   * @param variable
   */
  private void testPresentAbsent(String label, String predVariable, String variable) {
    testPresentAbsent(label, predVariable, "other", variable);
  }

  @Override
  public void postProcess() {
    super.postProcess();
    writeTable("email.csv", new ArrayList<IWritableTable>(emailAddresses));
  }

}