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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.RejectionMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class SummaryProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(SummaryProcessor.class);

  public record ExerciseSummary(String date, String name, String description, int totalMessages, int uniqueParticipants)
      implements IWritableTable {

    public static ExerciseSummary make(String[] fields) {
      return new ExerciseSummary(fields[0], fields[1], fields[2], parseInt(fields[3]), parseInt(fields[4]));
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Exercise Date", "Exercise Name", "Exercise Description", "Total Messages",
          "Unique Participants" };
    }

    @Override
    public String[] getValues() {
      return new String[] { date, name, description, valueOf(totalMessages), valueOf(uniqueParticipants) };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ExerciseSummary) other;
      var cmp = date.compareTo(o.date);
      if (cmp != 0) {
        return cmp;
      }
      return name.compareTo(o.name);
    }

  }

  public record ParticipantSummary(String date, String name, String call, String latitude, String longitude)
      implements IWritableTable {
    @Override
    public String[] getHeaders() {
      return new String[] { "Exercise Date", "Exercise Name", "Call", "Latitude", "Longitude" };
    }

    @Override
    public String[] getValues() {
      return new String[] { date, name, call, latitude, longitude };
    }

    public static ParticipantSummary make(String[] fields) {
      return new ParticipantSummary(fields[0], fields[1], fields[2], fields[3], fields[4]);
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ParticipantSummary) other;
      var cmp = date.compareTo(o.date);
      if (cmp != 0) {
        return cmp;
      }
      return name.compareTo(o.name);
    }

  }

  public record ParticipantHistory(String call, String date, String name, String latitude, String longitude,
      int exerciseCount, int messageCount, int categoryIndex) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Last Date", "Last Exercise", "Latitude", "Longitude", "Exercise Count",
          "Message Count", "Category Index", "Category Name" };
    }

    private static final String[] CATEGORY_NAMES = new String[] { "Undefined", "only once in the past",
        "1st time this exercise", "1% - 50%", "50% - 90%", "90% - 100%" };

    @Override
    public String[] getValues() {
      return new String[] { call, date, name, latitude, longitude, valueOf(exerciseCount), valueOf(messageCount),
          valueOf(categoryIndex), CATEGORY_NAMES[categoryIndex] };
    }

    public static ParticipantHistory make(String[] fields) {
      return new ParticipantHistory(fields[0], fields[1], fields[2], fields[3], fields[4], parseInt(fields[5]),
          parseInt(fields[6]), parseInt(fields[7]));
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ParticipantHistory) other;
      var cmp = date.compareTo(o.date);
      if (cmp != 0) {
        return cmp;
      }
      return name.compareTo(o.name);
    }
  }

  static enum CategoryIndex {
    UNDEFINED, ONE_AND_DONE, FIRST_TIME, NEEDS_ENCOURAGEMENT, GOING_STRING, HEAVY_HITTER
  };

  private Path inputDbPath;
  private Path outputDbPath;
  private String outputDbPathName;

  private String exerciseDate = null;
  private String exerciseName = null;
  private String exerciseDescription = null;

  private List<ExerciseSummary> oldExerciseSummaries;
  private List<ParticipantSummary> oldParticipantSummaries;
  private List<ParticipantHistory> oldParticipantHistories;
  private Map<String, ParticipantHistory> oldHistoryMap;
  private Set<String> allCallSet;
  private int totalExerciseCount;
  private boolean isHistoryEnabled = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var inputDbPathName = cm.getAsString(Key.DATABASE_PATH);
    isHistoryEnabled = !(inputDbPathName == null || inputDbPathName.isBlank());

    if (isHistoryEnabled) {
      inputDbPath = Path.of(inputDbPathName);
      if (!inputDbPath.toFile().exists()) {
        throw new RuntimeException("Input database directory: " + inputDbPath.toString() + " doesn't exist");
      }

      outputDbPath = Path.of(outputPathName, "database");
      if (!outputDbPath.toFile().exists()) {
        outputDbPath.toFile().mkdirs();
        if (!outputDbPath.toFile().exists()) {
          throw new RuntimeException("Output database directory: " + outputDbPath.toString() + " can't be created");
        }
      }
      outputDbPathName = outputDbPath.toString();

      exerciseDate = cm.getAsString(Key.EXERCISE_DATE);
      exerciseName = cm.getAsString(Key.EXERCISE_NAME);
      exerciseDescription = cm.getAsString(Key.EXERCISE_DESCRIPTION);

      oldExerciseSummaries = getExerciseSummaries(Path.of(inputDbPath.toString(), "exerciseSummary.csv"));
      oldParticipantSummaries = getParticipantSummaries(Path.of(inputDbPath.toString(), "participantSummary.csv"));
      oldParticipantHistories = getParticipantHistories(Path.of(inputDbPath.toString(), "participantHistory.csv"));
      totalExerciseCount = oldExerciseSummaries.size() + 1;

      allCallSet = new HashSet<>();
      oldHistoryMap = new HashMap<>();
      for (var ph : oldParticipantHistories) {
        oldHistoryMap.put(ph.call, ph);
        allCallSet.add(ph.call);
      }

    } // end if isHistoryEnabled
  }

  @Override
  public void process() {
    Counter participantCounter = new Counter();
    Counter messageTypeCounter = new Counter();
    Counter rejectCounter = new Counter();

    if (isHistoryEnabled) {
      int uniqueParticipant = 0;
      int totalMessages = 0;
      List<ParticipantSummary> newParticipantSummaries = new ArrayList<>();
      Map<String, ParticipantHistory> newHistoryMap = new HashMap<>();

      var it = mm.getSenderIterator();
      while (it.hasNext()) {
        var sender = it.next();
        participantCounter.increment(sender);
        allCallSet.add(sender);
        var senderMessages = 0;
        ++uniqueParticipant;

        LatLongPair mapLocation = null;
        var map = mm.getMessagesForSender(sender);
        for (var entry : map.entrySet()) {
          var messages = entry.getValue();
          var messageType = entry.getKey();
          messageTypeCounter.increment(messageType, messages.size());
          if (messageType == MessageType.REJECTS) {
            for (var m : messages) {
              var rejectMessage = (RejectionMessage) m;
              var reason = rejectMessage.reason.toString();
              rejectCounter.increment(reason);
            }
          }
          totalMessages += messages.size();
          senderMessages += messages.size();
          // use first location that's valid, regardless of message type
          if (mapLocation == null) {
            for (var message : messages) {
              if (message.mapLocation != null && message.mapLocation.isValid()) {
                mapLocation = message.mapLocation;
                break;
              } // end if message has a valid location
            } // end loop over messages for a given type
          } // end search for a valid location
        } // end loop over message types

        mapLocation = (mapLocation == null) ? LatLongPair.ZERO_ZERO : mapLocation;
        var participantSummary = new ParticipantSummary(exerciseDate, exerciseName, sender, mapLocation.getLatitude(),
            mapLocation.getLongitude());
        newParticipantSummaries.add(participantSummary);

        var participantHistory = new ParticipantHistory(sender, exerciseDate, exerciseName, mapLocation.getLatitude(),
            mapLocation.getLongitude(), 1, senderMessages, CategoryIndex.FIRST_TIME.ordinal());
        newHistoryMap.put(sender, participantHistory);

      } // end loop over senders/participants;

      // firstTimers, exercise-participantHistory and merged-participantHistory
      List<ParticipantHistory> firstTimers = new ArrayList<>();
      List<ParticipantHistory> exercisePartipantHistory = new ArrayList<>();
      List<ParticipantHistory> mergedParticipantHistory = new ArrayList<>();
      for (var sender : allCallSet) {
        var newPH = newHistoryMap.get(sender);
        var oldPH = oldHistoryMap.get(sender);
        var mergedPH = merge(oldPH, newPH, totalExerciseCount);
        if (oldPH == null) {
          firstTimers.add(newPH);
        }
        if (newPH != null) {
          exercisePartipantHistory.add(mergedPH);
        }
        mergedParticipantHistory.add(mergedPH);
      }

      // output stuff
      var exerciseSummary = new ExerciseSummary(exerciseDate, exerciseName, exerciseDescription, totalMessages,
          uniqueParticipant);

      oldExerciseSummaries.add(exerciseSummary);
      WriteProcessor.writeTable(fixES(oldExerciseSummaries), Path.of(outputDbPathName, "exerciseSummary.csv"));

      oldParticipantSummaries.addAll(newParticipantSummaries);
      WriteProcessor.writeTable(fixPS(oldParticipantSummaries), Path.of(outputDbPathName, "participantSummary.csv"));

      WriteProcessor.writeTable(fixPH(firstTimers), Path.of(outputDbPathName, "firstTimers.csv"));

      WriteProcessor
          .writeTable(fixPH(exercisePartipantHistory), Path.of(outputDbPathName, "exercise-participantHistory.csv"));

      WriteProcessor.writeTable(fixPH(mergedParticipantHistory), Path.of(outputDbPathName, "participantHistory.csv"));
    } else { // just the logic for when !isHistoryEnabled
      var it = mm.getSenderIterator();
      while (it.hasNext()) {
        var sender = it.next();
        participantCounter.increment(sender);
        var map = mm.getMessagesForSender(sender);
        for (var entry : map.entrySet()) {
          var messages = entry.getValue();
          var messageType = entry.getKey();
          messageTypeCounter.increment(messageType, messages.size());
          if (messageType == MessageType.REJECTS) {
            for (var m : messages) {
              var rejectMessage = (RejectionMessage) m;
              var reason = rejectMessage.reason.toString();
              rejectCounter.increment(reason);
            }
          } // end if reject
        } // end loop over message types
      } // end loop over senders/participants;
    } // end if not isHistoryEnabled

    var sb = new StringBuilder();
    var uniqueMessageCount = messageTypeCounter.getValueTotal();
    var totalMessageCount = uniqueMessageCount + (Integer) mm.getContextObject("dedupeCount");
    sb
        .append("\nSummary: unique participants: " + participantCounter.getValueTotal() //
            + ", unique messages: " + uniqueMessageCount //
            + ", total messages: " + totalMessageCount + "\n");
    sb.append("\nMessageTypes: \n" + formatCounter(messageTypeCounter.getDescendingCountIterator(), "type", "count"));
    sb.append("\nReject Reasons: \n" + formatCounter(rejectCounter.getDescendingCountIterator(), "reason", "count"));
    logger.info(sb.toString());
  }

  private ParticipantHistory merge(ParticipantHistory oldPH, ParticipantHistory newPH, int totalExerciseCount) {
    if (oldPH == null) {
      return newPH;
    }

    if (newPH == null) { // just "age" the category index
      var categoryIndex = computeCategoryIndex(oldPH.exerciseCount, totalExerciseCount);
      categoryIndex = (oldPH.exerciseCount == 1) ? CategoryIndex.ONE_AND_DONE.ordinal() : categoryIndex;
      return new ParticipantHistory(oldPH.call, oldPH.date, oldPH.name, oldPH.latitude, oldPH.longitude, // ,
          oldPH.exerciseCount, oldPH.messageCount, categoryIndex);
    } else { // use new values, compute new category index
      var categoryIndex = computeCategoryIndex(oldPH.exerciseCount + 1, totalExerciseCount);
      return new ParticipantHistory(oldPH.call, newPH.date, newPH.name, newPH.latitude, newPH.longitude, // ,
          1 + oldPH.exerciseCount, newPH.messageCount + oldPH.messageCount, categoryIndex);
    }
  }

  private int computeCategoryIndex(int exerciseCount, int totalExercises) {
    double percent = Math.round(100d * exerciseCount / totalExercises);
    if (percent >= 90d) {
      return CategoryIndex.HEAVY_HITTER.ordinal();
    } else if (percent >= 50d) {
      return CategoryIndex.GOING_STRING.ordinal();
    } else {
      return CategoryIndex.NEEDS_ENCOURAGEMENT.ordinal();
    }
  }

  private List<ExerciseSummary> getExerciseSummaries(Path path) {
    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(path, ',', false, 1);
    var list = new ArrayList<ExerciseSummary>(fieldsArray.size());
    for (var fields : fieldsArray) {
      var exerciseSummary = ExerciseSummary.make(fields);
      list.add(exerciseSummary);
    }
    return list;
  }

  private List<ParticipantSummary> getParticipantSummaries(Path path) {
    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(path);
    var list = new ArrayList<ParticipantSummary>(fieldsArray.size());
    for (var fields : fieldsArray) {
      var participantSummary = ParticipantSummary.make(fields);
      list.add(participantSummary);
    }
    return list;
  }

  private List<ParticipantHistory> getParticipantHistories(Path path) {
    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(path, ',', false, 1);
    var list = new ArrayList<ParticipantHistory>(fieldsArray.size());
    for (var fields : fieldsArray) {
      var participantHistory = ParticipantHistory.make(fields);
      list.add(participantHistory);
    }
    return list;
  }

  private static int parseInt(String s) {
    return Integer.parseInt(s);
  }

  private static String valueOf(int i) {
    return String.valueOf(i);
  }

  private List<IWritableTable> fixES(List<ExerciseSummary> list) {
    return new ArrayList<IWritableTable>(list);
  }

  private List<IWritableTable> fixPS(List<ParticipantSummary> list) {
    return new ArrayList<IWritableTable>(list);
  }

  private List<IWritableTable> fixPH(List<ParticipantHistory> list) {
    return new ArrayList<IWritableTable>(list);
  }

}
