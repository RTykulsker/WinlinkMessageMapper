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
import com.surftools.wimp.message.WA_WebEoc_Ics213RRMessage;

public class WA_WebEoc_Ics213RRParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(WA_WebEoc_Ics213RRParser.class);

  public WA_WebEoc_Ics213RRParser() {
	}

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

		var xmlString = new String(message.attachments.get(MessageType.WA_ICS_213_RR_WEB_EOC.attachmentName()));
      makeDocument(message.messageId, xmlString);

		var activityDate = getStringFromXml("input1");
		var activityTime = getStringFromXml("input2");
		var activityDateTime = activityDate + " " + activityTime;

		var organization = getStringFromXml("input4");

		var requestNumber = getStringFromXml("input7");

		var priority = getStringFromXml("input9");
		var requestedBy = getStringFromXml("input11");

      var lineItems = new ArrayList<LineItem>();


		var quantity = getStringFromXml("input19");
		var kind = getStringFromXml("input17");
		var type = getStringFromXml("input18");
		var item = getStringFromXml("input16");
		var requestedDateTime = getStringFromXml("input22");
		var estimatedDateTime = "";
		var cost = "";
        var lineItem = new LineItem(quantity, kind, type, item, requestedDateTime, estimatedDateTime, cost);
        lineItems.add(lineItem);


		var delivery = getStringFromXml("input20");
		var substitutes = getStringFromXml("die");

      var approvedBy = getStringFromXml("secapp");

		// these are deprecated
		var incidentName = getStringFromXml("die");

		var logisticsOrderNumber = getStringFromXml("die");
		var supplierInfo = getStringFromXml("die");
		var supplierName = getStringFromXml("die");
		var supplierPointOfContact = getStringFromXml("die");
		var supplyNotes = getStringFromXml("die");
		var logisticsAuthorizer = getStringFromXml("die");
		var logisticsDateTime = getStringFromXml("die");
		var orderedBy = getStringFromXml("die");

		var financeComments = getStringFromXml("die");
		var financeName = getStringFromXml("die");
		var financeDateTime = getStringFromXml("die");

		// these are new
		var creator = getStringFromXml("input3");
		var county = getStringFromXml("input5");
		var city = getStringFromXml("input6");
		var stateTrackingNumber = getStringFromXml("input8");
		var status = getStringFromXml("input10");
		var requestorPhone = getStringFromXml("input12");
		var requestorFax = getStringFromXml("input13");
		var requestorEmail = getStringFromXml("input14");

		var quickDescription = getStringFromXml("input15");

		var deliveryPOC = getStringFromXml("input21");
		var deliveryPhone = getStringFromXml("input22");
		var deliveryEmail = getStringFromXml("input23");
		var deliveryDateTime = getStringFromXml("input24");
		var duration = getStringFromXml("input25");
		var deliveryNeeded = getStringFromXml("input26");
		var deliveryAddress = getStringFromXml("input27");
		var deliveryDescription = getStringFromXml("input28");

		var localResourcesExhausted = getStringFromXml("input29");
		var mutualAidResourcesExhausted = getStringFromXml("input30");
		var commericalResourcesExhausted = getStringFromXml("input31");
		var willingToFund = getStringFromXml("input32");

		var templateVersion = getStringFromXml("templateversion");
		var version = "unknown";
		if (templateVersion != null) {
			var prefix = "RR WebEOC WA ";
			var index = templateVersion.indexOf(prefix);
			if (index != -1) {
				version = templateVersion.substring(prefix.length());
			}
		}

		var m = new WA_WebEoc_Ics213RRMessage(message, organization, incidentName, activityDateTime, requestNumber, //
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
