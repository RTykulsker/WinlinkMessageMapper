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

  public ExerciseSummary(String date, String name, String description, int totalMessages, int uniqueParticipants) {
    super();
    this.date = date;
    this.name = name;
    this.description = description;
    this.totalMessages = totalMessages;
    this.uniqueParticipants = uniqueParticipants;
  }

  public ExerciseSummary(String[] fields) {
    date = fields[0];
    name = fields[1];
    description = fields[2];
    totalMessages = Integer.parseInt(fields[3]);
    uniqueParticipants = Integer.parseInt(fields[4]);
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

  public static String[] getHeaders() {
    return new String[] { "Exercise Date", "Exercise Time", "Exercise Description", "Total Messages",
        "Unique Participants" };
  }

  public String[] getValues() {
    return new String[] { date, name, description, String.valueOf(totalMessages), String.valueOf(uniqueParticipants) };
  }

}
