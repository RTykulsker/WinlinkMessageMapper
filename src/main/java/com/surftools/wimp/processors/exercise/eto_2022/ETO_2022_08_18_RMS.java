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

package com.surftools.wimp.processors.exercise.eto_2022;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.GeoBox;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * The Required Portion (Part 1/Thursday) will be graded as follows:
 *
 * 50 points for Agency/Group name of: ETO WLT/P2P Exercise
 *
 * 50 points for including ETO or ETO-AAR either in the To or Cc list
 *
 * 25 point deduction for any non-Hawaiian Field station who includes any of the Hawaiian Target stations (channels
 * 31-35) in the Send To, To, or Cc list
 *
 * @author bobt
 *
 */
public class ETO_2022_08_18_RMS extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_08_18_RMS.class);

  private Map<String, Target> targetMap;
  private Set<String> targetSet;
  private Set<String> targetsInBoxSet;

  private GeoBox geoBox;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    LatLongPair nw = new LatLongPair(22.5, -161);
    LatLongPair ne = new LatLongPair(22.5, -154);
    LatLongPair sw = new LatLongPair(17.5, -161);
    LatLongPair se = new LatLongPair(17.5, -155);

    geoBox = new GeoBox(nw, ne, sw, se);

    // add targets that are in our GeoBox
    targetsInBoxSet = new HashSet<>();
    targetsInBoxSet.add("AH6T");

    readTargets();
  }

  private void readTargets() {
    targetMap = new HashMap<>();
    targetSet = new HashSet<>();

    String targetFilePathName = cm.getAsString(Key.P2P_TARGET_PATH);
    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(Path.of(pathName, targetFilePathName));
    for (var fields : fieldsArray) {
      var target = Target.makeTarget(fields);
      if (target == null) {
        continue;
      }
      logger.debug(target.toString());
      var call = target.call;
      targetSet.add(call);
      targetMap.put(call, target);
    }

    logger.info("read " + targetMap.size() + " target entries from " + targetFilePathName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    int ppCount = 0;
    int ppOrgOk = 0;
    int ppRequiredAddressOk = 0;
    int ppNonHawaiianPenalty = 0;
    int ppIsHawaiianCount = 0;

    int ppTargetCount = 0;
    int ppWillBeField = 0;

    var pointsCounter = new Counter();
    var targetCounter = new Counter();
    var bandCounter = new Counter();
    var regionCounter = new Counter();

    var results = new ArrayList<IWritableTable>();
    var it = mm.getSenderIterator();
    while (it.hasNext()) {
      var sender = it.next();
      var messageMap = mm.getMessagesForSender(sender);
      var messageList = messageMap.get(MessageType.CHECK_IN);
      if (messageList == null || messageList.size() == 0) {
        continue;
      }
      Collections.reverse(messageList);
      var m = (CheckInMessage) messageList.iterator().next();

      ++ppCount;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.info("dumpIds: " + m.from);
      }

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

      boolean isMessageInBox = geoBox.isInBox(m.mapLocation);
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

        targetCounter.increment(call);
        bandCounter.increment(target.band);
        regionCounter.increment(target.region);
      }

      points = Math.min(100, points);
      points = Math.max(0, points);
      pointsCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over senders

    var sb = new StringBuilder();
    sb.append("\nETO-2022-08-18 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    sb.append(formatPP("agency/group name", ppOrgOk, ppCount));
    sb.append(formatPP("required Address found", ppRequiredAddressOk, ppCount));
    sb.append(formatPP("non-Hawaiian penalty", ppNonHawaiianPenalty, ppCount));
    sb.append(formatPP("in Hawaii", ppIsHawaiianCount, ppCount));
    sb.append("\n");

    sb.append("RMS stations that will be Field Stations: " + ppWillBeField + "\n");
    sb.append("Total targets: " + ppTargetCount + "\n");

    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));
    sb.append("\nTargets: \n" + formatCounter(targetCounter.getDescendingKeyIterator(), "target", "count"));
    sb.append("\nBands: \n" + formatCounter(bandCounter.getDescendingKeyIterator(), "band", "count"));
    sb.append("\nRegions: \n" + formatCounter(regionCounter.getDescendingKeyIterator(), "region", "count"));

    logger.info(sb.toString());

    writeTable("results.csv", results);
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