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

package com.surftools.wimp.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surftools.wimp.core.MessageType;

public class WA_WebEoc_Ics213RRMessage extends Ics213RRMessage {

  static {
    lineItemsToDisplay = 1;
  }

  private static final String[] preHeaders = new String[] { "MessageId", "From", "To", "Subject", //
      "Date", "Time", "Latitude", "Longitude", //
      "Organization", "Incident Name", "Activity Date/Time", "Request Number" };

  private static final String[] lineHeaders = new String[] { "Qty", "Kind", "Type", "Item", "Reqested Date/Time",
      "Estimated Date/Time", "Cost" };

  private static final String[] postHeaders = new String[] { "Delivery/Reporting Location", "Substitutes",
      "Requested By", "Priority", "Approved By", //
      "Log Order Number", "SupplierInfo", "SupplierName", //
      "POC", "Notes", "Auth Log Rep", "Log Date/Time", "Ordered By", //
      "Finance Comments", "Finance Chief", "Finance Date/Time", };

  public WA_WebEoc_Ics213RRMessage(ExportedMessage xmlMessage, String organization, String incidentName, //
      String activityDateTime, String requestNumber, //
      List<LineItem> lineItems, String delivery, String substitutes, //
      String requestedBy, String priority, String approvedBy, //

      String logisticsOrderNumber, String supplierInfo, String supplierName, //
      String supplierPointOfContact, String supplyNotes, String logisticsAuthorizer, //
      String logisticsDateTime, String orderedBy, //

      String financeComments, String financeName, String financeDateTime) {
    super(xmlMessage, organization, incidentName, activityDateTime, requestNumber, //
        lineItems, //
        delivery, substitutes, requestedBy, priority, approvedBy, //
        logisticsOrderNumber, supplierInfo, supplierName, //
        supplierPointOfContact, supplyNotes, logisticsAuthorizer, //
        logisticsDateTime, orderedBy, //
        financeComments, financeName, financeDateTime);

  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {

    var resultList = new ArrayList<String>(preHeaders.length + (lineItemsToDisplay * 7) + postHeaders.length);

    Collections.addAll(resultList, preHeaders);
    for (var lineNumber = 1; lineNumber <= lineItemsToDisplay; ++lineNumber) {
      for (var lh : lineHeaders) {
        resultList.add(lh + String.valueOf(lineNumber));
      }
    }
    Collections.addAll(resultList, postHeaders);

    return resultList.toArray(new String[resultList.size()]);

  }

  @Override
  public String[] getValues() {
    var resultList = new ArrayList<String>(preHeaders.length + (lineItemsToDisplay * 7) + postHeaders.length);

    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    var preValues = new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        organization, incidentName, activityDateTime, requestNumber };
    var postValues = new String[] { delivery, substitutes, requestedBy, priority, approvedBy, //
        logisticsOrderNumber, supplierInfo, supplierName, //
        supplierPointOfContact, supplyNotes, logisticsAuthorizer, //
        logisticsDateTime, orderedBy, //
        financeComments, financeName, financeDateTime };

    Collections.addAll(resultList, preValues);
    for (int i = 1; i <= lineItemsToDisplay; ++i) {
      var li = lineItems.get(i - 1);
      resultList.add(li.quantity());
      resultList.add(li.kind());
      resultList.add(li.type());
      resultList.add(li.item());
      resultList.add(li.requestedDateTime());
      resultList.add(li.estimatedDateTime());
      resultList.add(li.cost());
    }
    Collections.addAll(resultList, postValues);
    return resultList.toArray(new String[resultList.size()]);
  }

  public static int getLineItemsToDisplay() {
    return lineItemsToDisplay;
  }

  public static void setLineItemsToDisplay(int lineItemsToDisplay) {
    WA_WebEoc_Ics213RRMessage.lineItemsToDisplay = lineItemsToDisplay;
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_ICS_213_RR_WEB_EOC;
  }

  @Override
  public String getMultiMessageComment() {
    return substitutes;
  }
}
