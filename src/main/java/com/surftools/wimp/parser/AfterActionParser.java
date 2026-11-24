/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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
import com.surftools.wimp.message.AfterActionMessage;
import com.surftools.wimp.message.ExportedMessage;

/**
 * parser for After Action Review
 *
 *
 * @author bobt
 *
 */
public class AfterActionParser extends AbstractBaseParser {
  protected Logger logger;
  protected MessageType messageType;

  public AfterActionParser() {
    logger = LoggerFactory.getLogger(AfterActionParser.class);
    messageType = MessageType.AFTER_ACTION;
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(messageType.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("formtitle");
      var incidentName = getStringFromXml("incident_name");
      var page = getStringFromXml("page");

      var opFrom = getStringFromXml("datetimefrom");
      var opTo = getStringFromXml("datetimeto");

      var name = getStringFromXml("name");
      var icsPosition = getStringFromXml("ics_position");
      var homeAgency = getStringFromXml("home_agency");
      var resource = new AfterActionMessage.Resource(name, icsPosition, homeAgency);

      var assignedResources = makeResources();

      var whatWasPlanned = getStringFromXml("fld7a");
      var whatActuallyHappened = getStringFromXml("fld7b");
      var whyDidItHappen = getStringFromXml("fld7c");
      var whatAreWeGoingToDoNext = getStringFromXml("fld7d");

      var preparedBy = getStringFromXml("preparedname");
      var preparedTime = getStringFromXml("datetime");
      var version = "";
      var templateVersion = getStringFromXml("templateversion");
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      var m = new AfterActionMessage(message, organization, incidentName, page, //
          opFrom, opTo, //
          resource, assignedResources, //
          whatWasPlanned, whatActuallyHappened, whyDidItHappen, whatAreWeGoingToDoNext, //
          preparedBy, preparedTime, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  protected List<AfterActionMessage.Resource> makeResources() {
    var list = new ArrayList<AfterActionMessage.Resource>();
    for (int i = 1; i <= 8; ++i) {
      var name = getStringFromXml("name" + i);
      var icsPosition = getStringFromXml("ics_position" + i);
      var homeAgency = getStringFromXml("home_agency" + i);
      var entry = new AfterActionMessage.Resource(name, icsPosition, homeAgency);
      list.add(entry);
    }
    return list;
  }

}
