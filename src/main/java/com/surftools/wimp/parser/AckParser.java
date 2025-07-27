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

import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.AckMessage;
import com.surftools.wimp.message.ExportedMessage;

public class AckParser extends AbstractBaseParser {

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      var plainContent = message.getPlainContent();
      var lines = plainContent.split("\n");

      var originalSubject = getStringFromFormLines(lines, ": ", "Subject");
      var sender = getStringFromFormLines(lines, ": ", "Sender");
      var originalTo = getStringFromFormLines(lines, ": ", "To");
      var received = getStringFromFormLines(lines, ": ", "Received");
      var acknowledged = getStringFromFormLines(lines, ": ", "Acknowledged");
      var originalId = getStringFromFormLines(lines, ": ", "Message-ID");
      var nAttachments = getStringFromFormLines(lines, ": ", "Number of attachments");
      var size = getStringFromFormLines(lines, ": ", "Size");

      var m = new AckMessage(message, //
          originalSubject, sender, originalTo, received, acknowledged, originalId, nAttachments, size);
      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
