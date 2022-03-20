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

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.WxHurricaneMessage;
import com.surftools.winlinkMessageMapper.dto.other.LatLongPair;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class WxHurricaneProcessor extends AbstractBaseProcessor {

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {
    try {
      String formData = new String(message.attachments.get("FormData.txt"));
      String[] formLines = formData.split("\r\n");
      String latitude = getStringFromFormLines(formLines, "Latitude");
      String longitude = getStringFromFormLines(formLines, "Longitude");
      var latLong = new LatLongPair(latitude, longitude);
      if (!latLong.isValid()) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String status = getStringFromFormLines(formLines, "Status");
      String isObserver = getStringFromFormLines(formLines, "Is Sending Station the Observing Party");
      String observerPhone = getStringFromFormLines(formLines, "Reporting Observer Phone Number");
      String observerEmail = getStringFromFormLines(formLines, "Reporting Observer Email");

      String city = getStringFromFormLines(formLines, "City");
      String county = getStringFromFormLines(formLines, "County");
      String state = getStringFromFormLines(formLines, "State");
      String country = getStringFromFormLines(formLines, "Country");

      String instrumentsUsed = getStringFromFormLines(formLines, "Weather Instruments Used");
      String windSpeed = getStringFromFormLines(formLines, "Wind Speed");
      String gustSpeed = getStringFromFormLines(formLines, "Gust Speed");
      String windDirection = getStringFromFormLines(formLines, "Wind Direction");
      String barometricPressure = getStringFromFormLines(formLines, "Barometric Pressure");

      String comments = getStringFromFormLines(formLines, "Event Comments");

      WxHurricaneMessage m = new WxHurricaneMessage(message, latLong.latitude(), latLong.longitude(), //
          status, isObserver, observerPhone, observerEmail, //
          city, county, state, country, //
          instrumentsUsed, windSpeed, gustSpeed, windDirection, barometricPressure, //
          comments);
      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
