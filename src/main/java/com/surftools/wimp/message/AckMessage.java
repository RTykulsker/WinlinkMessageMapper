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

package com.surftools.wimp.message;

import com.surftools.wimp.core.MessageType;

public class AckMessage extends ExportedMessage {
  public final String originalSubject;
  public final String sender;
  public final String originalTo;
  public final String received;
  public final String acknowledged;
  public final String originalId;
  public final String nAttachments;
  public final String size;

  public AckMessage(ExportedMessage exportedMessage, String originalSubject, String sender, String to, String received,
      String acknowledged, String originalId, String nAttachments, String size) {
    super(exportedMessage);

    this.originalSubject = originalSubject;
    this.sender = sender;
    this.originalTo = to;
    this.received = received;
    this.acknowledged = acknowledged;
    this.originalId = originalId;
    this.nAttachments = nAttachments;
    this.size = size;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "OrigSubject", "Sender", "OrigTo", "Received", "Acknowledged", "OrigMessageId", "NAttachments", "Size",
        "File Name" };

  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    return new String[] { messageId, from, to, subject, date, time, //
        originalSubject, sender, originalTo, received, acknowledged, originalId, nAttachments, size, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ACK;
  }
}
