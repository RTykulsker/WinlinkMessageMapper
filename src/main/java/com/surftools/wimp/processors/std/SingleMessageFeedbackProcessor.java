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

package com.surftools.wimp.processors.std;

import static java.time.temporal.ChronoUnit.DAYS;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import com.surftools.wimp.feedback.FeedbackMessage;
import com.surftools.wimp.feedback.FeedbackResult;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.service.chart.AbstractBaseChartService;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.service.simpleTestService.TestResult;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * the FeedbackProcessor was once simple and clean. Then I added multi-message support to it and it became messy. I then
 * created the MultiMessageFeedbackProcessor and this, SingleMessageFeedbackProcessor is an attempt to restore
 * simplicity
 *
 * support multiple message types
 */
public abstract class SingleMessageFeedbackProcessor extends AbstractBaseProcessor {
  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter ALT_DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING.replaceAll("-", "/"));
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static Logger logger;

  protected LocalDateTime windowOpenDT = null;
  protected LocalDateTime windowCloseDT = null;

  protected boolean doStsFieldValidation = true;

  protected List<String> excludedPieChartCounterLabels = new ArrayList<>();
  protected Map<String, Counter> summaryCounterMap = new LinkedHashMap<String, Counter>();

  protected int ppCount = 0;
  protected int ppMessageCorrectCount = 0;
  protected Counter ppFeedBackCounter = new Counter();

  protected Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

  protected LatLongPair feedbackLocation = null;
  protected Map<String, IWritableTable> mIdFeedbackMap = new HashMap<String, IWritableTable>();
  protected List<String> badLocationMessageIds = new ArrayList<String>();

  public LocalDateTime windowOpenDt;
  public LocalDateTime windowCloseDt;

  protected String extraOutboundMessageText = "";
  protected MessageType messageType;
  protected Set<MessageType> acceptableMessageTypesSet = new LinkedHashSet<>(); // order matters
  protected SimpleTestService sts = new SimpleTestService();
  protected ExportedMessage message;
  protected String sender;
  protected Set<MessageType> messageTypesRequiringSecondaryAddress = new HashSet<>();
  protected Set<String> secondaryDestinations = new LinkedHashSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);
    logger = _logger;

    var acceptableMessageTypesString = cm.getAsString(Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES);
    if (acceptableMessageTypesString != null) {
      var typeNames = acceptableMessageTypesString.split(",");
      if (typeNames.length != 1) {
        throw new RuntimeException("Configuration Key " + Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES.name()
            + " must specify exactly one messageType, not " + acceptableMessageTypesString);
      }
      for (var typeName : typeNames) {
        messageType = MessageType.fromString(typeName);
        if (messageType != null) {
          acceptableMessageTypesSet.add(messageType);
          logger.info("will accept " + messageType.toString() + " messageTypes");
        } else {
          throw new RuntimeException("No MessageType for: " + messageType + ", in "
              + Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES.toString() + ": " + acceptableMessageTypesString);
        }
      }
    } else {
      throw new RuntimeException("Configuration Key " + Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES.name() + " not set");
    }
    logger.info("will accept " + messageType.toString() + " messages");

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
      if (acceptableMessageTypesSet.size() > 0) {
        // process all messages for a type, in chronological order, in type order
        var map = mm.getMessagesForSender(sender);
        for (var messageType : acceptableMessageTypesSet) {
          var typedMessages = map.get(messageType);
          if (typedMessages == null || typedMessages.size() == 0) {
            continue;
          }
          for (var message : typedMessages) {
            beginCommonProcessing(message);
            specificProcessing(message);
            endCommonProcessing(message);
          } // end processing for a message
        } // end processing for a messageType
      }
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
  protected void endProcessingForSender(String sender) {
  }

  protected void beginCommonProcessing(ExportedMessage message) {
    var sender = message.from;

    if (dumpIds.contains(sender)) {
      logger.info("dump: " + sender);
    }

    sts.reset(sender);

    beforeCommonProcessing(sender, message);

    ++ppCount;

    if (secondaryDestinations.size() > 0) {
      if (messageTypesRequiringSecondaryAddress.size() == 0
          || messageTypesRequiringSecondaryAddress.contains(message.getMessageType())) {
        var addressList = (message.toList + "," + message.ccList).toUpperCase();
        for (var ev : secondaryDestinations) {
          count(sts.test("To and/or CC addresses should contain " + ev, addressList.contains(ev)));
        }
      }
    }

    sts.testOnOrAfter("Message should be posted on or after #EV", windowOpenDT, message.msgDateTime, DTF);
    sts.testOnOrBefore("Message should be posted on or before #EV", windowCloseDT, message.msgDateTime, DTF);

    feedbackLocation = message.msgLocation;
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

    var daysAfterOpen = DAYS.between(windowOpenDT, message.msgDateTime);
    getCounter("Message sent days after window opens").increment(daysAfterOpen);
  }

  protected void endCommonProcessing(ExportedMessage message) {
    var explanations = sts.getExplanations();
    var feedback = "";
    ppFeedBackCounter.increment(explanations.size());
    if (explanations.size() == 0) {
      ++ppMessageCorrectCount;
      feedback = "Perfect Message!";
    } else {
      feedback = String.join("\n", explanations);
    }

    var feedbackResult = new FeedbackResult(sender, feedbackLocation.getLatitude(), feedbackLocation.getLongitude(),
        explanations.size(), feedback);
    mIdFeedbackMap.put(message.messageId, new FeedbackMessage(feedbackResult, message));

    var outboundMessageFeedback = feedback + extraOutboundMessageText;
    var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
        outboundMessageSubject + " " + message.messageId, outboundMessageFeedback, null);
    outboundMessageList.add(outboundMessage);
  }

  /**
   * get Counter for label, create in current te if needed
   *
   * @param label
   * @return
   */
  protected Counter getCounter(String label) {
    var counter = counterMap.get(label);
    if (counter == null) {
      counter = new Counter(label);
      counterMap.put(label, counter);
    }

    return counter;
  }

  /**
   * get a TestResult!
   *
   * @param testResult
   */
  protected void count(TestResult testResult) {
    var label = testResult.key();
    var result = testResult.ok();
    getCounter(label).increment(result ? "correct" : "incorrect");
  }

  /**
   * this is the exercise-specific method to be implemented
   *
   * @param message
   */
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
        var newFeedbackMessage = feedbackMessage.updateLocation(newLocation);
        mIdFeedbackMap.put(messageId, newFeedbackMessage);
      }
    }

    var results = new ArrayList<>(mIdFeedbackMap.values());
    WriteProcessor.writeTable(results, Path.of(outputPathName, "feedback-" + messageType.toString() + ".csv"));

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }

    for (var key : counterMap.keySet()) {
      var summaryKey = messageType.name() + "_" + key;
      var value = counterMap.get(key);
      summaryCounterMap.put(summaryKey, value);
    }

    var chartService = AbstractBaseChartService.getChartService(cm, counterMap, messageType);
    chartService.makeCharts();
  }

  protected void beforePostProcessing(MessageType messageType) {
    // we have sts, etc. for type
  }

  protected void endPostProcessingForAllMessageTypes() {
  }

  protected boolean isNull(String s) {
    return s == null || s.isEmpty();
  }

  protected LocalDateTime parse(String s) {
    LocalDateTime dt = null;
    if (s == null) {
      throw new IllegalArgumentException("null input string");
    }

    try {
      dt = LocalDateTime.from(DTF.parse(s.trim()));
    } catch (Exception e) {
      dt = LocalDateTime.from(ALT_DTF.parse(s.trim()));
    }

    return dt;
  }

}
