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

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics309Message;

/**
 * parser for ICS 309 Communications Log
 *
 * @author bobt
 *
 */
public class Ics309Parser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(MessageType.ICS_309.rmsViewerName()));
      makeDocument(message.messageId, xmlString);
      var organization = getStringFromXml("title");
      var taskNumber = getStringFromXml("task");
      var dateTimePrepared = getStringFromXml("activitydatetime1");
      var operationalPeriod = getStringFromXml("opper");
      var taskName = getStringFromXml("taskname");
      var operatorName = getStringFromXml("opname");
      var stationId = getStringFromXml("operid");
      var incidentName = getStringFromXml("incident_name");
      var page = getStringFromXml("page");

      var version = "";
      var templateVersion = getStringFromXml("templateversion");
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      var activities = makeActivities(version);

      var m = new Ics309Message(message, organization, taskNumber, dateTimePrepared, operationalPeriod, taskName,
          operatorName, stationId, incidentName, page, version, activities);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private List<Ics309Message.Activity> makeActivities(String versionString) {

    // v13.9.2
    var versionNumber = 0;
    if (versionString.length() > 0) {
      var fields = versionString.substring(1).split("\\.");
      for (var field : fields) {
        versionNumber = (10 * versionNumber) + Integer.valueOf(field);
      }
      if (fields.length == 2) {
        versionNumber *= 10;
      }
    }

    var list = new ArrayList<Ics309Message.Activity>();
    for (int i = 1; i <= Ics309Message.getNDisplayActivities(); ++i) {
      var dateTime = getStringFromXml("time" + i);
      var from = getStringFromXml("from" + i);
      var to = getStringFromXml("to" + i);

      // defect in xml; fixed in v13.10
      var doSwap = false;
      if (doSwap || versionNumber < 1400) {
        var tmp = from;
        from = to;
        to = tmp;
      }

      var subject = getStringFromXml("sub" + i);
      var entry = new Ics309Message.Activity(dateTime, from, to, subject);

      list.add(entry);
    }
    return list;
  }

}
