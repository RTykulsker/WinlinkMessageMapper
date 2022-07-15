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

package com.surftools.winlinkMessageMapper.aggregation;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.opencsv.CSVWriter;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public abstract class AbstractBaseAggregator implements IAggregator {
  protected final Logger logger;
  protected List<AggregateMessage> aggregateMessages;
  protected final Map<String, AggregateMessage> aggregateMessageMap;
  protected final Map<String, Map<MessageType, List<ExportedMessage>>> fromMessageMap;
  protected final Map<String, List<ExportedMessage>> fromListMap;

  protected String outputFileName = "aggregate.csv";
  protected boolean doOutput = true;

  protected final String KEY_START_DATETIME = "startDateTime";
  protected final String KEY_END_DATETIME = "endDateTime";
  protected final String KEY_START_LATITUDE = "startLatitude";
  protected final String KEY_AVERAGE_LATITUDE = "averageLatitude";
  protected final String KEY_END_LATITUDE = "endLatitude";
  protected final String KEY_START_LONGITUDE = "startLongitude";
  protected final String KEY_AVERAGE_LONGITUDE = "averageLongitude";
  protected final String KEY_END_LONGITUDE = "lastLongitude";
  protected final String KEY_MESSAGE_COUNT = "messageCount";
  protected final String KEY_MESSAGE_TYPE_COUNT = "messageTypeCount";
  protected final String KEY_GIS_COUNT = "gisCount";

  public AbstractBaseAggregator(Logger logger) {
    this.logger = logger;
    this.aggregateMessages = new ArrayList<>();
    fromMessageMap = new HashMap<String, Map<MessageType, List<ExportedMessage>>>();
    fromListMap = new HashMap<String, List<ExportedMessage>>();
    aggregateMessageMap = new HashMap<String, AggregateMessage>();
  }

  @Override
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {

    var allMessages = new ArrayList<ExportedMessage>();
    messageMap.values().forEach(allMessages::addAll);

    // populate map of from(call) -> map(messageType ->list of messages);
    for (var message : allMessages) {
      var from = message.from;
      var fromMM = fromMessageMap.getOrDefault(from, new HashMap<MessageType, List<ExportedMessage>>());
      var messageType = message.getMessageType();
      var fromList = fromMM.getOrDefault(messageType, new ArrayList<ExportedMessage>());
      fromList.add(message);
      fromMM.put(messageType, fromList);
      fromMessageMap.put(from, fromMM);
    }

    // populate map of from(call) ->list of messages);
    for (var message : allMessages) {
      var from = message.from;
      var fromList = fromListMap.getOrDefault(from, new ArrayList<ExportedMessage>());
      fromList.add(message);
      fromListMap.put(from, fromList);
    }

    createAggregateMessages();
  }

  /**
   * fill in the common properties of an aggregate message
   */
  protected void createAggregateMessages() {
    for (var from : fromListMap.keySet()) {
      var list = fromListMap.get(from);
      var map = new HashMap<String, Object>();

      map.put(KEY_MESSAGE_COUNT, list.size());

      var typeMap = fromMessageMap.get(from);
      map.put(KEY_MESSAGE_TYPE_COUNT, typeMap.size());

      list.sort((m1, m2) -> m1.dateTime.compareTo(m2.dateTime));
      map.put(KEY_START_DATETIME, list.get(0).dateTime);
      map.put(KEY_END_DATETIME, list.get(list.size() - 1).dateTime);

      int gisCount = 0;
      double sumLatitude = 0d;
      double sumLongitude = 0d;
      for (ExportedMessage m : list) {
        if (m instanceof GisMessage) {
          var gis = (GisMessage) m;
          ++gisCount;
          sumLatitude += Double.parseDouble(gis.latitude);
          sumLongitude += Double.parseDouble(gis.longitude);
          if (map.get(KEY_START_LATITUDE) == null) {
            map.put(KEY_START_LATITUDE, gis.latitude);
          }
          if (map.get(KEY_START_LONGITUDE) == null) {
            map.put(KEY_START_LONGITUDE, gis.longitude);
          }
          map.put(KEY_END_LATITUDE, gis.latitude);
          map.put(KEY_END_LONGITUDE, gis.longitude);
        } // endif gis
      } // end loop over messages
      var avgLatitude = sumLatitude / gisCount;
      var avgLongitude = sumLongitude / gisCount;

      if (gisCount == 0) {
        map.put(KEY_AVERAGE_LATITUDE, "");
        map.put(KEY_AVERAGE_LONGITUDE, "");
      } else {
        map.put(KEY_AVERAGE_LATITUDE, String.format(String.format("%.5f", avgLatitude)));
        map.put(KEY_AVERAGE_LONGITUDE, String.format("%.5f", avgLongitude));
      }
      map.put(KEY_GIS_COUNT, gisCount);

      var aggregateMessage = new AggregateMessage(from, map);
      aggregateMessages.add(aggregateMessage);
      aggregateMessageMap.put(from, aggregateMessage);
    }
  }

  @Override
  public void output(String pathName) {
    if (!doOutput) {
      return;
    }

    Path outputPath = Path.of(pathName, "output", outputFileName);

    var messageCount = 0;
    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(getHeaders());

      if (aggregateMessages.size() > 0) {
        for (AggregateMessage m : aggregateMessages) {
          if (m != null) {
            var values = getValues(m);
            if (values != null) {
              writer.writeNext(values);
              ++messageCount;
            }
          } else {
            continue;
          }
        }
      }

      writer.close();
      logger.info("wrote " + messageCount + " aggregate messages to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public void setOutputFileName(String outputFileName) {
    this.outputFileName = outputFileName;
  }

  protected MessageType getMostCommonMessageType(Map<MessageType, List<ExportedMessage>> messageMap) {
    int maxCount = -1;
    MessageType maxType = null;

    for (var type : messageMap.keySet()) {
      var list = messageMap.get(type);
      if (list == null) {
        continue;
      }

      if (list.size() > maxCount) {
        maxCount = list.size();
        maxType = type;
      }
    }
    return maxType;
  }

}
