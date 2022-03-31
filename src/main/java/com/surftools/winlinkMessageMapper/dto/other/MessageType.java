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

package com.surftools.winlinkMessageMapper.dto.other;

import java.util.ArrayList;

public enum MessageType {

  CHECK_IN(0, "check_in", true, "RMS_Express_Form_Winlink_Check_In_Viewer.xml"), //
  CHECK_OUT(1, "check_out", true, "RMS_Express_Form_Winlink_Check_out_Viewer.xml"), //
  SPOTREP(2, "spotrep", true, "RMS_Express_Form_Shares_Spotrep-2_Viewer.xml"), //
  DYFI(3, "dyfi", true), //

  WX_LOCAL(4, "wx_local", true, "RMS_Express_Form_Local Weather Report Viewer.xml"), //
  WX_SEVERE(5, "wx_severe", true, "RMS_Express_Form_Severe WX Report viewer.xml"), //
  WX_HURRICANE(6, "wx_hurricane", true), //

  HOSPITAL_BED(7, "hospital_bed", true, "RMS_Express_Form_Hospital_Bed_Report_Viewer.xml"), //

  POSITION(8, "position", true), //

  ICS_213(9, "ics_213", false, "RMS_Express_Form_ICS213_Initial_Viewer.xml"), //
  GIS_ICS_213(10, "gis_ics_213", true), //
  ETO_CHECK_IN(11, "eto_check_in", true), //

  REJECTS(12, "rejects"), //
  UNKNOWN(13, "unknown"), // can't infer message type

  FIELD_SITUATION_REPORT(14, "field_situation", true, "RMS_Express_Form_Field Situation Report_viewer.xml"), //

  ;

  /**
   * id serves NO purpose other than to discourage re-ordering of values
   *
   * this will be needed when reading/writing counts by type
   */
  private final int id;
  private final String key;
  private final boolean isGis;
  private final String attachmentName;

  private MessageType(int id, String key, boolean isGis, String attachmentName) {
    this.id = id;
    this.key = key;
    this.isGis = isGis;
    this.attachmentName = attachmentName;
  }

  private MessageType(int id, String key, boolean isGis) {
    this(id, key, isGis, null);
  }

  private MessageType(int id, String key) {
    this(id, key, false, null);
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

  public boolean isGisType() {
    return isGis;
  }

  public String attachmentName() {
    return attachmentName;
  }

  public int id() {
    return id;
  }
}
