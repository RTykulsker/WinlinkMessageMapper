/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.persistence.IPersistenceManager;
import com.surftools.wimp.persistence.PersistenceManager;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.JoinedUser;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.IWritableConfigurationManager;

/**
 * compose maps of participation history
 */
public class ParticipantHistoryProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ParticipantHistoryProcessor.class);

  private IWritableConfigurationManager cm;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    this.cm = (IWritableConfigurationManager) cm;
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    var db = new PersistenceManager(cm);
    var ret = db.getHealth();
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Health Check failed: " + ret.content());
      return;
    }

    makeParticipantHistory(db);
  }

  private void makeParticipantHistory(IPersistenceManager db) {
    var epochDateString = cm.getAsString(Key.PERSISTENCE_EPOCH_DATE);
    var epochDate = LocalDate.parse(epochDateString);
    logger.info("Epoch Date: " + epochDate.toString());
    var ret = db.getFilteredExercises(null, epochDate); // all types, all dates
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get filteredExercises from database: " + ret.content());
      return;
    }
    @SuppressWarnings("unchecked")
    var filteredExercises = (List<Exercise>) ret.data();

    ret = db.getUsersHistory(filteredExercises);
    if (ret.status() != ReturnStatus.OK) {
      logger.error("Could not get userHistory from database: " + ret.content());
      return;
    }

    @SuppressWarnings("unchecked")
    var joins = (List<JoinedUser>) ret.data();
    var histories = new ArrayList<ParticipantHistory>(joins.size());
    var summaries = new HashMap<Integer, ParticipantSummary>(filteredExercises.size());
    for (var join : joins) {
      if (join.exercises.size() > 0) {
        var count = 0;
        var firstDate = LocalDate.MAX;
        var lastDate = LocalDate.MIN;
        for (var exercise : join.exercises) {
          ++count;
          firstDate = (exercise.date().isBefore(firstDate)) ? exercise.date() : firstDate;
          lastDate = (exercise.date().isAfter(lastDate)) ? exercise.date() : lastDate;
        } // end loop over exercises;
        var ph = new ParticipantHistory(join.user.call(), count, firstDate, lastDate);
        histories.add(ph);
        summaries.put(count, summaries.getOrDefault(count, new ParticipantSummary(count, 0)).increment());
      } // end if join has exercises
    }
    logger.info("Got " + histories.size() + " particpant Histories");

    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(histories),
            Path.of(outputPathName, dateString + "-participantHistory.csv"));
    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(summaries.values()),
            Path.of(outputPathName, dateString + "-participantSummary.csv"));
  }

  static record ParticipantHistory(String call, int count, LocalDate firstDate, LocalDate lastDate)
      implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ParticipantHistory) other;
      return call.compareTo(o.call);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Count", "First", "Last" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, String.valueOf(count), firstDate.toString(), lastDate.toString() };
    }
  }

  static record ParticipantSummary(int nEvents, int nParticipants) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (ParticipantSummary) other;
      return o.nEvents - nEvents;
    }

    public ParticipantSummary increment() {
      return new ParticipantSummary(nEvents, nParticipants + 1);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Events", "Participants" };
    }

    @Override
    public String[] getValues() {
      return new String[] { String.valueOf(nEvents), String.valueOf(nParticipants) };
    }
  }

}
