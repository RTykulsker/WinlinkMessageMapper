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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.MultiDateTimeParser;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.WxLocalMessage;

public class WxLocalParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(WxLocalParser.class);

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] { "gps", "GPS" };
  private static final String MERGED_LAT_LON_TAG_NAMES;

  private static final String MISSING = "--";
  private static final String SPEED_SUFFFIX = " MPH";
  private static final String TEMP_SUFFIX = " F";

  private static MultiDateTimeParser mdtp;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();

    mdtp = new MultiDateTimeParser(List
        .of(//
            "yyyy-MM-dd HH:mm:ss", "MM/dd/yyyy HH:mm:ss", //
            "yyyy/MM/dd HH:mm:ss", //
            "yyyy-MM-dd HH:mm:ss'Z'", "yyyy-MM-dd HH:mm:ss 'GMT'", "yyyy-MM-dd HH:mm:ss 'local time'", //
            "yyyy-M-dd HH:mm", "M/d/yyyy HHmm", "M-d-yyyy", //
            "M/d/yyyy HH:mm:ss", "M/d/yyyy HH:mm:ss a", "M/d/yyyy h:mm:ss a", "M/d/yyyy  h:mm:ss a", //
            "MM/dd/yyyy HH:mm a", "MM/dd/yyyy HH:mm 'PM'", "MM/dd/yyyy HH:mm  'AM'", "yyyy-MM-dd HH:mm a"
        //
        ));
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(MessageType.WX_LOCAL.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      String organization = getStringFromXml("title");

      var formLocation = getLatLongFromXml(OVERRIDE_LAT_LON_TAG_NAMES);
      if (formLocation == null || !formLocation.isValid()) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      LocalDateTime formDateTime = null;
      var formDateTimeString = getStringFromXml("datetime");
      try {
        formDateTime = mdtp.parse(formDateTimeString.replaceAll("  ", " "));
      } catch (Exception e) {
        logger.warn("### could not parse datetime: " + formDateTimeString + " for " + message.from);
      }

      var locationString = getStringFromXml("location");
      var city = getStringFromXml("city");
      var state = getStringFromXml("state");
      var county = getStringFromXml("county");

      String temperature = getStringFromXml("temp");
      String windspeed = getStringFromXml("windspeed");

      String unitsString = getStringFromXml("measurmentused");
      temperature = formatTemperature(temperature, unitsString, message.from);
      windspeed = formatWindspeed(windspeed, unitsString, message.from);
      String range = makeRange(temperature);
      String maxGusts = getStringFromXml("maxgusts");
      maxGusts = formatWindspeed(maxGusts, unitsString, message.from);

      var warningType = getStringFromXml("warning");
      var warningField = getStringFromXml("warningfld");

      String comments = getStringFromXml("comments");
      WxLocalMessage m = new WxLocalMessage(message, organization, formLocation, //
          formDateTime, locationString, city, state, county, //
          temperature, windspeed, range, maxGusts, warningType, warningField, comments);
      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WX_LOCAL;
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

    if (windspeed.isBlank()) {
      windspeed = "0.0";
    }

    var isMetric = (unitsString.equals("Metric"));

    if (windspeed.startsWith(MISSING) && windspeed.length() > 2) {
      windspeed = windspeed.substring(2);
    }

    windspeed = windspeed.replaceAll(" ", "");
    windspeed = windspeed.replaceAll(",", ".");
    String ret = windspeed + SPEED_SUFFFIX;

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

    if (isMetric) {
      int intWindspeed = (int) Math.round(doubleWindspeed * 0.62137119);
      ret = intWindspeed + SPEED_SUFFFIX;
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

    var isMetric = (unitsString.equals("Metric"));

    if (temperature.startsWith(MISSING) && temperature.length() > 2) {
      temperature = temperature.substring(2);
    }
    temperature = temperature.replaceAll(" ", "");
    temperature = temperature.replaceAll(",", ".");
    String ret = temperature + TEMP_SUFFIX;

    if (isMetric) {
      try {
        int d = (int) Math.round((Double.parseDouble(temperature) * 1.8d) + 32);
        ret = Integer.toString(d) + TEMP_SUFFIX;
      } catch (Exception e) {
        logger.warn("could not parse temperature: " + temperature + ", call: " + from);
        ret = MISSING + TEMP_SUFFIX;
      }
    } // end if non-required

    return ret;
  }

}
