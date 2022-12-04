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
import com.surftools.winlinkMessageMapper.dto.message.Ics213RRMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class Ics213RRProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(Ics213RRProcessor.class);

  public Ics213RRProcessor() {
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {
    try {

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

      var xmlString = new String(message.attachments.get(MessageType.ICS_213_RR.attachmentName()));
      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("formtitle");
      var incidentName = getStringFromXml("incname");
      var activityDateTime = getStringFromXml("activitydatetime1");
      var requestNumber = getStringFromXml("reqnum");

      var quantity1 = getStringFromXml("qty1");
      var kind1 = getStringFromXml("kind1");
      var type1 = getStringFromXml("type1");
      var item1 = getStringFromXml("item1");
      var requestedDateTime1 = getStringFromXml("reqdatetime1");
      var estimatedDateTime1 = getStringFromXml("estdatetime1");
      var cost1 = getStringFromXml("cost1");

      var delivery = getStringFromXml("delivery");
      var substitutes = getStringFromXml("subs1");
      var requestedBy = getStringFromXml("reqname");
      var priority = getStringFromXml("priority");
      var approvedBy = getStringFromXml("secapp");

      var m = new Ics213RRMessage(message, organization, incidentName, activityDateTime, requestNumber, //
          quantity1, kind1, type1, item1, requestedDateTime1, estimatedDateTime1, cost1, //
          delivery, substitutes, requestedBy, priority, approvedBy);

      return m;

    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
