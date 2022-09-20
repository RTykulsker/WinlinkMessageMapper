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

import com.surftools.utils.location.LatLongPair;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisIcs213Message;
import com.surftools.winlinkMessageMapper.dto.message.Ics213Message;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class Ics213Processor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(Ics213Processor.class);

  private final MessageType messageType;

  public Ics213Processor(MessageType messageType) {
    this.messageType = messageType;
  }

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

      var incidentName = getStringFromXml("inc_name");
      var from = getStringFromXml("fm_name");
      var to = getStringFromXml("to_name");
      var subject = getStringFromXml("subjectline");

      LatLongComment latLongComment = getLatLongAndCommentFromXml(messageText);
      if (latLongComment == null) {
        var m = new Ics213Message(message, organization, messageText, incidentName, from, to, subject);
        return m;
      } else {
        LatLongPair latLong = latLongComment.latLong;
        if (!latLong.isValid()) {
          var m = new Ics213Message(message, organization, messageText, incidentName, from, to, subject);
          return m;
        } else {
          String restOfMessage = latLongComment.comment;
          var m = new GisIcs213Message(message, latLong.getLatitude(), latLong.getLongitude(), organization,
              restOfMessage);
          return m;
        }
      }
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  /**
   * messageText should consist of lat/lon on line 1, other stuff to follow
   *
   * @param messageText
   *
   * @return
   */
  private LatLongComment getLatLongAndCommentFromXml(String messageText) {
    LatLongPair latLong = new LatLongPair("", "");
    String latLongString = null;
    String restOfMessage = "";
    String[] lines = messageText.split("\n");
    if (lines.length >= 1) {
      latLongString = lines[0];
      String[] latLonFields = lines[0].split("[\\s,:/]+");
      String latString = "";
      String lonString = "";
      int nLatLonFields = latLonFields.length;
      if (nLatLonFields >= 2) {
        for (int i = 0; i < nLatLonFields; ++i) {
          String latLonField = latLonFields[i];

          if (latLonField.startsWith("N") || latLonField.startsWith("W")) {
            latLonField = latLonField.substring(1);
          } else if (latLonField.endsWith("N") || latLonField.endsWith("W") || latLonField.endsWith("W.")
              || latLonField.endsWith(".")) {
            latLonField = latLonField.substring(0, latLonField.length() - 1);
          }

          try {
            Double.parseDouble(latLonField);
            if (latString.equals("")) {
              latString = latLonField;
            } else if (lonString.equals("")) {
              lonString = latLonField;
              latLong = new LatLongPair(latString, lonString);
              break;
            }
          } catch (Exception e) {
            // do nothing
            continue;
          } // end catch
        } // end for over fields
      } // end if 2 or more fields on first line
    }
    if (lines.length > 1) {
      StringBuilder sb = new StringBuilder();
      // NOTE WELL; skip first element
      for (int i = 1; i < lines.length; ++i) {
        sb.append(lines[i]);
        sb.append("\n");
      }
      restOfMessage = sb.toString();
      restOfMessage = restOfMessage.substring(0, restOfMessage.length() - 1);
      while (restOfMessage.startsWith("\n")) {
        restOfMessage = restOfMessage.substring(1);
      }
    } else {
      // only one line, and we don't have complete lat/long, we are boned
      if (latLong.getLatitude().equals("") || latLong.getLongitude().equals("")) {
        restOfMessage = messageText;
      }
    } // end if lines.length == 1

    return new LatLongComment(latLong, restOfMessage, latLongString);
  }

  private record LatLongComment(LatLongPair latLong, String comment, String latLongString) {
  }

  ;
}
