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

package com.surftools.winlinkMessageMapper.aggregation.named;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.CheckOutMessage;
import com.surftools.winlinkMessageMapper.dto.message.DyfiMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.UnifiedFieldSituationMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * https://docs.google.com/document/d/1-3P_Zjv5xn6maSvtfhSVEshFg9etMxnGo2cRVkvD9OI/edit
 *
 * Send a DYFI, Check In, FSR and Check Out message
 *
 * Each message has time and addressee constraints
 *
 * Optional Grading: 15 points for message, 5 points for time, 5 points for addressees
 *
 * @author bobt
 *
 */
public class LAX_2022_06_18_Shifting_SoCal extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(LAX_2022_06_18_Shifting_SoCal.class);

  public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  private static final String KEY_DYFI_COUNT = "dyfiCount";
  private static final String KEY_CHECK_IN_COUNT = "checkInCount";
  private static final String KEY_FSR_COUNT = "fsrCount";
  private static final String KEY_CHECK_OUT_COUNT = "checkOutCount";
  private static final String KEY_ALL_COUNT = "allCount";

  private static final String KEY_DYFI_TIME_OK = "dyfiTimeOk";
  private static final String KEY_CHECK_IN_TIME_OK = "checkInTimeOk";
  private static final String KEY_FSR_TIME_OK = "fsrTimeOk";
  private static final String KEY_CHECK_OUT_TIME_OK = "checkOutTimeOk";
  private static final String KEY_ALL_TIMES = "allTimes";

  private static final String KEY_DYFI_ADDRESSES_OK = "dyfiAddressesOk";
  private static final String KEY_CHECK_IN_ADDRESSES_OK = "checkInAddressesOk";
  private static final String KEY_FSR_ADDRESSES_OK = "fsrAddressesOk";
  private static final String KEY_CHECK_OUT_ADDRESSES_OK = "checkOutAddressesOk";
  private static final String KEY_ALL_ADDRESSES = "allAddresses";

  private static final String KEY_DYFI_WATERMELON = "dyfiWatermelon";
  private static final String KEY_CHECKIN_WATERMELON = "checkInWatermelon";
  private static final String KEY_FSR_WATERMELON = "fsrWatermelon";
  private static final String KEY_WATERMELON_COUNT = "watermelonCount";

  private static final String KEY_GRADE = "grade";
  private static final String KEY_EXPLANATION = "explanation";

  private static final String[] DYFI_REQUIRED_ADDRESSES = new String[] { //
      "SMTP:dyfi_reports_automated@usgs.gov", //
      "SMTP:dyfi@vccomm.org", //
      "LAXNORTHEAST@winlink.org", //
      "SDG-DRILL@winlink.org", //
      "DCS-S12@winlink.org", //
  };
  private static final Set<String> DYFI_REQUIRED_ADDRESS_SET = new HashSet<String>(
      Arrays.asList(DYFI_REQUIRED_ADDRESSES));

  private static final String[] OTHER_REQUIRED_ADDRESSES = new String[] { //
      "LAXNORTHEAST@winlink.org", //
      "SDG-DRILL@winlink.org", //
      "DCS-S12@winlink.org", //
  };
  private static final Set<String> OTHER_REQUIRED_ADDRESS_SET = new HashSet<String>(
      Arrays.asList(OTHER_REQUIRED_ADDRESSES));

  public LAX_2022_06_18_Shifting_SoCal() {
    super(logger);
  }

  @Override
  /**
   * include all DYFI, Check In, FSR and FSR_23 and Check Out
   *
   */
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    super.aggregate(messageMap);

    var newList = new ArrayList<AggregateMessage>();

    var ppCount = 0;

    var ppDyfiCount = 0;
    var ppCheckInCount = 0;
    var ppFsrCount = 0;
    var ppCheckOutCount = 0;
    var ppDyfiTimeOk = 0;
    var ppCheckInTimeOk = 0;
    var ppFsrTimeOk = 0;
    var ppCheckOutTimeOk = 0;
    var ppDyfiAddressesOk = 0;
    var ppCheckInAddressesOk = 0;
    var ppFsrAddressesOk = 0;
    var ppCheckOutAddressesOk = 0;

    var ppDyfiWatermelonCount = 0;
    var ppCheckInWatermelonCount = 0;
    var ppFsrWatermelonCount = 0;

    var ppScoreCountMap = new HashMap<Integer, Integer>();

    for (var m : aggregateMessages) {

      var from = m.from();
      var map = fromMessageMap.get(from);

      var lat = (String) m.data().get(KEY_AVERAGE_LATITUDE);
      if (lat == null || lat.length() == 0) {
        logger.debug("skipping messages from: " + from + " because no lat/lon");
        continue;
      }

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();

      var allCount = 0;
      var allTimes = 0;
      var allAddresses = 0;
      var watermelonCount = 0;

      final var exerciseStartDT = LocalDateTime.of(2022, 06, 18, 10, 18, 00);
      final var utcStartDT = LocalDateTime.of(2022, 06, 18, 17, 18, 00);
      final var LOCAL_ZONE_ID = ZoneId.of("America/Los_Angeles");
      final var zonedStartDT = ZonedDateTime.of(exerciseStartDT, LOCAL_ZONE_ID);
      final var startMillis = zonedStartDT.toInstant().toEpochMilli();

      final var exerciseStopDT = LocalDateTime.of(2022, 06, 19, 18, 00, 00);
      final var utcStopDT = LocalDateTime.of(2022, 06, 20, 1, 0, 00);
      final var zonedStopDT = ZonedDateTime.of(exerciseStopDT, LOCAL_ZONE_ID);
      final var stopMillis = zonedStopDT.toInstant().toEpochMilli();

      var checkInDTString = "";

      var dyfiMillis = -1L;
      var checkInMillis = -1L;
      var fsrMillis = -1L;
      var checkOutMillis = -1L;

      var list = map.get(MessageType.DYFI);
      if (list != null) {
        var exportedMessage = list.get(0);

        allCount += 1;
        points += 15;
        ++ppDyfiCount;

        var dyfiMessage = (DyfiMessage) list.get(0);
        m.data().put(KEY_DYFI_COUNT, 1);

        var dyfiDtUtc = dyfiMessage.dateTime;
        dyfiMillis = 1000 * dyfiDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = dyfiMillis < startMillis;
        boolean isAfter = dyfiMillis > stopMillis;
        var timeOk = !isBefore && !isAfter;
        m.data().put(KEY_DYFI_TIME_OK, timeOk);
        if (timeOk) {
          points += 5;
          ++ppDyfiTimeOk;
          ++allTimes;
        } else {
          if (isBefore) {
            explanations
                .add("DYFI time: " + dyfiDtUtc.toString() + " before exercise start("
                    + DATETIME_FORMATTER.format(utcStartDT) + ")");
          } else {
            explanations
                .add("DYFI time: " + dyfiDtUtc.toString() + " after exercise end("
                    + DATETIME_FORMATTER.format(utcStopDT) + ")");
          }
        }

        var addressesOk = checkRequiredAddreses(exportedMessage, DYFI_REQUIRED_ADDRESS_SET);
        m.data().put(KEY_DYFI_ADDRESSES_OK, addressesOk);
        if (addressesOk) {
          points += 5;
          ++ppDyfiAddressesOk;
          ++allAddresses;
        } else {
          explanations.add("DYFI didn't contain all required addresses");
        }

        var hasWatermelon = dyfiMessage.comments != null && dyfiMessage.comments.toLowerCase().contains("watermelon");
        m.data().put(KEY_DYFI_WATERMELON, hasWatermelon);
        ppDyfiWatermelonCount += (hasWatermelon) ? 1 : 0;
        watermelonCount += (hasWatermelon) ? 1 : 0;
      } else {
        m.data().put(KEY_DYFI_COUNT, 0);
        m.data().put(KEY_DYFI_TIME_OK, false);
        m.data().put(KEY_DYFI_ADDRESSES_OK, false);
        m.data().put(KEY_DYFI_WATERMELON, false);
        explanations.add("No DYFI message");
      }

      list = map.get(MessageType.CHECK_IN);
      if (list != null) {
        var exportedMessage = list.get(0);

        allCount += 1;
        points += 15;
        ++ppCheckInCount;

        var checkInMessage = (CheckInMessage) list.get(0);
        m.data().put(KEY_CHECK_IN_COUNT, 1);

        var checkInDtUtc = checkInMessage.dateTime;
        checkInDTString = DATETIME_FORMATTER.format(checkInDtUtc);
        checkInMillis = 1000 * checkInDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = checkInMillis < startMillis || checkInMillis < dyfiMillis;
        boolean isAfter = checkInMillis > stopMillis;
        var timeOk = !isBefore && !isAfter;
        m.data().put(KEY_CHECK_IN_TIME_OK, timeOk);
        if (timeOk) {
          points += 5;
          ++ppCheckInTimeOk;
          ++allTimes;
        } else {
          if (isBefore) {
            explanations.add("Check In time: " + checkInDtUtc.toString() + " before exercise start or DYFI");
          } else {
            explanations
                .add("Check In time: " + checkInDtUtc.toString() + " after exercise end("
                    + DATETIME_FORMATTER.format(utcStopDT) + ")");
          }
        }

        var addressesOk = checkRequiredAddreses(exportedMessage, OTHER_REQUIRED_ADDRESS_SET);
        m.data().put(KEY_CHECK_IN_ADDRESSES_OK, addressesOk);
        if (addressesOk) {
          points += 5;
          ++ppCheckInAddressesOk;
          ++allAddresses;
        } else {
          explanations.add("Check In didn't contain all required addresses");
        }

        var hasWatermelon = checkInMessage.comments != null
            && checkInMessage.comments.toLowerCase().contains("watermelon");
        m.data().put(KEY_CHECKIN_WATERMELON, hasWatermelon);
        ppCheckInWatermelonCount += (hasWatermelon) ? 1 : 0;
        watermelonCount += (hasWatermelon) ? 1 : 0;
      } else {
        m.data().put(KEY_CHECK_IN_COUNT, 0);
        m.data().put(KEY_CHECK_IN_TIME_OK, false);
        m.data().put(KEY_CHECK_IN_ADDRESSES_OK, false);
        m.data().put(KEY_CHECKIN_WATERMELON, false);
        explanations.add("No Check In message");
      }

      list = map.get(MessageType.UNIFIED_FIELD_SITUATION);
      if (list != null) {
        var exportedMessage = list.get(0);

        allCount += 1;
        points += 15;
        ++ppFsrCount;

        LocalDateTime fsrDtUtc = null;
        boolean hasWatermelon = false;

        var fsrMessage = (UnifiedFieldSituationMessage) list.get(0);
        fsrDtUtc = fsrMessage.dateTime;
        hasWatermelon = fsrMessage.additionalComments != null
            && fsrMessage.additionalComments.toLowerCase().contains("watermelon");

        m.data().put(KEY_FSR_COUNT, 1);

        fsrMillis = 1000 * fsrDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = fsrMillis < startMillis || checkInMillis < dyfiMillis;
        boolean isAfter = fsrMillis > stopMillis;
        var timeOk = !isBefore && !isAfter;
        m.data().put(KEY_FSR_TIME_OK, timeOk);
        if (timeOk) {
          points += 5;
          ++ppFsrTimeOk;
          ++allTimes;
        } else {
          if (isBefore) {
            explanations.add("FSR time: " + fsrDtUtc.toString() + " before exercise start or DYFI");
          } else {
            explanations
                .add("FSR time: " + fsrDtUtc.toString() + " after exercise end(" + DATETIME_FORMATTER.format(utcStopDT)
                    + ")");
          }
        }

        var addressesOk = checkRequiredAddreses(exportedMessage, OTHER_REQUIRED_ADDRESS_SET);
        m.data().put(KEY_FSR_ADDRESSES_OK, addressesOk);
        if (addressesOk) {
          points += 5;
          ++ppFsrAddressesOk;
          ++allAddresses;
        } else {
          explanations.add("FSR didn't contain all required addresses");
        }

        m.data().put(KEY_DYFI_WATERMELON, hasWatermelon);
        ppFsrWatermelonCount += (hasWatermelon) ? 1 : 0;
        watermelonCount += (hasWatermelon) ? 1 : 0;
      } else {
        m.data().put(KEY_FSR_COUNT, 0);
        m.data().put(KEY_FSR_TIME_OK, false);
        m.data().put(KEY_FSR_ADDRESSES_OK, false);
        m.data().put(KEY_FSR_WATERMELON, false);
        explanations.add("No FSR message");
      }

      list = map.get(MessageType.CHECK_OUT);
      if (list != null) {
        var exportedMessage = list.get(0);

        allCount += 1;
        points += 15;
        ++ppCheckOutCount;

        var checkOutMessage = (CheckOutMessage) list.get(0);
        m.data().put(KEY_CHECK_OUT_COUNT, 1);

        var checkOutDtUtc = checkOutMessage.dateTime;
        checkOutMillis = 1000 * checkOutDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = checkOutMillis < startMillis || checkInMillis < dyfiMillis;
        if (checkInMillis >= 0) {
          isBefore = isBefore || ((checkOutMillis - checkInMillis) < 12 * 3600 * 1000);
        }
        boolean isAfter = checkOutMillis > stopMillis;
        var timeOk = !isBefore && !isAfter;
        m.data().put(KEY_CHECK_OUT_TIME_OK, timeOk);
        if (timeOk) {
          points += 5;
          ++ppCheckOutTimeOk;
          ++allTimes;
        } else {
          if (isBefore) {
            explanations
                .add("Check Out time: " + checkOutDtUtc.toString() + " before 12 hours after check in("
                    + checkInDTString + ")");
          } else {
            explanations
                .add("Check Out time: " + checkOutDtUtc.toString() + " after exercise end("
                    + DATETIME_FORMATTER.format(utcStopDT) + ")");
          }
        }

        var addressesOk = checkRequiredAddreses(exportedMessage, OTHER_REQUIRED_ADDRESS_SET);
        m.data().put(KEY_CHECK_OUT_ADDRESSES_OK, addressesOk);
        if (addressesOk) {
          points += 5;
          ++ppCheckOutAddressesOk;
          ++allAddresses;
        } else {
          explanations.add("Check Out didn't contain all required addresses");
        }
      } else {
        m.data().put(KEY_CHECK_OUT_COUNT, 0);
        m.data().put(KEY_CHECK_OUT_TIME_OK, false);
        m.data().put(KEY_CHECK_OUT_ADDRESSES_OK, false);
        explanations.add("No Check out message");
      }

      m.data().put(KEY_ALL_COUNT, allCount);
      m.data().put(KEY_ALL_TIMES, allTimes);
      m.data().put(KEY_ALL_ADDRESSES, allAddresses);
      m.data().put(KEY_WATERMELON_COUNT, watermelonCount);

      points = Math.min(points, 100);
      m.data().put(KEY_GRADE, points);
      if (points == 100) {
        m.data().put(KEY_EXPLANATION, "Perfect score!");
      } else {
        m.data().put(KEY_EXPLANATION, String.join("\n", explanations));
      }
      var scoreCount = ppScoreCountMap.getOrDefault(points, Integer.valueOf(0));
      ++scoreCount;
      ppScoreCountMap.put(points, scoreCount);

      newList.add(m);
    } // end for over aggregate

    var sb = new StringBuilder();
    sb.append("\n\nShifting SoCal aggregate results:\n");
    sb.append("participants with required messages: " + ppCount + "\n");

    sb.append("\nCorrect message counts\n");
    sb.append("   DYFI message present:      " + ppDyfiCount + formatPercent(ppDyfiCount, ppCount) + "\n");
    sb.append("   Check In message present:  " + ppCheckInCount + formatPercent(ppCheckInCount, ppCount) + "\n");
    sb.append("   FSR message present:       " + ppDyfiCount + formatPercent(ppFsrCount, ppCount) + "\n");
    sb.append("   Check Out message present: " + ppCheckOutCount + formatPercent(ppCheckOutCount, ppCount) + "\n");

    sb.append("   DYFI Time Ok:              " + ppDyfiTimeOk + formatPercent(ppDyfiTimeOk, ppCount) + "\n");
    sb.append("   Check In Time Ok:          " + ppCheckInTimeOk + formatPercent(ppCheckInTimeOk, ppCount) + "\n");
    sb.append("   FSR Time Ok:               " + ppFsrTimeOk + formatPercent(ppFsrTimeOk, ppCount) + "\n");
    sb.append("   Check Out Time Ok:         " + ppCheckOutTimeOk + formatPercent(ppCheckOutTimeOk, ppCount) + "\n");

    sb.append("   DYFI Addresses Ok:         " + ppDyfiAddressesOk + formatPercent(ppDyfiAddressesOk, ppCount) + "\n");
    sb
        .append("   Check In Addresses Ok:     " + ppCheckInAddressesOk + formatPercent(ppCheckInAddressesOk, ppCount)
            + "\n");
    sb.append("   FSR Addresses Ok:          " + ppFsrAddressesOk + formatPercent(ppFsrAddressesOk, ppCount) + "\n");
    sb
        .append("   CheckOut Addresses Ok:     " + ppCheckOutAddressesOk + formatPercent(ppCheckOutAddressesOk, ppCount)
            + "\n");

    sb
        .append("   DYFI Watermelon Count:     " + ppDyfiWatermelonCount + formatPercent(ppDyfiWatermelonCount, ppCount)
            + "\n");
    sb
        .append("   CheckIn Watermelon Count:  " + ppCheckInWatermelonCount
            + formatPercent(ppCheckInWatermelonCount, ppCount) + "\n");
    sb
        .append("   FSR Watermelon Count:      " + ppFsrWatermelonCount + formatPercent(ppFsrWatermelonCount, ppCount)
            + "\n");

    var scores = new ArrayList<Integer>(ppScoreCountMap.keySet());
    Collections.sort(scores, Comparator.reverseOrder());
    sb.append("\nscores: \n");
    for (int score : scores) {
      var count = ppScoreCountMap.get(score);
      sb.append("   score: " + score + ", count: " + count + "\n");
    }

    logger.info(sb.toString());

    aggregateMessages = newList;
  }

  private boolean checkRequiredAddreses(ExportedMessage exportedMessage, Set<String> requiredSet) {
    var messageSet = new HashSet<String>();

    var toList = exportedMessage.toList;
    if (toList != null) {
      var fields = toList.split(",");
      for (var field : fields) {
        messageSet.add(field.trim());
      }
    }

    var ccList = exportedMessage.ccList;
    if (ccList != null) {
      var fields = ccList.split(",");
      for (var field : fields) {
        messageSet.add(field.trim());
      }
    }

    if (messageSet.size() < requiredSet.size()) {
      return false;
    }

    for (String s : requiredSet) {
      if (!messageSet.contains(s)) {
        return false;
      }
    }

    return true;
  }

  private String formatPercent(int numerator, int denominator) {
    double percent = (100d * numerator) / denominator;
    return " (" + String.format("%.2f", percent) + "%)";
  }

  @Override
  public void output(String pathName) {
    super.output(pathName);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { //
        "From", //
        "DateTime", //
        "Latitude", //
        "Longitude", //
        "DYFI Count", //
        "Check In Count", //
        "FSR Count", //
        "Check Out Count", //
        "All Count", //

        "DYFI Time OK", //
        "Check In Time OK", //
        "FSR Time OK", //
        "Check Out Time OK", //
        "All Times", //

        "DYFI Addresses OK", //
        "Check In Addresses OK", //
        "FSR Addresses OK", //
        "Check Out Addresses OK", //
        "All Addresses", //

        "DYFI Watermelon?", //
        "CheckIn Watermelon?", //
        "FSR Watermelon?", //
        "Watermelons", //

        "Grade", //
        "Explanation", //
    };
  }

  @Override
  public String[] getValues(AggregateMessage m) {
    var ret = new String[] { //
        m.from(), //
        m.data().get(KEY_END_DATETIME).toString(), //
        String.valueOf(m.data().get(KEY_END_LATITUDE)), //
        String.valueOf(m.data().get(KEY_END_LONGITUDE)), //
        String.valueOf(m.data().get(KEY_DYFI_COUNT)), //
        String.valueOf(m.data().get(KEY_CHECK_IN_COUNT)), //
        String.valueOf(m.data().get(KEY_FSR_COUNT)), //
        String.valueOf(m.data().get(KEY_CHECK_OUT_COUNT)), //
        String.valueOf(m.data().get(KEY_ALL_COUNT)), //

        String.valueOf(m.data().get(KEY_DYFI_TIME_OK)), //
        String.valueOf(m.data().get(KEY_CHECK_IN_TIME_OK)), //
        String.valueOf(m.data().get(KEY_FSR_TIME_OK)), //
        String.valueOf(m.data().get(KEY_CHECK_OUT_TIME_OK)), //
        String.valueOf(m.data().get(KEY_ALL_TIMES)), //

        String.valueOf(m.data().get(KEY_DYFI_ADDRESSES_OK)), //
        String.valueOf(m.data().get(KEY_CHECK_IN_ADDRESSES_OK)), //
        String.valueOf(m.data().get(KEY_FSR_ADDRESSES_OK)), //
        String.valueOf(m.data().get(KEY_CHECK_OUT_ADDRESSES_OK)), //
        String.valueOf(m.data().get(KEY_ALL_ADDRESSES)), //

        String.valueOf(m.data().get(KEY_DYFI_WATERMELON)), //
        String.valueOf(m.data().get(KEY_CHECKIN_WATERMELON)), //
        String.valueOf(m.data().get(KEY_FSR_WATERMELON)), //
        String.valueOf(m.data().get(KEY_WATERMELON_COUNT)), //

        String.valueOf(m.data().get(KEY_GRADE)), //
        String.valueOf(m.data().get(KEY_EXPLANATION)), //
    };
    return ret;
  }

}
