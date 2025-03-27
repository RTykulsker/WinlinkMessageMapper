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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics214Message;

/**
 * parser for ICS 214 Activity Log
 *
 * also handles ICS-214A Individual Activity Log, by extension
 *
 * @author bobt
 *
 */
public class Ics214Parser extends AbstractBaseParser {
  protected Logger logger;
  protected MessageType messageType;

  private static boolean strictParsing = false; // should a parse error fail here, or downstream during grading

  public Ics214Parser() {
    logger = LoggerFactory.getLogger(Ics214Parser.class);
    messageType = MessageType.ICS_214;
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(MessageType.ICS_214.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("formtitle");
      var incidentName = getStringFromXml("incident_name");
      var page = getStringFromXml("page");

      var opFrom = getStringFromXml("datetimefrom");
      var opTo = getStringFromXml("datetimeto");

      var name = getStringFromXml("name");
      var icsPosition = getStringFromXml("ics_position");
      var homeAgency = getStringFromXml("home_agency");
      var resource = new Ics214Message.Resource(name, icsPosition, homeAgency);

      var assignedResources = makeResources();
      var activities = makeActivities();

      var preparedBy = getStringFromXml("preparedname");
      var version = "";
      var templateVersion = getStringFromXml("templateversion");
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      if (strictParsing) {
        var reasons = new ArrayList<String>();
        try {
          Integer.parseInt(page);
        } catch (Exception e1) {
          reasons.add("can't parse Page: " + page);
        }

        if (reasons.size() > 0) {
          return reject(message, RejectType.EXPLICIT_OTHER, String.join(",", reasons));
        }
      }

      // this is for 214A -- individual
      if (messageType == MessageType.ICS_214A) {
        resource = assignedResources.get(0);
        assignedResources.clear();
      }

      var m = new Ics214Message(message, organization, incidentName, page, opFrom, opTo, resource, assignedResources,
          activities, preparedBy, version, messageType == MessageType.ICS_214A);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return messageType;
  }

  protected List<Ics214Message.Resource> makeResources() {
    var list = new ArrayList<Ics214Message.Resource>();
    for (int i = 1; i <= 8; ++i) {
      var name = getStringFromXml("name" + i);
      var icsPosition = getStringFromXml("ics_position" + i);
      var homeAgency = getStringFromXml("home_agency" + i);
      var entry = new Ics214Message.Resource(name, icsPosition, homeAgency);
      list.add(entry);
    }
    return list;
  }

  protected List<Ics214Message.Activity> makeActivities() {
    var list = new ArrayList<Ics214Message.Activity>();
    for (int i = 1; i <= 8; ++i) {
      var dateTime = getStringFromXml("activitydatetime" + i);
      var activities = getStringFromXml("activities" + i);
      var entry = new Ics214Message.Activity(dateTime, activities);
      list.add(entry);
    }
    return list;
  }

}
