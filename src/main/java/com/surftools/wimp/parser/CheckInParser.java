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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.MultiDateTimeParser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.CheckOutMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.ExportedMessage.ExportedKey;

/**
 * parser for everyone's favorite message type
 *
 * also handles Winlink Check Out, by extension
 *
 * @author bobt
 *
 */
public class CheckInParser extends AbstractBaseParser {
  protected Logger logger;
  protected MessageType messageType;

  protected MultiDateTimeParser mtdp = new MultiDateTimeParser(List
      .of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm 'UTC'", //
          "MM/dd/yyyy HH:mm", //
          "M-dd-yyyy", "yyyy-M-dd HH:mm", //
          "MM/dd/yyyy HHmm'hrs.'", "MM/dd/yyyy HHmm'hrs'", "yyyy-MM-dd HH:mm:ss'L'"));

  protected final DateTimeFormatter UTC_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.000Z'"); // 2024-05-23T23:55:11.000Z

  protected static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  protected static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  public CheckInParser() {
    logger = LoggerFactory.getLogger(CheckInParser.class);
    messageType = MessageType.CHECK_IN;
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    if (message.attachments.get(messageType.rmsViewerName()) != null) {
      try {
        String xmlString = new String(message.attachments.get(messageType.rmsViewerName()));
        makeDocument(message.messageId, xmlString);

        var organization = getStringFromXml("organization");
        var formDateTime = parseFormDateTime();
        var contactName = getStringFromXml("contactname");
        var initialOperators = getStringFromXml("assigned");

        var status = getStringFromXml("status");
        var service = getStringFromXml("service");
        var band = getStringFromXml("band");
        var mode = getStringFromXml("session");

        var locationString = getStringFromXml("location");
        var mgrs = getStringFromXml("mgrs");
        var gridSquare = getStringFromXml("grid");
        var formLocation = getLatLongFromXml(null);
        if (formLocation == null) {
          return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
        }

        var comments = getStringFromXml("comments");
        var version = parseVersion();

        var m = (ExportedMessage) null;
        if (messageType == MessageType.CHECK_IN) {
          m = new CheckInMessage(message, organization, //
              formDateTime, contactName, initialOperators, //
              status, service, band, mode, //
              locationString, formLocation, mgrs, gridSquare, //
              comments, version, DATA_SOURCE_RMS_VIEWER);
        } else {
          m = new CheckOutMessage(message, organization, //
              formDateTime, contactName, initialOperators, //
              status, service, band, mode, //
              locationString, formLocation, mgrs, gridSquare, //
              comments, version, DATA_SOURCE_RMS_VIEWER);
        }

        return m;

      } catch (Exception e) {
        return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
      }
    } else {
      try {
        @SuppressWarnings("unchecked")
        var formDataMap = (Map<ExportedKey, Map<String, String>>) mm.getContextObject("formDataMap");
        if (formDataMap == null) {
          return reject(message, RejectType.CANT_FIND_FORMDATA, "no FormData map");
        }

        var messageKey = new ExportedKey(message.from, message.messageId);
        var valueMap = formDataMap.get(messageKey);
        if (valueMap == null) {
          return reject(message, RejectType.CANT_FIND_FORMDATA, "no FormData entry for message: " + messageKey);
        }

        var organization = valueMap.get("0a. Organization");
        var formDateTime = mtdp.parse(valueMap.get("1a. Date-Time"));
        var contactName = valueMap.get("1d. Station Contact Name");
        var initialOperators = valueMap.get("1e. Initial Operators");

        var status = valueMap.get("2a. Type");
        var service = valueMap.get("2b. Service");
        var band = valueMap.get("2c. Band");
        var mode = valueMap.get("2d. Session");

        var locationString = valueMap.get("3a. Location");
        var mgrs = valueMap.get("3d. MGRS");
        var gridSquare = valueMap.get("3e. Grid Square");
        var formLocation = new LatLongPair(valueMap.get("3b. Latitude"), valueMap.get("3c. Longitude"));
        var comments = valueMap.get("4a. Comments");
        var version = valueMap.get("MapFileName").split(" ")[2];

        var m = (ExportedMessage) null;
        if (messageType == MessageType.CHECK_IN) {
          m = new CheckInMessage(message, organization, //
              formDateTime, contactName, initialOperators, //
              status, service, band, mode, //
              locationString, formLocation, mgrs, gridSquare, //
              comments, version, DATA_SOURCE_FORM_DATA);
        } else {
          m = new CheckOutMessage(message, organization, //
              formDateTime, contactName, initialOperators, //
              status, service, band, mode, //
              locationString, formLocation, mgrs, gridSquare, //
              comments, version, DATA_SOURCE_FORM_DATA);
        }

        return m;
      } catch (Exception e) {
        return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
      }
    }
  }

  protected LocalDateTime parseFormDateTime() {
    LocalDateTime formDateTime = null;

    var utcString = getStringFromXml("timestamp2").replaceAll("  ", " ");
    var localString = getStringFromXml("datetime").replaceAll("  ", " ");
    if (utcString != null) {
      try {
        formDateTime = LocalDateTime.from(UTC_PARSER.parse(utcString));
      } catch (Exception e) {
        formDateTime = mtdp.parse(localString);
      }
    } else {
      var s = getStringFromXml("datetime");
      if (s != null) {
        formDateTime = mtdp.parse(localString);
      }
    }

    return formDateTime;
  }

  protected String parseVersion() {
    var version = "";
    var templateVersion = getStringFromXml("templateversion");
    if (templateVersion != null) {
      var fields = templateVersion.split(" ");
      version = fields[fields.length - 1]; // last field
    }
    return version;
  }

}
