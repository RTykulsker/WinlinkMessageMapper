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
import java.util.List;
import java.util.Set;

import com.surftools.wimp.persistence.dto.BulkInsertEntry;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnRecord;

public interface IPersistenceManager {

  /**
   * can we connect to our database, etc.
   *
   * @return
   */
  public ReturnRecord getHealth();

  /**
   * ReturnRecord getHealth();
   */

  /**
   * get all Users, no filtering
   *
   * @return
   */
  public ReturnRecord getAllUsers();

  /**
   * get all Exercises, no filtering
   *
   * @return
   */
  public ReturnRecord getAllExercises();

  /**
   * get all Exercises, no filtering
   *
   * @return
   */
  public ReturnRecord getAllEvents();

  /**
   * support an exercise, by inserting events, etc.
   *
   * @param input
   * @return
   */
  public ReturnRecord bulkInsert(BulkInsertEntry input);

  /**
   * update the User.DateJoined with first exercise date
   *
   * @return
   */
  public ReturnRecord updateDateJoined();

  /**
   * return a List of Exercises that meet filter criteria
   *
   * @param requiredExerciseTypes
   *          -- if list is empty or null, all exercise types, else only specified types
   * @param fromDate
   *          -- if null, all exercises, else from on or after specified date
   * @return -- if Ok, a List of Exercises that meet the search criteria the context
   */
  public ReturnRecord getFilteredExercises(Set<String> requiredExerciseTypes, LocalDate fromDate);

  /**
   *
   * @param filteredExercises
   *          -- the list of exercises to consider
   * @param missLimit
   *          -- max number of Exercises to miss before we'll ignore the user, ie, 3 strikes and you're out!
   * @return -- if Ok, a list of JoinedUser entries that meet the search criteria, with a List of missed Exercises as
   *         the context
   */
  public ReturnRecord getUsersMissingExercises(List<Exercise> filteredExercises, int missLimit);

  /**
   * return User participation history. Specifically, a list of JoinedUsers
   *
   * @param filteredExercises
   *          -- the list of exercises to consider
   *
   * @return -- if Ok, a List of JoinedUser entries that meet the search criteria the context
   */
  public ReturnRecord getUsersHistory(List<Exercise> filteredExercises);
}
