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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

/**
 * Winlink Check In, with cropped and resized image, compared to reference, and difficulty in comments
 *
 * @author bobt
 *
 */
public class ETO_2022_06_09 implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_06_09.class);

  private final String REFERENCE_IMAGE_FILENAME = "/home/bobt/Documents/eto/2022-06-09/reference.jpg";
  private final String IMG_SIM_OVERRIDE_FILENAME = "/home/bobt/Documents/eto/2022-06-09/imgSim-overrides.txt";
  private final String OUTPUT_DIRECTORY = "/home/bobt/Documents/eto/2022-06-09/output/images";
  private final int MAX_ATTACHMENT_SIZE = 6000;
  private final Double SIMILARITY_THRESHOLD = 0.98;
  private final String REQUIRED_DATE = "2022/06/09";
  private final boolean doAnnotationByMessageId = false;
  private final boolean doAnnotationByCall = false;
  private final boolean gradeDateAsBonus = true;

  private final ImageHistogram histogrammer;
  private final float[] referenceFilter;
  private final Set<String> imgSimOverrideToPassSet = new HashSet<>();
  private final Set<String> imgSimOverrideToFailSet = new HashSet<>();

  private final Path outputPath;
  private final Path allPath;
  private final Path passPath;
  private final Path failPath;
  private final Path binnedPath;
  private Path annotatedMessageIdPath;
  private Path annotatedCallPath;

  // for post processing
  private int ppCount;
  private int ppImagePresentOk;
  private int ppImageSizeOk;
  private int ppImageSimOk;
  private int ppCommentOk;
  // private int ppOrganizationOk;
  private int ppDateOk;
  private int[] ppCommentCounts = new int[5];
  private double ppSumSimScore;
  private int ppSimScoreCount;
  private int ppSimScoreNotOkCount;
  private double ppSumSimOkScore;
  private double ppSumSimNotOkScore;
  private int[] ppBinCounts = new int[101];

  private int ppImgSimOverrideToPassCount;
  private int ppImgSimOverrideToFailCount;

  public ETO_2022_06_09() throws Exception {
    histogrammer = new ImageHistogram();

    var refFile = new File(REFERENCE_IMAGE_FILENAME);
    if (!refFile.exists()) {
      throw new RuntimeException("Reference file: " + REFERENCE_IMAGE_FILENAME + " not found");
    }

    outputPath = Path.of(OUTPUT_DIRECTORY);
    deleteDirectory(outputPath);
    allPath = createDirectory(Path.of(outputPath.toString(), "alll"));
    passPath = createDirectory(Path.of(outputPath.toString(), "pass"));
    failPath = createDirectory(Path.of(outputPath.toString(), "fail"));
    binnedPath = createDirectory(Path.of(outputPath.toString(), "binned"));

    if (doAnnotationByMessageId) {
      annotatedMessageIdPath = createDirectory(Path.of(outputPath.toString(), "annotated-messageId"));
    }

    if (doAnnotationByCall) {
      annotatedCallPath = createDirectory(Path.of(outputPath.toString(), "annotated-call"));
    }

    referenceFilter = histogrammer.filter(ImageIO.read(refFile));

    logger.info("Reference File: " + REFERENCE_IMAGE_FILENAME);
    logger.info("imgSim-overrides File: " + IMG_SIM_OVERRIDE_FILENAME);
    logger.info("Reference elements: " + referenceFilter.length);
    logger.info("Max Attachment Size: " + MAX_ATTACHMENT_SIZE);
    logger.info("Similarity Threshold: " + SIMILARITY_THRESHOLD);
    logger.info("Output Image Path: " + OUTPUT_DIRECTORY);
    logger.info("Required Date: " + REQUIRED_DATE);
    logger.info("Do Annotations by MessageId: " + doAnnotationByMessageId);
    logger.info("Do Annotations by Call: " + doAnnotationByCall);
    logger.info("Grade Date as Bonus: " + gradeDateAsBonus);

    processOverrideFile();
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
  private void processOverrideFile() {
    try {
      List<String> lines = Files.readAllLines(Path.of(IMG_SIM_OVERRIDE_FILENAME));
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
      logger.warn(IMG_SIM_OVERRIDE_FILENAME + " file not found");
    } catch (Exception e) {
      logger
          .error("Exception processing imgSim override file: " + IMG_SIM_OVERRIDE_FILENAME + ", "
              + e.getLocalizedMessage());
    }
    logger.info("overrides to pass: " + imgSimOverrideToPassSet.size());
    logger.info("overrides to fail: " + imgSimOverrideToFailSet.size());
  }

  @Override
  public GradeResult grade(GradableMessage gm) {
    if (!(gm instanceof CheckInMessage)) {
      return null;
    }

    CheckInMessage m = (CheckInMessage) gm;

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
    }

    if (bufferedImage != null) {
      var isImageSimilar = false;
      ++ppImagePresentOk;
      points += 25;

      if (bytes.length <= MAX_ATTACHMENT_SIZE) {
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
        ++ppBinCounts[(int) (100 * simScore)];
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
            ++ppCommentCounts[index];
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

    // var organization = m.organization;
    // if (organization != null && organization.equalsIgnoreCase("ETO Winlink Thursday")) {
    // ++ppOrganizationOk;
    // points += 5;
    // } else {
    // explanations.add("group/agency not 'ETO Winlink Thursday'");
    // }

    if (m.date.equals(REQUIRED_DATE)) {
      ++ppDateOk;
      points += 10;
      if (gradeDateAsBonus) {
        explanations.add("10 point bonus for sending on " + REQUIRED_DATE + " (UTC)");
      }
    } else {
      explanations.add("message wasn't sent on required date of " + REQUIRED_DATE + " (UTC)");
    }

    points = Math.min(100, points);
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);
    return new GradeResult(grade, explanation);
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
  public String getPostProcessReport(List<ExportedMessage> messages) {
    if (messages == null || messages.size() == 0 || messages.get(0).getMessageType() != MessageType.CHECK_IN) {
      return null;
    }

    // TODO compile statistics here
    for (var ex : messages) {
      var m = (CheckInMessage) ex;
      // makes sense to grade here, and not from CheckInProcessor
      // but need to move post processing before writing in WinlinkMessageMapper
    }

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);
    sb.append("\nETO-2022-06-09 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    sb.append(formatPP("image attached", ppImagePresentOk));
    sb.append(formatPP("image size", ppImageSizeOk));
    sb.append(formatPP("image similarity", ppImageSimOk));
    // sb.append(formatPP("agency/group name", ppOrganizationOk));
    sb.append(formatPP("sent on correct date", ppDateOk));
    sb.append(formatPP("comments on difficulty", ppCommentOk));
    sb.append(formatPPCounts());

    sb.append("\n   average simScore: " + formatPercent(ppSumSimScore / ppSimScoreCount));
    sb.append("\n   average simScore(ok): " + formatPercent(ppSumSimOkScore / ppImageSimOk));
    sb.append("\n   average simScore(not ok): " + formatPercent(ppSumSimNotOkScore / ppSimScoreNotOkCount));
    sb.append("\n");

    sb.append("\n   override to fail count: " + ppImgSimOverrideToFailCount);
    sb.append("\n   override to pass count: " + ppImgSimOverrideToPassCount);
    sb.append("\n");

    sb.append("\n   counts by bin\n");
    for (var binIndex = 100; binIndex >= 0; --binIndex) {
      if (ppBinCounts[binIndex] == 0) {
        continue;
      }
      sb.append("      count[" + binIndex + "] = " + ppBinCounts[binIndex] + "\n");
    }

    return sb.toString();
  }

  private Object formatPPCounts() {
    var sb = new StringBuilder();

    for (var i = 0; i < 5; ++i) {
      var commentPercent = (double) ppCommentCounts[i] / (double) ppCommentOk;
      sb
          .append("      comment value: " + (i + 1) + ", count: " + ppCommentCounts[i] + "("
              + formatPercent(commentPercent) + ")\n");
    }

    return sb.toString();

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
      var binnedImagePath = createDirectory(Path.of(binnedPath.toString(), String.valueOf(binIndex)));
      binnedImagePath = Path.of(binnedImagePath.toString(), scoredImageFileName);
      Files.createLink(binnedImagePath, allImagePath);

      // apply annotation by messageId -- write a file, don't link it!
      if (doAnnotationByMessageId) {
        var annotatedMessageIdImagePath = createDirectory(
            Path.of(annotatedMessageIdPath.toString(), String.valueOf(binIndex)));
        annotatedMessageIdImagePath = Path.of(annotatedMessageIdImagePath.toString(), scoredImageFileName);
        Files.write(annotatedMessageIdImagePath, bytes);

        var runtime = Runtime.getRuntime();
        var cmd = new String[] { "/usr/bin/convert", //
            annotatedMessageIdImagePath.toString(), //
            "label:" + messageId, //
            "-gravity", //
            "Center", //
            "-append", //
            annotatedMessageIdImagePath.toString() };
        var process = runtime.exec(cmd);
        process.waitFor();
      }

      // apply annotation by call -- write a file, don't link it!

      if (doAnnotationByCall) {
        var annotatedCallImagePath = Path.of(annotatedCallPath.toString(), scoredImageFileName);
        Files.write(annotatedCallImagePath, bytes);

        var runtime = Runtime.getRuntime();
        var cmd = new String[] { "/usr/bin/convert", //
            annotatedCallImagePath.toString(), //
            "label:" + from, //
            "-gravity", //
            "Center", //
            "-append", //
            annotatedCallImagePath.toString() };
        var process = runtime.exec(cmd);
        process.waitFor();
      }

    } catch (

    Exception e) {
      logger
          .error("Exception writing image file for call: " + from + ", messageId: " + messageId + ", "
              + e.getLocalizedMessage());
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

}