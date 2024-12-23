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

package com.surftools.wimp.processors.exercise.eto_2024;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-01-18 Exercise: an ICS-213, Winter Field Day questions
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_01_18 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_01_18.class);
  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  final LocalDateTime windowOpenDT = LocalDateTime.of(2024, 01, 16, 0, 0, 0);
  final LocalDateTime windowCloseDT = LocalDateTime.of(2024, 01, 19, 07, 59, 0);

  private SimpleTestService sts = new SimpleTestService();

  static record Result(String call, String latitude, String longitude, String feedback, String feedbackCountString,
      String willParticipate, String exchange, //
      Ics213Message message) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { "Call", "Map Latitude", "Map Longitude", "Feedback Count", "Feedback",
              "WillPlay", "Exchange" });
      Collections.addAll(resultList, Ics213Message.getStaticHeaders());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(Ics213Message.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { call, latitude, longitude, feedbackCountString, feedback, //
              willParticipate, exchange });
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
    var ppVersionCounter = new Counter();

    var messageIdResultMap = new HashMap<String, IWritableTable>();
    var badLocationMessageIds = new ArrayList<String>();

    for (var message : mm.getMessagesForType(MessageType.ICS_213)) {
      var m = (Ics213Message) message;
      var sender = message.from;

      sts.reset(sender);

      if (dumpIds.contains(sender)) {
        logger.info("dump: " + sender);
      }

      ++ppCount;
      ppVersionCounter.increment(m.version);

      var addressList = m.toList + "," + m.ccList;
      sts.test("To and/or CC addresses should contain ETO-BK", addressList.contains("ETO-BK"), null);
      sts.testOnOrAfter("Message should be sent on or after #EV", windowOpenDT, m.msgDateTime, DTF);
      sts.testOnOrBefore("Message should be sent on or after #EV", windowCloseDT, m.msgDateTime, DTF);
      sts.test("Agency/Group should be #EV", "EmComm Training Organization", m.organization);
      sts.test("This Is An Exercise should be checked", m.isExercise, null);
      sts.test("Box 1 IncidentName should be #EV", "ETO Winter Field Day 2024 Poll", m.incidentName);

      sts
          .test("Box 2 To address should be an ETO clearinghouse",
              cm.getAsString(Key.EXPECTED_DESTINATIONS).contains(m.formTo.toUpperCase()), m.formTo);

      {
        var expected = sts.stripEmbeddedSpaces(sender + " / Winlink Thursday Participant").toLowerCase();
        var actual = sts.stripEmbeddedSpaces(m.formFrom).toLowerCase();
        sts
            .test("Box 3 From address should be <YOURCALL> / Winlink Thursday Participant", actual.startsWith(expected),
                m.formFrom);
      }

      sts.test("Box 4 Subject should be #EV", "Winter Field Day 2024 Participation", m.formSubject);
      sts.test("Box 5 Date should be supplied", m.formDate != null, null);
      sts.test("Box 6 Time should be supplied", m.formTime != null, null);

      var messageLines = m.formMessage == null ? new String[] {} : m.formMessage.split("\n");
      messageLines = dropEmptyLines(messageLines);
      var mLL = messageLines.length;
      var willParticipate = "UNKNOWN";
      var exchange = "(null)";
      sts.test("Box 7 message should have 1 or 2 lines", mLL == 1 || mLL == 2, String.valueOf(mLL));
      if (mLL == 1 || mLL == 2) {
        var line1 = messageLines[0].trim().toUpperCase();
        if (line1.equals("YES")) {
          willParticipate = "YES";
        } else if (line1.equals("NO")) {
          willParticipate = "NO";
        } else {
          willParticipate = "OTHER";
        }
        ppWillParticipateCounter.increment(willParticipate);
        sts.test("Box 7 line 1 should be either YES or NO", line1.equals("YES") || line1.equals("NO"), line1);

        if (mLL == 1) {
          if (willParticipate.equals("NO")) {
            sts.test("Box 7 should only have 1 line if line 1 is NO", true, willParticipate);
          } else {
            sts.test("Box 7 should only have 1 line if line 1 is NO", false, willParticipate);
          }
        } else { // exactly 2 lines
          if (willParticipate.equals("YES")) {
            sts.test("Box 7 line 2 should only be present if line 1 is YES", true, willParticipate);
            exchange = messageLines[1];
            var fields = exchange.split(" ");
            if (fields.length == 4) {
              ppClassCounter.increment(fields[1]);
              ppCategoryCounter.increment(fields[2]);
              ppLocationCounter.increment(fields[3]);
              sts.test("Box 7 line 2 should have 4 fields in exchange", true, String.valueOf(fields.length));
            } else {
              sts.test("Box 7 line 2 should have 4 fields in exchange", false, String.valueOf(fields.length));
            }
          } else {
            sts.test("Box 7 line 2 should only be present if line 1 is YES", false, line1);
          }
        } // end if 2 lines
      } // end if 1 or 2 lines

      var approvedByPredicate = m.approvedBy != null
          || !m.approvedBy.toLowerCase().endsWith(", " + sender.toLowerCase())
              && m.approvedBy.length() >= (", " + sender).length();
      sts.test("Box 8 Approved should be firstName, callsign", approvedByPredicate, m.approvedBy);

      sts.test("Box 8b Postion/Title should be #EV", "Winlink Thursday Participant", m.position);

      var pair = m.formLocation == null ? message.msgLocation : m.formLocation;
      if (pair == null) {
        pair = LatLongPair.ZERO_ZERO;
        badLocationMessageIds.add(m.messageId);
        sts.test("LAT/LON should be provided", false, "missing");
      } else if (!pair.isValid() || pair.equals(LatLongPair.ZERO_ZERO)) {
        badLocationMessageIds.add(m.messageId);
        sts.test("LAT/LON should be provided", false, "invalid");
      } else {
        sts.test("LAT/LON should be provided", true, null);
      }

      var feedback = "Perfect Message";
      var feedbackCountString = "0";
      var explanations = sts.getExplanations();
      if (explanations.size() == 0) {
        ++ppMessageCorrectCount;
        ppFeedBackCounter.increment("0");
      } else {
        feedback = String.join("\n", explanations);
        feedbackCountString = (explanations.size() < 10) ? String.valueOf(explanations.size()) : "10 or more";
        ppFeedBackCounter.increment(feedbackCountString);
      }

      var result = new Result(m.from, pair.getLatitude(), pair.getLongitude(), feedback, feedbackCountString,
          willParticipate, exchange, m);

      messageIdResultMap.put(m.messageId, result);

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
    logger.info("field validation:\n" + sts.validate());

    var sb = new StringBuilder();
    var N = ppCount;
    sb.append("\n\nETO 2024-01-18 aggregate results:\n");
    sb.append("ICS-213 participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));

    var it = sts.iterator();
    while (it.hasNext()) {
      var key = it.next();
      if (sts.hasContent(key)) {
        sb.append(sts.format(key));
      }
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));
    sb.append(formatCounter("Version", ppVersionCounter));
    sb.append(formatCounter("Will Participate", ppWillParticipateCounter));
    sb.append(formatCounter("Exchange Class", ppClassCounter));
    sb.append(formatCounter("Exchange Category", ppCategoryCounter));
    sb.append(formatCounter("Exchange Location", ppLocationCounter));

    logger.info(sb.toString());

    if (badLocationMessageIds.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationMessageIds.size() + " messages: "
              + String.join(",", badLocationMessageIds));
      var newLocations = LocationUtils.jitter(badLocationMessageIds.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationMessageIds.size(); ++i) {
        var messageId = badLocationMessageIds.get(i);
        var result = (Result) messageIdResultMap.get(messageId);
        var newLocation = newLocations.get(i);
        result.message().setMapLocation(newLocation);
      }
    }

    var results = new ArrayList<>(messageIdResultMap.values());
    WriteProcessor.writeTable(results, Path.of(outputPathName, "ics_213_rr-with-feedback.csv"));

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }
  }

  private String[] dropEmptyLines(String[] messageLines) {
    var list = new ArrayList<String>();
    for (var line : messageLines) {
      if (line != null && line.length() != 0) {
        list.add(line);
      }
    }
    var array = list.toArray(new String[0]);
    return array;
  }

}
