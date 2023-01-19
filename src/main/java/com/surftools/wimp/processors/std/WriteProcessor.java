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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;

/**
 * writes all the processed messages
 *
 * @author bobt
 *
 */
public class WriteProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WriteProcessor.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override
  public void process() {
    var it = mm.getMessageTypeIteror();
    while (it.hasNext()) {
      var messageType = it.next();
      var messages = mm.getMessagesForType(messageType);
      writeOutput(messages, messageType);
    }

    writeOutput(new ArrayList<ExportedMessage>(mm.getOriginalMessages()), MessageType.EXPORTED);
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

}
