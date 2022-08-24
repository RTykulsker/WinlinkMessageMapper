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

package com.surftools.winlinkMessageMapper.grade.named;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.GeoBox;
import com.surftools.utils.location.LatLongPair;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

/**
 * The Required Portion (Part 1/Thursday) will be graded as follows:
 *
 * 100 points for Agency/Group name of: ETO WLT/P2P Exercise
 *
 * 50 points for including ETO or ETO-AAR either in the To or Cc list
 *
 * 25 point deduction for any non-Hawaiian Field station who includes any of the Hawaiian Target stations (channels
 * 31-35) in the Send To, To, or Cc list
 *
 * @author bobt
 *
 */
public class ETO_2022_08_18 implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_08_18.class);

  private Map<String, Target> targetMap;
  private Set<String> targetSet;
  private Set<String> targetsInBoxSet;

  // for post processing
  private int ppCount;
  private int ppOrgOk;
  private int ppRequiredAddressOk;
  private int ppNonHawaiianPenalty;
  private int ppIsHawaiianCount;

  private int ppTargetCount;
  private int ppWillBeField;
  private Map<String, Integer> ppTargetCountMap;
  private Map<String, Integer> ppBandCountMap;
  private Map<String, Integer> ppRegionCountMap;

  private Set<String> dumpIds;
  private IConfigurationManager cm;

  private final GeoBox geoBox;

  public ETO_2022_08_18() {
    dumpIds = new HashSet<>();

    LatLongPair nw = new LatLongPair(22.5, -161);
    LatLongPair ne = new LatLongPair(22.5, -154);
    LatLongPair sw = new LatLongPair(17.5, -161);
    LatLongPair se = new LatLongPair(17.5, -155);

    geoBox = new GeoBox(nw, ne, sw, se);

    // add targets that are in our GeoBox
    targetsInBoxSet = new HashSet<>();
    targetsInBoxSet.add("AH6T");
  }

  private void readTargets() {
    targetMap = new HashMap<>();
    targetSet = new HashSet<>();

    ppTargetCountMap = new HashMap<>();
    ppBandCountMap = new HashMap<>();
    ppRegionCountMap = new HashMap<>();

    String targetFilePathName = cm.getAsString(Key.TARGET_PATH);

    try {
      Reader reader = new FileReader(targetFilePathName);
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      String[] fields;
      while ((fields = csvReader.readNext()) != null) {
        var target = Target.makeTarget(fields);
        if (target == null) {
          continue;
        }
        logger.debug(target.toString());
        var call = target.call;
        targetSet.add(call);
        targetMap.put(call, target);
      }
    } catch (Exception e) {
      logger.error("exception processing targets: " + targetFilePathName + ", " + e.getLocalizedMessage());
    }

    logger.info("read " + targetMap.size() + " target entries from " + targetFilePathName);
  }

  @Override
  public GradeResult grade(GradableMessage gm) {
    // must defer initialization, since we require a no-args constructor
    if (ppTargetCountMap == null) {
      readTargets();
    }

    if (!(gm instanceof CheckInMessage)) {
      return null;
    }

    var m = (CheckInMessage) gm;
    ++ppCount;

    if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
      logger.info("dumpIds: " + m.from);
    }

    /**
     *
     * SCORING
     *
     * 50 points for getting “Setup” information correct
     *
     * 50 points having "ETO" in either To or Cc list
     *
     * 25 point penalty for non-Hawaiian sending to one or more Hawaiian Target
     *
     */
    var points = 0;
    var explanations = new ArrayList<String>();

    var org = m.organization;
    final var requiredOrg = "ETO WLT/P2P Exercise";
    if (org != null && org.equalsIgnoreCase(requiredOrg)) {
      points += 50;
      ++ppOrgOk;
    } else {
      explanations.add("agency/group name not " + requiredOrg);
    }

    final var requiredAddress = "ETO-AAR";
    final var altRequiredAddress = "ETO";
    var addresses = makeAddressSet(m);
    if (addresses.contains(requiredAddress) || addresses.contains(altRequiredAddress)) {
      points += 50;
      ++ppRequiredAddressOk;
    } else {
      explanations.add("required address '" + requiredAddress + "' not present");
    }

    for (var address : addresses) {
      if (targetMap.keySet().contains(address)) {
        ++ppWillBeField;
        break;
      }
    }

    boolean isMessageInBox = geoBox.isInBox(new LatLongPair(m.latitude, m.longitude));
    if (isMessageInBox) {
      ++ppIsHawaiianCount;
    }

    if (!isMessageInBox) {
      var resultSet = new HashSet<>(addresses);
      resultSet.retainAll(targetsInBoxSet);
      if (resultSet.size() > 0) {
        explanations.add("non-HI station attempting contact with HI-only targets");
        points -= 25;
        ++ppNonHawaiianPenalty;
      }
    }

    var targetAddresses = new HashSet<>(addresses);
    targetAddresses.retainAll(targetSet);
    ppTargetCount += targetAddresses.size();

    for (String call : targetAddresses) {
      var target = targetMap.get(call);

      var count = ppTargetCountMap.getOrDefault(call, Integer.valueOf(0));
      ppTargetCountMap.put(call, ++count);

      var band = target.band;
      count = ppBandCountMap.getOrDefault(band, Integer.valueOf(0));
      ppBandCountMap.put(band, ++count);

      var region = target.region;
      count = ppRegionCountMap.getOrDefault(region, Integer.valueOf(0));
      ppRegionCountMap.put(region, ++count);
    }

    points = Math.min(100, points);
    points = Math.max(0, points);
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

    gm.setIsGraded(true);
    gm.setGrade(grade);
    gm.setExplanation(explanation);

    return new GradeResult(grade, explanation);
  }

  private Set<String> makeAddressSet(CheckInMessage m) {
    var addresses = new HashSet<String>();
    for (var listAsString : new String[] { m.toList, m.ccList }) {
      var fields = listAsString.split(",");
      for (var field : fields) {
        var index = field.indexOf("@winlink.org");
        if (index >= 0) {
          field = field.substring(0, index);
          addresses.add(field.toUpperCase());
        }
      }
    }
    return addresses;
  }

  private String formatPercent(Double d) {
    if (d == null) {
      return "";
    }

    return String.format("%.2f", 100d * d) + "%";
  }

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    if (messages == null || messages.size() == 0 || messages.get(0).getMessageType() != MessageType.CHECK_IN) {
      return null;
    }

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);
    sb.append("\nETO-2022-08-18 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    sb.append(formatPP("agency/group name", ppOrgOk));
    sb.append(formatPP("required Address found", ppRequiredAddressOk));
    sb.append(formatPP("non-Hawaiian penalty", ppNonHawaiianPenalty));
    sb.append(formatPP("in Hawaii", ppIsHawaiianCount, ppCount));
    sb.append("\n");

    sb.append("RMS stations that will be Field Stations: " + ppWillBeField + "\n");
    sb.append("Total targets: " + ppTargetCount + "\n");
    sb.append("\nCounts by Target Call \n");
    for (String call : ppTargetCountMap.keySet()) {
      sb.append(formatPP("call: " + call, ppTargetCountMap.get(call), ppTargetCount));
    }

    sb.append("\nCounts by Band\n");
    for (String band : ppBandCountMap.keySet()) {
      sb.append(formatPP("band: " + band, ppBandCountMap.get(band), ppTargetCount));
    }

    sb.append("\nCounts by Region \n");
    for (String region : ppRegionCountMap.keySet()) {
      sb.append(formatPP("region: " + region, ppRegionCountMap.get(region), ppTargetCount));
    }

    return sb.toString();
  }

  private String formatPP(String label, int okCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = (double) okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
  }

  private String formatPP(String label, int count, int total) {
    var percent = count / (double) total;
    return "  " + label + ": " //
        + count + "(" + formatPercent(percent) + ")" + "\n";
  }

  @Override
  public void setDumpIds(Set<String> dumpIds) {
    this.dumpIds = dumpIds;
  }

  @Override
  public void setConfigurationManager(IConfigurationManager cm) {
    this.cm = cm;
  }

  /**
   * minimal version of what we need for processing
   *
   * @author bobt
   *
   */
  static record Target(String call, String region, String band, double latitude, double longitude) {

    public static Target makeTarget(String[] fields) {
      if (fields[8].isEmpty() || fields[9].isEmpty()) {
        return null;
      }

      var _call = fields[6];
      var _region = fields[5];
      var _band = fields[1];
      var _latitude = Double.valueOf(fields[8]);
      var _longitude = Double.valueOf(fields[9]);
      var target = new Target(_call, _region, _band, _latitude, _longitude);
      return target;
    }

  };
}