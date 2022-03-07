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

package com.surftools.winlinkMessageMapper.processor;

import java.util.Arrays;
import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.LAXHospitalBedMessage;
import com.surftools.winlinkMessageMapper.reject.MessageOrRejectionResult;
import com.surftools.winlinkMessageMapper.reject.RejectType;

public class LAXHospitalBedProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(LAXHospitalBedProcessor.class);

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  @Override
  public MessageOrRejectionResult process(ExportedMessage message) {
    var mime = message.mime;

    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    try {
      String xmlString = decodeAttachment(mime, "LAX Bed", message.from);

      var latLong = getLatLongFromXml(xmlString, null);
      if (latLong == null) {
        return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      var facility = getStringFromXml(xmlString, "hospital");

      var contactPerson = getStringFromXml(xmlString, "name");
      var contactPhone = getStringFromXml(xmlString, "phone");
      var contactEmail = getStringFromXml(xmlString, "");
      var serviceLevel = getStringFromXml(xmlString, "servicelevel");

      var emergencyBedCount = getStringFromXml(xmlString, "b1");
      var emergencyBedNotes = getStringFromXml(xmlString, "note1");

      var pediatricsBedCount = getStringFromXml(xmlString, "b2");
      var pediatricsBedNotes = getStringFromXml(xmlString, "note2");

      var medicalBedCount = getStringFromXml(xmlString, "b3");
      var medicalBedNotes = getStringFromXml(xmlString, "note3");

      var psychiatryBedCount = getStringFromXml(xmlString, "b4");
      var psychiatryBedNotes = getStringFromXml(xmlString, "note4");

      var burnBedCount = getStringFromXml(xmlString, "b5");
      var burnBedNotes = getStringFromXml(xmlString, "note5");

      var criticalBedCount = getStringFromXml(xmlString, "b6");
      var criticalBedNotes = getStringFromXml(xmlString, "note6");

      var other1Name = getStringFromXml(xmlString, "othertype2");
      var other1BedCount = getStringFromXml(xmlString, "b7");
      var other1BedNotes = getStringFromXml(xmlString, "note7");

      var other2Name = getStringFromXml(xmlString, "othertype3");
      var other2BedCount = getStringFromXml(xmlString, "b8");
      var other2BedNotes = getStringFromXml(xmlString, "note8");

      var totalBedCount = getStringFromXml(xmlString, "totalbeds");
      var additionalComments = getStringFromXml(xmlString, "comments");

      LAXHospitalBedMessage m = null;
      // LAXHospitalBedMessage message = new LAXHospitalBedMessage(xmlMessage, latLong.latitude(), latLong.longitude(),
      // //
      // facility, serviceLevel, contactPerson, contactPhone, //
      // emergencyBedCount, emergencyBedNotes, //
      // pediatricsBedCount, pediatricsBedNotes, //
      // medicalBedCount, medicalBedNotes, //
      // psychiatryBedCount, psychiatryBedNotes, //
      // burnBedCount, burnBedNotes, //
      // criticalBedCount, criticalBedNotes, //
      // other1Name, other1BedCount, other1BedNotes, //
      // other2Name, other2BedCount, other2BedNotes, //
      // additionalComments);

      return new MessageOrRejectionResult(m, null);
    } catch (Exception e) {
      return new MessageOrRejectionResult(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
