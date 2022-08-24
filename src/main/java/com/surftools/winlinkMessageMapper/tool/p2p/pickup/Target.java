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

package com.surftools.winlinkMessageMapper.tool.p2p.pickup;

import java.util.Set;
import java.util.TreeSet;

import com.surftools.utils.location.LatLongPair;

public class Target {
  public String call;
  public String band;
  public String dialFreq;
  public String location;
  public LatLongPair latlong;
  public Set<String> reservedCalls;
  public Set<String> completedCalls;
  public double completionPercent;
  public int completionScore;
  public boolean ignore;

  public Target(String[] fields) {
    // ChannelId Band Center Dial Mode Region Station Location Latitude Longitude Ignore
    // 0---------1----2------3----4----5------6-------7--------8--------9---------10
    this.band = fields[2];
    this.dialFreq = fields[3];
    this.call = fields[6];
    this.location = fields[7];
    this.latlong = new LatLongPair(fields[8], fields[9]);
    this.reservedCalls = new TreeSet<>();
    this.completedCalls = new TreeSet<>();
    this.ignore = Boolean.parseBoolean(fields[10]);
  }

  @Override
  public String toString() {
    return "{call: " + call + ", location:" + location + ", latlon: " + latlong + "}";
  }

  public static String[] getHeaders() {
    return new String[] { "call", "location", "latitude", "longitude", //
        "completedPercent", "completedScore", //
        "reservedCount", "completedCount", //
        "reserved", "completed" };
  }

  public String[] getValues() {
    return new String[] { call, location, latlong.getLatitude(), latlong.getLongitude(), //
        String.format("%.2f", 100d * completionPercent) + "%", String.valueOf(completionScore), //
        String.valueOf(reservedCalls.size()), String.valueOf(completedCalls.size()), //
        String.join(",", reservedCalls), String.join(",", completedCalls) };
  }

  public String getLatitude() {
    return latlong.getLatitude();
  }

  public String getLongitude() {
    return latlong.getLongitude();
  }

}
