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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.EtoCheckInV2Message;
import com.surftools.wimp.message.ExportedMessage;

public class EtoCheckInV2Parser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(EtoCheckInV2Parser.class);

  @SuppressWarnings("unchecked")
  @Override
  public ExportedMessage parse(ExportedMessage message) {
    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> map = null;
    try {
      var jsonString = message.plainContent;

      var plainContext = message.plainContent;
      var beginIndex = plainContext.indexOf("{");
      var endIndex = plainContext.lastIndexOf("}") + 1;
      jsonString = plainContext.substring(beginIndex, endIndex);

      jsonString = jsonString.replaceAll("\n", "");
      map = mapper.readValue(jsonString, Map.class);
    } catch (Exception e) {
      return reject(message, RejectType.CANT_PARSE_ETO_JSON, e.getMessage());
    }

    String latitude = map.get("Latitude");
    String longitude = map.get("Longitude");
    var formLocation = new LatLongPair(latitude, longitude);
    var comments = map.get("Comments");
    var formName = map.get("FormName");
    var version = map.get("Version");
    var dateTimeLocal = map.get("DateTimeLocal");
    var dateString = "";
    var timeString = "";
    if (dateTimeLocal != null) {
      var fields = dateTimeLocal.split(" ");
      if (fields.length == 2) {
        dateString = fields[0].trim();
        timeString = fields[1].trim();
      }
    }

    var m = new EtoCheckInV2Message(message, formLocation, comments, //
        dateString, timeString, formName, version);

    return m;
  }

}
