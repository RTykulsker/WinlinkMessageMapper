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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
import com.surftools.winlinkMessageMapper.grade.ImageHistogram;

/**
 * Winlink Check In, with cropped and resized image, compared to reference, and difficulty in comments
 *
 * @author bobt
 *
 */
public class ETO_2022_06_09 implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_06_09.class);

  private final String REFERENCE_IMAGE_FILENAME = "/home/bobt/Documents/eto/2022-06-09/reference.jpg";
  private final String OUTPUT_DIRECTORY = "/home/bobt/Documents/eto/2022-06-09/output/images";
  private final int MAX_ATTACHMENT_SIZE = 6000;
  private final Double SIMILARITY_THRESHOLD = 0.98;
  private final String REQUIRED_DATE = "2022/06/09";

  private final ImageHistogram histogrammer;
  private final float[] referenceFilter;
  private final Path outputPath;

  // for post processing
  private int ppCount;
  private int ppImagePresentOk;
  private int ppImageSizeOk;
  private int ppImageSimOk;
  private int ppCommentOk;
  private int ppOrganizationOk;
  private int ppDateOk;
  private int[] ppCommentCounts = new int[5];
  private double ppSumSimScore;
  private int ppSimScoreCount;
  private int ppSimScoreNotOkCount;
  private double ppSumSimOkScore;
  private double ppSumSimNotOkScore;

  public ETO_2022_06_09() throws Exception {
    histogrammer = new ImageHistogram();

    var refFile = new File(REFERENCE_IMAGE_FILENAME);
    if (!refFile.exists()) {
      throw new RuntimeException("Reference file: " + REFERENCE_IMAGE_FILENAME + " not found");
    }

    var outputDirectory = new File(OUTPUT_DIRECTORY);
    if (!outputDirectory.exists()) {
      var ok = outputDirectory.mkdirs();
      if (!ok) {
        throw new RuntimeException("Could not create output image directory");
      }
    }
    outputPath = outputDirectory.toPath();

    referenceFilter = histogrammer.filter(ImageIO.read(refFile));

    logger.info("Reference File: " + REFERENCE_IMAGE_FILENAME);
    logger.info("Reference elements: " + referenceFilter.length);
    logger.info("Max Attachment Size: " + MAX_ATTACHMENT_SIZE);
    logger.info("Similarity Threshold: " + SIMILARITY_THRESHOLD);
    logger.info("Output Image Path: " + outputPath);
    logger.info("Required Date: " + REQUIRED_DATE);
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
          ++ppImageSimOk;
          ppSumSimOkScore += simScore;
          points += 25;
        } else {
          ppSumSimNotOkScore += simScore;
          ++ppSimScoreNotOkCount;
          explanations.add("attached image too dissimilar: " + formatPercent(simScore));
        }

        var scoredImageFileName = m.from + "-" + m.messageId + "-" + formatPercent(simScore) + "-" + imageFileName;
        var scoredImagePath = Path.of(outputPath.toString(), scoredImageFileName);
        Files.write(scoredImagePath, bytes);
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
      points += 10;
    } else {
      explanations.add("no comment supplied of difficulty of assignment");
    }

    var organization = m.organization;
    if (organization != null && organization.equalsIgnoreCase("ETO Winlink Thursday")) {
      ++ppOrganizationOk;
      points += 5;
    } else {
      explanations.add("group/agency not 'ETO Winlink Thursday'");
    }

    if (m.date.equals(REQUIRED_DATE)) {
      ++ppDateOk;
      points += 10;
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

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);
    sb.append("\nETO-2022-06-09 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    sb.append(formatPP("image attached", ppImagePresentOk));
    sb.append(formatPP("image size", ppImageSizeOk));
    sb.append(formatPP("image similarity", ppImageSimOk));
    sb.append(formatPP("agency/group name", ppOrganizationOk));
    sb.append(formatPP("sent on correct date", ppDateOk));
    sb.append(formatPP("comments on difficulty", ppCommentOk));
    sb.append(formatPPCounts());

    sb.append("\n   average simScore: " + formatPercent(ppSumSimScore / ppSimScoreCount));
    sb.append("\n   average simScore(ok): " + formatPercent(ppSumSimOkScore / ppImageSimOk));
    sb.append("\n   average simScore(not ok): " + formatPercent(ppSumSimNotOkScore / ppSimScoreNotOkCount));
    sb.append("\n");

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

  private String formatPP(String label, int okCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = (double) okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
  }

}