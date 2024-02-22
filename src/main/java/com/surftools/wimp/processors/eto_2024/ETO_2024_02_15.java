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

package com.surftools.wimp.processors.eto_2024;

import java.nio.file.Path;
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
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.HumanitarianNeedsMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;

/**
 * Processor for 2024-02-15 Exercise: Humanitarian Needs Identification Exercise
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_02_15 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_02_15.class);
  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private SimpleTestService sts = new SimpleTestService();

  static record Result(String call, String map_latitude, String map_longitude, String feedback,
      String feedbackCountString, HumanitarianNeedsMessage message) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(HumanitarianNeedsMessage.getStaticHeaders().length + 5);
      Collections
          .addAll(resultList, new String[] { "Call", "Map_Latitude", "Map_Longitude", "Feedback Count", "Feedback" });
      Collections.addAll(resultList, HumanitarianNeedsMessage.getStaticHeaders());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(HumanitarianNeedsMessage.getStaticHeaders().length + 5);
      Collections.addAll(resultList, new String[] { call, map_latitude, map_longitude, feedbackCountString, feedback });
      Collections.addAll(resultList, message.getValues());
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      return this.message.compareTo(o.message);
    }

    public static Result updateMapLocation(Result result, LatLongPair newLocation) {
      return new Result(result.call, newLocation.getLatitude(), newLocation.getLongitude(), //
          result.feedback, result.feedbackCountString, result.message);
    }
  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override
  public void process() {

    var ppCount = 0;
    var ppMessageCorrectCount = 0;
    var ppFeedBackCounter = new Counter();
    var ppVersionCounter = new Counter();
    var ppAddressCounter = new Counter();

    var messageIdResultMap = new HashMap<String, IWritableTable>();
    var badLocationMessageIds = new ArrayList<String>();

    for (var message : mm.getMessagesForType(MessageType.HUMANITARIAN_NEEDS)) {
      var m = (HumanitarianNeedsMessage) message;
      var sender = message.from;

      sts.reset();

      if (dumpIds.contains(sender)) {
        logger.info("dump: " + sender);
      }

      ++ppCount;
      ppVersionCounter.increment(m.version);

      var addressList = m.toList + "," + m.ccList;
      sts.test("To and/or CC addresses should contain ETO-BK", addressList.toUpperCase().contains("ETO-BK"), null);

      var windowOpenDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_OPEN)));
      sts.testOnOrAfter("Message should be sent on or after #EV", windowOpenDT, m.msgDateTime, DTF);

      var windowCloseDT = LocalDateTime.from(DTF.parse(cm.getAsString(Key.EXERCISE_WINDOW_CLOSE)));
      sts.testOnOrBefore("Message should be sent on or before #EV", windowCloseDT, m.msgDateTime, DTF);

      if (m.formLocation != null) {
        sts.test("H1 GPS Latititude should be #EV", "33.682389", m.formLocation.getLatitude());
        sts.test("H1 GPS Longitude should be #EV", "-78.890722", m.formLocation.getLongitude());
      } else {
        sts.test("H1 GPS Latititude should be 33.682389", false);
        sts.test("H1 GPS Longitude should be -78.890722", false);

      }
      sts.test("H2 Team ID should be #EV", "H3-01", m.teamId);

      sts.testIsDateTime("H3 Date should be in 'MM/dd/yyyy' format", m.formDate, DATE_FORMATTER);
      sts.testIsDateTime("H4 Time should be in 'hh:mm' format", m.formTime, TIME_FORMATTER);

      // Alas, text instructions had one value, image had another. We must accept both
      final var expected212 = "212 S Ocean Blvd, Myrtle Beach, SC";
      final var expected202 = "202 S Ocean Blvd, Myrtle Beach, SC";
      var h5address = sts.toAlphaNumericWords(m.address == null ? "" : m.address);
      var pred212 = sts.compareWords(h5address, expected212);
      var pred202 = sts.compareWords(h5address, expected202);
      sts.test("H5 Address should be 212 (or 202) S Ocean Blvd, Myrtle Beach, SC", pred212 || pred202, h5address);
      ppAddressCounter.increment(pred212 ? "212" : pred202 ? "202" : h5address.length() == 0 ? "empty" : "other");

      sts.test("H6 Health should be checked", m.needsHealth);
      sts.test("H6 Shelter should be checked", m.needsShelter);
      sts.test("H6 Food should be NOT checked", !m.needsFood);
      sts.test("H6 Water should be NOT checked", !m.needsWater);
      sts.test("H6 Logistics should be checked", m.needsLogistics);
      sts.test("H6 Other should be checked", m.needsOther);

      sts
          .test("H7 Description should be #EV",
              "Harbor Three Resort experienced a gas leak which led to an explosion causing the West side of the resort to collapse. The resort personnel discovered they had 130 of the 240 guests accounted for with many of the remaining feared trapped in the rubble.",
              m.description);

      var debugH7 = false;
      if (debugH7) {
        final var h7Expected = "Harbor Three Resort experienced a gas leak which led to an explosion causing the West side of the resort to collapse. The resort personnel discovered they had 130 of the 240 guests accounted for with many of the remaining feared trapped in the rubble.";
        if (!sts.compareWords(h7Expected, m.description)) {
          System.err
              .println(
                  "for call: " + m.from + ", H7 description should be\n" + h7Expected + ", not \n" + m.description);
          System.err.println();
          System.err.flush();
        }
      }

      sts.test("H8 Other Information should be #EV", "EXERCISE: Request Urban Search and Rescue Assets.", m.other);

      sts.test("Completed by should be #EV", "Ricky Jones", m.approvedBy);
      sts.test("Title/Position should be #EV", "Emergency Communications Officer", m.position);

      var pair = m.msgLocation;
      if (pair == null || pair.equals(LatLongPair.ZERO_ZERO)) {
        pair = LatLongPair.ZERO_ZERO;
        badLocationMessageIds.add(m.messageId);
        sts.test("LAT/LON should be provided", false, "missing");
      } else if (!pair.isValid()) {
        sts.test("LAT/LON should be provided", false, "invalid " + pair.toString());
        pair = LatLongPair.ZERO_ZERO;
        badLocationMessageIds.add(m.messageId);
      } else {
        sts.test("LAT/LON should be provided", true, null);
      }

      var explanations = sts.getExplanations();
      var feedback = "";
      var feedbackCountString = String.valueOf(explanations.size());
      ppFeedBackCounter.increment(feedbackCountString);
      if (explanations.size() == 0) {
        ++ppMessageCorrectCount;
        feedback = "Perfect Message";
      } else {
        feedback = String.join("\n", explanations);
      }

      var result = new Result(m.from, pair.getLatitude(), pair.getLongitude(), feedback, feedbackCountString, m);
      messageIdResultMap.put(m.messageId, result);

      var outboundMessageFeedback = feedback;
      if (explanations.size() > 0) {
        outboundMessageFeedback += "\n\n" + STD_DISCLAIMER;
      }

      var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
          outboundMessageSubject + " " + m.messageId, outboundMessageFeedback, null);
      outboundMessageList.add(outboundMessage);
    } // end loop over for
    logger.info("field validation:\n" + sts.validate());

    var sb = new StringBuilder();
    var N = ppCount;
    sb.append("\n\nETO 2024-02-15 aggregate results:\n");
    sb.append("Humanitarian Needs participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));

    var it = sts.iterator();
    while (it.hasNext()) {
      var key = it.next();
      if (sts.hasContent(key)) {
        sb.append(sts.format(key));
      }
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));
    sb.append(formatCounter("H5 Address", ppAddressCounter));
    sb.append(formatCounter("Versions", ppVersionCounter));

    logger.info(sb.toString());

    if (badLocationMessageIds.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationMessageIds.size() + " messages: "
              + String.join(",", badLocationMessageIds));
      var newLocations = LocationUtils.jitter(badLocationMessageIds.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationMessageIds.size(); ++i) {
        var messageId = badLocationMessageIds.get(i);
        var result = (Result) messageIdResultMap.get(messageId);
        var newLocation = newLocations.get(i);
        var newResult = Result.updateMapLocation(result, newLocation);
        messageIdResultMap.put(messageId, newResult);
      }
    }

    var results = new ArrayList<>(messageIdResultMap.values());
    WriteProcessor.writeTable(results, Path.of(outputPathName, "humanitarian-with-feedback.csv"));

    if (doOutboundMessaging) {
      var service = new OutboundMessageService(cm);
      outboundMessageList = service.sendAll(outboundMessageList);
      writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    }
  }

}
