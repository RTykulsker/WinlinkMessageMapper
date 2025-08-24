/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

import java.util.HashMap;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Hics259Message.CasualtyEntry;

public class Hics259Parser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(MessageType.HICS_259.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      var incidentName = getStringFromXml("incidentname");

      var formDate = getStringFromXml("thedate");
      var formTime = getStringFromXml("thetime");

      var operationalPeriod = getStringFromXml("opperiod");
      var opFromDate = getStringFromXml("datefrom");
      var opFromTime = getStringFromXml("timefrom");
      var opToDate = getStringFromXml("dateto");
      var opToTime = getStringFromXml("timeto");

      var casualtyMap = new HashMap<String, CasualtyEntry>();

      var keys = Hics259Message.CASUALTY_KEYS;
      for (var i = 1; i <= keys.size(); ++i) {
        var key = keys.get(i - 1);
        var letter = Character.valueOf((char) ('a' + i - 1)).toString();
        var adultValue = getStringFromXml(letter + "1");
        var childValue = getStringFromXml(letter + "2");
        var comment = getStringFromXml("comment" + i);
        var entry = new CasualtyEntry(adultValue, childValue, comment);
        casualtyMap.put(key, entry);
      }

      var patientTrackingManager = getStringFromXml("prepedbychief");
      var facilityName = getStringFromXml("facility");

      // HICS 259 v 0.2
      var version = getStringFromXml("templateversion");
      if (version != null) {
        var fields = version.replaceAll("  ", " ").split(" ");
        if (fields.length > 1) {
          version = fields[fields.length - 1];
        }
      }

      var m = new Hics259Message(message, //
          incidentName, formDate, formTime, //
          operationalPeriod, opFromDate, opFromTime, opToDate, opToTime, //
          casualtyMap, //
          patientTrackingManager, facilityName, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
