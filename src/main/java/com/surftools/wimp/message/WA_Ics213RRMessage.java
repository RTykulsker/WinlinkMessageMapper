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

public class WA_Ics213RRMessage extends Ics213RRMessage {

  public static enum DetailLevel {
    LOW, MEDIUM, HIGH;

    public static DetailLevel fromString(String string) {
      for (DetailLevel detailLevel : DetailLevel.values()) {
        if (detailLevel.toString().equals(string)) {
          return detailLevel;
        }
      }
      return null;
    }
  };

  // how we discriminate between DetailLevel; set in processors
  public static DetailLevel detailLevel = DetailLevel.MEDIUM;

  // fields (not in Ics213RRMessage)
  public final String isExercise;
  public final String supportNeeded;
  public final String duration;
  public final String deliveryPOC;

  public final String commericalResourcesExhausted;
  public final String localResourcesExhausted;
  public final String mutualAidResourcesExhausted;
  public final String willingToFund;
  public final String fundingExplanation;

  public final String extraOrderedBy;
  public final String elevateToState;
  public final String stateTrackingNumber;
  public final String mutualAidTrackingNumber;

  public WA_Ics213RRMessage(ExportedMessage xmlMessage, //
      String isExercise, //
      String incidentName, String organization, //
      String activityDateTime, String requestNumber, //
      List<LineItem> lineItems, //
      String supportNeeded, String duration, //
      String deliveryLocation, String deliveryPOC, //
      String substitutes, String priority, //
      String commercialResourcesExhaused, String localResourcesExhausted, String mutualAidResourcesExhausted, //
      String willingToFund, String fundingExplanation, //
      String requestedBy, String approvedBy, //

      String logisticsOrderNumber, String supplierName, //
      String supplyNotes, //
      String logisticsAuthorizer, String logisticsDateTime, String orderedBy, String extraOrderedBy, //

      String elevateToState, String stateTrackingNumber, String mutualAidTrackingNumber, //

      String financeComments, String financeName, String financeDateTime) {

    super(xmlMessage, organization, incidentName, activityDateTime, requestNumber, //
        lineItems, //
        deliveryLocation, substitutes, requestedBy, priority, approvedBy, //
        logisticsOrderNumber, "n/a", supplierName, //
        "n/a", supplyNotes, logisticsAuthorizer, //
        logisticsDateTime, orderedBy, //
        financeComments, financeName, financeDateTime);

    this.isExercise = isExercise;
    this.supportNeeded = supportNeeded;
    this.duration = duration;
    this.deliveryPOC = deliveryPOC;

    this.commericalResourcesExhausted = commercialResourcesExhaused;
    this.localResourcesExhausted = localResourcesExhausted;
    this.mutualAidResourcesExhausted = mutualAidResourcesExhausted;
    this.willingToFund = willingToFund;
    this.fundingExplanation = fundingExplanation;

    this.extraOrderedBy = extraOrderedBy;

    this.elevateToState = elevateToState;
    this.stateTrackingNumber = stateTrackingNumber;
    this.mutualAidTrackingNumber = mutualAidTrackingNumber;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    if (detailLevel == DetailLevel.LOW) {
      return new String[] { //
          "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
          "Is Exercise", //
          "Mission/Incident Name", "Requesting Agency", //
          "Date & Time", "Requestor Tracking Number", //
          "Quantity", "Kind", "Type", "Detailed Description", "Requested Date/Time", "Estimated", "Cost", //
          "Personanel Needed", "Duration Needed", //
          "Delivery Location", "Delivery POC", //
          "Substitutes", "Priority", //
          "Commercial Resources Exhausted", "LocalResources Exhausted", "Mutual Aid Resources Exhausted", //
          "Willing to Fund", "Explanation", //
          "Requested By", "Authorized By" };
    } else if (detailLevel == DetailLevel.MEDIUM) {
      return new String[] { //
          "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
          "Is Exercise", //
          "Mission/Incident Name", "Requesting Agency", //
          "Date & Time", "Requestor Tracking Number", //
          "Quantity", "Kind", "Type", "Detailed Description", "Requested Date/Time", "Estimated", "Cost", //
          "Personnel/Support Needed", "Duration Needed", //
          "Delivery Location", "Delivery POC", //
          "Substitutes", "Priority", //
          "Commercial Resources Exhausted", "LocalResources Exhausted", "Mutual Aid Resources Exhausted", //
          "Willing to Fund", "Explanation", //
          "Requested By", "Authorized By", //

          "Logistics Tracking Number", "Name of Supplier/POC", //
          "Log Notes", //
          "Log Approver", "Log Date/Time", //
          "Order Placed By", "Other Info", //
          "Elevate to State", "State Tracking Number", "Mutual Aid Tracking Number", //

          "Finance Comments", //
          "Finance Signature", "Finance Date/Time" //
      };
    }
    throw new RuntimeException("Unsupported detailLevel: " + detailLevel);
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var li = lineItems.get(0);

    if (detailLevel == DetailLevel.LOW) {
      return new String[] { //
          messageId, from, to, subject, date, time, latitude, longitude, //
          isExercise, //
          incidentName, organization, //
          activityDateTime, requestNumber, //
          li.quantity(), li.kind(), li.type(), li.item(), li.requestedDateTime(), li.estimatedDateTime(), li.cost(), //
          supportNeeded, duration, //
          delivery, deliveryPOC, //
          substitutes, priority, //
          commericalResourcesExhausted, localResourcesExhausted, mutualAidResourcesExhausted, //
          willingToFund, fundingExplanation, //
          requestedBy, approvedBy, //
      };
    } else if (detailLevel == DetailLevel.MEDIUM) {
      return new String[] { //
          messageId, from, to, subject, date, time, latitude, longitude, //
          isExercise, //
          incidentName, organization, //
          activityDateTime, requestNumber, //
          li.quantity(), li.kind(), li.type(), li.item(), li.requestedDateTime(), li.estimatedDateTime(), li.cost(), //
          supportNeeded, duration, //
          delivery, deliveryPOC, //
          substitutes, priority, //
          commericalResourcesExhausted, localResourcesExhausted, mutualAidResourcesExhausted, //
          willingToFund, fundingExplanation, //
          requestedBy, approvedBy, //

          logisticsOrderNumber, supplierName, //
          supplyNotes, //
          logisticsAuthorizer, logisticsDateTime, //
          orderedBy, extraOrderedBy, //
          elevateToState, stateTrackingNumber, mutualAidTrackingNumber, //

          financeComments, financeName, financeDateTime //
      };
    }
    throw new RuntimeException("Unsupported detailLevel: " + detailLevel);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_ICS_213_RR;
  }

  @Override
  public String getMultiMessageComment() {
    return lineItems.get(0).item();
  }
}
