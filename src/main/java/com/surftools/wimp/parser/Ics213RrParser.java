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
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics213RRMessage.LineItem;

public class Ics213RrParser extends AbstractBaseParser {
  public Ics213RrParser() {
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      var xmlString = new String(message.attachments.get(MessageType.ICS_213_RR.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("formtitle");
      var incidentName = getStringFromXml("incname");
      var activityDateTime = getStringFromXml("activitydatetime1");
      var requestNumber = getStringFromXml("reqnum");

      var lineItems = new ArrayList<LineItem>();

      for (var i = 1; i <= 8; ++i) {
        var quantity = getStringFromXml("qty" + String.valueOf(i));
        var kind = getStringFromXml("kind" + String.valueOf(i));
        var type = getStringFromXml("type" + String.valueOf(i));
        var item = getStringFromXml("item" + String.valueOf(i));
        var requestedDateTime = getStringFromXml("reqdatetime" + String.valueOf(i));
        var estimatedDateTime = getStringFromXml("estdatetime" + String.valueOf(i));
        var cost = getStringFromXml("cost" + String.valueOf(i));
        var lineItem = new LineItem(quantity, kind, type, item, requestedDateTime, estimatedDateTime, cost);
        lineItems.add(lineItem);
      }

      var delivery = getStringFromXml("delivery");
      var substitutes = getStringFromXml("subs1");
      var requestedBy = getStringFromXml("reqname");
      var priority = getStringFromXml("priority");
      var approvedBy = getStringFromXml("secapp");

      var logisticsOrderNumber = getStringFromXml("lognum");
      var supplierInfo = getStringFromXml("supinfo");
      var supplierName = getStringFromXml("supname");
      var supplierPointOfContact = getStringFromXml("poc");
      var supplyNotes = getStringFromXml("notes");
      var logisticsAuthorizer = getStringFromXml("authsig");
      var logisticsDateTime = getStringFromXml("activitydatetime2");
      var orderedBy = getStringFromXml("orderby");

      var financeComments = getStringFromXml("fincomm");
      var financeName = getStringFromXml("finrepname");
      var financeDateTime = getStringFromXml("activitydatetime3");

      var m = new Ics213RRMessage(message, organization, incidentName, activityDateTime, requestNumber, //
          lineItems, //
          delivery, substitutes, requestedBy, priority, approvedBy, //
          logisticsOrderNumber, supplierInfo, supplierName, //
          supplierPointOfContact, supplyNotes, logisticsAuthorizer, //
          logisticsDateTime, orderedBy, //
          financeComments, financeName, financeDateTime);

      return m;

    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
