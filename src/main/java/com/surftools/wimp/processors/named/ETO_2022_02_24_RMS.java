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

package com.surftools.wimp.processors.named;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;

/**
 *
 *
 * @author bobt
 *
 */
public class ETO_2022_02_24_RMS extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_02_24_RMS.class);

  private int imageMaxSize;
  private Path imageAllPath; // path where all images are written
  private List<Path> imageGoodPaths; // list of paths to link when images is "good"
  private List<Path> imageBadPaths; // lists of paths to link when image is "bad"

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    imageMaxSize = cm.getAsInt(Key.IMAGE_MAX_SIZE, 6000);

    FileUtils.deleteDirectory(Path.of(outputPathName, "image"));

    imageAllPath = Path.of(outputPathName, "image", "all");
    FileUtils.createDirectory(imageAllPath);

    var imageGoodPath = Path.of(outputPathName, "image", "rightSized");
    imageGoodPaths = List.of(imageGoodPath);
    FileUtils.createDirectory(imageGoodPath);

    var imageBadPath = Path.of(outputPathName, "image", "tooBig");
    imageBadPaths = List.of(imageBadPath);
    FileUtils.createDirectory(imageBadPath);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    var ppCount = 0;

    var scoreCounter = new Counter();
    var imagesPerMessageCounter = new Counter();

    var ppImagePresentCount = 0;
    var ppImageSizeOkCount = 0;
    var ppImageSizeTooBigCount = 0;

    var results = new ArrayList<IWritableTable>();
    for (var message : mm.getMessagesForType(MessageType.CHECK_IN)) {
      CheckInMessage m = (CheckInMessage) message;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.debug("messageId: " + m.messageId + ", from: " + m.from);
      }

      int points = 0;
      var explanations = new ArrayList<String>();
      ++ppCount;

      var attachmentIndex = 0;
      var anyImagePresent = false;
      var anyImageRightSized = false;
      var imageCountForThisMessage = 0;
      for (var attachmentName : m.attachments.keySet()) {
        ++attachmentIndex;
        var bytes = m.attachments.get(attachmentName);
        if (areBytesAnImage(bytes)) {
          ++imageCountForThisMessage;
          anyImagePresent = true;
          var imageFileName = m.from + "-" + attachmentIndex + "-" + attachmentName;
          if (bytes.length <= imageMaxSize) {
            anyImageRightSized = true;
            writeContent(bytes, imageFileName, imageAllPath, imageGoodPaths);
          } else {
            writeContent(bytes, imageFileName, imageAllPath, imageBadPaths);
          }
        } // end if areBytesAnImage
      } // end loop over attachments
      if (anyImagePresent) {
        points += 50;
        ++ppImagePresentCount;
        imagesPerMessageCounter.increment(imageCountForThisMessage);
        if (anyImageRightSized) {
          points += 50;
          ++ppImageSizeOkCount;
        } else {
          explanations.add("no image(s) <= " + imageMaxSize + " bytes");
          ++ppImageSizeTooBigCount;
        }
      } else {
        explanations.add("no image(s) present");
      }

      points = Math.min(100, points);
      points = Math.max(0, points);
      scoreCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over Check In messages

    var sb = new StringBuilder();

    sb.append("\nCheck In messages: " + ppCount + "\n");
    sb.append(formatPP("Messages with Images", ppImagePresentCount, ppCount));
    sb.append(formatPP("Messages with Right-sized Images", ppImageSizeOkCount, ppCount));
    sb.append(formatPP("Messages with Images too large", ppImageSizeTooBigCount, ppCount));

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));
    sb
        .append("\nImages Per Message: \n"
            + formatCounter(imagesPerMessageCounter.getDescendingKeyIterator(), "images", "count"));

    logger.info(sb.toString());

    writeTable("graded-check_in.csv", results);
  }

}