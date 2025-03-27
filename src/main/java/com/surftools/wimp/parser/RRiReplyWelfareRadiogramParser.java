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

package com.surftools.wimp.parser;

import java.io.ByteArrayInputStream;

import javax.mail.internet.MimeUtility;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RRIReplyWelfareRadiogramMessage;

public class RRiReplyWelfareRadiogramParser extends AbstractBaseParser {

  private enum State {
    BEFORE_REPLY, IN_REPLY, IN_SOURCE
  };

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      var mime = message.plainContent;

      var inputStream = MimeUtility.decode(new ByteArrayInputStream(mime.getBytes()), "quoted-printable");
      var demimed = new String(inputStream.readAllBytes());
      String[] mimeLines = demimed.split("\\n");
      var state = State.BEFORE_REPLY;
      var replyStringBuilder = new StringBuilder();
      String replyDateTime = "";
      var sourceStringBuilder = new StringBuilder();
      for (var line : mimeLines) {
        if (state == State.BEFORE_REPLY) {
          if (line.trim().isEmpty()) {
            continue;
          } else {
            state = State.IN_REPLY;
          }
        }

        if (state == State.IN_REPLY) {
          if (line.trim().isEmpty()) {
            continue;
          } else {
            if (line.startsWith("On ") && line.endsWith(" wrote:")) {
              replyDateTime = line;
              state = State.IN_SOURCE;
            } else if (line.startsWith("> ")) {
              state = State.IN_SOURCE;
            } else {
              replyStringBuilder.append(line + "\n");
            }
          }
        }

        if (state == State.IN_SOURCE) {
          if (line.startsWith("> ")) {
            sourceStringBuilder.append(line.substring(2) + "\n");
          }
        }

      }

      String reply = replyStringBuilder.toString();
      if (reply.endsWith("\n")) {
        reply = reply.substring(0, reply.length() - 1);
      }

      String sourceMessage = sourceStringBuilder.toString();
      if (sourceMessage.endsWith("\n")) {
        sourceMessage = sourceMessage.substring(0, sourceMessage.length() - 1);
      }

      var m = new RRIReplyWelfareRadiogramMessage(message, //
          reply, replyDateTime, sourceMessage);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }

  }

  @Override
  public MessageType getMessageType() {
    return MessageType.RRI_REPLY_WELFARE_RADIOGRRAM;
  }

}
