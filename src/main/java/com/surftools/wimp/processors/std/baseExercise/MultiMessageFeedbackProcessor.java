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

package com.surftools.wimp.processors.std.baseExercise;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.feedback.StandardSummary;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.BulkInsertEntry;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.processors.std.AcknowledgementProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.chart.ChartServiceFactory;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.service.outboundMessage.AbstractBaseOutboundMessageEngine;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * more common processing, exercise processors must implement specificProcessing(...)
 *
 * explicitly for multiple messages; no TypeEntry, or typeEntryMap
 *
 * support multiple message types
 */
public abstract class MultiMessageFeedbackProcessor extends AbstractBaseFeedbackProcessor {

  protected Set<MessageType> acceptableMessageTypesSet = new LinkedHashSet<>(); // order matters

  public interface ISummary extends IWritableTable {
  };

  // concrete, so we can initialize
  public class BaseSummary implements ISummary {
    public String from;
    public String to;
    public LatLongPair location;
    public LocalDateTime dateTime;
    public List<String> explanations; // doesn't get published, but interpreted
    public int perfectMessageCount;
    public static String perfectMessageText = "Perfect messages!";
    public String messageIds;

    @Override
    public int compareTo(IWritableTable o) {
      var other = (BaseSummary) o;
      return from.compareTo(other.from);
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>(
          List.of("From", "To", "Latitude", "Longitude", "Date", "Time", "Feedback Count", "Feedback"));
      return list.toArray(new String[list.size()]);
    }

    public String getFeedbackCountString() {
      var feedbackCount = "0";

      if (explanations.size() > 0) {
        feedbackCount = String.valueOf(explanations.size() - perfectMessageCount);
      }

      return feedbackCount;
    }

    public String getFeedback() {
      var feedback = perfectMessageText;

      if (explanations.size() > 0) {
        feedback = String.join("\n", explanations);
      }

      return feedback;
    }

    @Override
    public String[] getValues() {
      var latitude = location == null ? "0.0" : location.getLatitude();
      var longitude = location == null ? "0.0" : location.getLongitude();
      var date = dateTime == null ? "" : dateTime.toLocalDate().toString();
      var time = dateTime == null ? "" : dateTime.toLocalTime().toString();
      var feedbackCount = getFeedbackCountString();
      var feedback = getFeedback();

      var list = new ArrayList<String>(
          List.of(ns(from), ns(to), latitude, longitude, date, time, feedbackCount, feedback));
      return list.toArray(new String[list.size()]);
    }
  }

  protected Map<String, BaseSummary> summaryMap = new LinkedHashMap<>();
  protected BaseSummary iSummary; // summary for current sender
  protected String messageId; // the mId of the current message
  protected MessageType messageType; // the messageType of the current message
  protected boolean doFirstIn = true; // fifo vs lifo for location, to fields

  public int ppMessageCount = 0;
  public int ppParticipantCount = 0;
  public int ppParticipantCorrectCount = 0;

  private List<String> badLocationSenders = new ArrayList<>();

  protected String outboundMessageExtraContent = FeedbackProcessor.OB_DISCLAIMER;

  protected boolean allowPerfectMessageReporting = true;

  protected record PerfectMessage(ExportedMessage m) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable o) {
      var other = (ExportedMessage) o;
      return this.compareTo(other);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "Type", "MessageId", "File Name" };
    }

    @Override
    public String[] getValues() {
      return new String[] { m.from, m.getMessageType().toString(), m.messageId, m.fileName };
    }

  }

  protected List<IWritableTable> perfectMessages = new ArrayList<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);

    if (acceptableMessageTypesSet == null || acceptableMessageTypesSet.size() == 0) {
      logger.error("acceptableMessageTypesSet not initialized in exercise processor. Exiting!");
      System.exit(1);
    }

    for (var type : acceptableMessageTypesSet) {
      logger.info("will accept " + type.toString() + " messages");
    }

  }

  protected boolean breakLoopForMessageType = false;

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) { // loop over senders
      sender = senderIterator.next();
      beforeProcessingForSender(sender);

      // process all messages for a type, in chronological order, in type order
      var map = mm.getMessagesForSender(sender);
      for (var messageType : acceptableMessageTypesSet) {
        var typedMessages = map.get(messageType);
        if (typedMessages == null || typedMessages.size() == 0) {
          continue;
        }
        breakLoopForMessageType = false;
        for (var message : typedMessages) {
          beginCommonProcessing(message);
          specificProcessing(message);
          if (breakLoopForMessageType) {
            break;
          }
          endCommonProcessing(message);
        } // end processing for a message
      } // end processing for a messageType
      endProcessingForSender(sender); // abstract in parent!
      baseEndProcessingForSender(sender);
    } // end loop over senders
  }

  /**
   * derived class really must implement this
   *
   * @param sender
   */
  @Override
  protected abstract void endProcessingForSender(String sender);

  /**
   * before all messageTypes for a given sender, a chance to look at cross-message relations
   */
  @Override
  protected void beforeProcessingForSender(String sender) {
    ++ppParticipantCount;
    sts.reset(sender);
  }

  /**
   * after all messageTypes for a given sender, a chance to look at cross-message relations
   */

  protected void baseEndProcessingForSender(String sender) {
    if (iSummary == null) {
      return;
    }

    if (iSummary.location == null || !iSummary.location.isValid()) {
      badLocationSenders.add(sender);
    }

    iSummary.explanations.addAll(sts.getExplanations());
    var nExplanations = iSummary.explanations.size();
    if (nExplanations == 0) {
      ++ppParticipantCorrectCount;
    }
    getCounter("Feedback Count").increment(nExplanations);

    summaryMap.put(sender, iSummary);
  }

  @Override
  protected void beginCommonProcessing(ExportedMessage message) {
    super.beginCommonProcessing(message);

    ++ppMessageCount;

    iSummary = summaryMap.get(sender);

    var explanationPrefix = "(" + message.getMessageType().toString() + ") (" + message.messageId + "): ";
    sts.setExplanationPrefix(explanationPrefix);

    // first-in or last-in for to and location
    if (doFirstIn) {
      if (iSummary.to == null) {
        iSummary.to = message.to;
      }

      if (iSummary.location == null) {
        iSummary.location = message.mapLocation;
      }

      if (iSummary.dateTime == null) {
        iSummary.dateTime = message.msgDateTime;
      }
    } else {
      if (message.to != null) {
        iSummary.to = message.to;
      }

      if (message.mapLocation != null && message.mapLocation.isValid()) {
        iSummary.location = message.mapLocation;
      }

      if (message.msgDateTime != null) {
        iSummary.dateTime = message.msgDateTime;
      }

    }

    var msgLocation = message.msgLocation;
    if (msgLocation == null || msgLocation.equals(LatLongPair.ZERO_ZERO)) {
      sts.test("Message LAT/LON should be provided", false, "missing");
    } else if (!msgLocation.isValid()) {
      sts.test("Message LAT/LON should be provided", false, "invalid " + msgLocation.toString());
    } else {
      sts.test("Message LAT/LON should be provided", true, null);
    }

  }

  @Override
  protected void endCommonProcessing(ExportedMessage message) {
    super.endCommonProcessing(message);
  }

  /**
   * this is the exercise-specific method to be implemented
   *
   * @param message
   */
  @Override
  protected abstract void specificProcessing(ExportedMessage message);

  @Override
  protected String makeOutboundMessageFeedback(BaseSummary summary) {
    var outboundMessageFeedback = (summary.explanations.size() == 0) ? "Perfect messages!"
        : String.join("\n", summary.explanations);
    return outboundMessageFeedback;
  }

  @Override
  public void postProcess() {

    var sb = new StringBuilder();
    sb.append("\n\n" + cm.getAsString(Key.EXERCISE_DESCRIPTION) + " aggregate results\n");
    sb.append("Messages: " + ppMessageCount + "\n");
    sb.append("Participants: " + ppParticipantCount + "\n");

    sb.append(formatPP("Correct Message Sets", ppParticipantCorrectCount, false, ppParticipantCount));

    var it = sts.iterator();
    while (it.hasNext()) {
      var key = it.next();
      if (sts.hasContent(key)) {
        sb.append(sts.format(key));
      }
    }

    sb.append("\n-------------------Histograms---------------------\n");
    for (var counterLabel : counterMap.keySet()) {
      sb.append(formatCounter(counterLabel, counterMap.get(counterLabel)));
    }

    logger.info(sb.toString());

    if (badLocationSenders.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationSenders.size() + " senders: "
              + String.join(",", badLocationSenders));
      var newLocations = LocationUtils.jitter(badLocationSenders.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationSenders.size(); ++i) {
        var sender = badLocationSenders.get(i);
        var iSummary = summaryMap.get(sender);
        var newLocation = newLocations.get(i);
        iSummary.location = newLocation;
        summaryMap.put(sender, iSummary);
      }
    }

    var list = new ArrayList<IWritableTable>((summaryMap.values()));
    WriteProcessor.writeTable(list, Path.of(outputPathName, "summary-feedback.csv"));

    // feedback to folks who only send unexpected messages
    // we already have a separate acknowledgement outbound message, so we just need feedback
    var enableFeedbackForOnlyUnexpected = true;
    if (enableFeedbackForOnlyUnexpected) {
      @SuppressWarnings("unchecked")
      var ackTextMap = (Map<String, String>) (mm.getContextObject(AcknowledgementProcessor.ACK_TEXT_MAP));
      var allSenderSet = new HashSet<String>(ackTextMap.keySet());
      var expectedSenderList = new ArrayList<String>(summaryMap.keySet());
      allSenderSet.removeAll(expectedSenderList);
      var unexpectedSenderSet = allSenderSet;
      logger.info("Senders who only sent unexpected messages: " + String.join(",", unexpectedSenderSet));

      var subject = cm.getAsString(Key.OUTBOUND_MESSAGE_SUBJECT);
      var typeNames = String.join(", ", acceptableMessageTypesSet.stream().map(s -> s.name()).toList());
      var text = "no " + typeNames + " messages received";
      for (var sender : unexpectedSenderSet) {
        var outboundMessage = new OutboundMessage(outboundMessageSender, sender, subject, text, null);
        outboundMessageList.add(outboundMessage);
      }
    }

    if (doOutboundMessaging) {
      for (var summary : summaryMap.values()) {
        var outboundMessageFeedback = makeOutboundMessageFeedback(summary);
        var outboundMessage = new OutboundMessage(outboundMessageSender, summary.from,
            makeOutboundMessageSubject(summary), outboundMessageFeedback,
            AbstractBaseOutboundMessageEngine.generateMid(outboundMessageFeedback));
        outboundMessageList.add(outboundMessage);
      }

      var service = new OutboundMessageService(cm, mm, outboundMessageExtraContent);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }

    // all messageTypes in one chart page
    var chartService = ChartServiceFactory.getChartService(cm);
    chartService.initialize(cm, counterMap, null);
    chartService.makeCharts();

    // maps
    makeFeedbackMap();
    makeClearinghouseMap();

    var standardSummaries = summaryMap.values().stream().map(s -> StandardSummary.fromMultiMessageFeedback(s)).toList();
    writeTable(dateString + "-standard-summary.csv", new ArrayList<IWritableTable>(standardSummaries));

    var db = new PersistenceManager(cm);
    var input = makeDbInput(cm, summaryMap.values());
    var dbResult = db.bulkInsert(input);
    if (dbResult.status() == ReturnStatus.ERROR) {
      logger.error("### database update failed: " + dbResult.content());
    }
  }

  private void makeFeedbackMap() {
    var feedbackCounter = new Counter();
    var truncatedCountMap = new HashMap<Integer, Integer>(); // 6 -> 6 or more
    for (var summary : summaryMap.values()) {
      var key = Math.min(FEEDBACK_MAP_N_LAYERS - 1, Integer.parseInt(summary.getFeedbackCountString()));
      var value = truncatedCountMap.getOrDefault(key, Integer.valueOf(0));
      ++value;
      truncatedCountMap.put(key, value);
    }

    var mapEntries = new ArrayList<MapEntry>(summaryMap.values().size());
    final var lastColorMapIndex = gradientMap.size() - 1;
    final var lastColor = gradientMap.get(lastColorMapIndex);
    for (var s : summaryMap.values()) {
      var count = Integer.parseInt(s.getFeedbackCountString());
      var location = s.location;
      var color = gradientMap.getOrDefault(count, lastColor);
      var prefix = "<b>" + s.from + "</b><hr>";
      var content = prefix + "Feedback Count: " + s.getFeedbackCountString() + "\n" + "Feedback: " + s.getFeedback();
      var mapEntry = new MapEntry(s.from, s.to, location, content, color);
      mapEntries.add(mapEntry);
      feedbackCounter.increment(Integer.parseInt(s.getFeedbackCountString()));
    }

    makeFeedbackMap(truncatedCountMap, mapEntries);
  }

  private void makeClearinghouseMap() {
    var organizationName = cm.getAsString(Key.EXERCISE_ORGANIZATION);
    if (!organizationName.equals("ETO")) {
      logger.info("skipping clearinghouse map because not defined for org: " + organizationName);
    } else {
      var colorMap = MapService.etoColorMap;
      var clearinghouseNames = new ArrayList<String>(List.of(cm.getAsString(Key.EXPECTED_DESTINATIONS).split(",")));
      clearinghouseNames.add("unknown");
      var clearinghouseCountMap = new LinkedHashMap<String, Integer>();
      for (var s : summaryMap.values()) {
        var to = s.to;
        if (!clearinghouseNames.contains(to)) {
          to = "unknown";
        }
        var count = clearinghouseCountMap.getOrDefault(to, Integer.valueOf(0));
        ++count;
        clearinghouseCountMap.put(to, count);
      }

      var mapEntries = new ArrayList<MapEntry>();
      for (var s : summaryMap.values()) {
        var to = s.to;
        var clearinghouseName = clearinghouseNames.contains(to) ? to : "unknown";
        var location = s.location;
        var color = colorMap.get(clearinghouseName);
        var prefix = "<b>From: " + s.from + "<br>To: " + to + "</b><hr>";
        var messageIds = s.messageIds;
        var content = (messageIds == null) ? "" : "MessageIds: " + messageIds + "\n";
        content = content + "Feedback Count: " + s.getFeedbackCountString() + "\n" + "Feedback: " + s.getFeedback();
        content = prefix + content;
        var mapEntry = new MapEntry(s.from, s.to, location, content, color);
        mapEntries.add(mapEntry);
      }

      makeClearinghouseMap(clearinghouseCountMap, colorMap, mapEntries);
    }
  }

  private BulkInsertEntry makeDbInput(IConfigurationManager cm, Collection<BaseSummary> summaries) {
    var exerciseDate = LocalDate.parse(cm.getAsString(Key.EXERCISE_DATE), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    var exerciseType = cm.getAsString(Key.PERSISTENCE_EXERCISE_TYPE, "Training");
    var exerciseName = cm.getAsString(Key.EXERCISE_NAME);
    var exerciseDescription = cm.getAsString(Key.EXERCISE_DESCRIPTION);
    Exercise exercise = new Exercise(-1, exerciseDate, exerciseType, exerciseName, exerciseDescription);
    List<Event> events = new ArrayList<>();
    for (var s : summaries) {
      var feedbackCount = 0;
      var feedback = BaseSummary.perfectMessageText;

      if (s.explanations.size() > 0) {
        feedbackCount = s.explanations.size() - s.perfectMessageCount;
        feedback = String.join("\n", s.explanations);
      }
      var event = new Event(-1, -1, -1, s.from, s.location, feedbackCount, feedback, null);
      events.add(event);
    }
    return new BulkInsertEntry(exercise, events);
  }

  protected boolean isPerfectMessage(ExportedMessage m) {
    if (!allowPerfectMessageReporting) {
      return false;
    }

    var mId = m.messageId;
    var prefix = sts.getPrefix();
    if (!sts.getExplanations().stream().anyMatch(s -> s.startsWith(prefix))) {
      sts.getExplanations().add(prefix + "Perfect Message! MessageId: " + mId);
      perfectMessages.add(new PerfectMessage(m));
      ++iSummary.perfectMessageCount;
      return true;
    } else {
      return false;
    }
  }

}
