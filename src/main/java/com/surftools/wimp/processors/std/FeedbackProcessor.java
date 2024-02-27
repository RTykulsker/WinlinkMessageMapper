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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.feedback.FeedbackMessage;
import com.surftools.wimp.feedback.FeedbackResult;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;

/**
 * more common processing, exercise processors must implement specificProcessing(...)
 */
public abstract class FeedbackProcessor extends AbstractBaseProcessor {
  protected static Logger logger;

  protected LocalDateTime windowOpenDT = null;
  protected LocalDateTime windowCloseDT = null;

  protected SimpleTestService sts = new SimpleTestService();

  protected int ppCount = 0;
  protected int ppMessageCorrectCount = 0;
  protected Counter ppFeedBackCounter = new Counter();

  protected Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

  protected LatLongPair feedbackLocation = null;
  protected Map<String, IWritableTable> messageIdFeedbackMessageMap = new HashMap<String, IWritableTable>();
  protected List<String> badLocationMessageIds = new ArrayList<String>();

  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  protected static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected ExportedMessage message;
  protected String sender;

  protected String extraOutboundMessageText;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);
    logger = _logger;
    counterMap.put("Feedback Counts", ppFeedBackCounter);
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) {
      sender = senderIterator.next();
      for (var message : mm.getAllMessagesForSender(sender)) {
        beginCommonProcessing(message);
        specificProcessing(message);
        endCommonProcessing(message);
      }
    }
  }

  protected void beginCommonProcessing(ExportedMessage message) {
    var sender = message.from;
    sts.reset();

    if (dumpIds.contains(sender)) {
      logger.info("dump: " + sender);
    }

    ++ppCount;

    var addressList = message.toList + "," + message.ccList;
    sts.test("To and/or CC addresses should contain ETO-BK", addressList.toUpperCase().contains("ETO-BK"), null);

    windowOpenDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_OPEN)));
    sts.testOnOrAfter("Message should be sent on or after #EV", windowOpenDT, message.msgDateTime, DTF);

    windowCloseDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_CLOSE)));
    sts.testOnOrBefore("Message should be sent on or before #EV", windowCloseDT, message.msgDateTime, DTF);

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
  }

  protected void endCommonProcessing(ExportedMessage message) {
    var explanations = sts.getExplanations();
    var feedback = "";
    var feedbackCountString = String.valueOf(explanations.size());
    ppFeedBackCounter.increment(feedbackCountString);
    if (explanations.size() == 0) {
      ++ppMessageCorrectCount;
      feedback = "Perfect Message!";
    } else {
      feedback = String.join("\n", explanations);
    }

    var feedbackResult = new FeedbackResult(sender, feedbackLocation.getLatitude(), feedbackLocation.getLongitude(),
        feedback, feedbackCountString);
    messageIdFeedbackMessageMap.put(message.messageId, new FeedbackMessage(feedbackResult, message));

    var outboundMessageFeedback = feedback + extraOutboundMessageText;
    var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
        outboundMessageSubject + " " + message.messageId, outboundMessageFeedback, null);
    outboundMessageList.add(outboundMessage);
  }

  /**
   * this is the exercise-specific method to be implemented
   *
   * @param message
   */
  protected abstract void specificProcessing(ExportedMessage message);

  @Override
  public void postProcess() {
    logger.info("field validation:\n" + sts.validate());

    var sb = new StringBuilder();
    var N = ppCount;

    sb.append("\n\n" + cm.getAsString(Key.EXERCISE_DESCRIPTION) + " aggregate results:\n");
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
        var feedbackMessage = (FeedbackMessage) messageIdFeedbackMessageMap.get(messageId);
        var newLocation = newLocations.get(i);
        var newFeedbackMessage = feedbackMessage.updateLocation(newLocation);
        messageIdFeedbackMessageMap.put(messageId, newFeedbackMessage);
      }
    }

    var results = new ArrayList<>(messageIdFeedbackMessageMap.values());
    WriteProcessor.writeTable(results, Path.of(outputPathName, cm.getAsString(Key.FEEDBACK_PATH)));

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }
  }

  public void setExtraOutboundMessageText(String extraOutboundMessageText) {
    this.extraOutboundMessageText = extraOutboundMessageText;
  }

  protected boolean isNull(String s) {
    return s == null || s.isEmpty();
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
