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

package com.surftools.winlinkMessageMapper.dto;

// see https://protect-public.hhs.gov/pages/hospital-utilization for real-world data

public class LAXHospitalBedMessage extends GisMessage {

  public final String facility;
  public final String serviceLevel;

  public final String contactPerson;
  public final String contactPhone;

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

  public final String additionalComments;

  private static boolean includeAllFields = false;

  public LAXHospitalBedMessage(ExportedMessage xmlMessage, String latitude, String longitude, //
      String facility, String serviceLevel, String contactPerson, String contactPhone, //
      String emergencyBedCount, String emergencyBedNotes, //
      String pediatricsBedCount, String pediatricsBedNotes, //
      String medicalBedCount, String medicalBedNotes, //
      String psychiatryBedCount, String psychiatryBedNotes, //
      String burnBedCount, String burnBedNotes, //
      String criticalBedCount, String criticalBedNotes, //
      String other1Name, String other1BedCount, String other1BedNotes, //
      String other2Name, String other2BedCount, String other2BedNotes, //
      String totalBedCount, String additionalComments) {
    super(xmlMessage, latitude, longitude, "");
    this.facility = facility;
    this.serviceLevel = serviceLevel;

    this.contactPerson = contactPerson;
    this.contactPhone = contactPhone;

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

    this.additionalComments = additionalComments;
  }

  public static void setIncludeAllFields(boolean _includeAllFields) {
    includeAllFields = _includeAllFields;
  }

  @Override
  public String[] getHeaders() {
    if (includeAllFields) {
      return new String[] { //
          "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
          "Facility", "ServiceLevel", "ContactPerson", "ContactPhone", //
          "EmergencyBedCount", "EmergencyBedNotes", //
          "PediatricsBedCount", "PediatricsBedNotes", //
          "MedicalBedCount", "MedicalBedNotes", //
          "PsychiatryBedCount", "PsychiatryBedNotes", //
          "BurnBedCount", "BurnBedNotes", //
          "CriticalBedCount", "CriticalBedNotes", //
          "Other1Name", "Other1BedCount", "Other1BedNotes", //
          "Other2Name", "Other2BedCount", "Other2BedNotes", //
          "AdditionalComments"//
      };
    } else {
      return new String[] { //
          "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
          "Facility", "ServiceLevel", "ContactPerson", "ContactPhone", //
          "MedicalBedCount", "MedicalBedNotes", //
          "CriticalBedCount", "CriticalBedNotes", //
          "AdditionalComments"//
      };
    }
  }

  @Override
  public String[] getValues() {
    if (includeAllFields) {
      return new String[] { //
          messageId, from, to, subject, date, time, latitude, longitude, //
          facility, serviceLevel, contactPerson, contactPhone, //
          emergencyBedCount, emergencyBedNotes, //
          pediatricsBedCount, pediatricsBedNotes, //
          medicalBedCount, medicalBedNotes, //
          psychiatryBedCount, psychiatryBedNotes, //
          burnBedCount, burnBedNotes, //
          criticalBedCount, criticalBedNotes, //
          other1Name, other1BedCount, other1BedNotes, //
          other2Name, other2BedCount, other2BedNotes, //
          additionalComments//
      };
    } else {
      return new String[] { //
          messageId, from, to, subject, date, time, latitude, longitude, //
          facility, serviceLevel, contactPerson, contactPhone, //
          medicalBedCount, medicalBedNotes, //
          criticalBedCount, criticalBedNotes, //
          additionalComments//
      };
    }
  }
}
