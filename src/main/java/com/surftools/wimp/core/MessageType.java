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

/**
 * enumeration to define the various message types that we know how to handle
 *
 * @author bobt
 *
 */

public enum MessageType {

  EXPORTED("exported"), // read but not classified
  PLAIN("plain"), // attempted to classify, but can't infer message type
  REJECTS("rejects"), //
  ACK("ack", (s -> s.startsWith("ACK:"))), //
  POSITION("position", (s -> s.equals("Position Report"))), //

  CHECK_IN("check_in", "RMS_Express_Form_Winlink_Check_In_Viewer.xml", "Winlink Check-in"), //
  CHECK_OUT("check_out", "RMS_Express_Form_Winlink_Check_out_Viewer.xml", "Winlink Check-out"), //
  SPOTREP("spotrep", "RMS_Express_Form_Shares_Spotrep-2_Viewer.xml"), //
  FIELD_SITUATION("fsr", "RMS_Express_Form_Field Situation Report"), //
  DYFI("dyfi", (s -> s.startsWith("DYFI Automatic Entry") || s.startsWith("FW: DYFI Automatic Entry"))), //

  WX_LOCAL("wx_local", "RMS_Express_Form_Local Weather Report Viewer.xml"), //
  WX_SEVERE("wx_severe", "RMS_Express_Form_Severe WX Report viewer.xml"), //
  WX_HURRICANE("wx_hurricane", (s -> s.startsWith("HurricaneReport"))), //

  HOSPITAL_BED("hospital_bed", "RMS_Express_Form_Hospital_Bed_Report_Viewer.xml"), //

  ICS_213("ics_213", "RMS_Express_Form_ICS213_Initial_Viewer.xml", "ICS-213"), //
  ICS_213_REPLY("ics_213_reply", "RMS_Express_Form_ICS213_SendReply_Viewer.xml"), //
  ICS_213_RR("ics_213_rr", "RMS_Express_Form_ICS213RR_Viewer.xml"), //
  ICS_214("ics_214", "RMS_Express_Form_ICS214_Viewer.xml"), //
  ICS_214A("ics_214A", "RMS_Express_Form_ICS214A_Viewer.xml"), // Individual
  ICS_309("ics_309", "RMS_Express_Form_ICS309_Viewer.xml"), //
  PDF_ICS_309("pfd_ics_309"), // generated by Winlink Express

  ETO_CHECK_IN("eto_check_in",
      (s -> s.startsWith("Winlink Thursday Net Check-In") || s.startsWith("Re: Winlink Thursday Net Check-In"))), //
  ETO_CHECK_IN_V2("eto_check_in_v2",
      (s -> s.startsWith("ETO Winlink Thursday Check-In") || s.startsWith("Re: ETO Winlink Thursday Check-In"))), //
  MIRO_CHECK_IN("miro_check_in",
      (s -> s.equals("MIRO Check In") || s.startsWith("MIRO Winlink Check In") || s.startsWith("MIRO After Action"))),

  DAMAGE_ASSESSMENT("windshield_damage_assessment", "RMS_Express_Form_Damage_Assessment_Viewer.xml"), //
  QUICK("quick", "RMS_Express_Form_Quick Message Viewer.xml"), //
  ICS_205_RADIO_PLAN("ics_205", "RMS_Express_Form_ICS205 Radio Plan_Viewer.xml"), //
  HUMANITARIAN_NEEDS("humanitarian", "RMS_Express_Form_Humanitarian Needs Identification viewer.xml"), //
  HOSPITAL_STATUS("hospital_status", "RMS_Express_Form_Hospital_Status_Viewer.xml"), //

  ETO_RESUME("eto_resume",
      (s -> s.startsWith("ETO Participant resume") || s.startsWith("ETO Resume") || s.startsWith("ETO-RESUME"))), //
  RRI_QUICK_WELFARE("rri_quick_welfare",
      (s -> s.startsWith("I Am Safe Message From") && s.endsWith(" - DO NOT REPLY!"))), //
  RRI_WELFARE_RADIOGRAM("rri_welfare_radiogram", (s -> s.startsWith("QTC 1 W") || s.startsWith("QTC 1 TEST W"))), //
  RRI_REPLY_WELFARE_RADIOGRRAM("rri_reply_welfare_radiogram",
      (s -> s.toUpperCase().startsWith("RE: QTC 1 W") || s.toUpperCase().startsWith("RE: QTC 1 TEST W"))), //

  WA_ICS_213_RR_WEB_EOC("wa_ics_213_rr_web_eoc", "RMS_Express_Form_RR_WebEOC_WA_Viewer.xml"), //
  WA_ICS_213_RR("wa_ics_213_rr", "RMS_Express_Form_ICS213RR_WA_Viewer.xml"), //
  WA_ISNAP("wa_isnap", "RMS_Express_Form_ISNAP_WA_Viewer.xml"), //
  WA_WSDOT_BRIDGE_DAMAGE("wa_wsdot_bridge_damage", "RMS_Express_Form_WSDOT Bridge Damage Report viewer.xml"), //
  WA_WSDOT_ROADWAY_DAMAGE("wa_wsdot_roadway_damage", "RMS_Express_Form_WSDOT Roadway Damage Report viewer.xml"), //
  WA_WSDOT_BRIDGE_ROADWAY_DAMAGE("wa_wsdot_bridge_roadway_damage",
      "RMS_Express_Form_WSDOT Bridge-Roadway Damage Report viewer.xml"), //
  WA_EYEWARN("wa_eyewarn", "RMS_Express_Form_EyeWarn_Form_Viewer.xml"), //

  WELFARE_BULLETIN_BOARD("welfare_bulletin_board", "RMS_Express_Form_Welfare Bulletin Board viewer.xml"), //

  EYEWARN("eyewarn", "RMS_Express_Form_Eyewarn.xml"), // custom form from SnoVArc
  EYEWARN_DETAIL("eyewarn_details"), // generated details

  PEGELSTAND("pegelstand", (s -> s.startsWith("Pegelstand Report"))), // German water level message, for Tsunami 2025

  HICS_259("hics_259", "RMS_Express_Form_HICS 259_viewer.xml"), //
  ;

  private final String key;
  private final String rmsViewerName;
  private final String formDataName;
  private final Predicate<String> subjectPredicate;

  private MessageType(String key, String rmsViewerName, String formDataName, Predicate<String> subjectPredicate) {
    this.key = key;
    this.rmsViewerName = rmsViewerName;
    this.formDataName = formDataName;
    this.subjectPredicate = subjectPredicate;
  }

  private MessageType(String key, String attachmentName) {
    this(key, attachmentName, null, null);
  }

  private MessageType(String key, String attachmentName, String formDataName) {
    this(key, attachmentName, formDataName, null);
  }

  private MessageType(String key) {
    this(key, null, null, null);
  }

  private MessageType(String key, Predicate<String> subjectPredicate) {
    this(key, null, null, subjectPredicate);
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
    return key;
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

}
