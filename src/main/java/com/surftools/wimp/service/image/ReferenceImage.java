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

import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * for image similarity
 */
public class ReferenceImage {
  private static Logger logger = LoggerFactory.getLogger(ReferenceImage.class);

  public final String referenceFileName; // just the last component
  private final double similarityThreshold;

  private final float[] referenceFilter;
  private final ImageHistogram histogrammer;

  public ReferenceImage(String referenceFileNameString, double similarityThreshold) {
    var referenceFile = Path.of(referenceFileNameString).toFile();
    referenceFileName = referenceFile.getName();
    this.similarityThreshold = similarityThreshold;

    histogrammer = new ImageHistogram();
    try {
      referenceFilter = histogrammer.filter(ImageIO.read(referenceFile));
    } catch (Exception e) {
      throw new RuntimeException(
          "Could not read reference file: " + referenceFile.toString() + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * compute the similarity score
   *
   * @param imageBytes
   * @return
   */
  public Double computeSimilarity(byte[] imageBytes, String imageName, String imageSource) {
    try {
      var simScore = histogrammer.match(referenceFilter, imageBytes);
      return simScore;
    } catch (Exception e) {
      logger
          .error("Exception computing histogram for image: " + imageName + " against reference: " + referenceFileName
              + " for source " + imageSource);
      return null;
    }
  }

  public Double getThreshold() {
    return similarityThreshold;
  }
}
