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

package com.surftools.wimp.parser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.QuickMessage;

/**
 * parser for General/Quick
 *
 * @author bobt
 *
 */
public class QuickParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(QuickParser.class);
  private final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    try {

      String xmlString = new String(message.attachments.get(MessageType.QUICK.attachmentName()));

      makeDocument(message.messageId, xmlString);

      var gridSquare = getStringFromXml("grid_square");
      var formLocation = new LatLongPair(LocationUtils.getLatitudeFromMaidenhead(gridSquare),
          LocationUtils.getLongitudeFromMaidenhead(gridSquare));

      var attention = getStringFromXml("attn");
      var sendToAddress = getStringFromXml("address");
      var fromNameGroup = getStringFromXml("from_name");
      var subject = getStringFromXml("subjectline");
      var messageText = getStringFromXml("message");

      var version = "";
      var templateVersion = getStringFromXml("templateversion");

      var dateTimeString = getStringFromXml("time");
      LocalDateTime formDateTime = null;
      try {
        formDateTime = LocalDateTime.parse(dateTimeString, DT_FORMATTER);
      } catch (Exception e) {
        ;
      }

      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      var m = new QuickMessage(message, //
          formLocation, formDateTime, //
          attention, sendToAddress, fromNameGroup, dateTimeString, //
          subject, messageText, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
