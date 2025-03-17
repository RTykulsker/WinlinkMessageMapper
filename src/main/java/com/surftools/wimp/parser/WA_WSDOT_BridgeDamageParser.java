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
import com.surftools.wimp.message.WA_WSDOT_BridgeDamageMessage;

public class WA_WSDOT_BridgeDamageParser extends AbstractBaseParser {

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {
      String xmlString = new String(message.attachments.get(MessageType.WA_WSDOT_BRIDGE_DAMAGE.attachmentName()));
      makeDocument(message.messageId, xmlString);

      var isExercise = getStringFromXml("isexercise") != null
          ? getStringFromXml("isexercise").equals("** THIS IS AN EXERCISE **")
          : false;
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

      var damageApproaches = getBooleanFromXmlButton("btna1");
      var damageWingWalls = getBooleanFromXmlButton("btna2");
      var damageAbutments = getBooleanFromXmlButton("btna3");
      var damageBentCapsAndColumns = getBooleanFromXmlButton("btna4");
      var damageBearings = getBooleanFromXmlButton("btna5");
      var damageBeamsGirders = getBooleanFromXmlButton("btna6");
      var damageDeck = getBooleanFromXmlButton("btna7");
      var damageHingeExpansionJoints = getBooleanFromXmlButton("btna8");
      var damageHandRails = getBooleanFromXmlButton("btna9");
      var damageUtilitiesSurroundingAreas = getBooleanFromXmlButton("btna10");
      var damageSeismicRestraintDevices = getBooleanFromXmlButton("btna11");

      var commLogSendingStation = getStringFromXml("sendingstation");
      var commLogReceivingStation = getStringFromXml("receivingstation");
      var commLogFrequencyMHz = getStringFromXml("freq");
      var commLogReceivedLocal = getStringFromXml("freq");

      var templateversion = getStringFromXml("templateversion");
      var version = templateversion == null ? "unknown" : templateversion.split(" ")[4];

      var m = new WA_WSDOT_BridgeDamageMessage(message, //
          isExercise, formDate, formTime, status, //
          region, county, route, milepost, bridgeNumber, //
          location, inspectorName, //
          remarks,

          damageApproaches, //
          damageWingWalls, //
          damageAbutments, //
          damageBentCapsAndColumns, //
          damageBearings, //
          damageBeamsGirders, //
          damageDeck, //
          damageHingeExpansionJoints, //
          damageHandRails, //
          damageUtilitiesSurroundingAreas, //
          damageSeismicRestraintDevices, //

          commLogSendingStation, commLogReceivingStation, commLogFrequencyMHz, commLogReceivedLocal,

          version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_WSDOT_BRIDGE_DAMAGE;
  }

  private boolean getBooleanFromXmlButton(String key) {
    if (key == null) {
      return false;
    }

    var string = getStringFromXml(key);
    if (string == null) {
      return false;
    }
    return string.equals("checkbox");
  }
}