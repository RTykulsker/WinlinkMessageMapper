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

import java.util.ArrayList;

import com.surftools.winlinkMessageMapper.dto.other.LatLongPair;

/**
 * one record, per participant, across all exercises
 *
 * @author bobt
 *
 */
public class ParticipantHistory {

  private String call;
  private String lastDate;
  private String lastName;
  private LatLongPair lastLocation;
  private int exerciseCount;
  private int messageCount;

  // category not read but is written!
  private String category;

  @Override
  public String toString() {
    return "{call: " + call + ", lastDate: " + lastDate + ", lastName: " + lastName + ", lastLocation: " + lastLocation
        + ", exerciseCount: " + exerciseCount + ", messageCount: " + messageCount + ", category: " + category + "}";
  }

  public ParticipantHistory(String call) {
    this.call = call;
  }

  public ParticipantHistory(String[] fields) {
    final int requiredFieldCount = 7;
    if (fields.length != requiredFieldCount) {
      throw new IllegalArgumentException("wrong field count: expected: " + requiredFieldCount + ", got: "
          + fields.length + ", " + String.join(",", fields));
    }

    call = fields[0];
    lastDate = fields[1];
    lastName = fields[2];
    lastLocation = new LatLongPair(fields[3], fields[4]);
    exerciseCount = Integer.parseInt(fields[5]);
    messageCount = Integer.parseInt(fields[6]);

  }

  public String getCall() {
    return call;
  }

  public void setCall(String call) {
    this.call = call;
  }

  public String getLastDate() {
    return lastDate;
  }

  public void setLastDate(String lastDate) {
    this.lastDate = lastDate;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public LatLongPair getLastLocation() {
    return lastLocation;
  }

  public void setLastLocation(LatLongPair lastLocation) {
    this.lastLocation = lastLocation;
  }

  public int getExerciseCount() {
    return exerciseCount;
  }

  public void setExerciseCount(int exerciseCount) {
    this.exerciseCount = exerciseCount;
  }

  public int getMessageCount() {
    return messageCount;
  }

  public void setMessageCount(int messageCount) {
    this.messageCount = messageCount;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public static String[] getHeaders() {
    var list = new ArrayList<String>();

    list.add("Call");
    list.add("Last Date");
    list.add("Last Exercise");

    list.add("Last Latitude");
    list.add("Last Longitude");

    list.add("Exercise Count");
    list.add("Message Count");

    list.add("Category");

    String[] array = new String[list.size()];
    list.toArray(array);

    return (array);
  }

  public String[] getValues() {
    var list = new ArrayList<String>();

    list.add(call);
    list.add(lastDate);
    list.add(lastName);

    if (lastLocation == null) {
      list.add("");
      list.add("");
    } else {
      list.add(lastLocation.latitude());
      list.add(lastLocation.longitude());
    }

    list.add(String.valueOf(exerciseCount));
    list.add(String.valueOf(messageCount));

    list.add(category);

    String[] array = new String[list.size()];
    list.toArray(array);

    return (array);
  }

}
