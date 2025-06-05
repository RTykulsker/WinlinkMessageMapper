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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.database.DatabaseManager;
import com.surftools.wimp.database.IDatabaseService;
import com.surftools.wimp.database.entity.ExerciseId;
import com.surftools.wimp.database.entity.ParticipantDetail;
import com.surftools.wimp.message.RejectionMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * manage a pseudo-database of exercise and participant records
 *
 * call sign changes handled externally
 *
 * no provision (yet) for deduplication (call, messageId, messageType) across multiple exercises
 */
public class DatabaseUpdateProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdateProcessor.class);

  private ExerciseId exerciseId;
  private List<ParticipantDetail> pdList = new ArrayList<>();
  private DatabaseManager db;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var DB_DTP = IDatabaseService.DB_DTF;
    var exerciseDateString = cm.getAsString(Key.EXERCISE_DATE).replaceAll("/", "-");
    var exerciseDate = LocalDate.parse(exerciseDateString, DB_DTP);
    var exerciseName = cm.getAsString(Key.EXERCISE_NAME);
    exerciseId = new ExerciseId(exerciseDate, exerciseName);

    var inputDbPathName = cm.getAsString(Key.NEW_DATABASE_PATH);
    var inputDbPath = Path.of(inputDbPathName);
    if (!inputDbPath.toFile().exists()) {
      logger.warn("### Input database directory: " + inputDbPath.toString() + " doesn't exist");
    }
    inputDbPathName = inputDbPath.toString();

    db = new DatabaseManager(cm);
    db.getEngine().load();
  }

  @Override
  public void process() {
    var participantCounter = new Counter();
    var messageTypeCounter = new Counter();
    var rejectCounter = new Counter();

    var senderIt = mm.getSenderIterator();
    while (senderIt.hasNext()) {
      var sender = senderIt.next();
      participantCounter.increment(sender);

      // the latest message with a valid location for a given exercise wins
      var messages = mm.getAllMessagesForSender(sender);
      Collections.sort(messages);
      messages = messages.reversed();
      var location = LatLongPair.ZERO_ZERO;
      for (var m : messages) {
        var messageType = m.getMessageType();

        messageTypeCounter.increment(messageType);
        if (messageType == MessageType.REJECTS) {
          var rejectMessage = (RejectionMessage) m;
          var reason = rejectMessage.reason.toString();
          rejectCounter.increment(reason);
        }

        if (m.mapLocation != null && m.mapLocation.isValid() && location.isValid()) {
          location = m.mapLocation;
        }
      } // end loop over messages

      var messageIds = String.join(",", messages.stream().map(m -> m.messageId).toList());

      var pd = new ParticipantDetail(sender, location, exerciseId, //
          messages.size(), messageIds);
      pdList.add(pd);
    }

    // execute here and not in postProcess to be sure that all processors will
    // have updates complete in postProcess()
    db.getEngine().update(exerciseId, pdList);
    db.getEngine().store();

    var sb = new StringBuilder();
    var uniqueMessageCount = messageTypeCounter.getValueTotal();
    var dedupeCount = 0;
    var dedupeCountObject = mm.getContextObject("dedupeCount");
    dedupeCount = dedupeCountObject == null ? 0 : (Integer) dedupeCountObject;
    var totalMessageCount = uniqueMessageCount + dedupeCount;
    sb
        .append("\nSummary: unique participants: " + participantCounter.getValueTotal() //
            + ", unique (not duplicated or superceded) messages: " + uniqueMessageCount //
            + ", total messages: " + totalMessageCount + "\n");
    sb.append("\nMessageTypes: \n" +

        formatCounter(messageTypeCounter.getDescendingCountIterator(), "type", "count"));
    sb.append("\nReject Reasons: \n" + formatCounter(rejectCounter.getDescendingCountIterator(), "reason", "count"));
    logger.info(sb.toString());
  }

  @Override
  public void postProcess() {
    super.postProcess();
  }

}
