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

package com.surftools.wimp.processors.exercise.eto_2026;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IExportedMessageEditor;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.parser.CheckInParser;
import com.surftools.wimp.processors.std.BaseReadProcessor;
import com.surftools.wimp.processors.std.ClassifierProcessor;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * a Winlink Check-In message, sent via a tactical address
 *
 * NOTE WELL: we will be changing the ExportedMessage sender field, if possible
 *
 * @author bobt
 *
 */
public class ETO_2026_08_20 extends SingleMessageFeedbackProcessor implements IExportedMessageEditor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_08_20.class);
  private static final String ORIGINAL_SENDER = "ORIGINAL_SENDER";

  ClassifierProcessor classifer;
  CheckInParser parser;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.CHECK_IN;
    var extraOutboundMessageText = getNextExerciseInstructions();
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

    // to enable editing of the exportedMessage
    classifer = new ClassifierProcessor();
    classifer.initialize(cm, mm);
    parser = new CheckInParser();
    BaseReadProcessor.setExportedMessageEditor(this);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (CheckInMessage) message;
    count(sts.test("Organization name should be #EV", "ANYTOWN Recreation Centre", m.organization));

    /**
     * public final String initialOperators;
     *
     */
    var addresses = m.toList + "," + m.ccList;
    count(sts.test("To address should be contain ANYTOWNEOC", addresses.contains("ANYTOWNEOC")));
    count(sts.test("Comments should be #EV", "Request relief in four hours", m.comments));

    var originalSender = m.extraData.getOrDefault(ORIGINAL_SENDER, m.from);
    var tactialPrefix = "ANYTOWN-SHELTER-";
    count(sts.test("Sender (Tactical Address) should start with #EV", tactialPrefix, originalSender));
    count(sts
        .test("Sender (Tactical Address) should *NOT* be Source (Winlink License Holder)",
            !originalSender.equals(m.source), "sender: " + originalSender + ", source: " + m.source));

    var senderSuffix = m.from.substring(m.from.lastIndexOf("-") + 1);
    var source = m.source;
    count(sts.test("Sender (Tactical Address) suffix should match Source", senderSuffix.equals(source)));

    getCounter("status").increment(m.status);
    getCounter("service").increment(m.service);
    getCounter("band").increment(m.band);
    getCounter("mode").increment(m.mode);
  }

  @Override
  public ExportedMessage edit(ExportedMessage message) {
    var messageType = classifer.findMessageType(message);
    if (messageType == MessageType.CHECK_IN) {
      var m = (CheckInMessage) parser.parse(message);

      var senderSuffix = m.from.substring(m.from.lastIndexOf("-") + 1);
      var source = m.source;

      if (senderSuffix.equals(source)) {
        var originalSender = new String(m.from);
        logger.info("whacking sender from: " + originalSender + " to: " + source + ", for mId: " + m.messageId);

        var newExportedMessage = new ExportedMessage(//
            m.messageId, source, source, m.to, m.toList, m.ccList, //
            m.subject, m.msgDateTime, //
            m.msgLocation, m.msgLocationSource, //
            m.mime, m.plainContent, m.attachments, m.isP2p, m.fileName, m.lines);

        newExportedMessage.extraData.put(ORIGINAL_SENDER, originalSender);

        return newExportedMessage;
      } // end if can edit
      return message;
    } else {
      return message;
    } // end if not a CheckInMessage
  }

}