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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RRIQuickWelfareMessage;
import com.surftools.wimp.message.RRIWelfareRadiogramMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-09-19: "awareness" exercise for both RRI Welfare Radiogram and RRI Quick Welfare
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_09_19 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_09_19.class);

  private class Summary implements IWritableTable {
    public String sender;
    public LatLongPair location;

    public List<String> explanations;

    public int rriQuickCount; // number of rri_quick_welfare messages received (0 or 1)
    public int rriRadiogramCount; // number of rri_welfare_radiogram messages received (0 or 1)

    public Summary(String sender) {
      this.sender = sender;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Summary) other;
      return sender.compareTo(o.sender);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "Latitude", "Longitude", "Feedback Count", "Feedback", //
          "Quick Count", "Radiogram Count" };
    }

    @Override
    public String[] getValues() {
      var latitude = (location != null && location.isValid()) ? location.getLatitude() : "0.0";
      var longitude = (location != null && location.isValid()) ? location.getLongitude() : "0.0";

      return new String[] { sender, latitude, longitude, String.valueOf(explanations.size()),
          String.join("\n", explanations), //
          String.valueOf(rriQuickCount), String.valueOf(rriRadiogramCount) };
    }
  }

  private Map<String, Summary> summaryMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    acceptableMessageTypesSet = Set.of(MessageType.RRI_QUICK_WELFARE, MessageType.RRI_WELFARE_RADIOGRAM);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var type = message.getMessageType();
    if (type == MessageType.RRI_QUICK_WELFARE) {
      handle_RriQuickWelfareMessage((RRIQuickWelfareMessage) message);
    } else if (type == MessageType.RRI_WELFARE_RADIOGRAM) {
      handle_RriRadiogramMessage((RRIWelfareRadiogramMessage) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }
  }

  private void handle_RriRadiogramMessage(RRIWelfareRadiogramMessage m) {
    var summary = summaryMap.getOrDefault(m.from, new Summary(m.from));
    ++summary.rriRadiogramCount;

    // radiogram processing
    count(sts.test("Message Precedence should be TEST W", m.header.contains("TEST W")));

    var bodyOneLine = m.body.replaceAll("\n", " ").replaceAll("\r", " ");

    var ev = "EVACUATING TO A FAMILY MEMBER/FRIENDS HOUSE";
    var predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));

    ev = "WILL CONTACT YOU WHEN ABLE";
    predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));

    // last valid location wins
    summary.location = m.mapLocation.isValid() ? m.mapLocation : LatLongPair.INVALID;

    summary.explanations.addAll(sts.getExplanations());

    summaryMap.put(m.from, summary);
  }

  private void handle_RriQuickWelfareMessage(RRIQuickWelfareMessage m) {
    var summary = summaryMap.getOrDefault(m.from, new Summary(m.from));
    ++summary.rriQuickCount;

    // quick processing
    var bodyOneLine = m.text.replaceAll("\n", "").replaceAll("\r", "");
    var ev = "I am safe and well.";
    var predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));

    ev = "All communications are down.";
    predicate = bodyOneLine.contains(ev);
    count(sts.test("Message Body should be contain " + ev, predicate));

    // last valid location wins
    summary.location = m.mapLocation.isValid() ? m.mapLocation : LatLongPair.INVALID;

    summary.explanations.addAll(sts.getExplanations());

    summaryMap.put(m.from, summary);
  }

  @Override
  protected void beforeCommonProcessing(String sender, ExportedMessage message) {
    var messageType = message.getMessageType();
    if (messageType == MessageType.RRI_QUICK_WELFARE) {
      sts.setExplanationPrefix("Quick: ");
    } else if (messageType == MessageType.RRI_WELFARE_RADIOGRAM) {
      sts.setExplanationPrefix("Radiogram: ");
    }
  }

  @Override
  protected void endProcessingForSender(String sender) {
    var summary = summaryMap.get(sender);

    if (summary.rriQuickCount == 0) {
      summary.explanations.add("No RRI Quick Welfare message received.");
    }

    if (summary.rriRadiogramCount == 0) {
      summary.explanations.add("No RRI Welfare Radiogram message received.");
    }

    summaryMap.put(sender, summary);
  }

  @Override
  public void postProcess() {
    // don't do any outbound messaging for individual messageTypes
    var cachedOutboundMessaging = doOutboundMessaging;
    doOutboundMessaging = false;
    outboundMessageList.clear();

    super.postProcess();

    // fix bad locations
    var badLocationSenders = new ArrayList<String>();
    var summaries = summaryMap.values();
    for (var summary : summaries) {
      if (!summary.location.isValid()) {
        badLocationSenders.add(summary.sender);
      }
    }

    if (badLocationSenders.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationSenders.size() + " summaries: "
              + String.join(",", badLocationSenders));
      var newLocations = LocationUtils.jitter(badLocationSenders.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationSenders.size(); ++i) {
        var sender = badLocationSenders.get(i);
        var summary = summaryMap.get(sender);
        summary.location = newLocations.get(i);
        summaryMap.put(sender, summary);
      }
    }

    // write outbound messages, but only for summary; change subject
    if (cachedOutboundMessaging) {
      setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
      for (var summary : summaryMap.values()) {
        var outboundMessageFeedback = (summary.explanations.size() == 0) ? "Perfect message!"
            : String.join("\n", summary.explanations) + OB_DISCLAIMER;
        var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
            "Feedback on ETO " + cm.getAsString(Key.EXERCISE_DATE) + " exercise", //
            outboundMessageFeedback, null);
        outboundMessageList.add(outboundMessage);

        var service = new OutboundMessageService(cm);
        outboundMessageList = service.sendAll(outboundMessageList);
        writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
      }
    }

    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(summaryMap.values()), Path.of(outputPathName, "rri-summary.csv"));
  }
}
