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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics205Message;
import com.surftools.wimp.message.Ics205Message.RadioEntry;

/**
 * parser for ICS 205 Radio Plan
 *
 * @author bobt
 *
 */
public class Ics205Parser extends AbstractBaseParser {
  private final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private static boolean strictParsing = false; // should a parse error fail here, or downstream during grading

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {

      String xmlString = new String(message.attachments.get(MessageType.ICS_205.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("formtitle");
      var incidentName = getStringFromXml("incident_name");

      var dateTimePreparedString = getStringFromXml("activitydatetime2");
      try {
        LocalDateTime.parse(dateTimePreparedString, DT_FORMATTER);
      } catch (Exception e1) {
        if (strictParsing) {
          String reason = "can't parse Date/Time Prepared: " + dateTimePreparedString;
          return reject(message, RejectType.CANT_PARSE_DATE_TIME, reason);
        }
      }

      var dateFrom = getStringFromXml("datefrom");
      var dateTo = getStringFromXml("dateto");
      var timeFrom = getStringFromXml("timefrom");
      var timeTo = getStringFromXml("timeto");

      var specialInstructions = getStringFromXml("specialinstructions");
      var approvedBy = getStringFromXml("preparedname");

      var radioEntries = makeRadioEntries();

      var version = "";
      var templateVersion = getStringFromXml("templateversion");

      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      var dateTimeApprovedString = getStringFromXml("activitydatetime1");
      try {
        LocalDateTime.parse(dateTimeApprovedString, DT_FORMATTER);
      } catch (Exception e2) {
        if (strictParsing) {
          String reason = "can't parse Date/Time Approved: " + dateTimeApprovedString;
          return reject(message, RejectType.CANT_PARSE_DATE_TIME, reason);
        }
      }

      var iapPageString = getStringFromXml("iap_page");
      try {
        Integer.parseInt(iapPageString);
      } catch (Exception e3) {
        if (strictParsing) {
          String reason = "can't parse IAP Page: " + iapPageString;
          return reject(message, RejectType.EXPLICIT_OTHER, reason);
        }
      }

      var m = new Ics205Message(message, organization, incidentName, //
          dateTimePreparedString, dateFrom, dateTo, timeFrom, timeTo, //
          specialInstructions, approvedBy, dateTimeApprovedString, iapPageString, //
          radioEntries, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private List<RadioEntry> makeRadioEntries() {

    var list = new ArrayList<RadioEntry>();
    for (int i = 1; i <= 10; ++i) {
      var rowNumber = i;
      var zoneGroup = getStringFromXml("zonegrp" + i);
      var channelNumber = getStringFromXml("ch" + i);
      var function = getStringFromXml("function" + i);
      var channelName = getStringFromXml("channelname" + i);
      var assignment = getStringFromXml("assignment" + i);
      var rxFrequency = getStringFromXml("rx" + i);
      var rxNarrowWide = getStringFromXml("nwmode" + i);
      var rxTone = getStringFromXml("rxtone" + i);
      var txFrequency = getStringFromXml("tx" + i);
      var txNarrowWide = getStringFromXml("tnwmode" + i);
      var txTone = getStringFromXml("txtone" + i);
      var mode = getStringFromXml("mode" + i);
      var remarks = getStringFromXml("remarks" + i);
      var entry = new RadioEntry(rowNumber, //
          zoneGroup, channelNumber, function, channelName, assignment, //
          rxFrequency, rxNarrowWide, rxTone, //
          txFrequency, txNarrowWide, txTone, //
          mode, remarks);
      list.add(entry);
    }
    return list;
  }

}
