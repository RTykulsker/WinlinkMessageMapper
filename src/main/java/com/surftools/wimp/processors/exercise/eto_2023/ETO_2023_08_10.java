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

package com.surftools.wimp.processors.exercise.eto_2023;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.Ics205RadioPlanMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2023-08-10 Exercise: one ICS-205; with feedback, but no grade
 *
 * "rejected" datetime and iapPageNumber processing moved here, as is explicit bad location reject processing
 *
 * feedback is given, but not an explicity grade
 *
 * @author bobt
 *
 */
public class ETO_2023_08_10 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2023_08_10.class);
  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private static Set<String> explicitBadLocationSet = Set.of("WA1CMR");

  private FormFieldManager ffm;

  static record Result(String call, String latitude, String longitude, String feedback, String feedbackCountString,
      Ics205RadioPlanMessage message) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(Ics205RadioPlanMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { "Call", "Map Latitude", "Map Longitude", "Feedback Count", "Feedback", });
      Collections.addAll(resultList, Ics205RadioPlanMessage.getStaticHeaders());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 5);
      Collections.addAll(resultList, new String[] { call, latitude, longitude, feedbackCountString, feedback, });
      Collections.addAll(resultList, message.getValues());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      return this.message.compareTo(o.message);
    }
  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    ffm = new FormFieldManager();
    ffm.add("to", new FormField(FFType.LIST, "To Address", cm.getAsString(Key.EXPECTED_DESTINATIONS), 0));

    ffm.add("org", new FormField(FFType.SPECIFIED, "Agency/Group", "Operation Clear Fog Radio Interoperability", 0));
    ffm.add("incidentName", new FormField(FFType.EQUALS_IGNORE_CASE, "IncidentName", "Operation Clear Fog", 0));

    ffm.add("opDateFrom", new FormField(FFType.EQUALS, "OP Date From", "2023-08-07", 0));
    ffm.add("opDateTo", new FormField(FFType.EQUALS, "OP Date To", "2023-08-11", 0));
    ffm.add("opTimeFrom", new FormField(FFType.EQUALS, "OP Time From", "00:00", 0));
    ffm.add("opTimeTo", new FormField(FFType.EQUALS, "OP Time To", "15:00", 0));

    for (int line = 1; line <= 10; ++line) {
      ffm.add("zone-" + line, new FormField(FFType.EMPTY, "line " + line + "-Zone/Group", null, 0));
      ffm.add("channel#-" + line, new FormField(FFType.EMPTY, "line " + line + "-Channel #", null, 0));
    }

    ffm.add("function-1", new FormField(FFType.SPECIFIED, "line 1-Function", "Interoperability", 0));
    ffm.add("channelName-1", new FormField(FFType.SPECIFIED, "line 1-Channel Name", "VTAC13", 0));
    ffm.add("assignment-1", new FormField(FFType.SPECIFIED, "line 1" + "-Assignment", "Talk-in / Tactical", 0));
    ffm.add("rxFreq-1", new FormField(FFType.DOUBLE, "line 1" + "-RxFreq", "158.7375", 0, 2));
    ffm.add("rwNW-1", new FormField(FFType.SPECIFIED, "line 1" + "-RxNW", "N", 0, 2));
    ffm.add("rxTone-1", new FormField(FFType.SPECIFIED, "line 1" + "-RxTone", "156.7", 0, 2));
    ffm.add("txFreq-1", new FormField(FFType.DOUBLE, "line 1" + "-TxFreq", "158.7375", 0, 2));
    ffm.add("txNW-1", new FormField(FFType.SPECIFIED, "line 1" + "-TxNW", "N", 0, 2));
    ffm.add("txTone-1", new FormField(FFType.SPECIFIED, "line 1" + "-TxTone", "156.7", 0, 2));
    ffm.add("mode-1", new FormField(FFType.SPECIFIED, "line 1" + "-Mode", "A", 0, 2));
    ffm.add("remarks-1", new FormField(FFType.SPECIFIED, "line 1" + "-remarks", "See Note 1", 0));

    ffm.add("function-2", new FormField(FFType.SPECIFIED, "line 2-Function", "Interoperability", 0));
    ffm.add("channelName-2", new FormField(FFType.SPECIFIED, "line 2-Channel Name", "VTAC36", 0));
    ffm.add("assignment-2", new FormField(FFType.SPECIFIED, "line 2" + "-Assignment", "Talk-in / Tactical", 0));
    ffm.add("rxFreq-2", new FormField(FFType.DOUBLE, "line 2" + "-RxFreq", "151.1375", 0, 2));
    ffm.add("rwNW-2", new FormField(FFType.SPECIFIED, "line 2" + "-RxNW", "N", 0, 2));
    ffm.add("rxTone-2", new FormField(FFType.SPECIFIED, "line 2" + "-RxTone", "156.7", 0, 2));
    ffm.add("txFreq-2", new FormField(FFType.DOUBLE, "line 2" + "-TxFreq", "159.4725", 0, 2));
    ffm.add("txNW-2", new FormField(FFType.SPECIFIED, "line 2" + "-TxNW", "N", 0, 2));
    ffm.add("txTone-2", new FormField(FFType.SPECIFIED, "line 2" + "-TxTone", "136.5", 0, 2));
    ffm.add("mode-2", new FormField(FFType.SPECIFIED, "line 2" + "-Mode", "A", 0, 2));
    ffm.add("remarks-2", new FormField(FFType.SPECIFIED, "line 2" + "-remarks", "See Note 1", 0));

    ffm.add("function-3", new FormField(FFType.SPECIFIED, "line 3-Function", "Amateur Service", 0));
    ffm.add("channelName-3", new FormField(FFType.SPECIFIED, "line 3-Channel Name", "K0DCA VHF", 0));
    ffm.add("assignment-3", new FormField(FFType.SPECIFIED, "line 3" + "-Assignment", "EOC/911 Interop", 0));
    ffm.add("rxFreq-3", new FormField(FFType.DOUBLE, "line 3" + "-RxFreq", "145.1500", 0, 2));
    ffm.add("rwNW-3", new FormField(FFType.SPECIFIED, "line 3" + "-RxNW", "W", 0, 2));
    ffm.add("rxTone-3", new FormField(FFType.EMPTY, "line 3" + "-RxTone", null, 0));
    ffm.add("txFreq-3", new FormField(FFType.DOUBLE, "line 3" + "-TxFreq", "144.5500", 0, 2));
    ffm.add("txNW-3", new FormField(FFType.SPECIFIED, "line 3" + "-TxNW", "W", 0, 2));
    ffm.add("txTone-3", new FormField(FFType.SPECIFIED, "line 3" + "-TxTone", "162.2", 0));
    ffm.add("mode-3", new FormField(FFType.SPECIFIED, "line 3" + "-Mode", "A", 0, 2));
    ffm.add("remarks-3", new FormField(FFType.EMPTY, "line 3" + "-remarks", null, 0));

    ffm.add("function-4", new FormField(FFType.SPECIFIED, "line 4-Function", "Amateur Service", 0));
    ffm.add("channelName-4", new FormField(FFType.SPECIFIED, "line 4-Channel Name", "K0DCA UHF", 0));
    ffm.add("assignment-4", new FormField(FFType.SPECIFIED, "line 4" + "-Assignment", "EOC/911 Interop", 0));
    ffm.add("rxFreq-4", new FormField(FFType.DOUBLE, "line 4" + "-RxFreq", "443.9250", 0, 2));
    ffm.add("rwNW-4", new FormField(FFType.SPECIFIED, "line 4" + "-RxNW", "W", 0, 2));
    ffm.add("rxTone-4", new FormField(FFType.EMPTY, "line 4" + "-RxTone", null, 0));
    ffm.add("txFreq-4", new FormField(FFType.DOUBLE, "line 4" + "-TxFreq", "448.9250", 0, 2));
    ffm.add("txNW-4", new FormField(FFType.SPECIFIED, "line 4" + "-TxNW", "W", 0, 2));
    ffm.add("txTone-4", new FormField(FFType.SPECIFIED, "line 4" + "-TxTone", "162.2", 0));
    ffm.add("mode-4", new FormField(FFType.SPECIFIED, "line 4" + "-Mode", "A", 0, 2));
    ffm.add("remarks-4", new FormField(FFType.EMPTY, "line 4" + "-remarks", null, 0));

    ffm.add("function-5", new FormField(FFType.SPECIFIED, "line 5-Function", "NWS Weather Info", 0));
    ffm.add("channelName-5", new FormField(FFType.LIST, "line 5-Channel Name", "KZZ43,WXJ61,WXL46", 0));
    ffm.add("assignment-5", new FormField(FFType.SPECIFIED, "line 5" + "-Assignment", "All units / agencies", 0));
    ffm
        .add("rxFreq-5",
            new FormField(FFType.CONTAINED_BY, "line 5" + "-RxFreq", "162.55000,162.42500,162.40000", 0, 2));
    ffm.add("rwNW-5", new FormField(FFType.SPECIFIED, "line 5" + "-RxNW", "W", 0, 2));
    ffm.add("rxTone-5", new FormField(FFType.EMPTY, "line 5" + "-RxTone", null, 0));
    ffm.add("txFreq-5", new FormField(FFType.EMPTY, "line 5" + "-TxFreq", null, 0, 2));
    ffm.add("txNW-5", new FormField(FFType.EMPTY, "line 5" + "-TxNW", null, 0, 2));
    ffm.add("txTone-5", new FormField(FFType.EMPTY, "line 5" + "-TxTone", null, 0));
    ffm.add("mode-5", new FormField(FFType.EMPTY, "line 5" + "-Mode", null, 0));
    ffm.add("remarks-5", new FormField(FFType.SPECIFIED, "line 5" + "-remarks", "Springfield NOAA Weather Radio", 0));

    for (int line = 6; line <= 10; ++line) {
      ffm.add("function-" + line, new FormField(FFType.EMPTY, "line " + line + "-Function", null, 0));
      ffm.add("channelName-" + line, new FormField(FFType.EMPTY, "line " + line + "-Channel Name", null, 0));
      ffm.add("assignment-" + line, new FormField(FFType.EMPTY, "line " + line + "-Assignment", null, 0));
      ffm.add("rxFreq-" + line, new FormField(FFType.EMPTY, "line " + line + "-RxFreq", null, 0));
      ffm.add("rwNW-" + line, new FormField(FFType.EMPTY, "line " + line + "-RxNW", null, 0));
      ffm.add("rxTone-" + line, new FormField(FFType.EMPTY, "line " + line + "-RxTone", null, 0));
      ffm.add("txFreq-" + line, new FormField(FFType.EMPTY, "line " + line + "-TxFreq", null, 0));
      ffm.add("txNW-" + line, new FormField(FFType.EMPTY, "line " + line + "-TxNW", null, 0));
      ffm.add("txTone-" + line, new FormField(FFType.EMPTY, "line " + line + "-TxTone", null, 0));
      ffm.add("mode-" + line, new FormField(FFType.EMPTY, "line " + line + "-Mode", null, 0));
      ffm.add("remarks-" + line, new FormField(FFType.EMPTY, "line " + line + "-remarks", null, 0));
    }

    ffm.add("approvedBy", new FormField(FFType.EQUALS_IGNORE_CASE, "Approved by", "Claudia Smith", 0));
    ffm.add("iapPage", new FormField(FFType.EQUALS, "IAP Page", "5", 0));
  }

  @Override
  public void process() {

    var ppCount = 0;
    var ppMessageCorrectCount = 0;
    var ppExplicitBadLocationCounter = 0;
    var ppMissingLocationCounter = 0;
    var ppBeforeExercise = 0;
    var ppAfterExercise = 0;

    var ppFeedBackCounter = new Counter();
    var ppDateTimePreparedCounter = new Counter();
    var ppTimeFromCounter = new Counter();
    var ppTimeToCounter = new Counter();
    var ppDateTimeApprovedCounter = new Counter();
    var ppSpecialInstructionsCounter = new Counter();
    var ppWxFreqError = new Counter();

    var callResultsMap = new HashMap<String, IWritableTable>();
    var zeroZeroLocationList = new ArrayList<String>();

    for (var m : mm.getMessagesForType(MessageType.ICS_205)) {
      var message = (Ics205RadioPlanMessage) m;
      var sender = message.from;

      var explanations = new ArrayList<String>();
      ffm.reset(explanations);
      ++ppCount;

      // exercise window
      {
        var beginExerciseWindow = LocalDateTime.of(2023, 8, 7, 0, 0);

        var messageDateTime = message.msgDateTime;
        if (messageDateTime.isBefore(beginExerciseWindow)) {
          explanations
              .add("!!!message sent (" + messageDateTime.format(DT_FORMATTER) + ") before exercise window opened");
        }

        var endExerciseWindow = LocalDateTime.of(2023, 8, 11, 15, 0);
        if (messageDateTime.isAfter(endExerciseWindow)) {
          explanations
              .add("!!!message sent (" + messageDateTime.format(DT_FORMATTER) + ") after exercise window closed");
          ++ppAfterExercise;
        }
      }

      var pair = message.msgLocation;
      if (pair == null) {
        zeroZeroLocationList.add(sender);
        ++ppMissingLocationCounter;
        explanations.add("!!! missing location ");
        pair = new LatLongPair("", "");
      } else if (!pair.isValid() || pair.equals(LatLongPair.ZERO_ZERO)) {
        zeroZeroLocationList.add(sender);
        ++ppMissingLocationCounter;
        explanations.add("!!!invalid location: " + pair.toString());
        pair = new LatLongPair("", "");
      }

      if (explicitBadLocationSet.contains(sender)) {
        ++ppExplicitBadLocationCounter;
        explanations.add("explicit bad location: " + pair.toString());
      }

      ffm.test("to", message.to);
      ffm.test("org", message.organization);
      ffm.test("incidentName", message.incidentName);

      ffm.test("opDateFrom", message.dateFrom);
      ffm.test("opDateTo", message.dateTo);

      var doSimpleOpTime = false;
      if (doSimpleOpTime) {
        ffm.test("opTimeFrom", message.timeFrom);
        ffm.test("opTimeTo", message.timeTo);
      } else {
        var times = new String[] { message.timeFrom, message.timeTo };
        var counters = new Counter[] { ppTimeFromCounter, ppTimeToCounter };
        for (int i = 0; i < 2; ++i) {
          var time = times[i];
          var counter = counters[i];
          if (time == null) {
            counter.increment(null);
          } else {
            var tokens = time.split(" ");
            boolean isOk = false;
            for (var token : tokens) {
              try {
                LocalTime.parse(token, TIME_FORMATTER);
                isOk = true;
                break;
              } catch (Exception e) {
                ;
              } // end try
            } // end for over tokens
            if (!isOk) {
              counter.increment(time);
            }
          } // time not null
        } // end loop over variables
      }

      final var wxChannelFreqMap = Map.of("KZZ43", 162.550, "WXJ61", 162.425, "WXL46", 162.400);

      for (var line : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }) {
        var radioEntry = message.radioEntries.get(line - 1);
        ffm.test("zone-" + line, radioEntry.zoneGroup());
        ffm.test("channel#-" + line, radioEntry.channelNumber());
        ffm.test("function-" + line, radioEntry.function());
        ffm.test("channelName-" + line, radioEntry.channelName());
        ffm.test("assignment-" + line, radioEntry.assignment());
        ffm.test("rxFreq-" + line, radioEntry.rxFrequency());
        ffm.test("rwNW-" + line, radioEntry.rxNarrowWide());
        ffm.test("rxTone-" + line, radioEntry.rxTone());
        ffm.test("txFreq-" + line, radioEntry.txFrequency());
        ffm.test("txNW-" + line, radioEntry.txNarrowWide());
        ffm.test("txTone-" + line, radioEntry.txTone());
        ffm.test("mode-" + line, radioEntry.mode());
        ffm.test("remarks-" + line, radioEntry.remarks());

        if (line == 5) {
          var name = radioEntry.channelName();
          var rxFreq = radioEntry.rxFrequency();
          if (name == null || rxFreq == null) {
            continue;
          }

          var expectedFreq = wxChannelFreqMap.get(name);
          if (expectedFreq == null) {
            continue;
          }

          try {
            var parsedFreq = Double.parseDouble(rxFreq);
            if (parsedFreq != expectedFreq) {
              ppWxFreqError.increment(name + " " + rxFreq + ", s/b:" + expectedFreq);
              explanations
                  .add("line 5 rxFreq(" + rxFreq + ") doesn't match expected rxFreq(" + expectedFreq + ") for station:"
                      + name);
            }
          } catch (Exception e) {
            ;
          }

        }
      }

      // special instructions
      {
        var expected = "Note 1 – VTAC13 and VTAC36 are insecure channels – use discretion or direct to your unit or agency secure channel";
        var value = message.specialInstructions;
        if (value == null) {
          ppSpecialInstructionsCounter.increment(null);
          explanations.add("Special Instructions must be: " + expected);
        } else {
          var lcValue = value.trim().toLowerCase();
          if (lcValue.contains("Note 1".toLowerCase())
              && lcValue.contains("VTAC13 and VTAC36 are insecure channels".toLowerCase())
              && lcValue.contains("use discretion or direct to your unit or agency secure channel".toLowerCase())) {
            if (!value.equalsIgnoreCase(expected)) {
              explanations.add("beware of special, non-ASCII characters in Special Instructions");
              ppSpecialInstructionsCounter.increment(value);
            } else {
              logger.info("correct special instructions from: " + sender + ", " + message.specialInstructions);
            }
          } else {
            explanations.add("Special Instructions(" + value + ") must be: " + expected);
            ppSpecialInstructionsCounter.increment(value);
          }
        }
      }

      ffm.test("approvedBy", message.approvedBy);
      ffm.test("iapPage", message.iapPage);

      {
        var dateTimePreparedString = message.dateTimePrepared;
        try {
          LocalDateTime.parse(dateTimePreparedString, DT_FORMATTER);
        } catch (Exception e) {
          logger
              .info("can't parse date/time prepared: " + dateTimePreparedString + " for call: " + sender + ", "
                  + e.getMessage());
          explanations
              .add("Can't parse Date/Time Prepared: " + dateTimePreparedString + ", expected format: "
                  + DT_FORMAT_STRING);
          ppDateTimePreparedCounter.increment(dateTimePreparedString);
        }
      }

      {
        var dateTimeApprovedString = message.approvedDateTime;
        try {
          LocalDateTime.parse(dateTimeApprovedString, DT_FORMATTER);
        } catch (Exception e) {
          logger
              .info("can't parse date/time approved: " + dateTimeApprovedString + " for call: " + sender + ", "
                  + e.getMessage());
          explanations
              .add("Can't parse Date/Time Approved: " + dateTimeApprovedString + ", expected format: "
                  + DT_FORMAT_STRING);
          ppDateTimeApprovedCounter.increment(dateTimeApprovedString);
        }
      }

      var feedback = "Perfect Message";
      var feedbackCountString = "0";
      if (explanations.size() == 0) {
        ++ppMessageCorrectCount;
      } else {
        feedback = String.join("\n", explanations);
        feedbackCountString = (explanations.size() < 10) ? String.valueOf(explanations.size()) : "10 or more";
        ppFeedBackCounter.increment(feedbackCountString);
      }

      var result = new Result(message.from, pair.getLatitude(), pair.getLongitude(), feedback, feedbackCountString,
          message);

      callResultsMap.put(result.message.from, result);
    } // end loop over for

    var sb = new StringBuilder();
    var N = ppCount;
    sb.append("\n\nETO 2023-08-10 aggregate results:\n");
    sb.append("ICS-205 participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));

    sb.append(formatPP("NO Explicit Bad Locations", ppExplicitBadLocationCounter, true, N));
    sb.append(formatPP("NO Missing or Invalid Locations", ppMissingLocationCounter, true, N));
    sb.append(formatPP("Before exercise window opened", ppBeforeExercise, true, N));
    sb.append(formatPP("After exercise window closed", ppAfterExercise, true, N));
    sb.append(formatField(ffm, "to", false, N));
    sb.append(formatField(ffm, "org", false, N));
    sb.append(formatField(ffm, "incidentName", false, N));
    sb.append(formatPP("Valid Date/Time Prepared", ppDateTimePreparedCounter.getValueTotal(), true, N));
    sb.append(formatField(ffm, "opDateFrom", true, N));
    sb.append(formatField(ffm, "opDateTo", true, N));

    sb.append(formatPP("Op Time From", ppTimeFromCounter.getKeyCount(), true, N));
    sb.append(formatPP("Op Time To", ppTimeToCounter.getKeyCount(), true, N));

    var fields = new String[] { "zone-", "channel#-", "function-", "channelName-", "assignment-", "rxFreq-", "rwNW-",
        "rxTone-", "txFreq-", "txNW-", "txTone-", "mode-", "remarks-", };
    for (var line : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }) {
      for (var field : fields) {
        sb.append(formatField(ffm, field + line, false, N));
      }
    }

    // sb.append(formatField("specialInstructions", false, N));
    sb.append(formatPP("Special Instructions", ppSpecialInstructionsCounter.getValueTotal(), true, N));

    sb.append(formatField(ffm, "approvedBy", false, N));
    sb.append(formatPP("Valid Date/Time Approved", ppDateTimeApprovedCounter.getKeyCount(), true, N));
    sb.append(formatField(ffm, "iapPage", false, N));

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));
    // sb.append(formatCounter("Invalid Op Time From", ppTimeFromCounter));
    // sb.append(formatCounter("Invalid Op Time To", ppTimeToCounter));

    sb.append(formatCounter("WX freq mismatch", ppWxFreqError));

    // sb.append(formatCounter("Special Instructions", ppSpecialInstructionsCounter));
    // sb.append(formatCounter(ffm, "org"));

    logger.info(sb.toString());

    if (zeroZeroLocationList.size() > 0) {
      logger
          .info("adjusting lat/long for " + zeroZeroLocationList.size() + " messages: "
              + String.join(",", zeroZeroLocationList));
      var newLocations = LocationUtils.jitter(zeroZeroLocationList.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < zeroZeroLocationList.size(); ++i) {
        var call = zeroZeroLocationList.get(i);
        var result = (Result) callResultsMap.get(call);
        var newLocation = newLocations.get(i);
        result.message().setMapLocation(newLocation);
      }
    }

    var results = new ArrayList<>(callResultsMap.values());

    WriteProcessor.writeTable(results, Path.of(outputPathName, "ics-205-with-feedback.csv"));
  }

}
