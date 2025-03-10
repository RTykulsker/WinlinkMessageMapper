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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.ImageHistogram;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Winlink Check In, with cropped and resized image, compared to reference, and difficulty in comments
 *
 * @author bobt
 *
 */
public class ETO_2022_06_09 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_06_09.class);

  // make configurable if appropriate
  private final Double SIMILARITY_THRESHOLD = 0.98;
  private final boolean gradeDateAsBonus = true;

  private int imageMaxSize;
  private LocalDate exerciseDate;
  private String exerciseDateString;

  private ImageHistogram histogrammer;
  private float[] referenceFilter;
  private final Set<String> imgSimOverrideToPassSet = new HashSet<>();
  private final Set<String> imgSimOverrideToFailSet = new HashSet<>();

  private Path allPath;
  private Path passPath;
  private Path failPath;
  private Path binnedPath;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    histogrammer = new ImageHistogram();

    imageMaxSize = cm.getAsInt(Key.IMAGE_MAX_SIZE, 6000);

    var exerciseDateString = cm.getAsString(Key.EXERCISE_DATE);
    exerciseDate = LocalDate.parse(exerciseDateString, DateTimeFormatter.ofPattern("yyyy/MM/dd"));

    var refFile = Path.of(pathName, "reference.jpg").toFile();
    if (!refFile.exists()) {
      throw new RuntimeException("Reference file: " + refFile.toString() + " not found");
    }
    try {
      referenceFilter = histogrammer.filter(ImageIO.read(refFile));
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not read reference file: " + refFile.toString() + ", " + e.getLocalizedMessage());
    }

    var imagePath = Path.of(outputPathName, "images");
    FileUtils.deleteDirectory(imagePath);
    allPath = FileUtils.createDirectory(Path.of(imagePath.toString(), "alll"));
    passPath = FileUtils.createDirectory(Path.of(imagePath.toString(), "pass"));
    failPath = FileUtils.createDirectory(Path.of(imagePath.toString(), "fail"));
    binnedPath = FileUtils.createDirectory(Path.of(imagePath.toString(), "binned"));

    logger.info("Reference File: " + refFile.toString());
    logger.info("Reference elements: " + referenceFilter.length);
    logger.info("Max Attachment Size: " + imageMaxSize);
    logger.info("Similarity Threshold: " + SIMILARITY_THRESHOLD);
    logger.info("Output Image Path: " + imagePath.toString());
    logger.info("Required Date: " + exerciseDateString);
    logger.info("Grade Date as Bonus: " + gradeDateAsBonus);

    processOverrideFile(Path.of(pathName, "imgSim-overrides.txt"));
  }

  /**
   * file format should be:
   *
   * XXXXXXXXXXXX -- to override messageId to pass
   *
   * XXXXXXXXXXXX,pass -- to override messageId to pass
   *
   * XXXXXXXXXXXX,fail -- to override messageId to fail
   *
   */
  private void processOverrideFile(Path overridePath) {
    try {
      List<String> lines = Files.readAllLines(overridePath);
      for (var line : lines) {
        var fields = line.split(",");
        if (fields.length == 1) {
          // assume a messageId to pass
          imgSimOverrideToPassSet.add(fields[0]);
        } else if (fields.length == 2) {
          var action = fields[1];
          if (action.equals("pass")) {
            imgSimOverrideToPassSet.add(fields[0]);
          } else if (action.equals("fail")) {
            imgSimOverrideToFailSet.add(fields[0]);
          } else {
            logger.warn("unsupported imgSim override line: " + line + " -- ignored");
          }
        } else {
          logger.warn("unsupported imgSim override line: " + line + " -- ignored");
        }
      }
    } catch (NoSuchFileException e) {
      logger.warn(overridePath.toString() + " file not found");
    } catch (Exception e) {
      logger
          .error(
              "Exception processing imgSim override file: " + overridePath.toString() + ", " + e.getLocalizedMessage());
    }
    logger.info("overrides to pass: " + imgSimOverrideToPassSet.size());
    logger.info("overrides to fail: " + imgSimOverrideToFailSet.size());
  }

  @Override
  public void process() {
    // for post processing
    int ppCount = 0;
    int ppImagePresentOk = 0;
    int ppImageSizeOk = 0;
    int ppImageSimOk = 0;
    int ppCommentOk = 0;
    // int ppOrganizationOk=0;
    int ppDateOk = 0;

    double ppSumSimScore = 0;
    int ppSimScoreCount = 0;
    int ppSimScoreNotOkCount = 0;
    double ppSumSimOkScore = 0;
    double ppSumSimNotOkScore = 0;
    int ppImgSimOverrideToPassCount = 0;
    int ppImgSimOverrideToFailCount = 0;

    var difficultyCounter = new Counter();
    var pointsCounter = new Counter();
    var results = new ArrayList<IWritableTable>();
    for (var gm : mm.getMessagesForType(MessageType.CHECK_IN)) {

      CheckInMessage m = (CheckInMessage) gm;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.info("ETO_2022_06_09 grader: " + m);
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
      }

      if (bufferedImage != null) {
        var isImageSimilar = false;
        ++ppImagePresentOk;
        points += 25;

        if (bytes.length <= imageMaxSize) {
          ++ppImageSizeOk;
          points += 25;
        } else {
          explanations.add("image attachment size too large");
        }

        Double simScore = null;
        try {
          simScore = histogrammer.match(referenceFilter, bytes);
          ppSumSimScore += simScore;
          ++ppSimScoreCount;

          if (simScore >= SIMILARITY_THRESHOLD) {
            isImageSimilar = true;
            if (imgSimOverrideToFailSet.contains(m.messageId)) {
              ++ppImgSimOverrideToFailCount;
              isImageSimilar = false;
            }
          } else {
            isImageSimilar = false;
            if (imgSimOverrideToPassSet.contains(m.messageId)) {
              ++ppImgSimOverrideToPassCount;
              isImageSimilar = true;
            }
          }

          if (isImageSimilar) {
            ++ppImageSimOk;
            ppSumSimOkScore += simScore;
            points += (gradeDateAsBonus) ? 25 : 30;
          } else {
            ppSumSimNotOkScore += simScore;
            ++ppSimScoreNotOkCount;
            explanations.add("attached image too dissimilar: " + formatPercent(simScore));
          }

          writeFile(simScore, bytes, m.from, m.messageId, imageFileName, isImageSimilar);
        } catch (Exception e) {
          logger.error("exception calculating simScore for messageId: " + m.messageId + ", " + e.getLocalizedMessage());
        }
      } else {
        explanations.add("no image attachment found");
      }

      // is first line of comments a number between 1 and 5
      var commentsOk = false;
      var comments = m.comments;
      if (comments != null) {
        comments = comments.trim();
        var fields = comments.split("\n");
        if (fields.length >= 1) {
          var field = fields[0].trim();
          if (field.length() > 0) {
            var c = field.charAt(0);
            if (c >= '1' && c <= '5') {
              int index = c - '1';
              difficultyCounter.increment(index);
              commentsOk = true;
            }
          }
        }
      }
      if (commentsOk) {
        ++ppCommentOk;
        points += (gradeDateAsBonus) ? 25 : 10;
      } else {
        explanations.add("no comment supplied of difficulty of assignment");
      }

      if (m.formDateTime != null) {
        if (m.formDateTime.toLocalDate().isEqual(exerciseDate)) {
          ++ppDateOk;
          points += 10;
          if (gradeDateAsBonus) {
            explanations.add("10 point bonus for sending on " + exerciseDateString + " (UTC)");
          }
        } else {
          explanations.add("message wasn't sent on required date of " + exerciseDateString + " (UTC)");
        }
      } else {
        explanations.add("message wasn't sent on required date of " + exerciseDateString + " (UTC)");
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
    sb.append("\nETO-2022-06-09 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    sb.append(formatPP("image attached", ppImagePresentOk, ppCount));
    sb.append(formatPP("image size", ppImageSizeOk, ppCount));
    sb.append(formatPP("image similarity", ppImageSimOk, ppCount));
    sb.append(formatPP("sent on correct date", ppDateOk, ppCount));
    sb.append(formatPP("comments on difficulty", ppCommentOk, ppCount));

    sb.append("\n   average simScore: " + formatPercent(ppSumSimScore / ppSimScoreCount));
    sb.append("\n   average simScore(ok): " + formatPercent(ppSumSimOkScore / ppImageSimOk));
    sb.append("\n   average simScore(not ok): " + formatPercent(ppSumSimNotOkScore / ppSimScoreNotOkCount));
    sb.append("\n");

    sb.append("\n   override to fail count: " + ppImgSimOverrideToFailCount);
    sb.append("\n   override to pass count: " + ppImgSimOverrideToPassCount);
    sb.append("\n");

    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));
    sb.append("\nDifficulty: \n" + formatCounter(difficultyCounter.getDescendingKeyIterator(), "level", "count"));

    logger.info(sb.toString());
    writeTable("graded-check_in.csv", results);
  }

  /**
   * write the image file, to the all/ directory
   *
   * write a link to either the graded/pass/ or graded/fail directory
   *
   * write a link to the binned/XX/ directory, where XX is (int)(100 * simScore)
   *
   * (optionally) write a file to the annotated/XX directory and then annotate the IMAGE with the messageId and/or call
   *
   * @param simScore
   * @param bytes
   * @param from
   * @param messageId
   * @param imageFileName
   * @param isImageSimilar
   */
  private void writeFile(Double simScore, byte[] bytes, String from, String messageId, String imageFileName,
      boolean isImageSimilar) {

    var scoredImageFileName = from + "-" + messageId + "-" + formatPercent(simScore) + "-" + imageFileName;

    try {
      // write the file
      var allImagePath = Path.of(allPath.toString(), scoredImageFileName);
      Files.write(allImagePath, bytes);

      // create the link to pass or fail
      if (isImageSimilar) {
        var passImagePath = Path.of(passPath.toString(), scoredImageFileName);
        Files.createLink(passImagePath, allImagePath);
      } else {
        var failImagePath = Path.of(failPath.toString(), scoredImageFileName);
        Files.createLink(failImagePath, allImagePath);
      }

      int binIndex = (int) (100 * simScore);
      var binnedImagePath = FileUtils.createDirectory(Path.of(binnedPath.toString(), String.valueOf(binIndex)));
      binnedImagePath = Path.of(binnedImagePath.toString(), scoredImageFileName);
      Files.createLink(binnedImagePath, allImagePath);
    } catch (Exception e) {
      logger
          .error("Exception writing image file for call: " + from + ", messageId: " + messageId + ", "
              + e.getLocalizedMessage());
    }
  }

}