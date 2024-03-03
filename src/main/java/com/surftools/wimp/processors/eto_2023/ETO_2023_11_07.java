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

package com.surftools.wimp.processors.eto_2023;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.FieldTestService;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2023-11_11 Exercise: an ICS-213-RR with feedback, but no grade
 *
 * Exercise Message Submission Window 2023-11-07 00:00 - 2023-11-12 23:59 UTC
 *
 * @author bobt
 *
 */
public class ETO_2023_11_07 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2023_11_07.class);
  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  final LocalDateTime windowOpenDT = LocalDateTime.of(2023, 11, 07, 0, 0, 0);
  final LocalDateTime windowCloseDT = LocalDateTime.of(2023, 11, 12, 23, 59, 0);

  private FieldTestService fts;

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

    fts = new FieldTestService();
    final var requestedDate = "2023-11-15 14:00";

    fts.add("msgLocation", "LAT/LON must be provided");
    fts.add("addresses-bk", "To and/or CC addresses must contain ETO-BK");
    fts.add("windowOpen", "Message must be sent on or after " + DT_FORMATTER.format(windowOpenDT), windowOpenDT);
    fts.add("windowClose", "Message must be sent on or before " + DT_FORMATTER.format(windowCloseDT), windowCloseDT);
    fts.add("org", "Agency/Group must be #EV", "EMComm Training Organization");
    fts.add("incidentName", "Box 1 IncidentName must be #EV", "Exercise - Operation Fire Support");
    fts.add("activityDateTime", "Box 2 Date/Time must be supplied, not empty");
    fts.add("requestNumber", "Box 3 Resource Request Number must be 28B5", "28B5");

    // line 1
    fts.add("quantity1", "Line 1: Item Quantity must be #EV", "10");
    fts.add("kind1", "Line 1: Kind must be empty");
    fts.add("type1", "Line 1: Type must be empty");
    fts.add("item1", "Line 1: Description must be #EV", "XB-1 Cart with (20) XB-1 Folding Cots");
    fts.add("requestedDateTime1", "Line 1: Requested Date/Time must be #EV", requestedDate);
    fts.add("estimatedDateTime1", "Line 1: Estimated Date/Time must be empty");
    fts.add("cost1", "Line 1: Item Cost must be empty");

    // line 2
    fts.add("quantity2", "Line 2: Item Quantity must be #EV", "7");
    fts.add("kind2", "Line 2: Kind must be empty");
    fts.add("type2", "Line 2: Type must be empty");
    fts.add("item2", "Line 2: Description must be #EV", "Aqua Literz Emergency Drinking Water, Pallet of 900");
    fts.add("requestedDateTime2", "Line 2: Requested Date/Time must be #EV", requestedDate);
    fts.add("estimatedDateTime2", "Line 2: Estimated Date/Time must be empty");
    fts.add("cost2", "Line 2: Item Cost must be empty");

    // line 3
    fts.add("quantity3", "Line 3: Item Quantity must be #EV", "200");
    fts.add("kind3", "Line 3: Kind must be empty");
    fts.add("type3", "Line 3: Type must be empty");
    fts.add("item3", "Line 3: Description must be #EV", "66 X 90 Poly Blankets");
    fts.add("requestedDateTime3", "Line 3: Requested Date/Time must be #EV", requestedDate);
    fts.add("estimatedDateTime3", "Line 3: Estimated Date/Time must be empty");
    fts.add("cost3", "Line 3: Item Cost must be empty");

    // line 4
    fts.add("quantity4", "Line 4: Item Quantity must be #EV", "4");
    fts.add("kind4", "Line 4: Kind must be empty");
    fts.add("type4", "Line 4: Type must be empty");
    fts.add("item4", "Line 4: Description must be #EV", "Bathroom Tissue, 2-ply 48 rolls/carton");
    fts.add("requestedDateTime4", "Line 4: Requested Date/Time must be #EV", requestedDate);
    fts.add("estimatedDateTime4", "Line 4: Estimated Date/Time must be empty");
    fts.add("cost4", "Line 4: Item Cost must be empty");

    // line 5
    fts.add("quantity5", "Line 5: Item Quantity must be #EV", "9");
    fts.add("kind5", "Line 5: Kind must be empty");
    fts.add("type5", "Line 5: Type must be empty");
    fts.add("item5", "Line 5: Description must be #EV", "Paper Plates 9\" 500 count box");
    fts.add("requestedDateTime5", "Line 5: Requested Date/Time must be #EV", requestedDate);
    fts.add("estimatedDateTime5", "Line 5: Estimated Date/Time must be empty");
    fts.add("cost5", "Line 5: Item Cost must be empty");

    // line 6
    fts.add("quantity6", "Line 6: Item Quantity must be #EV", "5");
    fts.add("kind6", "Line 6: Kind must be empty");
    fts.add("type6", "Line 6: Type must be empty");

    var line6items = Set
        .of("Dynarex Bulk BZK Antiseptic Towelettes 5\"x7\", 1000 packets",
            "Dynarex Bulk BZK Antiseptic Towelettes 5\"x7\", 1000 packet");
    fts.add("item6", "Line 6: Description must be one of: " + String.join(",", line6items), line6items);

    fts.add("requestedDateTime6", "Line 6: Requested Date/Time must be #EV", requestedDate);
    fts.add("estimatedDateTime6", "Line 6: Estimated Date/Time must be empty");
    fts.add("cost6", "Line 6: Item Cost must be empty");

    // line 7
    fts.add("quantity7", "Line 7: Item Quantity must be #EV", "4");
    fts.add("kind7", "Line 7: Kind must be empty");
    fts.add("type7", "Line 7: Type must be empty");
    fts.add("item7", "Line 7: Description must be #EV", "12 oz paper hot cups 1000/case");
    fts.add("requestedDateTime7", "Line 7: Requested Date/Time must be #EV", requestedDate);
    fts.add("estimatedDateTime7", "Line 7: Estimated Date/Time must be empty");
    fts.add("cost7", "Line 7: Item Cost must be empty");

    // line 8
    fts.add("quantity8", "Line 8: Item Quantity must be empty");
    fts.add("kind8", "Line 8: Kind must be empty");
    fts.add("type8", "Line 8: Type must be empty");
    fts.add("item8", "Line 8: Description must be empty");
    fts.add("requestedDateTime8", "Line 8: Requested Date/Time must be empty");
    fts.add("estimatedDateTime8", "Line 8: Estimated Date/Time must be empty");
    fts.add("cost8", "Line 8: Item Cost must be empty");

    // rest of request
    fts.add("delivery", "Box 5: Delivery/Reporting Location must be #EV", "Middle School Emergency Shelter");
    fts.add("substitutes", "Box 6: Substitutes must be #EV", "All Safe Industries");
    fts.add("requestedBy", "Box 7: Requested By must be #EV", "Claudia Richards, Shelter Supervisor");
    fts.add("priority", "Box 8: Priority must be #EV", "Low");
    fts.add("approvedBy", "Box 9: Section Chief must be #EV", "Kathy Redgrave");

    // logistics
    fts.add("logisticsOrderNumber", "Box 10: Log Order Number must be empty");
    fts.add("supplierInfo", "Box 11: Supplier Info must be empty");
    fts.add("supplierName", "Box 12: Supplier Name must be empty");
    fts.add("supplierPointOfContact", "Box 12A: Point of Contact must be empty");
    fts.add("supplyNotes", "Box 13: Notes must be empty");
    fts.add("logisticsAuthorizer", "Box 14: Logistics Rep must be empty");
    fts.add("logisticsDateTime", "Box 15: Logistics Date/Time must be empty");
    fts.add("orderedBy", "Box 16: Ordered By must be empty");

    // finance
    fts.add("financeComments", "Box 17: Finance Comments must be empty");
    fts.add("financeName", "Box 18: Finance Chief must be empty");
    fts.add("financeDateTime", "Box 19: Finance Date/Time By must be empty");

  }

  @Override
  public void process() {

    var ppCount = 0;
    var ppMessageCorrectCount = 0;

    var ppFeedBackCounter = new Counter();

    var callResultsMap = new HashMap<String, IWritableTable>();
    var badLocationCalls = new ArrayList<String>();

    for (var m : mm.getMessagesForType(MessageType.ICS_213_RR)) {
      var message = (Ics213RRMessage) m;
      var sender = message.from;

      fts.reset();

      if (dumpIds.contains(sender)) {
        logger.info("dump: " + sender);
      }

      ++ppCount;

      var pair = message.msgLocation;
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

      var addressList = m.toList + "," + m.ccList;
      if (!addressList.contains("ETO-BK")) {
        fts.fail("addresses-bk");
      }

      fts.testOnOrAfter("windowOpen", m.msgDateTime, DT_FORMATTER);
      fts.testOnOrBefore("windowClose", m.msgDateTime, DT_FORMATTER);
      fts.test("org", message.organization);
      fts.test("incidentName", message.incidentName);
      fts.testIfPresent("activityDateTime", message.activityDateTime);
      fts.test("requestNumber", message.requestNumber);

      for (var line : new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }) {
        var lineItem = message.lineItems.get(line - 1);

        if (line < 8) {
          fts.test("quantity" + line, lineItem.quantity());
          fts.testIfEmpty("kind" + line, lineItem.kind());
          fts.testIfEmpty("type" + line, lineItem.type());

          if (line == 6) {
            fts.testSetOfStrings("item" + line, lineItem.item());
          } else {
            fts.test("item" + line, lineItem.item());
          }

          fts.test("requestedDateTime" + line, lineItem.requestedDateTime());
          fts.testIfEmpty("estimatedDateTime" + line, lineItem.estimatedDateTime());
          fts.testIfEmpty("cost" + line, lineItem.cost());
        } else {
          fts.testIfEmpty("quantity" + line, lineItem.quantity());
          fts.testIfEmpty("kind" + line, lineItem.kind());
          fts.testIfEmpty("type" + line, lineItem.type());
          fts.testIfEmpty("item" + line, lineItem.item());
          fts.testIfEmpty("requestedDateTime" + line, lineItem.requestedDateTime());
          fts.testIfEmpty("estimatedDateTime" + line, lineItem.estimatedDateTime());
          fts.testIfEmpty("cost" + line, lineItem.cost());
        }
      }

      fts.test("delivery", message.delivery);
      fts.test("substitutes", message.substitutes);
      fts.test("requestedBy", message.requestedBy);
      fts.test("priority", message.priority);
      fts.test("approvedBy", message.approvedBy);

      // logistics
      fts.testIfEmpty("logisticsOrderNumber", message.logisticsOrderNumber);
      fts.testIfEmpty("supplierInfo", message.supplierInfo);
      fts.testIfEmpty("supplierName", message.supplierName);
      fts.testIfEmpty("supplierPointOfContact", message.supplierPointOfContact);
      fts.testIfEmpty("supplyNotes", message.supplyNotes);
      fts.testIfEmpty("logisticsAuthorizer", message.logisticsAuthorizer);
      fts.testIfEmpty("logisticsDateTime", message.logisticsDateTime);
      fts.testIfEmpty("orderedBy", message.orderedBy);

      // finance
      fts.testIfEmpty("financeComments", message.financeComments);
      fts.testIfEmpty("financeName", message.financeName);
      fts.testIfEmpty("financeDateTime", message.financeDateTime);

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
          message);

      callResultsMap.put(result.message.from, result);

      var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
          outboundMessageSubject + " " + m.messageId, feedback, null);
      outboundMessageList.add(outboundMessage);
    } // end loop over for

    var sb = new StringBuilder();
    var N = ppCount;
    sb.append("\n\nETO 2023-11-11 aggregate results:\n");
    sb.append("ICS-213-RR participants: " + N + "\n");
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
