/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.feedback.FeedbackMessage;
import com.surftools.wimp.feedback.FeedbackResult;
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
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * the FeedbackProcessor was once simple and clean. Then I added multi-message support to it and it became messy. I then
 *
 * I created the MultiMessageFeedbackProcessor and this, SingleMessageFeedbackProcessor is an attempt to restore
 * simplicity
 */
public abstract class SingleMessageFeedbackProcessor extends AbstractBaseFeedbackProcessor {

  protected boolean doStsFieldValidation = false;

  protected Map<String, Counter> summaryCounterMap = new LinkedHashMap<String, Counter>();

  protected int ppCount = 0;
  protected int ppMessageCorrectCount = 0;

  protected LatLongPair feedbackLocation = null;
  protected Map<String, IWritableTable> mIdFeedbackMap = new HashMap<String, IWritableTable>();
  protected List<String> badLocationMessageIds = new ArrayList<String>();

  protected String outboundMessagePrefixContent = "";
  protected String outboundMessagePostfixContent = "";
  protected String outboundMessageExtraContent = FeedbackProcessor.OB_DISCLAIMER;

  protected MessageType messageType;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);
    logger = _logger;

    var expectedMessageTypes = getExpectedMessageTypes();
    if (expectedMessageTypes.size() != 1) {
      throw new RuntimeException("Only expecting one messageType, not: "
          + String.join(",", expectedMessageTypes.stream().map(s -> s.toString()).toList()));
    }
    messageType = expectedMessageTypes.iterator().next();
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) { // loop over senders
      sender = senderIterator.next();

      // process all messages for a type, in ascending chronological order
      var messages = mm.getMessagesForSender(sender).get(messageType);
      if (messages == null || messages.size() == 0) {
        continue;
      }
      for (var message : messages) {
        beginCommonProcessing(message);
        specificProcessing(message);
        endCommonProcessing(message);
      } // end processing for a message

      endProcessingForSender(sender);
    } // end loop over senders
  }

  /**
   *
   * @param sender
   * @param message
   */
  protected void beforeCommonProcessing(String sender, ExportedMessage message) {
  }

  /**
   * after all messageTypes for a given sender, a chance to look at cross-message relations
   */
  @Override
  protected void endProcessingForSender(String sender) {
  }

  @Override
  protected void beginCommonProcessing(ExportedMessage message) {
    ++ppCount;
    sts.reset(sender);
    super.beginCommonProcessing(message);
    beforeCommonProcessing(sender, message);

    feedbackLocation = message.mapLocation;
  }

  @Override
  protected String makeOutboundMessageSubject(Object object) {
    var message = (ExportedMessage) object;
    return outboundMessageSubject + " " + message.messageId;
  }

  @Override
  protected void endCommonProcessing(ExportedMessage message) {
    if (feedbackLocation == null || feedbackLocation.equals(LatLongPair.ZERO_ZERO)) {
      feedbackLocation = LatLongPair.ZERO_ZERO;
      badLocationMessageIds.add(message.messageId);
      sts.test("LAT/LON should be provided", false, "missing");
    } else if (!feedbackLocation.isValid()) {
      sts.test("LAT/LON should be provided", false, "invalid " + feedbackLocation.toString());
      feedbackLocation = LatLongPair.ZERO_ZERO;
      badLocationMessageIds.add(message.messageId);
    } else {
      sts.test("LAT/LON should be provided", true, null);
    }

    var explanations = sts.getExplanations();
    var feedback = "";
    var count = explanations.size();
    var label = count >= 10 ? "10 or more" : String.valueOf(count);
    getCounter("Feedback Count").increment(label);
    if (explanations.size() == 0) {
      ++ppMessageCorrectCount;
      feedback = "Perfect Message!";
    } else {
      feedback = String.join("\n", explanations);
    }

    var feedbackResult = new FeedbackResult(sender, feedbackLocation.getLatitude(), feedbackLocation.getLongitude(),
        explanations.size(), feedback);
    mIdFeedbackMap.put(message.messageId, new FeedbackMessage(feedbackResult, message));

    var outboundMessageFeedback = outboundMessagePrefixContent + feedback + outboundMessagePostfixContent;
    var outboundMessage = new OutboundMessage(outboundMessageSender, sender, //
        makeOutboundMessageSubject(message), outboundMessageFeedback, null);
    outboundMessageList.add(outboundMessage);
  }

  /**
   * this is the exercise-specific method to be implemented
   *
   * @param message
   */
  @Override
  protected abstract void specificProcessing(ExportedMessage message);

  @Override
  public void postProcess() {

    beforePostProcessing(messageType);

    if (doStsFieldValidation) {
      logger.info("field validation:\n" + sts.validate());
    }

    var sb = new StringBuilder();
    var N = ppCount;

    sb
        .append("\n\n" + cm.getAsString(Key.EXERCISE_DESCRIPTION) + " aggregate results for " + messageType.toString()
            + ":\n");
    sb.append("Participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));

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

    if (badLocationMessageIds.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationMessageIds.size() + " messages: "
              + String.join(",", badLocationMessageIds));
      var newLocations = LocationUtils.jitter(badLocationMessageIds.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationMessageIds.size(); ++i) {
        var messageId = badLocationMessageIds.get(i);
        var feedbackMessage = (FeedbackMessage) mIdFeedbackMap.get(messageId);
        var newLocation = newLocations.get(i);
        feedbackMessage.message().mapLocation = newLocation;
        var newFeedbackMessage = feedbackMessage.updateLocation(newLocation);
        mIdFeedbackMap.put(messageId, newFeedbackMessage);
      }
    }

    // feedback to folks who only send unexpected messages
    // we already have a separate acknowledgement outbound message, so we just need feedback
    var enableFeedbackForOnlyUnexpected = true;
    if (enableFeedbackForOnlyUnexpected) {
      @SuppressWarnings("unchecked")
      var ackTextMap = (Map<String, String>) (mm.getContextObject(AcknowledgementProcessor.ACK_TEXT_MAP));
      if (ackTextMap != null) {
        var allSenderSet = new HashSet<String>(ackTextMap.keySet());
        var expectedSenderList = outboundMessageList.stream().map(m -> m.to()).toList();
        allSenderSet.removeAll(expectedSenderList);
        var unexpectedSenderSet = allSenderSet;
        logger.info("Senders who only sent unexpected messages: " + String.join(",", unexpectedSenderSet));

        var subject = outboundMessageSubject;
        var commaIndex = outboundMessageSubject.indexOf(",");
        if (commaIndex >= 0) {
          subject = subject.substring(0, subject.indexOf(","));
        }

        for (var sender : unexpectedSenderSet) {
          var text = "no " + messageType.name() + " message received";
          var outboundMessage = new OutboundMessage(outboundMessageSender, sender, subject, text, null);
          outboundMessageList.add(outboundMessage);
        }
      }
    }

    var results = new ArrayList<>(mIdFeedbackMap.values());
    WriteProcessor.writeTable(results, Path.of(outputPathName, "feedback-" + messageType.toString() + ".csv"));

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm, mm, outboundMessageExtraContent);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }

    for (var key : counterMap.keySet()) {
      var summaryKey = messageType.name() + "_" + key;
      var value = counterMap.get(key);
      summaryCounterMap.put(summaryKey, value);
    }

    var chartService = ChartServiceFactory.getChartService(cm);
    chartService.initialize(cm, counterMap, messageType);
    chartService.makeCharts();

    // maps
    makeFeedbackMap();
    makeClearinghouseMap();

    var standardSummaries = mIdFeedbackMap
        .values()
          .stream()
          .map(s -> StandardSummary.fromSingleMessageFeedback(s))
          .toList();
    writeTable(dateString + "-standard-summary.csv", new ArrayList<IWritableTable>(standardSummaries));

    var db = new PersistenceManager(cm);
    var input = makeDbInput(cm, mIdFeedbackMap.values());
    var dbResult = db.bulkInsert(input);
    if (dbResult.status() == ReturnStatus.ERROR) {
      logger.error("### database update failed: " + dbResult.content());
    }
  }

  private void makeFeedbackMap() {
    var truncatedCountMap = new HashMap<Integer, Integer>(); // 6 -> 6 or more
    for (var summary : mIdFeedbackMap.values()) {
      var feedbackMessage = (FeedbackMessage) summary;
      var count = feedbackMessage.feedbackResult().feedbackCount();
      var key = Math.min(FEEDBACK_MAP_N_LAYERS - 1, count);
      var value = truncatedCountMap.getOrDefault(key, Integer.valueOf(0));
      ++value;
      truncatedCountMap.put(key, value);
    }

    var mapEntries = new ArrayList<MapEntry>(mIdFeedbackMap.values().size());
    for (var s : mIdFeedbackMap.values()) {
      var feedbackMessage = (FeedbackMessage) s;
      var m = feedbackMessage.message();
      var r = feedbackMessage.feedbackResult();
      var count = r.feedbackCount();

      final var lastColorMapIndex = gradientMap.size() - 1;
      final var lastColor = gradientMap.get(lastColorMapIndex);

      var location = m.mapLocation;
      var color = gradientMap.getOrDefault(count, lastColor);
      var prefix = "<b>" + m.from + "</b><hr>";
      var content = prefix + "Feedback Count: " + r.feedbackCount() + "\n" + "Feedback: " + r.feedback();
      var mapEntry = new MapEntry(m.from, m.to, location, content, color);
      mapEntries.add(mapEntry);
    }

    makeFeedbackMap(truncatedCountMap, mapEntries);
  }

  private void makeClearinghouseMap() {
    var organizationName = cm.getAsString(Key.EXERCISE_ORGANIZATION);
    if (organizationName == null || !organizationName.equals("ETO")) {
      logger.info("skipping clearinghouse map because not defined for org: " + organizationName);
    } else {
      var colorMap = MapService.etoColorMap;
      var clearinghouseNames = new ArrayList<String>(List.of(cm.getAsString(Key.EXPECTED_DESTINATIONS).split(",")));
      clearinghouseNames.add("unknown");
      var clearinghouseCountMap = new LinkedHashMap<String, Integer>();
      for (var s : mIdFeedbackMap.values()) {
        var feedbackMessage = (FeedbackMessage) s;
        var m = feedbackMessage.message();
        var to = m.to;
        if (!clearinghouseNames.contains(to)) {
          to = "unknown";
        }
        var count = clearinghouseCountMap.getOrDefault(to, Integer.valueOf(0));
        ++count;
        clearinghouseCountMap.put(to, count);
      }

      var mapEntries = new ArrayList<MapEntry>();
      for (var s : mIdFeedbackMap.values()) {
        var feedbackMessage = (FeedbackMessage) s;
        var m = feedbackMessage.message();
        var r = feedbackMessage.feedbackResult();
        var to = m.to;
        var clearinghouseName = clearinghouseNames.contains(to) ? to : "unknown";
        var location = m.mapLocation;
        var color = colorMap.get(clearinghouseName);
        var prefix = "<b>From: " + m.from + "<br>To: " + to + "</b><hr>";
        var messageId = feedbackMessage.message().messageId;
        var content = "MessageId: " + messageId + "\n" + "Feedback Count: " + r.feedbackCount() + "\n" + "Feedback: "
            + r.feedback();
        content = prefix + content;
        var mapEntry = new MapEntry(m.from, m.to, location, content, color);
        mapEntries.add(mapEntry);
      }

      makeClearinghouseMap(clearinghouseCountMap, colorMap, mapEntries);
    }
  }

  private BulkInsertEntry makeDbInput(IConfigurationManager cm, Collection<IWritableTable> values) {
    var exerciseDate = LocalDate.parse(cm.getAsString(Key.EXERCISE_DATE), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    var exerciseType = cm.getAsString(Key.PERSISTENCE_EXERCISE_TYPE, "Training");
    var exerciseName = cm.getAsString(Key.EXERCISE_NAME);
    var exerciseDescription = cm.getAsString(Key.EXERCISE_DESCRIPTION);
    Exercise exercise = new Exercise(-1, exerciseDate, exerciseType, exerciseName, exerciseDescription);
    List<Event> events = new ArrayList<>();
    for (var value : values) {
      var fm = (FeedbackMessage) value;
      var fr = fm.feedbackResult();
      var em = fm.message();
      var event = new Event(-1, -1, -1, fr.call(), new LatLongPair(fr.latitude(), fr.longitude()), //
          fr.feedbackCount(), fr.feedback(), "{\"messageId\":\"" + em.messageId + "\"}");
      events.add(event);
    }
    return new BulkInsertEntry(exercise, events);
  }

  protected void beforePostProcessing(MessageType messageType) {
    // we have sts, etc. for type
  }

  protected void endPostProcessingForAllMessageTypes() {
  }

}
