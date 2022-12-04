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

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.WxLocalMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * Aggregator for 2022-12-01 DRILL: one WX Local message
 *
 * @author bobt
 *
 */
public class ETO_2022_12_01_Aggregator extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_12_01_Aggregator.class);

  private static final String REQUIRED_HEADER_TEXT = "Winlink Thursday Exercise";

  static record Entry(WxLocalMessage wx, String grade, String explanation) {

    public static String[] getHeaders() {
      var list = new ArrayList<String>();
      Collections.addAll(list, WxLocalMessage.getStaticHeaders());
      Collections.addAll(list, new String[] { "Grade", "Explanation" });
      var array = new String[list.size()];
      list.toArray(array);
      return array;
    }

    public String[] getValues() {
      var list = new ArrayList<String>();
      Collections.addAll(list, wx.getValues());
      Collections.addAll(list, new String[] { grade, explanation });
      var array = new String[list.size()];
      list.toArray(array);
      return array;
    }
  };

  private List<Entry> entries = new ArrayList<>();
  private Set<String> forecastOfficeSet = new HashSet<>();

  public ETO_2022_12_01_Aggregator() {
    super(logger);
  }

  public void initialize() {
    makeForecastOfficeSet();
  }

  private void makeForecastOfficeSet() {
    final var filename = "forecastOffices.txt";
    Path path = Path.of(cm.getAsString(Key.PATH), filename);
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

  @Override
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    initialize();

    super.aggregate(messageMap);

    var ppCount = 0;

    var ppSetupOk = 0;
    var ppLocationOk = 0;
    var ppCityOk = 0;
    var ppStateOk = 0;
    var ppCountyOk = 0;
    var ppTemperatureOk = 0;
    var ppWindSpeedOk = 0;

    var ppNotesHasTwoLinesOk = 0;
    var ppLine1HasCommaOk = 0;
    var ppLine2YesNoOk = 0;

    var ppIsUSCount = 0;
    var ppMatchForecastOffice = 0;

    var ppMesssagesWithValidLocation = 0;
    var ppSumLocationDifferenceMeters = 0;

    var ppScoreCountMap = new HashMap<Integer, Integer>();
    var ppForecastOfficeCounter = new Counter();

    var ppYesCount = 0;
    var ppNoCount = 0;

    for (var m : aggregateMessages) {
      var from = m.from();
      if (dumpIds.contains(from)) {
        logger.info("dump: " + from);
      }

      var map = fromMessageMap.get(from);
      var wxMessages = map.get(MessageType.WX_LOCAL);
      if (wxMessages == null || wxMessages.size() == 0) {
        continue;
      }
      var wxMessage = (WxLocalMessage) wxMessages.iterator().next();
      if (wxMessage == null) {
        logger.info("skipping messages from " + from + ", since wx_local");
        continue;
      }

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();

      var organization = wxMessage.organization;
      if (organization == null || !organization.equalsIgnoreCase(REQUIRED_HEADER_TEXT)) {
        explanations.add("Setup not equal to '" + REQUIRED_HEADER_TEXT + "'");
      } else {
        points += 10;
        ++ppSetupOk;
      }

      var location = wxMessage.locationString;
      if (location == null || location.isBlank()) {
        explanations.add("Location not provided");
      } else {
        points += 5;
        ++ppLocationOk;
      }

      var city = wxMessage.city;
      if (city == null || city.isBlank()) {
        explanations.add("City not provided");
      } else {
        points += 5;
        ++ppCityOk;
      }

      var state = wxMessage.state;
      if (state == null || state.isBlank()) {
        explanations.add("State not provided");
      } else {
        points += 5;
        ++ppStateOk;
      }

      var county = wxMessage.county;
      if (county == null || county.isBlank()) {
        explanations.add("County not provided");
      } else {
        points += 5;
        ++ppCountyOk;
      }

      var latitude = wxMessage.latitude;
      var longitude = wxMessage.longitude;
      var latlon = new LatLongPair(latitude, longitude);
      if (latlon.isValid()) {
        var messageLocation = wxMessage.location;
        if (messageLocation != null && messageLocation.isValid()) {
          var distanceMeters = LocationUtils.computeDistanceMeters(latlon, wxMessage.location);
          ++ppMesssagesWithValidLocation;
          ppSumLocationDifferenceMeters += distanceMeters;

          if (distanceMeters > 1000) {
            logger
                .debug("sender: " + wxMessage.from + ", message loc: " + messageLocation + ", form loc: " + latlon
                    + ", distance: " + distanceMeters + " meters");
          }
        }
      }

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

          ppForecastOfficeCounter.increment(forecastOffice);

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

      // informational, not graded
      var isUsCallSign = isUsCallSign(from);
      if (isUsCallSign) {
        ++ppIsUSCount;
      }

      var explanation = (points == 100 && explanations.size() == 0) //
          ? "Perfect Score!"
          : (points == 110 && explanations.size() == 1) ? "Perfect score, plus matched forecast office"
              : String.join("\n", explanations);

      points = Math.min(100, points);
      points = Math.max(0, points);
      var grade = String.valueOf(points);

      var entry = new Entry(wxMessage, grade, explanation);
      entries.add(entry);

      var scoreCount = ppScoreCountMap.getOrDefault(points, Integer.valueOf(0));
      ++scoreCount;
      ppScoreCountMap.put(points, scoreCount);

    } // end loop over messages

    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-12-01 aggregate results:\n");
    sb.append("total participants: " + ppCount + "\n");

    sb.append("\nScoring criteria\n");
    sb.append(format("  Setup OK", ppSetupOk, ppCount));
    sb.append(format("  Location OK", ppLocationOk, ppCount));
    sb.append(format("  City OK", ppCityOk, ppCount));
    sb.append(format("  State OK", ppStateOk, ppCount));
    sb.append(format("  County OK", ppCountyOk, ppCount));
    sb.append(format("  Temperature OK", ppTemperatureOk, ppCount));
    sb.append(format("  Windspeed OK", ppWindSpeedOk, ppCount));

    sb.append(format("  Notes has two lines", ppNotesHasTwoLinesOk, ppCount));
    sb.append(format("  Notes line 1 looks like forecast Office", ppLine1HasCommaOk, ppCount));
    sb.append(format("  Notes line 2 YES or NO", ppLine2YesNoOk, ppCount));

    sb.append(format("    Yes count: ", ppYesCount, ppLine2YesNoOk));
    sb.append(format("    No  count: ", ppNoCount, ppLine2YesNoOk));

    sb.append("\nNon-scoring criteria\n");
    var avgDistanceMeters = (double) ppSumLocationDifferenceMeters / (double) ppMesssagesWithValidLocation;
    sb.append(format("  messages with valid message location", ppMesssagesWithValidLocation, ppCount));
    sb.append("  average difference between message and form location: " + avgDistanceMeters + " meters");

    sb.append(format("  message from US location", ppIsUSCount, ppCount));
    sb.append(format("  exact match for US forecast office", ppMatchForecastOffice, ppCount));

    var scores = new ArrayList<Integer>(ppScoreCountMap.keySet());
    Collections.sort(scores, Comparator.reverseOrder());
    sb.append("\nscores: \n");
    for (int score : scores) {
      var count = ppScoreCountMap.get(score);
      sb.append(" score: " + score + ", count: " + count + "\n");
    }

    sb.append("\nCounts by (message) forecast Office\n");
    var it = ppForecastOfficeCounter.getDescendingCountIterator();
    while (it.hasNext()) {
      var entry = it.next();
      var count = entry.getValue();
      if (count == 0) {
        break;
      }
      sb.append("  office: " + entry.getKey() + ", count:" + entry.getValue() + "\n");
    }

    logger.info(sb.toString());

  }

  private boolean isUsCallSign(String call) {
    if (call == null || call.isBlank()) {
      return false;
    }

    final var US_CALL_REGEX = "[AKNWaknw][a-zA-Z]{0,2}[0-9][a-zA-Z]{1,3}";
    var isUs = call.matches(US_CALL_REGEX);

    return isUs;
  }

  private String formatPercent(int numerator, int denominator) {
    if (denominator == 0) {
      return "";
    }

    double percent = (100d * numerator) / denominator;
    return " (" + String.format("%.2f", percent) + "%)";
  }

  private String format(String label, int numerator, int denominator) {
    return label + ": " + numerator + " " + formatPercent(numerator, denominator) + "\n";
  }

  @Override
  public void output(String pathName) {
    output(pathName, "aggregate-wx_local.csv", entries);
  }

  private void output(String pathName, String fileName, List<Entry> entries) {
    Path outputPath = Path.of(pathName, "output", fileName);
    FileUtils.makeDirIfNeeded(outputPath.toString());

    var messageCount = 0;
    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(Entry.getHeaders());

      if (entries.size() > 0) {
        entries.sort((Entry s1, Entry s2) -> s1.wx.from.compareTo(s2.wx.from));
        for (Entry e : entries) {
          if (e != null) {
            var values = e.getValues();
            if (values != null) {
              writer.writeNext(e.getValues());
              ++messageCount;
            }
          } else {
            continue;
          }
        }
      }

      writer.close();
      logger.info("wrote " + messageCount + " aggregate messages to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  @Override
  public String[] getHeaders() {
    return null;
  }

  @Override
  public String[] getValues(AggregateMessage message) {
    return null;
  }

}
