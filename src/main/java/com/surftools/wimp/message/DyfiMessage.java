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

package com.surftools.wimp.message;

import java.time.LocalDateTime;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

public class DyfiMessage extends ExportedMessage {

  public static enum DetailLevel {
    LOW, MEDIUM, HIGH;

    public static DetailLevel fromString(String string) {
      for (DetailLevel detailLevel : DetailLevel.values()) {
        if (detailLevel.toString().equals(string)) {
          return detailLevel;
        }
      }
      return null;
    }
  };

  // how we discriminate between DetailLevel; set in processors

  public static DetailLevel detailLevel = DetailLevel.LOW;

  // common fields
  public final String exerciseId;
  public final boolean isRealEvent;
  public final boolean isFelt;

  public final LocalDateTime formDateTime;
  public final String location;
  public final LatLongPair formLocation;

  // LOW DetailType fields
  public final String response;
  public final String comments;
  public final String intensity;
  public final String formVersion;

  // MEDIUM DetailType fields
  public final String locationSource;

  public final String situation;
  public final String situationOther;

  public final String situationFloor;
  public final String situationFloorOther;

  public final String structureStories;
  public final String structureStoriesOther;

  public final String situationSleep;
  public final String situationOthersFeltIt;

  public final String experienceShaking;
  public final String experienceReaction;
  public final String experienceResponse;
  public final String experienceResponseOther;
  public final String experienceStand;

  public final String effectsDoors;
  public final String effectsSounds;
  public final String effectsShelves;
  public final String effectsPictures;
  public final String effectsFurniture;
  public final String effectsAppliances;
  public final String effectsWalls;

  public final String damageText;
  public final String buildingDamage;
  public final String language;

  // HIGH DetailType fields

  // LOW constructor
  public DyfiMessage(ExportedMessage exportedMessage, //
      String exerciseId, boolean isRealEvent, boolean isFelt, //
      LocalDateTime formDateTime, String location, LatLongPair formLocation, //
      String response, String comments, String intensity, String formVersion) {
    super(exportedMessage);
    this.exerciseId = exerciseId;
    this.isRealEvent = isRealEvent;
    this.isFelt = isFelt;

    this.formDateTime = formDateTime;
    this.location = location;
    this.formLocation = formLocation;

    this.response = response;
    this.comments = comments;
    this.intensity = intensity;
    this.formVersion = formVersion;

    if (formLocation.isValid()) {
      mapLocation = formLocation;
    } else {
      mapLocation = msgLocation;
    }

    if (formDateTime != null) {
      sortDateTime = formDateTime;
    }

    this.locationSource = "";

    this.situation = "";
    this.situationOther = "";

    this.situationFloor = "";
    this.situationFloorOther = "";

    this.structureStories = "";
    this.structureStoriesOther = "";

    this.situationSleep = "";
    this.situationOthersFeltIt = "";

    this.experienceShaking = "";
    this.experienceReaction = "";
    this.experienceResponse = "";
    this.experienceResponseOther = "";
    this.experienceStand = "";

    this.effectsDoors = "";
    this.effectsSounds = "";
    this.effectsShelves = "";
    this.effectsPictures = "";
    this.effectsFurniture = "";
    this.effectsAppliances = "";
    this.effectsWalls = "";

    this.damageText = "";
    this.buildingDamage = "";
    this.language = "";
  }

  // MEDIUM constructor
  public DyfiMessage(ExportedMessage exportedMessage, //
      String exerciseId, boolean isRealEvent, boolean isFelt, //
      LocalDateTime formDateTime, String location, LatLongPair formLocation, //
      String response, String comments, String intensity, String formVersion, //
      String locationSource, String situation, String situationOther, //
      String situationFloor, String situationFloorOther, //
      String structureStories, String structureStoriesOther, //
      String situationSleep, String situationOthersFeltIt, //
      String experienceShaking, String experienceReaction, String experienceResponse, String experienceResponseOther,
      String experienceStand, //
      String effectsDoors, String effectsSounds, String effectsShelves, String effectsPictures, //
      String effectsFurniture, String effectsAppliances, //
      String effectsWalls, String damageText, String buildingDamage, String language //
  ) {
    super(exportedMessage);
    this.exerciseId = exerciseId;
    this.isRealEvent = isRealEvent;
    this.isFelt = isFelt;

    this.formDateTime = formDateTime;
    this.location = location;
    this.formLocation = formLocation;

    this.response = response;
    this.comments = comments;
    this.intensity = intensity;
    this.formVersion = formVersion;

    if (formLocation.isValid()) {
      mapLocation = formLocation;
    } else {
      mapLocation = msgLocation;
    }

    if (formDateTime != null) {
      sortDateTime = formDateTime;
    }

    this.locationSource = locationSource;

    this.situation = situation;
    this.situationOther = situationOther;

    this.situationFloor = situationFloor;
    this.situationFloorOther = situationFloorOther;

    this.structureStories = structureStories;
    this.structureStoriesOther = structureStoriesOther;

    this.situationSleep = situationSleep;
    this.situationOthersFeltIt = situationOthersFeltIt;

    this.experienceShaking = experienceShaking;
    this.experienceReaction = experienceReaction;
    this.experienceResponse = experienceResponse;
    this.experienceResponseOther = experienceResponseOther;
    this.experienceStand = experienceStand;

    this.effectsDoors = effectsDoors;
    this.effectsSounds = effectsSounds;
    this.effectsShelves = effectsShelves;
    this.effectsPictures = effectsPictures;
    this.effectsFurniture = effectsFurniture;
    this.effectsAppliances = effectsAppliances;
    this.effectsWalls = effectsWalls;

    this.damageText = damageText;
    this.buildingDamage = buildingDamage;
    this.language = language;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    if (detailLevel == DetailLevel.LOW) {
      return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
          "Latitude", "Longitude", //
          "ExerciseId", "Location", "IsRealEvent", "IsFelt", "Response", "Comments", "Intensity", "FormVersion",
          "File Name" };
    } else if (detailLevel == DetailLevel.MEDIUM) {
      return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
          "Latitude", "Longitude", //
          "ExerciseId", "Location", "IsRealEvent", "IsFelt", "Response", "Comments", "Intensity", "FormVersion", //
          "Location Source", "Situation", "Situation(other)", //
          "Floor", "Floor(other)", //
          "StructureStories", "StructureStories(other)", //
          "Asleep?", "Others Felt It", //
          "Shaking", "Reaction", "Response", "Response(other)", "Difficult Standing", //
          "Doors", "Sounds", "Shelves", "Pictures", //
          "Furniture", "Appliances", "Walls", //
          "Damage", "Building Damage", "Language", //
      };
    } else {
      throw new RuntimeException("Unsupported detailLevel: " + detailLevel);
    }
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var lat = mapLocation == null ? "" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "" : mapLocation.getLongitude();

    if (detailLevel == DetailLevel.LOW) {
      return new String[] { messageId, from, to, subject, date, time, //
          lat, lon, //
          exerciseId, location, Boolean.toString(isRealEvent), Boolean.toString(isFelt), response, comments, intensity,
          formVersion, fileName };
    } else if (detailLevel == DetailLevel.MEDIUM) {
      return new String[] { messageId, from, to, subject, date, time, //
          lat, lon, //
          exerciseId, location, Boolean.toString(isRealEvent), Boolean.toString(isFelt), //
          response, comments, intensity, formVersion, //
          locationSource, situation, situationOther, //
          situationFloor, situationFloorOther, //
          structureStories, structureStoriesOther, //
          situationSleep, situationOthersFeltIt, //
          experienceShaking, experienceReaction, experienceResponse, experienceResponseOther, experienceStand, //
          effectsDoors, effectsSounds, effectsShelves, effectsPictures, //
          effectsFurniture, effectsAppliances, effectsWalls, //
          damageText, buildingDamage, language, //
      };
    } else {
      throw new RuntimeException("Unsupported detailLevel: " + detailLevel);
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.DYFI;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

  public static void setDetailLevel(DetailLevel _detailLevel) {
    detailLevel = _detailLevel;
  }

  public static DetailLevel getDetailLevel() {
    return detailLevel;
  }

}
