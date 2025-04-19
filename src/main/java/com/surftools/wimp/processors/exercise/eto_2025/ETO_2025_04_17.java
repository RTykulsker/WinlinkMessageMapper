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

package com.surftools.wimp.processors.exercise.eto_2025;

import java.nio.file.Path;
import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213ReplyMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.service.image.ImageService;
import com.surftools.wimp.service.image.ReferenceImage;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-213 reply; folks have to import 16 Hospital Bed messages, generate a map and reply to ICS-213 with a count of
 * hospitals with low (<=5) Emergency Bed count
 *
 * see ETO_2022_06_09 for Image Similarity
 *
 * @author bobt
 *
 */
public class ETO_2025_04_17 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_04_17.class);

  private final Double SIMILARITY_THRESHOLD = 0.99;
  private int imageMaxSize;
  private ReferenceImage referenceImage;
  private ImageService imageService;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213_REPLY;

    imageMaxSize = cm.getAsInt(Key.IMAGE_MAX_SIZE, 50_000);
    imageService = new ImageService(outputPathName);
    referenceImage = new ReferenceImage(Path.of(pathName, "reference.jpg").toString(), SIMILARITY_THRESHOLD);

  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213ReplyMessage) message;

    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Reply should be #EV", "6", m.reply));
    count(sts.test("Position should be #EV", "ETO participant", m.replyPosition));

    var images = imageService.getImageAttachments(m);
    count(sts.test("Number of attached images should be #EV", "1", String.valueOf(images.size())));
    if (images.size() > 0) {
      for (var image : images.values()) {
        imageService.writeImages(m);

        var isRightSize = image.bytes().length <= imageMaxSize * 1.05d;
        count(sts
            .test("Image file size should be <= " + imageMaxSize + " bytes", isRightSize,
                String.valueOf(image.bytes().length) + " for image " + image.fileName()));

        count(sts.test("Image file name should be #EV", "eto-exercise.jpg", image.fileName()));
      }

      var results = imageService.findSimilarityScores(message, referenceImage, SIMILARITY_THRESHOLD);
      imageService.writeSimiliarityResults(results);

      for (var result : results) {
        var isSimilar = result.score() != null && result.score() >= SIMILARITY_THRESHOLD;
        count(sts
            .test("Image should be similar to reference", isSimilar,
                "similarity score of " + formatScore(result.score()) + " for attachment: " + result.imageName()));
      }
    } // end if images.size() > 0
  }

  private String formatScore(Double score) {
    final var formatter = new DecimalFormat("0.##");
    return formatter.format(score * 100d) + "%";
  }

  @Override
  public void postProcess() {
    super.postProcess();
    imageService.writeSimilarityResults("similarityResults.csv");
  }

}