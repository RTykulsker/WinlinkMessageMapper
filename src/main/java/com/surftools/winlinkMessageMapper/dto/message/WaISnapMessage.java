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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.expect.ExpectGradableMessage;

/**
 * Washington State ISNAP message, not used by ETO
 *
 * @author bobt
 *
 */
public class WaISnapMessage extends ExportedMessage implements ExpectGradableMessage {
  public static final String[] tags = { //
      "date", //
      "time", //
      "isn_ver", //
      "inc_type", //
      "sta_mis_num", //

      "aff_jur", //
      "rep_jur", //

      "poi_con", //
      "eoc_sta", //
      "cty_sta", //

      "sit", //

      "selec1", //
      "gvt_cmt", //

      "selec2", //
      "tran_cmt", //

      "selec3", //
      "util_cmt", //

      "selec4", //
      "med_cmt", //

      "selec5", //
      "comm_cmt", //

      "selec6", //
      "psaf_cmt", //

      "selec7", //
      "envi_cmt", //
  };

  public static final String[] labels = { //
      "Date", //
      "Time", //
      "ISNAP Version", //
      "Incident Type", //
      "State Mission Number", //

      "Affected Jurisdictions", //
      "Reporting Jurisdiction", //

      "Point of Contact", //
      "EOC Status", //
      "County Status", //

      "Situation", //

      "Government Status", //
      "Government Comments", //

      "Transportation Status", //
      "Transportation Comments", //

      "Utilities Status", //
      "Utilities Comments", //

      "Medical Status", //
      "Medical Comments", //

      "Communications Status", //
      "Communications Comments", //

      "Public Safety Status", //
      "Public Safety Comments", //

      "Environment Status", //
      "Environment Comments", //

  };

  public final Map<String, String> map;
  private boolean isGraded;
  private String grade;
  private String explanation;

  public WaISnapMessage(ExportedMessage xmlMessage, Map<String, String> map) {
    super(xmlMessage);
    this.map = map;
  }

  @Override
  public String[] getHeaders() {
    var list = new ArrayList<String>(5 + labels.length + ((isGraded) ? 2 : 0));
    list.addAll(List.of("MessageId", "From", "To", "Subject", "Date", "Time"));
    list.addAll(Arrays.asList(labels));
    if (isGraded) {
      list.addAll(List.of("Grade", "Explanation"));
    }
    var array = new String[list.size()];
    list.toArray(array);
    return array;
  }

  @Override
  public String[] getValues() {
    var list = new ArrayList<String>(5 + tags.length + ((isGraded) ? 2 : 0));
    list.addAll(List.of(messageId, from, to, subject, date, time));
    for (var key : tags) {
      list.add(map.get(key));
    }
    if (isGraded) {
      list.addAll(List.of(grade, explanation));
    }
    var array = new String[list.size()];
    list.toArray(array);
    return array;
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_RR;
  }

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
  public Map<String, String> getMap() {
    return map;
  }
}
