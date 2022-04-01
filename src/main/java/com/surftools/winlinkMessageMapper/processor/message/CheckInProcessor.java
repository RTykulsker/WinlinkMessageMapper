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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.CheckOutMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.IGrader;
import com.surftools.winlinkMessageMapper.grade.MultipleChoiceGrader;

public class CheckInProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CheckInProcessor.class);

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  private final MessageType messageType;

  private final String gradeKey;
  private IGrader grader = null;

  public CheckInProcessor(boolean isCheckIn, String gradeKey) {
    messageType = (isCheckIn) ? MessageType.CHECK_IN : MessageType.CHECK_OUT;
    this.gradeKey = gradeKey;

    if (gradeKey != null) {
      if (gradeKey.equals("ETO-2022-03-24")) {
        grader = new MultipleChoiceGrader(messageType);
        MultipleChoiceGrader g = (MultipleChoiceGrader) grader;
        g.setValidResponseString("A, B, C, or D");
        g.setCorrectResponseSet(new HashSet<String>(Arrays.asList(new String[] { "B" })));
        g.setIncorrectResponseSet(new HashSet<String>(Arrays.asList(new String[] { "A", "C", "D" })));
      } else {
        grader = new DefaultGrader(gradeKey);
      }
    }
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {

    try {
      if (saveAttachments) {
        saveAttachments(message);
      }

      String xmlString = new String(message.attachments.get(messageType.attachmentName()));

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

      var version = "";
      var templateVersion = getStringFromXml(xmlString, "templateversion");
      if (templateVersion == null) {
        templateVersion = getStringFromXml(xmlString, "Templateversion");
      }
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      CheckInMessage m = null;
      if (messageType == MessageType.CHECK_IN) {
        m = new CheckInMessage(message, latLong, organization, comments, status, band, mode, version, messageType);
      } else if (messageType == MessageType.CHECK_OUT) {
        m = new CheckOutMessage(message, latLong, organization, comments, status, band, mode, version, messageType);
      } else {
        return reject(message, RejectType.UNSUPPORTED_TYPE, messageType.name());
      }

      if (grader != null) {
        grade(m);
      }

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private void grade(CheckInMessage m) {
    GradeResult result = null;
    if (gradeKey.equals("ETO-2022-03-24")) {
      result = grade_ETO_2022_03_24(m);
    } else {
      result = grader.grade(m);
    }

    if (result != null) {
      m.setIsGraded(true);
      m.setGrade(result.grade());
      m.setExplanation(result.explanation());
    }
  }

  /**
   * response is first line, if any from comments valid responses are single letter, optionally quoted in {A,B,C,D}
   * correct responses are {B}
   *
   * @param m
   */
  private GradeResult grade_ETO_2022_03_24(CheckInMessage m) {
    var response = "";
    if (m.comments != null) {
      String[] commentsLines = m.comments.trim().split("\n");
      if (commentsLines != null && commentsLines.length > 0) {
        response = commentsLines[0].trim().toUpperCase();
      }
    }

    var gradeResult = grader.grade(response);
    return gradeResult;
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
