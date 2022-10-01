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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.CheckOutMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

/**
 * processor for ETO's favorite message type
 *
 * @author bobt
 *
 */
public class CheckInProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CheckInProcessor.class);

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  private final MessageType messageType;

  public CheckInProcessor(boolean isCheckIn) {
    messageType = (isCheckIn) ? MessageType.CHECK_IN : MessageType.CHECK_OUT;
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {

    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    try {
      if (saveAttachments) {
        saveAttachments(message);
      }

      String xmlString = new String(message.attachments.get(messageType.attachmentName()));

      makeDocument(message.messageId, xmlString);

      var latLong = getLatLongFromXml(null);
      if (latLong == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      var organization = getStringFromXml("organization");
      var band = getStringFromXml("band");
      var status = getStringFromXml("status");
      var mode = getStringFromXml("session");
      var comments = getStringFromXml("comments");

      var version = "";
      var templateVersion = getStringFromXml("templateversion");

      String formDate = null;
      String formTime = null;
      var datetime = getStringFromXml("datetime");
      if (datetime != null) {
        var fields = datetime.split(" ");
        if (fields != null && fields.length >= 1) {
          formDate = fields[0];
        }
        if (fields.length >= 2) {
          formTime = fields[1];
        }
      }

      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      CheckInMessage m = null;
      if (messageType == MessageType.CHECK_IN) {
        m = new CheckInMessage(message, latLong, organization, comments, status, band, mode, version, formDate,
            formTime, messageType);
      } else if (messageType == MessageType.CHECK_OUT) {
        m = new CheckOutMessage(message, latLong, organization, comments, status, band, mode, version, formDate,
            formTime, messageType);
      } else {
        return reject(message, RejectType.UNSUPPORTED_TYPE, messageType.name());
      }

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public String getPostProcessReport(List<ExportedMessage> messages) {
    return "";
  }

}
