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

import java.nio.file.Path;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * alternative grader on a Winlink Check In message in lieu of Position Report
 *
 * @author bobt
 *
 */
public class ETO_2022_04_28 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_04_28.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    var results = new ArrayList<IWritableTable>();
    var gradeCounter = new Counter();
    for (var message : mm.getMessagesForType(MessageType.CHECK_IN)) {

      CheckInMessage m = (CheckInMessage) message;
      var grade = "";
      String explanation = null;

      var comments = m.comments;
      if (comments != null && comments.equals("Alternate Exercise for 4/28/2022")) {
        grade = "Alternative OK";
      } else {
        grade = "Not graded";
      }
      gradeCounter.increment(grade);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    }
    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-04-28 alternative results (Winlink Check in):\n");
    sb.append("\nGrades: \n" + formatCounter(gradeCounter.getDescendingKeyIterator(), "grade", "count"));
    logger.info(sb.toString());

    WriteProcessor.writeTable(results, Path.of(outputPathName, "graded-check-in"));
  }
}