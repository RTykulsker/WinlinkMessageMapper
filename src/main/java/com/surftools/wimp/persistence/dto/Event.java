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

package com.surftools.wimp.persistence.dto;

import com.surftools.utils.location.LatLongPair;

/**
 * Data Transfer Object (DTO) for Event, a join between a User and an Exercise
 */
public record Event(long id, //
    long userId, // foreign key to User
    long exerciseId, // foreign key to Exercise
    String call, // alternative to userId
    LatLongPair location, int feedbackCount, String feedback, String context) {

  public static Event updateUserId(Event old, long newUserId, long newExerciseId) {
    return new Event(old.id, newUserId, newExerciseId, old.call, //
        old.location, old.feedbackCount, old.feedback, old.context);
  }

  public static Event updateEventId(Event old, long eventId) {
    return new Event(eventId, old.userId, old.exerciseId, old.call, //
        old.location, old.feedbackCount, old.feedback, old.context);
  }
}
