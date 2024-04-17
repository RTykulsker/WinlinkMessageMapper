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

import java.time.LocalDateTime;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.parser.HospitalStatusParser;

// support General Medical/Hospital Status

public class HospitalStatusMessage extends ExportedMessage {

  public final String organization;
  public final boolean isExercise;
  public final String reportType;
  public final String updateString;

  public final String incidentName;
  public final LocalDateTime formDateTime;

  public final String facilityName;
  public final String facilityType;
  public final String facilityOther;

  public final String contactName;
  public final String contactPhone;
  public final String contactExtension;
  public final String contactCellPhone;
  public final String contactEmail;

  public final LatLongPair formLocation;

  public final String facilityStatus;
  public final String facilityComments;

  public final boolean isCommsImpacted;
  public final String commsEmail;
  public final String commsLandline;
  public final String commsFax;
  public final String commsInternet;
  public final String commsCell;
  public final String commsSatPhone;
  public final String commsHamRadio;
  public final String commsComments;

  public final boolean isUtilsImpacted;
  public final String utilsPower;
  public final String utilsWater;
  public final String utilsSanitation;
  public final String utilsHVAC;
  public final String utilsComments;

  public final boolean areEvacConcerns;
  public final String evacuating;
  public final String evacuatingStatus;
  public final String partialEvac;
  public final String partialEvacStatus;
  public final String totalEvac;
  public final String totalEvacStatus;
  public final String shelterInPlace;
  public final String shelterInPlaceStatus;
  public final String evacComments;

  public final boolean areCasualties;
  public final String casImmediate;
  public final String casDelayed;
  public final String casMinor;
  public final String casFatalities;
  public final String casComments;

  public final boolean planActivated;
  public final boolean commandCenterActivated;
  public final boolean generatorInUse;
  public final boolean rrIn4Hours;
  public final String additionalComments;

  public final String version;

  public HospitalStatusMessage(ExportedMessage exportedMessage, //
      String organization, boolean isExercise, String reportType, String updateString, //
      String incidentName, LocalDateTime formDateTime, //
      String facilityName, String facilityType, String facilityOther, //
      LatLongPair formLocation, //
      String contactName, String contactPhone, String contactExtension, String contactCellPhone, String contactEmail, //
      String facilityStatus, String facilityComments, //

      boolean isCommsImpacted, String commsEmail, String commsLandline, String commsFax, String commsInternet,
      String commsCell, String commsSatPhone, String commsHamRadio, String commsComments,

      boolean isUtilsImpacted, String utilsPower, String utilsWater, String utilsSanitation, String utilsHVAC,
      String utilsComments, //

      boolean areEvacConcerns, String evacuating, String evacuatingStatus, String partialEvac, String partialEvacStatus,
      String totalEvac, String totalEvacStatus, String shelterInPlace, String shelterInPlaceStatus, String evacComments, //

      boolean areCasualties, String casImmediate, String casDelayed, String casMinor, String casFatalities,
      String casComments, //

      boolean planActivated, boolean commandCenterActivated, boolean generatorInUse, boolean rrIn4Hours,
      String additionalComments, //

      String version //
  ) {
    super(exportedMessage);

    this.organization = organization;
    this.isExercise = isExercise;
    this.reportType = reportType;
    this.updateString = updateString;

    this.incidentName = incidentName;
    this.formDateTime = formDateTime;

    this.facilityName = facilityName;
    this.facilityType = facilityType;
    this.facilityOther = facilityOther;

    this.formLocation = formLocation;

    this.contactName = contactName;
    this.contactPhone = contactPhone;
    this.contactExtension = contactExtension;
    this.contactCellPhone = contactCellPhone;
    this.contactEmail = contactEmail;

    this.facilityStatus = facilityStatus;
    this.facilityComments = facilityComments;

    this.isCommsImpacted = isCommsImpacted;
    this.commsEmail = commsEmail;
    this.commsLandline = commsLandline;
    this.commsFax = commsFax;
    this.commsInternet = commsInternet;
    this.commsCell = commsCell;
    this.commsSatPhone = commsSatPhone;
    this.commsHamRadio = commsHamRadio;
    this.commsComments = commsComments;

    this.isUtilsImpacted = isUtilsImpacted;
    this.utilsPower = utilsPower;
    this.utilsWater = utilsWater;
    this.utilsSanitation = utilsSanitation;
    this.utilsHVAC = utilsHVAC;
    this.utilsComments = utilsComments;

    this.areEvacConcerns = areEvacConcerns;
    this.evacuating = evacuating;
    this.evacuatingStatus = evacuatingStatus;
    this.partialEvac = partialEvac;
    this.partialEvacStatus = partialEvacStatus;
    this.totalEvac = totalEvac;
    this.totalEvacStatus = totalEvacStatus;
    this.shelterInPlace = shelterInPlace;
    this.shelterInPlaceStatus = shelterInPlaceStatus;
    this.evacComments = evacComments;

    this.areCasualties = areCasualties;
    this.casImmediate = casImmediate;
    this.casDelayed = casDelayed;
    this.casMinor = casMinor;
    this.casFatalities = casFatalities;
    this.casComments = casComments;

    this.planActivated = planActivated;
    this.commandCenterActivated = commandCenterActivated;
    this.generatorInUse = generatorInUse;
    this.rrIn4Hours = rrIn4Hours;
    this.additionalComments = additionalComments;

    this.version = version;
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Msg Location", //
        "Organization", "IsExercise", "Report Type", "Update #", //

        "Incident Name", "Form Date/Time", //

        "Facility Name", "Facility Type", "Facility Other", //

        "Form Location", //

        "Contact Name", "Contact Phone", "Contact Extension", "Contact Cell Phone", "Contact Email Address", //

        "Facility Status", "Facility Comments", //

        "Is Comms Impacted", "Email", "Landline", "Fax", "Internet", "Cell", "Sat Phone", "Ham Radio", "Comms Comments", //

        "Are Utils Impacted", "Power", "Water", "Sanitation", "HVAC", "Utils Comments", //

        "Evacuation Concerns", "Evacuating", "Evacuating Status", "Partial Evacuation", "Partial Evacuation Status",
        "Total Evacuation", "Total Evacuation Status", "Shelter In Place", "Shelter In Place Status",
        "Evacuation Comments", //

        "Casualties?", "Immediate", "Delayed", "Minor", "Fatalities", "Casualty Comments", //

        "Disaster Plan Activated", "Command Center Activated", "Generator Power", "Will Send RR in 4 hours",
        "Additional Comments", //

        "Version" };
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    return new String[] { messageId, from, to, subject, date, time, //
        msgLocation == null ? "" : msgLocation.toString(), //
        organization, String.valueOf(isExercise), reportType, updateString, //

        incidentName, HospitalStatusParser.DT_FORMATTER.format(formDateTime), //

        facilityName, facilityType, facilityOther, //

        formLocation == null ? "" : formLocation.toString(), //

        contactName, contactPhone, contactExtension, contactCellPhone, contactEmail, //

        facilityStatus, facilityComments, //

        String.valueOf(isCommsImpacted), commsEmail, commsLandline, commsFax, commsInternet, commsCell, commsSatPhone,
        commsHamRadio, commsComments, //

        String.valueOf(isUtilsImpacted), utilsPower, utilsWater, utilsSanitation, utilsHVAC, utilsComments, //

        String.valueOf(areEvacConcerns), evacuating, evacuatingStatus, partialEvac, partialEvacStatus, totalEvac,
        totalEvacStatus, shelterInPlace, shelterInPlaceStatus, evacComments, //

        String.valueOf(areCasualties), casImmediate, casDelayed, casMinor, casFatalities, casComments, //

        String.valueOf(planActivated), String.valueOf(commandCenterActivated), String.valueOf(generatorInUse),
        String.valueOf(rrIn4Hours), additionalComments, //

        version //
    };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HOSPITAL_STATUS;
  }

  @Override
  public String getMultiMessageComment() {
    return additionalComments;
  }
}
