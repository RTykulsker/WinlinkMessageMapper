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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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

  private static final Map<UnderlyingMessageType, Integer> typeCountMap;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();

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

    UnderlyingMessageType underlyingMessageType = getUnderlyingMessageType(message);

    // increment count by type
    var count = typeCountMap.getOrDefault(underlyingMessageType, Integer.valueOf(0));
    typeCountMap.put(underlyingMessageType, ++count);

    if (underlyingMessageType == UnderlyingMessageType.UNSUPPORTED) {
      ;
      return reject(message, RejectType.UNSUPPORTED_TYPE, underlyingMessageType.toString());
    }

    try {
      String xmlString = new String(message.attachments.get(underlyingMessageType.toString()));

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
  private UnderlyingMessageType getUnderlyingMessageType(ExportedMessage message) {
    var attachments = message.attachments;
    if (attachments != null && attachments.size() > 0) {
      var attachmentNames = attachments.keySet();

      for (var name : attachmentNames) {
        if (name.startsWith(MessageType.UNIFIED_FIELD_SITUATION.attachmentName())) {
          var umt = UnderlyingMessageType.fromString(name);
          return umt;
        }
      }
    }
    return null;
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
    for (UnderlyingMessageType type : UnderlyingMessageType.values()) {
      sb.append("  type: " + type.name() + ", count: " + typeCountMap.getOrDefault(type, Integer.valueOf(0)) + "\n");
    }
    sb.append("\n");
    return sb.toString();
  }

  private static enum UnderlyingMessageType {

    FIELD_SITUATION_REPORT("RMS_Express_Form_Field Situation Report_viewer.xml"), //
    FIELD_SITUATION_REPORT_23("RMS_Express_Form_Field Situation Report 23_viewer.xml"), //
    FIELD_SITUATION_REPORT_25("RMS_Express_Form_Field Situation Report 25_viewer.xml"), //
    FIELD_SITUATION_REPORT_26("RMS_Express_Form_Field Situation Report 26_viewer.xml"), //
    UNSUPPORTED("unsupported");
    ;

    private final String attachmentName;

    private UnderlyingMessageType(String attachmentName) {
      this.attachmentName = attachmentName;
    }

    public static UnderlyingMessageType fromString(String string) {
      for (UnderlyingMessageType umt : UnderlyingMessageType.values()) {
        if (umt.toString().equals(string)) {
          return umt;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return attachmentName;
    }

  }
}
