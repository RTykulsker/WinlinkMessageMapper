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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;
import com.surftools.winlinkMessageMapper.dto.other.LatLongPair;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class Summarizer {
  private static final Logger logger = LoggerFactory.getLogger(Summarizer.class);

  private final String inputPathName;
  private final String outputPathName;

  private String exerciseDate = null;
  private String exerciseName = null;
  private String exerciseDescription = null;

  public Summarizer(String inputPathName, String outputPathName) {
    this.inputPathName = inputPathName;
    this.outputPathName = outputPathName;
  }

  public Summarizer(String inputPathName, String outputPathName, String exerciseDate, String exerciseName,
      String exerciseDescription) {
    this(inputPathName, outputPathName);
    this.exerciseDate = exerciseDate;
    this.exerciseName = exerciseName;
    this.exerciseDescription = exerciseDescription;
  }

  public void summarize(Map<MessageType, List<ExportedMessage>> messageMap) {
    var messages = flatten(messageMap);
    var exerciseMessageCounts = new MessageCounts();
    var exerciseRejectCounts = new RejectCounts();

    var totalMessageCount = messages.size();
    var dateCountMap = new HashMap<String, Integer>();
    var callPartipantSummaryMap = new HashMap<String, ParticipantSummary>();

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
    var exerciseSummary = new ExerciseSummary(exerciseDate, exerciseName, exerciseDescription, //
        totalMessageCount, callPartipantSummaryMap.keySet().size(), MessageType.values().length, exerciseMessageCounts,
        RejectType.values().length, exerciseRejectCounts);

    // fix up participantSummaries, now that exercise data and name are known
    var callParticipantHistoryMap = new HashMap<String, ParticipantHistory>();
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

    var summaryDao = new SummaryDao(inputPathName, outputPathName);
    summaryDao.persistExerciseSummary(exerciseSummary);
    summaryDao.persistParticipantSummary(callPartipantSummaryMap);
    summaryDao.persistParticipantHistory(callParticipantHistoryMap);
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
}
