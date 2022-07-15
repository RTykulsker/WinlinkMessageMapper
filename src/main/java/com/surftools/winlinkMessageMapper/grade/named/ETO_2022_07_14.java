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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.WxSevereMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

/**
 * Severe Weather, with cropped and resized image
 *
 * @author bobt
 *
 */
public class ETO_2022_07_14 implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_07_14.class);

  private final String OUTPUT_DIRECTORY = "/home/bobt/Documents/eto/2022-07-14/output/images";
  private final int MAX_ATTACHMENT_SIZE = 6000;

  private final boolean doAnnotation = true;

  private final Path outputPath;
  private final Path allPath;
  private final Path rightSizePath;
  private final Path tooBigPath;
  private Path annotatedAllPath;
  private Path annotatedRightSizePath;
  private Path annotatedTooBigPath;

  // for post processing
  private int ppCount;
  private int ppImagePresentOk;
  private int ppImageSizeOk;
  private Map<String, Integer> ppMissingValueCountMap = new HashMap<>();

  private Set<String> dumpIds;

  public ETO_2022_07_14() throws Exception {
    dumpIds = new HashSet<>();

    outputPath = Path.of(OUTPUT_DIRECTORY);
    deleteDirectory(outputPath);
    allPath = createDirectory(Path.of(outputPath.toString(), "all"));
    rightSizePath = createDirectory(Path.of(outputPath.toString(), "rightSized"));
    tooBigPath = createDirectory(Path.of(outputPath.toString(), "tooBig"));

    if (doAnnotation) {
      annotatedAllPath = createDirectory(Path.of(outputPath.toString(), "annotated", "all"));
      annotatedRightSizePath = createDirectory(Path.of(outputPath.toString(), "annotated", "rightSized"));
      annotatedTooBigPath = createDirectory(Path.of(outputPath.toString(), "annotated", "tooBig"));
    }

    logger.info("Max Attachment Size: " + MAX_ATTACHMENT_SIZE);
    logger.info("Output Image Path: " + OUTPUT_DIRECTORY);
    logger.info("Do Annotations: " + doAnnotation);
  }

  @Override
  public GradeResult grade(GradableMessage gm) {
    if (!(gm instanceof WxSevereMessage)) {
      return null;
    }

    WxSevereMessage m = (WxSevereMessage) gm;

    if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
      // logger.info("ETO_2022_06_09 grader: " + m);
    }

    ++ppCount;
    var points = 0;
    var explanations = new ArrayList<String>();

    // do we have an attachment
    BufferedImage bufferedImage = null;
    String imageFileName = null;
    byte[] bytes = null;
    for (String key : m.attachments.keySet()) {
      try {
        bytes = m.attachments.get(key);
        bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
          continue;
        }
        logger.info("image found for attachment: " + key + ", size:" + bytes.length);
        imageFileName = key;
        break;
      } catch (Exception e) {
        ;
      }
    } // end for over attachments

    if (bufferedImage != null) {
      ++ppImagePresentOk;
      points += 40;

      if (bytes.length <= MAX_ATTACHMENT_SIZE) {
        ++ppImageSizeOk;
        points += 40;
      } else {
        explanations.add("image attachment size too large");
      }
      writeImageFile(bytes, m.from, imageFileName);
    } else {
      explanations.add("no image attachment found");
    }

    /**
     * severe weather -- must have values
     */
    points += gradeRequireValue(m.flood, "Flood", 2, explanations);
    points += gradeRequireValue(m.hailSize, "Hail size", 2, explanations);
    points += gradeRequireValue(m.windSpeed, "High Wind Speed", 2, explanations);
    points += gradeRequireValue(m.tornado, "Tornado/Funnel Cloud", 2, explanations);
    points += gradeRequireValue(m.windDamage, "Wind Damage", 2, explanations);
    points += gradeRequireValue(m.precipitation, "Winter Precipitation", 2, explanations);
    points += gradeRequireValue(m.snow, "Snow", 2, explanations);
    points += gradeRequireValue(m.freezingRain, "Freezing Rain", 2, explanations);
    points += gradeRequireValue(m.rain, "Heavy Rain", 2, explanations);
    points = (points == 98) ? 100 : points;

    points = Math.min(100, points);
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

    gm.setIsGraded(true);
    gm.setGrade(grade);
    gm.setExplanation(explanation);

    return new GradeResult(grade, explanation);
  }

  private int gradeRequireValue(String value, String label, int points, ArrayList<String> explanations) {
    if (value == null || value.isBlank()) {
      explanations.add(label + " must be specified");
      var count = ppMissingValueCountMap.getOrDefault(label, Integer.valueOf(0));
      ++count;
      ppMissingValueCountMap.put(label, count);
      return 0;
    } else {
      return points;
    }

  }

  private String formatPercent(Double d) {
    if (d == null) {
      return "";
    }

    return String.format("%.2f", 100d * d) + "%";
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
    sb.append("\nETO-2022-07-14 Grading Report: graded " + ppCount + " Severe Weather messages\n");
    sb.append(formatPP("image attached", ppImagePresentOk));
    sb.append(formatPP("image size", ppImageSizeOk));
    sb.append("\n");

    if (ppMissingValueCountMap.size() > 0) {
      sb.append("Missing value count:\n");
      for (String key : ppMissingValueCountMap.keySet()) {
        int value = ppCount - ppMissingValueCountMap.get(key);
        sb.append(formatPP(key, value));
      }
    }

    return sb.toString();
  }

  /**
   * write the image file, to the all/ directory
   *
   * write a link to either the rightSized or tooBig directory
   *
   * (optionally) write a file to the annotated/all directory and then annotate the IMAGE with the messageId and/or call
   *
   * @param bytes
   * @param from
   * @param imageFileName
   */
  private void writeImageFile(byte[] bytes, String from, String imageFileName) {

    // wtf with dumpIds?
    var doDebug = false;
    var myDumpIds = Arrays.asList("KI7IHU");
    if (myDumpIds.contains(from)) {
      doDebug = true;
    }

    try {
      // write the file
      imageFileName = from + "-" + imageFileName;
      var allImagePath = Path.of(allPath.toString(), imageFileName);
      Files.write(allImagePath, bytes);

      // create the link to pass or fail
      if (bytes.length <= MAX_ATTACHMENT_SIZE) {
        var rightSizeImagePath = Path.of(rightSizePath.toString(), imageFileName);
        Files.createLink(rightSizeImagePath, allImagePath);
      } else {
        var tooBigImagePath = Path.of(tooBigPath.toString(), imageFileName);
        Files.createLink(tooBigImagePath, allImagePath);
      }

      // apply annotation by call -- write a file, don't link it!

      if (doAnnotation) {
        var annotatedImageFileName = from + ".png";
        var annotatedAllImagePath = Path.of(annotatedAllPath.toString(), annotatedImageFileName);

        var source = ImageIO.read(new ByteArrayInputStream(bytes));
        Graphics g = source.getGraphics();
        var width = source.getWidth();
        var height = source.getHeight();
        var deltaHeight = 25;
        var font = g.getFont().deriveFont((float) deltaHeight);
        var type = source.getType();
        if (type == 0) {
          type = BufferedImage.TYPE_INT_RGB;
        }

        width = Math.max(100, width);
        var newImage = new BufferedImage(width, height + deltaHeight, type);
        g.dispose();
        g = newImage.getGraphics();
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height + deltaHeight);
        g.drawImage(source, 0, 0, null);
        g.setColor(Color.BLACK);
        g.drawString(from, width / 2 - (8 * from.length()), height + deltaHeight - 2);
        g.dispose();

        ImageIO.write(newImage, "png", new File(annotatedAllImagePath.toString()));

        // var runtime = Runtime.getRuntime();
        // var cmd = new String[] { "/usr/bin/convert", //
        // allImagePath.toString(), //
        // "label:" + from, //
        // "-gravity", //
        // "Center", //
        // "-append", //
        // annotatedAllImagePath.toString() };
        // var process = runtime.exec(cmd);
        // process.waitFor();
        if (bytes.length <= MAX_ATTACHMENT_SIZE) {
          var annotatedRightSizeImagePath = Path.of(annotatedRightSizePath.toString(), annotatedImageFileName);
          Files.createLink(annotatedRightSizeImagePath, annotatedAllImagePath);
        } else {
          var annotatedTooBigImagePath = Path.of(annotatedTooBigPath.toString(), annotatedImageFileName);
          Files.createLink(annotatedTooBigImagePath, annotatedAllImagePath);
        }
      } // endif doAnnotation
    } catch (Exception e) {
      logger.error("Exception writing image file for call: " + from + ", " + e.getLocalizedMessage());
    }
  }

  private String formatPP(String label, int okCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = (double) okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
  }

  /**
   * recursively remove directory and all contents
   *
   * @param path
   */
  public void deleteDirectory(Path path) {
    try {
      if (Files.exists(path)) {
        Files //
            .walk(path) //
              .map(Path::toFile) //
              .sorted((o1, o2) -> -o1.compareTo(o2)) //
              .forEach(File::delete);
      }
    } catch (Exception e) {
      throw new RuntimeException("exception deleting directory: " + path.toString() + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * (recursively) create directory
   *
   * @param path
   */
  public Path createDirectory(Path path) {
    try {
      return Files.createDirectories(path);
    } catch (Exception e) {
      throw new RuntimeException("exception creating directory: " + path.toString() + ", " + e.getLocalizedMessage());
    }
  }

  @Override
  public void setDumpIds(Set<String> dumpIds) {
    this.dumpIds = dumpIds;
  }

}