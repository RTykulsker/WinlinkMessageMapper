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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.HospitalBedMessage;

public class HospitalBedParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(CheckInParser.class);
  private final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    try {
      String xmlString = new String(message.attachments.get(MessageType.HOSPITAL_BED.attachmentName()));
      makeDocument(message.messageId, xmlString);

      var formLocation = getLatLongFromXml(null);
      if (formLocation == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      LocalDateTime formDateTime = null;
      try {
        formDateTime = LocalDateTime.parse(getStringFromXml("datetime"), DT_FORMATTER);
      } catch (Exception e) {
        ;
      }

      var organization = getStringFromXml("title");
      var facility = getStringFromXml("facility");

      var contactPerson = getStringFromXml("contact");
      var contactPhone = getStringFromXml("phone");
      var contactEmail = getStringFromXml("email");

      var emergencyBedCount = getStringFromXml("b1");
      var emergencyBedNotes = getStringFromXml("note1");

      var pediatricsBedCount = getStringFromXml("b2");
      var pediatricsBedNotes = getStringFromXml("note2");

      var medicalBedCount = getStringFromXml("b3");
      var medicalBedNotes = getStringFromXml("note3");

      var psychiatryBedCount = getStringFromXml("b4");
      var psychiatryBedNotes = getStringFromXml("note4");

      var burnBedCount = getStringFromXml("b5");
      var burnBedNotes = getStringFromXml("note5");

      var criticalBedCount = getStringFromXml("b6");
      var criticalBedNotes = getStringFromXml("note6");

      var other1Name = getStringFromXml("othertype2");
      var other1BedCount = getStringFromXml("b7");
      var other1BedNotes = getStringFromXml("note7");

      var other2Name = getStringFromXml("othertype3");
      var other2BedCount = getStringFromXml("b8");
      var other2BedNotes = getStringFromXml("note8");

      var totalBedCount = getStringFromXml("totalbeds");
      var additionalComments = getStringFromXml("comments");

      HospitalBedMessage m = new HospitalBedMessage(message, formDateTime, formLocation, //
          organization, facility, contactPerson, contactPhone, contactEmail, //
          emergencyBedCount, emergencyBedNotes, //
          pediatricsBedCount, pediatricsBedNotes, //
          medicalBedCount, medicalBedNotes, //
          psychiatryBedCount, psychiatryBedNotes, //
          burnBedCount, burnBedNotes, //
          criticalBedCount, criticalBedNotes, //
          other1Name, other1BedCount, other1BedNotes, //
          other2Name, other2BedCount, other2BedNotes, //
          totalBedCount, additionalComments);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
