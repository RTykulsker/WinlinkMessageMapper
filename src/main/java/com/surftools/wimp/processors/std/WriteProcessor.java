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

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * writes all the processed messages
 *
 * @author bobt
 *
 */
public class WriteProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WriteProcessor.class);

  static class TypedMessage implements IWritableTable {
    private ExportedMessage m;
    private LatLongPair newLocation;

    public TypedMessage(ExportedMessage message) {
      m = message;
    }

    public TypedMessage(ExportedMessage message, LatLongPair latLongPair) {
      this(message);
      this.newLocation = latLongPair;
    }

    @Override
    public int compareTo(IWritableTable o) {
      var other = (ExportedMessage) o;
      return m.compareTo(other);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "MessageId", "Type", "From", "To", "Other Addresses", "Subject", // "
          "Date", "Time", "Latitude", "Longitude", "LocSource", //
          "#Attachments"//
      };
    }

    @Override
    public String[] getValues() {
      var date = m.sortDateTime == null ? "" : m.sortDateTime.toLocalDate().toString();
      var time = m.sortDateTime == null ? "" : m.sortDateTime.toLocalTime().toString();
      var lat = newLocation == null ? m.mapLocation.getLatitude() : newLocation.getLatitude();
      var lon = newLocation == null ? m.mapLocation.getLongitude() : newLocation.getLongitude();

      var addressesList = new ArrayList<String>();
      addressesList.addAll(Arrays.asList(m.toList.split(",")));
      addressesList.addAll(Arrays.asList(m.ccList.split(",")));
      addressesList.remove(m.to);
      var addresses = String.join(",", addressesList);
      var nAttachments = m.attachments == null ? "" : String.valueOf(m.attachments.size());
      return new String[] { m.messageId, m.getMessageType().toString(), m.from, m.to, addresses, m.subject, //
          date, time, lat, lon, m.msgLocationSource, //
          nAttachments };
    }

  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override
  public void postProcess() {
    var typedMessages = new ArrayList<IWritableTable>();
    var badLocationMessages = new ArrayList<ExportedMessage>();
    var it = mm.getMessageTypeIteror();
    while (it.hasNext()) {
      var messageType = it.next();
      var messages = mm.getMessagesForType(messageType);
      writeOutput(messages, messageType);
      if (messageType != MessageType.EXPORTED) {
        for (var message : messages) {
          if (message.mapLocation == null || message.mapLocation.equals(LatLongPair.ZERO_ZERO)
              || !message.mapLocation.isValid()) {
            badLocationMessages.add(message);
          } else {
            typedMessages.add(new TypedMessage(message));
          }
        }
      }
    }
    writeOutput(new ArrayList<ExportedMessage>(mm.getOriginalMessages()), MessageType.EXPORTED);

    if (badLocationMessages.size() > 0) {
      var newLocations = LocationUtils.jitter(badLocationMessages.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationMessages.size(); ++i) {
        var message = badLocationMessages.get(i);
        typedMessages.add(new TypedMessage(message, newLocations.get(i)));
      }
    }
    writeTable("typedMessages.csv", typedMessages);
  }

  /**
   * write all the processed messages
   *
   * @param messageMap
   */
  public void writeAll(Map<MessageType, List<ExportedMessage>> messageMap) {
    for (MessageType messageType : messageMap.keySet()) {
      List<ExportedMessage> messages = messageMap.get(messageType);
      if (messages != null) {
        writeOutput(messages, messageType);
      }
    }
  }

  /**
   * write out the type-specific messages
   *
   * @param messages
   * @param messageType
   */
  public void writeOutput(List<ExportedMessage> messages, MessageType messageType) {
    Path outputPath = Path.of(outputPathName, messageType.toString() + ".csv");
    Collections.sort(messages);
    writeTable(new ArrayList<IWritableTable>(messages), outputPath);
  }

  public static void writeTable(List<IWritableTable> records, Path path) {

    try {
      File outputDirectory = new File(path.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      Collections.sort(records);

      CSVWriter writer = new CSVWriter(new FileWriter(path.toString()));
      if (records.size() > 0) {
        var aMessage = records.get(0);
        writer.writeNext(aMessage.getHeaders());
        for (IWritableTable m : records) {
          writer.writeNext(m.getValues());
        }
      } else {
        writer.writeNext(new String[] { "No Data" });
      }
      writer.close();
      logger.info("wrote " + records.size() + " records to file: " + path);
    } catch (Exception e) {
      logger.error("Exception writing file: " + path + ", " + e.getLocalizedMessage());
    }
  }

  public static void writeCounter(Counter counter, Path path) {
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(path.toString()));
      writer.writeNext(new String[] { "Value", "Count" });
      var it = counter.getDescendingCountIterator();
      while (it.hasNext()) {
        var entry = it.next();
        writer.writeNext(new String[] { (String) entry.getKey(), String.valueOf((entry.getValue())) });
      }
      writer.close();
      logger.info("wrote " + counter.getKeyCount() + " counts to file: " + path);
    } catch (Exception e) {
      logger.error("Exception writing file: " + path + ", " + e.getLocalizedMessage());
    }
  }

  public static void writeString(String content, Path path) {
    try {
      Files.writeString(path, content, StandardCharsets.UTF_8);
      logger.info("wrote " + path.toString());
    } catch (Exception e) {
      logger.error("Exception writing file: " + path + ", " + e.getLocalizedMessage());
    }
  }

  public static void writeString(String content, String fileName) {
    writeString(content, Path.of(outputPathName, fileName));
  }

  @Override
  public void process() {
  }

}
