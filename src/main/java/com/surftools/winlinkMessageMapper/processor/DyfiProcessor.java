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

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.winlinkMessageMapper.dto.DyfiMessage;
import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.reject.MessageOrRejectionResult;
import com.surftools.winlinkMessageMapper.reject.RejectType;

public class DyfiProcessor extends AbstractBaseProcessor {

  @SuppressWarnings("unchecked")
  @Override
  public MessageOrRejectionResult process(ExportedMessage message) {

    var mime = message.mime;

    final var BEGIN_JSON = "--- BEGIN json ---";
    final var END_JSON = "--- END json ---";

    if (!mime.contains(BEGIN_JSON)) {
      return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_DYFI_JSON, BEGIN_JSON);
    }

    if (!mime.contains(END_JSON)) {
      return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_DYFI_JSON, END_JSON);
    }

    String jsonString = mime.substring(mime.indexOf(BEGIN_JSON) + BEGIN_JSON.length(), mime.indexOf(END_JSON));
    jsonString = jsonString.trim();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> map = null;
    try {
      map = mapper.readValue(jsonString, Map.class);
    } catch (Exception e) {
      return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_DYFI_JSON, e.getMessage());
    }

    String latitude = map.get("ciim_mapLat");
    String longitude = map.get("ciim_mapLon");
    String exerciseId = map.get("exercise_id");
    if (latitude == null || latitude.length() == 0 || longitude == null || longitude.length() == 0) {
      return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_LATLONG,
          "lat: " + latitude + ", lon: " + longitude);
    }

    String comments = map.get("comments");
    String location = map.get("ciim_mapAddress");
    boolean isRealEvent = !map.get("eventType").equalsIgnoreCase("EXERCISE");
    boolean isFelt = map.get("fldSituation_felt").equals("1");
    var intensity = makeIntensityResult(computeIntensity(map));
    String intensityString = intensity == null ? "" : intensity.intensity;

    DyfiMessage m = new DyfiMessage(message, latitude, longitude, exerciseId, location, isRealEvent, isFelt, comments,
        intensityString);

    return new MessageOrRejectionResult(m, null);

  }

  private int jsonValueParseInt(String s) {
    try {
      String[] fields = s.split(" ");
      return Integer.parseInt(fields[0]);
    } catch (Exception e) {
      return 0;
    }
  }

  private double computeFelt(Map<String, String> json) {
    int other = jsonValueParseInt(json.get("fldSituation_others"));
    int felt = jsonValueParseInt(json.get("fldSituation_felt"));
    double value = 0;

    // this is how the radio buttons are laid out
    if (other <= 2) {
      other = 0;
    }

    if (other == 3) {
      value = 0.72;
    } else if (other > 3) {
      value = 1d;
    } else {
      value = felt;
    }
    return value;
  }

  private double computeDamage(String text) {
    final String[] DAMAGE1 = new String[] { "_move", "_chim", "_found", "_collapse", "_porch", "_majormodernchim",
        "_tiltedwall" };
    final String[] DAMAGE2 = new String[] { "_wall", "_pipe", "_win", "_brokenwindows", "_majoroldchim",
        "_masonryfell" };
    final String[] DAMAGE3 = new String[] { "_crackwallmany", "_crackwall", "_crackfloor", "_crackchim", "_tilesfell" };
    final String[] DAMAGE4 = new String[] { "_crackwallfew" };
    final String[] DAMAGE5 = new String[] { "_crackmin", "_crackwindows" };

    final Map<String[], Double> damageMap = Map
        .of( //
            DAMAGE1, 3d, //
            DAMAGE2, 2d, //
            DAMAGE3, 1d, //
            DAMAGE4, 0.75d, //
            DAMAGE5, 0.5d //
        );

    if (text == null) {
      return 0;
    }

    for (Map.Entry<String[], Double> entry : damageMap.entrySet()) {
      for (String s : entry.getKey()) {
        if (text.contains(s)) {
          return entry.getValue();
        }
      }
    }
    return 0;
  }

  /**
   * see: https://github.com/usgs/earthquake-dyfi-response/blob/master/src/htdocs/inc/response.inc.php#L39-L75
   *
   * @param json
   * @return
   */

  public int computeIntensity(Map<String, String> json) {

    // if not felt, return 1;
    if (!json.get("fldSituation_felt").equals("1")) {
      return 1;
    }

    final Map<String, Double> CDI_MAP = Map
        .of( //
            "felt", 5d, //
            "fldExperience_shaking", 1d, //
            "fldExperience_reaction", 1d, //
            "fldExperience_stand", 2d, //
            "fldEffects_shelved", 5d, //
            "fldEffects_pictures", 2d, //
            "fldEffects_furniture", 3d, //
            "damage", 5d //
        );

    double cwsSum = 0d;
    for (Map.Entry<String, Double> entry : CDI_MAP.entrySet()) {
      double value = 0d;
      String key = entry.getKey();
      double weight = entry.getValue();
      switch (key) {
      case "felt":
        value = computeFelt(json);
        break;
      case "damage":
        String text = json.get("d_text");
        value = computeDamage(text);
        break;
      default:
        value = jsonValueParseInt(json.get(key));
        break;
      } // end switch key
      cwsSum += (value * weight);
    } // end for over CDI_MAP

    if (cwsSum <= 0) {
      return 1;
    }

    double cdi = (3.3996 * Math.log(cwsSum)) - 4.3781;

    if (cdi <= 1) {
      return 1;
    }

    if (cdi <= 2) {
      return 2;
    }

    return (int) Math.round(cdi);
  }

  private IntensityResult makeIntensityResult(int value) {
    final String[] SCALE_STRINGS = new String[] { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI",
        "XII" };
    final String[] STRENGTH_STRINGS = new String[] { "", "Not Felt", "Weak", "Weak", "Light", "Moderate", "Strong",
        "Very Strong", "Severe", "Violent", "Extreme", "Extreme", "Extreme" };
    String[] DESCRIPTION_STRINGS = new String[] { "", //
        "Not felt except by a very few under especially favorable conditions.", //
        "Felt only by a few persons at rest, especially on upper floors of buildings. Delicately suspended objects may swing.", //
        "Felt quite noticeably by persons indoors, especially on upper floors of buildings. Many people do not recognize it as an earthquake. Standing motor cars may rock slightly. Vibration similar to the passing of a truck. Duration estimated.", //
        "Felt indoors by many, outdoors by few during the day. At night, some awakened. Dishes, windows, doors disturbed; walls make cracking sound. Sensation like heavy truck striking building. Standing motor cars rocked noticeably.", //
        "Felt by nearly everyone; many awakened. some dishes, windows broken. Unstable objects overturned. Pendulum clocks may stop.", //
        " Felt by all, many frightened. Some heavy furniture moved; a few instances of fallen plaster. Damage slight.", //
        "Damage negligible in buildings of good design and construction; slight to moderate in well-built ordinary structures; considerable damage in poorly built or badly designed structures; some chimneys broken.", //
        "Damage slight in specially designed structures; considerable damage in ordinary substantial buildings with partial collapse. Damage great in poorly built structures. Fall of chmineys, factory stacks, columns, monuments, walls. Heavy furniture overturned.", //
        " Damage considerable in specially designed structures; well-designed frame structures thrown out of plumb. Damage great in substantial buildings, with partial collapse. Buildings shifted off foundations.", //
        "Some well-built wooden structures destroyed; most masonry and frame structures destroyed with foundations. Rail bent.", //
        "Few, if any (masonry) structures remain standing. Bridges destroyed. Rails bent greatly.", //
        "Damage total. Lines of sight and level are distorted. Objects thrown into the air."//
    };

    return new IntensityResult(value, DESCRIPTION_STRINGS[value], SCALE_STRINGS[value], STRENGTH_STRINGS[value]);
  }

  private record IntensityResult(int value, String description, String intensity, String strenth) {
  }

}
