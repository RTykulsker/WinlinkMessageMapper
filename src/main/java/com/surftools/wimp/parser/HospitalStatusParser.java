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
import com.surftools.wimp.message.HospitalStatusMessage;

public class HospitalStatusParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(CheckInParser.class);
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};

  @SuppressWarnings("unused")
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
      String xmlString = new String(message.attachments.get(MessageType.HOSPITAL_STATUS.attachmentName()));
      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("title");
      var isExercise = Boolean.valueOf(getStringFromXml("exbtn"));
      var reportType = getStringFromXml("rtype");
      var updateString = getStringFromXml("updatenum");

      var incidentName = getStringFromXml("incident1");
      LocalDateTime formDateTime = null;
      try {
        formDateTime = LocalDateTime.parse(getStringFromXml("datetime"), DT_FORMATTER);
      } catch (Exception e) {
        ;
      }

      var facilityName = getStringFromXml("facilityname3a");
      var facilityType = getStringFromXml("factype3b");
      var facilityOther = getStringFromXml("othertype");

      var formLocation = getLatLongFromXml(null);

      var contactName = getStringFromXml("contact4");
      var contactPhone = getStringFromXml("contactphone");
      var contactExtension = getStringFromXml("phonex");
      var contactCellPhone = getStringFromXml("cell4");
      var contactEmail = getStringFromXml("contactemail");

      var facilityStatus = getStringFromXml("facility5");
      var facilityComments = getStringFromXml("comments5");

      var isCommsImpacted = Boolean.valueOf(getStringFromXml("bcommipaired").equals("YES"));
      var commsEmail = getStringFromXml("commsemail");
      var commsLandline = getStringFromXml("commsemail");
      var commsFax = getStringFromXml("commsfax");
      var commsInternet = getStringFromXml("commsinternet");
      var commsCell = getStringFromXml("commscell");
      var commsSatPhone = getStringFromXml("commssat");
      var commsHamRadio = getStringFromXml("commsheart");
      var commsComments = getStringFromXml("comments6");

      var isUtilsImpacted = Boolean.valueOf(getStringFromXml("butilitiesimpared").equals("YES"));
      var utilsPower = getStringFromXml("utilitiesa");
      var utilsWater = getStringFromXml("utilitiesb");
      var utilsSanitation = getStringFromXml("utilitiesc");
      var utilsHVAC = getStringFromXml("utilitiesd");
      var utilsComments = getStringFromXml("comments7");

      var areEvacConcerns = Boolean.valueOf(getStringFromXml("bevacconcerns").equals("YES"));
      var evacuating = getStringFromXml("bevac8");
      var evacuatingStatus = getStringFromXml("a8a");
      var partialEvac = getStringFromXml("bpartial8");
      var partialEvacStatus = getStringFromXml("a8b");
      var totalEvac = getStringFromXml("btotal8");
      var totalEvacStatus = getStringFromXml("a8c");
      var shelterInPlace = getStringFromXml("bshelter8");
      var shelterInPlaceStatus = getStringFromXml("a8d");
      var evacComments = getStringFromXml("comments8");

      var areCasualties = Boolean.valueOf(getStringFromXml("btherearecausalties").equals("YES"));
      var casImmediate = getStringFromXml("immediate9");
      var casDelayed = getStringFromXml("delayed9");
      var casMinor = getStringFromXml("minor9");
      var casFatalities = getStringFromXml("fatalities9");
      var casComments = getStringFromXml("comments9");

      var planActivated = Boolean.valueOf(getStringFromXml("bplan10a").equals("YES"));
      var commandCenterActivated = Boolean.valueOf(getStringFromXml("bfccactive10").equals("YES"));
      var generatorInUse = Boolean.valueOf(getStringFromXml("bgenerator10").equals("YES"));
      var rrIn4Hours = Boolean.valueOf(getStringFromXml("bresiourcerequest10").equals("YES"));
      var additionalComments = getStringFromXml("comments10");

      // General Hospital Status 2.2
      var version = getStringFromXml("templateversion");
      if (version != null) {
        var fields = version.replaceAll("  ", " ").split(" ");
        if (fields.length > 1) {
          version = fields[fields.length - 1];
        }
      }

      HospitalStatusMessage m = new HospitalStatusMessage(message, //
          organization, isExercise, reportType, updateString, //

          incidentName, formDateTime, //

          facilityName, facilityType, facilityOther, //

          formLocation, //

          contactName, contactPhone, contactExtension, contactCellPhone, contactEmail, //

          facilityStatus, facilityComments, //

          isCommsImpacted, commsEmail, commsLandline, commsFax, commsInternet, commsCell, commsSatPhone, commsHamRadio,
          commsComments, //

          isUtilsImpacted, utilsPower, utilsWater, utilsSanitation, utilsHVAC, utilsComments, //
          areEvacConcerns, evacuating, evacuatingStatus, partialEvac, partialEvacStatus, totalEvac, totalEvacStatus,
          shelterInPlace, shelterInPlaceStatus, evacComments, //

          areCasualties, casImmediate, casDelayed, casMinor, casFatalities, casComments, //

          planActivated, commandCenterActivated, generatorInUse, rrIn4Hours, additionalComments, //

          version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.HOSPITAL_STATUS;
  }
}
