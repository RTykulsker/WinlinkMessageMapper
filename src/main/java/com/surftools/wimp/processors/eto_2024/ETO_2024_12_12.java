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

package com.surftools.wimp.processors.eto_2024;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.processors.std.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

import info.debatty.java.stringsimilarity.experimental.Sift4;

/**
 * Processor for 2024-12-12: an ICS-213-RR and an ICS-214 that references the RR, themed around Santa's Wish List
 *
 * @author bobt
 *
 */
public class ETO_2024_12_12 extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_12_12.class);

  private static final int NUMBER_OF_ACTIVITIES_TO_BE_NICE = 2;

  private boolean hasSentimentService = false;

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
    public String sentiment;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list
          .addAll(Arrays
              .asList(new String[] { "Ics213RR mId", "Ics214 mId", //
                  "Requests", "Resources", "Activities", //
                  "Activity Count", "Is Nice", "Sentiment", //
              }));
      return list.toArray(new String[0]);
    }

    private String s(int i) {
      return String.valueOf(i);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(Arrays
              .asList(new String[] { mId(ics213RRMessage), mId(ics214Message), //
                  allRequests, allResources, allActivities, //
                  s(activityCount), Boolean.toString(isNice), sentiment, //
              }));

      return list.toArray(new String[0]);
    };
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    // #MM must define acceptableMessages
    var acceptableMessageTypesList = List.of(MessageType.ICS_213_RR, MessageType.ICS_214); // order matters, last
                                                                                           // location wins,
    acceptableMessageTypesSet.addAll(acceptableMessageTypesList);

    outboundMessageExtraContent = getNagString(2025) + FeedbackProcessor.OB_DISCLAIMER;

    hasSentimentService = testSentimentService();
    if (!hasSentimentService) {
      logger.error("#### SentimentServer not running! #####");
      final int SLEEP_SECONDS = 60;
      logger.error("#### Sleeping for " + SLEEP_SECONDS + " seconds! #####");
      try {
        TimeUnit.SECONDS.sleep(SLEEP_SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private boolean testSentimentService() {
    try {
      var httpClient = HttpClient.newBuilder().build();
      var request = HttpRequest.newBuilder().uri(URI.create("http://localhost:7000/status")).GET().build();
      var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      var statusCode = response.statusCode();
      var body = response.body();
      logger.info("statusCode: " + statusCode + ", body: " + body);
      return body.equals("alive");
    } catch (Exception e) {
      logger.error("error getting sentimentServer status: " + e.getLocalizedMessage());
      return false;
    }
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
    if (type == MessageType.PLAIN) {
      handle_PlainMessage(summary, (PlainMessage) message);
    } else if (type == MessageType.ICS_213_RR) {
      handle_Ics213RRMessage(summary, (Ics213RRMessage) message);
    } else if (type == MessageType.ICS_214) {
      handle_Ics214Message(summary, (Ics214Message) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_PlainMessage(Summary summary, PlainMessage message) {
    count(sts.test("Message could not be identified (missing form attachment?)", false, message.messageId));
  }

  private void handle_Ics213RRMessage(Summary summary, Ics213RRMessage m) {

    final List<String> REQUEST_LIST = List
        .of(//
            "Wolf River Silver Bullet 1000", //
            "LDG Electronics AT-1000ProII Automatic Antenna Tuner", //
            "Heil Sound PRO 7 Headset", //
            "Bioenno Power BLF-1220A LiFePO4 Battery", //
            "RigExpert Antenna Analyzer AA-55ZOOM", //
            "Kenwood TS-990S HF/6 Meter Base Transceiver", //
            "DX Engineering Hat DXE-HAT" //
        );

    final List<String> REQUEST_KEYS = REQUEST_LIST.stream().map(s -> toKey(s)).collect(Collectors.toList());

    final Set<String> REQUEST_KEY_SET = new HashSet<>(REQUEST_KEYS);

    count(sts.test("Agency/Group name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Box 1: Incident Name should be #EV", "Exercise Santa Wish List", m.incidentName));
    count(sts.testIfPresent("Box 2: Date/Time should be present", m.activityDateTime));
    count(sts.test("Box 3 Resource Request Number should be #EV", " Santa 001", m.requestNumber));

    var lineItems = m.lineItems;
    var foundArray = new boolean[REQUEST_LIST.size()];
    var allLinesInOrder = true;
    for (var index = 0; index < lineItems.size(); ++index) {
      var lineNumber = index + 1;
      var lineItem = lineItems.get(index);

      if (lineNumber >= 1 && lineNumber <= 7) {
        count(sts.test("Box 4 line " + lineNumber + ": Qty should be #EV", "1", lineItem.quantity()));
        count(sts.test("Box 4 line " + lineNumber + ": Kind should be #EV", "N/A", lineItem.kind()));
        count(sts.test("Box 4 line " + lineNumber + ": Type should be #EV", "N/A", lineItem.type()));

        // order DOES NOT matter
        var item = lineItem.item();
        var key = sts.toAlphaNumericString(item);
        var isFound = REQUEST_KEY_SET.contains(key);
        var bestIndex = findBestMatchingIndex(item, REQUEST_LIST, REQUEST_KEYS);
        var bestRequest = REQUEST_LIST.get(bestIndex);
        if (isFound) {
          foundArray[bestIndex] = true;
          count(sts.test("Box 4 line " + lineNumber + ": Item Description should be #EV", bestRequest, item));
        } else {
          count(sts.test_2line("Box 4 line " + lineNumber + ": Item Description should be #EV", bestRequest, item));
        }

        if (bestIndex != index) {
          allLinesInOrder = false;
        }

        count(sts
            .test("Box 4 line " + lineNumber + ": Requested Date/Time should be #EV", "2024-12-25",
                lineItem.requestedDateTime()));
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

    count(sts.test("Box 4: All items should be in order", allLinesInOrder));

    for (var index = 0; index < REQUEST_LIST.size(); ++index) {
      var isFound = foundArray[index];
      count(sts.test("Box 4: item " + REQUEST_LIST.get(index) + " requested", isFound));
    }

    count(sts.testIfPresent("Box 5, Delivery Location should be present", m.delivery));
    count(sts.test("Box 6 Substitutes should be #EV", "DX Engineering", m.delivery));
    sts.testIfPresent("Box 7 Requested By should be present", m.requestedBy);
    count(sts.test("Box 8 Priority should be #EV", "Routine", m.priority));
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
    var allRequests = String
        .join("\n", lineItems.stream().map(a -> a.item()).filter(Objects::nonNull).collect(Collectors.toList()));
    summary.allRequests = allRequests;
    summary.ics213RRMessage = m;
  }

  private void handle_Ics214Message(Summary summary, Ics214Message m) {
    final Map<String, String> RESOURCE_MAP = Map
        .of(//
            "Santa Clause", "Logistics Unit Leader", //
            "Mrs. Claus", "Incident Commander", //
            "Rudolf", "Ground Support Unit Leader", //
            "The Grinch", "Supply Unit Leader", //
            "The Nutcracker", "Food Unit Leader");

    final List<String> RESOURCE_LIST = RESOURCE_MAP.keySet().stream().collect(Collectors.toList());

    final List<String> RESOURCE_KEYS = RESOURCE_LIST.stream().map(s -> toKey(s)).collect(Collectors.toList());

    final Set<String> RESOURCE_KEY_SET = new HashSet<>(RESOURCE_KEYS);

    final Map<String, String> RESOURCE_KEY_MAP = new HashMap<>();

    if (RESOURCE_KEY_MAP.size() == 0) {
      for (var key : RESOURCE_MAP.keySet()) {
        RESOURCE_KEY_MAP.put(toKey(key), RESOURCE_MAP.get(key));
      }
    }

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
              reportedMid,
              "Box 5: Home Agency and Unit should match ICS-213-RR messageId: " + trueMid + ", not " + reportedMid));

    } else {
      count(sts
          .test("Box 5: Home Agency and Unit should match ICS-213-RR messageId: #EV", false, null,
              "because no ICS-213-RR received"));
    }

    var resources = m.assignedResources;
    var foundArray = new boolean[RESOURCE_LIST.size()];
    var allResourcesInOrder = true;
    for (var index = 0; index < resources.size(); ++index) {
      var lineNumber = index + 1;
      var resource = resources.get(index);

      if (lineNumber >= 1 && lineNumber <= 5) {
        // order DOES NOT matter
        var name = resource.name();
        var key = sts.toAlphaNumericString(name);
        var isFound = RESOURCE_KEY_SET.contains(key);
        var bestIndex = findBestMatchingIndex(name, RESOURCE_LIST, RESOURCE_KEYS);
        var bestResource = RESOURCE_LIST.get(bestIndex);
        if (isFound) {
          foundArray[bestIndex] = true;
          count(sts.test("Box 6 line " + lineNumber + ": Name should be #EV", bestResource, name));

          // ICS position
          var expectedIcsPosition = RESOURCE_KEY_MAP.get(key);
          count(sts
              .test("Box 6 line " + lineNumber + ": ICS Position should be #EV", expectedIcsPosition,
                  resource.icsPosition()));
        } else {
          System.err
              .println("not found: name: " + name + ", bestIndex: " + bestIndex + ", bestResouce: " + bestResource);
          count(sts.test_2line("Box 6 line " + lineNumber + ": Name should be #EV", bestResource, name));
        }

        if (bestIndex != index) {
          allResourcesInOrder = false;
        }

        count(sts
            .testIfEmpty("Box 6 line " + lineNumber + ": Home Agency and Unit should be empty", resource.homeAgency()));

      } else {
        count(sts.testIfEmpty("Box 6 line " + lineNumber + ": Name should be empty", resource.name()));
        count(sts.testIfEmpty("Box 6 line " + lineNumber + ": ICS Position should be empty", resource.icsPosition()));
        count(sts
            .testIfEmpty("Box 6 line " + lineNumber + ": Home Agency and Unit should be empty", resource.homeAgency()));
      }
    } // end loop over lines

    count(sts.test("Box 6: All items should be in order", allResourcesInOrder));

    for (var index = 0; index < RESOURCE_LIST.size(); ++index) {
      var isFound = foundArray[index];
      count(sts.test("Box 6: resource " + RESOURCE_LIST.get(index) + " not found", isFound));
    }

    var activities = m.activities;
    var allActivitiesList = activities
        .stream()
          .map(a -> a.activities())
          .filter(Objects::nonNull)
          .collect(Collectors.toList());

    var isNice = allActivitiesList.size() >= NUMBER_OF_ACTIVITIES_TO_BE_NICE;
    count(sts
        .test("Number of Notable Activites should be at least " + NUMBER_OF_ACTIVITIES_TO_BE_NICE, isNice,
            " not: " + String.valueOf(allActivitiesList.size())));

    var allActivities = String.join("\n", allActivitiesList);

    var expectedPreparedBy = selfResource.name() + "/" + selfResource.icsPosition();
    count(sts.test("Box 9 Prepared By should match Boxes 3 and 4: #EV", expectedPreparedBy, m.preparedBy));

    var sentiment = getSentiment(allActivities);

    // #MM update summary
    var allResourcesList = resources
        .stream()
          .filter(Objects::nonNull)
          .filter(a -> a.name() != null)
          .map(a -> a.name() + ", " + a.icsPosition())
          .collect(Collectors.toList());
    var allResources = String.join("\n", allResourcesList);

    summary.ics214Message = m;
    summary.allResources = allResources;
    summary.allActivities = allActivities;
    summary.activityCount = allActivitiesList.size();
    summary.isNice = isNice;
    summary.sentiment = sentiment;

  }

  private String getSentiment(String allActivities) {
    if (!hasSentimentService) {
      return ("n/a");
    }

    if (allActivities == null) {
      return "null";
    }

    try {
      var httpClient = HttpClient.newBuilder().build();
      var formData = Map.of("q", allActivities);
      var request = HttpRequest
          .newBuilder()
            .uri(URI.create("http://localhost:7000/query"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .build();
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      var statusCode = response.statusCode();
      var body = response.body();
      logger.debug("statusCode: " + statusCode + ", body: " + body);
      return body;
    } catch (Exception e) {
      logger.error("error getting sentimentServer status: " + e.getLocalizedMessage());
      return "error";
    }
  }

  private String getFormDataAsString(Map<String, String> formData) {
    var sb = new StringBuilder();
    for (var entry : formData.entrySet()) {
      if (sb.length() > 0) {
        sb.append("&");
      }
      sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
      sb.append("=");
      sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return sb.toString();
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("");

    var summary = (Summary) summaryMap.get(sender); // #MM
    if (summary.ics213RRMessage == null) {
      summary.explanations.add("No ICS-213-RR message received.");
    }

    if (summary.ics214Message == null) {
      summary.explanations.add("No ICS-214 message received.");
    }

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM
  }

  private int findBestMatchingIndex(String searchName, List<String> nameList, List<String> keyList) {
    var searchKey = sts.toAlphaNumericString(searchName);
    var sifter = new Sift4();
    var rankedMap = new TreeMap<Double, List<Integer>>();
    for (var i = 0; i < nameList.size(); ++i) {
      var key = keyList.get(i);
      var name = nameList.get(i);
      var distance = sifter.distance(searchKey, key);
      var list = rankedMap.getOrDefault(distance, new ArrayList<Integer>());
      list.add(i);
      rankedMap.put(distance, list);
      logger.debug("request: " + name + ", score: " + distance);
    }

    var firstEntry = rankedMap.firstEntry();
    var list = firstEntry.getValue();
    if (list.size() > 1) {
      logger.warn("multiple resources found for: " + searchName);
    }

    return list.get(0);
  }

}
