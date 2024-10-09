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

import java.util.LinkedHashMap;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.WA_WSDOT_BridgeRoadwayDamageMessage;
import com.surftools.wimp.message.WA_WSDOT_BridgeRoadwayDamageMessage.DamageType;

public class WA_WSDOT_BridgeRoadwayDamageParser extends AbstractBaseParser {

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {
      String xmlString = new String(
          message.attachments.get(MessageType.WA_WSDOT_BRIDGE_ROADWAY_DAMAGE.attachmentName()));
      makeDocument(message.messageId, xmlString);

      var isExercise = getStringFromXml("isexercise");
      var formDate = getStringFromXml("inspectdate");
      var formTime = getStringFromXml("inspecttime");
      var status = getStringFromXml("status");
      var region = getStringFromXml("region");
      var county = getStringFromXml("county");
      var route = getStringFromXml("route");
      var milepost = getStringFromXml("milepost");
      var bridgeNumber = getStringFromXml("bridgenumber");
      var location = getStringFromXml("location");
      var inspectorName = getStringFromXml("inspector");
      var remarks = getStringFromXml("remarks");

      var damageMap = new LinkedHashMap<DamageType, String>();
      for (var dt : DamageType.values()) {
        var value = getStringFromXml(dt.getFieldName());
        damageMap.put(dt, value);
      }

      var commLogSendingStation = getStringFromXml("sendingstation");
      var commLogReceivingStation = getStringFromXml("receivingstation");
      var commLogFrequencyMHz = getStringFromXml("freq");
      var commLogReceivedLocal = getStringFromXml("timesend");

      var templateversion = getStringFromXml("templateversion");
      var version = templateversion == null ? "unknown" : templateversion.split(" ")[4];

      var m = new WA_WSDOT_BridgeRoadwayDamageMessage(message, //
          isExercise, formDate, formTime, status, //
          region, county, route, milepost, bridgeNumber, //
          location, inspectorName, //
          remarks, damageMap, //
          commLogSendingStation, commLogReceivingStation, commLogFrequencyMHz, commLogReceivedLocal, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}