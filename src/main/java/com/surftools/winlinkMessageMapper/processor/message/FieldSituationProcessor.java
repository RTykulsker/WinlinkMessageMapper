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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.FieldSituationMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.FieldSituationReportGrader;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.IGrader;

public class FieldSituationProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(FieldSituationProcessor.class);

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  private IGrader grader = null;

  public FieldSituationProcessor(String gradeKey) {
    if (gradeKey != null) {
      if (gradeKey.startsWith("check_in:mc")) {
        grader = null;
      } else if (gradeKey.equals("fsr:ETO-2022-04-14")) {
        grader = new FieldSituationReportGrader(gradeKey);
      } else if (gradeKey.equals("fsr:ETO-2022-05-14")) {
        grader = new FieldSituationReportGrader(gradeKey);
      } else {
        grader = new DefaultGrader(gradeKey);
      }
    }
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {

    try {
      String xmlString = new String(message.attachments.get(MessageType.FIELD_SITUATION_REPORT.attachmentName()));

      makeDocument(message.messageId, xmlString);

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

      var latLong = getLatLongFromXml(null);
      if (latLong == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String organization = getStringFromXml("title");
      String precedence = getStringFromXml("precedence");
      String task = getStringFromXml("msgnr");
      String isHelpNeeded = getStringFromXml("safetyneed");
      String neededHelp = getStringFromXml("comm0");

      String city = getStringFromXml("city");
      String county = getStringFromXml("county");
      String state = getStringFromXml("state");
      String territory = getStringFromXml("territory");

      String landlineStatus = getStringFromXml("land");
      String landlineComments = getStringFromXml("comm1");

      String cellPhoneStatus = getStringFromXml("cell");
      String cellPhoneComments = getStringFromXml("comm2");

      String radioStatus = getStringFromXml("amfm");
      String radioComments = getStringFromXml("comm3");

      String tvStatus = getStringFromXml("tvstatus");
      String tvComments = getStringFromXml("comm4");

      String waterStatus = getStringFromXml("waterworks");
      String waterComments = getStringFromXml("comm5");

      String powerStatus = getStringFromXml("powerworks");
      String powerComments = getStringFromXml("comm6");

      String internetStatus = getStringFromXml("inter");
      String internetComments = getStringFromXml("comm7");

      String noaaStatus = getStringFromXml("noaa");
      String noaaComments = getStringFromXml("noaacom");

      String additionalComments = getStringFromXml("message");
      String poc = getStringFromXml("poc");
      String formVersion = parseFormVersion(getStringFromXml("templateversion"));

      FieldSituationMessage m = new FieldSituationMessage(message, latLong.getLatitude(), latLong.getLongitude(), //
          precedence, task, isHelpNeeded, neededHelp, //
          organization, city, county, state, territory, //
          landlineStatus, landlineComments, cellPhoneStatus, cellPhoneComments, radioStatus, radioComments, tvStatus,
          tvComments, waterStatus, waterComments, powerStatus, powerComments, internetStatus, internetComments,
          noaaStatus, noaaComments, additionalComments, poc, formVersion);

      if (grader != null) {
        grade(m);
      }

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private void grade(FieldSituationMessage m) {
    GradeResult result = grader.grade(m);

    if (result != null) {
      m.setIsGraded(true);
      m.setGrade(result.grade());
      m.setExplanation(result.explanation());
    }
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

  @Override
  public String getPostProcessReport(List<ExportedMessage> messages) {
    if (grader != null) {
      return grader.getPostProcessReport(messages);
    } else {
      return "";
    }
  }
}
