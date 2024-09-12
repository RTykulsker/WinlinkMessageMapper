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

import java.util.Arrays;
import java.util.List;

import com.surftools.wimp.core.MessageType;

public class PdfIcs309Message extends Ics309Message {

  public final String pdfAttachmentIndices;
  public final boolean areActivitiesCombined;

  public PdfIcs309Message(ExportedMessage exportedMessage, String organization, String taskNumber,
      String dateTimePrepared, String operationalPeriod, String taskName, String operatorName, String stationId,
      String incidentName, String page, String version, List<Activity> activities, String pdfAttachmentIndices,
      boolean areActivitiesCombined) {
    super(exportedMessage, organization, taskNumber, dateTimePrepared, operationalPeriod, taskName, operatorName,
        stationId, incidentName, page, version, activities);

    this.pdfAttachmentIndices = pdfAttachmentIndices;
    this.areActivitiesCombined = areActivitiesCombined;
  }

  public static String[] getStaticHeaders() {
    var superHeaders = Ics309Message.getStaticHeaders();
    var headers = new String[] { "309 attachment indexes", "MULTIPLE 309s", "Activities Combined" };

    var result = Arrays.copyOf(superHeaders, superHeaders.length + headers.length);
    System.arraycopy(headers, 0, result, superHeaders.length, headers.length);
    return result;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  @Override
  public String[] getValues() {
    var multiple309s = pdfAttachmentIndices.contains(",") ? "***" : "";
    var showCombined = areActivitiesCombined ? "***" : "";
    var values = new String[] { pdfAttachmentIndices, multiple309s, showCombined };

    var superValues = super.getValues();
    var result = Arrays.copyOf(superValues, superValues.length + values.length);
    System.arraycopy(values, 0, result, superValues.length, values.length);
    return result;
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.PDF_ICS_309;
  }

}
