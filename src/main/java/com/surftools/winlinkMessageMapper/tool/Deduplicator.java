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

package com.surftools.winlinkMessageMapper.tool;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.GisMessage;
import com.surftools.winlinkMessageMapper.dto.IMessage;
import com.surftools.winlinkMessageMapper.dto.MessageType;
import com.surftools.winlinkMessageMapper.reject.MessagesRejectionsResult;
import com.surftools.winlinkMessageMapper.reject.RejectType;
import com.surftools.winlinkMessageMapper.reject.Rejection;

public class Deduplicator {
  private static final Logger logger = LoggerFactory.getLogger(Deduplicator.class);

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

  private static final int DEFAULT_THRESHOLD_DISTANCE_METERS = 100;
  private static final boolean DEFAULT_DEDUPLICATE_NONGIS_BY_CALL = false;

  private final int thresholdDistanceMeters;
  private final boolean deduplicateNonGisByCall;

  public Deduplicator() {
    this(DEFAULT_THRESHOLD_DISTANCE_METERS, DEFAULT_DEDUPLICATE_NONGIS_BY_CALL);
  }

  public Deduplicator(int thresholdDistanceMeters) {
    this(thresholdDistanceMeters, DEFAULT_DEDUPLICATE_NONGIS_BY_CALL);
  }

  public Deduplicator(int thresholdDistanceMeters, boolean deduplicateNonGisByCall) {
    this.thresholdDistanceMeters = thresholdDistanceMeters;
    this.deduplicateNonGisByCall = deduplicateNonGisByCall;
  }

  public MessagesRejectionsResult deduplicate(MessageType messageType, List<ExportedMessage> messages) {
    if (messageType.isGisType()) {
      return deduplicateGIS(messages);
    } else {
      if (deduplicateNonGisByCall) {
        return deduplicateNonGis(messages);
      } else {
        logger.info("processed: " + messages.size() + " messages, returning all messages");
        return new MessagesRejectionsResult(messages, new ArrayList<Rejection>());
      }
    }
  }

  private MessagesRejectionsResult deduplicateNonGis(List<ExportedMessage> inputMessages) {
    List<ExportedMessage> outputMessages = new ArrayList<ExportedMessage>(inputMessages.size());
    List<Rejection> rejections = new ArrayList<>();

    // call -> latest IMessage
    Map<String, ExportedMessage> map = new HashMap<>(inputMessages.size());

    for (IMessage iThisMessage : inputMessages) {
      ExportedMessage thisMessage = (ExportedMessage) iThisMessage;
      String call = thisMessage.from;
      ExportedMessage thatMessage = map.get(call);
      if (thatMessage == null) {
        map.put(call, thisMessage);
      } else {
        ZonedDateTime thisTime = makeDateTime(thisMessage);
        ZonedDateTime thatTime = makeDateTime(thatMessage);
        long seconds = ChronoUnit.SECONDS.between(thatTime, thisTime);
        if (thisTime.isAfter(thatTime)) {
          rejections.add(new Rejection(thatMessage, RejectType.SAME_CALL, thisMessage.messageId));
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
    logger.info("processed: " + inputMessages.size() + " messages, returning: " + outputMessages.size() + " messages");

    return new MessagesRejectionsResult(outputMessages, rejections);
  }

  private MessagesRejectionsResult deduplicateGIS(List<ExportedMessage> inputMessages) {
    if (thresholdDistanceMeters < 0) {
      logger
          .info("threshold negative: deduplication skipped: returning: " + inputMessages.size()
              + " messages, returning: " + inputMessages.size() + " messages");
      return new MessagesRejectionsResult(inputMessages, new ArrayList<Rejection>());
    }

    List<ExportedMessage> outputMessages = new ArrayList<ExportedMessage>(inputMessages.size());
    List<Rejection> rejections = new ArrayList<>();

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
            ZonedDateTime thisTime = makeDateTime(thisMessage);
            ZonedDateTime thatTime = makeDateTime(thatMessage);
            long seconds = ChronoUnit.SECONDS.between(thatTime, thisTime);
            if (thisTime.isAfter(thatTime)) {
              removeList.add(thatMessage);
              rejections.add(new Rejection(thatMessage, RejectType.SAME_LOCATION, thisMessage.messageId));
              addList.add(thisMessage);
              logger
                  .debug("call: " + call + ", messageId: " + thisMessage.messageId + " REPLACING messageId: "
                      + thatMessage.messageId + ", difference: " + seconds + " seconds, distance: " + distanceMeters);
            } else {
              logger
                  .debug("call: " + call + ", messageId: " + thisMessage.messageId + " keeping messageId: "
                      + thatMessage.messageId + ", difference: " + seconds + " seconds, distance: " + distanceMeters);
              rejections.add(new Rejection(thisMessage, RejectType.SAME_LOCATION, thatMessage.messageId));
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
    logger.info("processed: " + inputMessages.size() + " messages, returning: " + outputMessages.size() + " messages");

    return new MessagesRejectionsResult(outputMessages, rejections);
  }

  private ZonedDateTime makeDateTime(ExportedMessage message) {
    String dateString = message.date; // yyyy/mm/dd
    String timeString = message.time; // hh:mm
    ZoneId zoneId = ZoneId.of("UTC");

    LocalDate localDate = null;
    LocalTime localTime = null;
    boolean useFormatters = true;
    if (useFormatters) {
      localDate = LocalDate.parse(dateString, dateFormatter);
      localTime = LocalTime.parse(timeString, timeFormatter);
    } else {
      String[] dateFields = dateString.split("/");
      String[] timeFields = timeString.split(":");
      localDate = LocalDate
          .of(Integer.parseInt(dateFields[0]), Integer.parseInt(dateFields[1]), Integer.parseInt(dateFields[2]));
      localTime = LocalTime.of(Integer.parseInt(timeFields[0]), Integer.parseInt(timeFields[1]));
    }
    ZonedDateTime zdt = ZonedDateTime.of(localDate, localTime, zoneId);
    return zdt;
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
