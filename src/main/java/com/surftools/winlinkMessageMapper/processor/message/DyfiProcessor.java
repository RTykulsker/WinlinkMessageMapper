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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.winlinkMessageMapper.dto.message.DyfiMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class DyfiProcessor extends AbstractBaseProcessor {

  private static final Logger logger = LoggerFactory.getLogger(DyfiProcessor.class);

  @SuppressWarnings("unchecked")
  @Override
  public ExportedMessage process(ExportedMessage message) {

    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    var mime = message.plainContent;

    final var BEGIN_JSON = "--- BEGIN json ---";
    final var END_JSON = "--- END json ---";

    if (!mime.contains(BEGIN_JSON)) {
      return reject(message, RejectType.CANT_PARSE_DYFI_JSON, BEGIN_JSON);
    }

    if (!mime.contains(END_JSON)) {
      return reject(message, RejectType.CANT_PARSE_DYFI_JSON, END_JSON);
    }

    String jsonString = mime.substring(mime.indexOf(BEGIN_JSON) + BEGIN_JSON.length(), mime.indexOf(END_JSON));
    jsonString = jsonString.trim();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> map = null;
    try {
      map = mapper.readValue(jsonString, Map.class);
    } catch (Exception e) {
      return reject(message, RejectType.CANT_PARSE_DYFI_JSON, e.getMessage());
    }

    String latitude = map.get("ciim_mapLat");
    String longitude = map.get("ciim_mapLon");
    if (latitude == null || latitude.length() == 0 || longitude == null || longitude.length() == 0) {
      var location = message.location;
      if (location == null || !location.isValid()) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, "lat: " + latitude + ", lon: " + longitude);
      }
      latitude = location.getLatitude();
      longitude = location.getLongitude();
      if (latitude == null || latitude.length() == 0 || longitude == null || longitude.length() == 0) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, "lat: " + latitude + ", lon: " + longitude);
      }
    }

    String exerciseId = map.get("exercise_id");
    String comments = map.get("comments");
    String location = map.get("ciim_mapAddress");
    boolean isRealEvent = !map.get("eventType").equalsIgnoreCase("EXERCISE");
    var fldSituationFelt = map.get("fldSituation_felt");
    boolean isFelt = fldSituationFelt != null ? fldSituationFelt.equals("1") : false;
    // boolean isFelt = map.get("fldSituation_felt").equals("1");
    var intensity = map.get("mapmmscale");
    var formVersion = map.get("form_version");
    var response = map.get("fldExperience_response");

    DyfiMessage m = new DyfiMessage(message, latitude, longitude, exerciseId, location, isRealEvent, isFelt, response,
        comments, intensity, formVersion);

    return m;
  }

}
