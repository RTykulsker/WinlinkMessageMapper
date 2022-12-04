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

package com.surftools.winlinkMessageMapper.dto.message;

import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class Ics213RRMessage extends ExportedMessage {
  public final String organization;
  public final String incidentName;
  public final String activityDateTime;
  public final String requestNumber;

  public final String quantity1;
  public final String kind1;
  public final String type1;
  public final String item1;
  public final String requestedDateTime1;
  public final String estimatedDateTime1;
  public final String cost1;

  public final String delivery;
  public final String substitutes;
  public final String requestedBy;
  public final String priority;
  public final String approvedBy;

  public Ics213RRMessage(ExportedMessage xmlMessage, String organization, String incidentName, //
      String activityDateTime, String requestNumber, //
      String quantity1, String kind1, String type1, String item1, //
      String requestedDateTime1, String estimatedDateTime1, String cost1, //
      String delivery, String substitutes, //
      String requestedBy, String priority, String approvedBy) {
    super(xmlMessage);

    this.organization = organization;
    this.incidentName = incidentName;
    this.activityDateTime = activityDateTime;
    this.requestNumber = requestNumber;

    this.quantity1 = quantity1;
    this.kind1 = kind1;
    this.type1 = type1;
    this.item1 = item1;
    this.requestedDateTime1 = requestedDateTime1;
    this.estimatedDateTime1 = estimatedDateTime1;
    this.cost1 = cost1;

    this.delivery = delivery;
    this.substitutes = substitutes;
    this.requestedBy = requestedBy;
    this.priority = priority;
    this.approvedBy = approvedBy;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Organization", "Incident Name", "Activity Date/Time", "Request Number", //
        "Qty", "Kind", "Type", "Item", "Reqested Date/Time", "Estimated Date/Time", "Cost", //
        "Delivery/Reporting Location", "Substitutes", "Requested By", "Priority", "Approved By" };
  }

  @Override
  public String[] getValues() {
    var latitude = location == null ? null : location.getLatitude();
    var longitude = location == null ? null : location.getLongitude();
    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        organization, incidentName, activityDateTime, requestNumber, //
        quantity1, kind1, type1, item1, requestedDateTime1, estimatedDateTime1, cost1, //
        delivery, substitutes, requestedBy, priority, approvedBy };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ICS_213_RR;
  }

  @Override
  public String getMultiMessageComment() {
    return substitutes;
  }
}
