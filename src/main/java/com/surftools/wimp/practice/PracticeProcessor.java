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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PracticeProcessor extends SingleMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PracticeProcessor.class);
  private LocalDate date;
  private MessageType messageType;
  private ExportedMessage referenceMessage;

  private List<Summary> summaries = new ArrayList<>();

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

    var summary = new Summary(m);
    summaries.add(summary);
  }

  private void handle_Ics213(ExportedMessage message) {
    var m = (Ics213Message) message;
    var ref = (Ics213Message) referenceMessage;

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));

    // TODO compare and contrast
  }

  @Override
  protected void endProcessingForSender(String sender) {
  }

  @Override
  public void postProcess() {
    WriteProcessor.writeTable("practice-summaries", summaries);
    super.postProcess();
  }

  protected class Summary implements IWritableTable {
    public String from;
    public String to;
    public LatLongPair location;
    public LocalDateTime dateTime;
    public List<String> explanations; // doesn't get published, but interpreted
    public String messageId;
    public int exerciseCount;
    public LocalDate firstDate;

    public static final String perfectMessageText = "Perfect messages!";
    public static final int perfectMessageCount = 0; // in case we need to adjust

    public Summary(ExportedMessage m) {
      this.from = m.from;
      this.to = m.to;
      this.location = m.mapLocation;
      this.dateTime = m.sortDateTime;
      this.messageId = m.messageId;
      this.explanations = sts.getExplanations();

      // TODO get exerciseCount, firstDate from database
    }

    @Override
    public int compareTo(IWritableTable o) {
      var other = (Summary) o;
      return from.compareTo(other.from);
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>(List
          .of("From", "To", "Latitude", "Longitude", "Date", "Time", "Feedback Count", "Feedback", "Message Id",
              "Exercise Count", "First Date"));
      return list.toArray(new String[list.size()]);
    }

    @Override
    public String[] getValues() {
      var latitude = location == null ? "0.0" : location.getLatitude();
      var longitude = location == null ? "0.0" : location.getLongitude();
      var date = dateTime == null ? "" : dateTime.toLocalDate().toString();
      var time = dateTime == null ? "" : dateTime.toLocalTime().toString();
      var feedbackCount = "0";
      var feedback = perfectMessageText;

      if (explanations.size() > 0) {
        feedbackCount = String.valueOf(explanations.size() - perfectMessageCount);
        feedback = String.join("\n", explanations);
      }

      var nsTo = to == null ? "(null)" : to;

      var list = new ArrayList<String>(List
          .of(from, nsTo, latitude, longitude, date, time, feedbackCount, feedback, messageId, s(exerciseCount),
              firstDate.toString()));
      return list.toArray(new String[list.size()]);
    }
  }
}
