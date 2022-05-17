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

package com.surftools.winlinkMessageMapper.processor.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.Ics213ReplyMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class Ics213ReplyProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(Ics213ReplyProcessor.class);

  private final MessageType messageType = MessageType.ICS_213_REPLY;

  @Override
  public ExportedMessage process(ExportedMessage message) {
    try {

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

      String xmlString = new String(message.attachments.get(messageType.attachmentName()));
      makeDocument(message.messageId, xmlString);

      String organization = getStringFromXml("formtitle");

      // we want the value of the <message> element
      String messageText = getStringFromXml("message");
      if (messageText == null) {
        messageText = getStringFromXml("Message");
      }

      var reply = getStringFromXml("reply");
      var replyBy = getStringFromXml("rply_by");
      var replyPosition = getStringFromXml("rply_position");
      var replyDateTime = getStringFromXml("rply_dtm");

      var m = new Ics213ReplyMessage(message, organization, messageText, //
          reply, replyBy, replyPosition, replyDateTime);
      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
