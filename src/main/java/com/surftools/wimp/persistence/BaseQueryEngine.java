/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.persistence;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnRecord;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.persistence.dto.User;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * because some queries are too hard for me to do in SQL, I'll do it in code
 */
public abstract class BaseQueryEngine implements IPersistenceEngine {
  private static final Logger logger = LoggerFactory.getLogger(BaseQueryEngine.class);

  protected IConfigurationManager cm;

  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  protected boolean isDirty = true;
  protected Exercise lastExercise = null;

  protected Map<String, User> callUserMap = new HashMap<>();
  protected Map<Long, User> idUserMap = new HashMap<>();
  protected Map<Long, Exercise> idExerciseMap = new HashMap<>();
  protected Map<Long, Event> idEventMap = new HashMap<>();
  protected Map<String, JoinedUser> allJoinMap = new HashMap<>();
  protected Map<String, JoinedUser> activeJoinMap = new HashMap<>();
  protected Set<Exercise> allExerciseSet = new LinkedHashSet<>();
  protected Set<String> allExerciseTypes = new HashSet<>();
  protected final boolean onlyUseActive;
  protected final boolean allowFuture;

  public BaseQueryEngine(IConfigurationManager cm) {
    this.cm = cm;
    onlyUseActive = cm.getAsBoolean(Key.PERSISTENCE_ONLY_USE_ACTIVE, true);
    allowFuture = cm.getAsBoolean(Key.PERSISTENCE_ALLOW_FUTURE, false);
  }

  /**
   * create our own join
   */
  protected void join() {
    // var debugShortList = List.of(1L);

    for (var event : idEventMap.values()) {
      var user = idUserMap.get(event.userId());
      var exercise = idExerciseMap.get(event.exerciseId());
      var entry = allJoinMap.getOrDefault(user.call(), new JoinedUser(user));

      // if (debugShortList.contains(user.id())) {
      // logger.info("Processing user: " + user.call() + ", date: " + exercise.date()
      // + ", type: " + exercise.type());
      // } else {
      // logger.debug("skipping user: " + user.call());
      // continue;
      // }

      entry.update(event, exercise);
      allJoinMap.put(user.call(), entry);
      if (user.isActive()) {
        activeJoinMap.put(user.call(), entry);
      }
    }

    for (var entry : allJoinMap.values()) {
      entry.finish();
    }

    var tmpExercises = new ArrayList<Exercise>(idExerciseMap.values());
    Collections.sort(tmpExercises);
    allExerciseSet.addAll(tmpExercises);
    allExerciseTypes = new HashSet<String>(allExerciseSet.stream().map(e -> e.type()).toList());
  }

  @Override
  public ReturnRecord getUsersMissingExercises(List<Exercise> filteredExercises, int missLimit) {
    var joinMap = onlyUseActive ? activeJoinMap : allJoinMap;
    logger.debug("joinMap.size(): " + joinMap.size());

    if (filteredExercises.size() == 0) {
      logger.info("no filtered Exercises");
      return new ReturnRecord(ReturnStatus.OK, null, null);
    }

    Collections.sort(filteredExercises);
    var firstFilteredExercise = filteredExercises.get(0);
    var candidateExercises = new LinkedHashSet<Exercise>();
    for (var exercise : filteredExercises) {
      if (candidateExercises.size() <= missLimit) {
        candidateExercises.add(exercise);
        logger.debug("adding candidateExercise: " + exercise);
      } else {
        break;
      }
    }
    logger.debug("candidateExercises.size(): " + candidateExercises.size());

    var candidateJoins = new ArrayList<JoinedUser>();
    for (var join : joinMap.values()) {
      var joinExerciseIds = new HashSet<Long>(join.exercises.stream().map(e -> e.id()).toList());
      if (joinExerciseIds.contains(firstFilteredExercise.id())) {
        logger.debug("skipping call: " + join.user.call() + " because they participated in last exercise");
        continue;
      } else {
        var intersection = new HashSet<Exercise>(candidateExercises);
        intersection.retainAll(join.exercises);
        if (intersection.size() > 0) {
          logger.debug("adding call: " + join.user.call() + " with " + intersection.size() + " exercises");
          join.context = intersection;
          candidateJoins.add(join);
        } else {
          logger.debug("skipping call: " + join.user.call() + " because didn't participate in previous " + missLimit
              + " exercises");
        }
      }
    } // end for over all calls/joins
    logger.info("candidateJoins.size(): " + candidateJoins.size());

    return new ReturnRecord(ReturnStatus.OK, null, candidateJoins);

  }

  @Override
  public ReturnRecord getFilteredExercises(Set<String> _requiredExerciseTypes, LocalDate fromDate) {
    // pull from cm EVERY time, because we might me hacking it
    var epochDateString = cm.getAsString(Key.PERSISTENCE_EPOCH_DATE, "2026-01-01");
    var epochDate = LocalDate.parse(epochDateString);

    var filteredExercises = new ArrayList<Exercise>();
    var requiredExerciseTypes = (_requiredExerciseTypes == null || _requiredExerciseTypes.size() == 0) //
        ? allExerciseTypes
        : _requiredExerciseTypes;
    logger.debug("requiredTypes: " + String.join(", ", requiredExerciseTypes));

    var allExercises = new ArrayList<Exercise>(idExerciseMap.values());
    Collections.sort(allExercises); // most recent first

    for (var exercise : allExercises.reversed()) {
      if (!requiredExerciseTypes.contains(exercise.type())) {
        logger.debug("skipping exercise type(" + exercise.type() + "):" + exercise);
        continue;
      }

      if (exercise.date().isBefore(epochDate)) {
        logger.debug("skipping exercise because before EpochDate(" + epochDate.toString() + "): " + exercise);
        continue;
      }

      if (fromDate != null && exercise.date().isBefore(fromDate)) {
        logger.debug("skipping exercise because before fromDate(" + fromDate.toString() + "): " + exercise);
        continue;
      }

      filteredExercises.add(exercise);
    }

    logger.info("filteredExercises.size(): " + filteredExercises.size());
    return new ReturnRecord(ReturnStatus.OK, null, filteredExercises);
  }

  @Override
  public ReturnRecord getUsersHistory(List<Exercise> filteredExercises) {
    var joinMap = onlyUseActive ? activeJoinMap : allJoinMap;
    var content = new ArrayList<JoinedUser>();

    if (filteredExercises.size() == 0) {
      logger.info("no filtered Exercises");
      return new ReturnRecord(ReturnStatus.OK, null, content);
    }

    for (var join : joinMap.values()) {
      var intersection = new HashSet<Exercise>(filteredExercises);
      intersection.retainAll(join.exercises);

      var selectedExercises = new ArrayList<Exercise>(intersection);
      Collections.sort(selectedExercises);
      join.context = selectedExercises;

      join.exercises = selectedExercises;
      content.add(join);
    } // end for over all calls/joins

    return new ReturnRecord(ReturnStatus.OK, null, content);
  }

}
