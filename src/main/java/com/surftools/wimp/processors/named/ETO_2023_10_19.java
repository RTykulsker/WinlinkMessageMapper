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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.service.FieldTestService;
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
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");
  public static final DateTimeFormatter FORM_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  public static final DateTimeFormatter FORM_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private record Result(LatLongPair location, String feedbackCountString, String feedback, DyfiMessage message)
      implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(DyfiMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { "Call", "Form Latitude", "Form Longitude", //
              "Feedback Count", "Feedback", });
      Collections.addAll(resultList, DyfiMessage.getStaticHeaders());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(DyfiMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { message.from, //
              location.getLatitude(), location.getLongitude(), feedbackCountString, feedback, });
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
    final var windowOpenDT = LocalDateTime.of(2023, 10, 19, 14, 30, 0);
    final var windowCloseDT = LocalDateTime.of(2023, 10, 20, 23, 59, 0);
    var ppCount = 0;
    var ppMessageCorrectCount = 0;
    var intensityCounter = new Counter();
    var versionCounter = new Counter();
    var feedBackCounter = new Counter();
    var badLocationCalls = new ArrayList<String>();
    var callResultsMap = new HashMap<String, IWritableTable>();

    var fts = new FieldTestService();
    fts.add("addresses-bk", "To and/or CC addresses must contain ETO-BK");
    fts.add("addresses-usgs", "To and/or CC addresses must contain " + REQUIRED_USGS_ADDRESS);
    fts.add("windowOpen", "Message must be sent on or after " + DT_FORMATTER.format(windowOpenDT), windowOpenDT);
    fts.add("windowClose", "Message must be sent on or before " + DT_FORMATTER.format(windowCloseDT), windowCloseDT);
    fts.add("eventType", "Event Type must be: #EV", "EXERCISE");
    fts.add("exerciseId", "Exercise Id must be: #EV", "2023 SHAKEOUT");
    fts.add("dyfi", "Did You feel it must be: #EV", "Yes");
    fts.add("formDate", "Date of Earthquake must be: 10/19/2023");
    fts.add("formTime", "Time of Earthquake must be: 10:19");
    fts.add("response", "How did you respond must be: #EV", "Dropped and covered");
    fts.add("formLocation", "LAT/LON must be provided");

    for (var message : mm.getMessagesForType(MessageType.DYFI)) {
      DyfiMessage m = (DyfiMessage) message;
      var call = m.from;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.debug("messageId: " + m.messageId + ", from: " + m.from);
      }

      ++ppCount;
      fts.reset();

      var addressList = m.toList + "," + m.ccList;
      fts.test("addresses-bk", addressList, addressList.contains("ETO-BK"));
      fts.test("addresses-usgs", addressList, addressList.contains(REQUIRED_USGS_ADDRESS));
      fts.testOnOrAfter("windowOpen", m.msgDateTime, DT_FORMATTER);
      fts.testOnOrBefore("windowClose", m.msgDateTime, DT_FORMATTER);
      fts.test("eventType", m.isRealEvent ? "REAL EVENT" : "EXERCISE");
      fts.test("exerciseId", m.exerciseId == null ? null : m.exerciseId.trim().toUpperCase());
      fts.test("dyfi", m.isFelt ? "Yes" : "No");

      var response = m.response == null ? "Not specified" : m.response;
      fts
          .test("response", response.equalsIgnoreCase("duck") ? "Dropped and covered"
              : m.response == null ? "Not specified" : m.response);

      try {
        fts.testDtEquals("formDate", m.msgDateTime, FORM_DATE_FORMATTER);
      } catch (Exception e) {
        fts.fail("formDate", "Missing/Invalid Date of Earthquake");
      }

      try {
        fts.testDtEquals("formTime", m.msgDateTime, FORM_TIME_FORMATTER);
      } catch (Exception e) {
        fts.fail("formTime", "Missing/Invalid Time of Earthquake");
      }

      intensityCounter.incrementNullSafe(m.intensity);
      versionCounter.incrementNullSafe(m.formVersion);

      var formLocation = m.formLocation;
      if (formLocation == null) {
        badLocationCalls.add(call);
        fts.fail("formLocation", "Missing LAT/LON");
      } else if (!formLocation.isValid() || formLocation.equals(LatLongPair.ZERO_ZERO)) {
        badLocationCalls.add(call);
        fts.fail("formLocation", "Invalid LAT/LON");
      } else {
        fts.pass("formLocation");
      }

      var feedback = "Perfect Message";
      var feedbackCountString = "0";
      var explanations = fts.getExplanations();
      if (explanations.size() == 0) {
        ++ppMessageCorrectCount;
        feedBackCounter.increment("0");
      } else {
        feedback = String.join("\n", explanations);
        feedbackCountString = (explanations.size() < 10) ? String.valueOf(explanations.size()) : "10 or more";
        feedBackCounter.increment(feedbackCountString);
      }

      var result = new Result(formLocation, feedbackCountString, feedback, m);

      var outboundMessage = new OutboundMessage(outboundMessageSender, call, outboundMessageSubject + " " + m.messageId,
          feedback, null);
      outboundMessageList.add(outboundMessage);

      callResultsMap.put(call, result);
    } // end loop over DYFI messages

    var sb = new StringBuilder();
    sb.append("\n\nETO 2023-10-19 aggregate results:\n");
    sb.append("DYFI participants: " + ppCount + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, ppCount));

    var it = fts.iterator();
    while (it.hasNext()) {
      var key = it.next();
      sb.append(fts.format(key));
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Responses", fts.getCounter("response")));
    sb.append(formatCounter("ExerciseId", fts.getCounter("exerciseId")));
    sb.append(formatCounter("Intensity", intensityCounter));
    sb.append(formatCounter("Version", versionCounter));

    logger.info(sb.toString());

    if (badLocationCalls.size() > 0) {
      logger
          .info(
              "adjusting lat/long for " + badLocationCalls.size() + " messages: " + String.join(",", badLocationCalls));
      var newLocations = LocationUtils.jitter(badLocationCalls.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationCalls.size(); ++i) {
        var call = badLocationCalls.get(i);
        var result = (Result) callResultsMap.get(call);
        var newLocation = newLocations.get(i);

        callResultsMap.put(call, new Result(newLocation, result.feedbackCountString, result.feedback, result.message));
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