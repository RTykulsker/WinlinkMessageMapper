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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.processors.std.baseExercise.FeedbackProcessor;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-12-12: an ICS-213-RR and an ICS-214 that references the RR, themed around Santa's Wish List
 *
 * This is an after-the-fact edit to remove support for out-of-order requests and resources.
 *
 * @author bobt
 *
 */
public class ETO_2024_12_12 extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_12_12.class);

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

    public Ics213RRMessage ics213RRMessage;
    public Ics214Message ics214Message;

    public String allRequests;
    public String allResources;
    public String allActivities;

    public int activityCount;
    public boolean isNice;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list
          .addAll(List
              .of("Ics213RR mId", "Ics214 mId", //
                  "Requests", "Resources", "Activities", //
                  "Activity Count", "Is Nice") //
          );
      return list.toArray(new String[0]);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(List
              .of(mId(ics213RRMessage), mId(ics214Message), //
                  allRequests, allResources, allActivities, //
                  s(activityCount), Boolean.toString(isNice) //
              ));

      return list.toArray(new String[0]);
    };
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    acceptableMessageTypesSet = new LinkedHashSet<MessageType>(List.of(MessageType.ICS_213_RR, MessageType.ICS_214));
    outboundMessageExtraContent = getNagString(2025, 2) + FeedbackProcessor.OB_DISCLAIMER;
    super.initialize(cm, mm, logger);
  }

  @Override
  protected void beforeProcessingForSender(String sender) {
    super.beforeProcessingForSender(sender);

    // #MM must instantiate a derived Summary object
    iSummary = summaryMap.getOrDefault(sender, new Summary(sender));
    summaryMap.put(sender, iSummary);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var summary = (Summary) iSummary;

    var type = message.getMessageType();
    if (type == MessageType.ICS_213_RR) {
      handle_Ics213RRMessage(summary, (Ics213RRMessage) message);
    } else if (type == MessageType.ICS_214) {
      handle_Ics214Message(summary, (Ics214Message) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_Ics213RRMessage(Summary summary, Ics213RRMessage m) {
    count(sts.test("Agency/Group name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Box 1: Incident Name should be #EV", "Exercise Santa Wish List", m.incidentName));
    count(sts.testIfPresent("Box 2: Date/Time should be present", m.activityDateTime));
    count(sts.test("Box 3 Resource Request Number should be #EV", " Santa 001", m.requestNumber));

    final String[] REQUESTS = new String[] { //
        "Wolf River Silver Bullet 1000", //
        "LDG Electronics AT-1000ProII Automatic Antenna Tuner", //
        "Heil Sound PRO 7 Headset", //
        "Bioenno Power BLF-1220A LiFePO4 Battery", //
        "RigExpert Antenna Analyzer AA-55ZOOM", //
        "Kenwood TS-990S HF/6 Meter Base Transceiver", //
        "DX Engineering Hat DXE-HAT" //
    };

    var lineItems = m.lineItems;
    for (var index = 0; index < lineItems.size(); ++index) {
      var lineNumber = index + 1;
      var lineItem = lineItems.get(index);

      if (lineNumber >= 1 && lineNumber <= 7) {
        count(sts.test("Box 4 line " + lineNumber + ": Qty should be #EV", "1", lineItem.quantity()));
        count(sts.test("Box 4 line " + lineNumber + ": Kind should be #EV", "N/A", lineItem.kind()));
        count(sts.test("Box 4 line " + lineNumber + ": Type should be #EV", "N/A", lineItem.type()));
        count(sts.test_2line("Box 4 line " + lineNumber + ": Item should be #EV", REQUESTS[index], lineItem.item()));
        getCounter("Box 4 line " + lineNumber + " item").increment(lineItem.item());

        var requestedDate = lineItem.requestedDateTime() == null ? "(null)" : lineItem.requestedDateTime();
        var resourceDateOk = startsWithAnyOf(requestedDate, List.of("2024-12-25", "12/25/2024"));
        count(sts
            .test("Box 4 line " + lineNumber + ": Requested Date/Time should start with 2024-12-25", resourceDateOk,
                requestedDate));

        count(sts
            .testIfEmpty("Box 4 line " + lineNumber + ": Estimated Date/Time should be empty",
                lineItem.estimatedDateTime()));
        count(sts.testIfEmpty("Box 4 line " + lineNumber + ": Cost should be empty", lineItem.cost()));
      } else {
        count(sts.testIfEmpty("Box 4 line 8: Qty should be empty", lineItem.quantity()));
        count(sts.testIfEmpty("Box 4 line 8: Kind should be empty", lineItem.kind()));
        count(sts.testIfEmpty("Box 4 line 8: Type should be empty", lineItem.type()));
        count(sts.testIfEmpty("Box 4 line 8: Item Description should be empty", lineItem.item()));
        count(sts.testIfEmpty("Box 4 line 8: Requested Date/Time should be empty", lineItem.requestedDateTime()));
        count(sts.testIfEmpty("Box 4 line 8: Estimated Date/Time should be empty", lineItem.estimatedDateTime()));
        count(sts.testIfEmpty("Box 4 line 8: Cost should be empty", lineItem.cost()));
      }
    } // end loop over lines

    count(sts.testIfPresent("Box 5, Delivery Location should be present", m.delivery));
    count(sts.test("Box 6 Substitutes should be #EV", "DX Engineering", m.substitutes));
    getCounter("Box 6 Substitutes").increment(m.substitutes);

    sts.testIfPresent("Box 7 Requested By should be present", m.requestedBy);
    count(sts.test("Box 8 Priority should be #EV", "Routine", m.priority));
    getCounter("Box Priorities").increment(m.priority);
    count(sts.test("Box 9 Section Chief Name should be #EV", "Bernard Elf", m.approvedBy));

    count(sts.testIfEmpty("Box 10 Logistics Order Number should be empty", m.logisticsOrderNumber));
    count(sts.testIfEmpty("Box 11 Supplier Info should be empty", m.supplierInfo));
    count(sts.testIfEmpty("Box 12 Supplier Name should be empty", m.supplierName));
    count(sts.testIfEmpty("Box 12A Supplier POC should be empty", m.supplierPointOfContact));
    count(sts.testIfEmpty("Box 13 Logistics Notes should be empty", m.supplyNotes));
    count(sts.testIfEmpty("Box 14 Logistics Authorized By should be empty", m.logisticsAuthorizer));
    count(sts.testIfEmpty("Box 15 Logistics Authorized Date/Time should be empty", m.logisticsDateTime));
    count(sts.testIfEmpty("Box 16 Order Requested By should be empty", m.orderedBy));

    count(sts.testIfEmpty("Box 17 Finance Reply/Comments should be empty", m.financeComments));
    count(sts.testIfEmpty("Box 18 Finance Section Chief Name should be empty", m.financeName));
    count(sts.testIfEmpty("Box 19 Finance Date/Time should be empty", m.financeDateTime));

    // #MM update summary
    var allRequests = String.join("\n", lineItems.stream().map(a -> a.item()).filter(Objects::nonNull).toList());
    summary.allRequests = allRequests;
    summary.ics213RRMessage = m;
  }

  private void handle_Ics214Message(Summary summary, Ics214Message m) {
    count(sts.test("Box 1 Incident Name should be #EV", "Exercise Santa Wish List", m.incidentName));
    count(sts.test("Box 1 Page # should be #EV", "1", m.page));

    // this will be a nightmare, maybe just check for presence
    count(sts.testIfPresent("Box 2 Op Period From should be present", m.opFrom));
    count(sts.testIfPresent("Box 2 Op Period To should be present", m.opTo));

    var selfResource = m.selfResource;
    count(sts.testIfPresent("Box 3 Name should be present", selfResource.name()));
    count(sts.testIfPresent("Box 4 ICS Position should be present", selfResource.icsPosition()));

    // link 213RR messageId
    if (summary.ics213RRMessage != null) {
      var trueMid = summary.ics213RRMessage.messageId;
      var reportedMid = m.selfResource.homeAgency();
      var areLinked = toKey(trueMid).equals(toKey(reportedMid));
      count(sts
          .test("Box 5: Home Agency and Unit should match ICS-213-RR messageId", areLinked, //
              null,
              "Box 5: Home Agency and Unit should match ICS-213-RR messageId: " + trueMid + ", not " + reportedMid));

    } else {
      count(sts
          .test("Box 5: Home Agency and Unit should match ICS-213-RR messageId: #EV", false, null,
              "because no ICS-213-RR received"));
    }

    final var NAMES = new String[] { "Santa Claus", "Mrs. Claus", "Rudolf", "The Grinch", "The Nutcracker" };
    final var POSITIONS = new String[] { "Logistics Unit Leader", "Incident Commander", "Ground Support Unit Leader",
        "Supply Unit Leader", "Food Unit Leader" };
    var resources = m.assignedResources;

    for (var index = 0; index < resources.size(); ++index) {
      var lineNumber = index + 1;
      var resource = resources.get(index);

      if (lineNumber >= 1 && lineNumber <= 5) {
        count(sts.test("Box 6 line " + lineNumber + ": Name should be #EV", NAMES[index], resource.name()));
        count(sts
            .test("Box 6 line " + lineNumber + ": ICS Position should #EV", POSITIONS[index], resource.icsPosition()));
      } else {
        count(sts.testIfEmpty("Box 6 line " + lineNumber + ": Name should be empty", resource.name()));
        count(sts.testIfEmpty("Box 6 line " + lineNumber + ": ICS Position should be empty", resource.icsPosition()));
      }

      count(sts
          .testIfEmpty("Box 6 line " + lineNumber + ": Home Agency and Unit should be empty", resource.homeAgency()));
    } // end loop over lines

    var activities = m.activities;
    var allActivitiesList = activities.stream().map(a -> a.activities()).filter(Objects::nonNull).toList();
    var allActivities = String.join("\n", allActivitiesList);

    var validActivityCount = 0;
    var activityLineNumber = 0;
    for (var a : activities) {
      ++activityLineNumber;
      if (isFull(a.dateTimeString()) && isFull(a.activities())) {
        ++validActivityCount;
        var dtString = a.dateTimeString().trim();
        count(sts
            .test("activity line #" + activityLineNumber + " should start with 12-25-2024",
                startsWithAnyOf(dtString, List.of("12-25-2024", "2024-12-25")), dtString));
      }
    }

    final int NUMBER_OF_ACTIVITIES_TO_BE_NICE = 2;
    var isNice = validActivityCount >= NUMBER_OF_ACTIVITIES_TO_BE_NICE;
    count(sts
        .test("Number of Notable Activites should be at least " + NUMBER_OF_ACTIVITIES_TO_BE_NICE, isNice,
            " not: " + String.valueOf(validActivityCount)));

    var expectedPreparedBy = selfResource.name() + "/" + selfResource.icsPosition();
    var isMatch = toKey(expectedPreparedBy).equals(toKey(m.preparedBy));
    count(sts.test("Box 9 Prepared By should match Boxes 3 and 4", isMatch, m.preparedBy));

    // #MM update summary
    var allResourcesList = resources
        .stream()
          .filter(a -> a != null && a.name() != null)
          .map(a -> a.name() + ", " + a.icsPosition())
          .toList();
    var allResources = String.join("\n", allResourcesList);

    summary.ics214Message = m;
    summary.allResources = allResources;
    summary.allActivities = allActivities;
    summary.activityCount = validActivityCount;
    summary.isNice = isNice;
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("");

    var summary = (Summary) summaryMap.get(sender); // #MM
    if (summary.ics213RRMessage == null) {
      summary.explanations.add("No ICS-213-RR message received, so Santa doesn't know what's on you wish list!");
    }

    if (summary.ics214Message == null) {
      summary.explanations.add("No ICS-214 message received, so Santa doesn't know if you've been naughty or nice!");
    }

    summaryMap.put(sender, summary); // #MM
  }

}
