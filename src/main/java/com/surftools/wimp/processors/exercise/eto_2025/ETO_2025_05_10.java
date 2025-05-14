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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213ReplyMessage;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.service.image.ImageService;
import com.surftools.wimp.service.image.ImageSimilarityResult;
import com.surftools.wimp.service.image.ReferenceImage;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ETO Spring Drill: ICS-213-Reply, with 3 attached maps ICS-213 reply; folks import 16 each Hospital Bed, Field
 * Situation Report and Local Weather messages, generate 3 maps and reply to ICS-213 with a 3 counts
 *
 * Make this a MultiMessageFeedbackProcessor so that I can use the Summary record
 *
 * see ETO_2025-04_17 for a single map version
 *
 * @author bobt
 *
 */
public class ETO_2025_05_10 extends MultiMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_05_10.class);
  private final Double SIMILARITY_THRESHOLD = 0.98;

  private int imageMaxSize;
  private ImageService imageService;

  record TypeEntry(String fileName, Double simScore, int count) {
    public static List<String> getHeaders(String label) {
      var up = label.toUpperCase() + " ";
      return Arrays.asList(new String[] { up + "File", up + "Sim", up + "Count" });
    }

    public List<String> getValues() {
      final DecimalFormat SDF = new DecimalFormat("0.##");
      var name = fileName == null ? "" : fileName;
      var score = simScore == null ? "" : SDF.format(simScore * 100d) + "%";
      var countString = count == 0 ? "" : String.valueOf(count);
      return Arrays.asList(new String[] { name, score, countString });
    }
  };

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {
    public Map<String, TypeEntry> typeMap = new HashMap<>();
    public String messageId;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list.addAll(Arrays.asList(new String[] { "MessageId" }));
      list.addAll(TypeEntry.getHeaders("fsr"));
      list.addAll(TypeEntry.getHeaders("hsb"));
      list.addAll(TypeEntry.getHeaders("lwx"));
      return list.toArray(new String[0]);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list.addAll(Arrays.asList(new String[] { messageId }));
      list.addAll(typeMap.getOrDefault("fsr", new TypeEntry(null, null, 0)).getValues());
      list.addAll(typeMap.getOrDefault("hsb", new TypeEntry(null, null, 0)).getValues());
      list.addAll(typeMap.getOrDefault("lwx", new TypeEntry(null, null, 0)).getValues());
      return list.toArray(new String[0]);
    };

  }

  private List<String> typeKeys = List.of("fsr", "hsb", "lwx");
  private Map<String, ReferenceImage> referenceImages = new HashMap<>();
  private Summary summary;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    messageType = MessageType.ICS_213_REPLY;
    acceptableMessageTypesSet.add(messageType);

    super.initialize(cm, mm, logger);

    imageMaxSize = cm.getAsInt(Key.IMAGE_MAX_SIZE, 35_000);

    for (var key : typeKeys) {
      var fileName = Path.of(pathName, "reference-" + key + ".jpg").toString();
      var referenceImage = new ReferenceImage(fileName, SIMILARITY_THRESHOLD);
      referenceImages.put(key, referenceImage);
    }
    imageService = new ImageService(outputPathName);

    var extraOutboundMessageText = "\nNEW: video on how to complete drill: https://youtu.be/yg59-FpY7s0\n";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void beforeProcessingForSender(String sender) {
    super.beforeProcessingForSender(sender);

    sts.setExplanationPrefix(""); // only one messageType

    // #MM must instantiate a derived Summary object
    iSummary = summary = (Summary) summaryMap.getOrDefault(sender, new Summary(sender));
    summaryMap.put(sender, iSummary);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213ReplyMessage) message;
    sts.setExplanationPrefix(""); // only one messageType
    summary.messageId = m.messageId;
    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));

    var reply = m.reply;
    if (reply != null) {
      count(sts.test("ICS-213 reply should be present", true));
      var replyLines = reply.split("\n");
      if (replyLines.length == 2) {
        count(sts.test("ICS-213 reply should be 2 lines", true));

        var line1 = replyLines[0];
        count(sts.test("Reply line 1 should be #EV", "FSR,HSB,LWX", line1));

        var line2 = replyLines[1];
        count(sts.test("Reply line 2 should be #EV", "12,9,9", line2));
      } else {
        count(sts.test("ICS-213 reply should be 2 lines", false));
      }
    } else {
      count(sts.test("ICS-213 reply should be present", false));
    }

    Map<String, List<ImageSimilarityResult>> imSimResultMap = new HashMap<>();

    var images = imageService.getImageAttachments(m);
    imageService.writeImages(m);
    if (images.size() > 0) {
      for (var image : images.values()) {

        var isRightSize = image.bytes().length <= imageMaxSize * 1.05d;
        count(sts
            .test("Image file size should be <= " + imageMaxSize + " bytes", isRightSize,
                String.valueOf(image.bytes().length) + " for image " + image.fileName()));

        var key = getKeyForFileName(image.fileName());
        if (key != null) {

          var referenceImage = referenceImages.get(key);
          var imSimResult = imageService.findSimilarityScore(m, image, referenceImage);

          var list = imSimResultMap.getOrDefault(key, new ArrayList<ImageSimilarityResult>());
          list.add(imSimResult);
          imSimResultMap.put(key, list);
          count(sts.test("Image name recognized", true, image.fileName()));
        } else { // key == null; unknown image type
          count(sts.test("Image name recognized", false, image.fileName()));
        }
      } // end loop over images

      for (var key : typeKeys) {
        var imSimList = imSimResultMap.get(key);
        if (imSimList == null || imSimList.size() == 0) {
          count(sts.test("Should attach 1 " + key + " image", false));
          continue;
        }
        Collections.sort(imSimList, (i1, i2) -> i2.score().compareTo(i1.score()));
        var mostSimIm = imSimList.get(0);
        var typeEntry = new TypeEntry(mostSimIm.imageName(), mostSimIm.score(), imSimList.size());
        count(sts.test("Should attach #EV image", "1 " + key, String.valueOf(typeEntry.count) + " " + key));
        summary.typeMap.put(key, typeEntry);
      }

      var allResults = imSimResultMap.values().stream().flatMap(List::stream).toList();
      imageService.writeSimiliarityResults(allResults);
    } // end if images.size() > 0
  }

  private String getKeyForFileName(String fileName) {
    final var fsrSet = Set.of("fsr");
    final var hsbSet = Set.of("hsb", "hbr", "hbs");
    final var lwxSet = Set.of("lwx", "lws");

    var fields = fileName.toLowerCase().split("\\.");
    var s = fields[0];

    if (fsrSet.contains(s) && !hsbSet.contains(s) && !lwxSet.contains(s)) {
      return "fsr";
    }

    if (hsbSet.contains(s) && !fsrSet.contains(s) && !lwxSet.contains(s)) {
      return "hsb";
    }

    if (lwxSet.contains(s) && !fsrSet.contains(s) && !hsbSet.contains(s)) {
      return "lwx";
    }

    logger.warn("no match for: " + fileName);
    return null;
  }

  @Override
  protected void endProcessingForSender(String sender) {
    if (summary.messageId == null) {
      summary = null;
      iSummary = null;
      summaryMap.remove(sender);
    }
  }

  @Override
  protected String makeOutboundMessageSubject(Object object) {
    var summary = (Summary) object;
    return outboundMessageSubject + " " + summary.messageId;
  }

  @Override
  public void postProcess() {
    super.postProcess();
    imageService.writeSimilarityResults("similarityResults.csv");
  }

}