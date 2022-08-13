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

import com.surftools.utils.location.LatLongPair;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

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
  private int messageVersion;
  private MessageCounts messageCounts;
  private int rejectsVersion;
  private RejectCounts rejectCounts;

  public ParticipantSummary(String call) {
    this.call = call;
    this.messageCounts = new MessageCounts();
    this.rejectCounts = new RejectCounts();
    this.messageVersion = MessageType.values().length;
    this.rejectsVersion = RejectType.values().length;
  }

  public ParticipantSummary(String[] fields) {
    date = fields[0];
    name = fields[1];
    call = fields[2];
    lastLocation = new LatLongPair(fields[3], fields[4]);

    messageVersion = Integer.parseInt(fields[5]);
    messageCounts = new MessageCounts(fields, 6);

    int index = 6 + messageVersion;
    rejectsVersion = Integer.parseInt(fields[index]);
    rejectCounts = new RejectCounts(fields, index + 1);
  }

  @Override
  public String toString() {
    return "ParticipantSummary {date: " + date + ", name: " + name + ", call: " + call + ", lastLocation: "
        + lastLocation + ", messageCounts: " + messageCounts + ", rejectCounts: " + rejectCounts + "}";
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

  public int getMessageVersion() {
    return messageVersion;
  }

  public void setMessageVersion(int messageVersion) {
    this.messageVersion = messageVersion;
  }

  public MessageCounts getMessageCounts() {
    return messageCounts;
  }

  public void setMessageCounts(MessageCounts messageCounts) {
    this.messageCounts = messageCounts;
  }

  public int getRejectsVersion() {
    return rejectsVersion;
  }

  public void setRejectsVersion(int rejectsVersion) {
    this.rejectsVersion = rejectsVersion;
  }

  public RejectCounts getRejectCounts() {
    return rejectCounts;
  }

  public void setRejectCounts(RejectCounts rejectCounts) {
    this.rejectCounts = rejectCounts;
  }

  public static String[] getHeaders() {
    var list = new ArrayList<String>();

    list.add("Exercise Date");
    list.add("Exercise Name");
    list.add("Call");
    list.add("Last Latitude");
    list.add("Last Longitude");

    list.add("Message Version");
    list.addAll(MessageCounts.getHeaders());

    list.add("Rejects Version");
    list.addAll(RejectCounts.getHeaders());

    String[] array = new String[list.size()];
    list.toArray(array);

    return (array);
  }

  public String[] getValues() {
    var list = new ArrayList<String>();

    list.add(date);
    list.add(name);
    list.add(call);

    if (lastLocation == null) {
      list.add("");
      list.add("");
    } else {
      list.add(lastLocation.getLatitude());
      list.add(lastLocation.getLongitude());
    }

    list.add(String.valueOf(messageVersion));
    list.addAll(messageCounts.getValues());

    list.add(String.valueOf(rejectsVersion));
    list.addAll(rejectCounts.getValues());

    String[] array = new String[list.size()];
    list.toArray(array);

    return (array);
  }

}
