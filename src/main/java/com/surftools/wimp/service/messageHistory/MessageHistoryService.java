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

package com.surftools.wimp.service.messageHistory;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class MessageHistoryService implements IMessageHistoryService {
  private static final Logger logger = LoggerFactory.getLogger(MessageHistoryService.class);

  private Map<MessageHistoryKey, MessageHistoryEntry> map;
  private IConfigurationManager cm;
  private Path inputPath;
  private String inputPathName;

  public MessageHistoryService(IConfigurationManager cm) {
    this.cm = cm;
    map = new HashMap<>();
  }

  @Override
  public void initialize() {
    var exercisePathName = "";
    Path exercisePath = null;
    var exercisesPathName = cm.getAsString(Key.PATH_EXERCISES);
    if (exercisesPathName.startsWith("!!")) {
      exercisePathName = exercisesPathName.substring(2);
      exercisePath = Path.of(exercisePathName);
    } else {
      var dateString = cm.getAsString(Key.EXERCISE_DATE);
      var date = LocalDate.parse(dateString);
      var exerciseYear = date.getYear();
      var exerciseYearString = String.valueOf(exerciseYear);
      exercisePath = Path.of(exercisesPathName, exerciseYearString, dateString);
      exercisePathName = exercisePath.toString();
    }

    inputPath = Path.of(exercisePathName, "input", "messageHistory.csv");
    inputPathName = inputPath.toString();

    var inputFile = inputPath.toFile();
    if (!inputFile.exists()) {
      logger.info("messageHistory file: " + inputPathName + " does not exist");
      return;
    }

    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(inputPath, ',', false, 1);
    for (var fields : fieldsArray) {
      var messageType = MessageType.fromString(fields[2]);
      if (messageType == null) {
        logger.error("can't find message type for: " + fields[2]);
        continue;
      }
      var key = new MessageHistoryKey(fields[0], fields[1], messageType);
      var localDate = LocalDate.parse(fields[3]);
      var localTime = LocalTime.parse(fields[4]);
      var localDateTime = LocalDateTime.of(localDate, localTime);
      var messageHistoryEntry = new MessageHistoryEntry(key, localDateTime);
      map.put(key, messageHistoryEntry);
    }
    logger.info("read: " + map.size() + " messageHistoryEntries from: " + inputPathName);
  }

  @Override
  public MessageHistoryEntry get(MessageHistoryKey key) {
    return map.get(key);
  }

  @Override
  public void add(MessageHistoryKey key) {
    map.put(key, new MessageHistoryEntry(key, LocalDateTime.now()));
  }

  @Override
  public void store() {
    WriteProcessor.writeTable(new ArrayList<IWritableTable>(map.values()), inputPath);
  }

}
