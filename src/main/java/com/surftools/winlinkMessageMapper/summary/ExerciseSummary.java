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

import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

/**
 * typically one row per exercise
 *
 * @author bobt
 *
 */
public class ExerciseSummary {
  private String date;
  private String name;
  private String description;
  private int totalMessages;
  private int uniqueParticipants;
  private int messageVersion;
  private MessageCounts messageCounts;
  private int rejectsVersion;
  private RejectCounts rejectCounts;

  public ExerciseSummary(String date, String name, String description, int totalMessages, int uniqueParticipants,
      int messageVersion, MessageCounts messageCounts, int rejectsVersion, RejectCounts rejectCounts) {
    super();
    this.date = date;
    this.name = name;
    this.description = description;
    this.totalMessages = totalMessages;
    this.uniqueParticipants = uniqueParticipants;
    this.messageVersion = messageVersion;
    this.messageCounts = messageCounts;
    this.rejectsVersion = rejectsVersion;
    this.rejectCounts = rejectCounts;
  }

  public ExerciseSummary(String[] fields) {
    final int requiredFieldCount = 5 + 2 + MessageType.values().length + RejectType.values().length;
    if (fields.length != requiredFieldCount) {
      throw new IllegalArgumentException("wrong field count: expected: " + requiredFieldCount + ", got: "
          + fields.length + ", " + String.join(",", fields));
    }

    date = fields[0];
    name = fields[1];
    description = fields[2];
    totalMessages = Integer.parseInt(fields[3]);
    uniqueParticipants = Integer.parseInt(fields[4]);
    messageVersion = Integer.parseInt(fields[5]);
    messageCounts = new MessageCounts(fields, 6);
    int index = 6 + MessageType.values().length;
    rejectsVersion = Integer.parseInt(fields[index]);
    rejectCounts = new RejectCounts(fields, index + 1);
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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public int getTotalMessages() {
    return totalMessages;
  }

  public void setTotalMessages(int totalMessages) {
    this.totalMessages = totalMessages;
  }

  public int getUniqueParticipants() {
    return uniqueParticipants;
  }

  public void setUniqueParticipants(int uniqueParticipants) {
    this.uniqueParticipants = uniqueParticipants;
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
    list.add("Exercise Description");
    list.add("Total Messages");
    list.add("Unique Participants");

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
    list.add(description);
    list.add(String.valueOf(totalMessages));
    list.add(String.valueOf(uniqueParticipants));

    list.add(String.valueOf(messageVersion));
    list.addAll(messageCounts.getValues());

    list.add(String.valueOf(rejectsVersion));
    list.addAll(rejectCounts.getValues());

    String[] array = new String[list.size()];
    list.toArray(array);

    return (array);
  }

}
