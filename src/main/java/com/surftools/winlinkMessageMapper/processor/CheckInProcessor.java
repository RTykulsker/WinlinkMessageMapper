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

package com.surftools.winlinkMessageMapper.processor;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.MessageType;
import com.surftools.winlinkMessageMapper.reject.MessageOrRejectionResult;
import com.surftools.winlinkMessageMapper.reject.RejectType;

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
  public MessageOrRejectionResult process(ExportedMessage message) {

    try {
      if (saveAttachments) {
        saveAttachments(message);
      }

      var mime = message.mime;
      String xmlString = decodeAttachment(mime, "RMS_Express_Form", message.from);

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

      var latLong = getLatLongFromXml(xmlString, null);
      if (latLong == null) {
        return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      var organization = getStringFromXml(xmlString, "organization");
      var band = getStringFromXml(xmlString, "band");
      var status = getStringFromXml(xmlString, "status");
      var mode = getStringFromXml(xmlString, "session");
      var comments = getStringFromXml(xmlString, "comments");

      var version = "";
      var templateVersion = getStringFromXml(xmlString, "templateversion");
      if (templateVersion == null) {
        templateVersion = getStringFromXml(xmlString, "Templateversion");
      }
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      CheckInMessage m = new CheckInMessage(message, latLong.latitude(), latLong.longitude(), organization, comments,
          status, band, mode, version);

      return new MessageOrRejectionResult(m, null);
    } catch (Exception e) {
      return new MessageOrRejectionResult(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
