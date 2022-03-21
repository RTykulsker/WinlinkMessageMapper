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

package com.surftools.winlinkMessageMapper.dto.message;

import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class WxSevereMessage extends GisMessage {
  public final String type;
  public final String contactPerson;
  public final String contactPhone;
  public final String contactEmail;
  public final String city;
  public final String region;
  public final String county;
  public final String other;
  public final String flood;
  public final String hailSize;
  public final String windSpeed;
  public final String tornado;
  public final String windDamage;
  public final String precipitation;
  public final String snow;
  public final String freezingRain;
  public final String rain;
  public final String rainPeriod;
  public final String comments;

  public WxSevereMessage(ExportedMessage xmlMessage, String latitude, String longitude, //
      String type, String contactPerson, String contactPhone, String contactEmail, //
      String city, String region, String county, String other, //
      String flood, String hailSize, String windSpeed, String tornado, //
      String windDamage, String precipitation, String snow, String freezingRain, //
      String rain, String rainPeriod, //
      String comments) {
    super(xmlMessage, latitude, longitude, "");
    this.type = type;
    this.contactPerson = contactPerson;
    this.contactPhone = contactPhone;
    this.contactEmail = contactEmail;
    this.city = city;
    this.region = region;
    this.county = county;
    this.other = other;
    this.flood = flood;
    this.hailSize = hailSize;
    this.windSpeed = windSpeed;
    this.tornado = tornado;
    this.windDamage = windDamage;
    this.precipitation = precipitation;
    this.snow = snow;
    this.freezingRain = freezingRain;
    this.rain = rain;
    this.rainPeriod = rainPeriod;
    this.comments = comments;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Type", "ContactPerson", "ContactPhone", "ContactEmail", //
        "City", "Region", "County", "Other", //
        "Flood", "HailSize", "WindSpeed", "Tornado", //
        "WindDamage", "Precipitation", "Snow", "FreezingRain", //
        "Rain", "RainPeriod", //
        "Comments" };
  }

  @Override
  public String[] getValues() {
    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        type, contactPerson, contactPhone, contactEmail, //
        city, region, county, other, //
        flood, hailSize, windSpeed, tornado, //
        windDamage, precipitation, snow, freezingRain, //
        rain, rainPeriod, //
        comments };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WX_SEVERE;
  }
}
