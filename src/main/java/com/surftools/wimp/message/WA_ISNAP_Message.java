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

import com.surftools.wimp.core.MessageType;

public class WA_ISNAP_Message extends ExportedMessage {
  public final String formDateTimeString;
  public final String isnapVersion;
  public final String incidentType;
  public final String stateMissionNumber;

  public final String affectedJurisdictions;
  public final String reportingJurisdiction;

  public final String pointOfContact;
  public final String eocStatus;
  public final String countyStatus;

  public final String description;

  public final String governmentStatus;
  public final String governmentComments;

  public final String transportationStatus;
  public final String transportationComments;

  public final String utilitiesStatus;
  public final String utilitiesComments;

  public final String medicalStatus;
  public final String medicalComments;

  public final String communicationsStatus;
  public final String communicationsComments;

  public final String publicSafetyStatus;
  public final String publicSafetyComments;

  public final String environmentStatus;
  public final String environmentComments;

  public WA_ISNAP_Message(ExportedMessage exportedMessage, //
      String formDate, String formTime, String isnapVersion, String incidentType, String stateMissionNumber,
      String affectedJurisdictions, String reportingJurisdiction, //
      String pointOfContact, String eocStatus, String countyStatus, //
      String description, //
      String governmentStatus, String governmentComments, //
      String transportationStatus, String transportationComments, //
      String utilitiesStatus, String utilitiesComments, //
      String medicalStatus, String medicalComments, //
      String communicationsStatus, String communicationsComments, //
      String publicSafetyStatus, String publicSafetyComments, //
      String environmentStatus, String environmentComments) {
    super(exportedMessage);

    formDateTimeString = formDate + "-" + formTime;
    this.isnapVersion = isnapVersion;
    this.incidentType = incidentType;
    this.stateMissionNumber = stateMissionNumber;

    this.affectedJurisdictions = affectedJurisdictions;
    this.reportingJurisdiction = reportingJurisdiction;

    this.pointOfContact = pointOfContact;
    this.eocStatus = eocStatus;
    this.countyStatus = countyStatus;

    this.description = description;

    this.governmentStatus = governmentStatus;
    this.governmentComments = governmentComments;

    this.transportationStatus = transportationStatus;
    this.transportationComments = transportationComments;

    this.utilitiesStatus = utilitiesStatus;
    this.utilitiesComments = utilitiesComments;

    this.medicalStatus = medicalStatus;
    this.medicalComments = medicalComments;

    this.communicationsStatus = communicationsStatus;
    this.communicationsComments = communicationsComments;

    this.publicSafetyStatus = publicSafetyStatus;
    this.publicSafetyComments = publicSafetyComments;

    this.environmentStatus = environmentStatus;
    this.environmentComments = environmentComments;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date/Time", "Latitude", "Longitude", //
        "ISNAP Version", "Incident Type", "State Mission Number", //
        "Affected Jurisdictions", "Reporting Jurisdiction", //
        "Point of Contact", "EOC Status", "County Status", //
        "Brief Description", //
        "Government Status", "Government Comments", //
        "Transportation Status", "Transportation Comments", //
        "Utilities Status", "Utilities Comments", //
        "Medical Status", "Medical Comments", //
        "Communications Status", "communications Comments", //
        "Public Safety Status", "Public Safety Comments", //
        "Environment Status", "Environment Comments", "File Name" //
    };
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, formDateTimeString, latitude, longitude, //
        isnapVersion, incidentType, stateMissionNumber, //
        affectedJurisdictions, reportingJurisdiction, //
        pointOfContact, eocStatus, countyStatus, //
        description, //
        governmentStatus, governmentComments, //
        transportationStatus, transportationComments, //
        utilitiesStatus, utilitiesComments, //
        medicalStatus, medicalComments, //
        communicationsStatus, communicationsComments, //
        publicSafetyStatus, publicSafetyComments, //
        environmentStatus, environmentComments, fileName //
    };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_ISNAP;
  }

  @Override
  public String getMultiMessageComment() {
    return description;
  }
}
