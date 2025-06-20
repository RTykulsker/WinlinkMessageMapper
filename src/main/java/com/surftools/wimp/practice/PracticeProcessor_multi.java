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

package com.surftools.wimp.practice;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PracticeProcessor_multi extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PracticeProcessor_multi.class);
  private LocalDate date;
  private MessageType messageType;
  private ExportedMessage referenceMessage;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    date = LocalDate.parse(dateString);
    var ord = PracticeUtils.getOrdinalDayOfWeek(date);
    var dow = date.getDayOfWeek();
    if (dow != DayOfWeek.THURSDAY) {
      throw new RuntimeException("Exercise Date: " + dateString + " is NOT a THURSDAY, but is " + dow.toString());
    }

    var ordinalList = new ArrayList<Integer>(PracticeGeneratorTool.VALID_ORDINALS);
    Collections.sort(ordinalList);
    var ordinalLabels = ordinalList.stream().map(i -> PracticeUtils.getOrdinalLabel(i)).toList();
    if (!PracticeGeneratorTool.VALID_ORDINALS.contains(ord)) {
      throw new RuntimeException(
          "Exercise Date: " + dateString + " is NOT one of " + String.join(",", ordinalLabels) + " THURSDAYS");
    }

    messageType = PracticeGeneratorTool.MESSAGE_TYPE_MAP.get(ord);
    if (messageType == null) {
      throw new RuntimeException("No messageType for ordinal: " + ord);
    }
    // #MM must define acceptableMessages
    acceptableMessageTypesSet.add(messageType);

    referenceMessage = (ExportedMessage) mm.getContextObject(PracticeProcessorTool.REFERENCE_MESSAGE_KEY);

    super.initialize(cm, mm, logger);
  }

  @Override
  protected void specificProcessing(ExportedMessage m) {
    switch (messageType) {
    case ICS_213:
      handle_Ics213(m);
      break;
    default:
      throw new RuntimeException("unsupported messageType: " + messageType.toString() + " for sender: " + m.from);
    }
  }

  private void handle_Ics213(ExportedMessage message) {
    var m = (Ics213Message) message;
    var ref = (Ics213Message) referenceMessage;

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));

    // TODO compare and contrast
  }

  @Override
  protected void endProcessingForSender(String sender) {
    // TODO Auto-generated method stub

  }

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  protected class Summary extends BaseSummary {
    public String messageId;
    public int exerciseCount;
    public LocalDate firstDate;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list.addAll(Arrays.asList(new String[] { "MessageId", "Exercise Count", "First Date" }));
      return list.toArray(new String[0]);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list.addAll(Arrays.asList(new String[] { messageId, s(exerciseCount), firstDate.toString() }));
      return list.toArray(new String[0]);
    };

  }
}
