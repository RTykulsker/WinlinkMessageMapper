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
 * Washington State, WA-ICS-213-RR, not used by ETO
 *
 * @author bobt
 *
 */
public class WaResourceRequestMessage extends ExportedMessage implements ExpectGradableMessage {
  public static final String[] requestorTags = { //
      "incname", //
      "agname", //
      "datetime", //
      "reqtracknum", //
      "qty1", //
      "kind1", //
      "type1", //
      "item1", //
      "reqdatetime1", //
      "supneed", //
      "duration", //
      "reqloc1", //
      "reqloc", //
      "sub", //
      "priority", // or should it be priority1
      "b12a", //
      "b12b", //
      "b12c", //
      "rb13", //
      "explain", //
      "reqname", //
      "reqauth", //
  };

  public static final String[] requestorLabels = { //
      "Incident Name", //
      "Agency Name", //
      "Date & Time", //
      "Requestor Tracking #", //
      "Quantity", //
      "Kind", //
      "Type", //
      "Description", //
      "Requested Date/Time", //
      "Support Needed", //
      "Duration", //
      "Delivery/Report Location", //
      "Delivery/Report POC", //
      "Substitutes", //
      "Priority", //
      "All commerical resources exhausted", //
      "All local resources exhausted", //
      "All mutual aid resources exhausted", //
      "Requester willing to fund", //
      "If 'No', explain", //
      "Requested By", //
      "Authorized By" //
  };

  public static final String[] otherTags = { //
      "estdatetime1", //
      "cost1", //

      "eocnum", //
      "supname1", //
      "notes", //
      "datetime1", //
      "other", //
      "orderby", //
      "logrep", //

      "elevate", //
      "statenum", //
      "matracking", //

      "fincomm", //
      "finrepname", //
      "datetime2", //
  };

  public static final String[] otherLabels = { //
      "Estimated Date/Time", //
      "Estimated Cost", //

      "EOC/ECC Tracking #", //
      "Name of Supplier", //
      "Notes", //
      "Log Approve Date/Time", //
      "Other", //
      "Order Placed By", //
      "Logistics Representative", //

      "Elevate to State", //
      "State Tracking #", //
      "Mutual Aid Tracking", //

      "Comments from Finance", //
      "Finance Section Signature", //
      "Fin Approve Date/Time", //
  };

  public final Map<String, String> map;
  private boolean isGraded;
  private String grade;
  private String explanation;

  public WaResourceRequestMessage(ExportedMessage xmlMessage, Map<String, String> map) {
    super(xmlMessage);
    this.map = map;
  }

  @Override
  public String[] getHeaders() {
    var list = new ArrayList<String>(5 + requestorLabels.length + ((isGraded) ? 2 : 0));
    list.addAll(List.of("MessageId", "From", "To", "Subject", "Date", "Time"));
    list.addAll(Arrays.asList(requestorLabels));
    if (isGraded) {
      list.addAll(List.of("Grade", "Explanation"));
    }
    var array = new String[list.size()];
    list.toArray(array);
    return array;
  }

  @Override
  public String[] getValues() {
    var list = new ArrayList<String>(5 + requestorTags.length + ((isGraded) ? 2 : 0));
    list.addAll(List.of(messageId, from, to, subject, date, time));
    for (var key : requestorTags) {
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
