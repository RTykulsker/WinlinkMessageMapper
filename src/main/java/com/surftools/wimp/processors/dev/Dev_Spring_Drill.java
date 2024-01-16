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

package com.surftools.wimp.processors.dev;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

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
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.FieldTestService;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.rmsGateway.RmsGatewayService;

/**
 * Processor for ETO 2024 Spring Drill: multiple FSR plus an ICS-309 that should reference the FSRs
 *
 * FSRs can be sent in 3 stages:
 *
 * Stage1: a single message via Telnet to ETO-DRILL
 *
 * Stage2: multiple messages, each to a different RMS gateway to ETO-DRILL
 *
 * Stage3: multiple messages, but only one each to a "pseudo" NERC (North American Electric Reliability Corporation).
 * The suggested limit of one message per grid may be relaxed if the RMS is a certain distance away from sender
 *
 * ICS-309: send via any session type to normal clearing house
 *
 * @author bobt
 *
 */
public class Dev_Spring_Drill extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(Dev_Spring_Drill.class);
  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  final LocalDateTime windowOpenDT = LocalDateTime.of(2024, 5, 9, 0, 0, 0);
  final LocalDateTime windowCloseDT = LocalDateTime.of(2024, 5, 16, 23, 59, 0);

  private FieldTestService fts;
  private RmsGatewayService rmsGatewayService;

  static record FsrEntry(String messageId, String call, String band, String distanceMiles) {
    @Override
    public String toString() {
      return call + ":" + messageId + ":" + band + ":" + distanceMiles;
    };
  }

  static record Result(String call, String latitude, String longitude, String feedback, String feedbackCountString, //
      int fsrMessageCount, int icsMessageCount, //
      String fsrMessages, String icsMessageId) implements IWritableTable {

    public Result(Result result, LatLongPair newLocation) {
      this(result.call, newLocation.getLatitude(), newLocation.getLongitude(), result.feedback,
          result.feedbackCountString, result.fsrMessageCount, result.icsMessageCount, result.fsrMessages,
          result.icsMessageId);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", "Feedback Count", "Feedback", //
          "Fsr #Messages", "Ics #Messages", //
          "Fsr Messages", "IcsMessageId" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, latitude, longitude, feedbackCountString, feedback, //
          String.valueOf(fsrMessageCount), String.valueOf(icsMessageCount), //
          fsrMessages, icsMessageId };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      return this.call.compareTo(o.call);
    }

  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    if (cm.getAsString(Key.CMS_AUTHORIZATION_KEY) != null) {
      rmsGatewayService = new RmsGatewayService(cm);
    }

    fts = new FieldTestService();

    fts.add("msgLocation", "LAT/LON missing or invalid");
    fts.add("addresses-bk", "To and/or CC addresses should contain ETO-BK");
    fts.add("addresses-drill", "To and/or CC addresses should contain ETO-DRILL");
    fts.add("addresses-clearinghouse", "To and/or CC addresses should contain a regular clearinghouse address");
    fts.add("windowOpen", "Message should be sent on or after " + DT_FORMATTER.format(windowOpenDT), windowOpenDT);
    fts.add("windowClose", "Message should be sent on or before " + DT_FORMATTER.format(windowCloseDT), windowCloseDT);
    fts.add("noMessages", "Neither FSR nor ICS-309 messages received");
    fts.add("noFsrMessages", "At Field Situation Report messages received");
    fts.add("noIcsMessages", "No ICS-309 messages received");

    fts.add("org", "Agency/Group should be #EV", "EmComm Training Organization");
    fts.add("incidentName", "Box 1 IncidentName should be #EV", "Exercise - Santa Wish List");
    fts.add("activityDateTime", "Box 2 Date/Time should be supplied, not empty");
    fts.add("requestNumber", "Box 3 Resource Request Number should be #EV", "Santa001");

  }

  @Override
  public void process() {

    var ppSenderCount = 0;
    var ppFsrMessageCount = 0;
    var ppIcsMessageCount = 0;
    var ppSenderPerfectCount = 0;

    var ppFeedBackCounter = new Counter();

    var callResultsMap = new HashMap<String, IWritableTable>();
    var badLocationCalls = new ArrayList<String>();
    Ics309Message icsMessage = null;

    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) {
      var sender = senderIterator.next();
      ++ppSenderCount;

      if (dumpIds.contains(sender)) {
        logger.info("dump: " + sender);
      }

      // will be used in our Result record
      var location = LatLongPair.ZERO_ZERO;

      fts.reset();
      var messageMap = mm.getMessagesForSender(sender);
      var fsrMessages = messageMap.get(MessageType.FIELD_SITUATION);
      var fsrEntries = new ArrayList<FsrEntry>();
      if (fsrMessages != null) {
        Collections.sort(fsrMessages, (o1, o2) -> o1.sortDateTime.compareTo(o2.sortDateTime));
        for (var fsrMessage : fsrMessages) {
          ++ppFsrMessageCount;

          // last valid location is used
          var pair = fsrMessage.mapLocation;
          if (pair != null && pair.isValid()) {
            location = pair;
          }

          var serviceResult = rmsGatewayService.getLocationOfRmsGateway(sender, fsrMessage.messageId);
          if (serviceResult.isFound()) {
            // TODO
          } else {

          }
          // TODO
        }
      } // end fsrMessages != null

      var icsMessages = messageMap.get(MessageType.ICS_309);
      if (icsMessages != null) {
        Collections.sort(icsMessages, (o2, o1) -> o1.sortDateTime.compareTo(o2.sortDateTime));
        icsMessage = (Ics309Message) icsMessages.get(0);
        ++ppIcsMessageCount;

        if (!location.isValid()) {
          location = icsMessage.mapLocation;
        }
      } // end if icsMessages != null

      // var pair = message.msgLocation;
      // if (pair == null) {
      // pair = LatLongPair.ZERO_ZERO;
      // badLocationCalls.add(sender);
      // fts.fail("msgLocation", "Missing LAT/LON");
      // } else if (!pair.isValid() || pair.equals(LatLongPair.ZERO_ZERO)) {
      // badLocationCalls.add(sender);
      // fts.fail("msgLocation", "Invalid LAT/LON");
      // } else {
      // fts.pass("msgLocation");
      // }
      //
      // var addressList = m.toList + "," + m.ccList;
      // if (!addressList.contains("ETO-BK")) {
      // fts.fail("addresses-bk");
      // }
      //
      // fts.testOnOrAfter("windowOpen", m.msgDateTime, DT_FORMATTER);
      // fts.testOnOrBefore("windowClose", m.msgDateTime, DT_FORMATTER);
      // fts.test("org", message.organization);
      // fts.test("incidentName", message.incidentName);
      // fts.testIfPresent("activityDateTime", message.activityDateTime);
      // fts.test("requestNumber", message.requestNumber);
      //
      // for (var line : new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }) {
      // var lineItem = message.lineItems.get(line - 1);
      //
      // if (line < 8) {
      // fts.test("quantity" + line, lineItem.quantity());
      // fts.testIfEmpty("kind" + line, lineItem.kind());
      // fts.testIfEmpty("type" + line, lineItem.type());
      // fts.test("item" + line, lineItem.item());
      // fts.test("requestedDateTime" + line, lineItem.requestedDateTime());
      // fts.testIfEmpty("estimatedDateTime" + line, lineItem.estimatedDateTime());
      // fts.testIfEmpty("cost" + line, lineItem.cost());
      // } else {
      // fts.testIfEmpty("quantity" + line, lineItem.quantity());
      // fts.testIfEmpty("kind" + line, lineItem.kind());
      // fts.testIfEmpty("type" + line, lineItem.type());
      // fts.testIfEmpty("item" + line, lineItem.item());
      // fts.testIfEmpty("requestedDateTime" + line, lineItem.requestedDateTime());
      // fts.testIfEmpty("estimatedDateTime" + line, lineItem.estimatedDateTime());
      // fts.testIfEmpty("cost" + line, lineItem.cost());
      // }
      // }
      //
      // fts.test("delivery", message.delivery);
      // fts.test("substitutes", message.substitutes);
      // fts.test("requestedBy", message.requestedBy);
      // fts.test("priority", message.priority);
      // fts.test("approvedBy", message.approvedBy);
      //
      // // logistics
      // fts.testIfEmpty("logisticsOrderNumber", message.logisticsOrderNumber);
      // fts.testIfEmpty("supplierInfo", message.supplierInfo);
      // fts.testIfEmpty("supplierName", message.supplierName);
      // fts.testIfEmpty("supplierPointOfContact", message.supplierPointOfContact);
      // fts.testIfEmpty("supplyNotes", message.supplyNotes);
      // fts.testIfEmpty("logisticsAuthorizer", message.logisticsAuthorizer);
      // fts.testIfEmpty("logisticsDateTime", message.logisticsDateTime);
      // fts.testIfEmpty("orderedBy", message.orderedBy);
      //
      // // finance
      // fts.testIfEmpty("financeComments", message.financeComments);
      // fts.testIfEmpty("financeName", message.financeName);
      // fts.testIfEmpty("financeDateTime", message.financeDateTime);
      //

      if (fsrMessages == null && icsMessage == null) {
        fts.fail("noMessages");
      } else if (fsrMessages == null) {
        fts.fail("noFsrMessages");
      } else if (icsMessage == null) {
        fts.fail("noIcsMessages");
      }

      if (!location.isValid() || location.equals(LatLongPair.ZERO_ZERO)) {
        fts.fail("msgLocation");
        badLocationCalls.add(sender);
      }

      var feedback = "Perfect Drill!";
      var feedbackCountString = "0";
      var explanations = fts.getExplanations();
      if (explanations.size() == 0) {
        ++ppSenderPerfectCount;
        ppFeedBackCounter.increment("0");
      } else {
        feedback = String.join("\n", explanations);
        feedbackCountString = (explanations.size() < 10) ? String.valueOf(explanations.size()) : "10 or more";
        ppFeedBackCounter.increment(feedbackCountString);
      }

      var fsrMessageIds = fsrEntries.stream().map((obj) -> Objects.toString(obj, null)).toList();
      var icsMessageId = icsMessage == null ? "" : icsMessage.messageId;
      var result = new Result(sender, location.getLatitude(), location.getLongitude(), feedback, feedbackCountString,
          fsrMessages == null ? 0 : fsrMessages.size(), icsMessages == null ? 0 : icsMessages.size(),
          String.join("\n", fsrMessageIds), icsMessageId);

      callResultsMap.put(sender, result);

      var feedbackRequest = "\n\n=======================================================================\n\n"
          + "ETO would love to hear from you! Would you please take a few minutes to answer the following questions:\n\n" //
          + "1. Were the exercise instructions clear? If not, where did they need improvement?\n" //
          + "2. Did you find the exercise useful?\n" //
          + "3. Did you find the above feedback useful?\n" //
          + "4. What did you dislike about the exercise?\n" //
          + "5. Any additional comments?\n" //
          + "\nPlease reply to this Winlink message or to ETO.Technical.Team@EmComm-Training.groups.io. Thank you!";

      var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
          outboundMessageSubject + " ETO 2024 Spring Drill Feedback", feedback + feedbackRequest, null);
      outboundMessageList.add(outboundMessage);
    } // end loop over sender

    var sb = new StringBuilder();
    var N = ppSenderCount;
    sb.append("\n\nETO 2024 Spring Drill aggregate results:\n");
    sb.append("Participants: " + N + "\n");
    sb.append(formatPP("Perfect Drill", ppSenderPerfectCount, false, N));
    sb.append("Fsr Messages: " + ppFsrMessageCount + "\n");
    sb.append("Ics Messages: " + ppIcsMessageCount + "\n");

    var it = fts.iterator();
    while (it.hasNext()) {
      var key = it.next();
      if (fts.hasContent(key)) {
        sb.append(fts.format(key));
      }
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));

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
        result = new Result(result, newLocation);
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
}
