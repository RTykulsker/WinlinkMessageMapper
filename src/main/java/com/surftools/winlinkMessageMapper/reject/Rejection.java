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

package com.surftools.winlinkMessageMapper.reject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.surftools.winlinkMessageMapper.dto.ExportedMessage;

public class Rejection implements Comparable<Rejection> {
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

  public final String messageId;
  public final String from;
  public final String to;
  public final String date;
  public final String time;
  public final RejectType reason;
  public final String context;

  public final LocalDateTime dateTime;

  public Rejection(String messageId, String from, String to, String date, String time, RejectType reason,
      String context) {
    this.messageId = messageId;
    this.from = from;
    this.to = to;
    this.date = date;
    this.time = time;
    this.reason = reason;
    this.context = context;

    dateTime = LocalDateTime.parse(date + " " + time, FORMATTER);
  }

  public Rejection(ExportedMessage m, RejectType reason, String context) {
    this.messageId = m.messageId;
    this.from = m.from;
    this.to = m.to;
    this.date = m.date;
    this.time = m.time;
    this.reason = reason;
    this.context = context;

    dateTime = LocalDateTime.parse(date + " " + time, FORMATTER);
  }

  public static String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Date", "Time", "Reason", "Context" };
  }

  public String[] getValues() {
    return new String[] { messageId, from, to, date, time, reason.toString(), context };
  }

  @Override
  public int compareTo(Rejection o) {
    int compare = reason.id() - o.reason.id();
    if (compare != 0) {
      return compare;
    }
    return dateTime.compareTo(o.dateTime);
  }
}
