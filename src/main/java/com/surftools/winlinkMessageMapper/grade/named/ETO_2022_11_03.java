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

package com.surftools.winlinkMessageMapper.grade.named;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.WxSevereMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;

/**
 *
 * Send Severe WX Message with text and image
 *
 * @author bobt
 *
 */
public class ETO_2022_11_03 extends DefaultGrader {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_11_03.class);

  private final int MAX_ATTACHMENT_SIZE = 5_120;

  private Path outputPath;

  private Path imageAllPath;
  private Path imagePassPath;
  private Path imageBadSizePath;

  // for post processing

  private int ppImagePresentOk;
  private int ppImageSizeOk;

  // non-observables
  private int ppPhoneOk;
  private int ppEmailOk;
  private int ppCityOk;
  private int ppStateOk;
  private int ppCountyOk;

  // observables(flood, hail, wind speed, winter precip, snow, freezing rain, heavy rain, time period): 5 points each
  private int ppFloodOk;
  private int ppHailSizeOk;
  private int ppwindSpeedOk;
  private int ppTornadoOk;
  private int ppWindDamageOk;
  private int ppPrecipitationOk;
  private int ppSnowOk;
  private int ppFreezingRainOk;
  private int ppRainOk;
  private int ppRainPeriodOk;

  // hoist here to share between grade)_ and getPostProcessReport()
  private List<String> missingValues = new ArrayList<>();
  private int points;

  private Map<Integer, Integer> ppScoreCountMap;

  private Set<String> dumpIds;

  private boolean isInitialized;

  public ETO_2022_11_03() {
    super(logger);

    // don't have a cm when we are constructed;
    isInitialized = false;

    ppScoreCountMap = new HashMap<>();
  }

  /**
   * we don't have access to our IConfigurationManager during construction, hence need to initialize
   */
  private void initialize() {
    outputPath = Path.of(cm.getAsString(Key.PATH), "output", "image");
    FileUtils.deleteDirectory(outputPath);

    imageAllPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "alll"));
    imagePassPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "pass"));
    imageBadSizePath = FileUtils.createDirectory(Path.of(outputPath.toString(), "badSize"));

    isInitialized = true;
  }

  @Override
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
  public GradeResult grade(GradableMessage gm) {
    if (!isInitialized) {
      initialize();
      isInitialized = true;
    }

    if (!(gm instanceof WxSevereMessage)) {
      return null;
    }

    WxSevereMessage m = (WxSevereMessage) gm;

    if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
      logger.info("ETO_2022_11_03 grader: " + m);
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

    var imageFileName = getImageFile(m);
    if (imageFileName != null) {
      var bytes = m.attachments.get(imageFileName);
      ++ppImagePresentOk;
      points += 10;

      var isImageSizeOk = false;
      if (bytes.length <= MAX_ATTACHMENT_SIZE) {
        ++ppImageSizeOk;
        points += 15;
        isImageSizeOk = true;
      } else {
        explanations.add("image size (" + bytes.length + ") larger than " + MAX_ATTACHMENT_SIZE + " bytes");
      }

      writeImage(m, m.from + " -" + imageFileName, bytes, isImageSizeOk);
    } else {
      explanations.add("no image attachment found");
    }

    points = Math.min(100, points);
    points = Math.max(0, points);
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

    var count = ppScoreCountMap.getOrDefault(points, Integer.valueOf(0));
    ++count;
    ppScoreCountMap.put(points, count);

    gm.setIsGraded(true);
    gm.setGrade(grade);
    gm.setExplanation(explanation);

    return new GradeResult(grade, explanation);
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

  private String getImageFile(ExportedMessage m) {
    for (String key : m.attachments.keySet()) {
      try {
        var bytes = m.attachments.get(key);
        var bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
          continue;
        }
        logger.info("image found for call: " + m.from + ", attachment: " + key + ", size:" + bytes.length);
        return key;
      } catch (Exception e) {
        ;
      }
    }
    return null;
  }

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    if (messages == null || messages.size() == 0 || messages.get(0).getMessageType() != MessageType.WX_SEVERE) {
      return null;
    }

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);

    sb.append("\nETO-2022-11-03 Grading Report: graded " + ppCount + " Severe Weather messages\n");

    sb.append(formatPP("Phone Number value present", ppPhoneOk));
    sb.append(formatPP("Email Address value present", ppEmailOk));
    sb.append(formatPP("City value present", ppCityOk));
    sb.append(formatPP("State/Province/Region value present", ppStateOk));
    sb.append(formatPP("County value present", ppCountyOk));

    sb.append(formatPP("Flood value present", ppFloodOk));
    sb.append(formatPP("Hail Size value present", ppHailSizeOk));
    sb.append(formatPP("High Wind Speed value present", ppwindSpeedOk));
    sb.append(formatPP("Tornado / Funnel Cloud value present", ppTornadoOk));
    sb.append(formatPP("Wind Damage value present", ppWindDamageOk));
    sb.append(formatPP("Winter Precipitation value present", ppPrecipitationOk));
    sb.append(formatPP("Snow value present", ppSnowOk));
    sb.append(formatPP("Freezing Rain value present", ppFreezingRainOk));
    sb.append(formatPP("Heavy Rain value present", ppRainOk));
    sb.append(formatPP("Time Period value present", ppRainPeriodOk));

    sb.append(formatPP("Image attached", ppImagePresentOk));
    sb.append(formatPP("Image size <= " + MAX_ATTACHMENT_SIZE + " bytes", ppImageSizeOk));

    var scores = new ArrayList<Integer>(ppScoreCountMap.keySet());
    Collections.sort(scores, Comparator.reverseOrder());
    sb.append("\nscores: \n");
    for (int score : scores) {
      var count = ppScoreCountMap.get(score);
      sb.append(" score: " + score + ", count: " + count + "\n");
    }

    return sb.toString();
  }

  /**
   * write the image file, to the all/ directory
   *
   * write a link to pass, badName and/or badSize depending
   *
   * @param m
   * @param imageFileName
   * @param bytes
   * @param isImageSizeOk
   */
  private void writeImage(ExportedMessage m, String imageFileName, byte[] bytes, boolean isImageSizeOk) {

    try {
      // write the file
      var allImagePath = Path.of(imageAllPath.toString(), imageFileName);
      Files.write(allImagePath, bytes);

      // create the link to pass or fail
      if (isImageSizeOk) {
        var passImagePath = Path.of(imagePassPath.toString(), imageFileName);
        Files.createLink(passImagePath, allImagePath);
      } else {
        var badSizePath = Path.of(imageBadSizePath.toString(), imageFileName);
        Files.createLink(badSizePath, allImagePath);
      }
    } catch (Exception e) {
      logger
          .error("Exception writing image file for call: " + m.from + ", messageId: " + m.messageId + ", "
              + e.getLocalizedMessage());
    }
  }

}