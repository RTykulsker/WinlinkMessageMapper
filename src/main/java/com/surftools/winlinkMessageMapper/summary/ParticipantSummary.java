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

package com.surftools.winlinkMessageMapper.summary;

import com.surftools.utils.location.LatLongPair;

/**
 * one record/row per participant, per exercise
 *
 * @author bobt
 *
 */
public class ParticipantSummary {

  private String date;
  private String name;
  private String call;
  private LatLongPair lastLocation;

  public ParticipantSummary(String call) {
    this.call = call;
  }

  public ParticipantSummary(String[] fields) {
    date = fields[0];
    name = fields[1];
    call = fields[2];
    lastLocation = new LatLongPair(fields[3], fields[4]);
  }

  @Override
  public String toString() {
    return "ParticipantSummary {date: " + date + ", name: " + name + ", call: " + call + ", lastLocation: "
        + lastLocation + "}";
  }

  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCall() {
    return call;
  }

  public void setCall(String call) {
    this.call = call;
  }

  public LatLongPair getLastLocation() {
    return lastLocation;
  }

  public void setLastLocation(LatLongPair lastLocation) {
    this.lastLocation = lastLocation;
  }

  public static String[] getHeaders() {
    return new String[] { "Exercise Date", "Exercise Name", "Call", "Last Latitude", "Last Longitude" };
  }

  public String[] getValues() {
    return new String[] { date, name, call, //
        ((lastLocation == null) ? "" : lastLocation.getLatitude()), //
        ((lastLocation == null) ? "" : lastLocation.getLongitude()) };
  }

}
