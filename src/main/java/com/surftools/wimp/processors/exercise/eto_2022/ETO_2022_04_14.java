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
import java.util.Arrays;
import java.util.HashSet;

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
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class ETO_2022_04_14 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_04_14.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {

    var precedenceFF = new FormField(FFType.SPECIFIED, "Precedence", "(R) - Routine", 10);
    var taskFF = new FormField(FFType.SPECIFIED, "Task", "WLT-001", 10);
    var isHelpNeededFF = new FormField(FFType.SPECIFIED, "Is Help Needed", "NO", 10);
    var neededHelpFF = new FormField(FFType.EMPTY, "Needed Help", null, 10);

    final var THIS_IS_AN_EXERCISE = "this is an exercise";
    var tvCommentsFF = new FormField(FFType.SPECIFIED, "TV Comments", THIS_IS_AN_EXERCISE, 10);
    var waterCommentsFF = new FormField(FFType.SPECIFIED, "Water Comments", THIS_IS_AN_EXERCISE, 10);
    var internetCommentsFF = new FormField(FFType.SPECIFIED, "Internet Comments", THIS_IS_AN_EXERCISE, 10);

    var eastFFM = new FormFieldManager();
    var centralFFM = new FormFieldManager();
    var westFFM = new FormFieldManager();
    var ffms = new FormFieldManager[] { eastFFM, centralFFM, westFFM };
    for (int i = 0; i < ffms.length; ++i) {
      var ffm = ffms[i];
      ffm.add("precedence", precedenceFF);
      ffm.add("task", taskFF);
      ffm.add("isHelpNeeded", isHelpNeededFF);
      ffm.add("neededHelp", neededHelpFF);

      if (i == 0) {
        ffm.add("tvStatus", new FormField(FFType.SPECIFIED, "TV Status", "YES", 10));
        ffm.add("waterStatus", new FormField(FFType.SPECIFIED, "Water Status", "Unknown - N/A", 10));
        ffm.add("internetStatus", new FormField(FFType.SPECIFIED, "Internet Status", "NO", 10));
      } else if (i == 1) {
        ffm.add("tvStatus", new FormField(FFType.SPECIFIED, "TV Status", "Unknown - N/A", 10));
        ffm.add("waterStatus", new FormField(FFType.SPECIFIED, "Water Status", "NO", 10));
        ffm.add("internetStatus", new FormField(FFType.SPECIFIED, "Internet Status", "YES", 10));
      } else {
        ffm.add("tvStatus", new FormField(FFType.SPECIFIED, "TV Status", "NO", 10));
        ffm.add("waterStatus", new FormField(FFType.SPECIFIED, "Water Status", "YES", 10));
        ffm.add("internetStatus", new FormField(FFType.SPECIFIED, "Internet Status", "Unknown - N/A", 10));
      }

      ffm.add("tvComments", tvCommentsFF);
      ffm.add("waterComments", waterCommentsFF);
      ffm.add("internetComments", internetCommentsFF);
    }

    var eastSet = new HashSet<String>(Arrays.asList("ETO-01", "ETO-02", "ETO-03", "ETO-04"));
    var centralSet = new HashSet<String>(Arrays.asList("ETO-05", "ETO-06", "ETO-07", "ETO-08"));
    var westSet = new HashSet<String>(Arrays.asList("ETO-09", "ETO-10", "ETO-DX"));
    var regionSets = new ArrayList<HashSet<String>>();
    regionSets.add(eastSet);
    regionSets.add(centralSet);
    regionSets.add(westSet);

    var ppCount = 0;
    var ppAutoFail = 0;
    var scoreCounter = new Counter();

    var results = new ArrayList<IWritableTable>();
    var it = mm.getSenderIterator();
    while (it.hasNext()) {
      var sender = it.next();
      var messageList = mm.getMessagesForSender(sender).get(MessageType.FIELD_SITUATION);
      if (messageList == null) {
        continue;
      }
      if (messageList.size() != 1) {
        logger.info("### expected 1 FSR message, got: " + messageList.size() + ", skipping!");
        continue;
      }
      var m = (FieldSituationMessage) messageList.get(0);

      var points = 0;
      var explanations = new ArrayList<String>();
      boolean isFailure = false;

      ++ppCount;

      var to = m.to;
      FormFieldManager ffm = null;
      if (to.equals("ETO-01") || to.equals("ETO-02") || to.equals("ETO-03") || to.equals("ETO-04")) {
        ffm = eastFFM;
      } else if (to.equals("ETO-05") || to.equals("ETO-06") || to.equals("ETO-07") || to.equals("ETO-08")) {
        ffm = centralFFM;
      } else {
        ffm = westFFM;
      }
      ffm.reset(explanations);

      // automatic fail with 0 grade if not Routine precedence
      var precedence = m.precedence;
      ffm.test("precedence", precedence);
      if (!precedence.equals("(R) - Routine")) {
        ++ppAutoFail;
        isFailure = true;
      }

      ffm.test("task", m.task);
      ffm.test("isHelpNeeded", m.isHelpNeeded);
      ffm.test("neededHelp", m.neededHelp);

      ffm.test("tvStatus", m.tvStatus);
      ffm.test("tvComments", m.tvComments);
      ffm.test("waterStatus", m.waterStatus);
      ffm.test("waterComments", m.waterComments);
      ffm.test("internetStatus", m.internetStatus);
      ffm.test("internetComments", m.internetComments);

      points += ffm.getPoints();
      points = Math.min(100, points);
      points = Math.max(0, points);

      if (isFailure) {
        points = 0;
      }

      scoreCounter.increment(points);
      var grade = String.valueOf(points);

      var explanation = (points == 100) ? "perfect score" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over senders

    var sb = new StringBuilder();

    sb.append("\nSummary for ETO-2022-04-14\n");
    sb.append("\nFSR messages: " + ppCount + "\n");
    sb.append("Automatic Fail Count: " + ppAutoFail + "\n");

    var ffm = new FormFieldManager();

    var summableFieldKeys = new String[] { "tvStatus", "tvComments", "waterStatus", "waterComments", "internetStatus",
        "internetComments" };
    ffm.merge(ffms, summableFieldKeys);

    sb.append("\nScorable Actionable Fields\n");
    for (var key : ffm.keySet()) {
      var af = ffm.get(key);
      sb.append(" " + formatPP(af.label, af.count, ppCount));
    }

    sb.append("\nScorable Action Field Counts" + ffm.formatCounters());

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());

    writeTable("graded-fsr.csv", results);

  }

}