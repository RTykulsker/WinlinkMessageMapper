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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage.LineItem;
import com.surftools.wimp.message.WA_Ics213RRMessage;

public class WA_Ics213RRParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(WA_Ics213RRParser.class);

  public WA_Ics213RRParser() {
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

		var xmlString = new String(message.attachments.get(MessageType.WA_ICS_213_RR.attachmentName()));
      makeDocument(message.messageId, xmlString);

		var organization = getStringFromXml("agname");
      var incidentName = getStringFromXml("incname");
		var activityDateTime = getStringFromXml("datetime");
		var requestNumber = getStringFromXml("reqtracknum");

      var lineItems = new ArrayList<LineItem>();

		// only ONE line item
		for (var i = 1; i <= 1; ++i) {
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

		var supportNeeded = getStringFromXml("supneed");
		var delivery = getStringFromXml("reqloc1");
		var substitutes = getStringFromXml("sub");
      var requestedBy = getStringFromXml("reqname");
      var priority = getStringFromXml("priority");
		var approvedBy = getStringFromXml("reqauth");

		// WA unique
		// TODO add to message, constructor, getHeaders/getValues
		var deliveryPOC = getStringFromXml("reqloc");
		var commericalResourcesExhausted = getStringFromXml("b12a");
		var localResourcesExhausted = getStringFromXml("b12b");
		var mutualAidResourcesExhausted = getStringFromXml("b12c");
		var willingToFund = getStringFromXml("b13");
		var fundingExplanation = getStringFromXml("explain");

		var logisticsOrderNumber = getStringFromXml("eocnum");
		var supplierInfo = getStringFromXml("supinfo"); // not used
		var supplierName = getStringFromXml("supname1");
		var supplierPointOfContact = getStringFromXml("poc"); // not used
      var supplyNotes = getStringFromXml("notes");
		var logisticsAuthorizer = getStringFromXml("logrep");
		var logisticsDateTime = getStringFromXml("datetime1");
		var orderedBy = getStringFromXml("orderby");

		// new
		var extraOrderdBy = getStringFromXml("other");
		var elevateToState = getStringFromXml("elevate");
		var stateTrackingNumber = getStringFromXml("statenum");
		var mutualAidTrackingNumber = getStringFromXml("matracking");

      var financeComments = getStringFromXml("fincomm");
      var financeName = getStringFromXml("finrepname");
		var financeDateTime = getStringFromXml("datetime2");

		var m = new WA_Ics213RRMessage(message, organization, incidentName, activityDateTime, requestNumber, //
          lineItems, //
          delivery, substitutes, requestedBy, priority, approvedBy, //
          logisticsOrderNumber, supplierInfo, supplierName, //
          supplierPointOfContact, supplyNotes, logisticsAuthorizer, //
          logisticsDateTime, orderedBy, //
          financeComments, financeName, financeDateTime
      );

      return m;

    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
