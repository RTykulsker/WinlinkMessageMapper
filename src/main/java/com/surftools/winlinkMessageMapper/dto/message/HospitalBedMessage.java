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
import com.surftools.winlinkMessageMapper.grade.GradableMessage;

// see https://protect-public.hhs.gov/pages/hospital-utilization for real-world data

public class HospitalBedMessage extends GisMessage implements GradableMessage {

  public final String facility;

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

  private boolean isGraded;
  private String grade;
  private String explanation;

  private static boolean includeAllFields = true;

  public HospitalBedMessage(ExportedMessage xmlMessage, String latitude, String longitude, //
      String organization, String facility, String contactPerson, String contactPhone, String contactEmail, //
      String emergencyBedCount, String emergencyBedNotes, //
      String pediatricsBedCount, String pediatricsBedNotes, //
      String medicalBedCount, String medicalBedNotes, //
      String psychiatryBedCount, String psychiatryBedNotes, //
      String burnBedCount, String burnBedNotes, //
      String criticalBedCount, String criticalBedNotes, //
      String other1Name, String other1BedCount, String other1BedNotes, //
      String other2Name, String other2BedCount, String other2BedNotes, //
      String totalBedCount, String additionalComments) {
    super(xmlMessage, latitude, longitude, organization);
    this.facility = facility;

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
  }

  public static void setIncludeAllFields(boolean _includeAllFields) {
    includeAllFields = _includeAllFields;
  }

  @Override
  public String[] getHeaders() {
    if (includeAllFields) {
      if (isGraded) {
        return new String[] { //
            "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
            "Organization", "Facility", "ContactPerson", "ContactPhone", "ContactEmail", //
            "EmergencyBedCount", "EmergencyBedNotes", //
            "PediatricsBedCount", "PediatricsBedNotes", //
            "MedicalBedCount", "MedicalBedNotes", //
            "PsychiatryBedCount", "PsychiatryBedNotes", //
            "BurnBedCount", "BurnBedNotes", //
            "CriticalBedCount", "CriticalBedNotes", //
            "Other1Name", "Other1BedCount", "Other1BedNotes", //
            "Other2Name", "Other2BedCount", "Other2BedNotes", //
            "TotalBedCount", "AdditionalComments", "Grade", "Explanation"//
        };
      } else {
        return new String[] { //
            "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
            "Organization", "Facility", "ContactPerson", "ContactPhone", "ContactEmail", //
            "EmergencyBedCount", "EmergencyBedNotes", //
            "PediatricsBedCount", "PediatricsBedNotes", //
            "MedicalBedCount", "MedicalBedNotes", //
            "PsychiatryBedCount", "PsychiatryBedNotes", //
            "BurnBedCount", "BurnBedNotes", //
            "CriticalBedCount", "CriticalBedNotes", //
            "Other1Name", "Other1BedCount", "Other1BedNotes", //
            "Other2Name", "Other2BedCount", "Other2BedNotes", //
            "TotalBedCount", "AdditionalComments"//
        };
      }
    } else {
      if (isGraded) {
        return new String[] { //
            "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
            "Organization", "Facility", "ContactPerson", "ContactPhone", "ContactEmail", //
            "MedicalBedCount", "MedicalBedNotes", //
            "CriticalBedCount", "CriticalBedNotes", //
            "TotalBedCount", "AdditionalComments", "Grade", "Explanation"//
        };
      } else {
        return new String[] { //
            "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
            "Organization", "Facility", "ContactPerson", "ContactPhone", "ContactEmail", //
            "MedicalBedCount", "MedicalBedNotes", //
            "CriticalBedCount", "CriticalBedNotes", //
            "TotalBedCount", "AdditionalComments"//
        };
      }
    }
  }

  @Override
  public String[] getValues() {
    if (includeAllFields) {
      if (isGraded) {
        return new String[] { //
            messageId, from, to, subject, date, time, latitude, longitude, //
            organization, facility, contactPerson, contactPhone, contactEmail, //
            emergencyBedCount, emergencyBedNotes, //
            pediatricsBedCount, pediatricsBedNotes, //
            medicalBedCount, medicalBedNotes, //
            psychiatryBedCount, psychiatryBedNotes, //
            burnBedCount, burnBedNotes, //
            criticalBedCount, criticalBedNotes, //
            other1Name, other1BedCount, other1BedNotes, //
            other2Name, other2BedCount, other2BedNotes, //
            totalBedCount, additionalComments, grade, explanation//
        };
      } else {
        return new String[] { //
            messageId, from, to, subject, date, time, latitude, longitude, //
            organization, facility, contactPerson, contactPhone, contactEmail, //
            emergencyBedCount, emergencyBedNotes, //
            pediatricsBedCount, pediatricsBedNotes, //
            medicalBedCount, medicalBedNotes, //
            psychiatryBedCount, psychiatryBedNotes, //
            burnBedCount, burnBedNotes, //
            criticalBedCount, criticalBedNotes, //
            other1Name, other1BedCount, other1BedNotes, //
            other2Name, other2BedCount, other2BedNotes, //
            totalBedCount, additionalComments//
        };
      }
    } else { // all fields
      if (isGraded) {
        return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
            organization, facility, contactPerson, contactPhone, contactEmail, //
            medicalBedCount, medicalBedNotes, //
            criticalBedCount, criticalBedNotes, //
            totalBedCount, additionalComments, grade, explanation//
        };
      } else {
        return new String[] { messageId, from, to, subject, date, time, latitude, longitude, //
            organization, facility, contactPerson, contactPhone, contactEmail, //
            medicalBedCount, medicalBedNotes, //
            criticalBedCount, criticalBedNotes, //
            totalBedCount, additionalComments//
        };
      } // not graded
    } // not all fields
  } // get values

  @Override
  public boolean isGraded() {
    return isGraded;
  }

  @Override
  public void setIsGraded(boolean isGraded) {
    this.isGraded = isGraded;
  }

  @Override
  public String getGrade() {
    return grade;
  }

  @Override
  public void setGrade(String grade) {
    this.grade = grade;
  }

  @Override
  public String getExplanation() {
    return explanation;
  }

  @Override
  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HOSPITAL_BED;
  }
}
