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

// see https://protect-public.hhs.gov/pages/hospital-utilization for real-world data

public class HospitalBedMessage extends ExportedMessage {

  public final String organization;
  public final boolean isExercise;
  public final LocalDateTime formDateTime;
  public final LatLongPair formLocation;
  public final String facility;

  public final String streetAddress;
  public final String city;
  public final String state;
  public final String zip;

  public final String contactPerson;
  public final String contactPhone;
  public final String contactEmail;

  public final String emergencyBedCount;
  public final String emergencyBedNotes;

  public final String pediatricsBedCount;
  public final String pediatricsBedNotes;

  public final String medicalBedCount;
  public final String medicalBedNotes;

  public final String psychiatryBedCount;
  public final String psychiatryBedNotes;

  public final String burnBedCount;
  public final String burnBedNotes;

  public final String criticalBedCount;
  public final String criticalBedNotes;

  public final String other1Name;
  public final String other1BedCount;
  public final String other1BedNotes;

  public final String other2Name;
  public final String other2BedCount;
  public final String other2BedNotes;

  public final String totalBedCount;
  public final String additionalComments;

  public final String version;

  public HospitalBedMessage(ExportedMessage exportedMessage, //
      LocalDateTime formDateTime, LatLongPair formLocation, //
      String organization, boolean isExercise, String facility, //
      String streetAddress, String city, String state, String zip, //
      String contactPerson, String contactPhone, String contactEmail, //
      String emergencyBedCount, String emergencyBedNotes, //
      String pediatricsBedCount, String pediatricsBedNotes, //
      String medicalBedCount, String medicalBedNotes, //
      String psychiatryBedCount, String psychiatryBedNotes, //
      String burnBedCount, String burnBedNotes, //
      String criticalBedCount, String criticalBedNotes, //
      String other1Name, String other1BedCount, String other1BedNotes, //
      String other2Name, String other2BedCount, String other2BedNotes, //
      String totalBedCount, String additionalComments, String version) {
    super(exportedMessage);

    this.organization = organization;
    this.isExercise = isExercise;
    this.formDateTime = formDateTime;
    this.formLocation = formLocation;

    this.facility = facility;
    this.streetAddress = streetAddress;
    this.city = city;
    this.state = state;
    this.zip = zip;

    this.contactPerson = contactPerson;
    this.contactPhone = contactPhone;
    this.contactEmail = contactEmail;

    this.emergencyBedCount = emergencyBedCount;
    this.emergencyBedNotes = emergencyBedNotes;

    this.pediatricsBedCount = pediatricsBedCount;
    this.pediatricsBedNotes = pediatricsBedNotes;

    this.medicalBedCount = medicalBedCount;
    this.medicalBedNotes = medicalBedNotes;

    this.psychiatryBedCount = psychiatryBedCount;
    this.psychiatryBedNotes = psychiatryBedNotes;

    this.burnBedCount = burnBedCount;
    this.burnBedNotes = burnBedNotes;

    this.criticalBedCount = criticalBedCount;
    this.criticalBedNotes = criticalBedNotes;

    this.other1Name = other1Name;
    this.other1BedCount = other1BedCount;
    this.other1BedNotes = other1BedNotes;

    this.other2Name = other2Name;
    this.other2BedCount = other2BedCount;
    this.other2BedNotes = other2BedNotes;

    this.totalBedCount = totalBedCount;
    this.additionalComments = additionalComments;
    this.version = version;

    if (formLocation.isValid()) {
      mapLocation = formLocation;
    } else {
      mapLocation = msgLocation;
    }

    if (formDateTime != null) {
      sortDateTime = formDateTime;
    }
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "Organization", "IsExercise", "Facility", //
        "Street Address", "City", "State", "Zip", //
        "ContactPerson", "ContactPhone", "ContactEmail", //
        "MedicalBedCount", "MedicalBedNotes", //
        "CriticalBedCount", "CriticalBedNotes", //
        "TotalBedCount", "AdditionalComments", "Version", "File Name"//
    };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
        organization, String.valueOf(isExercise), facility, //
        streetAddress, city, state, zip, //
        contactPerson, contactPhone, contactEmail, //
        medicalBedCount, medicalBedNotes, //
        criticalBedCount, criticalBedNotes, //
        totalBedCount, additionalComments, version, fileName//
    };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HOSPITAL_BED;
  }

  @Override
  public String getMultiMessageComment() {
    return additionalComments;
  }
}
