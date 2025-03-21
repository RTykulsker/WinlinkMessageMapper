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
// https://github.com/nivance/image-similarity/blob/master/src/main/java/image/similarity/ImageHistogram.java

package com.surftools.wimp.service.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * @desc 相似图片识别（直方图）
 *
 * @desc Similar picture recognition (histogram)
 */
public class ImageHistogram {

  private int redBins;
  private int greenBins;
  private int blueBins;

  public ImageHistogram() {
    redBins = greenBins = blueBins = 4;
  }

  public float[] filter(BufferedImage src) {
    int width = src.getWidth();
    int height = src.getHeight();

    int[] inPixels = new int[width * height];
    float[] histogramData = new float[redBins * greenBins * blueBins];
    getRGB(src, 0, 0, width, height, inPixels);
    int index = 0;
    int redIdx = 0, greenIdx = 0, blueIdx = 0;
    int singleIndex = 0;
    float total = 0;
    for (int row = 0; row < height; row++) {
      int tr = 0, tg = 0, tb = 0;
      for (int col = 0; col < width; col++) {
        index = row * width + col;
        tr = (inPixels[index] >> 16) & 0xff;
        tg = (inPixels[index] >> 8) & 0xff;
        tb = inPixels[index] & 0xff;
        redIdx = (int) getBinIndex(redBins, tr, 255);
        greenIdx = (int) getBinIndex(greenBins, tg, 255);
        blueIdx = (int) getBinIndex(blueBins, tb, 255);
        singleIndex = redIdx + greenIdx * redBins + blueIdx * redBins * greenBins;
        histogramData[singleIndex] += 1;
        total += 1;
      }
    }

    // start to normalize the histogram data
    for (int i = 0; i < histogramData.length; i++) {
      histogramData[i] = histogramData[i] / total;
    }

    return histogramData;
  }

  private float getBinIndex(int binCount, int color, int colorMaxValue) {
    float binIndex = (((float) color) / ((float) colorMaxValue)) * (binCount);
    if (binIndex >= binCount)
      binIndex = binCount - 1;
    return binIndex;
  }

  private int[] getRGB(BufferedImage image, int x, int y, int width, int height, int[] pixels) {
    int type = image.getType();
    if (type == BufferedImage.TYPE_INT_ARGB || type == BufferedImage.TYPE_INT_RGB)
      return (int[]) image.getRaster().getDataElements(x, y, width, height, pixels);
    return image.getRGB(x, y, width, height, pixels, 0, width);
  }

  /**
   * Bhattacharyya Coefficient http://www.cse.yorku.ca/~kosta/CompVis_Notes/bhattacharyya.pdf
   *
   * @return 返回值大于等于0.8可以简单判断这两张图片内容一致
   *
   * @return The return value is greater than or equal to 0.8, which can be simply judged that the content of these two
   *         pictures is consistent
   * @throws IOException
   */
  public double match(File srcFile, File canFile) throws IOException {
    float[] sourceData = this.filter(ImageIO.read(srcFile));
    float[] candidateData = this.filter(ImageIO.read(canFile));
    return calcSimilarity(sourceData, candidateData);
  }

  /**
   * for re-using sourceDate multiple times ...
   *
   * @param sourceData
   * @param canFile
   * @return
   * @throws Exception
   */
  public double match(float[] sourceData, File canFile) throws Exception {
    float[] candidateData = this.filter(ImageIO.read(canFile));
    return calcSimilarity(sourceData, candidateData);
  }

  public double match(float[] sourceData, byte[] canData) throws Exception {
    float[] candidateData = this.filter(ImageIO.read(new ByteArrayInputStream(canData)));
    return calcSimilarity(sourceData, candidateData);
  }

  /**
   * @return 返回值大于等于0.8可以简单判断这两张图片内容一致
   *
   * @return The return value is greater than or equal to 0.8, which can be simply judged that the content of these two
   *         pictures is consistent
   * @throws IOException
   */
  public double match(URL srcUrl, URL canUrl) throws IOException {
    float[] sourceData = this.filter(ImageIO.read(srcUrl));
    float[] candidateData = this.filter(ImageIO.read(canUrl));
    return calcSimilarity(sourceData, candidateData);
  }

  private double calcSimilarity(float[] sourceData, float[] candidateData) {
    double[] mixedData = new double[sourceData.length];
    for (int i = 0; i < sourceData.length; i++) {
      mixedData[i] = Math.sqrt(sourceData[i] * candidateData[i]);
    }

    // The values of Bhattacharyya Coefficient ranges from 0 to 1,
    double similarity = 0;
    for (int i = 0; i < mixedData.length; i++) {
      similarity += mixedData[i];
    }

    // The degree of similarity
    return similarity;
  }

}
