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

package com.surftools.wimp.processors.eto_2022;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.message.WxLocalMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;

/**
 * Processor for 2022-12-01 exercise: one WX Local message (442 lines)
 *
 * @author bobt
 *
 */
public class ETO_2022_12_01 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_12_01.class);

  private Set<String> forecastOfficeSet = new HashSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Path path = Path.of(pathName, "forecastOffices.txt");
    try {
      var lines = Files.readAllLines(path);
      for (var line : lines) {
        forecastOfficeSet.add(line.toUpperCase());
      }
      logger.info("created forecastOffice set with " + forecastOfficeSet.size() + " entries from " + path);
    } catch (Exception e) {
      logger.error("Exception reading file " + path + ", " + e.getLocalizedMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    var ppCount = 0;

    var ppTemperatureOk = 0;
    var ppWindSpeedOk = 0;

    var ppNotesHasTwoLinesOk = 0;
    var ppLine1HasCommaOk = 0;
    var ppLine2YesNoOk = 0;
    var ppMatchForecastOffice = 0;

    var forecastOfficeCounter = new Counter();
    var scoreCounter = new Counter();

    var ppYesCount = 0;
    var ppNoCount = 0;

    var ffm = new FormFieldManager();
    ffm.add("organization", new FormField(FFType.SPECIFIED, "Setup", "Winlink Thursday Exercise", 10));
    ffm.add("location", new FormField(FFType.REQUIRED, "Location", null, 5));
    ffm.add("city", new FormField(FFType.REQUIRED, "City", null, 5));
    ffm.add("state", new FormField(FFType.REQUIRED, "State", null, 5));
    ffm.add("county", new FormField(FFType.REQUIRED, "County", null, 5));

    List<IWritableTable> entries = new ArrayList<>();
    for (var m : mm.getMessagesForType(MessageType.WX_LOCAL)) {
      var from = m.from;
      var wxMessage = (WxLocalMessage) m;
      if (dumpIds.contains(from)) {
        logger.info("dump: " + from);
      }

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();
      ffm.reset(explanations);

      ffm.test("organization", wxMessage.organization);
      ffm.test("location", wxMessage.locationString);
      ffm.test("city", wxMessage.city);
      ffm.test("state", wxMessage.state);
      ffm.test("county", wxMessage.county);

      var temperature = wxMessage.temperature;
      if (temperature == null || temperature.isBlank() || temperature.startsWith("--")) {
        explanations.add("Temperation not provided");
      } else {
        points += 5;
        ++ppTemperatureOk;
      }

      var windspeed = wxMessage.windspeed;
      if (windspeed == null || windspeed.isBlank() || windspeed.startsWith("--")) {
        explanations.add("Windspeed not provided");
      } else {
        points += 5;
        ++ppWindSpeedOk;
      }

      var comments = wxMessage.comments;
      if (comments == null || comments.isBlank()) {
        explanations.add("Notes not provided");
      } else {
        var lines = comments.split("\n");
        if (lines.length != 2) {
          explanations.add("Didn't find exactly two comment lines");
        } else {
          points += 10;
          ++ppNotesHasTwoLinesOk;
        }

        var forecastOffice = lines[0].toUpperCase();
        if (!forecastOffice.contains(",")) {
          explanations.add("line 1 of comments doesn't look like forecast office");
        } else {
          points += 25;
          ++ppLine1HasCommaOk;

          forecastOfficeCounter.increment(forecastOffice);

          if (forecastOfficeSet.contains(forecastOffice)) {
            ++ppMatchForecastOffice;
            points += 10;
            explanations.add("Bonus for matching official forecast office");
          }
        }

        if (lines.length >= 2) {
          var yesno = lines[1];
          if (yesno == null) {
            explanations.add("line 2 of comments not YES or NO");
          } else {
            yesno = yesno.toUpperCase();
            if (yesno.equals("YES") || yesno.equals("NO")) {
              points += 25;
              ++ppLine2YesNoOk;
              if (yesno.equals("YES")) {
                ++ppYesCount;
              } else {
                ++ppNoCount;
              }
            } else {
              explanations.add("line 2 of comments not YES or NO");
            }
          }
        }
      }

      points += ffm.getPoints();
      var explanation = (points == 100 && explanations.size() == 0) //
          ? "Perfect Score!"
          : (points == 110 && explanations.size() == 1) ? "Perfect score, plus matched forecast office"
              : String.join("\n", explanations);

      points = Math.min(100, points);
      points = Math.max(0, points);
      var grade = String.valueOf(points);
      scoreCounter.increment(points);

      var entry = new GradedResult(wxMessage, grade, explanation);
      entries.add(entry);

    } // end loop over messages

    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-12-01 aggregate results:\n");
    sb.append("total participants: " + ppCount + "\n");

    sb.append("\nScoring criteria\n");
    for (var key : ffm.keySet()) {
      var af = ffm.get(key);
      sb.append("  " + formatPP(af.label, af.count, ppCount));
    }

    sb.append(formatPP("  Temperature OK", ppTemperatureOk, ppCount));
    sb.append(formatPP("  Windspeed OK", ppWindSpeedOk, ppCount));
    sb.append(formatPP("  Notes has two lines", ppNotesHasTwoLinesOk, ppCount));
    sb.append(formatPP("  Notes line 1 looks like forecast Office", ppLine1HasCommaOk, ppCount));
    sb.append(formatPP("  Notes line 2 YES or NO", ppLine2YesNoOk, ppCount));
    sb.append(formatPP("    Yes count: ", ppYesCount, ppLine2YesNoOk));
    sb.append(formatPP("    No  count: ", ppNoCount, ppLine2YesNoOk));
    sb.append(formatPP("  BONUS: exact match for US forecast office", ppMatchForecastOffice, ppCount));

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));
    sb
        .append("\nForecast Offices: \n"
            + formatCounter(forecastOfficeCounter.getDescendingCountIterator(), "response", "count"));

    logger.info(sb.toString());

    writeTable("graded-wx_local.csv", entries);
  }

}
