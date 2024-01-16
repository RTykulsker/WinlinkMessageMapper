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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;

/**
 * Processor for 2023-12_14 Exercise: an ICS-213-RR with feedback, but no grade, using SimpleTestService
 *
 * @author bobt
 *
 */
public class Dev_2023_12_14 extends AbstractBaseProcessor {
  final Logger logger = LoggerFactory.getLogger(Dev_2023_12_14.class);
  final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  final LocalDateTime windowOpenDT = LocalDateTime.of(2023, 12, 12, 0, 0, 0);
  final LocalDateTime windowCloseDT = LocalDateTime.of(2023, 12, 15, 15, 00, 0);
  final String requestedDate = "2023-12-25 02:00";
  final SimpleTestService sts = new SimpleTestService();

  static record Result(String call, String latitude, String longitude, String feedback, String feedbackCountString,
      Ics213RRMessage message) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { "Call", "Map Latitude", "Map Longitude", "Feedback Count", "Feedback", });
      Collections.addAll(resultList, Ics213RRMessage.getStaticHeaders());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 5);
      Collections.addAll(resultList, new String[] { call, latitude, longitude, feedbackCountString, feedback, });
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
    var callResultsMap = new HashMap<String, IWritableTable>();
    var badLocationCalls = new ArrayList<String>();

    for (var message : mm.getMessagesForType(MessageType.ICS_213_RR)) {
      var m = (Ics213RRMessage) message;
      var sender = m.from;
      sts.reset();
      ++ppCount;

      var addressList = message.toList + "," + message.ccList;
      sts.test("To and/or CC addresses should contain ETO-BK", addressList.contains("ETO-BK"), null);

      sts.testOnOrAfter("Message should be sent on or after #EV", windowOpenDT, message.msgDateTime, DTF);
      sts.testOnOrBefore("Message should be sent on or after #EV", windowCloseDT, message.msgDateTime, DTF);

      sts.test("Agency/Group should be #EV", "EmComm Training Organization", m.organization);
      sts.test("Box 1 IncidentName should be #EV", "Exercise - Santa Wish List", m.incidentName);
      sts.testIfPresent("Box 2 Date/Time should be supplied, not empty", m.activityDateTime);
      sts.test("Box 3 Resource Request Number should be #EV", "Santa001", m.requestNumber);

      var pair = m.msgLocation;
      if (pair == null) {
        pair = LatLongPair.ZERO_ZERO;
        badLocationCalls.add(sender);
        sts.test("LAT/LON should be provided", false, "missing");
      } else if (!pair.isValid() || pair.equals(LatLongPair.ZERO_ZERO)) {
        badLocationCalls.add(sender);
        sts.test("LAT/LON should be provided", false, "invalid");
      } else {
        sts.test("LAT/LON should be provided", true, null);
      }

      for (var line : new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }) {
        var lineItem = m.lineItems.get(line - 1);
        switch (line) {
        case 1:
          sts.test("Line 1: Item Quantity should be #EV", "1", lineItem.quantity());
          sts.testIfEmpty("Line 1: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 1: Type should be empty", lineItem.type());
          sts.test("Line 1: Description should be #EV", "Chameleon CHA CRL Pro loop antenna", lineItem.item());
          sts.test("Line 1: Requested Date/Time should be #EV", requestedDate, lineItem.requestedDateTime());
          sts.testIfEmpty("Line 1: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 1: Item Cost should be empty", lineItem.cost());
          break;

        case 2:
          sts.test("Line 2: Item Quantity should be #EV", "1", lineItem.quantity());
          sts.testIfEmpty("Line 2: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 2: Type should be empty", lineItem.type());
          sts.test("Line 2: Description should be #EV", "Xiegu X6100 HF Transceiver", lineItem.item());
          sts.test("Line 2: Requested Date/Time should be #EV", requestedDate, lineItem.requestedDateTime());
          sts.testIfEmpty("Line 2: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 2: Item Cost should be empty", lineItem.cost());
          break;

        case 3:
          sts.test("Line 3: Item Quantity should be #EV", "1", lineItem.quantity());
          sts.testIfEmpty("Line 3: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 3: Type should be empty", lineItem.type());
          sts.test("Line 3: Description should be #EV", "Radioddity PB3 Protective Carry Case", lineItem.item());
          sts.test("Line 3: Requested Date/Time should be #EV", requestedDate, lineItem.requestedDateTime());
          sts.testIfEmpty("Line 3: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 3: Item Cost should be empty", lineItem.cost());
          break;

        case 4:
          sts.test("Line 4: Item Quantity should be #EV", "1", lineItem.quantity());
          sts.testIfEmpty("Line 4: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 4: Type should be empty", lineItem.type());
          sts.test("Line 4: Description should be #EV", "MFJ 259D Antenna SWR Analyzer", lineItem.item());
          sts.test("Line 4: Requested Date/Time should be #EV", requestedDate, lineItem.requestedDateTime());
          sts.testIfEmpty("Line 4: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 4: Item Cost should be empty", lineItem.cost());
          break;

        case 5:
          sts.test("Line 5: Item Quantity should be #EV", "1", lineItem.quantity());
          sts.testIfEmpty("Line 5: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 5: Type should be empty", lineItem.type());
          sts.test("Line 5: Description should be #EV", "Powerwerx Crimp Bag", lineItem.item());
          sts.test("Line 5: Requested Date/Time should be #EV", requestedDate, lineItem.requestedDateTime());
          sts.testIfEmpty("Line 5: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 5: Item Cost should be empty", lineItem.cost());
          break;

        case 6:
          sts.test("Line 6: Item Quantity should be #EV", "1", lineItem.quantity());
          sts.testIfEmpty("Line 6: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 6: Type should be empty", lineItem.type());
          sts.test("Line 6: Description should be #EV", "CHA EmComm III Portable Antenna", lineItem.item());
          sts.test("Line 6: Requested Date/Time should be #EV", requestedDate, lineItem.requestedDateTime());
          sts.testIfEmpty("Line 6: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 6: Item Cost should be empty", lineItem.cost());
          break;

        case 7:
          sts.test("Line 7: Item Quantity should be #EV", "1", lineItem.quantity());
          sts.testIfEmpty("Line 7: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 7: Type should be empty", lineItem.type());
          sts.test("Line 7: Description should be #EV", "Geochron Digital Atlas 2 4K 400-1000B", lineItem.item());
          sts.test("Line 7: Requested Date/Time should be #EV", requestedDate, lineItem.requestedDateTime());
          sts.testIfEmpty("Line 7: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 7: Item Cost should be empty", lineItem.cost());
          break;

        case 8:
          sts.testIfEmpty("Line 8: Item Quantity should be empty", lineItem.quantity());
          sts.testIfEmpty("Line 8: Kind should be empty", lineItem.kind());
          sts.testIfEmpty("Line 8: Type should be empty", lineItem.type());
          sts.testIfEmpty("Line 8: Description should be empty", lineItem.item());
          sts.testIfEmpty("Line 8: Requested Date/Time should be empty", lineItem.requestedDateTime());
          sts.testIfEmpty("Line 8: Estimated Date/Time should be empty", lineItem.estimatedDateTime());
          sts.testIfEmpty("Line 8: Item Cost should be empty", lineItem.cost());
          break;
        }
      }

      // rest of request
      sts.test("Box 5: Delivery/Reporting Location should be #EV", "Child’s address", m.delivery);
      sts.test("Box 6: Substitutes should be #EV", "DX Engineering", m.substitutes);
      sts.test("Box 7: Requested By should be #EV", "Child/Nice list", m.requestedBy);
      sts.test("Box 8: Priority should be #EV", "Routine", m.priority);
      sts.test("Box 9: Section Chief should be #EV", "Gertrude Claus", m.approvedBy);

      // logistics
      sts.testIfEmpty("Box 10: Log Order Number should be empty", m.logisticsOrderNumber);
      sts.testIfEmpty("Box 11: Supplier Info should be empty", m.supplierInfo);
      sts.testIfEmpty("Box 12: Supplier Name should be empty", m.supplierName);
      sts.testIfEmpty("Box 12A: Point of Contact should be empty", m.supplierPointOfContact);
      sts.testIfEmpty("Box 13: Notes should be empty", m.supplyNotes);
      sts.testIfEmpty("Box 14: Logistics Rep should be empty", m.logisticsAuthorizer);
      sts.testIfEmpty("Box 15: Logistics Date/Time should be empty", m.logisticsDateTime);
      sts.testIfEmpty("Box 16: Ordered By should be empty", m.orderedBy);

      // finance
      sts.testIfEmpty("Box 17: Finance Comments should be empty", m.financeComments);
      sts.testIfEmpty("Box 18: Finance Chief should be empty", m.financeName);
      sts.testIfEmpty("Box 19: Finance Date/Time By should be empty", m.financeDateTime);

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

      var result = new Result(m.from, pair.getLatitude(), pair.getLongitude(), feedback, feedbackCountString, m);
      callResultsMap.put(result.message.from, result);

      // var feedbackRequest = "\n\n=======================================================================\n\n"
      // + "ETO would love to hear from you! Would you please take a few minutes to answer the following questions:\n\n"
      // //
      // + "1. Were the exercise instructions clear? If not, where did they need improvement?\n" //
      // + "2. Did you find the exercise useful?\n" //
      // + "3. Did you find the above feedback useful?\n" //
      // + "4. What did you dislike about the exercise?\n" //
      // + "5. Any additional comments?\n" //
      // + "\nPlease reply to this Winlink message or to ETO.Technical.Team@EmComm-Training.groups.io. Thank you!";

      var nag = "\n\n=======================================================================\n\n"
          + "ETO needs sponsors to be able to renew our groups.io subscription for 2024.\n"
          + "By sponsoring this group, you are helping pay the Groups.io hosting fees.\n"
          + "Here is the link to sponsor our group:  https://emcomm-training.groups.io/g/main/sponsor\n"
          + "Any amount you sponsor will be held by Groups.io and used to pay hosting fees as needed.\n"
          + "The minimum sponsorship is $5.00.\n" //
          + "Thank you for your support!\n";

      var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
          outboundMessageSubject + " " + message.messageId, feedback + nag, null);
      outboundMessageList.add(outboundMessage);
    } // end loop over for

    var sb = new StringBuilder();
    var N = ppCount;
    sb.append("\n\nETO 2023-12-14 aggregate results:\n");
    sb.append("ICS-213-RR participants: " + N + "\n");
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
}
