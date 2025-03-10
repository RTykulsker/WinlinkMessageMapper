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

package com.surftools.wimp.processors.exercise.eto_2022;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.EtoCheckInV2Message;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ETO Check In V2 (custom form)
 *
 * @author bobt
 *
 */
public class ETO_2022_09_08 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_09_08.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override

  /**
   * Grading Message is from the ETO Check-In form 75%
   *
   * Correct Version Number entered in comment and is 5 characters long. 25%
   */
  public void process() {
    var ppCount = 0;
    var pointsCounter = new Counter();

    var ffm = new FormFieldManager();
    ffm.add("version", new FormField(FFType.SPECIFIED, "Version", "1.0.4", 0));
    ffm.add("comments", new FormField(FFType.SPECIFIED, "Version", "1.0.4", 25));
    var results = new ArrayList<IWritableTable>();
    for (var mm : mm.getMessagesForType(MessageType.ETO_CHECK_IN_V2)) {
      var m = (EtoCheckInV2Message) mm;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        // logger.info("ETO_2022_09_08 grader: " + m);
      }

      ++ppCount;
      var points = 75;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      ffm.test("version", m.version);
      ffm.test("comments", m.comments);

      points += ffm.getPoints();

      points = Math.min(100, points);
      points = Math.max(0, points);
      pointsCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over messages

    var sb = new StringBuilder();
    sb.append("\nETO-2022-09-08 Grading Report: graded " + ppCount + " ETO Check In V2 messages\n");

    for (var key : ffm.keySet()) {
      var af = ffm.get(key);
      sb.append("  " + formatPP(af.label, af.count, ppCount));
    }

    sb.append(ffm.formatCounters());

    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());

    writeTable("graded-eto_check_in_v2", results);
  }

}