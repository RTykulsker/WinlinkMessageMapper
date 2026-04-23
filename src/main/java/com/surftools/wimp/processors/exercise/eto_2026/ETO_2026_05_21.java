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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CustomizableFormMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * a CustomizableForm message
 *
 * @author bobt
 *
 */
public class ETO_2026_05_21 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_05_21.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.CUSTOMIZABLE_FORM;

    var extraOutboundMessageText = getNextExerciseInstructions();
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (CustomizableFormMessage) message;
    count(sts.test("Organization name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Event or Use Name should be #EV", "Customizable Information Form Exercise", m.incidentName));
    count(sts.test("Description or Form Info should be #EV", "Alternative/Non-emergency Phone Numbers", m.description));
    count(sts.testIfPresent("Form Date/Time should be present", m.formDateTimeString));

    count(sts.test("1st column name should be #EV", "Agency", m.columnAName));
    count(sts.test("2nd column name should be #EV", "Name/Station #", m.columnBName));
    count(sts.test("3rd column name should be #EV", "Alternative/Non-Emergency Number", m.columnCName));

    for (int i = 1; i <= 3; ++i) {
      var entry = m.entryArray[i];
      count(sts.testIfPresent("Row " + i + " agency should be present", entry.assignment()));
      count(sts.testIfPresent("Row " + i + " name should be present", entry.name()));
      count(sts.testIfPresent("Row " + i + " number should be present", entry.method()));
    }

    count(sts.test("Comments should be #EV", "Exercise, exercise", m.comments));
  }
}