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

package com.surftools.winlinkMessageMapper.processor.other;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;
import com.surftools.winlinkMessageMapper.dto.message.IMessage;
import com.surftools.winlinkMessageMapper.dto.message.RejectionMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class Deduplicator {
  private static final Logger logger = LoggerFactory.getLogger(Deduplicator.class);

  private static final int DEFAULT_THRESHOLD_DISTANCE_METERS = 100;
  private static final boolean DEFAULT_DEDUPLICATE_NONGIS_BY_CALL = false;

  private final int thresholdDistanceMeters;
  private final boolean deduplicateNonGisByCall;
  private final boolean doJitter;

  public Deduplicator() {
    this(DEFAULT_THRESHOLD_DISTANCE_METERS, DEFAULT_DEDUPLICATE_NONGIS_BY_CALL);
  }

  public Deduplicator(int thresholdDistanceMeters) {
    this(thresholdDistanceMeters, DEFAULT_DEDUPLICATE_NONGIS_BY_CALL);
  }

  public Deduplicator(int thresholdDistanceMeters, boolean deduplicateNonGisByCall) {
    this.thresholdDistanceMeters = thresholdDistanceMeters;
    this.deduplicateNonGisByCall = deduplicateNonGisByCall;
    this.doJitter = true;
  }

  public void deduplicate(Map<MessageType, List<ExportedMessage>> messageMap) {
    for (MessageType messageType : messageMap.keySet()) {
      if (messageType.isGisType()) {
        deduplicateGIS(messageType, messageMap);
      } else {
        if (deduplicateNonGisByCall) {
          deduplicateNonGis(messageType, messageMap);
        } else {
          logger.info("   type: " + messageType.toString() + ", returning all messages");
        }
      }
    }
  }

  private void deduplicateNonGis(MessageType messageType, Map<MessageType, List<ExportedMessage>> messageMap) {
    List<ExportedMessage> inputMessages = messageMap.get(messageType);
    if (inputMessages == null) {
      logger.debug("no messages of type: " + messageType);
      return;
    }

    if (messageType == MessageType.REJECTS) {
      logger.debug("no deduplication performed for type: " + messageType);
      return;
    }

    List<ExportedMessage> outputMessages = new ArrayList<ExportedMessage>(inputMessages.size());
    List<ExportedMessage> rejections = new ArrayList<>();

    // call -> latest IMessage
    Map<String, ExportedMessage> map = new HashMap<>(inputMessages.size());

    for (ExportedMessage thisMessage : inputMessages) {
      String call = thisMessage.from;
      ExportedMessage thatMessage = map.get(call);
      if (thatMessage == null) {
        map.put(call, thisMessage);
      } else {
        var thisTime = thisMessage.dateTime;
        var thatTime = thatMessage.dateTime;
        long seconds = ChronoUnit.SECONDS.between(thatTime, thisTime);
        if (thisTime.isAfter(thatTime)) {
          rejections.add(new RejectionMessage(thatMessage, RejectType.SAME_CALL, thisMessage.messageId));
          logger
              .debug("call: " + call + ", messageId: " + thisMessage.messageId + " REPLACING messageId: "
                  + thatMessage.messageId + ", difference: " + seconds + " seconds");
        } else {
          logger
              .debug("call: " + call + ", messageId: " + thisMessage.messageId + " keeping messageId: "
                  + thatMessage.messageId + ", difference: " + seconds + " seconds");
        }
      }
    } // end loop of input messages

    for (String call : map.keySet()) {
      ExportedMessage message = map.get(call);
      outputMessages.add(message);
    }

    // merge rejections
    var existingRejections = messageMap.getOrDefault(MessageType.REJECTS, new ArrayList<>());
    rejections.addAll(existingRejections);
    messageMap.put(MessageType.REJECTS, rejections);

    messageMap.put(messageType, outputMessages);

    logger
        .info("type: " + messageType + ", processed: " + inputMessages.size() + " messages, returning: "
            + outputMessages.size() + " messages");

    return;
  }

  private void jitter(MessageType messageType, Map<MessageType, List<ExportedMessage>> messageMap) {
    List<ExportedMessage> inputMessages = messageMap.get(messageType);
    if (inputMessages == null) {
      logger.debug("no messages of type: " + messageType);
      return;
    }

    if (messageType == MessageType.REJECTS) {
      logger.debug("no deduplication performed for type: " + messageType);
      return;
    }

    // make a list of messages for each call
    Map<String, List<ExportedMessage>> callMessageListMap = new HashMap<>();
    for (ExportedMessage message : inputMessages) {
      var call = message.from;
      var list = callMessageListMap.getOrDefault(call, new ArrayList<ExportedMessage>());
      list.add(message);
      callMessageListMap.put(call, list);
    }

    List<ExportedMessage> outputMessages = new ArrayList<ExportedMessage>(inputMessages.size());
    for (String call : callMessageListMap.keySet()) {
      var list = callMessageListMap.get(call);
      if (list.size() == 1) {
        outputMessages.addAll(list);
        continue;
      }
      var n = list.size();

      // we now have multiple messages for the same call
      var latSum = 0d;
      var lonSum = 0d;
      for (var message : list) {
        GisMessage m = (GisMessage) message;
        latSum += Double.parseDouble(m.latitude);
        lonSum += Double.parseDouble(m.longitude);
      }
      var latCenter = latSum / n;
      var lonCenter = lonSum / n;
      var radius = 0.00005d;

      for (var i = 0; i < n; ++i) {
        var m = (GisMessage) list.get(i);
        double theta = 360d * i / n;
        double radians = Math.toRadians(theta);
        double latNew = latCenter + (radius * Math.sin(radians));
        double lonNew = lonCenter + (radius * Math.cos(radians));
        m.latitude = String.valueOf(latNew);
        m.longitude = String.valueOf(lonNew);
      }
      outputMessages.addAll(list);
    }

    messageMap.put(messageType, outputMessages);

    logger
        .info("type: " + messageType + ", processed: " + inputMessages.size() + " messages, returning: "
            + outputMessages.size() + " messages");
  }

  private void deduplicateGIS(MessageType messageType, Map<MessageType, List<ExportedMessage>> messageMap) {
    if (thresholdDistanceMeters < 0) {
      if (doJitter) {
        jitter(messageType, messageMap);
      }
      logger.info("threshold negative: deduplication skipped");
      return;
    }

    List<ExportedMessage> inputMessages = messageMap.get(messageType);
    if (inputMessages == null) {
      logger.debug("no messages of type: " + messageType);
      return;
    }

    if (messageType == MessageType.REJECTS) {
      logger.debug("no deduplication performed for type: " + messageType);
      return;
    }

    List<ExportedMessage> outputMessages = new ArrayList<ExportedMessage>(inputMessages.size());
    List<ExportedMessage> rejections = new ArrayList<>();

    // call -> list of IMessage
    Map<String, List<ExportedMessage>> map = new HashMap<>(inputMessages.size());

    for (ExportedMessage iThisMessage : inputMessages) {
      GisMessage thisMessage = (GisMessage) iThisMessage;
      String call = thisMessage.from;
      List<ExportedMessage> mapList = map.get(call);
      List<ExportedMessage> removeList = new ArrayList<>();
      List<ExportedMessage> addList = new ArrayList<>();
      if (mapList == null) {
        mapList = new ArrayList<>();
        addList.add(thisMessage);
      } else {
        // call in map
        boolean anyCloseLocations = false;
        for (IMessage iThatMessage : mapList) {
          GisMessage thatMessage = (GisMessage) iThatMessage;
          int distanceMeters = computeDistanceMeters(thisMessage, thatMessage);
          if (distanceMeters <= thresholdDistanceMeters) {
            anyCloseLocations = true;
            // does this message occur after existing message?
            var thisTime = thisMessage.dateTime;
            var thatTime = thatMessage.dateTime;
            long seconds = ChronoUnit.SECONDS.between(thatTime, thisTime);
            if (thisTime.isAfter(thatTime)) {
              removeList.add(thatMessage);
              rejections.add(new RejectionMessage(thatMessage, RejectType.SAME_LOCATION, thisMessage.messageId));
              addList.add(thisMessage);
              logger
                  .debug("call: " + call + ", messageId: " + thisMessage.messageId + " REPLACING messageId: "
                      + thatMessage.messageId + ", difference: " + seconds + " seconds, distance: " + distanceMeters);
            } else {
              logger
                  .debug("call: " + call + ", messageId: " + thisMessage.messageId + " keeping messageId: "
                      + thatMessage.messageId + ", difference: " + seconds + " seconds, distance: " + distanceMeters);
              rejections.add(new RejectionMessage(thisMessage, RejectType.SAME_LOCATION, thatMessage.messageId));
            }
          } else {
            logger
                .debug("call: " + call + ", messageId: " + thisMessage.messageId + ", other messagId: "
                    + thatMessage.messageId + ", distance: " + distanceMeters + " meters away");
          } // end if distance threshold exceeded
          if (!anyCloseLocations) {
            // need to add
            addList.add(thisMessage);
            logger
                .debug("call: " + call + ", messageId: " + thisMessage.messageId + " not close to any points, adding");
          }
        } // end loop over list
      } // end if existing list

      if (addList.size() > 0) {
        mapList.addAll(addList);
        map.put(call, mapList);
      }

      if (removeList.size() > 0) {
        mapList.removeAll(removeList);
        map.put(call, mapList);
      }

    } // end loop of input messages

    for (Map.Entry<String, List<ExportedMessage>> entry : map.entrySet()) {
      String call = entry.getKey();
      List<ExportedMessage> list = entry.getValue();
      if (list.size() > 1) {
        logger.debug("adding " + list.size() + " entries for call: " + call);
      }
      outputMessages.addAll(list);
    }

    // merge rejections
    var existingRejections = messageMap.getOrDefault(MessageType.REJECTS, new ArrayList<>());
    rejections.addAll(existingRejections);
    messageMap.put(MessageType.REJECTS, rejections);

    messageMap.put(messageType, outputMessages);

    logger
        .info("type: " + messageType + ", processed: " + inputMessages.size() + " messages, returning: "
            + outputMessages.size() + " messages");

    return;
  }

  public static double haversin(double val) {
    return Math.pow(Math.sin(val / 2), 2);
  }

  public static double computeDistanceMeters(double startLat, double startLong, double endLat, double endLong) {
    final double EARTH_RADIUS_METERS = 6371_000;
    double dLat = Math.toRadians((endLat - startLat));
    double dLong = Math.toRadians((endLong - startLong));

    startLat = Math.toRadians(startLat);
    endLat = Math.toRadians(endLat);

    double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return EARTH_RADIUS_METERS * c; // <-- d
  }

  private int computeDistanceMeters(GisMessage from, GisMessage to) {
    try {
      int d = (int) Math
          .round(computeDistanceMeters( //
              Double.parseDouble(from.latitude), //
              Double.parseDouble(from.longitude), //
              Double.parseDouble(to.latitude), //
              Double.parseDouble(to.longitude)));
      return d;
    } catch (Exception e) {
      String prefix = "Call: " + from.from + ", from: " + from.messageId + ", to: " + to.messageId;
      logger.error(prefix + ", " + "could not compute distance: " + e.getLocalizedMessage());
      return 0;
    }
  }

}
