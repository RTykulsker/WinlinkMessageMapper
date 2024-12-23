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

package com.surftools.wimp.processors.exercise.other;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.CheckOutMessage;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Shifting SoCal
 *
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
public class LAX_2022_06_18_Shifting_SoCal extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(LAX_2022_06_18_Shifting_SoCal.class);

  public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

  static record Entry(String call, LocalDateTime dateTime, LatLongPair location, //
      int dyfiCount, int checkInCount, int fsrCount, int checkOutCount, //
      boolean dyfiTimeOk, boolean checkInTimeOk, boolean fsrTimeOk, boolean checkOutTimeOk, //
      boolean dyfiAddrOk, boolean checkInAddrOk, boolean fsrAddrOk, boolean checkOutAddrOk, //
      boolean dyfiWatermelon, boolean checkInWatermelon, boolean fsrWatermelon, //
      String grade, String explanation) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      return new String[] { //
          "From", "DateTime", "Latitude", "Longitude", //
          "DYFI Count", "Check In Count", "FSR Count", "Check Out Count", "All Count", //
          "DYFI Time OK", "Check In Time OK", "FSR Time OK", "Check Out Time OK", "All Times", //
          "DYFI Addresses OK", "Check In Addresses OK", "FSR Addresses OK", "Check Out Addresses OK", "All Addresses", //
          "DYFI Watermelon?", "CheckIn Watermelon?", "FSR Watermelon?", "All Watermelons", //
          "Grade", "Explanation", //
      };
    }

    @Override
    public String[] getValues() {
      var latitude = (location == null) ? "" : location.getLatitude();
      var longitude = (location == null) ? "" : location.getLongitude();
      var allCount = dyfiCount + checkInCount + fsrCount + checkOutCount;
      var allTimeOk = dyfiTimeOk && checkInTimeOk && fsrTimeOk && checkOutTimeOk;
      var allAddrOk = dyfiAddrOk && checkInAddrOk && fsrAddrOk && checkOutAddrOk;
      var allWatermelons = dyfiWatermelon && checkInWatermelon && fsrWatermelon;
      var ret = new String[] { //
          call, dateTime.format(DATETIME_FORMATTER), latitude, longitude, //
          String.valueOf(dyfiCount), String.valueOf(checkInCount), String.valueOf(fsrCount),
          String.valueOf(checkOutCount), String.valueOf(allCount), //
          String.valueOf(dyfiTimeOk), String.valueOf(checkInTimeOk), String.valueOf(fsrTimeOk),
          String.valueOf(checkOutTimeOk), String.valueOf(allTimeOk), //
          String.valueOf(dyfiAddrOk), String.valueOf(checkInAddrOk), String.valueOf(fsrAddrOk),
          String.valueOf(checkOutAddrOk), String.valueOf(allAddrOk), //
          String.valueOf(dyfiWatermelon), String.valueOf(checkInWatermelon), String.valueOf(fsrWatermelon),
          String.valueOf(allWatermelons), //
          grade, explanation, };
      return ret;
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Entry) other;
      return call.compareTo(o.call);
    }
  };

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

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  @Override
  /**
   * include all DYFI, Check In, FSR and FSR_23 and Check Out
   *
   */
  public void process() {
    final var exerciseStartDT = LocalDateTime.of(2022, 06, 18, 10, 18, 00);
    final var utcStartDT = LocalDateTime.of(2022, 06, 18, 17, 18, 00);
    final var LOCAL_ZONE_ID = ZoneId.of("America/Los_Angeles");
    final var zonedStartDT = ZonedDateTime.of(exerciseStartDT, LOCAL_ZONE_ID);
    final var startMillis = zonedStartDT.toInstant().toEpochMilli();

    final var exerciseStopDT = LocalDateTime.of(2022, 06, 19, 18, 00, 00);
    final var utcStopDT = LocalDateTime.of(2022, 06, 20, 1, 0, 00);
    final var zonedStopDT = ZonedDateTime.of(exerciseStopDT, LOCAL_ZONE_ID);
    final var stopMillis = zonedStopDT.toInstant().toEpochMilli();

    var entries = new ArrayList<IWritableTable>();

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

    var scoreCounter = new Counter();

    var it = mm.getSenderIterator();
    while (it.hasNext()) {
      var from = it.next();

      var map = mm.getMessagesForSender(from);

      // instance variables
      String call = null;
      LocalDateTime dateTime = null;
      LatLongPair location = null;
      int dyfiCount = 0;
      int checkInCount = 0;
      int fsrCount = 0;
      int checkOutCount = 0;
      boolean dyfiTimeOk = false;
      boolean checkInTimeOk = false;
      boolean fsrTimeOk = false;
      boolean checkOutTimeOk = false;
      boolean dyfiAddrOk = false;
      boolean checkInAddrOk = false;
      boolean fsrAddrOk = false;
      boolean checkOutAddrOk = false;
      boolean dyfiWatermelon = false;
      boolean checkInWatermelon = false;
      boolean fsrWatermelon = false;
      String grade = null;
      String explanation = null;

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();

      var checkInDTString = "";

      var dyfiMillis = -1L;
      var checkInMillis = -1L;
      var fsrMillis = -1L;
      var checkOutMillis = -1L;

      var list = map.get(MessageType.DYFI);
      if (list != null) {
        Collections.reverse(list);
        var dyfiMessage = (DyfiMessage) list.iterator().next();

        if (dyfiMessage.mapLocation.isValid()) {
          location = dyfiMessage.mapLocation;
        }

        points += 15;
        ++ppDyfiCount;

        var dyfiDtUtc = dyfiMessage.sortDateTime;
        dyfiMillis = 1000 * dyfiDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = dyfiMillis < startMillis;
        boolean isAfter = dyfiMillis > stopMillis;
        dyfiTimeOk = !isBefore && !isAfter;

        if (dyfiTimeOk) {
          points += 5;
          ++ppDyfiTimeOk;
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

        dyfiAddrOk = checkRequiredAddreses(dyfiMessage, DYFI_REQUIRED_ADDRESS_SET);
        if (dyfiAddrOk) {
          points += 5;
          ++ppDyfiAddressesOk;
        } else {
          explanations.add("DYFI didn't contain all required addresses");
        }

        dyfiWatermelon = dyfiMessage.comments != null && dyfiMessage.comments.toLowerCase().contains("watermelon");
        ppDyfiWatermelonCount += (dyfiWatermelon) ? 1 : 0;
      } else {
        explanations.add("No DYFI message");
      }

      list = map.get(MessageType.CHECK_IN);
      if (list != null) {
        Collections.reverse(list);
        var checkInMessage = (CheckInMessage) list.iterator().next();

        if ((location == null || !location.isValid()) && checkInMessage.mapLocation.isValid()) {
          location = checkInMessage.mapLocation;
        }

        points += 15;
        ++ppCheckInCount;

        var checkInDtUtc = checkInMessage.sortDateTime;
        checkInDTString = DATETIME_FORMATTER.format(checkInDtUtc);
        checkInMillis = 1000 * checkInDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = checkInMillis < startMillis || checkInMillis < dyfiMillis;
        boolean isAfter = checkInMillis > stopMillis;
        checkInTimeOk = !isBefore && !isAfter;
        if (checkInTimeOk) {
          points += 5;
          ++ppCheckInTimeOk;
        } else {
          if (isBefore) {
            explanations.add("Check In time: " + checkInDtUtc.toString() + " before exercise start or DYFI");
          } else {
            explanations
                .add("Check In time: " + checkInDtUtc.toString() + " after exercise end("
                    + DATETIME_FORMATTER.format(utcStopDT) + ")");
          }
        }

        checkInAddrOk = checkRequiredAddreses(checkInMessage, OTHER_REQUIRED_ADDRESS_SET);
        if (checkInAddrOk) {
          points += 5;
          ++ppCheckInAddressesOk;
        } else {
          explanations.add("Check In didn't contain all required addresses");
        }

        checkInWatermelon = checkInMessage.comments != null
            && checkInMessage.comments.toLowerCase().contains("watermelon");
        ppCheckInWatermelonCount += (checkInWatermelon) ? 1 : 0;
      } else {
        explanations.add("No Check In message");
      }

      list = map.get(MessageType.FIELD_SITUATION);
      if (list != null) {
        Collections.reverse(list);
        var fsrMessage = (FieldSituationMessage) list.iterator().next();

        if ((location == null || !location.isValid()) && fsrMessage.mapLocation.isValid()) {
          location = fsrMessage.mapLocation;
        }

        points += 15;
        ++ppFsrCount;

        var fsrDtUtc = fsrMessage.sortDateTime;
        fsrMillis = 1000 * fsrDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = fsrMillis < startMillis || checkInMillis < dyfiMillis;
        boolean isAfter = fsrMillis > stopMillis;
        fsrTimeOk = !isBefore && !isAfter;
        if (fsrTimeOk) {
          points += 5;
          ++ppFsrTimeOk;
        } else {
          if (isBefore) {
            explanations.add("FSR time: " + fsrDtUtc.toString() + " before exercise start or DYFI");
          } else {
            explanations
                .add("FSR time: " + fsrDtUtc.toString() + " after exercise end(" + DATETIME_FORMATTER.format(utcStopDT)
                    + ")");
          }
        }

        fsrAddrOk = checkRequiredAddreses(fsrMessage, OTHER_REQUIRED_ADDRESS_SET);
        if (fsrAddrOk) {
          points += 5;
          ++ppFsrAddressesOk;
        } else {
          explanations.add("FSR didn't contain all required addresses");
        }

        fsrWatermelon = fsrMessage.additionalComments != null
            && fsrMessage.additionalComments.toLowerCase().contains("watermelon");
        ppFsrWatermelonCount += (fsrWatermelon) ? 1 : 0;
      } else {
        explanations.add("No FSR message");
      }

      list = map.get(MessageType.CHECK_OUT);
      if (list != null) {
        Collections.reverse(list);
        var checkOutMessage = (CheckOutMessage) list.iterator().next();

        if ((location == null || !location.isValid()) && checkOutMessage.mapLocation.isValid()) {
          location = checkOutMessage.mapLocation;
        }

        points += 15;
        ++ppCheckOutCount;

        var checkOutDtUtc = checkOutMessage.sortDateTime;
        checkOutMillis = 1000 * checkOutDtUtc.toEpochSecond(ZoneOffset.UTC);
        boolean isBefore = checkOutMillis < startMillis || checkInMillis < dyfiMillis;
        if (checkInMillis >= 0) {
          isBefore = isBefore || ((checkOutMillis - checkInMillis) < 12 * 3600 * 1000);
        }
        boolean isAfter = checkOutMillis > stopMillis;
        checkOutTimeOk = !isBefore && !isAfter;
        if (checkOutTimeOk) {
          points += 5;
          ++ppCheckOutTimeOk;
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

        checkOutAddrOk = checkRequiredAddreses(checkOutMessage, OTHER_REQUIRED_ADDRESS_SET);
        if (checkOutAddrOk) {
          points += 5;
          ++ppCheckOutAddressesOk;
        } else {
          explanations.add("Check Out didn't contain all required addresses");
        }
      } else {
        explanations.add("No Check out message");
      }
      points = Math.min(100, points);
      points = Math.max(0, points);
      grade = String.valueOf(points);
      scoreCounter.increment(points);

      if (points == 100) {
        explanation = "Perfect score!";
      } else {
        explanation = String.join("\n", explanations);
      }

      var entry = new Entry(call, dateTime, location, //
          dyfiCount, checkInCount, fsrCount, checkOutCount, //
          dyfiTimeOk, checkInTimeOk, fsrTimeOk, checkOutTimeOk, //
          dyfiAddrOk, checkInAddrOk, fsrAddrOk, checkOutAddrOk, //
          dyfiWatermelon, checkInWatermelon, fsrWatermelon, //
          grade, explanation);

      entries.add(entry);
    } // end for over aggregate

    var sb = new StringBuilder();
    sb.append("\n\nShifting SoCal aggregate results:\n");
    sb.append("participants with required messages: " + ppCount + "\n");

    sb.append("\nCorrect message counts\n");
    sb.append(formatPP("DYFI message present", ppDyfiCount, ppCount));
    sb.append(formatPP("Check In message present", ppCheckInCount, ppCount));
    sb.append(formatPP("FSR message present", ppFsrCount, ppCount));
    sb.append(formatPP("Check Out message present", ppCheckOutCount, ppCount));

    sb.append(formatPP("DYFI Time Ok" + ppDyfiTimeOk, ppDyfiTimeOk, ppCount));
    sb.append(formatPP("Check In Time Ok", ppCheckInTimeOk, ppCount));
    sb.append(formatPP("FSR Time Ok", ppFsrTimeOk, ppCount));
    sb.append(formatPP("Check Out Time Ok", ppCheckOutTimeOk, ppCount));

    sb.append(formatPP("DYFI Addresses Ok", ppDyfiAddressesOk, ppCount));
    sb.append(formatPP("Check In Addresses Ok", ppCheckInAddressesOk, ppCount));
    sb.append(formatPP("FSR Addresses Ok", ppFsrAddressesOk, ppCount));
    sb.append(formatPP("CheckOut Addresses Ok", ppCheckOutAddressesOk, ppCount));

    sb.append(formatPP("DYFI Watermelon Count", ppDyfiWatermelonCount, ppCount));
    sb.append(formatPP("CheckIn Watermelon Count", ppCheckInWatermelonCount, ppCount));
    sb.append(formatPP("FSR Watermelon Count", ppFsrWatermelonCount, ppCount));

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());

    WriteProcessor.writeTable(entries, Path.of(outputPathName, "results.csv"));
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

}
