/**

  FIELD_SITUATION_REPORT(14, "field_situation", true, "RMS_Express_Form_Field Situation Report_viewer.xml"), //
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.UnifiedFieldSituationMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

/**
 * necessary because the WDT made FieldSituation, FieldSituation23, FieldSituation25, etc.
 *
 * @author bobt
 *
 */
public class UnifiedFieldSituationProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(UnifiedFieldSituationProcessor.class);

  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  private static final MessageType[] SUPPORTED_TYPES = { //
      MessageType.FIELD_SITUATION_REPORT, //
      MessageType.FIELD_SITUATION_REPORT_23, //
      MessageType.FIELD_SITUATION_REPORT_25, //
      MessageType.FIELD_SITUATION_REPORT_26, //
  };
  private static final Set<MessageType> SUPPORTED_TYPES_SET;
  private static final Map<MessageType, Integer> typeCountMap;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();

    SUPPORTED_TYPES_SET = new HashSet<>(Arrays.asList(SUPPORTED_TYPES));
    typeCountMap = new HashMap<>();
  }

  public UnifiedFieldSituationProcessor() {
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {
    var messageId = message.messageId;
    var from = message.from;

    if (dumpIds.contains(messageId) || dumpIds.contains(from)) {
      logger.info("dump: " + message);
    }

    MessageType baseMessageType = getMessageType(message);
    if (!SUPPORTED_TYPES_SET.contains(baseMessageType)) {
      return reject(message, RejectType.UNSUPPORTED_TYPE, baseMessageType.toString());
    }

    // increment count by type
    var count = typeCountMap.getOrDefault(baseMessageType, Integer.valueOf(0));
    typeCountMap.put(baseMessageType, ++count);

    try {
      String xmlString = new String(message.attachments.get(baseMessageType.attachmentName()));

      makeDocument(message.messageId, xmlString);

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

      UnifiedFieldSituationMessage m = new UnifiedFieldSituationMessage(//
          message, latLong.getLatitude(), latLong.getLongitude(), //
          precedence, task, isHelpNeeded, neededHelp, //
          organization, city, county, state, territory, //
          landlineStatus, landlineComments, cellPhoneStatus, cellPhoneComments, radioStatus, radioComments, tvStatus,
          tvComments, waterStatus, waterComments, powerStatus, powerComments, internetStatus, internetComments,
          noaaStatus, noaaComments, //
          additionalComments, poc, formVersion);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR,
          "mId: " + messageId + ", from: " + from + ", " + e.getMessage());
    }
  }

  /**
   * return the underlying type, NOT UnifiedFSR
   *
   * @param message
   * @return
   */
  private MessageType getMessageType(ExportedMessage message) {
    var attachments = message.attachments;
    if (attachments != null && attachments.size() > 0) {
      var attachmentNames = attachments.keySet();

      if (attachmentNames.contains(MessageType.FIELD_SITUATION_REPORT.attachmentName())) {
        return MessageType.FIELD_SITUATION_REPORT;
      } else if (attachmentNames.contains(MessageType.FIELD_SITUATION_REPORT_23.attachmentName())) {
        return MessageType.FIELD_SITUATION_REPORT_23;
      } else if (attachmentNames.contains(MessageType.FIELD_SITUATION_REPORT_25.attachmentName())) {
        return MessageType.FIELD_SITUATION_REPORT_25;
      } else if (attachmentNames.contains(MessageType.FIELD_SITUATION_REPORT_26.attachmentName())) {
        return MessageType.FIELD_SITUATION_REPORT_26;
      } else {

        return MessageType.UNKNOWN;
      }
    }
    return MessageType.UNKNOWN;
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
    if (messages == null || messages.size() == 0) {
      return "";
    }

    var sb = new StringBuilder();
    sb.append("\nField Situation Report types:\n");
    for (MessageType type : SUPPORTED_TYPES) {
      sb.append("  type: " + type.name() + ", count: " + typeCountMap.getOrDefault(type, Integer.valueOf(0)) + "\n");
    }
    sb.append("\n");
    return sb.toString();
  }
}
