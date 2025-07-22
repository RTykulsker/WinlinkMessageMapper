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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor to send Acknowledgement messages via Winlink and map to every message sender
 *
 *
 * @author bobt
 *
 */
public class AcknowledgementProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AcknowledgementProcessor.class);

  private static final boolean LAST_LOCATION_WINS = true;

  public static final String ACK_MAP = "ackMap";
  public static final String ACK_TEXT_MAP = "ackTextMap";

  private Set<MessageType> expectedMessageTypes;
  private Map<String, AckEntry> ackMap;
  private Map<String, String> ackTextMap;
  private List<String> badLocationSenders;
  private AckSpecification requiredSpecification;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var ackSpecString = cm.getAsString(Key.ACKNOWLEDGEMENT_SPECIFICATION, "all");
    requiredSpecification = AckSpecification.fromString(ackSpecString);
    if (requiredSpecification == null) {
      throw new RuntimeException("No AcknowledgementSpecification found for: " + ackSpecString);
    }

    badLocationSenders = new ArrayList<>();
    expectedMessageTypes = getExpectedMessageTypes();
    ackMap = new HashMap<>();
    ackTextMap = new HashMap<>();
    mm.putContextObject(ACK_TEXT_MAP, ackTextMap);
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) { // loop over senders
      var sender = senderIterator.next();
      var ackEntry = new AckEntry(sender);
      for (var m : mm.getAllMessagesForSender(sender)) {
        ackEntry.update(m);
      } // end loop over messages for sender
      ackMap.put(ackEntry.from, ackEntry);
      if (ackEntry.location == null || !ackEntry.location.isValid()) {
        badLocationSenders.add(ackEntry.from);
      }
    } // end loop over senders

    makeText();
  }

  private void makeText() {
    final var EXPECTED_CONTENT = "Feedback messages and maps for expected message types will be generated and published shortly.\n";
    final var UNEXPECTED_CONTENT = "No feedback can or will be produced for unexpected message types.\n";
    var expectedContent = cm.getAsString(Key.ACKNOWLEDGEMENT_EXPECTED, EXPECTED_CONTENT);
    var unexpectedContent = cm.getAsString(Key.ACKNOWLEDGEMENT_UNEXPECTED, UNEXPECTED_CONTENT);
    var extraContent = cm.getAsString(Key.ACKNOWLEDGEMENT_EXTRA_CONTENT, "");

    var acknowledgments = new ArrayList<AckEntry>(ackMap.values().stream().filter(s -> isSelected(s)).toList());
    for (var ackEntry : acknowledgments) {
      var sb = new StringBuilder();
      if (ackEntry.expectedMessageMap.size() > 0) {
        sb.append("The following expected message types are acknowledged:\n");
        sb.append(ackEntry.format(true, 4));
        sb.append("\n");
        sb.append(expectedContent);
        sb.append("\n");
        sb.append("----------------------------------------------------------------------------------------------");
        sb.append("\n\n");
      }
      if (ackEntry.unexpectedMessageMap.size() > 0) {
        sb.append("The following unexpected message types are acknowledged:\n");
        sb.append(ackEntry.format(false, 4));
        sb.append("\n");
        sb.append(unexpectedContent);
      }
      sb.append(extraContent);
      var text = sb.toString();
      ackTextMap.put(ackEntry.from, text);
    }
    mm.putContextObject(ACK_TEXT_MAP, ackTextMap);
  }

  @Override
  public void postProcess() {
    if (badLocationSenders.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationSenders.size() + " messages from: "
              + String.join(",", badLocationSenders));
      var newLocations = LocationUtils.jitter(badLocationSenders.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationSenders.size(); ++i) {
        var from = badLocationSenders.get(i);
        var ackEntry = ackMap.get(from);
        var newLocation = newLocations.get(i);
        ackEntry.update(newLocation);
        ackMap.put(from, ackEntry);
      }
    }
    mm.putContextObject("ackMap", ackMap);

    var acknowledgments = new ArrayList<AckEntry>(ackMap.values().stream().filter(s -> isSelected(s)).toList());
    var selectedAckMap = acknowledgments.stream().collect(Collectors.toMap(AckEntry::getId, item -> item));
    mm.putContextObject("selectedAckMap", selectedAckMap);

    Collections.sort(acknowledgments);
    writeTable("acknowledgments.csv", acknowledgments);

    // must defer until post-processing to fix bad locations, etc.
    if (doOutboundMessaging) {
      var outboundAcknowledgementList = new ArrayList<OutboundMessage>();
      var subject = "Message acknowledement";
      var exerciseName = cm.getAsString(Key.EXERCISE_NAME);
      var exerciseDescription = cm.getAsString(Key.EXERCISE_DESCRIPTION);
      if (exerciseName != null && exerciseDescription != null) {
        subject = subject + " for " + exerciseName + ", " + exerciseDescription;
      }
      for (var ackEntry : acknowledgments) {
        var text = ackTextMap.get(ackEntry.from);
        var outboundMessage = new OutboundMessage(outboundMessageSender, ackEntry.from, subject, text, null);
        outboundAcknowledgementList.add(outboundMessage);
      }

      var fileName = "acknowledgment-winlinkExpressOutboundMessages.xml";
      var service = (OutboundMessageService) null;
      service = new OutboundMessageService(cm, fileName);
      service.sendAll(outboundAcknowledgementList);
    }
  }

  private boolean isSelected(AckEntry e) {
    switch (requiredSpecification) {
    case ALL:
      return true;
    case BOTH:
      return e.expectedMessageMap.size() > 0 && e.unexpectedMessageMap.size() > 0;
    case ONLY_EXPECTED:
      return e.expectedMessageMap.size() > 0 && e.unexpectedMessageMap.size() == 0;
    case ONLY_UNEXPECTED:
      return e.expectedMessageMap.size() == 0 && e.unexpectedMessageMap.size() > 0;
    case NOT_ONLY_EXPECTED:
      return e.unexpectedMessageMap.size() > 0;
    case NOT_ONLY_UNEXPECTED:
      return e.expectedMessageMap.size() > 0;
    }
    return false;
  }

  enum AckSpecification {
    ALL("all"), //
    ONLY_EXPECTED("onlyExpected"), //
    ONLY_UNEXPECTED("onlyUnexpected"), //
    BOTH("both"), // must contain both
    NOT_ONLY_EXPECTED("notOnlyExpected"), // must contain some unexpected
    NOT_ONLY_UNEXPECTED("notOnlyUnexpected"), // must contain some expected
    ;

    private final String key;

    private AckSpecification(String key) {
      this.key = key;
    }

    public static AckSpecification fromString(String string) {
      for (AckSpecification key : AckSpecification.values()) {
        if (key.toString().equals(string)) {
          return key;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return key;
    }
  };

  record AckKey(String from, String messageId, MessageType messageType) {
  };

  class AckEntry implements IWritableTable {
    public final String from;
    public LatLongPair location;
    public Map<AckKey, ExportedMessage> expectedMessageMap;
    public Map<AckKey, ExportedMessage> unexpectedMessageMap;

    public AckEntry(String sender) {
      this.from = sender;

      expectedMessageMap = new HashMap<>();
      unexpectedMessageMap = new HashMap<>();
    }

    public String getId() {
      return from;
    }

    public void update(ExportedMessage m) {
      // de-duplicate identical messages, support multiple messages of same type
      var ackKey = new AckKey(m.from, m.messageId, m.getMessageType());
      var isExpected = expectedMessageTypes.contains(m.getMessageType());
      if (isExpected) {
        expectedMessageMap.put(ackKey, m);
      } else {
        unexpectedMessageMap.put(ackKey, m);
      }

      if (LAST_LOCATION_WINS) {
        this.location = m.mapLocation;
      } else {
        if (this.location == null) {
          this.location = m.mapLocation;
        }
      }
    }

    public void update(LatLongPair newLocation) {
      this.location = newLocation;
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (AckEntry) other;
      return from.compareTo(o.from);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "Latitude", "Longitude", //
          "Status", "Expected Messages", "Unexpected Messages", //
          "Expected Message Count", "Unexpected Message Count", "Total Message Count" };
    }

    @Override
    public String[] getValues() {
      return new String[] { from, location.getLatitude(), location.getLongitude(), //
          getStatusString(), format(true, 3), format(false, 3), //
          s(expectedMessageMap.size()), s(unexpectedMessageMap.size()),
          s(expectedMessageMap.size() + unexpectedMessageMap.size()) };
    }

    private String format(boolean useExpected, int formatStyle) {
      final Map<Integer, String> formatMap = Map
          .of(1, "%s,%s,%s", 2, "%s %s %s", //
              3, "Date: %s\nMessageId: %s\nType: %s\n", 4, "Date: %s, MessageId: %s, Type: %s");

      var formatString = formatMap.get(formatStyle);
      var map = (useExpected) ? expectedMessageMap : unexpectedMessageMap;
      var values = new ArrayList<ExportedMessage>(map.values());
      Collections.sort(values); // by sort time!
      var resultList = new ArrayList<String>();
      for (var m : values) {
        var aResult = String
            .format(formatString, DTF.format(m.msgDateTime), m.messageId, m.getMessageType().toString());
        resultList.add(aResult);
      }
      var results = String.join("\n", resultList);
      return results;
    }

    private String getStatusString() {
      if (expectedMessageMap.size() > 0 && unexpectedMessageMap.size() == 0) {
        return "only expected messages types";
      } else if (expectedMessageMap.size() == 0 && unexpectedMessageMap.size() > 0) {
        return "only unexpected message types";
      } else if (expectedMessageMap.size() > 0 && unexpectedMessageMap.size() > 0) {
        return "mixed message types";
      } else {
        return "unknown";
      }
    }
  }

}
