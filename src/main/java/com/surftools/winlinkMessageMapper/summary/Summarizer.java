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

package com.surftools.winlinkMessageMapper.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;
import com.surftools.winlinkMessageMapper.dto.other.LatLongPair;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class Summarizer {
  private static final Logger logger = LoggerFactory.getLogger(Summarizer.class);

  private String exerciseDate = null;
  private String exerciseName = null;
  private String exerciseDescription = null;

  private MessageType exerciseMessageType = null;
  private Map<String, ExportedMessage> exerciseCallMessageMap = new HashMap<>();

  private ExerciseSummary exerciseSummary;
  private List<ExerciseSummary> currentExerciseSummaryList;
  private List<ExerciseSummary> pastExerciseSummaryList;

  private HashMap<String, ParticipantSummary> callPartipantSummaryMap;
  private List<ParticipantSummary> currentParticipantSummaryList;
  private List<ParticipantSummary> pastParticipantSummaryList;

  private HashMap<String, ParticipantHistory> callParticipantHistoryMap;
  private List<ParticipantHistory> currentParticipantHistoryList;
  private List<ParticipantHistory> pastParticipantHistoryList;

  protected Set<String> dumpIds = new HashSet<>();

  private SummaryDao summaryDao;

  public Summarizer(String inputPathName, String outputPathName) {
    summaryDao = new SummaryDao(inputPathName, outputPathName);
  }

  public Summarizer(String inputPathName, String outputPathName, String exerciseDate, String exerciseName,
      String exerciseDescription) {
    this(inputPathName, outputPathName);
    this.exerciseDate = exerciseDate;
    this.exerciseName = exerciseName;
    this.exerciseDescription = exerciseDescription;
  }

  /**
   * public entry point
   *
   * @param messageMap
   */
  public void summarize(Map<MessageType, List<ExportedMessage>> messageMap) {

    // compute the ExerciseSummary, ParticipantSummary and ParticipantHistory just for the current messages
    computeCurrentValues(messageMap);

    // persist the current values; not much use for them, but maybe for debug
    persistCurrentValues();

    // read past values from database
    getPastValues();

    // update (append/merge) current with past
    updateCurrentWithPast();

    // persist updated values; after review, move to database
    persistUpdatedValues();
  }

  private void persistCurrentValues() {
    currentExerciseSummaryList = new ArrayList<>();
    currentExerciseSummaryList.add(exerciseSummary);
    summaryDao.writeExerciseSummary(currentExerciseSummaryList, true);

    currentParticipantSummaryList = new ArrayList<>();
    currentParticipantSummaryList.addAll(callPartipantSummaryMap.values());
    summaryDao.writeParticipantSummary(currentParticipantSummaryList, true);

    currentParticipantHistoryList = new ArrayList<>();
    currentParticipantHistoryList.addAll(callParticipantHistoryMap.values());
    summaryDao.writeParticipantHistory(currentParticipantHistoryList, true);
  }

  private void getPastValues() {
    pastExerciseSummaryList = summaryDao.readExerciseSummaries();
    pastParticipantSummaryList = summaryDao.readParticipantSummaries();
    pastParticipantHistoryList = summaryDao.readParticipantHistories();
  }

  private void updateCurrentWithPast() {
    // ExerciseSummary is a simple append
    currentExerciseSummaryList.addAll(pastExerciseSummaryList);

    // ParticipantSummary is append all
    currentParticipantSummaryList.addAll(pastParticipantSummaryList);

    // ParticipantHistory is a bit more involved :)
    var pastMap = new HashMap<String, ParticipantHistory>();
    for (ParticipantHistory ph : pastParticipantHistoryList) {
      pastMap.put(ph.getCall(), ph);
    }

    List<ParticipantHistory> mergedList = new ArrayList<>();
    for (ParticipantHistory current : currentParticipantHistoryList) {
      var call = current.getCall();
      var past = pastMap.get(call);
      if (past != null) {
        // any time is better than no time
        if (current.getLastDate() == null || current.getLastDate() == "") {
          current.setLastDate(past.getLastDate());
        }

        // any place is better than no place
        if (current.getLastLocation() == null || !current.getLastLocation().isValid()) {
          current.setLastLocation(past.getLastLocation());
        }

        current.setMessageCount(current.getMessageCount() + past.getMessageCount());
        current.setExerciseCount(current.getExerciseCount() + past.getExerciseCount());

        // remove the past value, so it won't be there when we addAll(), below ...
        pastMap.remove(call);
      }
      mergedList.add(current);
    }
    mergedList.addAll(pastMap.values());

    boolean doJitter = true;
    if (doJitter) {
      currentParticipantHistoryList = jitterParticipantHistory(mergedList);
    } else {

      // place the unmappables in a circle around "Zero Zero Island"
      var goodList = mergedList
          .stream()
            .filter(p -> p.getLastLocation() != null && p.getLastLocation().isValid())
            .collect(Collectors.toList());
      var badList = mergedList
          .stream()
            .filter(p -> p.getLastLocation() == null || !p.getLastLocation().isValid())
            .collect(Collectors.toList());
      var n = badList.size();

      for (int i = 0; i < n; ++i) {
        double theta = 360d * i / n;
        double radians = Math.toRadians(theta);
        double r = 1_000_000d;
        double x = Math.round(r * Math.cos(radians)) / r;
        double y = Math.round(r * Math.sin(radians)) / r;
        LatLongPair latLong = new LatLongPair(y, x);
        var ph = badList.get(i);
        ph.setMapLocation(latLong);
      }
      goodList.addAll(badList);
      currentParticipantHistoryList = goodList;
    }

    setCategory(mergedList, currentExerciseSummaryList.size(), exerciseDate);
  }

  private List<ParticipantHistory> jitterParticipantHistory(List<ParticipantHistory> inList) {
    var callHistoryMap = new HashMap<String, ParticipantHistory>();

    // make sure every call has a valid location
    for (ParticipantHistory ph : inList) {
      callHistoryMap.put(ph.getCall(), ph);
      if (ph.getLastLocation() == null || !ph.getLastLocation().isValid()) {
        ph.setLastLocation(new LatLongPair("0.0", "0.0"));
      }
    }

    // build map of call -> list of stations close to call
    final var distanceThresholdMeters = 10d;
    var callStationListMap = new HashMap<String, List<ParticipantHistory>>();
    for (ParticipantHistory ph : inList) {
      for (ParticipantHistory other : inList) {
        String call = ph.getCall();
        if (call.equals(other.getCall())) {
          continue;
        }
        double distanceMeters = ph.getLastLocation().computeDistanceMeters(other.getLastLocation());
        if (distanceMeters <= distanceThresholdMeters) {
          var list = callStationListMap.getOrDefault(call, new ArrayList<>());
          list.add(other);
          callStationListMap.put(call, list);
        } // end if within threshold
      } // end loop over others
    } // end loop over PH

    var nextIndex = -1;
    var neighborMap = new TreeMap<Integer, Set<ParticipantHistory>>();
    for (String call : callStationListMap.keySet()) {
      var ph = callHistoryMap.get(call);
      var stationList = callStationListMap.get(call);
      boolean isCallFound = false;
      for (Set<ParticipantHistory> set : neighborMap.values()) {
        if (set.contains(callHistoryMap.get(call))) {
          isCallFound = true;
          break;
        } // end if call is found in set
      } // end loop over all sets
      if (!isCallFound) {
        ++nextIndex;
        var set = new HashSet<ParticipantHistory>();
        set.add(ph);
        set.addAll(stationList);
        neighborMap.put(nextIndex, set);
      } // end if call not found
    } // end loop over calls

    // finally can jitter!
    for (Set<ParticipantHistory> set : neighborMap.values()) {
      jitter(set);
    }

    return inList;
  }

  private void jitter(Set<ParticipantHistory> set) {
    // find centroid;
    var latitude = 0d;
    var longitude = 0d;
    var n = set.size();
    for (ParticipantHistory ph : set) {
      var loc = ph.getLastLocation();
      latitude += loc.getLatitudeAsDouble();
      longitude += loc.getLongitudeAsDouble();
    }
    latitude = latitude / n;
    longitude = longitude / n;
    var centroid = new LatLongPair(latitude, longitude);
    var distanceFromZeroZeroIsland = centroid.computeDistanceMeters(new LatLongPair(0d, 0d));
    var isZeroZeroIsland = (distanceFromZeroZeroIsland <= 10);
    var radius = (isZeroZeroIsland) ? 1_000_000d : 0.0001d;

    var list = new ArrayList<ParticipantHistory>(set);
    Collections.sort(list, new ParticipantHistoryCallComparator());
    for (int i = 0; i < n; ++i) {
      ParticipantHistory ph = list.get(i);
      double theta = 360d * i / n;
      double radians = Math.toRadians(theta);

      LatLongPair mapLocation = null;
      if (isZeroZeroIsland) {
        double newLongitude = longitude + (Math.round(radius * Math.cos(radians)) / radius);
        double newLatitude = latitude + (Math.round(radius * Math.sin(radians)) / radius);
        mapLocation = new LatLongPair(newLatitude, newLongitude);
      } else {
        double newLongitude = longitude + (radius * Math.cos(radians));
        double newLatitude = latitude + (radius * Math.sin(radians));
        mapLocation = new LatLongPair(newLatitude, newLongitude);
      }

      ph.setMapLocation(mapLocation);
      double d = mapLocation.computeDistanceMeters(ph.getLastLocation());
      logger
          .debug("call: " + ph.getCall() + ", centroid: " + centroid + ", last: " + ph.getLastLocation() + ", map: "
              + ph.getMapLocation() + ", dist: " + d + " meters");
    }

  }

  private void persistUpdatedValues() {
    summaryDao.writeExerciseSummary(currentExerciseSummaryList, false);

    summaryDao.writeParticipantSummary(currentParticipantSummaryList, false);

    summaryDao.writeParticipantHistory(currentParticipantHistoryList, false);

    summaryDao.writeExerciseParticipantHistory(currentParticipantHistoryList, exerciseDate);

    summaryDao
        .writeFirstTimers(currentParticipantHistoryList, exerciseDate, exerciseMessageType, exerciseCallMessageMap);
  }

  private void computeCurrentValues(Map<MessageType, List<ExportedMessage>> messageMap) {
    var messages = flatten(messageMap);
    var exerciseMessageCounts = new MessageCounts();
    var exerciseRejectCounts = new RejectCounts();

    var totalMessageCount = messages.size();
    var dateCountMap = new HashMap<String, Integer>();
    callPartipantSummaryMap = new HashMap<String, ParticipantSummary>();

    for (ExportedMessage message : messages) {

      var date = message.date;
      var dateCount = dateCountMap.getOrDefault(date, Integer.valueOf(0));
      ++dateCount;
      dateCountMap.put(date, dateCount);

      var call = message.from;
      var participantSummary = callPartipantSummaryMap.getOrDefault(call, new ParticipantSummary(call));
      participantSummary.getMessageCounts().increment(message);
      participantSummary.getRejectCounts().increment(message);
      if (participantSummary.getLastLocation() == null) {
        if (message.getMessageType().isGisType()) {
          var gisMessage = (GisMessage) message;
          var lastLocation = new LatLongPair(gisMessage.latitude, gisMessage.longitude);
          participantSummary.setLastLocation(lastLocation);
        }
      }
      callPartipantSummaryMap.put(call, participantSummary);

      exerciseMessageCounts.increment(message);
      exerciseRejectCounts.increment(message);
    } // end loop over messages

    exerciseDate = makeExerciseDate(exerciseDate, dateCountMap);
    exerciseName = makeExerciseName(exerciseName, exerciseMessageCounts);
    exerciseDescription = makeExerciseDescription(exerciseDescription, exerciseDate, exerciseName);

    exerciseMessageType = MessageType.fromString(exerciseName);
    if (exerciseMessageType != null && exerciseMessageType != MessageType.UNKNOWN) {
      var list = messageMap.get(exerciseMessageType);
      for (var message : list) {
        exerciseCallMessageMap.put(message.from, message);
      }
    }

    exerciseSummary = new ExerciseSummary(exerciseDate, exerciseName, exerciseDescription, //
        totalMessageCount, callPartipantSummaryMap.keySet().size(), MessageType.values().length, exerciseMessageCounts,
        RejectType.values().length, exerciseRejectCounts);

    // fix up participantSummaries, now that exercise data and name are known
    callParticipantHistoryMap = new HashMap<String, ParticipantHistory>();
    for (ParticipantSummary ps : callPartipantSummaryMap.values()) {
      ps.setDate(exerciseDate);
      ps.setName(exerciseName);

      var call = ps.getCall();
      var ph = new ParticipantHistory(call);
      ph.setLastName(exerciseName);
      ph.setLastDate(exerciseDate);
      ph.setLastLocation(ps.getLastLocation());
      ph.setMessageCount(ps.getMessageCounts().getTotalCount());
      ph.setExerciseCount(1);
      callParticipantHistoryMap.put(call, ph);
    }

    boolean doMe = false;
    if (doMe) {
      var my = callPartipantSummaryMap.get("KM6SO");
      logger.info("my summary: " + my.toString());
      logger.info("my history: " + callParticipantHistoryMap.get("KM6SO"));
    }

    var summaryText = makeSummaryText(totalMessageCount, exerciseMessageCounts, exerciseRejectCounts);
    logger.info(summaryText);
  }

  /**
   * flatten the map into a list, sort by call, date/time in descending order so that we can pick up last location
   * easily
   *
   * @param messageMap
   * @return
   */
  private List<ExportedMessage> flatten(Map<MessageType, List<ExportedMessage>> messageMap) {
    var messages = new ArrayList<ExportedMessage>();
    for (MessageType messageType : messageMap.keySet()) {
      messages.addAll(messageMap.get(messageType));
    }

    Collections.sort(messages, new ExportedMessageComparator());
    return messages;
  }

  private String makeExerciseDescription(String exerciseDescription, String exerciseDate, String exerciseName) {
    if (exerciseDescription != null) {
      return exerciseDescription;
    }

    return "ETO WLT on " + exerciseDate + " for " + exerciseName;
  }

  private String makeExerciseName(String exerciseName, MessageCounts messageCounts) {
    if (exerciseName != null) {
      return exerciseName;
    }

    var countMap = messageCounts.getCountMap();
    int maxCount = -1;
    MessageType maxType = null;
    for (MessageType messageType : countMap.keySet()) {
      int count = countMap.get(messageType);
      if (count > maxCount) {
        maxCount = count;
        maxType = messageType;
      }
    }

    if (maxType == null) {
      maxType = MessageType.UNKNOWN;
    }

    return maxType.toString();
  }

  private String makeExerciseDate(String exerciseDate, HashMap<String, Integer> dateCountMap) {
    if (exerciseDate != null) {
      return exerciseDate;
    }

    int maxCount = -1;
    String maxDate = null;
    for (String date : dateCountMap.keySet()) {
      int count = dateCountMap.get(date);
      if (count > maxCount) {
        maxDate = date;
        maxCount = count;
      }
    }
    return maxDate;
  }

  private String makeSummaryText(int totalMessageCount, MessageCounts messageCounts, RejectCounts rejectCounts) {
    var sb = new StringBuilder();
    sb.append("\n");
    sb.append("summary: " + totalMessageCount + " messages received\n");
    sb.append("----------------------------------------\n");
    sb.append(messageCounts.getText());
    sb.append("----------------------------------------\n");
    sb.append(rejectCounts.getText());
    sb.append("----------------------------------------\n");
    var s = sb.toString();
    return s;
  }

  private void setCategory(List<ParticipantHistory> list, int totalExerciseCount, String exerciseDate) {
    for (ParticipantHistory ph : list) {
      if (dumpIds.contains(ph.getCall())) {
        logger.info("setCategory: " + ph);
      }

      var exerciseCount = ph.getExerciseCount();
      if (exerciseCount == 1) {
        if (ph.getLastDate().equals(exerciseDate)) {
          ph.setCategory(HistoryCategory.FIRST_TIME_LAST_TIME);
        } else {
          ph.setCategory(HistoryCategory.ONE_AND_DONE);
        }
        continue;
      }

      double percent = Math.round(100d * exerciseCount / totalExerciseCount);
      // if (percent >= 99d) {
      // ph.setCategory(HistoryCategory.HUNDRED_PERCENT);
      // continue;
      // } else
      if (percent >= 90d) {
        ph.setCategory(HistoryCategory.HEAVEY_HITTER);
        continue;
      } else if (percent >= 50d) {
        ph.setCategory(HistoryCategory.GOING_STRONG);
        continue;
      } else {
        ph.setCategory(HistoryCategory.NEEDS_ENCOURAGEMENT);
        continue;
      }
    } // end loop over ParticipantHistory

  }

  public void setDumpIds(Set<String> dumpIds) {
    this.dumpIds = dumpIds;
  }

  /**
   * ascending call, descending datetime
   *
   * @author bobt
   *
   */
  static class ExportedMessageComparator implements Comparator<ExportedMessage> {

    @Override
    public int compare(ExportedMessage o1, ExportedMessage o2) {
      int compare = o1.from.compareTo(o2.from);
      if (compare != 0) {
        return compare;
      }

      compare = o2.dateTime.compareTo(o1.dateTime);

      return compare;
    }
  }

  static class ParticipantHistoryCallComparator implements Comparator<ParticipantHistory> {

    @Override
    public int compare(ParticipantHistory o1, ParticipantHistory o2) {
      return o1.getCall().compareTo(o2.getCall());
    }

  }
}
