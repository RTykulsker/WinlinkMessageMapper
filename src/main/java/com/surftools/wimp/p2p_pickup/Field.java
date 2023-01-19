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

package com.surftools.wimp.p2p_pickup;

import java.util.Set;
import java.util.TreeSet;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;

public class Field implements IWritableTable {
  public String call;
  public LatLongPair latlong;
  public Set<String> reservedCalls;
  public Set<String> completedCalls;
  public double completionPercent;
  public int completionScore;

  public Field(String[] fields) {
    this.call = fields[1];
    this.latlong = new LatLongPair(fields[6], fields[7]);
    this.reservedCalls = new TreeSet<>();
    this.completedCalls = new TreeSet<>();
  }

  @Override
  public String toString() {
    return "{call: " + call + ", latlon: " + latlong + "}";
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "call", "latitude", "longitude", //
        "completedPercent", "completedScore", //
        "reservedCount", "completedCount", //
        "reserved", "completed" };
  }

  @Override
  public String[] getValues() {
    return new String[] { call, latlong.getLatitude(), latlong.getLongitude(), //
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

  @Override
  public int compareTo(IWritableTable other) {
    var o = (Field) other;
    return call.compareTo(o.call);
  }

}
