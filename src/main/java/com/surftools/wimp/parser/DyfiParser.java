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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.DyfiMessage.DetailLevel;
import com.surftools.wimp.message.ExportedMessage;

public class DyfiParser extends AbstractBaseParser {

  private static final Logger logger = LoggerFactory.getLogger(DyfiParser.class);

  private final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("M/dd/yyyy HH:mm");

  @SuppressWarnings("unchecked")
  @Override
  public ExportedMessage parse(ExportedMessage message) {

    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.debug("messageId: " + message.messageId + ", from: " + message.from);
    }

    try {
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

      String exerciseId = map.get("exercise_id");
      boolean isRealEvent = !map.get("eventType").equalsIgnoreCase("EXERCISE");
      var fldSituationFelt = map.get("fldSituation_felt");
      boolean isFelt = fldSituationFelt != null ? fldSituationFelt.equals("1") : false;

      LocalDateTime formDateTime = null;
      try {
        formDateTime = LocalDateTime.parse(map.get("ciim_time"), DT_FORMATTER);
      } catch (Exception e) {
        ;
      }

      String location = map.get("ciim_mapAddress");
      String latitude = map.get("ciim_mapLat");
      String longitude = map.get("ciim_mapLon");
      var formLocation = new LatLongPair(latitude, longitude);

      String comments = map.get("comments");
      var intensity = map.get("mapmmscale");
      var formVersion = map.get("form_version");
      var response = map.get("fldExperience_response");

      if (DyfiMessage.getDetailLevel() == DetailLevel.LOW) {
        DyfiMessage m = new DyfiMessage(message, //
            exerciseId, isRealEvent, isFelt, //
            formDateTime, location, formLocation, //
            response, comments, intensity, formVersion);

        return m;
      } else if (DyfiMessage.getDetailLevel() == DetailLevel.MEDIUM) {
        var locationSource = map.get("location_source");
        var situation = map.get("fldSituation_situation");
        var situationOther = map.get("fldSituation_others");

        var situationFloor = map.get("fldSituation_floor");
        var situationFloorOther = map.get("ifHigherPleaseDescribe");

        var structureStories = map.get("fldSituation_structureStories");
        var structureStoriesOther = map.get("howTallPleaseDescribe");

        var situationSleep = map.get("fldSituation_sleep");
        var situationOthersFeltIt = map.get("fldSituation_others");

        var experienceShaking = map.get("fldExperience_shaking");
        var experienceReaction = map.get("fldExperience_reaction");
        var experienceResponse = map.get("fldExperience_response");
        var experienceResponseOther = map.get("fldExperience_response_other");
        var experienceStand = map.get("fldExperience_stand");

        var effectsDoors = map.get("fldEffects_doors");
        var effectsSounds = map.get("fldEffects_sounds");
        var effectsShelves = map.get("fldEffects_shelved"); // typo in form?
        var effectsPictures = map.get("fldEffects_pictures");
        var effectsFurniture = map.get("fldEffects_furniture");
        var effectsAppliances = map.get("fldEffects_appliances");
        var effectsWalls = map.get("fldEffects_walls");

        var damageText = map.get("d_text");
        var buildingDamage = map.get("BuildingDamage");
        var language = map.get("language");

        DyfiMessage m = new DyfiMessage(message, //
            exerciseId, isRealEvent, isFelt, //
            formDateTime, location, formLocation, //
            response, comments, intensity, formVersion, //
            locationSource, situation, situationOther, //
            situationFloor, situationFloorOther, //
            structureStories, structureStoriesOther, //
            situationSleep, situationOthersFeltIt, //
            experienceShaking, experienceReaction, experienceResponse, experienceResponseOther, experienceStand, //
            effectsDoors, effectsSounds, effectsShelves, effectsPictures, //
            effectsFurniture, effectsAppliances, //
            effectsWalls, damageText, buildingDamage, language);
        return m;
      } else {
        throw new RuntimeException("Unsupported detailType: " + DyfiMessage.getDetailLevel());
      }
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.DYFI;
  }

}
