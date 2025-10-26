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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.persistence.dto.BulkInsertEntry;
import com.surftools.wimp.persistence.dto.Event;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnRecord;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.persistence.dto.User;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class SQLIteNativeEngine implements IPersistenceEngine {
  private static final Logger logger = LoggerFactory.getLogger(SQLIteNativeEngine.class);
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private final String url;

  public SQLIteNativeEngine(IConfigurationManager cm) {
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
        var user = new User(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), dateJoined);
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

  /**
   * support an exercise, by inserting events, etc.
   *
   * @param input
   * @return
   */
  @Override
  public ReturnRecord bulkInsert(BulkInsertEntry input) {
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
        event = bulkInsert_user(connection, event, exerciseId);
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

  private Event bulkInsert_user(Connection connection, Event event, long exerciseId) throws SQLException {
    long userId = -1;

    var sql = """
        INSERT INTO Users (CallSign) VALUES (?)
        ON CONFLICT(CallSign)
        DO UPDATE SET Active = 1
        """;

    var ps1 = connection.prepareStatement(sql);
    ps1.setString(1, event.call());
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

}
