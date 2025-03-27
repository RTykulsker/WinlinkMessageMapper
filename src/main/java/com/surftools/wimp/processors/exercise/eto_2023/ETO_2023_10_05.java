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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessageService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * New (version 5) Winlink Check In form
 *
 * @author bobt
 *
 */
public class ETO_2023_10_05 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2023_10_05.class);

  // public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  // public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  // public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private FormFieldManager ffm = new FormFieldManager();

  static record Result(String feedback, String feedbackCountString, CheckInMessage message) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(CheckInMessage.getStaticHeaders().length + 2);
      Collections.addAll(resultList, CheckInMessage.getStaticHeaders());
      Collections.addAll(resultList, new String[] { "Feedback Count", "Feedback", });
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var resultList = new ArrayList<String>(CheckInMessage.getStaticHeaders().length + 2);
      Collections.addAll(resultList, message.getValues());
      Collections.addAll(resultList, new String[] { feedbackCountString, feedback, });
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
    var ppMessageCorrectCount = 0;
    var ppExplicitBadLocationCounter = 0;
    var ppMissingLocationCounter = 0;

    var ppVersionCountOk = 0;

    var ppFeedBackCounter = new Counter();
    var ppVersionCounter = new Counter();

    ffm.add("org", new FormField("Agency/Group name", "EmComm Training Organization"));
    ffm.add("addresses", new FormField(FFType.CONTAINS, "To and/or CC addresses", "ETO-BK"));
    ffm.add("dateTime", new FormField(FFType.DATE_TIME, "Date/Time", "yyyy-MM-dd HH:mm:ss"));
    ffm.add("type", new FormField("Session Type", "EXERCISE"));
    ffm.add("band", new FormField(FFType.REQUIRED, "Session Band"));
    ffm.add("service", new FormField("Session Service", "AMATEUR"));
    ffm.add("mode", new FormField(FFType.REQUIRED, "Session/Mode"));
    ffm.add("location", new FormField(FFType.REQUIRED, "Location"));
    ffm.add("comments", new FormField(FFType.LIST, "Comments", "GENERAL,MAPPING-GIS"));

    ffm.add("windowOpen", new FormField(FFType.DATE_TIME_ON_OR_AFTER, "Message sent too early", "2023-10-03 00:00"));
    ffm.add("windowClose", new FormField(FFType.DATE_TIME_ON_OR_BEFORE, "Message sent too late", "2023-10-06 15:00"));

    var results = new ArrayList<IWritableTable>();

    for (var message : mm.getMessagesForType(MessageType.CHECK_IN)) {
      CheckInMessage m = (CheckInMessage) message;
      ++ppCount;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      ffm.test("windowOpen", FormFieldManager.FORMATTER.format(message.msgDateTime));
      ffm.test("windowClose", FormFieldManager.FORMATTER.format(message.msgDateTime));

      ffm.test("org", m.organization);

      var addressList = m.toList + "," + m.ccList;
      ffm.test("addresses", addressList);

      var formDTFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
      if (m.formDateTime != null) {
        ffm.test("dateTime", formDTFormatter.format(m.formDateTime));
      } else {
        logger.warn("### null date/time for call: " + m.from);
        ffm.test("dateTime", "");
      }

      ffm.test("type", m.status);
      ffm.test("service", m.service);
      ffm.test("band", m.band);
      ffm.test("mode", m.mode);

      ffm.test("location", m.formLocation.toString());

      ffm.test("comments", m.comments.toUpperCase());

      // version
      {
        var version = m.version;
        ppVersionCounter.increment(version);
        if (version != null) {
          var versionFields = version.split("\\.");
          try {
            var major = Integer.parseInt(versionFields[0]);
            if (major >= 5) {
              ++ppVersionCountOk;
            } else {
              explanations.add("version(" + version + ") must be > 5.0.0");
            }
          } catch (Exception e) {
            explanations.add("can't parse version: " + version);
          }
        }
      }

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

      var result = new Result(feedback, feedbackCountString, m);

      results.add(result);

      var outboundMessage = new OutboundMessage(outboundMessageSender, m.from,
          outboundMessageSubject + " " + m.messageId, feedback, null);
      outboundMessageList.add(outboundMessage);

    } // end loop over Check In messages

    var sb = new StringBuilder();
    var N = ppCount;

    sb.append("\n\nETO 2023-10-05 aggregate results:\n");
    sb.append("Winlink Check In participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));
    sb.append(formatPP("NO Explicit Bad Locations", ppExplicitBadLocationCounter, true, N));
    sb.append(formatPP("NO Missing or Invalid Locations", ppMissingLocationCounter, true, N));
    sb.append(formatPP("Version >= 5", ppVersionCountOk, false, N));

    for (var key : ffm.keySet()) {
      sb.append(formatField(ffm, key, false, N));
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));
    sb.append(formatCounter("Version", ppVersionCounter));
    sb.append(formatCounter("Type", ffm.get("type").counter));
    sb.append(formatCounter("Service", ffm.get("service").counter));
    sb.append(formatCounter("Band", ffm.get("band").counter));
    sb.append(formatCounter("Session Mode", ffm.get("mode").counter));
    sb.append(formatCounter("Comments", ffm.get("comments").counter));

    logger.info(sb.toString());

    writeTable("check-in-with-feedback.csv", results);
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