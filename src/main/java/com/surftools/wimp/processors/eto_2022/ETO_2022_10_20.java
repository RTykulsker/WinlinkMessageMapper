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

package com.surftools.wimp.processors.eto_2022;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * DYFI for Shakeout 2022
 *
 * @author bobt
 *
 */
public class ETO_2022_10_20 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_10_20.class);

  public static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  /**
   * This Earthquake report is an: check Exercise. In case of a “real world event”, do not send messages to your ETO
   * clearinghouse. We are a training organization, not a response organization. Your message may sit, unread, for days.
   * For this exercise, if you check REAL EVENT, you will earn a score of 0 points for this exercise, regardless of
   * subsequent answers.
   *
   * Optional Exercise ID: ETO Winlink Thursday SHAKEOUT 2022 (50 points)
   *
   * How did you respond? Check Dropped and Covered (50 points). Drop, cover and hold is the recommended guideline for
   * how to respond to an earthquake. Even if that is not what you actually did at 10:20 AM or if there was no place to
   * cover yourself, Dropped and Covered is the answer we want.
   */
  @Override
  public void process() {
    var ppCount = 0;
    var ppIsExerciseOk = 0;
    var ppAddressToUSGSOk = 0;
    var ppAutoFailCount = 0;

    var ffm = new FormFieldManager();
    ffm
        .add("exerciseId",
            new FormField(FFType.SPECIFIED, "Exercise Id", "ETO WINLINK THURSDAY SHAKEOUT 2022", 50));
    ffm.add("response", new FormField(FFType.SPECIFIED, "Response", "duck", 50));

    var scoreCounter = new Counter();
    var responseCounter = new Counter();
    var exerciseIdCounter = new Counter();
    var intensityCounter = new Counter();
    var versionCounter = new Counter();

    var noUSGSCallAddressMap = new TreeMap<String, String>();

    var results = new ArrayList<IWritableTable>();
    for (var message : mm.getMessagesForType(MessageType.DYFI)) {
      DyfiMessage m = (DyfiMessage) message;
      var call = m.from;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.debug("messageId: " + m.messageId + ", from: " + m.from);
      }

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);
      var autoFail = false;

      var isRealEvent = m.isRealEvent;
      if (isRealEvent) {
        explanations.add("FAIL: must not select 'Real Event'");
        autoFail = true;
        ++ppAutoFailCount;
      } else {
        ++ppIsExerciseOk;
      }

      var addresses = new HashSet<String>();
      addresses.addAll(Arrays.asList(m.toList.replaceAll("SMTP:", "").split(",")));
      addresses.addAll(Arrays.asList(m.ccList.replaceAll("SMTP:", "").split(",")));

      if (!addresses.contains(REQUIRED_USGS_ADDRESS)) {
        explanations.add("FAIL: To: and Cc: addresses don't contain required address: " + REQUIRED_USGS_ADDRESS);
        autoFail = true;
        ++ppAutoFailCount;
        noUSGSCallAddressMap.put(call, addresses.toString());
      } else {
        ++ppAddressToUSGSOk;
      }

      var exerciseId = m.exerciseId == null ? null : m.exerciseId.trim().toUpperCase();
      exerciseIdCounter.incrementNullSafe(exerciseId);
      ffm.test("exerciseId", exerciseId);

      var response = m.response;
      responseCounter.incrementNullSafe(response);
      ffm.test("response", response);

      var intensityString = m.intensity;
      intensityCounter.incrementNullSafe(intensityString);

      var version = m.formVersion;
      versionCounter.incrementNullSafe(version);

      points = ffm.getPoints();
      points = Math.min(100, points);
      points = Math.max(0, points);
      points = autoFail ? 0 : points;
      scoreCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over DYFI messages

    var sb = new StringBuilder();
    sb.append("Automatic Fail Count: " + ppAutoFailCount + "\n");
    sb.append(formatPP("Is Exerercise", ppIsExerciseOk, ppCount));
    sb.append(formatPP("Addressed to USGS", ppAddressToUSGSOk, ppCount));

    sb.append("\nScorable Actionable Fields\n");
    for (var key : ffm.keySet()) {
      var af = ffm.get(key);
      sb.append("  " + formatPP(af.label, af.count, ppCount));
    }

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));
    sb.append("\nResponses: \n" + formatCounter(responseCounter.getDescendingCountIterator(), "response", "count"));
    sb
        .append(
            "\nExerciseId: \n" + formatCounter(exerciseIdCounter.getDescendingCountIterator(), "exerciseId", "count"));
    sb.append("\nIntensity: \n" + formatCounter(intensityCounter.getDescendingKeyIterator(), "intensity", "count"));
    sb.append("\nVersion: \n" + formatCounter(versionCounter.getDescendingKeyIterator(), "version", "count"));

    sb.append("\nFailed to address message to USGS: (" + REQUIRED_USGS_ADDRESS + ")\n");
    for (var call : noUSGSCallAddressMap.keySet()) {
      sb.append("  " + call + ", addresses: " + noUSGSCallAddressMap.get(call) + "\n");
    }

    logger.info(sb.toString());

    writeTable("graded-dyfi.csv", results);
  }

}