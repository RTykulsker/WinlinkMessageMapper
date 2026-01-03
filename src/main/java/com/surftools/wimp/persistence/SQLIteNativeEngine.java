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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.persistence.dto.BulkInsertEntry;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnRecord;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.persistence.dto.User;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class SQLIteNativeEngine extends BaseQueryEngine {
  private static final Logger logger = LoggerFactory.getLogger(SQLIteNativeEngine.class);

  private String url;

  public SQLIteNativeEngine(IConfigurationManager cm) {
    super(cm);
    url = cm.getAsString(Key.PERSISTENCE_SQLITE_URL);
  }

  @Override
  public ReturnRecord getAllUsers() {
    List<User> users = new ArrayList<>();

    var sql = "SELECT userIdx, callsign, name, active, dateJoined FROM users";

    Connection connection = null;
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:" + url);
      Statement statement = connection.createStatement();
      var rs = statement.executeQuery(sql);
      while (rs.next()) {
        logger.debug(rs.getString(2));
        var dateJoined = LocalDate.parse(rs.getString(5));
        var user = new User(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), dateJoined);
        idUserMap.put(user.id(), user);
        callUserMap.put(user.call(), user);
        users.add(user);
      }
      statement.close();
      connection.close();
    } catch (Exception e) {
      logger.error("SQL Exception: " + e.getMessage());
      return new ReturnRecord(ReturnStatus.ERROR, e.getMessage(), null);
    }

    return new ReturnRecord(ReturnStatus.OK, "", users);
  }

  @Override
  public ReturnRecord getAllExercises() {
    List<Exercise> exercises = new ArrayList<>();

    var sql = "SELECT exerciseIdx, date, type, name, description FROM exercises";

    Connection connection = null;
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:" + url);
      Statement statement = connection.createStatement();
      var rs = statement.executeQuery(sql);
      while (rs.next()) {
        var date = LocalDate.parse(rs.getString(2));
        var exercise = new Exercise(rs.getLong(1), date, rs.getString(3), rs.getString(4), rs.getString(5));
        idExerciseMap.put(exercise.id(), exercise);
        exercises.add(exercise);
      }
      statement.close();
      connection.close();
    } catch (Exception e) {
      logger.error("SQL Exception: " + e.getMessage());
      return new ReturnRecord(ReturnStatus.ERROR, e.getMessage(), null);
    }

    return new ReturnRecord(ReturnStatus.OK, "", exercises);
  }

  @Override
  public ReturnRecord getAllEvents() {
    List<Event> events = new ArrayList<>();

    var sql = "SELECT eventIdx, userIdx, exerciseIdx, latitude, longitude, feedbackCount, FeedbackText, Context FROM Events";

    /**
     * public record Event(long id, // long userId, // foreign key to User long exerciseId, // foreign key to Exercise
     * String call, // alternative to userId LatLongPair location, int feedbackCount, String feedback, String context) {
     */
    Connection connection = null;
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:" + url);
      Statement statement = connection.createStatement();
      var rs = statement.executeQuery(sql);
      while (rs.next()) {
        var location = new LatLongPair(rs.getDouble(4), rs.getDouble(5));
        var call = idUserMap.get(rs.getLong(2)).call();
        var event = new Event(rs.getLong(1), rs.getLong(2), rs.getLong(3), call, //
            location, rs.getInt(6), rs.getString(7), rs.getString(8));
        idEventMap.put(event.id(), event);
        events.add(event);
      }
      statement.close();
      connection.close();
    } catch (Exception e) {
      logger.error("SQL Exception: " + e.getMessage());
      return new ReturnRecord(ReturnStatus.ERROR, e.getMessage(), null);
    }

    return new ReturnRecord(ReturnStatus.OK, "", events);
  }

  /**
   * support an exercise, by inserting events, etc.
   *
   * @param input
   * @return
   */
  @Override
  public ReturnRecord bulkInsert(BulkInsertEntry input) {
    if (!allowFuture) {
      var exerciseDate = input.exercise().date();
      var nowDate = LocalDate.now();
      if (exerciseDate.isAfter(nowDate)) {
        var message = "skipping bulkInsert because Exercise date: " + exerciseDate + " is in future of now: " + nowDate;
        logger.warn("### " + message);
        return new ReturnRecord(ReturnStatus.ERROR, message, nowDate);
      }
    }

    Connection connection = null;
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:" + url);
      connection.setAutoCommit(false); // begin transaction

      var exercise = input.exercise();
      var exerciseId = bulkInsert_exercise(connection, exercise);
      exercise = Exercise.updateExerciseId(exercise, exerciseId);

      var events = input.events();
      for (var event : events) {
        event = bulkInsert_user(connection, event, exercise);
        bulkInsert_event(connection, event);
      }

      logger.info("persisted exercise: " + exercise.toString() + ", plus " + events.size() + " events to: " + url);

      connection.commit();
      connection.close();
    } catch (Exception e) {
      try {
        connection.rollback(); // roll back transaction on error
      } catch (Exception ee) {
        logger.error("SQL Exception rolling back transaction: " + ee.getMessage());
        return new ReturnRecord(ReturnStatus.ERROR, ee.getMessage(), null);
      }
      logger.error("SQL Exception: " + e.getMessage());
      return new ReturnRecord(ReturnStatus.ERROR, e.getMessage(), null);
    }
    return new ReturnRecord(ReturnStatus.OK, null, null);
  }

  private Event bulkInsert_event(Connection connection, Event event) throws SQLException {
    long eventId = -1;

    var sql = """
        INSERT INTO Events (UserIDX, ExerciseIDX, Latitude, Longitude, FeedbackCount, FeedbackText, Context)
          VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(UserIDX,ExerciseIDX)
        DO UPDATE SET Latitude=excluded.Latitude, Longitude=excluded.Longitude,
          FeedbackCount=excluded.FeedbackCount, FeedbackText = excluded.feedbackText, Context=excluded.Context
        """;

    var ps1 = connection.prepareStatement(sql);
    ps1.setLong(1, event.userId());
    ps1.setLong(2, event.exerciseId());
    ps1.setDouble(3, event.location().getLatitudeAsDouble());
    ps1.setDouble(4, event.location().getLongitudeAsDouble());
    ps1.setInt(5, event.feedbackCount());
    ps1.setString(6, event.feedback());
    ps1.setString(7, event.context());
    ps1.executeUpdate();
    ps1.close();

    sql = "SELECT EventIDX FROM Events WHERE UserIDX = ? AND ExerciseIDX = ?";
    ps1 = connection.prepareStatement(sql);
    ps1.setLong(1, event.userId());
    ps1.setLong(2, event.exerciseId());
    var rs = ps1.executeQuery();
    while (rs.next()) {
      eventId = rs.getLong(1);
      if (eventId >= 0) {
        rs.close();
        ps1.close();
        logger.debug("User/call: " + event.call() + ", eventId: " + eventId);
      }
    }
    return Event.updateEventId(event, eventId);
  }

  private Event bulkInsert_user(Connection connection, Event event, Exercise exercise) throws SQLException {
    long userId = -1;
    long exerciseId = exercise.id();

    var sql = """
        INSERT INTO Users (CallSign, DateJoined) VALUES (?,?)
        ON CONFLICT(CallSign)
        DO UPDATE SET Active = 1
        """;

    var ps1 = connection.prepareStatement(sql);
    ps1.setString(1, event.call());
    ps1.setString(2, DATE_FORMATTER.format(exercise.date()));
    ps1.executeUpdate();
    ps1.close();

    sql = "SELECT UserIdX FROM Users WHERE CallSign = ?";
    ps1 = connection.prepareStatement(sql);
    ps1.setString(1, event.call());
    var rs = ps1.executeQuery();
    while (rs.next()) {
      userId = rs.getLong(1);
      if (userId >= 0) {
        rs.close();
        ps1.close();
        logger.debug("User/call: " + event.call() + ", userId: " + userId);
      }
    }
    return Event.updateUserId(event, userId, exerciseId);
  }

  private long bulkInsert_exercise(Connection connection, Exercise exercise) throws SQLException {
    var sql = """
        INSERT INTO Exercises (Date,Type,Name,Description) VALUES (?,?,?,?)
        ON CONFLICT(Date,Type,Name)
        DO UPDATE SET Description = excluded.Description
        """;

    var ps1 = connection.prepareStatement(sql);
    ps1.setString(1, DATE_FORMATTER.format(exercise.date()));
    ps1.setString(2, exercise.type());
    ps1.setString(3, exercise.name());
    ps1.setString(4, exercise.description());
    ps1.executeUpdate();
    ps1.close();

    long exerciseId = -1;
    sql = "SELECT exerciseIdx FROM Exercises WHERE Date = ? AND Type = ? AND Name = ?";
    ps1 = connection.prepareStatement(sql);
    ps1.setString(1, DATE_FORMATTER.format(exercise.date()));
    ps1.setString(2, exercise.type());
    ps1.setString(3, exercise.name());
    var rs = ps1.executeQuery();
    while (rs.next()) {
      exerciseId = rs.getLong(1);
      if (exerciseId >= 0) {
        rs.close();
        ps1.close();
        logger.debug("new exerciseId: " + exerciseId);
      }
    }
    return exerciseId;
  }

  @Override
  public ReturnRecord getHealth() {
    List<User> users = new ArrayList<>();

    var sql = "SELECT 1";

    Connection connection = null;
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:" + url);
      Statement statement = connection.createStatement();
      var rs = statement.executeQuery(sql);
      if (rs.next()) {
        var val = rs.getInt(1);
        if (val != 1) {
          new ReturnRecord(ReturnStatus.ERROR, "Unexpected value: " + val, null);
        }
      }
      statement.close();
      connection.close();
    } catch (Exception e) {
      logger.error("SQL Exception: " + e.getMessage());
      return new ReturnRecord(ReturnStatus.ERROR, e.getMessage(), null);
    }

    return new ReturnRecord(ReturnStatus.OK, "", users);
  }

  @Override
  public ReturnRecord updateDateJoined() {
    handleDirty();

    Connection connection = null;
    try {
      Class.forName("org.sqlite.JDBC");
      connection = DriverManager.getConnection("jdbc:sqlite:" + url);
      connection.setAutoCommit(false); // begin transaction

      for (var entry : allJoinMap.values()) {
        var user = entry.user;
        var lastExerciseIndex = entry.exercises.size() - 1;
        var firstExercise = entry.exercises.get(lastExerciseIndex);
        var firstExerciseDate = firstExercise.date();
        if (firstExerciseDate.isBefore(entry.dateJoined)) {
          logger
              .info("found candidate: call: " + user.call() + ", dateJoined: " + user.dateJoined() + ", firstExercise: "
                  + firstExerciseDate);
        }

        var sql = "UPDATE Users SET DateJoined = ? WHERE UserIdx = ?";
        var ps1 = connection.prepareStatement(sql);
        ps1.setString(1, DATE_FORMATTER.format(firstExerciseDate));
        ps1.setLong(2, user.id());
        ps1.executeUpdate();
        ps1.close();
      } // end loop over joins

      connection.commit();
      connection.close();
    } catch (Exception e) {
      try {
        connection.rollback(); // roll back transaction on error
      } catch (Exception ee) {
        logger.error("SQL Exception rolling back transaction: " + ee.getMessage());
        return new ReturnRecord(ReturnStatus.ERROR, ee.getMessage(), null);
      }
      logger.error("SQL Exception: " + e.getMessage());
      return new ReturnRecord(ReturnStatus.ERROR, e.getMessage(), null);
    }
    return new ReturnRecord(ReturnStatus.OK, null, null);
  }

  protected void loadTables() {
    var ret = getAllUsers();
    if (ret.status() != ReturnStatus.OK) {
      throw new RuntimeException("Could not getAllUsers: " + ret.content());
    }

    ret = getAllExercises();
    if (ret.status() != ReturnStatus.OK) {
      throw new RuntimeException("Could not getAllExercises: " + ret.content());
    }

    ret = getAllEvents();
    if (ret.status() != ReturnStatus.OK) {
      throw new RuntimeException("Could not getAllEvents: " + ret.content());
    }

    var exercises = new ArrayList<Exercise>(idExerciseMap.values());
    Collections.sort(exercises);
    lastExercise = exercises.get(0);

    join();

    var debug = true;
    if (debug == true) {
      logger.info("users (call): " + idUserMap.size());
      logger.info("users (id): " + idUserMap.size());
      logger.info("users (join): " + allJoinMap.size());
      logger.info("active users (join): " + activeJoinMap.size());
      logger.info("exercies: " + idExerciseMap.size());
      logger.info("events: " + idEventMap.size());
      logger.info("lastExercise: " + lastExercise);
    }
  }

  private void handleDirty() {
    if (isDirty) {
      idUserMap.clear();
      callUserMap.clear();
      idExerciseMap.clear();
      idEventMap.clear();

      loadTables();

      isDirty = false;
    }
  }

  @Override
  public ReturnRecord getUsersMissingExercises(List<Exercise> filteredExercises, int missLimit) {

    handleDirty();
    return super.getUsersMissingExercises(filteredExercises, missLimit);
  }

  @Override
  public ReturnRecord getFilteredExercises(Set<String> requiredExerciseTypes, LocalDate fromDate) {

    handleDirty();
    return super.getFilteredExercises(requiredExerciseTypes, fromDate);
  }

  @Override
  public ReturnRecord getUsersHistory(List<Exercise> filteredExercises) {

    handleDirty();
    return super.getUsersHistory(filteredExercises);
  }

}
