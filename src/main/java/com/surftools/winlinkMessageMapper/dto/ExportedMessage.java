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

package com.surftools.winlinkMessageMapper.dto;

public class ExportedMessage implements IMessage {
  public final String messageId;
  public final String from;
  public final String to;
  public final String subject;
  public final String date;
  public final String time;
  public final String mime;

  public ExportedMessage(String messageId, String from, String to, String subject, String date, String time,
      String mime) {
    this.messageId = messageId;
    this.from = from;
    this.to = to;
    this.subject = subject;
    this.date = date;
    this.time = time;
    this.mime = mime;
  }

  @Override
  public String toString() {
    return "ExportedMessage {messageId: " + messageId + ", from: " + from + ", to: " + to + ", subject: " + subject
        + ", date: " + date + ", time: " + time + ", mime: " + mime + "}";
  }

  public ExportedMessage(ExportedMessage xmlMessage) {
    this.messageId = xmlMessage.messageId;
    this.from = xmlMessage.from;
    this.to = xmlMessage.to;
    this.subject = xmlMessage.subject;
    this.date = xmlMessage.date;
    this.time = xmlMessage.time;
    this.mime = xmlMessage.mime;
  }

  public String[] getMimeLines() {
    return mime.split("\n");
  }

  @Override
  public String[] getHeaders() {
    throw new RuntimeException("XmlMessage.getHeaders() not implemented");
  }

  @Override
  public String[] getValues() {
    throw new RuntimeException("XmlMessage.getHeaders() not implemented");
  }

}
