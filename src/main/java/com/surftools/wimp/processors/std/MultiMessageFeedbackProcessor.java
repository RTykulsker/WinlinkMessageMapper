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
import java.util.ArrayList;
import java.util.HashMap;
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
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.service.chart.AbstractBaseChartService;
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
  protected class BaseSummary implements ISummary {
    public String from;
    public String to;
    public LatLongPair location;
    public List<String> explanations; // doesn't get published, but interpreted
    public static String perfectMessageText = "Perfect messages!";

    @Override
    public int compareTo(IWritableTable o) {
      var other = (BaseSummary) o;
      return from.compareTo(other.from);
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>(List.of("From", "To", "Latitude", "Longitude", "Feedback Count", "Feedback"));
      return list.toArray(new String[list.size()]);
    }

    @Override
    public String[] getValues() {
      var latitude = location == null ? "0.0" : location.getLatitude();
      var longitude = location == null ? "0.0" : location.getLongitude();
      var feedbackCount = "0";
      var feedback = perfectMessageText;

      if (explanations.size() > 0) {
        feedbackCount = String.valueOf(explanations.size());
        feedback = String.join("\n", explanations);
      }

      var nsTo = to == null ? "(null)" : to;

      var list = new ArrayList<String>(List.of(from, nsTo, latitude, longitude, feedbackCount, feedback));
      return list.toArray(new String[list.size()]);
    }
  }

  protected Map<String, BaseSummary> summaryMap = new HashMap<>();
  protected BaseSummary iSummary; // summary for current sender
  protected String messageId; // the mId of the current message
  protected MessageType messageType; // the messageType of the current message
  protected boolean doFirstIn = true; // fifo vs lifo for location, to fields

  public int ppMessageCount = 0;
  public int ppParticipantCount = 0;
  public int ppParticipantCorrectCount = 0;

  public Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

  private List<String> badLocationSenders = new ArrayList<>();

  protected String outboundMessageExtraContent = FeedbackProcessor.OB_DISCLAIMER;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);

    var acceptableMessageTypesString = cm.getAsString(Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES);
    if (acceptableMessageTypesString == null) {
      throw new RuntimeException(
          "Must specify " + Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES.toString() + " in configuration");
    }

    var typeNames = acceptableMessageTypesString.split(",");
    for (var typeName : typeNames) {
      var messageType = MessageType.fromString(typeName);
      if (messageType != null) {
        acceptableMessageTypesSet.add(messageType);
        logger.info("will accept " + messageType.toString() + " messageTypes");
      } else {
        throw new RuntimeException("No MessageType for: " + typeName + ", in "
            + Key.FEEDBACK_ACCEPTABLE_MESSAGE_TYPES.toString() + ": " + acceptableMessageTypesString);
      }
    }

  }

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
        for (var message : typedMessages) {
          beginCommonProcessing(message);
          specificProcessing(message);
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

    var explanationPrefix = message.getMessageType().toString() + " (" + message.messageId + "): ";
    sts.setExplanationPrefix(explanationPrefix);

    // first-in or last-in for to and location
    if (doFirstIn) {
      if (iSummary.to == null) {
        iSummary.to = message.to;
      }

      if (iSummary.location == null) {
        iSummary.location = message.mapLocation;
      }
    } else {
      if (message.to != null) {
        iSummary.to = message.to;
      }

      if (message.mapLocation != null && message.mapLocation.isValid()) {
        iSummary.location = message.mapLocation;
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

    if (doOutboundMessaging) {
      for (var summary : summaryMap.values()) {
        var outboundMessageFeedback = makeOutboundMessageFeedback(summary);
        var outboundMessage = new OutboundMessage(outboundMessageSender, summary.from,
            cm.getAsString(Key.OUTBOUND_MESSAGE_SUBJECT), outboundMessageFeedback, null);
        outboundMessageList.add(outboundMessage);
      }

      var service = new OutboundMessageService(cm, outboundMessageExtraContent);

      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }

    // all messageTypes in one chart page
    var chartService = AbstractBaseChartService.getChartService(cm, counterMap, null);
    chartService.makeCharts();
  }

}
