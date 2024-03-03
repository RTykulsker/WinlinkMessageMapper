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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.WxSevereMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Severe Weather, with cropped and resized image
 *
 * @author bobt
 *
 */
public class ETO_2022_07_14 extends AbstractBaseProcessor {

  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_07_14.class);

  private final int MAX_ATTACHMENT_SIZE = 6000;
  private final int MIN_ATTATCHMENT_SIZE = 3000;
  private final int MIN_IMAGE_PIXELS = 64;

  private Path allPath;
  private Path rightSizePath;
  private Path tooBigPath;
  private Path fileTooSmallPath;
  private Path imageTooSmallPath;

  private Counter missingValueCounter = new Counter();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var imageOutputPath = Path.of(outputPathName.toString(), "image");
    FileUtils.deleteDirectory(imageOutputPath);
    var imagePathName = imageOutputPath.toString();

    allPath = FileUtils.createDirectory(Path.of(imagePathName, "all"));
    rightSizePath = FileUtils.createDirectory(Path.of(imagePathName, "rightSized"));
    tooBigPath = FileUtils.createDirectory(Path.of(imagePathName, "tooBig"));
    fileTooSmallPath = FileUtils.createDirectory(Path.of(imagePathName, "fileTooSmall"));
    imageTooSmallPath = FileUtils.createDirectory(Path.of(imagePathName, "imageTooSmall"));

    logger.info("Max Attachment Size: " + MAX_ATTACHMENT_SIZE);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    int ppCount = 0;
    int ppImagePresentOk = 0;
    int ppFileTooBig = 0;
    int ppFileNotTooBig = 0;
    int ppFileRightSize = 0;
    int ppFileTooSmall = 0;
    int ppPixelsTooSmall = 0;
    int ppSumWidth = 0;
    int ppSumHeight = 0;

    var pointsCounter = new Counter();
    var results = new ArrayList<IWritableTable>();
    for (var gm : mm.getMessagesForType(MessageType.WX_SEVERE)) {

      WxSevereMessage m = (WxSevereMessage) gm;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.info(m.messageId + ", " + m.from);
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
          logger.debug("image found for attachment: " + key + ", size:" + bytes.length);
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
          ++ppFileNotTooBig;
          points += 40;
        } else {
          ++ppFileTooBig;
          explanations.add("image attachment size too large");
        }

        var width = bufferedImage.getWidth();
        var height = bufferedImage.getHeight();
        var isImageTooSmall = (width <= MIN_IMAGE_PIXELS) || (height <= MIN_IMAGE_PIXELS);

        if (bytes.length < MIN_ATTATCHMENT_SIZE) {
          ++ppFileTooSmall;
        }

        if (isImageTooSmall) {
          ++ppPixelsTooSmall;
        }

        if (bytes.length >= MIN_ATTATCHMENT_SIZE && bytes.length <= MAX_ATTACHMENT_SIZE) {
          ++ppFileRightSize;
          ppSumWidth += width;
          ppSumHeight += height;
        }

        writeImageFile(bytes, m.from, m.messageId, imageFileName, isImageTooSmall);
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
      points = Math.max(0, points);
      pointsCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over messages

    var sb = new StringBuilder();

    logger.info(sb.toString());
    sb.append("\nETO-2022-07-14 Grading Report: graded " + ppCount + " Severe Weather messages\n");
    sb.append(formatPP("image attached", ppImagePresentOk, ppCount));
    sb.append(formatPP("image file too big", ppFileTooBig, ppCount));
    sb.append(formatPP("image file not too big", ppFileNotTooBig, ppCount));
    sb.append(formatPP("image file right sized", ppFileRightSize, ppCount));
    sb.append(formatPP("image file too small", ppFileTooSmall, ppCount));
    sb.append(formatPP("image too small", ppPixelsTooSmall, ppCount));

    int avgWidth = (int) Math.round(ppSumWidth / (double) ppFileRightSize);
    int avgHeight = (int) Math.round(ppSumHeight / (double) ppFileRightSize);
    sb.append("  average image size: " + avgWidth + "x" + avgHeight + " for right sized images\n");

    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));

    sb.append("\nMissing Values: \n" + formatCounter(missingValueCounter.getDescendingKeyIterator(), "field", "count"));

    writeTable("graded-wx_severe.csv", results);
  }

  private int gradeRequireValue(String value, String label, int points, ArrayList<String> explanations) {
    if (value == null || value.isBlank()) {
      explanations.add(label + " must be specified");
      missingValueCounter.increment(label);
      return 0;
    } else {
      return points;
    }
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
   * @param messageId
   * @param imageFileName
   * @param isImageTooSmall
   */
  void writeImageFile(byte[] bytes, String from, String messageId, String imageFileName, boolean isImageTooSmall) {
    try {
      // write the file
      imageFileName = from + "-" + messageId + "-" + imageFileName;
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

      if (bytes.length < MIN_ATTATCHMENT_SIZE) {
        var fileTooSmallImagePath = Path.of(fileTooSmallPath.toString(), imageFileName);
        Files.createLink(fileTooSmallImagePath, allImagePath);
      }

      if (isImageTooSmall) {
        var imageTooSmallImagePath = Path.of(imageTooSmallPath.toString(), imageFileName);
        Files.createLink(imageTooSmallImagePath, allImagePath);
      }

      // apply annotation by call -- write a file, don't link it!

    } catch (Exception e) {
      logger.error("Exception writing image file for call: " + from + ", " + e.getLocalizedMessage());
    }
  }

}