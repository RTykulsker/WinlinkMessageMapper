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

package com.surftools.wimp.processors.exercise.eto_2025;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-213 Resource Request
 *
 * @author bobt
 *
 */
public class ETO_2025_07_17 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_07_17.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    messageType = MessageType.ICS_213_RR;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    Ics213RRMessage m = (Ics213RRMessage) message;

    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Box 1: Incident Name should be #EV", "Mills Pt", m.incidentName));
    count(sts.testIfPresent("Box 2: Request Date Time should be present", m.activityDateTime));
    count(sts.test("Box 3: Resource Request Number should be #EV", "B01009", m.requestNumber));

    record Line(String qty, String kind, String item) {
    }
    var expectedItems = new Line[] { //
        null, // line 0
        new Line("168", "Bottle", "Metformin (1000mg) Bottle of 60 tablets extended-release"), //
        new Line("84", "Bottle", "Glimepiride (4mg) Bottle of 60 tablets"), //
        new Line("33", "Carton", "Glargine (Insulin) (3ml) 5 - 100 unit pens"), //
        new Line("84", "Box", "Empagliflozin (12.5mg) 10 X 10 Tablets"), //
        new Line("84", "Bottle", "Pioglitazone (30mg) Bottle of 30 tablets."), //
        new Line("84", "Bottle", "Lisinopril (30mg) Bottle of 90 tablets"), //
        new Line("84", "Bottle", "Amlodipin (10mg Bottle of 30 tablets"), //
        new Line("84", "Bottle", "Atorvastatin (40mg) Bottle of 30 tablets"), //
    };
    int lineNumber = 0;
    for (var lineItem : m.lineItems) {
      ++lineNumber;
      var expectedItem = expectedItems[lineNumber];
      count(sts.test("Box 4 (line " + lineNumber + "): Quantity should be #EV", expectedItem.qty, lineItem.quantity()));
      count(sts.test("Box 4 (line " + lineNumber + "): Kind should be #EV", expectedItem.kind, lineItem.kind()));
      count(sts.testIfEmpty("Box 4 (line " + lineNumber + "): Type should be empty", lineItem.type()));
      count(sts.test_2line("Box 4 (line " + lineNumber + "): Item should be #EV", expectedItem.item, lineItem.item()));
      count(sts
          .test_2line("Box 4 (line " + lineNumber + "): Requested Date should be #EV", "15 July 2025 1400",
              lineItem.requestedDateTime()));
      count(sts
          .testIfEmpty("Box 4 (line " + lineNumber + "): Estimated Date should be empty",
              lineItem.estimatedDateTime()));
      count(sts.testIfEmpty("Box 4 (line " + lineNumber + "): Cost should be empty", lineItem.cost()));
    }

    count(
        sts.test_2line("Box 5: Delivery/Reporting Location should be #EV", "Mills Pt High School Shelter", m.delivery));
    count(sts.test_2line("Box 6: Substitutes should be #EV", "No Substitutes - Fill as ordered", m.substitutes));
    count(sts.test_2line("Box 7: Requested by should be #EV", "Sam Chasse MD Shelter Physician", m.requestedBy));
    count(sts.test("Box 8: Priority should be #EV", "URGENT", m.priority));
    count(sts.test("Box 9: Approved by should be #EV", "Jeff Barton", m.approvedBy));
    count(sts.testIfEmpty("Box 10 Logistics Order Number should be empty", m.logisticsOrderNumber));
    count(sts.testIfEmpty("Box 11 Supplier Phone Number should be empty", m.supplierInfo));
    count(sts.testIfEmpty("Box 12 Supplier Name should be empty", m.supplierName));
    count(sts.testIfEmpty("Box 12A Supplier POC should be empty", m.supplierPointOfContact));
    count(sts.testIfEmpty("Box 13 Supply Notes should be empty", m.supplyNotes));
    count(sts.testIfEmpty("Box 14 Logistics Authorizer should be empty", m.logisticsAuthorizer));
    count(sts.testIfEmpty("Box 15 Logistics Date/Time should be empty", m.logisticsDateTime));
    count(sts.test_2line("Box 16 Logistics Ordered by should be #EV", "Sam Chasse MD Shelter Physician", m.orderedBy));
    count(sts.testIfEmpty("Box 17 Finance Comments should be empty", m.financeComments));
    count(sts.test("Box 18 Finance Section Chief Name should be #EV", "Kimberly Barton", m.financeName));
    count(sts.test("Box 19 Finance Date/Time should be #EV", "2025-07-03 1200", m.financeDateTime));
  }
}