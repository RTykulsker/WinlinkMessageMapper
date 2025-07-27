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

package com.surftools.wimp.parser;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.WA_ISNAP_Message;

public class WaIsnapParser extends AbstractBaseParser {

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {
      String xmlString = new String(message.attachments.get(MessageType.WA_ISNAP.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      var formDate = getStringFromXml("date");
      var formTime = getStringFromXml("time");
      var isnapVersion = getStringFromXml("isn_ver");
      var incidentType = getStringFromXml("inc_type");
      var stateMissionNumber = getStringFromXml("sta_mis_num");

      var affectedJurisdictions = getStringFromXml("aff_jur");
      var reportingJurisdiction = getStringFromXml("rep_jur");

      var pointOfContact = getStringFromXml("poi_con");
      var eocStatus = getStringFromXml("eoc_sta");
      var countyStatus = getStringFromXml("cty_sta");

      var description = getStringFromXml("sit");

      var governmentStatus = getStringFromXml("selec1");
      var governmentComments = getStringFromXml("gvt_cmt");

      var transportationStatus = getStringFromXml("selec2");
      var transportationComments = getStringFromXml("tran_cmt");

      var utilitiesStatus = getStringFromXml("selec3");
      var utilitiesComments = getStringFromXml("util_cmt");

      var medicalStatus = getStringFromXml("selec4");
      var medicalComments = getStringFromXml("med_cmt");

      var communicationsStatus = getStringFromXml("selec5");
      var communicationsComments = getStringFromXml("comm_cmt");

      var publicSafetyStatus = getStringFromXml("selec6");
      var publicSafetyComments = getStringFromXml("psaf_cmt");

      var environmentStatus = getStringFromXml("selec7");
      var environmentComments = getStringFromXml("envi_cmt");

      var m = new WA_ISNAP_Message(message, //
          formDate, formTime, isnapVersion, incidentType, stateMissionNumber, //

          affectedJurisdictions, reportingJurisdiction, //

          pointOfContact, eocStatus, countyStatus, //

          description, //

          governmentStatus, governmentComments, //

          transportationStatus, transportationComments, //

          utilitiesStatus, utilitiesComments, //

          medicalStatus, medicalComments, //

          communicationsStatus, communicationsComments, //

          publicSafetyStatus, publicSafetyComments, //

          environmentStatus, environmentComments);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
