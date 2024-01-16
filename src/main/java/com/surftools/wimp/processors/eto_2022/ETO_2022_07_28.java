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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;

/**
 * Winlink Check In, looking for specific text in comments, plus setup
 *
 * @author bobt
 *
 */
public class ETO_2022_07_28 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_07_28.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    int ppCount = 0;
    var pointsCounter = new Counter();

    /**
     * https://emcomm-training.org/ETO%20US%20Message%20Viewer%20Exercise%20for%207-28-2022.pdf
     *
     * SCORING
     *
     * 25 points for getting “Setup” information correct
     *
     * 25 points for getting the correct sender’s call sign
     *
     * 50 additional points for correct sender’s call sign on the first line and all upper case letters with no
     * punctuation or spaces preceding or following the call sign
     */
    var ffm = new FormFieldManager();
    ffm.add("organization", new FormField(FFType.SPECIFIED, "Agency/Group name", "ETO Winlink Thursday", 25));
    ffm.add("comments", new FormField(FFType.CONTAINS, "Comments", "W7YAM", 25));
    ffm.add("line1", new FormField(FFType.SPECIFIED, "Comments Line 1", "W7YAM", 50));

    var results = new ArrayList<IWritableTable>();
    for (var gm : mm.getMessagesForType(MessageType.CHECK_IN)) {
      var m = (CheckInMessage) gm;
      ++ppCount;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.info("ETO_2022_07_28 grader: " + m);
      }

      var points = 0;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      ffm.test("organization", m.organization);

      var comments = m.comments;
      ffm.test("comments", comments);

      String line1 = (comments == null) ? null : comments.split("\n")[0];
      ffm.test("line1", line1);

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
    sb.append("\nETO-2022-07-28 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    for (var key : ffm.keySet()) {
      var af = ffm.get(key);
      sb.append("  " + formatPP(af.label, af.count, ppCount));
    }
    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());
    writeTable("graded-check_in.csv", results);
  }

}