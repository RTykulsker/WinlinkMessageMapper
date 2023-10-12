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

package com.surftools.wimp.processors.named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;

/**
 * DYFI for Shakeout 2023
 *
 * @author bobt
 *
 */
public class ETO_2023_10_19 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2023_10_19.class);

  public static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";

  static record Result(String call, String latitude, String longitude, String feedback, String feedbackCountString,
      DyfiMessage message) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(DyfiMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { "Call", "Map Latitude", "Map Longitude", "Feedback Count", "Feedback", });
      Collections.addAll(resultList, DyfiMessage.getStaticHeaders());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(DyfiMessage.getStaticHeaders().length + 5);
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
  }

  @Override
  public void process() {
    var ppCount = 0;
    var ppMissingLocationCounter = 0;
    var ppMessageCorrectCount = 0;
    var ppIsExerciseOk = 0;
    var ppAddressToUSGSOk = 0;

    var ffm = new FormFieldManager();
    ffm.add("eventType", new FormField("!!! Event Type", "EXERCISE"));
    ffm.add("exerciseId", new FormField("Exercise Id", "2023 SHAKEOUT"));
    ffm.add("response", new FormField("Response", "duck"));
    ffm.add("windowOpen", new FormField(FFType.DATE_TIME_ON_OR_AFTER, "Message sent too early", "2023-10-19 14:30"));
    ffm.add("windowClose", new FormField(FFType.DATE_TIME_ON_OR_BEFORE, "Message sent too late", "2023-10-20 23:59"));

    var scoreCounter = new Counter();
    var responseCounter = new Counter();
    var exerciseIdCounter = new Counter();
    var intensityCounter = new Counter();
    var versionCounter = new Counter();
    var ppFeedBackCounter = new Counter();

    var noUSGSCallAddressMap = new TreeMap<String, String>();

    var callResultsMap = new HashMap<String, IWritableTable>();
    var zeroZeroLocationList = new ArrayList<String>();

    for (var message : mm.getMessagesForType(MessageType.DYFI)) {
      DyfiMessage m = (DyfiMessage) message;
      var call = m.from;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.debug("messageId: " + m.messageId + ", from: " + m.from);
      }

      ++ppCount;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      ffm.test("eventType", m.isRealEvent ? "REAL EVENT" : "EXERCISE");

      var addresses = new HashSet<String>();
      addresses.addAll(Arrays.asList(m.toList.replaceAll("SMTP:", "").split(",")));
      addresses.addAll(Arrays.asList(m.ccList.replaceAll("SMTP:", "").split(",")));
      if (!addresses.contains(REQUIRED_USGS_ADDRESS)) {
        explanations.add("!!! To: and Cc: addresses don't contain required address: " + REQUIRED_USGS_ADDRESS);
        noUSGSCallAddressMap.put(call, addresses.toString());
      } else {
        ++ppAddressToUSGSOk;
      }

      var exerciseId = m.exerciseId == null ? null : m.exerciseId.trim().toUpperCase();
      exerciseIdCounter.incrementNullSafe(exerciseId);
      ffm.test("exerciseId", exerciseId);

      var response = m.response;
      responseCounter.incrementNullSafe(response);
      ffm.test("response", response);

      var intensityString = m.intensity;
      intensityCounter.incrementNullSafe(intensityString);

      var mapLocation = m.mapLocation;
      if (mapLocation == null) {
        zeroZeroLocationList.add(call);
        ++ppMissingLocationCounter;
        explanations.add("!!! missing location ");
        mapLocation = new LatLongPair("", "");
      } else if (!mapLocation.isValid() || mapLocation.equals(LatLongPair.ZERO_ZERO)) {
        zeroZeroLocationList.add(call);
        ++ppMissingLocationCounter;
        explanations.add("!!!invalid location: " + mapLocation.toString());
        mapLocation = new LatLongPair("", "");
      }

      var version = m.formVersion;
      versionCounter.incrementNullSafe(version);

      var feedback = "Perfect Message";
      var feedbackCountString = "0";
      if (explanations.size() == 0) {
        ++ppMessageCorrectCount;
        ppFeedBackCounter.increment("0");
      } else {
        feedback = String.join("\n", explanations);
        feedbackCountString = (explanations.size() < 10) ? String.valueOf(explanations.size()) : "10 or more";
        ppFeedBackCounter.increment(feedbackCountString);
      }

      var result = new Result(call, mapLocation.getLatitude(), mapLocation.getLongitude(), feedback,
          feedbackCountString, m);

      var outboundMessage = new OutboundMessage(outboundMessageSender, call, outboundMessageSubject + " " + m.messageId,
          feedback, null);
      outboundMessageList.add(outboundMessage);

      callResultsMap.put(call, result);
    } // end loop over DYFI messages

    var sb = new StringBuilder();
    sb.append("\n\nETO 2023-10-19 aggregate results:\n");
    sb.append("DYFI participants: " + ppCount + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, ppCount));
    sb.append(formatPP("  Is Exerercise", ppIsExerciseOk, ppCount));
    sb.append(formatPP("  Addressed to USGS", ppAddressToUSGSOk, ppCount));
    sb.append(formatPP("NO Missing or Invalid Locations", ppMissingLocationCounter, true, ppCount));

    for (var key : ffm.keySet()) {
      sb.append(formatField(ffm, key, false, ppCount));
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Scores", scoreCounter));
    sb.append(formatCounter("Responses", responseCounter));
    sb.append(formatCounter("ExerciseId", exerciseIdCounter));
    sb.append(formatCounter("Intensity", intensityCounter));
    sb.append(formatCounter("Version", versionCounter));

    sb.append("\nFailed to address message to USGS: (" + REQUIRED_USGS_ADDRESS + ")\n");
    for (var call : noUSGSCallAddressMap.keySet()) {
      sb.append("  " + call + ", addresses: " + noUSGSCallAddressMap.get(call) + "\n");
    }

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

    writeTable("dyfi-with-feedback.csv", results);
  }

  @Override
  public void postProcess() {
    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }
  }
}