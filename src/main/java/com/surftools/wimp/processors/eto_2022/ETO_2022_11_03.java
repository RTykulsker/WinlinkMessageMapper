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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.WxSevereMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 *
 * Send Severe WX Message with text and image
 *
 * @author bobt
 *
 */
public class ETO_2022_11_03 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_11_03.class);

  private Path imageAllPath;
  private List<Path> imageGoodPaths; // list of paths to link when images is "good"
  private List<Path> imageBadPaths; // lists of paths to link when image is "bad"

  // hoist here to share between grade)_ and getPostProcessReport()
  private List<String> missingValues = new ArrayList<>();
  private int points = 0;
  private int imageMaxSize;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    imageMaxSize = cm.getAsInt(Key.IMAGE_MAX_SIZE, 6000);

    FileUtils.deleteDirectory(Path.of(outputPathName, "image"));

    imageAllPath = Path.of(outputPathName, "image", "all");
    FileUtils.createDirectory(imageAllPath);

    var imageGoodPath = Path.of(outputPathName, "image", "rightSized");
    imageGoodPaths = List.of(imageGoodPath);
    FileUtils.createDirectory(imageGoodPath);

    var imageBadPath = Path.of(outputPathName, "image", "tooBig");
    imageBadPaths = List.of(imageBadPath);
    FileUtils.createDirectory(imageBadPath);
  }

  /**
   * Grading – proposed by Bob, approved by Lyn
   *
   * (almost)No field left blank (except comments)
   *
   * Non-observables (Phone, email, City, State, County): 5 points each
   *
   * Observables (flood, hail, wind speed, winter precit, snow, freezing rain, heavy rain, time period): 5 points each
   *
   * Attached image file: 10 points
   *
   * Right sized image file: 15 points
   *
   *
   */
  @SuppressWarnings("unchecked")
  @Override
  public void process() {

    // for post processing
    int ppCount = 0;

    int ppImagePresentOk = 0;
    int ppImageSizeOk = 0;
    int ppImageSizeTooBig = 0;

    // non-observables
    int ppPhoneOk = 0;
    int ppEmailOk = 0;
    int ppCityOk = 0;
    int ppStateOk = 0;
    int ppCountyOk = 0;

    // observables(flood, hail, wind speed, winter precip, snow, freezing rain, heavy rain, time period): 5 points each
    int ppFloodOk = 0;
    int ppHailSizeOk = 0;
    int ppwindSpeedOk = 0;
    int ppTornadoOk = 0;
    int ppWindDamageOk = 0;
    int ppPrecipitationOk = 0;
    int ppSnowOk = 0;
    int ppFreezingRainOk = 0;
    int ppRainOk = 0;
    int ppRainPeriodOk = 0;

    var pointsCounter = new Counter();
    var results = new ArrayList<IWritableTable>();
    for (var message : mm.getMessagesForType(MessageType.WX_SEVERE)) {

      WxSevereMessage m = (WxSevereMessage) message;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.info("ETO_2022_11_03: " + m);
      }

      ++ppCount;

      // instance variables
      points = 0;
      missingValues.clear();

      // Non-observables (Phone, email, City, State, County): 5 points each
      ppPhoneOk += checkMissing(m.contactPhone, "Phone", 5);
      ppEmailOk += checkMissing(m.contactEmail, "Email", 5);
      ppCityOk += checkMissing(m.city, "City", 5);
      ppStateOk += checkMissing(m.region, "State/Province/Region", 5);
      ppCountyOk += checkMissing(m.county, "County", 5);

      // Observables
      ppFloodOk += checkMissing(m.flood, "Flood", 5);
      ppHailSizeOk += checkMissing(m.hailSize, "Hail Size", 5);
      ppwindSpeedOk += checkMissing(m.windSpeed, "High Wind Speed", 5);
      ppTornadoOk += checkMissing(m.tornado, "Tornado / Funnel Cloud", 5);
      ppWindDamageOk += checkMissing(m.windDamage, "Wind Damage", 5);
      ppPrecipitationOk += checkMissing(m.precipitation, "Winter Precipitation", 5);
      ppSnowOk += checkMissing(m.snow, "Snow", 5);
      ppFreezingRainOk += checkMissing(m.freezingRain, "Freezing Rain", 5);
      ppRainOk += checkMissing(m.rain, "Heavy Rain", 5);
      ppRainPeriodOk += checkMissing(m.rainPeriod, "Time Period", 5);

      var explanations = new ArrayList<String>();
      if (missingValues.size() > 0) {
        explanations.add("missing values: " + String.join(",", missingValues));
      }

      var imageFileName = getFirstImageFile(m);
      if (imageFileName != null) {
        var bytes = m.attachments.get(imageFileName);
        ++ppImagePresentOk;
        points += 10;

        if (bytes.length <= imageMaxSize) {
          ++ppImageSizeOk;
          points += 15;
          writeContent(bytes, imageFileName, imageAllPath, imageGoodPaths);
        } else {
          ++ppImageSizeTooBig;
          explanations.add("image size (" + bytes.length + ") larger than " + imageMaxSize + " bytes");
          writeContent(bytes, imageFileName, imageAllPath, imageBadPaths);
        }
      } else {
        explanations.add("no image attachment found");
      }

      points = Math.min(100, points);
      points = Math.max(0, points);
      pointsCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over messages

    var sb = new StringBuilder();

    sb.append("\nETO-2022-11-03 Grading Report: graded " + ppCount + " Severe Weather messages\n");

    sb.append(formatPP("Phone Number value present", ppPhoneOk, ppCount));
    sb.append(formatPP("Email Address value present", ppEmailOk, ppCount));
    sb.append(formatPP("City value present", ppCityOk, ppCount));
    sb.append(formatPP("State/Province/Region value present", ppStateOk, ppCount));
    sb.append(formatPP("County value present", ppCountyOk, ppCount));

    sb.append(formatPP("Flood value present", ppFloodOk, ppCount));
    sb.append(formatPP("Hail Size value present", ppHailSizeOk, ppCount));
    sb.append(formatPP("High Wind Speed value present", ppwindSpeedOk, ppCount));
    sb.append(formatPP("Tornado / Funnel Cloud value present", ppTornadoOk, ppCount));
    sb.append(formatPP("Wind Damage value present", ppWindDamageOk, ppCount));
    sb.append(formatPP("Winter Precipitation value present", ppPrecipitationOk, ppCount));
    sb.append(formatPP("Snow value present", ppSnowOk, ppCount));
    sb.append(formatPP("Freezing Rain value present", ppFreezingRainOk, ppCount));
    sb.append(formatPP("Heavy Rain value present", ppRainOk, ppCount));
    sb.append(formatPP("Time Period value present", ppRainPeriodOk, ppCount));

    sb.append(formatPP("Image attached", ppImagePresentOk, ppCount));
    sb.append(formatPP("Image size <= " + imageMaxSize + " bytes", ppImageSizeOk, ppCount));
    sb.append(formatPP("Image size > " + imageMaxSize + " bytes", ppImageSizeTooBig, ppCount));

    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());
    writeTable("graded-wx_severe.csv", results);
  }

  /**
   * convenience method with SIDE EFFECTS
   *
   * @param value
   * @param missingLabel
   * @param pointsToBeAdded
   * @return 1 if value is not missing, 0 if it is
   *
   */
  private int checkMissing(String value, String missingLabel, int pointsToBeAdded) {
    if (value == null || value.isBlank()) {
      missingValues.add(missingLabel);
      return 0;
    } else {
      points += pointsToBeAdded;
      return 1;
    }
  }

}