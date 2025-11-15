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
import java.util.List;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.User;

public class JoinedUser {
  public User user;
  public LatLongPair location;
  public LocalDate dateJoined;
  public LocalDate lastExerciseDate;
  public List<Event> events;
  public List<Exercise> exercises;
  public Object context;

  public JoinedUser(User user) {
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

  public void finish() {
    Collections.sort(exercises);
  }

}
