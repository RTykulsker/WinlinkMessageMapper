/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.processors.exercise.eto_2026;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * a plain text, with info about multiple winlink instances
 *
 * @author bobt
 *
 */
public class ETO_2026_04_16 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_04_16.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.PLAIN;

    var extraOutboundMessageText = getNextExerciseInstructions();
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void specificProcessing(ExportedMessage m) {
    var lines = m.plainContent.split("\n");
    if (lines == null || lines.length == 0) {
      count(sts.test("Message content present", false));
    } else {
      count(sts.test("Message content present", true));
      var list = Arrays.asList(lines);
      if (list.size() >= 1) {
        var line1 = list.get(0).strip();
        count(sts.test("Line 1 should be #EV", "Test Installed", line1));
      }
      if (list.size() > 1) {
        var dirCount = 0;
        for (var i = 0; i < list.size(); ++i) {
          if (i == 0) {
            continue;
          }
          var line = list.get(i).strip();
          var lineNumber = i + 1;
          sts.setExplanationPrefix("line number: " + lineNumber + ": ");
          count(sts.test("Line should contain #<DIR>", line.contains("<DIR>")));
          var fields = line.split("<DIR>");
          if (fields.length >= 2) {
            var dirName = fields[1].trim();
            getCounter("RMS directory name").increment(dirName);
            ++dirCount;
          }
          logger.info("fields: " + String.join(",", fields));
        } // end loop over DIR lines
        getCounter("RMS directory count").increment(dirCount);
      } // end if more than 1 line of message
    } // end if message content present
  }
}