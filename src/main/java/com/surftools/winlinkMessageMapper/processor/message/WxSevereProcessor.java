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
import com.surftools.winlinkMessageMapper.dto.message.WxSevereMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class WxSevereProcessor extends AbstractBaseProcessor {

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
      String xmlString = new String(message.attachments.get(MessageType.WX_SEVERE.attachmentName()));

      var latLong = getLatLongFromXml(xmlString, null);
      if (latLong == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String type = getStringFromXml(xmlString, "type");

      String contactPerson = getStringFromXml(xmlString, "repname");
      String contactPhone = getStringFromXml(xmlString, "phone");
      String contactEmail = getStringFromXml(xmlString, "email");

      String city = getStringFromXml(xmlString, "city");
      String region = getStringFromXml(xmlString, "region");
      String county = getStringFromXml(xmlString, "county");
      String other = getStringFromXml(xmlString, "other");

      String flood = getStringFromXml(xmlString, "flood");
      String hailSize = getStringFromXml(xmlString, "hailsize");
      String windSpeed = getStringFromXml(xmlString, "windspeed");
      String tornado = getStringFromXml(xmlString, "tornado");

      String windDamage = getStringFromXml(xmlString, "winddamage");
      String precipitation = getStringFromXml(xmlString, "precipitation");
      String snow = getStringFromXml(xmlString, "snow");
      String freezingRain = getStringFromXml(xmlString, "freezingrain");

      String rain = getStringFromXml(xmlString, "rain");
      String rainPeriod = getStringFromXml(xmlString, "rainperiod");

      String comments = getStringFromXml(xmlString, "comments");

      WxSevereMessage m = new WxSevereMessage(message, latLong.getLatitude(), latLong.getLongitude(), //
          type, contactPerson, contactPhone, contactEmail, //
          city, region, county, other, //
          flood, hailSize, windSpeed, tornado, //
          windDamage, precipitation, snow, freezingRain, //
          rain, rainPeriod, //
          comments);
      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
