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

package com.surftools.wimp.processors.std;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * more common processing, exercise processors must implement specificProcessing(...)
 *
 * support multiple message types
 */
public abstract class FeedbackProcessor extends AbstractBaseProcessor {
  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  protected static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter ALT_DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING.replaceAll("-", "/"));
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static Logger logger;

  protected LocalDateTime windowOpenDT = null;
  protected LocalDateTime windowCloseDT = null;

  /**
   * this is the context that we need for each message type, but we won't share it with subtypes
   */
  private static class TypeEntry {
    public SimpleTestService sts = new SimpleTestService();

    public int ppCount = 0;
    public int ppMessageCorrectCount = 0;
    public Counter ppFeedBackCounter = new Counter();

    public Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

    public LatLongPair feedbackLocation = null;
    public Map<String, IWritableTable> mIdFeedbackMap = new HashMap<String, IWritableTable>();
    public List<String> badLocationMessageIds = new ArrayList<String>();

    public String extraOutboundMessageText;
  }

  private Map<MessageType, TypeEntry> typeEntryMap = new HashMap<>();
  private TypeEntry te;
  private Set<MessageType> acceptableMessageTypesSet = new HashSet<>();

  protected SimpleTestService sts = new SimpleTestService();
  protected ExportedMessage message;
  protected String sender;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);
    logger = _logger;

    var acceptableMessageTypesString = cm.getAsString(Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES);
    if (acceptableMessageTypesString != null) {
      var typeNames = acceptableMessageTypesString.split(",");
      for (var typeName : typeNames) {
        var messageType = MessageType.fromString(typeName);
        if (messageType != null) {
          acceptableMessageTypesSet.add(messageType);
          logger.info("will accept " + messageType.toString() + " messageTypes");
        } else {
          throw new RuntimeException("No MessageType for: " + messageType + ", in "
              + Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES.toString() + ": " + acceptableMessageTypesString);
        }
      }
    } else {
      logger.info("will accept *ALL* message types");
    }
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) {
      sender = senderIterator.next();
      for (var message : mm.getAllMessagesForSender(sender)) {
        if (acceptableMessageTypesSet.size() > 0) {
          if (!acceptableMessageTypesSet.contains(message.getMessageType())) {
            continue;
          }
        }
        beginCommonProcessing(message);
        specificProcessing(message);
        endCommonProcessing(message);
      }
    }
  }

  protected void beginCommonProcessing(ExportedMessage message) {
    var sender = message.from;

    if (dumpIds.contains(sender)) {
      logger.info("dump: " + sender);
    }

    te = typeEntryMap.getOrDefault(message.getMessageType(), new TypeEntry());
    sts = te.sts;
    sts.reset();
    ++te.ppCount;

    var addressList = message.toList + "," + message.ccList;
    sts.test("To and/or CC addresses should contain ETO-BK", addressList.toUpperCase().contains("ETO-BK"), null);

    windowOpenDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_OPEN)));
    sts.testOnOrAfter("Message should be sent on or after #EV", windowOpenDT, message.msgDateTime, DTF);

    windowCloseDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_CLOSE)));
    sts.testOnOrBefore("Message should be sent on or before #EV", windowCloseDT, message.msgDateTime, DTF);

    te.feedbackLocation = message.msgLocation;
    if (te.feedbackLocation == null || te.feedbackLocation.equals(LatLongPair.ZERO_ZERO)) {
      te.feedbackLocation = LatLongPair.ZERO_ZERO;
      te.badLocationMessageIds.add(message.messageId);
      sts.test("LAT/LON should be provided", false, "missing");
    } else if (!te.feedbackLocation.isValid()) {
      sts.test("LAT/LON should be provided", false, "invalid " + te.feedbackLocation.toString());
      te.feedbackLocation = LatLongPair.ZERO_ZERO;
      te.badLocationMessageIds.add(message.messageId);
    } else {
      sts.test("LAT/LON should be provided", true, null);
    }
  }

  protected void endCommonProcessing(ExportedMessage message) {
    var explanations = sts.getExplanations();
    var feedback = "";
    var feedbackCountString = String.valueOf(explanations.size());
    te.ppFeedBackCounter.increment(feedbackCountString);
    if (explanations.size() == 0) {
      ++te.ppMessageCorrectCount;
      feedback = "Perfect Message!";
    } else {
      feedback = String.join("\n", explanations);
    }

    var feedbackResult = new FeedbackResult(sender, te.feedbackLocation.getLatitude(),
        te.feedbackLocation.getLongitude(), feedback, feedbackCountString);
    te.mIdFeedbackMap.put(message.messageId, new FeedbackMessage(feedbackResult, message));

    var outboundMessageFeedback = feedback + te.extraOutboundMessageText;
    var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
        outboundMessageSubject + " " + message.messageId, outboundMessageFeedback, null);
    outboundMessageList.add(outboundMessage);

    typeEntryMap.put(message.getMessageType(), te);
  }

  /**
   * get Counter for label, create in current te if needed
   *
   * @param label
   * @return
   */
  protected Counter getCounter(String label) {
    var counter = te.counterMap.get(label);
    if (counter == null) {
      counter = new Counter();
      te.counterMap.put(label, counter);
    }

    return counter;
  }

  /**
   * this is the exercise-specific method to be implemented
   *
   * @param message
   */
  protected abstract void specificProcessing(ExportedMessage message);

  @Override
  public void postProcess() {
    for (var messageType : typeEntryMap.keySet()) {
      te = typeEntryMap.get(messageType);

      logger.info("field validation:\n" + sts.validate());

      var sb = new StringBuilder();
      var N = te.ppCount;

      sb
          .append("\n\n" + cm.getAsString(Key.EXERCISE_DESCRIPTION) + " aggregate results for " + messageType.toString()
              + ":\n");
      sb.append("Participants: " + N + "\n");
      sb.append(formatPP("Correct Messages", te.ppMessageCorrectCount, false, N));

      var it = sts.iterator();
      while (it.hasNext()) {
        var key = it.next();
        if (sts.hasContent(key)) {
          sb.append(sts.format(key));
        }
      }

      sb.append("\n-------------------Histograms---------------------\n");
      for (var counterLabel : te.counterMap.keySet()) {
        sb.append(formatCounter(counterLabel, te.counterMap.get(counterLabel)));
      }

      logger.info(sb.toString());

      if (te.badLocationMessageIds.size() > 0) {
        logger
            .info("adjusting lat/long for " + te.badLocationMessageIds.size() + " messages: "
                + String.join(",", te.badLocationMessageIds));
        var newLocations = LocationUtils.jitter(te.badLocationMessageIds.size(), LatLongPair.ZERO_ZERO, 10_000);
        for (int i = 0; i < te.badLocationMessageIds.size(); ++i) {
          var messageId = te.badLocationMessageIds.get(i);
          var feedbackMessage = (FeedbackMessage) te.mIdFeedbackMap.get(messageId);
          var newLocation = newLocations.get(i);
          var newFeedbackMessage = feedbackMessage.updateLocation(newLocation);
          te.mIdFeedbackMap.put(messageId, newFeedbackMessage);
        }
      }

      var results = new ArrayList<>(te.mIdFeedbackMap.values());
      WriteProcessor.writeTable(results, Path.of(outputPathName, "feedback-" + messageType.toString() + ".csv"));

      if (doOutboundMessaging) {
        var service = new OutboundMessageService(cm);
        outboundMessageList = service.sendAll(outboundMessageList);
        writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
      }
    } // end loop over message types
  }

  public void setExtraOutboundMessageText(String extraOutboundMessageText) {
    te.extraOutboundMessageText = extraOutboundMessageText;
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

  protected static final String OB_DISCLAIMER = """


      =====================================================================================================

      DISCLAIMER: This feedback is automatically generated and provided for your consideration only.
      It's not an evaluation of your individual performance. Differences in spelling or numbers will
      trigger this automated message (differences in capitalization and punctuation are ignored).
      You may think that some of our feedback is "nit picking" and that your responses would be understood
      by any reasonable person -- and you'd be correct! You're welcome to disagree with any or all of our
      feedback. You're also welcome to reply via Winlink to this message or send an email to
      ETO.Technical.Team@emcomm-training.groups.io. In any event, thank you for participating
      in this exercise. We look forward to seeing you at our next Winlink Thursday Exercise!
              """;

  protected static final String OB_NAG = "\n\n=======================================================================\n\n"
      + "ETO needs sponsors to be able to renew our groups.io subscription for 2024.\n"
      + "By sponsoring this group, you are helping pay the Groups.io hosting fees.\n"
      + "Here is the link to sponsor our group:  https://emcomm-training.groups.io/g/main/sponsor\n"
      + "Any amount you sponsor will be held by Groups.io and used to pay hosting fees as needed.\n"
      + "The minimum sponsorship is $5.00.\n" //
      + "Thank you for your support!\n";

  protected static final String OB_REQUEST_FEEDBACK = "\n\n=======================================================================\n\n"
      + "ETO would love to hear from you! Would you please take a few minutes to answer the following questions:\n\n" //
      + "1. Were the exercise instructions clear? If not, where did they need improvement?\n" //
      + "2. Did you find the exercise useful?\n" //
      + "3. Did you find the above feedback useful?\n" //
      + "4. What did you dislike about the exercise?\n" //
      + "5. Any additional comments?\n" //
      + "\nPlease reply to this Winlink message or to ETO.Technical.Team@EmComm-Training.groups.io. Thank you!";

}