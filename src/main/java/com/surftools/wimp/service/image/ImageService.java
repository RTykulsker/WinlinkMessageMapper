/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.service.image;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.IService;

public class ImageService implements IService {

  private static Logger logger = LoggerFactory.getLogger(ImageService.class);

  protected String outputPathName;
  protected Path outputPath;
  protected List<ImageSimilarityResult> similarityResults = new ArrayList<>();

  public ImageService(String outputPathName) {
    this.outputPathName = outputPathName;
    this.outputPath = Path.of(outputPathName);
  }

  @Override
  public String getName() {
    return "ImageService";
  }

  /**
   * return only the message attachments that are images
   *
   * @param m
   * @return
   */
  public Map<String, byte[]> getImageAttachments(ExportedMessage m) {
    var returnMap = new LinkedHashMap<String, byte[]>();

    for (Entry<String, byte[]> entry : m.attachments.entrySet()) {
      try {
        var bytes = entry.getValue();
        var bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
          continue;
        }
        logger.debug("image found for attachment: " + entry.getKey() + ", size:" + bytes.length);
        returnMap.put(entry.getKey(), bytes);
        break;
      } catch (Exception e) {
        ;
      }
    }
    logger.debug("found " + returnMap.size() + " images for " + m.from + ", mId: " + m.messageId);
    return returnMap;
  }

  /**
   * return a sorted (decreasing score) list of ImageSimilarityResult objects against a ReferenceImage
   *
   * @param m
   * @param references
   * @param similarityThreshold
   *
   * @return
   */
  public List<ImageSimilarityResult> findSimilarityScores(ExportedMessage m, ReferenceImage reference,
      Double similarityThreshold) {
    var images = getImageAttachments(m);
    var list = new ArrayList<ImageSimilarityResult>();
    for (Entry<String, byte[]> image : images.entrySet()) {
      Double simScore = reference.computeSimilarity(image.getValue(), image.getKey(), m.from);
      if (simScore != null) {
        var result = new ImageSimilarityResult(image.getKey(), image.getValue(), reference, simScore, m);
        list.add(result);
        similarityResults.add(result);
      }
    }

    list.sort((s1, s2) -> s2.score().compareTo(s1.score()));
    return list;
  }

  /**
   * return a sorted (decreasing score) list of ImageSimilarityResult objects against a ReferenceImage
   *
   * All returned images must exceed threshold
   *
   * @param m
   * @param references
   * @return
   */
  public List<ImageSimilarityResult> findSimilarImages(ExportedMessage m, ReferenceImage reference,
      Double similarityThreshold) {
    return findSimilarityScores(m, reference, similarityThreshold).stream().filter(r -> r.isSimilar()).toList();
  }

  /**
   * write all the images contained as attachments
   *
   * output/images/all/filename
   *
   * and a link in
   *
   * output/images/by-message-type/<messageType>/filename
   *
   * where filename is from-messageId-messageType-imageName-attachmentIndexNumber
   *
   * @param m
   */
  public void writeImages(ExportedMessage m) {
    var images = getImageAttachments(m);
    for (Entry<String, byte[]> image : images.entrySet()) {
      writeImage(m, image.getKey(), image.getValue());
    }
  }

  /**
   * write a image contained as an attachments
   *
   * output/images/all/filename
   *
   * and a link in
   *
   * output/images/by-message-type/<messageType>/filename
   *
   * where filename is from-messageId-messageType-imageName-attachmentIndexNumber
   *
   * @param m
   * @param imageFileName
   * @param bytes
   */
  public void writeImage(ExportedMessage m, String imageFileName, byte[] bytes) {
    var imagePath = FileUtils.makeDirIfNeeded(outputPath, "images");
    var allPath = FileUtils.makeDirIfNeeded(imagePath, "all");
    var byTypePath = FileUtils.makeDirIfNeeded(imagePath, "by-type");

    var messageTypeName = m.getMessageType().toString();
    var index = getAttachmentIndex(m.attachments, imageFileName, bytes);

    var filename = String.format("%s-%s-%s-%s-%s", m.from, m.messageId, messageTypeName, imageFileName, index);
    var allImagePath = Path.of(allPath.toString(), filename);
    try (FileOutputStream fos = new FileOutputStream(allImagePath.toString())) {
      fos.write(bytes);
    } catch (Exception e) {
      logger.error("Exception writing image file: " + filename + ", " + e.getMessage());
    }

    var messageTypePath = FileUtils.makeDirIfNeeded(byTypePath, messageTypeName);
    var byTypeImagePath = Path.of(messageTypePath.toString(), filename);
    try {
      Files.createLink(byTypeImagePath, allImagePath);
    } catch (IOException e) {
      logger.error("Exception creating linked image file: " + filename + ", " + e.getMessage());
    }
  }

  /**
   * because there can be two different attachments with same name, etc.
   *
   * @param attachments
   * @param imageFileName
   * @param bytes
   * @return
   */
  private String getAttachmentIndex(Map<String, byte[]> attachments, String imageFileName, byte[] bytes) {
    var index = 0;
    for (Entry<String, byte[]> image : attachments.entrySet()) {
      if (image.getKey().equals(imageFileName) && Arrays.equals(image.getValue(), bytes)) {
        return String.valueOf(index);
      }
      ++index;
    }
    return "###";
  }

  public void writeSimilarityResult(ImageSimilarityResult result) {
    var simPath = FileUtils.makeDirIfNeeded(outputPath, "similarity");
    var refPath = FileUtils
        .makeDirIfNeeded(simPath,
            result.referenceImage().referenceFileName + "-" + "-Threshold-" + result.referenceImage().getThreshold());
    var allPath = FileUtils.makeDirIfNeeded(refPath, "all");
    var passPath = FileUtils.makeDirIfNeeded(refPath, "pass");
    var failPath = FileUtils.makeDirIfNeeded(refPath, "fail");
    var binPath = FileUtils.makeDirIfNeeded(refPath, "binned");
    var scoreDouble = result.score();
    var score = "";
    if (scoreDouble == null) {
      score = "NULL";
    } else {
      int scoreInt = (int) (scoreDouble * 100d);
      score = String.valueOf(scoreInt);
    }
    var binnedPath = FileUtils.makeDirIfNeeded(binPath, score);

    var m = result.m();
    var from = m.from;
    var mId = m.messageId;
    var messageType = m.getMessageType();
    var messageTypeName = messageType.toString();
    var index = getAttachmentIndex(m.attachments, result.imageName(), result.imageBytes());
    var filename = String.join("-", List.of(from, mId, messageTypeName, result.imageName(), index, score));

    var allImagePath = Path.of(allPath.toString(), filename);
    try (FileOutputStream fos = new FileOutputStream(allImagePath.toString())) {
      fos.write(result.imageBytes());
    } catch (Exception e) {
      logger.error("Exception writing image file: " + filename + ", " + e.getMessage());
    }

    var isPass = result.isSimilar();
    var passFailPath = isPass ? passPath : failPath;
    for (var linkPath : List.of(passFailPath, binnedPath)) {
      var linkImagePath = Path.of(linkPath.toString(), filename);
      try {
        Files.createLink(linkImagePath, allImagePath);
      } catch (IOException e) {
        logger.error("Exception creating linked image file: " + linkImagePath.toString() + ", " + e.getMessage());
      }
    }
  }

  /**
   * write all the similarity results for a ReferenceImage
   *
   * @param results
   */
  public void writeSimiliarityResults(List<ImageSimilarityResult> results) {
    for (var result : results) {
      writeSimilarityResult(result);
    }
  }

  public void writeSimilarityResults(String fileName) {
    WriteProcessor.writeTable(fileName, similarityResults);
  }
}
