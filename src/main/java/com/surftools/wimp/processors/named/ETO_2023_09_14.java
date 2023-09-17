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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;

/**
 * Processor for 2023-09_14 Exercise: an ICS-213-RR with feedback, but no grade
 *
 * Exercise Message Submission Window 2023-09-14 00:00 - 2023-09-15 15:00 UTC
 *
 * @author bobt
 *
 */
public class ETO_2023_09_14 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2023_09_14.class);
  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private static Set<String> explicitBadLocationSet = Set.of("");

  private FormFieldManager ffm;

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

    ffm = new FormFieldManager();

    ffm.add("windowOpen", new FormField(FFType.DATE_TIME_ON_OR_AFTER, "Message sent too early", "2023-09-14 00:00"));
    ffm.add("windowClose", new FormField(FFType.DATE_TIME_ON_OR_BEFORE, "Message sent too late", "2023-09-15 15:00"));

    ffm.add("org", new FormField("Agency/Group", "EMComm Training Organization"));
    ffm.add("incidentName", new FormField("Box 1 IncidentName", "Exercise - Operation Fire Support"));
    ffm.add("activityDateTime", new FormField(FFType.DATE_TIME, "Box 2 Date/Time", DT_FORMAT_STRING));
    ffm.add("requestNumber", new FormField("Resource Request Number", "28B5"));

    // line 1
    ffm.add("quantity1", new FormField(FFType.EQUALS, "Item Quantity#1", "10"));
    ffm.add("type1", new FormField(FFType.EMPTY, "Item Type#1"));
    ffm.add("kind1", new FormField(FFType.EMPTY, "Item Kind#1"));
    ffm.add("item1", new FormField("Item Description#1", "XB-1 Cart with (20) XB-1 Folding Cots"));
    ffm.add("requestedDateTime1", new FormField(FFType.EQUALS, "Requested Date/Time#1", "2023-09-20 14:00"));
    ffm.add("estimatedDateTime1", new FormField(FFType.EMPTY, "Estimated Date/Time#1"));
    ffm.add("cost1", new FormField(FFType.EMPTY, "Item Cost#1"));

    // line 2
    ffm.add("quantity2", new FormField(FFType.EQUALS, "Item Quantity#2", "7"));
    ffm.add("type2", new FormField(FFType.EMPTY, "Item Type#2"));
    ffm.add("kind2", new FormField(FFType.EMPTY, "Item Kind#2"));
    ffm.add("item2", new FormField("Item Description#2", "Aqua Literz Emergency Drinking Water, Pallet of 900"));
    ffm.add("requestedDateTime2", new FormField(FFType.EQUALS, "Requested Date/Time#2", "2023-09-20 14:00"));
    ffm.add("estimatedDateTime2", new FormField(FFType.EMPTY, "Estimated Date/Time#2"));
    ffm.add("cost2", new FormField(FFType.EMPTY, "Item Cost#2"));

    // line 3
    ffm.add("quantity3", new FormField(FFType.EQUALS, "Item Quantity#3", "200"));
    ffm.add("type3", new FormField(FFType.EMPTY, "Item Type#3"));
    ffm.add("kind3", new FormField(FFType.EMPTY, "Item Kind#3"));
    ffm.add("item3", new FormField("Item Description#3", "66 X 90 Poly Blanket"));
    ffm.add("requestedDateTime3", new FormField(FFType.EQUALS, "Requested Date/Time#3", "2023-09-20 14:00"));
    ffm.add("estimatedDateTime3", new FormField(FFType.EMPTY, "Estimated Date/Time#3"));
    ffm.add("cost3", new FormField(FFType.EMPTY, "Item Cost#3"));

    // line 4
    ffm.add("quantity4", new FormField(FFType.EQUALS, "Item Quantity#4", "4"));
    ffm.add("type4", new FormField(FFType.EMPTY, "Item Type#4"));
    ffm.add("kind4", new FormField(FFType.EMPTY, "Item Kind#4"));
    ffm.add("item4", new FormField("Item Description#4", "Bathroom Tissue, 2-ply 48 rolls/carton"));
    ffm.add("requestedDateTime4", new FormField(FFType.EQUALS, "Requested Date/Time#4", "2023-09-20 14:00"));
    ffm.add("estimatedDateTime4", new FormField(FFType.EMPTY, "Estimated Date/Time#4"));
    ffm.add("cost4", new FormField(FFType.EMPTY, "Item Cost#4"));

    // line 5
    ffm.add("quantity5", new FormField(FFType.EQUALS, "Item Quantity#5", "9"));
    ffm.add("type5", new FormField(FFType.EMPTY, "Item Type#5"));
    ffm.add("kind5", new FormField(FFType.EMPTY, "Item Kind#5"));
    ffm.add("item5", new FormField("Item Description#5", "Paper Plates 9\" 500 count box"));
    ffm.add("requestedDateTime5", new FormField(FFType.EQUALS, "Requested Date/Time#5", "2023-09-20 14:00"));
    ffm.add("estimatedDateTime5", new FormField(FFType.EMPTY, "Estimated Date/Time#5"));
    ffm.add("cost5", new FormField(FFType.EMPTY, "Item Cost#5"));

    // line 6
    ffm.add("quantity6", new FormField(FFType.EQUALS, "Item Quantity#6", "5"));
    ffm.add("type6", new FormField(FFType.EMPTY, "Item Type#6"));
    ffm.add("kind6", new FormField(FFType.EMPTY, "Item Kind#6"));
    ffm
        .add("item6",
            new FormField("Item Description#6", "Dynarex Bulk BZK Antiseptic Towelettes 5″x7″ – 1000 Packets"));
    ffm.add("requestedDateTime6", new FormField(FFType.EQUALS, "Requested Date/Time#6", "2023-09-20 14:00"));
    ffm.add("estimatedDateTime6", new FormField(FFType.EMPTY, "Estimated Date/Time#6"));
    ffm.add("cost6", new FormField(FFType.EMPTY, "Item Cost#6"));

    // line 7
    ffm.add("quantity7", new FormField(FFType.EQUALS, "Item Quantity#7", "4"));
    ffm.add("type7", new FormField(FFType.EMPTY, "Item Type#7"));
    ffm.add("kind7", new FormField(FFType.EMPTY, "Item Kind#7"));
    ffm.add("item7", new FormField(FFType.ALPHANUMERIC, "Item Description#7", "12 oz paper hot cups 1000/case"));
    ffm.add("requestedDateTime7", new FormField(FFType.EQUALS, "Requested Date/Time#7", "2023-09-20 14:00"));
    ffm.add("estimatedDateTime7", new FormField(FFType.EMPTY, "Estimated Date/Time#7"));
    ffm.add("cost7", new FormField(FFType.EMPTY, "Item Cost#7"));

    // line 8
    ffm.add("quantity8", new FormField(FFType.EMPTY, "Item Quantity#8"));
    ffm.add("type8", new FormField(FFType.EMPTY, "Item Type#8"));
    ffm.add("kind8", new FormField(FFType.EMPTY, "Item Kind#8"));
    ffm.add("item8", new FormField(FFType.EMPTY, "Item Description#8"));
    ffm.add("requestedDateTime8", new FormField(FFType.EMPTY, "Requested Date/Time#8"));
    ffm.add("estimatedDateTime8", new FormField(FFType.EMPTY, "Estimated Date/Time#8"));
    ffm.add("cost8", new FormField(FFType.EMPTY, "Item Cost#8"));

    // rest of request
    ffm.add("delivery", new FormField("Delivery/Reporting Location", "Middle School Emergency Shelter"));
    ffm.add("substitutes", new FormField(FFType.EQUALS_IGNORE_CASE, "Substitutes", "All Safe Industries"));
    ffm.add("requestedBy", new FormField("Requested By", "Claudia Richards, Shelter Supervisor"));
    ffm.add("priority", new FormField("Priority", "Low"));
    ffm.add("approvedBy", new FormField(FFType.EQUALS_IGNORE_CASE, "Section Chief", "Kathy Redgrave"));

    // logistics
    ffm.add("logisticsOrderNumber", new FormField(FFType.EMPTY, "Log Order Number"));
    ffm.add("supplierInfo", new FormField(FFType.EMPTY, "Supplier Info"));
    ffm.add("supplierName", new FormField(FFType.EMPTY, "Supplier Name"));
    ffm.add("supplierPointOfContact", new FormField(FFType.EMPTY, "Point of Contact"));
    ffm.add("supplyNotes", new FormField(FFType.EMPTY, "Notes"));
    ffm.add("logisticsAuthorizer", new FormField(FFType.EMPTY, "Logistics Rep"));
    ffm.add("logisticsDateTime", new FormField(FFType.EMPTY, "Logistics Date/Time"));
    ffm.add("orderedBy", new FormField(FFType.EMPTY, "Ordered By"));

    // finance
    ffm.add("financeComments", new FormField(FFType.EMPTY, "Finance Comments"));
    ffm.add("financeName", new FormField(FFType.EMPTY, "Finance Chief"));
    ffm.add("financeDateTime", new FormField(FFType.EMPTY, "Finance Date/Time By"));
  }

  @Override
  public void process() {

    var ppCount = 0;
    var ppMessageCorrectCount = 0;
    var ppExplicitBadLocationCounter = 0;
    var ppMissingLocationCounter = 0;

    var ppFeedBackCounter = new Counter();

    var callResultsMap = new HashMap<String, IWritableTable>();
    var zeroZeroLocationList = new ArrayList<String>();

    for (var m : mm.getMessagesForType(MessageType.ICS_213_RR)) {
      var message = (Ics213RRMessage) m;
      var sender = message.from;

      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      if (dumpIds.contains(sender)) {
        logger.info("dump: " + sender);
      }

      ++ppCount;

      var pair = message.msgLocation;
      if (pair == null) {
        zeroZeroLocationList.add(sender);
        ++ppMissingLocationCounter;
        explanations.add("!!! missing location ");
        pair = new LatLongPair("", "");
      } else if (!pair.isValid() || pair.equals(LatLongPair.ZERO_ZERO)) {
        zeroZeroLocationList.add(sender);
        ++ppMissingLocationCounter;
        explanations.add("!!!invalid location: " + pair.toString());
        pair = new LatLongPair("", "");
      }

      if (explicitBadLocationSet.contains(sender)) {
        ++ppExplicitBadLocationCounter;
        explanations.add("explicit bad location: " + pair.toString());
      }

      ffm.test("windowOpen", FormFieldManager.FORMATTER.format(message.msgDateTime));
      ffm.test("windowClose", FormFieldManager.FORMATTER.format(message.msgDateTime));
      ffm.test("org", message.organization);
      ffm.test("incidentName", message.incidentName);
      ffm.test("activityDateTime", message.activityDateTime);
      ffm.test("requestNumber", message.requestNumber);

      for (var line : new int[] { 1, 2, 3, 4, 5, 6, 7, 8 }) {
        var lineItem = message.lineItems.get(line - 1);
        ffm.test("quantity" + line, lineItem.quantity());
        ffm.test("kind" + line, lineItem.kind());
        ffm.test("type" + line, lineItem.type());
        ffm.test("item" + line, lineItem.item());
        ffm.test("requestedDateTime" + line, lineItem.requestedDateTime());
        ffm.test("estimatedDateTime" + line, lineItem.estimatedDateTime());
        ffm.test("cost" + line, lineItem.cost());
      }

      ffm.test("delivery", message.delivery);
      ffm.test("substitutes", message.substitutes);
      ffm.test("requestedBy", message.requestedBy);
      ffm.test("priority", message.priority);
      ffm.test("approvedBy", message.approvedBy);

      // logistics
      ffm.test("logisticsOrderNumber", message.logisticsOrderNumber);
      ffm.test("supplierInfo", message.supplierInfo);
      ffm.test("supplierName", message.supplierName);
      ffm.test("supplierPointOfContact", message.supplierPointOfContact);
      ffm.test("supplyNotes", message.supplyNotes);
      ffm.test("logisticsAuthorizer", message.logisticsAuthorizer);
      ffm.test("logisticsDateTime", message.logisticsDateTime);
      ffm.test("orderedBy", message.orderedBy);

      // finance
      ffm.test("financeComments", message.financeComments);
      ffm.test("financeName", message.financeName);
      ffm.test("financeDateTime", message.financeDateTime);

      var feedback = "Perfect Message";
      var feedbackCountString = "0";
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
    } // end loop over for

    var sb = new StringBuilder();
    var N = ppCount;
    sb.append("\n\nETO 2023-09-14 aggregate results:\n");
    sb.append("ICS-213-RR participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));
    sb.append(formatPP("NO Explicit Bad Locations", ppExplicitBadLocationCounter, true, N));
    sb.append(formatPP("NO Missing or Invalid Locations", ppMissingLocationCounter, true, N));

    for (var key : ffm.keySet()) {
      sb.append(formatField(ffm, key, false, N));
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));

    logger.info(sb.toString());

    if (zeroZeroLocationList.size() > 0) {
      logger
          .info("adjusting lat/long for " + zeroZeroLocationList.size() + " messages: "
              + String.join(",", zeroZeroLocationList));
      var newLocations = LocationUtils.jitter(zeroZeroLocationList.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < zeroZeroLocationList.size(); ++i) {
        var call = zeroZeroLocationList.get(i);
        var result = (Result) callResultsMap.get(call);
        var newLocation = newLocations.get(i);
        result.message().setMapLocation(newLocation);
      }
    }

    var results = new ArrayList<>(callResultsMap.values());

    WriteProcessor.writeTable(results, Path.of(outputPathName, "ics_213_rr-with-feedback.csv"));
  }
}
