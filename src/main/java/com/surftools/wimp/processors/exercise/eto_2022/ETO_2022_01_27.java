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

import static com.surftools.wimp.formField.FFType.REQUIRED;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Try to parse lat/long from first line of message field of ICS-213. Not a good idea!
 *
 * @author bobt
 *
 */
public class ETO_2022_01_27 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_01_27.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override
  public void process() {
    var ppCount = 0;
    var ppMessageBodyLine1IsLocation = 0;

    var ffm = new FormFieldManager();
    ffm.add("formMessage", new FormField(REQUIRED, "Message Body", null, 50));

    var scoreCounter = new Counter();

    var results = new ArrayList<IWritableTable>();
    for (var message : mm.getMessagesForType(MessageType.ICS_213)) {
      Ics213Message m = (Ics213Message) message;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.debug("messageId: " + m.messageId + ", from: " + m.from);
      }

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      var formMessage = m.formMessage == null ? null : m.formMessage.trim();
      ffm.test("formMessage", formMessage);

      if (formMessage != null) {
        var line1 = getLine1FromMessage(formMessage);
        if (line1 == null || line1.isEmpty()) {
          explanations.add("line 1 of message body is empty");
        } else {
          var messageLocation = getLocationFromLine1(line1);
          if (messageLocation.isValid()) {
            points += 50;
            ++ppMessageBodyLine1IsLocation;
            // this exercise was run before exported messages had locations
            message.mapLocation = messageLocation;
          } else {
            explanations.add("could not easily parse lat/long line 1: " + line1);
          }
        }
      }

      points = ffm.getPoints();
      points = Math.min(100, points);
      points = Math.max(0, points);
      scoreCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over ICS-213 messages

    var sb = new StringBuilder();

    sb.append("\nScorable Actionable Fields\n");
    for (var key : ffm.keySet()) {
      var af = ffm.get(key);
      sb.append("  " + formatPP(af.label, af.count, ppCount));
    }
    sb.append("  " + formatPP("Valid lat/long", ppMessageBodyLine1IsLocation, ppCount));

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());

    writeTable("graded-ics-213.csv", results);
  }

  private String getLine1FromMessage(String formMessage) {
    if (formMessage == null) {
      return null;
    }

    var lines = formMessage.split("\n");
    if (lines != null) {
      return lines[0];
    }

    return null;
  }

  private LatLongPair getLocationFromLine1(String line1) {
    var fields = line1.split(",");
    if (fields.length >= 2) {
      return new LatLongPair(fields[0].trim(), fields[1].trim());
    }
    return LatLongPair.ZERO_ZERO;
  }

}