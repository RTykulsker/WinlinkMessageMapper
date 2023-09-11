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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.counter.ICounter;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;

/**
 * New (version 5) Winlink Check In form
 *
 * @author bobt
 *
 */
public class ETO_2023_10_05 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2023_10_05.class);

  public static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

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
    var ppBeforeExercise = 0;
    var ppAfterExercise = 0;

    var ppVersionCountOk = 0;

    var ppFeedBackCounter = new Counter();
    var ppVersionCounter = new Counter();
    var ppTypeCounter = new Counter();
    var ppServiceCounter = new Counter();
    var ppBandCounter = new Counter();
    var ppSessionCounter = new Counter();

    ffm.add("org", new FormField("Agency/Group name", "EmComm Training WLT"));

    var results = new ArrayList<IWritableTable>();
    for (var message : mm.getMessagesForType(MessageType.CHECK_IN)) {
      CheckInMessage m = (CheckInMessage) message;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.debug("messageId: " + m.messageId + ", from: " + m.from);
      }

      ++ppCount;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      ffm.test("org", m.organization);

      ppTypeCounter.increment(m.status);
      ppServiceCounter.increment(m.service);
      ppBandCounter.increment(m.band);
      ppSessionCounter.increment(m.mode);

      // exercise window
      {
        var beginExerciseWindow = LocalDateTime.of(2023, 10, 5, 0, 0);

        var messageDateTime = message.msgDateTime;
        if (messageDateTime.isBefore(beginExerciseWindow)) {
          explanations
              .add("!!!message sent (" + messageDateTime.format(DT_FORMATTER) + ") before exercise window opened");
          ++ppBeforeExercise;
        }

        var endExerciseWindow = LocalDateTime.of(2023, 10, 6, 15, 0);
        if (messageDateTime.isAfter(endExerciseWindow)) {
          explanations
              .add("!!!message sent (" + messageDateTime.format(DT_FORMATTER) + ") after exercise window closed");
          ++ppAfterExercise;
        }
      }

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
    } // end loop over Check In messages

    var sb = new StringBuilder();
    var N = ppCount;

    sb.append("\n\nETO 2023-10-05 aggregate results:\n");
    sb.append("Winlink Check In participants: " + N + "\n");
    sb.append(formatPP("Correct Messages", ppMessageCorrectCount, false, N));
    sb.append(formatPP("NO Explicit Bad Locations", ppExplicitBadLocationCounter, true, N));
    sb.append(formatPP("NO Missing or Invalid Locations", ppMissingLocationCounter, true, N));
    sb.append(formatPP("Before exercise window opened", ppBeforeExercise, true, N));
    sb.append(formatPP("After exercise window closed", ppAfterExercise, true, N));
    sb.append(formatPP("Version >= 5", ppVersionCountOk, false, N));

    for (var key : ffm.keySet()) {
      sb.append(formatField(key, false, N));
    }

    sb.append("\n-------------------Histograms---------------------\n");
    sb.append(formatCounter("Feedback items", ppFeedBackCounter));
    sb.append(formatCounter("Version", ppVersionCounter));
    sb.append(formatCounter("Type", ppTypeCounter));
    sb.append(formatCounter("Service", ppServiceCounter));
    sb.append(formatCounter("Band", ppBandCounter));
    sb.append(formatCounter("Session", ppSessionCounter));

    logger.info(sb.toString());

    writeTable("check-in-with-feedback.csv", results);
  }

  private String formatField(String key, boolean invert, int N) {
    var field = ffm.get(key);
    var value = invert ? N - field.count : field.count;
    return (value == N) ? "" : formatPP("  " + field.label, value, N);
  }

  private String formatPP(String label, int count, boolean invert, int N) {
    var value = invert ? N - count : count;
    return formatPP("  " + label, value, N);
  }

  @SuppressWarnings("unused")
  private String formatCounter(FormFieldManager ffm, String key) {
    var field = ffm.get(key);
    return "\n" + field.label + ":\n" + formatCounter(field.counter.getDescendingCountIterator(), "value", "count");
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private String formatCounter(String label, ICounter counter) {
    return ("\n" + label + ":\n" + formatCounter(counter.getDescendingCountIterator(), "value", "count"));
  }

}