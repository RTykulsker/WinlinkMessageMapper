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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.AcknowledgementProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PracticeProcessor extends SingleMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PracticeProcessor.class);
  private LocalDate date;
  private MessageType messageType;
  private ExportedMessage referenceMessage;

  private List<Summary> summaries = new ArrayList<>();

  protected Map<String, String> ackTextMap;

  protected String nextInstructions;

  protected final List<String> clearinghouseList = new ArrayList<String>();

  @SuppressWarnings("unchecked")
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
    ackTextMap = (Map<String, String>) mm.getContextObject(AcknowledgementProcessor.ACK_TEXT_MAP);

    nextInstructions = (String) mm.getContextObject(PracticeProcessorTool.INSTRUCTIONS_KEY);

    for (var i = 1; i <= 9; ++i) {
      clearinghouseList.add("ETO-0" + i + "@winlink.org");
    }
    for (var extra : List.of("ETO-10", "ETO-BK", "ETO-CAN", "ETO-DX")) {
      clearinghouseList.add(extra + "@winlink.org");
    }
  }

  @Override
  protected void beginCommonProcessing(ExportedMessage m) {
    super.beginCommonProcessing(m);
    var addressesString = m.toList + "," + m.ccList;
    var addressesList = Arrays.asList(addressesString.split(","));

    var result = addressesList.stream().distinct().filter(clearinghouseList::contains).toList();

    var intersection = String.join(",", result);
    var pred = intersection.length() == 0;
    count(sts.test("To and Cc list should not contain \"monthly/training\" addresses", pred, intersection));
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
    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    var m = (Ics213Message) message;
    var ref = (Ics213Message) referenceMessage;

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", ref.incidentName, m.incidentName));
    count(sts.test("Form To should be #EV", ref.formTo, m.formTo));
    count(sts.test("Form From should be #EV", ref.formFrom, m.formFrom));
    count(sts.test("Form Subject should be #EV", ref.formSubject, m.formSubject));

    var formDateTime = LocalDateTime.parse(m.formDate + " " + m.formTime, dtf);
    count(sts.testOnOrAfter("Form Date and Time should be on or after #EV", windowOpenDT, formDateTime, dtf));
    count(sts.testOnOrBefore("Form Date and Time should be on or before #EV", windowCloseDT, formDateTime, dtf));

    count(sts.test("Message should be #EV", ref.formMessage, m.formMessage));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));
    count(sts.test("Position/Title should be #EV", ref.position, m.position));
  }

  @Override
  protected String makeOutboundMessageSubject(Object object) {
    return "ETO Practice Exercise Feedback for " + date;
  }

  @Override
  protected void endCommonProcessing(ExportedMessage m) {
    var ackText = ackTextMap.get(sender);
    var sb = new StringBuilder();
    sb.append("ACKNOWLEDGEMENTS" + "\n");
    sb.append(ackText);
    sb.append("FEEDBACK" + "\n");
    outboundMessagePrefixContent = sb.toString();

    outboundMessagePostfixContent = nextInstructions;
    super.endCommonProcessing(m);
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
    public int feedbackCount;
    public List<String> explanations;
    public String messageId;
    public String messageType;
    public int exerciseCount; // history TBD
    public LocalDate firstDate; // history TBD

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
      var firstDateString = firstDate == null ? "(null)" : firstDate.toString();

      var list = new ArrayList<String>(List
          .of(from, nsTo, latitude, longitude, date, time, feedbackCount, feedback, messageId, //
              s(exerciseCount), firstDateString));
      return list.toArray(new String[list.size()]);
    }
  }

}
