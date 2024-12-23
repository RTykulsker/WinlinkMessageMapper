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

package com.surftools.wimp.processors.exercise.miro;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.HospitalBedMessage;
import com.surftools.wimp.message.HospitalStatusMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-06-13 Exercise: 2024 EHPC Hurricane Communication and Information Sharing Exercise
 *
 * Since we are "out-of-state", just a Hospital Bed report, values between 1 and 20, two required addresses
 *
 *
 * @author bobt
 *
 */
public class MIRO_2024_06_13 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(MIRO_2024_06_13.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    doStsFieldValidation = false;
    messageTypesRequiringSecondaryAddress = Set.of(MessageType.MIRO_CHECK_IN);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    if (message.getMessageType() == MessageType.HOSPITAL_BED) {
      handleHospitalBedMessage((HospitalBedMessage) message);
    } else if (message.getMessageType() == MessageType.HOSPITAL_STATUS) {
      handleHospitalStatusMessage((HospitalStatusMessage) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }
  }

  private void handleHospitalStatusMessage(HospitalStatusMessage m) {
    getCounter("versions").increment(m.version);

    if (dumpIds.contains(m.messageId) || dumpIds.contains(m.from)) {
      logger.info("### call: " + m.from + "\n" + sts.toString());
    }

    sts.test("Should not send Hospital Status Message", false);
  }

  protected void handleHospitalBedMessage(HospitalBedMessage m) {

    getCounter("versions").increment(m.version);

    if (dumpIds.contains(m.messageId) || dumpIds.contains(m.from)) {
      logger.info("### call: " + m.from + "\n" + sts.toString());
    }

    var addresses = (m.toList + " " + m.ccList).toUpperCase();
    var primaryAddress = "NCS862@winlink.org".toUpperCase();
    sts.test("Addresses should contain primary address " + primaryAddress, addresses.contains(primaryAddress));

    var secondaryAddress = "COMMEXAUXCOMM@gmail.com".toUpperCase();
    sts.test("Addresses should contain secondary address " + secondaryAddress, addresses.contains(secondaryAddress));

    var org = m.organization;
    var isMiro = org != null && (org.toUpperCase().contains("MIRO") || org.toUpperCase().contains("MERCER ISLAND"));
    sts.test("Agency/Group name should be contain either MIRO or Mercer Island", isMiro, m.organization);

    sts.test("'THIS IS AN EXERCISE' should be checked", m.isExercise);

    // sts.testIfPresent("Name of Reporting Facility should be specified", m.facility);
    // sts.testIfPresent("Report Date/Time should be present and valid", m.formDateTime.toString());
    sts.test("Latitude/Longitude should be present and valid", m.formLocation.isValid(), m.formLocation.toString());

    // sts.testIfPresent("Contact Person should be present", m.contactPerson);
    sts.testIfPresent("Contact Phone Number should be present", m.contactPhone);
    sts.testIfPresent("Contact Email should be present", m.contactEmail);

    var labelSuffix = " should be between 1 and 20";
    sts.test("Emergency Bed Count" + labelSuffix, isValidCount(m.emergencyBedCount), m.emergencyBedCount);
    sts.test("Pediatrics Bed Count" + labelSuffix, isValidCount(m.pediatricsBedCount), m.pediatricsBedCount);
    sts.test("Medical/Surgical Bed Count" + labelSuffix, isValidCount(m.medicalBedCount), m.medicalBedCount);
    sts.test("Psychiatry Bed Count" + labelSuffix, isValidCount(m.psychiatryBedCount), m.psychiatryBedCount);
    sts.test("Burn Bed Count" + labelSuffix, isValidCount(m.burnBedCount), m.burnBedCount);
    sts.test("Critical Care Bed Count" + labelSuffix, isValidCount(m.criticalBedCount), m.criticalBedCount);

    String disclaimer = "\n\nThanks for participating -- see you next month!";
    setExtraOutboundMessageText(disclaimer);
  }

  private boolean isValidCount(String countString) {
    if (countString == null || countString.isEmpty()) {
      return false;
    }

    var count = -1;
    try {
      count = Integer.parseInt(countString);
    } catch (Exception e) {
      return false;
    }

    return count >= 1 && count <= 20;
  }

}
