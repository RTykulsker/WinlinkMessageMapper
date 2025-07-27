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

package com.surftools.wimp.parser;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;

/**
 * necessary because the WDT made FieldSituation, FieldSituation23, FieldSituation25, etc.
 *
 * @author bobt
 *
 */
public class FieldSituationParser extends AbstractBaseParser {
  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  // private static final Map<UnderlyingMessageType, Integer> typeCountMap;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  public FieldSituationParser() {
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    var messageId = message.messageId;
    var from = message.from;

    UnderlyingMessageType underlyingMessageType = getUnderlyingMessageType(message);
    if (underlyingMessageType == UnderlyingMessageType.UNSUPPORTED) {
      return reject(message, RejectType.UNSUPPORTED_TYPE, underlyingMessageType.toString());
    }

    try {
      String xmlString = new String(message.attachments.get(underlyingMessageType.toString()));

      makeDocument(message.messageId, xmlString);

      var formLocation = getLatLongFromXml(null);
      if (formLocation == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String organization = getStringFromXml("title");
      String precedence = getStringFromXml("precedence");
      String formDateTime = getStringFromXml("udtgfld");
      String task = getStringFromXml("msgnr");
      String formTo = getStringFromXml("msgto");
      String formFrom = getStringFromXml("msgsender");
      String isHelpNeeded = getStringFromXml("safetyneed");
      String neededHelp = getStringFromXml("comm0");

      String city = getStringFromXml("city");
      String county = getStringFromXml("county");
      String state = getStringFromXml("state");
      String territory = getStringFromXml("territory");

      String landlineStatus = getStringFromXml("pots");
      String landlineComments = getStringFromXml("comm1");

      String voipStatus = getStringFromXml("voip");
      String voipComments = getStringFromXml("comm1a");

      String cellPhoneStatus = getStringFromXml("cell");
      String cellPhoneComments = getStringFromXml("comm2");

      String cellTextStatus = getStringFromXml("celltext");
      String cellTextComments = getStringFromXml("comm2a");

      String radioStatus = getStringFromXml("amfm");
      String radioComments = getStringFromXml("comm3");

      String tvStatus = getStringFromXml("tvstatus");
      String tvComments = getStringFromXml("comm4");

      String satTvStatus = getStringFromXml("tvstatusb");
      String satTvComments = getStringFromXml("comm4b");

      String cableTvStatus = getStringFromXml("tvstatusc");
      String cableTvComments = getStringFromXml("comm4c");

      String waterStatus = getStringFromXml("waterworks");
      String waterComments = getStringFromXml("comm5");

      String powerStatus = getStringFromXml("powerworks");
      String powerComments = getStringFromXml("comm6");

      String powerStable = getStringFromXml("powerstable");
      String powerStableComments = getStringFromXml("comm6a");

      String naturalGasStatus = getStringFromXml("natgas");
      String naturalGasComments = getStringFromXml("comm9c");

      String internetStatus = getStringFromXml("inter");
      String internetComments = getStringFromXml("comm7");

      String noaaStatus = getStringFromXml("noaa");
      String noaaComments = getStringFromXml("noaacom");

      String noaaAudioDegraded = getStringFromXml("noaab");
      String noaaAudioDegradedComments = getStringFromXml("noaacomb");

      String additionalComments = getStringFromXml("message");
      String poc = getStringFromXml("poc");
      String formVersion = parseFormVersion(getStringFromXml("templateversion"));

      // TODO verify that I've identified base variables and variants

      FieldSituationMessage m = new FieldSituationMessage(//
          message, organization, formLocation, //
          precedence, formDateTime, task, formTo, formFrom, isHelpNeeded, neededHelp, //
          city, county, state, territory, //
          landlineStatus, landlineComments, voipStatus, voipComments, //
          cellPhoneStatus, cellPhoneComments, cellTextStatus, cellTextComments, //
          radioStatus, radioComments, //
          tvStatus, tvComments, satTvStatus, satTvComments, cableTvStatus, cableTvComments, //
          waterStatus, waterComments, //
          powerStatus, powerComments, powerStable, powerStableComments, //
          naturalGasStatus, naturalGasComments, //
          internetStatus, internetComments, //
          noaaStatus, noaaComments, noaaAudioDegraded, noaaAudioDegradedComments, //
          additionalComments, poc, formVersion);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR,
          "mId: " + messageId + ", from: " + from + ", " + e.getMessage());
    }
  }

  /**
   * return the underlying type, NOT FSR
   *
   * @param message
   * @return
   */
  private UnderlyingMessageType getUnderlyingMessageType(ExportedMessage message) {
    var attachments = message.attachments;
    if (attachments != null && attachments.size() > 0) {
      var attachmentNames = attachments.keySet();

      for (var name : attachmentNames) {
        if (name.startsWith(MessageType.FIELD_SITUATION.rmsViewerName())) {
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

  private static enum UnderlyingMessageType {

    FIELD_SITUATION_REPORT_20("RMS_Express_Form_Field Situation Report_viewer.xml"), //
    FIELD_SITUATION_REPORT_23("RMS_Express_Form_Field Situation Report 23_viewer.xml"), //
    FIELD_SITUATION_REPORT_25("RMS_Express_Form_Field Situation Report 25_viewer.xml"), //
    FIELD_SITUATION_REPORT_26("RMS_Express_Form_Field Situation Report 26_viewer.xml"), //
    FIELD_SITUATION_REPORT("RMS_Express_Form_Field Situation Report viewer.xml"), //
    UNSUPPORTED("unsupported");

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
