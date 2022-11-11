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

package com.surftools.winlinkMessageMapper.aggregation.named;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.FileUtils;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.Ics213Message;
import com.surftools.winlinkMessageMapper.dto.message.UnifiedFieldSituationMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * Aggregator for 2022-11-12 DRILL: one FSR and one ICS-213.
 *
 * NOTE WELL: This is for the graded/Part 1/RMS/Clearinghouse portion of the drill
 *
 * FSR graded on:
 *
 * --- FSR received: 25%
 *
 * --- required header: 25%
 *
 * --- automatic fail if Box 1 is Yes
 *
 * ICS-213 Populated as requested
 *
 * --- ICS received: 25%
 *
 * --- required header: 25%
 *
 * BOTH messages must be present (and not failed) to allow participation in P2P
 *
 * Optional image attached to ICS, bonus 25% if under 5K
 *
 * @author bobt
 *
 *
 *
 * @author bobt
 *
 */
public class ETO_2022_11_12_RMS_Aggregator extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_11_12_RMS_Aggregator.class);

  private static final String REQUIRED_HEADER_TEXT = "EmComm Training Organization 1112 THIS IS A DRILL";

  private List<Entry> passList = new ArrayList<>(); // both messages, no failures
  private List<Entry> failList = new ArrayList<>(); // one message OR automatic failure

  private Path imageAllPath;
  private Path imagePassPath;
  private Path imageBadSizePath;
  private int maxImageSize = -1;

  static record Entry(String call, LatLongPair location, //
      String fsrClearinghouse, String fsrMessageId, String fsrComment, //
      String icsClearinghouse, String icsMessageId, String icsComment, int icsImageSize, //
      String grade, String explanation) {

    public static String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", //
          "FsrTo", "FsrMiD", "FsrComment", //
          "IcsTo", "IcsMiD", "IcsMessage", "IcsImageBytes", //
          "Grade", "Explanation" };
    }

    public String[] getValues() {
      return new String[] { call, location.getLatitude(), location.getLongitude(), //
          fsrClearinghouse, fsrMessageId, fsrComment, //
          icsClearinghouse, icsMessageId, icsComment, String.valueOf(icsImageSize), //
          grade, explanation };
    }
  };

  public ETO_2022_11_12_RMS_Aggregator() {
    super(logger);
  }

  public void initialize() {
    var outputPath = Path.of(cm.getAsString(Key.PATH), "output", "image");
    FileUtils.deleteDirectory(outputPath);

    imageAllPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "all"));
    imagePassPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "pass"));
    imageBadSizePath = FileUtils.createDirectory(Path.of(outputPath.toString(), "badSize"));

    maxImageSize = cm.getAsInt(Key.MAX_IMAGE_SIZE, 5120);
  }

  @Override
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    initialize();

    super.aggregate(messageMap);

    var ppCount = 0;
    var ppFsrReceived = 0;
    var ppIcsReceived = 0;
    var ppFsrSetupOk = 0;
    var ppIcsSetupOk = 0;
    var ppIcsImageAttachedOk = 0;
    var ppIcsImageSizeOk = 0;

    var ppPassCount = 0;
    var ppFailCount = 0;
    var ppFailNoFsrCount = 0;
    var ppFailNoIcsCount = 0;
    var ppFailFcsBox1Count = 0;
    var ppFailNoLocationCount = 0;
    var ppFailDifferentClearingHousesCount = 0;

    var ppScoreCountMap = new HashMap<Integer, Integer>();

    for (var m : aggregateMessages) {
      var from = m.from();
      ++ppCount;

      if (dumpIds.contains(from)) {
        logger.info("dump: " + from);
      }

      var map = fromMessageMap.get(from);

      var isFailure = false; // either message missing or auto-fail

      UnifiedFieldSituationMessage fsrMessage = null;
      Ics213Message icsMessage = null;

      String fsrMessageId = null;
      LatLongPair fsrLocation = null;
      String fsrClearinghouse = null;
      String fsrSetup = null;
      String fsrComment = null;

      String icsMessageId = null;
      LatLongPair icsLocation = null;
      String icsClearinghouse = null;
      String icsSetup = null;
      String icsComment = null;
      int icsImageSize = -1;

      var points = 0;
      var explanations = new ArrayList<String>();

      var fsrList = map.get(MessageType.UNIFIED_FIELD_SITUATION);
      if (fsrList != null) {
        Collections.reverse(fsrList);
        fsrMessage = (UnifiedFieldSituationMessage) fsrList.iterator().next();
        if (fsrMessage == null) {
          explanations.add("FAIL because no FSR message received");
          ++ppFailNoFsrCount;
          isFailure = true;
        } else {
          points += 25;
          ++ppFsrReceived;
          fsrMessageId = fsrMessage.messageId;
          fsrClearinghouse = fsrMessage.to;
          fsrComment = fsrMessage.additionalComments;
          fsrLocation = new LatLongPair(fsrMessage.latitude, fsrMessage.longitude);
          if (!fsrLocation.isValid()) {
            isFailure = true;
            ++ppFailNoLocationCount;
            explanations.add("FAIL because FSR location(" + fsrLocation.toString() + ") is not valid");
          }

          fsrSetup = fsrMessage.organization;
          if (fsrSetup == null || fsrSetup.isBlank()) {
            explanations.add("FSR setup not provided");
          } else {
            if (fsrSetup.equalsIgnoreCase(REQUIRED_HEADER_TEXT)) {
              points += 25;
              ++ppFsrSetupOk;
            } else {
              explanations.add("FSR setup (" + fsrSetup + ") doesn't match required(" + REQUIRED_HEADER_TEXT + ")");
            }
          }

          var box1 = fsrMessage.isHelpNeeded;
          if (box1.equalsIgnoreCase("YES")) {
            isFailure = true;
            explanations.add("FAIL because FSR Box1 set to 'YES'");
            ++ppFailFcsBox1Count;
          }

        } // end if fsr != null
      } else {
        explanations.add("no FSR message received");
        ++ppFailNoFsrCount;
        isFailure = true;
      } // end if fsrList != null

      var icsList = map.get(MessageType.ICS_213);
      if (icsList != null) {
        Collections.reverse(icsList);
        icsMessage = (Ics213Message) icsList.iterator().next();
        if (icsMessage == null) {
          explanations.add("FAIL because no ICS-213 message received");
          isFailure = true;
          ++ppFailNoIcsCount;
        } else {
          ++ppIcsReceived;
          points += 25;

          icsMessageId = icsMessage.messageId;
          icsClearinghouse = icsMessage.to;
          icsSetup = icsMessage.organization;
          icsComment = icsMessage.message;
          icsLocation = icsMessage.location;

          icsSetup = icsMessage.organization;
          if (icsSetup == null || icsSetup.isBlank()) {
            explanations.add("ICS setup not provided");
          } else {
            if (icsSetup.equalsIgnoreCase(REQUIRED_HEADER_TEXT)) {
              points += 25;
              ++ppIcsSetupOk;
            } else {
              explanations.add("ICS setup (" + icsSetup + ") doesn't match required(" + REQUIRED_HEADER_TEXT + ")");
            }
          }

          var imageFileName = getImageFile(icsMessage);
          if (imageFileName != null) {
            var bytes = icsMessage.attachments.get(imageFileName);
            if (bytes != null) {
              ++ppIcsImageAttachedOk;
              icsImageSize = bytes.length;

              var isImageSizeOk = false;
              if (bytes.length <= maxImageSize) {
                ++ppIcsImageSizeOk;
                points += 25;
                isImageSizeOk = true;
                explanations.add("extra credit for attached image");
              } else {
                explanations.add("no extra credit too large image");
              }
              writeImage(icsMessage, imageFileName, bytes, isImageSizeOk);
            }
          } else {
            explanations.add("no optional image attachment on ICS found");
          }

        }
      } else {
        explanations.add("no ICS message received");
        ++ppFailNoIcsCount;
        isFailure = true;
      } // end if icsList != null

      // only want folks who sent at least one of the right type of messages
      if (fsrMessage != null && icsMessage != null) {
        if (!fsrClearinghouse.equalsIgnoreCase(icsClearinghouse)) {
          isFailure = true;
          ++ppFailDifferentClearingHousesCount;
          explanations.add("FAIL because clearinghouses are different");
        }

        // this is just for fun
        if (fsrLocation.isValid() && icsLocation.isValid()) {
          var distanceMeters = LocationUtils.computeDistanceMeters(fsrLocation, icsLocation);
          if (distanceMeters > 10) {
            logger
                .warn("### call: " + from + " location difference: " + distanceMeters + ", fsr: "
                    + fsrLocation.toString() + ", ics: " + icsLocation.toString());
          }
        }
      }

      points = Math.min(100, points);
      points = Math.max(0, points);

      if (isFailure) {
        points = 0;
      }

      var grade = String.valueOf(points);
      var explanation = (points == 100 && explanations.size() == 0) //
          ? "Perfect Score!"
          : String.join("\n", explanations);

      var scoreCount = ppScoreCountMap.getOrDefault(points, Integer.valueOf(0));
      ++scoreCount;
      ppScoreCountMap.put(points, scoreCount);

      var entry = new Entry(from, fsrLocation, //
          fsrClearinghouse, fsrMessageId, fsrComment, //
          icsClearinghouse, icsMessageId, icsComment, icsImageSize, //
          grade, explanation);

      if (isFailure) {
        ++ppFailCount;
        failList.add(entry);
      } else {
        ++ppPassCount;
        passList.add(entry);
      }
    } // end loop over for

    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-09-22 RMS aggregate results:\n");
    sb.append("total participants: " + ppCount + "\n");

    sb.append("participants with both required messages (may also fail): " + ppPassCount + "\n");
    sb.append(format("  FSR received", ppFsrReceived, ppCount));
    sb.append(format("  FSR setup Ok", ppFsrSetupOk, ppCount));

    sb.append(format("  ICS received", ppIcsReceived, ppCount));
    sb.append(format("  ICS setup Ok", ppIcsSetupOk, ppCount));
    sb.append(format("  ICS image present", ppIcsImageAttachedOk, ppCount));
    sb.append(format("  ICS image size Ok", ppIcsImageSizeOk, ppCount));

    sb.append("\nparticipants with automatic fails: " + ppFailCount + "\n");
    sb.append(format("  FAIL because no FSR", ppFailNoFsrCount, ppFailCount));
    sb.append(format("  FAIL because no ICS", ppFailNoIcsCount, ppFailCount));
    sb.append(format("  FAIL because FSR Box 1", ppFailFcsBox1Count, ppFailCount));
    sb.append(format("  FAIL because Location invalid", ppFailNoLocationCount, ppFailCount));
    sb.append(format("  FAIL because different clearinghouses", ppFailDifferentClearingHousesCount, ppFailCount));

    var scores = new ArrayList<Integer>(ppScoreCountMap.keySet());
    Collections.sort(scores, Comparator.reverseOrder());
    sb.append("\nscores: \n");
    for (int score : scores) {
      var count = ppScoreCountMap.get(score);
      sb.append(" score: " + score + ", count: " + count + "\n");
    }

    logger.info(sb.toString());

  }

  private String formatPercent(int numerator, int denominator) {
    if (denominator == 0) {
      return "";
    }

    double percent = (100d * numerator) / denominator;
    return " (" + String.format("%.2f", percent) + "%)";
  }

  private String format(String label, int numerator, int denominator) {
    return label + ": " + numerator + " " + formatPercent(numerator, denominator) + "\n";
  }

  @Override
  public void output(String pathName) {
    output(pathName, "aggregate-pass.csv", passList);
    output(pathName, "aggregate-fail.csv", failList);
  }

  private void output(String pathName, String fileName, List<Entry> entries) {
    Path outputPath = Path.of(pathName, "output", fileName);
    FileUtils.makeDirIfNeeded(outputPath.toString());

    var messageCount = 0;
    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(Entry.getHeaders());

      if (entries.size() > 0) {
        entries.sort((Entry s1, Entry s2) -> s1.call.compareTo(s2.call));
        for (Entry e : entries) {
          if (e != null) {
            var values = e.getValues();
            if (values != null) {
              writer.writeNext(e.getValues());
              ++messageCount;
            }
          } else {
            continue;
          }
        }
      }

      writer.close();
      logger.info("wrote " + messageCount + " aggregate messages to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  private String getImageFile(ExportedMessage m) {
    for (String key : m.attachments.keySet()) {
      try {
        var bytes = m.attachments.get(key);
        var bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
          continue;
        }
        logger.info("image found for call: " + m.from + ", attachment: " + key + ", size:" + bytes.length);
        return key;
      } catch (Exception e) {
        ;
      }
    }
    return null;
  }

  private void writeImage(ExportedMessage m, String imageFileName, byte[] bytes, boolean isImageSizeOk) {

    imageFileName = m.from + "-" + imageFileName;
    try {
      // write the file
      var allImagePath = Path.of(imageAllPath.toString(), imageFileName);
      Files.write(allImagePath, bytes);

      // create the link to pass or fail
      if (isImageSizeOk) {
        var passImagePath = Path.of(imagePassPath.toString(), imageFileName);
        Files.createLink(passImagePath, allImagePath);
      } else {
        var badSizePath = Path.of(imageBadSizePath.toString(), imageFileName);
        Files.createLink(badSizePath, allImagePath);
      }
    } catch (Exception e) {
      logger
          .error("Exception writing image file for call: " + m.from + ", messageId: " + m.messageId + ", "
              + e.getLocalizedMessage());
    }
  }

  @Override
  public String[] getHeaders() {
    return null;
  }

  @Override
  public String[] getValues(AggregateMessage message) {
    return null;
  }

}
