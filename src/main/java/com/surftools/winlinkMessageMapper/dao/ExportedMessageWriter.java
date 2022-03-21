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

package com.surftools.winlinkMessageMapper.dao;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.IMessage;
import com.surftools.winlinkMessageMapper.dto.message.RejectionMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class ExportedMessageWriter {
  private static final Logger logger = LoggerFactory.getLogger(ExportedMessageWriter.class);

  private final String pathName;

  public ExportedMessageWriter(String pathName) {
    this.pathName = pathName;
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
   * @param pathName
   * @param messageType
   */
  public void writeOutput(List<ExportedMessage> messages, MessageType messageType) {
    Path outputPath = Path.of(pathName, "output", messageType.toString() + ".csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      var comparator = (messageType == MessageType.REJECTS) ? new RejectionMessageComparator()
          : new DefaultMessasgeComparator();
      Collections.sort(messages, comparator);

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      if (messages.size() > 0) {
        IMessage aMessage = messages.get(0);
        writer.writeNext(aMessage.getHeaders());
        for (IMessage m : messages) {
          writer.writeNext(m.getValues());
        }
      } else {
        writer.writeNext(new String[] { "MessageId" });
      }

      writer.close();
      logger.info("wrote " + messages.size() + " messages to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  static class DefaultMessasgeComparator implements Comparator<ExportedMessage> {

    @Override
    public int compare(ExportedMessage o1, ExportedMessage o2) {
      return o1.from.compareTo(o2.from);
    }

  }

  static class RejectionMessageComparator implements Comparator<ExportedMessage> {
    @Override
    public int compare(ExportedMessage o1, ExportedMessage o2) {
      RejectionMessage r1 = (RejectionMessage) o1;
      RejectionMessage r2 = (RejectionMessage) o2;

      int compare = r1.reason.ordinal() - r2.reason.ordinal();
      if (compare != 0) {
        return compare;
      }

      compare = r1.from.compareTo(r2.from);
      if (compare != 0) {
        return compare;
      }

      return r1.dateTime.compareTo(r2.dateTime);
    }
  }

}
