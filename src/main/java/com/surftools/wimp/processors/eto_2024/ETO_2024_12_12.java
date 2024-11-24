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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
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
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
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

  private static final List<String> RESOURCE_LIST = List
      .of(//
          "Wolf River Silver Bullet 1000", //
          "LDG Electronics AT-1000ProII Automatic Antenna Tuner", //
          "Heil Sound PRO 7 Headset", //
          "Bioenno Power BLF-1220A LiFePO4 Battery", //
          "RigExpert Antenna Analyzer AA-55ZOOM", //
          "Kenwood TS-990S HF/6 Meter Base Transceiver", //
          "DX Engineering Hat DXE-HAT" //
      );

  private static final List<String> RESOURCE_KEYS = RESOURCE_LIST
      .stream()
        .map(s -> SimpleTestService.toKey(s))
        .collect(Collectors.toList());

  private static final Set<String> RESOURCE_KEY_SET = new HashSet<>(RESOURCE_KEYS);

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
                  "Resources", "Requests", "Activities", //
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
    count(sts.test("Agency/Group name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Box 1: Incident Name should be #EV", "Exercise Santa Wish List", m.incidentName));
    count(sts.testIfPresent("Box 2: Date/Time should be present", m.activityDateTime));
    count(sts.test("Box 3 Resource Request Number should be #EV", " Santa 001", m.requestNumber));

    var lineItems = m.lineItems;
    var foundArray = new boolean[RESOURCE_LIST.size()];
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
        var isFound = RESOURCE_KEY_SET.contains(key);
        var bestIndex = findBestMatchingResourceIndex(item);
        var bestResource = RESOURCE_LIST.get(bestIndex);
        if (isFound) {
          foundArray[bestIndex] = true;
          count(sts.test("Box 4 line " + lineNumber + ": Item Description should be #EV", bestResource, item));
        } else {
          count(sts.test_2line("Box 4 line " + lineNumber + ": Item Description should be #EV", bestResource, item));
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

    for (var index = 0; index < RESOURCE_LIST.size(); ++index) {
      var isFound = foundArray[index];
      count(sts.test("Box 4: item " + RESOURCE_LIST.get(index) + " requested", isFound));
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

    final var resourceNames = List.of("Santa Claus", "Mrs. Claus", "Rudolf", "The Grinch", "the Nutcracker");
    final var ucResourceNames = resourceNames.stream().map(a -> a.toUpperCase()).collect(Collectors.toList());

    final var icsPositions = List
        .of("Incident Commander", "Public Information Officer", "Safety Officer", "Liaison Officer",
            "Operations Section Chief", "Finance Section Chief", "Logistics Section Chief",
            "Finance/Admin Section Chief");
    final var ucPositions = icsPositions.stream().map(a -> a.toUpperCase()).collect(Collectors.toList());

    // TODO implement
    // count(sts.test("OPTION (via comments) should be readable", option >= 1, m.comments));

    // TODO search for 213RR messageId

    // #MM update summary
    summary.ics214Message = m;
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

    // TODO naught/nice, sentiment, nag

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM
  }

  private int findBestMatchingResourceIndex(String searchName) {
    var searchKey = sts.toAlphaNumericString(searchName);
    var sifter = new Sift4();
    var rankedMap = new TreeMap<Double, List<Integer>>();
    for (var i = 0; i < RESOURCE_LIST.size(); ++i) {
      var key = RESOURCE_KEYS.get(i);
      var name = RESOURCE_LIST.get(i);
      var distance = sifter.distance(searchKey, key);
      var list = rankedMap.getOrDefault(distance, new ArrayList<Integer>());
      list.add(i);
      rankedMap.put(distance, list);
      logger.debug("resource: " + name + ", score: " + distance);
    }

    var firstEntry = rankedMap.firstEntry();
    var list = firstEntry.getValue();
    if (list.size() > 1) {
      logger.warn("multiple resources found for: " + searchName);
    }

    return list.get(0);
  }

  // https://medium.com/@varun85gupta/harnessing-sentiment-analysis-with-java-a-step-by-step-guide-using-nlp-f464398bb664
  // https://blogs.oracle.com/javamagazine/post/java-sentiment-analysis-stanford-corenlp

  /**
   * public static void main(String[] args) { // Initialize the Stanford NLP pipeline Properties properties = new
   * Properties(); properties.put("tokenize.whitespace", "true"); properties.put("ssplit.eolonly", "true");
   * properties.put("annotators", "tokenize, ssplit, parse, sentiment");
   *
   * StanfordCoreNLP pipeline = new StanfordCoreNLP(properties);
   *
   * var sampleTexts = List .of( // // "Simplest codings is the best place to learn and grow. I am glad to be here.", //
   * // "I hate this place. I am not coming back here again. I am very disappointed.", // "Signed in Hospital Command
   * Center", // "Received appointment as Safety Officer", // "Participated in Incident Briefing", // "Developed
   * Incident Action Plan Safety Analysis 215A and presented to Incident commander for approval.", // "Participated in
   * Command and General Staff Meeting" // );
   *
   * var mergedTexts = List.of(String.join(". ", sampleTexts));
   *
   * var useSingleTexts = true; var texts = (useSingleTexts) ? sampleTexts : mergedTexts;
   *
   * for (var text : texts) { var sentiment = getSentiment(text, pipeline); System.out.println("Text: " + text + ",
   * Sentiment: " + sentiment); // // estimatingSentiment(text, pipeline); // System.out.println(); }
   *
   * }
   *
   * private static String getSentiment(String text, StanfordCoreNLP pipeline) { // Create an Annotation object with the
   * input text Annotation annotation = new Annotation(text);
   *
   * // Run all the NLP annotators on the text pipeline.annotate(annotation);
   *
   * // Extract the sentiment from the annotation CoreMap sentence =
   * annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0); String sentiment =
   * sentence.get(SentimentCoreAnnotations.SentimentClass.class);
   *
   * return sentiment; }
   *
   * public static void estimatingSentiment(String text, StanfordCoreNLP pipeline) { int sentimentInt; String
   * sentimentName; Annotation annotation = pipeline.process(text); for (CoreMap sentence :
   * annotation.get(CoreAnnotations.SentencesAnnotation.class)) { Tree tree =
   * sentence.get(SentimentAnnotatedTree.class); sentimentInt = RNNCoreAnnotations.getPredictedClass(tree);
   * sentimentName = sentence.get(SentimentCoreAnnotations.SentimentClass.class); System.out.println(sentimentName +
   * "\t" + sentimentInt + "\t" + sentence); } }
   *
   **/
}
