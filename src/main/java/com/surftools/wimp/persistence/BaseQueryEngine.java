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
 * because some queries are too complicated for me to figure out in SQL, I'll do it in code
 */
public abstract class BaseQueryEngine implements IPersistenceEngine {
  private static final Logger logger = LoggerFactory.getLogger(BaseQueryEngine.class);

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
    onlyUseActive = cm.getAsBoolean(Key.PERSISTENCE_ONLY_USE_ACTIVE, true);
    allowFuture = cm.getAsBoolean(Key.PERSISTENCE_ALLOW_FUTURE, false);
  }

  /**
   * create our own join
   */
  protected void join() {
    for (var event : idEventMap.values()) {
      var user = idUserMap.get(event.userId());
      var exercise = idExerciseMap.get(event.exerciseId());
      var entry = allJoinMap.getOrDefault(user.call(), new JoinedUser(user));
      entry.update(event, exercise);
      allJoinMap.put(user.call(), entry);
      if (user.isActive()) {
        activeJoinMap.put(user.call(), entry);
      }
    }

    for (var entry : allJoinMap.values()) {
      entry.finalize();
    }

    var tmpExercises = new ArrayList<Exercise>(idExerciseMap.values());
    Collections.sort(tmpExercises);
    allExerciseSet.addAll(tmpExercises);
    allExerciseTypes = new HashSet<String>(allExerciseSet.stream().map(e -> e.type()).toList());
  }

  protected List<Exercise> getFilteredExercises(Set<String> _requiredExerciseTypes, Exercise fromExercise) {
    var filteredExercises = new ArrayList<Exercise>();
    var requiredExerciseTypes = (_requiredExerciseTypes == null || _requiredExerciseTypes.size() == 0) //
        ? allExerciseTypes
        : _requiredExerciseTypes;
    logger.debug("requiredTypes: " + String.join(", ", requiredExerciseTypes));

    var startExercise = fromExercise == null ? lastExercise : fromExercise;
    if (startExercise.id() <= 0) {
      throw new IllegalArgumentException("Invalid startExerciseId: " + startExercise);
    }
    logger.debug("startExercise: " + startExercise);

    // skip over exercises until the startExercise, then include them all
    var include = false;
    var candidateExercises = new ArrayList<Exercise>();
    for (var exercise : allExerciseSet) {
      if (exercise.id() == startExercise.id()) {
        include = true;
      }
      if (include) {
        candidateExercises.add(exercise);
      }
    }
    logger.debug("candidateExercises.size(): " + candidateExercises.size());

    for (var exercise : candidateExercises) {
      if (requiredExerciseTypes.contains(exercise.type())) {
        logger.debug("including exercise: " + exercise);
        filteredExercises.add(exercise);
      } else {
        logger.debug("excluding wrong type exercise: " + exercise);
      }
    }
    logger.info("filteredExercises.size(): " + filteredExercises.size());
    return filteredExercises;
  }

  @Override
  public ReturnRecord getUsersMissingExercises(Set<String> requiredExerciseTypes, Exercise fromExercise,
      int missLimit) {
    var joinMap = onlyUseActive ? activeJoinMap : allJoinMap;
    logger.debug("joinMap.size(): " + joinMap.size());

    var filteredExercises = getFilteredExercises(requiredExerciseTypes, fromExercise);
    if (filteredExercises.size() == 0) {
      logger.info("no filtered Exercises");
      return new ReturnRecord(ReturnStatus.OK, null, null);
    }

    var firstFilteredExercise = filteredExercises.get(0);
    var candidateExercises = new LinkedHashSet<Exercise>();
    for (var exercise : filteredExercises) {
      if (candidateExercises.size() <= missLimit) {
        candidateExercises.add(exercise);
        logger.debug("adding candidateExercise: " + exercise);
      }
    }
    logger.debug("candidateExercises.size(): " + candidateExercises.size());

    var candidateJoins = new ArrayList<JoinedUser>();
    for (var join : joinMap.values()) {
      if (join.exercises.get(0).id() == firstFilteredExercise.id()) {
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
          logger
              .debug("skipping call: " + join.user.call() + " because didn't participate in previous " + missLimit
                  + " exercises");
        }
      }
    } // end for over all calls/joins
    logger.info("candidateJoins.size(): " + candidateJoins.size());

    return new ReturnRecord(ReturnStatus.OK, null, candidateJoins);
  }

  @Override
  public ReturnRecord getUsersHistory(Set<String> requiredExerciseTypes, Exercise fromExercise, boolean doPartition) {
    Map<HistoryType, List<JoinedUser>> map = new HashMap<>();
    var joinMap = onlyUseActive ? activeJoinMap : allJoinMap;
    Object content = (doPartition) ? map : new ArrayList<JoinedUser>();

    var filteredExercises = getFilteredExercises(requiredExerciseTypes, fromExercise);
    if (filteredExercises.size() == 0) {
      logger.info("no filtered Exercises");
      return new ReturnRecord(ReturnStatus.OK, null, content);
    }
    var firstFilteredExercise = filteredExercises.get(0);
    var heavyHitLimit = (int) Math.round(filteredExercises.size() * 0.9);

    for (var join : joinMap.values()) {
      HistoryType ht = null;
      var intersection = new HashSet<Exercise>(filteredExercises);
      intersection.retainAll(join.exercises);

      join.context = intersection;

      var iSize = intersection.size();
      if (iSize == 0) {
        ht = HistoryType.FILTERED_OUT;
        logger.debug("call: " + join.user.call() + ", type: " + ht.name() + ", iSize: " + iSize);
      } else if (iSize == 1) {
        if (join.exercises.get(0).id() == firstFilteredExercise.id()) {
          ht = HistoryType.FIRST_TIME;
          logger.debug("call: " + join.user.call() + ", type: " + ht.name() + ", iSize: " + iSize);
        } else {
          ht = HistoryType.ONE_AND_DONE;
          logger.debug("call: " + join.user.call() + ", type: " + ht.name() + ", iSize: " + iSize);
        }
      } else if (iSize >= heavyHitLimit) {
        ht = HistoryType.HEAVY_HITTER;
        logger.debug("call: " + join.user.call() + ", type: " + ht.name() + ", iSize: " + iSize);
      } else {
        ht = HistoryType.ALL_OTHER;
        logger.debug("call: " + join.user.call() + ", type: " + ht.name() + ", iSize: " + iSize);
      }

      var list = map.getOrDefault(ht, new ArrayList<JoinedUser>());
      list.add(join);
      map.put(ht, list);
    } // end for over all calls/joins

    for (var ht : HistoryType.values()) {
      logger.info("HistoryType: " + ht.name() + ", count: " + map.getOrDefault(ht, new ArrayList<JoinedUser>()).size());
    }

    if (!doPartition) {
      @SuppressWarnings("unchecked")
      var list = (ArrayList<JoinedUser>) content;
      for (var ht : HistoryType.values()) {
        list.addAll(map.getOrDefault(ht, new ArrayList<JoinedUser>()));
      }
    }

    return new ReturnRecord(ReturnStatus.OK, null, content);
  }

}
