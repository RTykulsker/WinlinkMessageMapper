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

package com.surftools.winlinkMessageMapper.processor.message;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.FieldSituationMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class FieldSituationProcessor extends AbstractBaseProcessor {
  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  private final String gradeKey;
  private String grade;
  private String explanation;

  public FieldSituationProcessor(String gradeKey) {
    this.gradeKey = gradeKey;
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {

    try {
      String xmlString = new String(message.attachments.get(MessageType.FIELD_SITUATION_REPORT.attachmentName()));

      var latLong = getLatLongFromXml(xmlString, null);
      if (latLong == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String organization = getStringFromXml(xmlString, "title");
      String task = getStringFromXml(xmlString, "msgnr");
      String isHelpNeeded = getStringFromXml(xmlString, "safetyneed");
      String neededHelp = getStringFromXml(xmlString, "comm0");

      String city = getStringFromXml(xmlString, "city");
      String county = getStringFromXml(xmlString, "county");
      String state = getStringFromXml(xmlString, "state");
      String territory = getStringFromXml(xmlString, "territory");

      String landlineStatus = getStringFromXml(xmlString, "land");
      String landlineComments = getStringFromXml(xmlString, "comm1");

      String cellPhoneComments = getStringFromXml(xmlString, "cell");
      String cellPhoneStatus = getStringFromXml(xmlString, "comm2");

      String radioStatus = getStringFromXml(xmlString, "amfm");
      String radioComments = getStringFromXml(xmlString, "comm3");

      String tvStatus = getStringFromXml(xmlString, "tvstatus");
      String tvComments = getStringFromXml(xmlString, "comm4");

      String waterStatus = getStringFromXml(xmlString, "waterworks");
      String waterComments = getStringFromXml(xmlString, "comm5");

      String powerStatus = getStringFromXml(xmlString, "powerworks");
      String powerComments = getStringFromXml(xmlString, "comm6");

      String internetStatus = getStringFromXml(xmlString, "inter");
      String internetComments = getStringFromXml(xmlString, "comm7");

      String noaaStatus = getStringFromXml(xmlString, "noaa");
      String noaaComments = getStringFromXml(xmlString, "noaacom");

      String additionalComments = getStringFromXml(xmlString, "message");
      String poc = getStringFromXml(xmlString, "poc");
      String formVersion = parseFormVersion(getStringFromXml(xmlString, "templateversion"));

      FieldSituationMessage m = new FieldSituationMessage(message, latLong.getLatitude(), latLong.getLongitude(), //
          task, isHelpNeeded, neededHelp, //
          organization, city, county, state, territory, //
          landlineStatus, landlineComments, cellPhoneStatus, cellPhoneComments, radioStatus, radioComments, tvStatus,
          tvComments, waterStatus, waterComments, powerStatus, powerComments, internetStatus, internetComments,
          noaaStatus, noaaComments, additionalComments, poc, formVersion);

      if (gradeKey != null) {
        grade(m);
        m.setIsGraded(true);
        m.setGrade(grade);
        m.setExplanation(explanation);
      }

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private void grade(FieldSituationMessage m) {
    // TODO Auto-generated method stub

  }

  private String parseFormVersion(String string) {
    if (string == null) {
      return null;
    }

    String[] fields = string.split(" ");
    if (fields.length == 5) {
      return fields[4];
    } else {
      return string;
    }
  }
}
