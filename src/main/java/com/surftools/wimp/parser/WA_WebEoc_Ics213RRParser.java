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

import java.util.ArrayList;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage.LineItem;
import com.surftools.wimp.message.WA_WebEoc_Ics213RRMessage;

public class WA_WebEoc_Ics213RRParser extends AbstractBaseParser {

  public WA_WebEoc_Ics213RRParser() {
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      var xmlString = new String(message.attachments.get(MessageType.WA_ICS_213_RR_WEB_EOC.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      var input = new ArrayList<String>();
      input.add(null); // so we can do 1-based indexing in message
      for (var i = 1; i <= 32; ++i) {
        var value = getStringFromXml("input" + i);
        input.add(value);
      }

      var lineItems = new ArrayList<LineItem>();

      var quantity = getStringFromXml("input19");
      var kind = getStringFromXml("input17");
      var type = getStringFromXml("input18");
      var item = getStringFromXml("input16");
      var requestedDateTime = getStringFromXml("input24");
      var estimatedDateTime = "";
      var cost = "";
      var lineItem = new LineItem(quantity, kind, type, item, requestedDateTime, estimatedDateTime, cost);
      lineItems.add(lineItem);

      var templateVersion = getStringFromXml("templateversion");
      var version = "unknown";
      if (templateVersion != null) {
        var prefix = "RR WebEOC WA ";
        var index = templateVersion.indexOf(prefix);
        if (index != -1) {
          version = templateVersion.substring(prefix.length());
        }
      }

      var m = new WA_WebEoc_Ics213RRMessage(message, input, lineItems, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
