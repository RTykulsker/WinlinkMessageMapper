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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.HumanitarianNeedsMessage;

public class HumanitarianNeedsParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(HumanitarianNeedsParser.class);
  private static final String[] mapLocationTags = new String[] { "maplat", "maplon" };

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.debug("messageId: " + message.messageId + ", from: " + message.from);
      }

      String xmlString = new String(message.attachments.get(MessageType.HUMANITARIAN_NEEDS.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      // box 1
      var formLocation = getLatLongFromXml(mapLocationTags);

      // box 2
      var teamId = getStringFromXml("teamid");

      // box 3
      var formDate = getStringFromXml("thedate");

      // box 4
      var formTime = getStringFromXml("thetime");

      // box 5
      var address = getStringFromXml("address");

      // box 6 -- needs
      var needsHealth = valid(getStringFromXml("health"));
      var needsShelter = valid(getStringFromXml("shelter"));
      var needsFood = valid(getStringFromXml("food"));
      var needsWater = valid(getStringFromXml("water"));
      var needsLogistics = valid(getStringFromXml("logistics"));
      var needsOther = valid(getStringFromXml("other"));

      // box 7
      var description = getStringFromXml("situationdescription");

      // box 8
      var other = getStringFromXml("otherinfo");

      // non-boxed
      var approvedBy = getStringFromXml("approvedby");
      var position = getStringFromXml("titlepos");

      var version = getStringFromXml("templateversion");
      if (version != null) {
        var fields = version.replaceAll("  ", " ").split(" ");
        if (fields.length >= 3) {
          version = fields[2];
        }
      }

      var m = new HumanitarianNeedsMessage(message, formLocation, teamId, //
          formDate, formTime, address, //
          needsHealth, needsShelter, needsFood, needsWater, needsLogistics, needsOther, //
          description, other, approvedBy, position, version); //

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HUMANITARIAN_NEEDS;
  }

  private boolean valid(String s) {
    return s != null && s.length() > 0;
  }

}
