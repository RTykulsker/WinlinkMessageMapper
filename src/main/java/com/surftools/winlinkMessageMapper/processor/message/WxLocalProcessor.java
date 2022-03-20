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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.WxLocalMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;
import com.surftools.winlinkMessageMapper.dto.other.Units;

public class WxLocalProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WxLocalProcessor.class);
  private static Units requiredUnits = Units.IMPERIAL;

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] { "gps", "GPS" };
  private static final String MERGED_LAT_LON_TAG_NAMES;

  private static final String MISSING = "--";
  private static final String SPEED_SUFFFIX = " MPH";
  private static final String TEMP_SUFFIX = " F";

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(MessageType.WX_LOCAL.attachmentName()));

      var latLong = getLatLongFromXml(xmlString, OVERRIDE_LAT_LON_TAG_NAMES);
      if (latLong == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String organization = getStringFromXml(xmlString, "title");
      String temperature = getStringFromXml(xmlString, "temp");
      String windspeed = getStringFromXml(xmlString, "windspeed");

      String unitsString = getStringFromXml(xmlString, "measurmentused");
      temperature = formatTemperature(temperature, unitsString, message.from);
      windspeed = formatWindspeed(windspeed, unitsString, message.from);
      String range = makeRange(temperature);

      String comments = getStringFromXml(xmlString, "comments");
      WxLocalMessage m = new WxLocalMessage(message, latLong.latitude(), latLong.longitude(), //
          organization, temperature, windspeed, range, comments);
      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private String makeRange(String temperatureString) {
    if (temperatureString.startsWith(MISSING)) {
      return MISSING;
    }

    int temp = 0;
    try {
      temperatureString = temperatureString.substring(0, temperatureString.length() - 2);
      temp = (int) Math.round(Double.parseDouble(temperatureString));
    } catch (Exception e) {
      return MISSING;
    }

    if (temp >= 0) {
      if (temp < 10) {
        return "single digits";
      } else if (temp < 20) {
        return "teens";
      } else {
        return "" + (temp / 10) + "0s";
      }
    } else {
      if (temp < -10) {
        return "negative single digits";
      } else if (temp < -20) {
        return "negative teens";
      } else {
        return "really cold";
      }
    } // end if negatives
  }

  private String formatWindspeed(String windspeed, String unitsString, String from) {
    if (windspeed == null || windspeed.equals(MISSING)) {
      return MISSING + SPEED_SUFFFIX;
    }

    if (unitsString == null) {
      unitsString = Units.IMPERIAL.toString();
    }

    if (windspeed.startsWith(MISSING) && windspeed.length() > 2) {
      windspeed = windspeed.substring(2);
    }

    windspeed = windspeed.replaceAll(" ", "");
    String ret = windspeed + SPEED_SUFFFIX;
    Units units = Units.fromString(unitsString);

    Double doubleWindspeed = null;
    try {
      // is it numeric
      doubleWindspeed = Double.parseDouble(windspeed);
    } catch (Exception e) {
      // 6-12? average!
      String[] fields = windspeed.split("-");
      if (fields.length == 2) {
        try {
          double d0 = Double.parseDouble(fields[0]);
          double d1 = Double.parseDouble(fields[1]);
          doubleWindspeed = (d0 + d1) / 2;
        } catch (Exception e2) {
          logger.error("could not parse windspeed: " + windspeed + ", call: " + from);
          ret = MISSING + SPEED_SUFFFIX;
        }
      } else { // end could split into 2 fields
        logger.error("could not parse windspeed: " + windspeed + ", call: " + from);
        ret = MISSING + SPEED_SUFFFIX;
      } // end could split into 2 fields
    }

    if (units != requiredUnits) {
      if (requiredUnits == Units.IMPERIAL) {
        if (units == Units.METRIC) {
          int intWindspeed = (int) Math.round(doubleWindspeed * 0.62137119);
          ret = intWindspeed + SPEED_SUFFFIX;
        } else {
          throw new RuntimeException("don't know how to deal with units: " + units + ", call: " + from);
        } // end if non-metric
      } // end if non-required
    } else {
      int intWindspeed = (int) Math.round(doubleWindspeed);
      ret = intWindspeed + SPEED_SUFFFIX;
    } // end if required units

    return ret;
  }

  private String formatTemperature(String temperature, String unitsString, String from) {
    if (temperature == null) {
      temperature = MISSING;
    }

    if (unitsString == null) {
      unitsString = Units.IMPERIAL.toString();
    }

    if (temperature.startsWith(MISSING) && temperature.length() > 2) {
      temperature = temperature.substring(2);
    }
    temperature = temperature.replaceAll(" ", "");
    String ret = temperature + TEMP_SUFFIX;
    Units units = Units.fromString(unitsString);
    if (units != requiredUnits && !temperature.equals(MISSING)) {
      if (requiredUnits == Units.IMPERIAL) {
        if (units == Units.METRIC) {
          try {
            int d = (int) Math.round((Double.parseDouble(temperature) * 1.8d) + 32);
            ret = Integer.toString(d) + TEMP_SUFFIX;
          } catch (Exception e) {
            logger.error("could not parse temperature: " + temperature + ", call: " + from);
            ret = MISSING + TEMP_SUFFIX;
          }
        } else {
          throw new RuntimeException("don't know how to deal with units: " + units + ", call: " + from);
        } // end if non-supported
      } // end if non-required
    }
    return ret;
  }

}
