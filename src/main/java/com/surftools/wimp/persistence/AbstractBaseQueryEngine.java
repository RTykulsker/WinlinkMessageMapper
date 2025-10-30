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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnRecord;
import com.surftools.wimp.persistence.dto.User;

/**
 * because some queries are too complicated for me to figure out in SQL, I'll do it in code
 */
public abstract class AbstractBaseQueryEngine implements IPersistenceEngine {
  protected boolean isDirty = true;

  protected Exercise lastExercise = null;

  protected Map<String, User> callUserMap = new HashMap<>();
  protected Map<Long, User> idUserMap = new HashMap<>();
  protected Map<Long, Exercise> idExerciseMap = new HashMap<>();
  protected Map<Long, Event> idEventMap = new HashMap<>();
  protected Map<String, FullUser> joinMap = new HashMap<>();
  protected Map<String, FullUser> activeJoinMap = new HashMap<>();

  protected class FullUser {
    public User user;
    public LatLongPair location;
    public LocalDate dateJoined;
    public LocalDate lastExerciseDate;
    public List<Event> events;
    public List<Exercise> exercises;

    public FullUser(User user) {
      this.user = user;
      this.location = null;
      this.dateJoined = user.dateJoined();
      this.lastExerciseDate = null;
      this.events = new ArrayList<>();
      this.exercises = new ArrayList<>();
    }

    public void update(Event event, Exercise exercise) {
      if (lastExerciseDate == null || exercise.date().isAfter(lastExerciseDate)) {
        lastExerciseDate = exercise.date();
        location = event.location();
      }

      events.add(event);
      exercises.add(exercise);
    }

    @Override
    public void finalize() {
      Collections.sort(exercises);
    }
  }

  /**
   * create our own join
   */
  protected void join() {
    for (var event : idEventMap.values()) {
      var user = idUserMap.get(event.userId());
      var exercise = idExerciseMap.get(event.exerciseId());
      var entry = joinMap.getOrDefault(user.call(), new FullUser(user));
      entry.update(event, exercise);
      joinMap.put(user.call(), entry);
      if (user.isActive()) {
        activeJoinMap.put(user.call(), entry);
      }
    }

    for (var entry : joinMap.values()) {
      entry.finalize();
    }
  }

  @Override
  public abstract ReturnRecord updateDateJoined();

}
