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

import java.util.List;

import com.surftools.wimp.core.MessageType;

public class WA_WebEoc_Ics213RRMessage extends Ics213RRMessage {

  // fields (not in Ics213RRMessage)
  public final String formDate;
  public final String formTime;
  public final String creator;
  public final String county;
  public final String city;
  public final String stateTrackingNumber;
  public final String status;
  public final String requestorPhone;
  public final String requestorFax;
  public final String requestorEmail;
  public final String quickDescription;
  public final String deliveryPOC;
  public final String deliveryPOCPhone;
  public final String deliveryPOCEmail;
  public final String duration;
  public final String deliveryNeeded;
  public final String deliveryAddress;
  public final String deliveryDescription;
  public final String localResourcesExhausted;
  public final String mutualAidResourcesExhausted;
  public final String commericalResourcesExhausted;
  public final String willingToFund;
  public final String version;

  public WA_WebEoc_Ics213RRMessage(ExportedMessage xmlMessage, //
      List<String> input, List<LineItem> lineItems, String version) {
    super(xmlMessage, input.get(4), "n/a", input.get(1) + " " + input.get(2), input.get(7), //
        lineItems, //
        input.get(20), "n/a", input.get(7), input.get(9), "n/a", //
        "n/a", "n/a", "n/a", //
        "n/a", "n/a", "n/a", //
        "n/a", "n/a", //
        "n/a", "n/a", "n.a");

    formDate = input.get(1);
    formTime = input.get(2);
    creator = input.get(3);

    county = input.get(5);
    city = input.get(6);

    stateTrackingNumber = input.get(8);
    status = input.get(10);

    requestorPhone = input.get(12);
    requestorFax = input.get(13);
    requestorEmail = input.get(14);

    quickDescription = input.get(15);

    deliveryPOC = input.get(21);
    deliveryPOCPhone = input.get(22);
    deliveryPOCEmail = input.get(23);

    duration = input.get(25);
    deliveryNeeded = input.get(26);
    deliveryAddress = input.get(27);
    deliveryDescription = input.get(28);

    localResourcesExhausted = input.get(29);
    mutualAidResourcesExhausted = input.get(30);
    commericalResourcesExhausted = input.get(31);
    willingToFund = input.get(32);

    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {

    return new String[] { //
        "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Form Date", "Form Time", //
        "Creator", //
        "Requesting Agency", //
        "County", "City/Tribe", //
        "Requestor Tracking Number", //
        "State Tracking Number", //
        "Priority", //
        "Order Status", //
        "Requestor Name", "Requestor Phone", //
        "Requestor Fax", "Requestor Email", //
        "Resource Requested", //
        "Detailed Description", //
        "Kind", "Type", "Quantity", //
        "Delivery Location", //
        "Delivery POC", "POC Phone", //
        "POC Email", //
        "Requested Date/Time", //
        "Duration Needed", //
        "Delivery Needed", //
        "Delivery Location", "Delivery Description", //
        "LocalResources Exhausted", "Mutual Aid Resources Exhausted", "Commercial Resources Exhausted", //
        "Willing to Fund", //
        "Version", //
    };
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var li = lineItems.get(0);

    return new String[] { //
        messageId, from, to, subject, date, time, latitude, longitude, //
        formDate, formTime, //
        creator, //
        organization, //
        county, city, //
        requestNumber, //
        stateTrackingNumber, //
        priority, //
        status, //
        requestedBy, requestorPhone, //
        requestorFax, requestorEmail, //
        quickDescription, //
        li.item(), //
        li.kind(), li.type(), li.quantity(), //
        delivery, deliveryPOC, deliveryPOCPhone, //
        deliveryPOCEmail, //
        li.requestedDateTime(), //
        duration, //
        deliveryNeeded, //
        deliveryAddress, //
        deliveryDescription, //
        localResourcesExhausted, mutualAidResourcesExhausted, commericalResourcesExhausted, //
        willingToFund, //
        version, //
    };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_ICS_213_RR_WEB_EOC;
  }

  @Override
  public String getMultiMessageComment() {
    return quickDescription;
  }
}
