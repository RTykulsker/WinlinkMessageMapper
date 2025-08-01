/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.wimp.core;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * enumeration to define the various message types that we know how to handle
 *
 * @author bobt
 *
 */

public enum MessageType {

  EXPORTED(), // read but not classified
  PLAIN(), // attempted to classify, but can't infer message type
  REJECTS(), //
  ACK((s -> s.startsWith("ACK:"))), //
  POSITION((s -> s.equals("Position Report"))), //

  CHECK_IN("RMS_Express_Form_Winlink_Check_In_Viewer.xml", "Winlink Check-in"), //
  CHECK_OUT("RMS_Express_Form_Winlink_Check_out_Viewer.xml", "Winlink Check-out"), //
  SPOTREP("RMS_Express_Form_Shares_Spotrep-2_Viewer.xml"), //
  FIELD_SITUATION("RMS_Express_Form_Field Situation Report"), //
  DYFI((s -> s.startsWith("DYFI Automatic Entry") || s.startsWith("FW: DYFI Automatic Entry"))), //

  WX_LOCAL("RMS_Express_Form_Local Weather Report Viewer.xml"), //
  WX_SEVERE("RMS_Express_Form_Severe WX Report viewer.xml"), //
  WX_HURRICANE((s -> s.startsWith("HurricaneReport"))), //

  ICS_205("RMS_Express_Form_ICS205 Radio Plan_Viewer.xml"), //
  ICS_213("RMS_Express_Form_ICS213_Initial_Viewer.xml", "ICS-213"), //
  ICS_213_REPLY("RMS_Express_Form_ICS213_SendReply_Viewer.xml"), //
  ICS_213_RR("RMS_Express_Form_ICS213RR_Viewer.xml"), //
  ICS_214("RMS_Express_Form_ICS214_Viewer.xml"), //
  ICS_214A("RMS_Express_Form_ICS214A_Viewer.xml"), // Individual
  ICS_309("RMS_Express_Form_ICS309_Viewer.xml"), //
  PDF_ICS_309(), // generated by Winlink Express

  ETO_CHECK_IN(
      (s -> s.startsWith("Winlink Thursday Net Check-In") || s.startsWith("Re: Winlink Thursday Net Check-In"))), //
  ETO_CHECK_IN_V2(
      (s -> s.startsWith("ETO Winlink Thursday Check-In") || s.startsWith("Re: ETO Winlink Thursday Check-In"))), //
  ETO_RESUME((s -> s.startsWith("ETO Participant resume") || s.startsWith("ETO Resume") || s.startsWith("ETO-RESUME"))), //
  MIRO_CHECK_IN(
      (s -> s.equals("MIRO Check In") || s.startsWith("MIRO Winlink Check In") || s.startsWith("MIRO After Action"))),

  DAMAGE_ASSESSMENT("RMS_Express_Form_Damage_Assessment_Viewer.xml"), //
  QUICK("RMS_Express_Form_Quick Message Viewer.xml"), //

  HICS_259("RMS_Express_Form_HICS 259_viewer.xml"), //
  HOSPITAL_BED("RMS_Express_Form_Hospital_Bed_Report_Viewer.xml"), //
  HOSPITAL_STATUS("RMS_Express_Form_Hospital_Status_Viewer.xml"), //
  HUMANITARIAN_NEEDS("RMS_Express_Form_Humanitarian Needs Identification viewer.xml"), //

  RRI_QUICK_WELFARE((s -> s.startsWith("I Am Safe Message From") && s.endsWith(" - DO NOT REPLY!"))), //
  RRI_WELFARE_RADIOGRAM((s -> s.startsWith("QTC 1 W") || s.startsWith("QTC 1 TEST W"))), //
  RRI_REPLY_WELFARE_RADIOGRAM(
      (s -> s.toUpperCase().startsWith("RE: QTC 1 W") || s.toUpperCase().startsWith("RE: QTC 1 TEST W"))), //

  WA_ICS_213_RR_WEB_EOC("RMS_Express_Form_RR_WebEOC_WA_Viewer.xml"), //
  WA_ICS_213_RR("RMS_Express_Form_ICS213RR_WA_Viewer.xml"), //
  WA_ISNAP("RMS_Express_Form_ISNAP_WA_Viewer.xml"), //
  WA_WSDOT_BRIDGE_DAMAGE("RMS_Express_Form_WSDOT Bridge Damage Report viewer.xml"), //
  WA_WSDOT_ROADWAY_DAMAGE("RMS_Express_Form_WSDOT Roadway Damage Report viewer.xml"), //
  WA_WSDOT_BRIDGE_ROADWAY_DAMAGE("RMS_Express_Form_WSDOT Bridge-Roadway Damage Report viewer.xml"), //
  WA_EYEWARN("RMS_Express_Form_EyeWarn_Form_Viewer.xml"), //

  WELFARE_BULLETIN_BOARD("RMS_Express_Form_Welfare Bulletin Board viewer.xml"), //

  EYEWARN("RMS_Express_Form_Eyewarn.xml"), // custom form from SnoVArc
  EYEWARN_DETAIL(), // generated details

  PEGELSTAND((s -> s.startsWith("Pegelstand Report"))), // German water level message, for Tsunami 2025

  ;

  private final String rmsViewerName;
  private final String formDataName;
  private final Predicate<String> subjectPredicate;

  private MessageType(String rmsViewerName, String formDataName, Predicate<String> subjectPredicate) {
    this.rmsViewerName = rmsViewerName;
    this.formDataName = formDataName;
    this.subjectPredicate = subjectPredicate;
  }

  private MessageType() {
    this(null, null, null);
  }

  private MessageType(String attachmentName) {
    this(attachmentName, null, null);
  }

  private MessageType(String attachmentName, String formDataName) {
    this(attachmentName, formDataName, null);
  }

  private MessageType(Predicate<String> subjectPredicate) {
    this(null, null, subjectPredicate);
  }

  public static final String getAllNames() {
    var strings = new ArrayList<String>();
    for (MessageType type : values()) {
      strings.add(type.toString());
    }
    return String.join(", ", strings);
  }

  public static MessageType fromString(String string) {
    for (MessageType key : MessageType.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return name().toLowerCase();
  }

  public String rmsViewerName() {
    return rmsViewerName;
  }

  public String formDataName() {
    return formDataName;
  }

  public boolean testSubject(String subject) {
    return subjectPredicate != null && subjectPredicate.test(subject);
  }

  public Predicate<String> getSubjectPredicate() {
    return subjectPredicate;
  }

  public String makeParserName() {
    var parserName = "";
    var fields = name().toLowerCase().split("_");
    for (var field : fields) {
      parserName += StringUtils.capitalize(field);
    }
    return parserName;
  }
}
