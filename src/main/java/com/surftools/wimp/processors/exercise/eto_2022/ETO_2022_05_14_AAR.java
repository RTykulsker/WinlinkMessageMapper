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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.Ics213ReplyMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.SummaryProcessor.ParticipantHistory;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.processors.std.WriteProcessor;

/**
 * After Action Report on Operation Ashfall; we sent them an ICS-213; then replied
 *
 * @author bobt
 *
 */
public class ETO_2022_05_14_AAR extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_05_14_AAR.class);

  public record ParticipantSummary(String call, String latitude, String longitude) {
    public static ParticipantSummary make(String[] fields) {
      return new ParticipantSummary(fields[0], fields[3], fields[4]);
    }
  }

  private Map<String, LatLongPair> callLocationMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var summaryPath = Path.of(pathName, "participantSummary.csv");
    var fieldsList = ReadProcessor.readCsvFileIntoFieldsArray(summaryPath);
    for (var fields : fieldsList) {
      var ph = ParticipantHistory.make(fields);
      callLocationMap.put(ph.call(), new LatLongPair(ph.latitude(), ph.longitude()));
    }
    logger.info("read " + callLocationMap.size() + " summary records from: " + summaryPath);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    int ppCount = 0;
    /**
     * might as well score it: 30 points for a reply; 30 points for 8 lines of reply, 30 points for numbers starting
     * each line; 10 point for valid location
     */
    int ppReplyLocationValid = 0;
    int ppSummaryLocationValid = 0;
    int ppReplyPresent = 0;
    int ppNumberOfLinesOk = 0;
    int ppAll8LinesStartWithNumber = 0;

    var results = new ArrayList<IWritableTable>();
    var pointsCounter = new Counter();
    for (var message : mm.getMessagesForType(MessageType.ICS_213_REPLY)) {

      var m = (Ics213ReplyMessage) message;
      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();

      var reply = m.reply;
      if (reply != null && !reply.isBlank()) {
        ++ppReplyPresent;
        points += 30;

        var lines = reply.split("\n");
        if (lines.length == 8) {
          points += 30;
          ++ppNumberOfLinesOk;

          var allLinesStartwithNumber = true;
          for (var line : lines) {
            var fields = line.split(" ");
            try {
              Double.parseDouble(fields[0]);
            } catch (Exception e) {
              allLinesStartwithNumber = false;
            }
          }

          if (allLinesStartwithNumber) {
            points += 30;
            ++ppAll8LinesStartWithNumber;
          } else {
            explanations.add("not all reply lines started with a number");
          }
        } else {
          explanations.add("Expected 8 lines of reply, got " + lines.length + " lines");
        }
      } else {
        explanations.add("no message reply");
      }

      if (m.mapLocation == null || !m.mapLocation.isValid()) { // and it shouldn't be
        var location = callLocationMap.get(m.from);
        m.mapLocation = location;
        if (location.isValid()) {
          ++ppSummaryLocationValid;
          points += 10;
        } else {
          explanations.add("missing/invalid historic location");
        }
      } else {
        explanations.add("reply location specified when it shouldn't be");
        ++ppReplyLocationValid;
      }

      points = Math.min(100, points);
      points = Math.max(0, points);
      var grade = String.valueOf(points);
      pointsCounter.increment(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    }
    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-05-14 After Action Report: " + ppCount + " unique replies\n");

    sb.append(formatPP("Reply Location OK", ppReplyLocationValid, ppCount));
    sb.append(formatPP("Summary Location OK", ppSummaryLocationValid, ppCount));
    sb.append(formatPP("Reply Present OK", ppReplyPresent, ppCount));
    sb.append(formatPP("Reply #Lines OK", ppNumberOfLinesOk, ppCount));
    sb.append(formatPP("Reply lines all start with numbers", ppAll8LinesStartWithNumber, ppCount));

    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));
    logger.info(sb.toString());

    WriteProcessor.writeTable(results, Path.of(outputPathName, "graded-ics_213_reply.csv"));
  }
}