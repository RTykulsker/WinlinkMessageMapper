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

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics251Message;
import com.surftools.wimp.message.Hics251Message.StatusEntry;
import com.surftools.wimp.message.Hics251Message.StatusType;

public class Hics251Parser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(MessageType.HICS_251.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      var incidentName = getStringFromXml("incident_name");
      var pageNumber = getStringFromXml("page");
      var pageTotal = getStringFromXml("page1");

      var operationalPeriod = getStringFromXml("opnum");
      var opFromDate = getStringFromXml("datefrom");
      var opFromTime = getStringFromXml("timefrom");
      var opToDate = getStringFromXml("dateto");
      var opToTime = getStringFromXml("timeto");

      var departmentName = getStringFromXml("department");
      var contactNumber = getStringFromXml("contactnumber");

      var streetAddress = getStringFromXml("streetAddress");
      var city = getStringFromXml("city");
      var state = getStringFromXml("state");
      var zip = getStringFromXml("zipcode");

      var statusEntries = new ArrayList<StatusEntry>();

      var keys = Hics251Message.SYSTEM_NAMES;
      for (var i = 1; i <= keys.size(); ++i) {
        var key = keys.get(i - 1);
        var letter = Character.valueOf((char) ('a' + i - 1)).toString();
        var statusString = getStringFromXml("select" + letter);
        var statusType = StatusType.parse(statusString);
        var comment = getStringFromXml("comment" + letter);
        var entry = new StatusEntry(key, statusType, comment);
        statusEntries.add(entry);
      }

      var preparedBy = getStringFromXml("preparedname");
      var formDateTime = getStringFromXml("datetime");
      var facilityName = getStringFromXml("facility");

      var remarks = getStringFromXml("remarks");
      var radioOperator = getStringFromXml("operator");

      var formLatitude = getStringFromXml("maplat");
      var formLongitude = getStringFromXml("maplon");
      var formLocation = new LatLongPair(formLatitude, formLongitude);

      // HICS-251 v .6
      var formVersion = getStringFromXml("templateversion");
      if (formVersion != null) {
        var fields = formVersion.replaceAll("  ", " ").split(" ");
        if (fields.length > 1) {
          formVersion = fields[fields.length - 1];
        }
      }

      var expressVersion = "";

      var m = new Hics251Message(message, //
          incidentName, pageNumber, pageTotal, //
          operationalPeriod, opFromDate, opFromTime, opToDate, opToTime, //
          departmentName, contactNumber, //
          streetAddress, city, state, zip, //
          statusEntries, //
          preparedBy, formDateTime, facilityName, //
          remarks, //
          radioOperator, formLocation, //
          formVersion, expressVersion);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
