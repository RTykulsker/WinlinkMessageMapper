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

import com.surftools.utils.location.LatLongPair;

public class TargetStation {

  public final String band;
  public final String centerFrequency;
  public final String dialFrequency;
  public final String mode;
  public final String region;
  public final String call;
  public final String city;
  public final String state;
  public final String grid;
  public final LatLongPair latLong;

  public TargetStation(String[] fields) {
    int index = 0;
    band = fields[index++].trim();
    centerFrequency = fields[index++].trim();
    dialFrequency = fields[index++].trim();
    mode = fields[index++].trim();
    region = fields[index++].trim();
    call = fields[index++].trim();
    city = fields[index++].trim();
    state = fields[index++].trim();
    grid = fields[index++].trim();
    String latitude = fields[index++].trim();
    String longitude = fields[index++].trim();
    latLong = new LatLongPair(latitude, longitude);
  }

  public TargetStation(TargetStation other, String latitude, String longitude) {
    this.band = other.band;
    this.centerFrequency = other.centerFrequency;
    this.dialFrequency = other.dialFrequency;
    this.mode = other.mode;
    this.region = other.region;
    this.call = other.call;
    this.city = other.city;
    this.state = other.state;
    this.grid = other.grid;
    latLong = new LatLongPair(latitude, longitude);
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
    return "TargetStation {call: " + call + ", city: " + city + ", state: " + state + ", latitude: "
        + latLong.getLatitude() + ", longitude: " + latLong.getLongitude() + "}";
  }

}
