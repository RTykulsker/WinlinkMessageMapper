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

package com.surftools.wimp.processors.named;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.FieldTestService;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;

/**
 * Processor for 2023-01_18 Exercise: an ICS-213, Winter Field Day questions
 *
 * Exercise Message Submission Window 2023-11-07 00:00 - 2023-11-12 23:59 UTC
 *
 * @author bobt
 *
 */
public class ETO_2024_01_18 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_01_18.class);
  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  final LocalDateTime windowOpenDT = LocalDateTime.of(2024, 01, 16, 0, 0, 0);
  final LocalDateTime windowCloseDT = LocalDateTime.of(2024, 01, 19, 07, 59, 0);

  private FieldTestService fts;

  /**
   * result of parsing the message text
   */
  static record ParseRecord(String willParticipate, String exchange, String opCallSign, String className,
      String category, String locationId, String[] lines, String[] line2Words) {

    public boolean isValid() {
      if (lines == null || lines.length == 0 || lines.length > 2) {
        return false;
      }

      if (willParticipate.equals("OTHER")) {
        return false;
      }

      if (willParticipate.equals("NO") && lines.length != 1) {
        return false;
      }

      if (willParticipate.equals("YES") && lines.length == 2) {
        if (line2Words != null && line2Words.length != 4) {
          return false;
        }
      }

      return true;
    }
  }

  static record Result(String call, String latitude, String longitude, String feedback, String feedbackCountString,
      ParseRecord parseRecord, //
      Ics213Message message) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { "Call", "Map Latitude", "Map Longitude", "Feedback Count", "Feedback",
              "WillPlay", "Exchange", "OpCall", "Class", "Category", "LocationId" });
      Collections.addAll(resultList, Ics213Message.getStaticHeaders());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(Ics213Message.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { call, latitude, longitude, feedbackCountString, feedback, //
              parseRecord.willParticipate, parseRecord.exchange, parseRecord.opCallSign, parseRecord.className,
              parseRecord.category, parseRecord.locationId });
      Collections.addAll(resultList, message.getValues());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      return this.message.compareTo(o.message);
    }
  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    fts = new FieldTestService();
    fts.add("addresses-bk", "To and/or CC addresses should contain ETO-BK");
    fts.add("windowOpen", "Message should be sent on or after " + DT_FORMATTER.format(windowOpenDT), windowOpenDT);
    fts.add("windowClose", "Message should be sent on or before " + DT_FORMATTER.format(windowCloseDT), windowCloseDT);
    fts.add("org", "Agency/Group should be #EV", "EmComm Training Organization");
    fts.add("isExercise", "This Is An Exercise should be checked");
    fts.add("box1_IncidentName", "Box 1 IncidentName should be #EV", "ETO Winter Field Day 2024 Poll");
    fts
        .add("box2_To", "Box 2To address should be an ETO clearinghouse",
            new HashSet<String>(Arrays.asList(cm.getAsString(Key.EXPECTED_DESTINATIONS).split(","))));
    fts.add("box3_From", "Box 3 From address correct");
    fts.add("box4_Subject", "Box 4 Subject should be #EV", "Winter Field Day 2024 Participation");
    fts.add("box5_Date", "Box 5 Date should be supplied");
    fts.add("box6_Time", "Box 6 Time should be supplied");
    fts.add("box7_Line1_Yes_No", "Box 7 Line 1 should only be YES or NO");
    fts.add("box7_Line2_IFF", "Box 7 Line 2 should only be specified if Line 1 is YES");
    fts.add("box7_Line2_Fields", "Box 7 Line 2 should have 4 fields");
    fts.add("box7_Lines", "Box 7 Should have only 1 or 2 lines");
    fts.add("box8_Approved", "Box 8 Approved should be correctly formatted");
    fts.add("box8b_Position", "Box 8b Postion/Title should be #EV", "Winlink Thursday Participant");
    fts.add("msgLocation", "LAT/LON should be provided");
  }

  @Override
  public void process() {

    var ppCount = 0;
    var ppMessageCorrectCount = 0;

    var ppFeedBackCounter = new Counter();
    var ppWillParticipateCounter = new Counter();
    var ppClassCounter = new Counter();
    var ppCategoryCounter = new Counter();
    var ppLocationCounter = new Counter();

    var callResultsMap = new HashMap<String, IWritableTable>();
    var badLocationCalls = new ArrayList<String>();

    for (var m : mm.getMessagesForType(MessageType.ICS_213)) {
      var message = (Ics213Message) m;
      var sender = message.from;

      fts.reset();

      if (dumpIds.contains(sender)) {
        logger.info("dump: " + sender);
      }

      ++ppCount;

      var addressList = m.toList + "," + m.ccList;
      if (!addressList.contains("ETO-BK")) {
        fts.fail("addresses-bk");
      }

      fts.testOnOrAfter("windowOpen", m.msgDateTime, DT_FORMATTER);
      fts.testOnOrBefore("windowClose", m.msgDateTime, DT_FORMATTER);
      fts.test("org", message.organization);

      fts.test("isExercise", String.valueOf(message.isExercise), message.isExercise);
      fts.test("box1_IncidentName", message.incidentName);
      fts.testSetOfStrings("box2_To", message.formTo);

      if (!message.formFrom.equalsIgnoreCase(sender + " Winlink Thursday Participant")) {
        fts
            .fail("box3_From",
                "Box 3 From should be " + sender + " Winlink Thursday Participant, not " + message.formFrom);
      }

      fts.test("box4_Subject", message.formSubject);
      fts.testIfPresent("box5_Date", message.formDate);
      fts.testIfPresent("box6_Time", message.formTime);

      var parseRecord = parse(message.formMessage);
      var willParticipate = parseRecord.willParticipate;
      switch (willParticipate) {
      case "YES":
        if (parseRecord.isValid()) {
          ppClassCounter.increment(parseRecord.className);
          ppCategoryCounter.increment(parseRecord.category);
          ppLocationCounter.increment(parseRecord.locationId);
        } else {
          fts.fail("box7_Line2_Fields", "Box 7 Should have 4 fields");
        }
        if (parseRecord.lines.length != 2) {
          fts.fail("box7_Lines", message.formMessage);
        }
        break;
      case "NO":
        if (parseRecord.lines.length != 1) {
          fts.fail("box7_Lines", message.formMessage);
        }
        break;
      case "OTHER":
        fts.fail("box7_Line1_Yes_No", message.formMessage);
        if (parseRecord.lines.length == 0 || parseRecord.lines().length > 2) {
          fts.fail("box7_Lines", message.formMessage);
        }
        if (parseRecord.lines.length == 2) {
          fts.fail("box7_Line2_IFF", message.formMessage);
        }
      default:
        break;
      }

      if (parseRecord.lines.length == 0 || parseRecord.lines().length > 2) {
        fts.fail("box7_Lines", message.formMessage);
      }

      var approvedBy = message.approvedBy;
      var expectedApprovedBy = " " + sender;
      if (approvedBy == null || !approvedBy.toLowerCase().endsWith(expectedApprovedBy.toLowerCase())
          || approvedBy.length() == expectedApprovedBy.length()) {
        fts.fail("box8_Approved", message.approvedBy);
      }

      fts.test("box8b_Position", message.position);

      var pair = message.formLocation == null ? message.msgLocation : message.formLocation;
      if (pair == null) {
        pair = LatLongPair.ZERO_ZERO;
        badLocationCalls.add(sender);
        fts.fail("msgLocation", "Missing LAT/LON");
      } else if (!pair.isValid() || pair.equals(LatLongPair.ZERO_ZERO)) {
        badLocationCalls.add(sender);
        fts.fail("msgLocation", "Invalid LAT/LON");
      } else {
        fts.pass("msgLocation");
      }

      var feedback = "Perfect Message";
      var feedbackCountString = "0";
      var explanations = fts.getExplanations();
      if (explanations.size() == 0) {
        ++ppMessageCorrectCount;
        ppFeedBackCounter.increment("0");
      } else {
        feedback = String.join("\n", explanations);
        feedbackCountString = (explanations.size() < 10) ? String.valueOf(explanations.size()) : "10 or more";
        ppFeedBackCounter.increment(feedbackCountString);
      }

      var result = new Result(message.from, pair.getLatitude(), pair.getLongitude(), feedback, feedbackCountString,
          parseRecord, message);

      callResultsMap.put(result.message.from, result);

      var feedbackRequest = "\n\n=======================================================================\n\n"
          + "ETO would love to hear from you! Would you please take a few minutes to answer the following questions:\n\n" //
          + "1. Were the exercise instructions clear? If not, where did they need improvement?\n" //
          + "2. Did you find the exercise useful?\n" //
          + "3. Did you find the above feedback useful?\n" //
          + "4. What did you dislike about the exercise?\n" //
          + "5. Any additional comments?\n" //
          + "\nPlease reply to this Winlink message or to ETO.Technical.Team@EmComm-Training.groups.io. Thank you!";

      var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
          outboundMessageSubject + " " + m.messageId, feedback + feedbackRequest, null);
      outboundMessageList.add(outboundMessage);
    } // end loop over for

    var sb = new StringBuilder();
    var N = ppCount;
    sb.append("\n\nETO 2024-01-18 aggregate results:\n");
    sb.append("ICS-213 participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));

    var it = fts.iterator();
    while (it.hasNext()) {
      var key = it.next();
      if (fts.hasContent(key)) {
        sb.append(fts.format(key));
      }
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));
    sb.append(formatCounter("Will Participate", ppWillParticipateCounter));
    sb.append(formatCounter("Exchange Class", ppClassCounter));
    sb.append(formatCounter("Exchange Category", ppCategoryCounter));
    sb.append(formatCounter("Exchange Location", ppLocationCounter));

    logger.info(sb.toString());

    if (badLocationCalls.size() > 0) {
      logger
          .info(
              "adjusting lat/long for " + badLocationCalls.size() + " messages: " + String.join(",", badLocationCalls));
      var newLocations = LocationUtils.jitter(badLocationCalls.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationCalls.size(); ++i) {
        var call = badLocationCalls.get(i);
        var result = (Result) callResultsMap.get(call);
        var newLocation = newLocations.get(i);
        result.message().setMapLocation(newLocation);
      }
    }

    var results = new ArrayList<>(callResultsMap.values());

    WriteProcessor.writeTable(results, Path.of(outputPathName, "ics_213_rr-with-feedback.csv"));

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }
  }

  private ParseRecord parse(String formMessage) {
    var willParticipate = "OTHER";
    var exchange = "";
    var opCallSign = "";
    var className = "";
    var category = "";
    var locationId = "";

    if (formMessage == null || formMessage.length() == 0) {
      return new ParseRecord(willParticipate, exchange, opCallSign, className, category, locationId, null, null);
    }

    String[] line2Words = null;
    var lines = formMessage.split("\n");

    if (lines == null) {
      return new ParseRecord(willParticipate, exchange, opCallSign, className, category, locationId, null, null);
    }

    if (lines.length >= 1) {
      var line = lines[0];
      var words = line.split(" ");
      if (words != null && words.length == 1) {
        var word = words[0];
        if (word != null) {
          if (word.equalsIgnoreCase("yes")) {
            willParticipate = "YES";
          } else if (word.equalsIgnoreCase("no")) {
            willParticipate = "NO";
          }
        }
      } else {
        ; // already set to OTHER
      }
    }

    if (lines.length >= 2) {
      // "OpCall", "Class", "Category", "LocationId"
      exchange = lines[1];
      line2Words = exchange.split(" ");
      if (line2Words != null) {
        if (line2Words.length >= 1) {
          opCallSign = line2Words[0];
        }
        if (line2Words.length >= 2) {
          className = line2Words[1];
        }
        if (line2Words.length >= 3) {
          category = line2Words[2];
        }
        if (line2Words.length >= 4) {
          locationId = line2Words[3];
        }
      }
    }

    var parseRecord = new ParseRecord(willParticipate, exchange, opCallSign, className, category, locationId, lines,
        line2Words);
    return parseRecord;
  }
}
