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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.EtoResumeMessage;
import com.surftools.wimp.message.ExportedMessage;

public class EtoResumeParser extends AbstractBaseParser {

  private static final Logger logger = LoggerFactory.getLogger(EtoResumeParser.class);

  private final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

  @SuppressWarnings("unchecked")
  @Override
  public ExportedMessage parse(ExportedMessage message) {

    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.debug("messageId: " + message.messageId + ", from: " + message.from);
    }

    try {
      var mime = message.plainContent;

      var jsonString = mime.trim().replaceAll("\n", "");
      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> map = null;
      try {
        map = mapper.readValue(jsonString, Map.class);
      } catch (Exception e) {
        return reject(message, RejectType.CANT_PARSE_ETO_JSON, e.getMessage());
      }

      var sentBy = map.get("SentBy");

      LocalDateTime formDateTime = null;
      try {
        formDateTime = LocalDateTime.parse(map.get("DateTimeLocal"), DT_FORMATTER);
      } catch (Exception e) {
        ;
      }

      String latitude = map.get("Latitude");
      String longitude = map.get("Longitude");
      var formLocation = new LatLongPair(latitude, longitude);

      var hasIs100 = parseBoolean(map, "IS100");
      var hasIs200 = parseBoolean(map, "IS200");
      var hasIs700 = parseBoolean(map, "IS700");
      var hasIs800 = parseBoolean(map, "IS800");
      var hasAces = parseBoolean(map, "ACES");
      var hasEc001 = parseBoolean(map, "EC001");
      var hasEc016 = parseBoolean(map, "EC016");
      var hasSkywarn = parseBoolean(map, "SKYWARN");
      var hasAuxComm = parseBoolean(map, "AUXCOMM");
      var hasComT = parseBoolean(map, "COMT");
      var hasComL = parseBoolean(map, "COML");

      var agencies = parseAgencies(map);

      var comments = map.get("comments");
      var version = map.get("version");

      var m = new EtoResumeMessage(message, //
          sentBy, formDateTime, formLocation, //
          hasIs100, hasIs200, hasIs700, hasIs800, //
          hasAces, hasEc001, hasEc016, hasSkywarn, //
          hasAuxComm, hasComT, hasComL, //
          agencies, comments, version);

      return m;
    } catch (

    Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private List<String> parseAgencies(Map<String, String> map) {
    var agencies = new ArrayList<String>();
    var value = map.get("Agencies");
    if (value != null) {
      var array = value.split("\\|");
      for (var agency : array) {
        agencies.add(agency.trim());
      }
    }
    return agencies;
  }

  private boolean parseBoolean(Map<String, String> map, String key) {
    var value = map.get(key);
    if (value == null) {
      return false;
    }
    return value.equalsIgnoreCase("YES") ? true : false;
  }
}
