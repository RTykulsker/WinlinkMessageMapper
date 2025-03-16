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

import java.text.DecimalFormat;

import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.message.ExportedMessage;

public record ImageSimilarityResult(String imageName, byte[] imageBytes, ReferenceImage referenceImage, Double score,
    ExportedMessage m) implements IWritableTable {

  final static DecimalFormat formatter = new DecimalFormat("#0.0000");

  public boolean isSimilar() {
    return score >= referenceImage.getThreshold();
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "From", "MessageId", "Image Name", "Image Size", "Score", "Is Similar" };
  }

  @Override
  public String[] getValues() {
    var scoreString = formatter.format(100d * score);
    return new String[] { m().from, m().messageId, imageName(), String.valueOf(imageBytes().length), //
        scoreString, String.valueOf(isSimilar()) };
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ImageSimilarityResult) other;
    int cmp = m.from.compareTo(o.m.from);
    if (cmp != 0) {
      return cmp;
    }

    cmp = m.messageId.compareTo(o.m.messageId);
    if (cmp != 0) {
      return cmp;
    }

    cmp = imageName.compareTo(o.imageName);
    if (cmp != 0) {
      return cmp;
    }

    cmp = imageBytes.length - o.imageBytes.length;
    if (cmp != 0) {
      return cmp;
    }

    return (int) Math.signum(score - o.score);
  }
}
