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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.EyeWarnMessage;
import com.surftools.wimp.message.EyeWarnMessage.EyeWarnDetail;

/**
 * parser for Eyewarn Messages, custom SnoVArc
 *
 * @author bobt
 *
 */
public class EyewarnParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(EyewarnParser.class);

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    try {
      String xmlString = new String(message.attachments.get(MessageType.EYEWARN.attachmentName()));

      makeDocument(message.messageId, xmlString);

      var exerciseOrIncident = getStringFromXml("form-exercise");
      var formDate = getStringFromXml("form-date");
      var formTime = getStringFromXml("form-time");
      var ncs = getStringFromXml("form-ncs");
      var incidentName = getStringFromXml("form-name");
      var totalCheckins = getStringFromXml("form-checkins");
      var parseme = getStringFromXml("parseme");
      var redReports = makeDetails(parseme, "RED");
      var yellowReports = makeDetails(parseme, "YELLOW");
      var greenReports = makeDetails(parseme, "GREEN");
      var version = getVersion("template-version");

      var m = new EyeWarnMessage(message, exerciseOrIncident, //
          formDate, formTime, ncs, //
          incidentName, totalCheckins, //
          redReports, yellowReports, greenReports, //
          version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private List<EyeWarnDetail> makeDetails(String blob, String color) {
    var list = new ArrayList<EyeWarnDetail>();
    if (blob == null) {
      return list;
    }
    var key = color.toLowerCase() + "Reports";

    blob = blob.trim();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> map = null;
    try {
      map = mapper.readValue(blob, Map.class);
      var data1 = map.get(key);
      System.err.println("key: " + key + ", data: " + data1);
    } catch (Exception e) {
      throw new RuntimeException("can't parse json: " + e.getLocalizedMessage());
    }

    return list;
  }

}
