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
  ACK("ack", null), //
  POSITION("position"), //

  CHECK_IN("check_in", "RMS_Express_Form_Winlink_Check_In_Viewer.xml"), //
  CHECK_OUT("check_out", "RMS_Express_Form_Winlink_Check_out_Viewer.xml"), //
  SPOTREP("spotrep", "RMS_Express_Form_Shares_Spotrep-2_Viewer.xml"), //
  FIELD_SITUATION("FSR", "RMS_Express_Form_Field Situation Report"), //
  DYFI("dyfi"), //

  WX_LOCAL("wx_local", "RMS_Express_Form_Local Weather Report Viewer.xml"), //
  WX_SEVERE("wx_severe", "RMS_Express_Form_Severe WX Report viewer.xml"), //
  WX_HURRICANE("wx_hurricane"), //

  HOSPITAL_BED("hospital_bed", "RMS_Express_Form_Hospital_Bed_Report_Viewer.xml"), //

  ICS_213("ics_213", "RMS_Express_Form_ICS213_Initial_Viewer.xml"), //
  ICS_213_REPLY("ics_213_reply", "RMS_Express_Form_ICS213_SendReply_Viewer.xml"), //
  ICS_213_RR("ics_213_rr", "RMS_Express_Form_ICS213RR_Viewer.xml"), //
  ICS_214("ics_214", "RMS_Express_Form_ICS214_Viewer.xml"), //
  ICS_214A("ics_214A", "RMS_Express_Form_ICS214A_Viewer.xml"), // Individual
  ICS_309("ics_309", "RMS_Express_Form_ICS309_Viewer.xml"), //

  ETO_CHECK_IN("eto_check_in"), //
  ETO_CHECK_IN_V2("eto_check_in_v2"), //
  MIRO_CHECK_IN("miro_check_in"), //

  DAMAGE_ASSESSMENT("windshield_damage_assessment", "RMS_Express_Form_Damage_Assessment_Viewer.xml"), //
  QUICK("quick", "RMS_Express_Form_Quick Message Viewer.xml"), //
  ICS_205_RADIO_PLAN("ics_205", "RMS_Express_Form_ICS205 Radio Plan_Viewer.xml"), //
  HUMANITARIAN_NEEDS("humanitarian", "RMS_Express_Form_Humanitarian Needs Identification viewer.xml"), //
  HOSPITAL_STATUS("hospital_status", "RMS_Express_Form_Hospital_Status_Viewer.xml"), //

  ETO_RESUME("eto_resume"), //
  RRI_QUICK_WELFARE("rri_quick_welfare"), //
  ;

  /**
   * id serves NO purpose other than to discourage re-ordering of values
   *
   * this will be needed when reading/writing counts by type
   */

  private final String key;
  private final String attachmentName;

  private MessageType(String key, String attachmentName) {
    this.key = key;
    this.attachmentName = attachmentName;
  }

  private MessageType(String key) {
    this(key, null);
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

  public String attachmentName() {
    return attachmentName;
  }

}
