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

package com.surftools.winlinkMessageMapper.dto.p2p;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.surftools.utils.location.LatLongPair;

public class FieldStation implements Comparable<FieldStation> {
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

  public final String messageId;
  public final String from;
  public final String to;
  public final String subject;
  public final String date;
  public final String time;
  public final LatLongPair latLong;
  public final String organization;
  public final String comments;

  public final LocalDateTime dateTime;

  public FieldStation(String[] fields) {
    int index = 0;

    messageId = fields[index++].trim();
    from = fields[index++].trim();
    to = fields[index++].trim();
    subject = fields[index++].trim();
    date = fields[index++].trim();
    time = fields[index++].trim();
    String latitude = fields[index++].trim();
    String longitude = fields[index++].trim();
    organization = fields[index++].trim();
    comments = fields[index++].trim();

    latLong = new LatLongPair(latitude, longitude);

    dateTime = LocalDateTime.parse(date + " " + time, FORMATTER);
  }

  public LatLongPair getLatLongPair() {
    return latLong;
  }

  public String getLatitude() {
    return latLong.getLatitude();
  }

  public String getLongitude() {
    return latLong.getLongitude();
  }

  @Override
  public String toString() {
    return "FieldStation {from: " + from + ", to: " + to + ", latitude: " + latLong.getLatitude() + ", longitude: "
        + latLong.getLongitude() + "}";
  }

  @Override
  public int compareTo(FieldStation o) {
    return dateTime.compareTo(o.dateTime);
  }

}
