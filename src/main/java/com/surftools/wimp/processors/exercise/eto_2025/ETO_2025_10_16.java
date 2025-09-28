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

package com.surftools.wimp.processors.exercise.eto_2025;

import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.DyfiMessage.DetailLevel;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * DYFI for Shakeout 2025
 *
 * @author bobt
 *
 */
public class ETO_2025_10_16 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_10_16.class);

  public static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";

  protected static final DateTimeFormatter DYFI_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter DYFI_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static final String EXPECTED_DATE = "10/16/2025";
  protected static final String EXPECTED_TIME = "10:16";

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.DYFI;
    DyfiMessage.setDetailLevel(DetailLevel.LOW);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    DyfiMessage m = (DyfiMessage) message;

    var hasUSGSAddress = (m.toList + "," + m.ccList).toUpperCase().contains(REQUIRED_USGS_ADDRESS.toUpperCase());
    count(sts.test("To and/or CC addresses must contain " + REQUIRED_USGS_ADDRESS, hasUSGSAddress));
    count(sts.test("Event Type must be: EXERCISE", !m.isRealEvent));
    count(sts.test("Exercise Id must be: #EV", "SHAKEOUT", m.exerciseId));
    count(sts.test("Did You feel it must be: Yes", m.isFelt));
    var response = m.response == null ? "Not specified" : m.response;
    count(sts.test("How did you respond must be: Dropped and covered", "duck", response));
    getCounter("Response").increment(response);

    // date and time should be 10/16 and 10:16
    count(sts.test("Date of Earthquake should be #EV", EXPECTED_DATE, m.formDateTime.format(DYFI_DATE_FORMATTER)));
    count(sts.test("Time of Earthquake should be #EV", EXPECTED_TIME, m.formDateTime.format(DYFI_TIME_FORMATTER)));

    var intensityString = m.intensity;
    getCounter("Intensity").increment(intensityString);
    try {
      intensityString = intensityString == null || intensityString.isEmpty() ? "0" : intensityString;
      var intensity = Integer.parseInt(intensityString);
      count(sts.test("Intensity should be at least 5", intensity >= 5));
    } catch (Exception e) {
      count(sts.test("Intensity should be at least 5", false));
    }
    getCounter("Version").increment(m.formVersion);

    if (feedbackLocation == null) {
      feedbackLocation = m.formLocation;
    }

    if (m.comments != null) {
      var fields = m.comments.split(",");
      if (fields.length >= 4) { // AFFILIATION, ORGANIZATION, MODE, BAND,COMMENTS
        getCounter("Affiliation").increment(fields[0].toUpperCase().trim());
        getCounter("Organization").increment(fields[1].toUpperCase().trim());
        getCounter("Mode").increment(fields[2].toUpperCase().trim());
        getCounter("Band").increment(fields[3].toUpperCase().trim());
      } // endif fields.length >= 4
    } // endif comments != null

  }

  @Override
  public void postProcess() {
    super.postProcess();
  }

}