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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.chart.ChartServiceFactory;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapHeader;
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

    var windowOpenString = cm.getAsString(Key.EXERCISE_WINDOW_OPEN);
    if (windowOpenString != null) {
      windowOpenDT = LocalDateTime.from(DTF.parse(windowOpenString));
    }

    var windowCloseString = cm.getAsString(Key.EXERCISE_WINDOW_CLOSE);
    if (windowCloseString != null) {
      windowCloseDT = LocalDateTime.from(DTF.parse(windowCloseString));
    }

    var secondaryDestinationsString = cm.getAsString(Key.SECONDARY_DESTINATIONS);
    if (secondaryDestinationsString != null) {
      var fields = secondaryDestinationsString.split(",");
      for (var field : fields) {
        secondaryDestinations.add(field.toUpperCase());
      }
    }
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
    getCounter("Feedback Count").increment(explanations.size());
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

    var mapEntries = mIdFeedbackMap.values().stream().map(s -> MapEntry.fromSingleMessageFeedback(s)).toList();
    var mapService = new MapService(null, null);
    mapService.makeMap(outputPath, new MapHeader(cm.getAsString(Key.EXERCISE_NAME), ""), mapEntries);
  }

  protected void beforePostProcessing(MessageType messageType) {
    // we have sts, etc. for type
  }

  protected void endPostProcessingForAllMessageTypes() {
  }

}
