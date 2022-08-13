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

package com.surftools.winlinkMessageMapper.dto.message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class ExportedMessage implements IMessage {
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

  public final String messageId;
  public final String from;
  public final String to;
  public final String toList;
  public final String ccList;
  public final String subject;
  public final String date;
  public final String time;
  public final String mime;

  public final String plainContent;
  public final Map<String, byte[]> attachments;

  public final LocalDateTime dateTime;

  public ExportedMessage(String messageId, String from, String to, String toList, String ccList, //
      String subject, String date, String time, String mime, String plainContent, Map<String, byte[]> attachments) {
    this.messageId = messageId;
    this.from = from;
    this.to = to;
    this.toList = toList;
    this.ccList = ccList;
    this.subject = subject;
    this.date = date;
    this.time = time;
    this.mime = mime;

    this.plainContent = plainContent;
    this.attachments = attachments;

    dateTime = LocalDateTime.parse(date + " " + time, FORMATTER);
  }

  @Override
  public String toString() {
    String attachmentsString = "\n" + attachments.size() + " attachments(" + String.join(",", attachments.keySet())
        + ")\n";
    return "ExportedMessage {messageId: " + messageId + ", from: " + from + ", to: " + to + ", subject: " + subject
        + ", date: " + date + ", time: " + time + ", plainContent: \n" + plainContent + attachmentsString + "}";
  }

  public ExportedMessage(ExportedMessage exportedMessage) {
    this.messageId = exportedMessage.messageId;
    this.from = exportedMessage.from;
    this.to = exportedMessage.to;
    this.toList = exportedMessage.toList;
    this.ccList = exportedMessage.ccList;
    this.subject = exportedMessage.subject;
    this.date = exportedMessage.date;
    this.time = exportedMessage.time;
    this.mime = exportedMessage.mime;

    this.plainContent = exportedMessage.plainContent;
    this.attachments = exportedMessage.attachments;

    dateTime = LocalDateTime.parse(date + " " + time, FORMATTER);
  }

  public String[] getMimeLines() {
    return mime.split("\n");
  }

  public String getPlainContent() {
    return plainContent;
  }

  @Override
  public String[] getHeaders() {
    throw new RuntimeException("ExportedMessage.getHeaders() not implemented");
  }

  @Override
  public String[] getValues() {
    throw new RuntimeException("ExportedMessage.getValues() not implemented");
  }

  public MessageType getMessageType() {
    return MessageType.UNKNOWN;
  }

  /**
   * default behavior will be to return empty string
   *
   * @return
   */
  public String getMultiMessageComment() {
    return "";
  }

  public String notnull(String s) {
    return (s == null) ? "" : s;
  }
}
