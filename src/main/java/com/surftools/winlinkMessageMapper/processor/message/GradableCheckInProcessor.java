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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GradedCheckInMessage;
import com.surftools.winlinkMessageMapper.dto.other.Grade;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class GradableCheckInProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(GradableCheckInProcessor.class);

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  private final Map<String, Grade> responseGradeMap;
  private final Map<Grade, Integer> gradeCountMap;
  private int totalGraded;

  public GradableCheckInProcessor(String gradableResponses) {
    responseGradeMap = new HashMap<>();
    gradeCountMap = new HashMap<>();
    totalGraded = 0;

    if (gradableResponses != null) {
      String[] fields = gradableResponses.split(",");
      for (String field : fields) {
        String[] subfields = field.split("\\|");
        if (subfields.length != 2) {
          throw new IllegalArgumentException("can't parse gradable: " + field);
        }
        String response = subfields[0];
        String gradeString = subfields[1];
        Grade grade = Grade.fromString(gradeString);
        if (grade == null) {
          throw new IllegalArgumentException("can't parse grade: " + gradeString);
        }
        responseGradeMap.put(response.trim().toLowerCase(), grade);
      } // for over fields
    } // end if not null gradableResponses
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {
    try {
      if (saveAttachments) {
        saveAttachments(message);
      }

      String xmlString = new String(message.attachments.get(MessageType.CHECK_IN.attachmentName()));

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

      var latLong = getLatLongFromXml(xmlString, null);
      if (latLong == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      var organization = getStringFromXml(xmlString, "organization");
      var band = getStringFromXml(xmlString, "band");
      var status = getStringFromXml(xmlString, "status");
      var mode = getStringFromXml(xmlString, "session");
      var comments = getStringFromXml(xmlString, "comments");
      if (comments == null) {
        comments = getStringFromXml(xmlString, "Comments");
      }

      var version = "";
      var templateVersion = getStringFromXml(xmlString, "templateversion");
      if (templateVersion == null) {
        templateVersion = getStringFromXml(xmlString, "Templateversion");
      }
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      var response = getResponse(comments);
      var grade = getGrade(response);

      ExportedMessage m = new GradedCheckInMessage(message, latLong.latitude(), latLong.longitude(), organization,
          comments, status, band, mode, version, response, grade);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private String dequote(String response) {
    if (response.startsWith("\"") && response.endsWith("\"")) {
      response = response.substring(1, response.length() - 1);
    }
    return response;
  }

  private String getResponse(String comments) {
    String[] fields = comments.trim().split("\n");
    if (fields == null || fields.length == 0) {
      return null;
    }

    return dequote(fields[0].trim().toLowerCase());
  }

  private Grade getGrade(String response) {
    Grade grade = responseGradeMap.getOrDefault(response, Grade.UNGRADABLE);

    int count = gradeCountMap.getOrDefault(grade, Integer.valueOf(0));
    gradeCountMap.put(grade, count + 1);
    ++totalGraded;

    return grade;
  }

  @Override
  public String getPostProcessReport() {
    final var INDENT = "   ";
    final var FORMAT = new DecimalFormat("0.00");
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("GradableCheckInProcessor: " + totalGraded + " messages" + "\n");
    for (Grade grade : Grade.values()) {
      int count = gradeCountMap.getOrDefault(grade, Integer.valueOf(0));
      double percent = 100d * count / totalGraded;
      var percentString = FORMAT.format(percent) + "%";
      sb.append(INDENT + grade.toString() + ": " + count + " (" + percentString + ")\n");
    }
    var s = sb.toString();
    return s;
  }

}
