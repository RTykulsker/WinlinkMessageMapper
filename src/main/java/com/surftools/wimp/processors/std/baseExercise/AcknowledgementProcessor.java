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

package com.surftools.wimp.processors.std.baseExercise;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor to send Acknowledgement messages via Winlink, useful if we are acting as a "clearinghouse"
 *
 *
 * @author bobt
 *
 */
public class AcknowledgementProcessor extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AcknowledgementProcessor.class);

  static record AckKey(String from, String messageId) {

    public static AckKey fromMessage(OutboundMessage ackMessage) {
      return new AckKey(ackMessage.to(), ackMessage.messageId());
    }
  }

  private final Map<AckKey, OutboundMessage> oldMap = new HashMap<>();
  private final Map<AckKey, OutboundMessage> newMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var oldPath = Path.of(pathName, "oldAcknowledgements.txt");
    if (oldPath.toFile().exists()) {
      var lines = ReadProcessor.readCsvFileIntoFieldsArray(oldPath);
      for (var fields : lines) {
        var oldMessage = OutboundMessage.fromFields(fields);
        var key = AckKey.fromMessage(oldMessage);
        oldMap.put(key, oldMessage);
      }
      logger.info("read " + oldMap.size() + " old ack messages from " + oldPath);
    } else {
      logger.info("previous old ack file not found: " + oldPath);
    }

  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) { // loop over senders
      sender = senderIterator.next();
      for (var m : mm.getAllMessagesForSender(sender)) {
        var key = new AckKey(m.from, m.messageId);
        var oldAck = oldMap.get(key);
        if (oldAck == null) {
          var subject = "Acknowledging your " + m.getMessageType().toString() + " message, mId: " + m.messageId;
          var body = "Message received. Thank you!";
          var outboundMessage = new OutboundMessage(outboundMessageSender, m.from, subject, body, m.messageId);
          outboundMessageList.add(outboundMessage);
          logger.info("creating acknowledgement for " + key);
          newMap.put(key, outboundMessage);
        } else {
          logger.info("skipping acknowledgement for " + key);
        }
      } // end processing for a message
    } // end loop over senders
  }

  @Override
  public void postProcess() {
    outboundMessageList = new ArrayList<OutboundMessage>(newMap.values());
    logger.info("preparing acknowledgements for " + outboundMessageList.size() + " messages");

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }

    oldMap.forEach(newMap::putIfAbsent); // merge old acknowledgments into new
    outboundMessageList = new ArrayList<OutboundMessage>(newMap.values());
    var oldPath = Path.of(pathName, "oldAcknowledgements.txt");
    if (oldPath.toFile().exists()) {
      var timeStamp = LocalDateTime.now().toString();
      var newPath = Path.of(oldPath.toString() + "-" + timeStamp + ".txt");
      try {
        Files.move(oldPath, newPath); // rename
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    var values = new ArrayList<IWritableTable>(newMap.values());
    WriteProcessor.writeTable(values, oldPath);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
  }

}
