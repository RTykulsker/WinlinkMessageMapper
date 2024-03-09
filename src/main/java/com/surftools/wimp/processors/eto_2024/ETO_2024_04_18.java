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

package com.surftools.wimp.processors.eto_2024;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.HospitalBedMessage;
import com.surftools.wimp.message.HospitalStatusMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-04-18 Exercise: Hospital Status
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_04_18 extends FeedbackProcessor {
  // public class ETO_2024_04_18 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_04_18.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  private void handleHospitalBedMessage(HospitalBedMessage m) {
    getCounter("versions").increment(m.version);

    sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization);
    sts.test("THIS IS AN EXERCISE should be #EV", "true", String.valueOf(m.isExercise));

    sts.testIsDateTime("Report Date/Time should be correctly formatted", DTF.format(m.formDateTime), DTF);
    sts.test("Form latitude should be #EV", "42.3539626", m.formLocation.getLatitude());
    sts.test("Form longitude should be #EV", "-83.0534570", m.formLocation.getLongitude());

    sts.test("Contact Person should be #EV", "Dominic Leblanc", m.contactPerson);
    sts.test("Contact Phone should be #EV", "555-555-5555", m.contactPhone);
    sts.test("Contact email should be #EV", "dleblanc@nodomain.com", m.contactEmail);

    sts.test("Emergency Bed count should be #EV", "10", m.emergencyBedCount);
    sts.test("Emergency Bed notes should be #EV", "Holding 3 patients for surgery", m.emergencyBedNotes);

    sts.test("Pediatrics Bed count should be #EV", "8", m.pediatricsBedCount);
    sts.testIfEmpty("Pediatrics Bed notes should be empty", m.pediatricsBedNotes);

    sts.test("Medical Bed count should be #EV", "8", m.medicalBedCount);
    sts.testIfEmpty("Medical Bed notes should be empty", m.medicalBedNotes);

    sts.test("Psychiatry Bed count should be #EV", "4", m.psychiatryBedCount);
    sts.testIfEmpty("Psychiatry Bed notes should be empty", m.psychiatryBedNotes);

    sts.test("Burn Bed count should be #EV", "6", m.burnBedCount);
    sts.testIfEmpty("Burn Bed notes should be empty", m.burnBedNotes);

    sts.test("Critical Care Bed count should be #EV", "12", m.criticalBedCount);
    sts.testIfEmpty("Critical Care Bed notes should be empty", m.criticalBedNotes);

    sts.testIfEmpty("Other (1) label should be empty", m.other1Name);
    sts.testIfEmpty("Other (1) count should be empty", m.other1BedCount);
    sts.testIfEmpty("Other (1) notes should be empty", m.other1BedNotes);

    sts.testIfEmpty("Other (2) label should be empty", m.other2Name);
    sts.testIfEmpty("Other (2) count should be empty", m.other2BedCount);
    sts.testIfEmpty("Other (2) notes should be empty", m.other2BedNotes);

    sts.test("Total Available beds should be #EV", "48", m.totalBedCount);

    sts.test("Additional Comments should be #EV", "At full Emergency Plan staffing", m.additionalComments);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  /**
   * this is the money shot
   *
   * @param m
   */
  private void handleHospitalStatusMessage(HospitalStatusMessage m) {
    getCounter("versions").increment(m.version);

    sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization);
    sts.test("THIS IS AN EXERCISE should be #EV", "true", String.valueOf(m.isExercise));
    sts.test("Report Type should be #EV", "Initial", m.reportType);

    sts.test("Box 1 Incident Name should be #EV", "MERCY TORNADO", m.incidentName);
    sts.testIsDateTime("Box 2 Date Time", DTF.format(m.formDateTime), DTF);
    sts.test("Box 3a Facility Name should be #EV", "Mercy Hospital Joplin, MO", m.facilityName);
    sts.test("Box 3b Facility Type should be #EV", "Hospital", m.facilityType);
    sts.test("Form latitude should be #EV", "37.0361615", m.formLocation.getLatitude());
    sts.test("Form longitude should be #EV", "-94.5099951", m.formLocation.getLongitude());

    sts.test("Box 4a Contact Name should be #EV", "Jane Doe, EM Mgr", m.contactName);
    sts.test("Box 4b Contact Phone should be #EV", " 555-555-5555", m.contactPhone);
    sts.test("Box 4b Extenstion should be #EV", "1234", m.contactExtension);
    sts.test("Box 4c Contact Cell Phone should be #EV", "555-555-5551", m.contactCellPhone);
    sts.test("Box 4d Contact Email should be #EV", "mercyeoc@nodomain.com", m.contactEmail);

    sts.test("Box 5 Facility Operating Status should be #EV", "NOT FUNCTIONAL", m.facilityStatus);
    sts
        .test("Box 5 Facility comments should be #EV", "Windows blown out, major interior destruction",
            m.facilityComments);

    sts.test("Box 6 Communications Impacted should be #EV", "YES", m.isCommsImpacted ? "YES" : "NO");
    sts.test("Box 6a Email should be #EV", "NOT FUNCTIONAL", m.commsEmail);
    sts.test("Box 6b Comms Landline should be #EV", "NOT FUNCTIONAL", m.commsLandline);
    sts.test("Box 6c Comms Fax should be #EV", "NOT FUNCTIONAL", m.commsFax);
    sts.test("Box 6d Comms Internet should be #EV", "NOT FUNCTIONAL", m.commsInternet);
    sts.test("Box 6e Comms Cell Phone should be #EV", "IMPAIRED", m.commsCell);
    sts.test("Box 6f Comms Satellite Phone should be #EV", "NOT FUNCTIONAL", m.commsSatPhone);
    sts.test("Box 6g Comms Amateur Radio should be #EV", "LIMITED", m.commsHamRadio);
    sts.test("Box 6 Comments should be #EV", "Winlink functional, Callsign WB6ZZZ", m.commsComments);

    sts.test("Box 7 Utilities Impacted should be #EV", "YES", m.isUtilsImpacted ? "YES" : "NO");
    sts.test("Box 7a Power should be #EV", "NOT FUNCTIONAL", m.utilsPower);
    sts.test("Box 7b Water Landline should be #EV", "NOT FUNCTIONAL", m.utilsWater);
    sts.test("Box 7c Sanitation Fax should be #EV", "UNKNOWN", m.utilsSanitation);
    sts.test("Box 7d HVAC Internet should be #EV", "NOT FUNCTIONAL", m.utilsHVAC);
    sts.test("Box 7 Comments should be #EV", "No fresh water available", m.utilsComments);

    sts.test("Box 8 Evacuations should be #EV", "YES", m.areEvacConcerns ? "YES" : "NO");
    sts.test("Box 8a Evacuating should be #EV", "YES", m.evacuating);
    sts.test("Box 8a Evacuating Status should be #EV", "Evacuation is in Progress", m.evacuatingStatus);
    sts.test("Box 8b Partial Evacuation should be #EV", "NO", m.partialEvac);
    sts.testIfEmpty("Box 8b Partial Evacuation Status should not be checked", m.partialEvacStatus);
    sts.test("Box 8c Total Evacuation should be #EV", "YES", m.totalEvac);
    sts.test("Box 8c Total Evacuation status should be #EV", "Total Evacuation is Anticipated", m.totalEvacStatus);
    sts.test("Box 8d Shelter In Place should be #EV", "NO", m.shelterInPlace);
    sts.testIfEmpty("Box 8d Shelter In Place Status should not be checked", m.shelterInPlaceStatus);
    sts
        .test("Box 8 Comments should be #EV",
            "“All avail logistics/housekeeping staff employed clearing ER spaces and hallways in preparation for evac operation",
            m.evacComments);

    sts.test("Box 9 Casualties should be #EV", "YES", m.areCasualties ? "YES" : "NO");
    sts.test("Box 9a Immediate injuries should be #EV", "50", m.casImmediate);
    sts.test("Box 9b Delayed injuries should be #EV", "100", m.casDelayed);
    sts.test("Box 9c Minor injuries should be #EV", "300", m.casMinor);
    sts.test("Box 9d Fatalities should be #EV", "5", m.casFatalities);
    sts.test("Box 9 Comments should be #EV", "On site staff: 15% RED, 20% YELLOW, 45% GREEN”", m.casComments);

    sts.test("Box 10 Disaster plan activated should be #EV", "YES", m.planActivated ? "YES" : "NO");
    sts.test("Box 10 Command Center activated should be #EV", "YES", m.commandCenterActivated ? "YES" : "NO");
    sts.test("Box 10 Generator in use should be #EV", "YES", m.generatorInUse ? "YES" : "NO");
    sts.test("Box 10 Will send RR in 4 hours should be #EV", "YES", m.rrIn4Hours ? "YES" : "NO");
    sts
        .test("Box 10 additional comments should be #EV",
            "Preparing non-dischargeable patients for State EOC managed Mass Evacuation", m.additionalComments);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    switch (message.getMessageType()) {
    case HOSPITAL_STATUS:

      handleHospitalStatusMessage((HospitalStatusMessage) message);
      break;

    case HOSPITAL_BED:
      handleHospitalBedMessage((HospitalBedMessage) message);
      break;

    default:
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    } // end switch
  }

}
