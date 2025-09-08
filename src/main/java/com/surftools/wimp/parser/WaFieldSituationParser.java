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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import com.surftools.utils.MultiDateTimeParser;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.WA_FieldSituationMessage;

/**
 * necessary because the WDT made FieldSituation, FieldSituation23, FieldSituation25, etc.
 *
 * @author bobt
 *
 */
public class WaFieldSituationParser extends AbstractBaseParser {
  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  // private static final Map<UnderlyingMessageType, Integer> typeCountMap;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  public WaFieldSituationParser() {
  }

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    var messageId = message.messageId;
    var from = message.from;

    try {
      String xmlString = new String(message.attachments.get(MessageType.WA_FIELD_SITUATION.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var formLocation = getLatLongFromXml(null);
      if (formLocation == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String formTo = gx("msgcc");
      String formCc = gx("msgcc");

      String reportingParty = gx("reportingparty");
      String reportingLocation = gx("location");
      String provideUpdate = gx("providepdate");

      String winlinkAddress = gx("winlinkaddress");
      String servedAgency = gx("servedagency");

      String county = gx("county");
      String town = gx("town");
      String formLocationSource = gx("locationsource");

      String frequenciesMonitored = gx("freqmonitored");
      String radioService = gx("service");
      String precedence = gx("precedence");

      String formDateTimeString = gx("thedate");
      LocalDateTime formDateTime = parseDateTime(formDateTimeString);

      boolean transportHighway = gb("btf1");
      boolean transportMassTransit = gb("btf2");
      boolean transportRailroad = gb("btf3");
      boolean transportAirport = gb("btf4");
      boolean transportSeaport = gb("btf5");
      boolean transportArterial = gb("btf6");
      boolean transportPipeline = gb("btf7");
      String transportComments = gx("transportation");

      boolean utilGas = gb("btf8");
      String utilGasCo = gx("gascompany");
      boolean utilWater = gb("btf9");
      String utilWaterCo = gx("watercompany");
      boolean utilSewer = gb("btf10");
      String utilSewerCo = gx("sewercompany");
      boolean utilElectric = gb("btf11");
      String utilElectricCo = gx("electricompany");
      String utilElectricStable = gx("stablepwr");
      String utilComments = gx("utilitynote");

      boolean envAir = gb("btf12");
      boolean envWater = gb("btf13");
      boolean envLandslide = gb("btf14");
      boolean envAvalanche = gb("btf15");
      boolean envHazmat = gb("btf16");
      boolean envFlood = gb("btf17");
      String envComments = gx("enviromentnaotes");

      boolean healthNeeds = gb("healthobserved"); // true if needed
      String healthComments = gx("healthnotes");

      boolean commLandline = gb("btf18"); // true if non-functional
      String commLandlineProvider = gx("provider1");
      boolean commVoip = gb("btf19");
      String commVoipProvider = gx("provider1");
      boolean commCellVoice = gb("btf29");
      String commCellVoiceProvider = gx("provider1");
      boolean commCellText = gb("btf21");
      String commCellTextProvider = gx("provider1");
      boolean commCellData = gb("btf22");
      String commCellDataProvider = gx("provider1");

      String commCableInternet = gx("func1"); // functionality: YES, YES but degraded, NO, Not observed
      String commCableInternetNotes = gx("funstr1");
      String commCableTelevison = gx("func2");
      String commCableTelevisionNotes = gx("funstr2");
      String commSatInternet = gx("func3");
      String commSatInternetNotes = gx("funstr3");
      String commSatTelevision = gx("func4");
      String commSatTelevisionNotes = gx("funstr4");
      String commOtaTelevision = gx("func5");
      String commOtaTelevisionNotes = gx("funstr5");
      String commAmFmRadio = gx("func6");
      String commAmFmRadioNotes = gx("funstr6");
      String commNoaaRadio = gx("func7");
      String commNoaaRadioNotes = gx("funstr7");

      String approvedBy = gx("approved_name");
      String approvedByTitle = gx("approved_postitle");
      String approvedDateTimeString = gx("approvetime");
      LocalDateTime approvedDateTime = parseDateTime(approvedDateTimeString);

      String version = parseFormVersion(getStringFromXml("templateversion"));

      WA_FieldSituationMessage m = new WA_FieldSituationMessage(message, //
          formTo, formCc, //
          reportingParty, reportingLocation, provideUpdate, //
          winlinkAddress, servedAgency, //
          county, town, formLocation, formLocationSource, //
          frequenciesMonitored, radioService, precedence, //
          formDateTime, formDateTimeString, //

          transportHighway, transportMassTransit, transportRailroad, transportAirport, transportSeaport,
          transportArterial, transportPipeline, transportComments, //

          utilGas, utilGasCo, //
          utilWater, utilWaterCo, //
          utilSewer, utilSewerCo, //
          utilElectric, utilElectricCo, utilElectricStable, utilComments, //

          envAir, envWater, envLandslide, envAvalanche, envHazmat, envFlood, envComments, //

          healthNeeds, healthComments, //

          commLandline, commLandlineProvider, //
          commVoip, commVoipProvider, //
          commCellVoice, commCellVoiceProvider, //
          commCellText, commCellTextProvider, //
          commCellData, commCellDataProvider, //

          commCableInternet, commCableInternetNotes, //
          commCableTelevison, commCableTelevisionNotes, //
          commSatInternet, commSatInternetNotes, //
          commSatTelevision, commSatTelevisionNotes, //
          commOtaTelevision, commOtaTelevisionNotes, //
          commAmFmRadio, commAmFmRadioNotes, //
          commNoaaRadio, commNoaaRadioNotes, //

          approvedBy, approvedByTitle, approvedDateTime, approvedDateTimeString, //
          version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR,
          "mId: " + messageId + ", from: " + from + ", " + e.getMessage());
    }
  }

  private LocalDateTime parseDateTime(String s) {
    if (s.endsWith(":")) {
      s = s.substring(0, s.length() - 1);
    }
    var parser = new MultiDateTimeParser(
        List.of("yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss", "yyyy-M-d HH:mm", "yyyy-M-d HHmm", "yyyy-M-d HH:mm:ss"));
    try {
      var dateTime = parser.parse(s);
      return dateTime;
    } catch (Exception e) {
      return null;
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

  private String gx(String tagName) {
    return getStringFromXml(tagName);
  }

  private boolean gb(String tagName) {
    var s = gx(tagName);
    return s.equals("CHECKED");
  }

}
