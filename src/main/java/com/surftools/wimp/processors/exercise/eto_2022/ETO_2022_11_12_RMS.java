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

package com.surftools.wimp.processors.exercise.eto_2022;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2022-11-12 DRILL: one FSR and one ICS-213.
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
public class ETO_2022_11_12_RMS extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_11_12_RMS.class);

  private static final String REQUIRED_HEADER_TEXT = "EmComm Training Organization 1112 THIS IS A DRILL";

  private List<IWritableTable> passList = new ArrayList<>(); // both messages, no failures
  private List<IWritableTable> failList = new ArrayList<>(); // one message OR automatic failure

  private Path imageAllPath;
  private List<Path> imageGoodPaths; // list of paths to link when images is "good"
  private List<Path> imageBadPaths; // lists of paths to link when image is "bad"

  private int maxImageSize = -1;

  static record Result(String call, String to, LatLongPair location, //
      String fsrClearinghouse, String fsrMessageId, String fsrComment, //
      String icsClearinghouse, String icsMessageId, String icsComment, int icsImageSize, //
      String grade, String explanation) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "To", "Latitude", "Longitude", //
          "FsrTo", "FsrMiD", "FsrComment", //
          "IcsTo", "IcsMiD", "IcsMessage", "IcsImageBytes", //
          "Grade", "Explanation" };
    }

    @Override
    public String[] getValues() {
      var aLocation = location != null ? location : LatLongPair.ZERO_ZERO;
      return new String[] { call, to, aLocation.getLatitude(), aLocation.getLongitude(), //
          fsrClearinghouse, fsrMessageId, fsrComment, //
          icsClearinghouse, icsMessageId, icsComment, String.valueOf(icsImageSize), //
          grade, explanation };
    }

    public Result updateLocation(LatLongPair newLocation) {
      return new Result(call, to, newLocation, //
          fsrClearinghouse, fsrMessageId, fsrComment, //
          icsClearinghouse, icsMessageId, icsComment, icsImageSize, //
          grade, explanation);
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      var cmp = call.compareTo(o.call);
      if (cmp != 0) {
        return cmp;
      }
      return to.compareTo(o.to);
    }
  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var imageOutputPath = Path.of(outputPathName, "image");
    FileUtils.deleteDirectory(imageOutputPath);

    FileUtils.deleteDirectory(Path.of(outputPathName, "image"));

    imageAllPath = Path.of(outputPathName, "image", "all");
    FileUtils.createDirectory(imageAllPath);

    var imageGoodPath = Path.of(outputPathName, "image", "pass");
    imageGoodPaths = List.of(imageGoodPath);
    FileUtils.createDirectory(imageGoodPath);

    var imageBadPath = Path.of(outputPathName, "image", "badSize");
    imageBadPaths = List.of(imageBadPath);
    FileUtils.createDirectory(imageBadPath);
    maxImageSize = cm.getAsInt(Key.IMAGE_MAX_SIZE, 5500);
  }

  @Override
  public void process() {

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

    var it = mm.getSenderIterator();
    while (it.hasNext()) {
      var from = it.next();
      if (dumpIds.contains(from)) {
        logger.info("dump: " + from);
      }

      var map = mm.getMessagesForSender(from);
      var fsrMessage = getFsrMessage(map);
      var icsMessage = getIcsMessage(map);
      if (fsrMessage == null && icsMessage == null) {
        logger.info("skipping messages from " + from + ", since no fsr or ics");
        continue;
      }

      ++ppCount;

      var isFailure = false; // either message missing or auto-fail

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
        fsrLocation = fsrMessage.formLocation;
        if (!fsrLocation.isValid()) {
          isFailure = true;
          ++ppFailNoLocationCount;
          explanations.add("FAIL because FSR location(" + fsrLocation.toString() + ") is not valid");
          fsrLocation = LatLongPair.ZERO_ZERO;
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
        icsComment = icsMessage.formMessage;
        icsLocation = icsMessage.mapLocation;

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

            if (bytes.length <= maxImageSize) {
              ++ppIcsImageSizeOk;
              points += 25;
              explanations.add("extra credit for attached image");
              writeContent(bytes, imageFileName, imageAllPath, imageGoodPaths);
            } else {
              explanations.add("no extra credit too large image");
              writeContent(bytes, imageFileName, imageAllPath, imageBadPaths);
            }
          }
        } else {
          explanations.add("no optional image attachment on ICS found");
        }

      }

      // only want folks who sent at least one of the right type of messages
      if (fsrMessage != null && icsMessage != null) {
        if (!fsrClearinghouse.equalsIgnoreCase(icsClearinghouse)) {
          isFailure = true;
          ++ppFailDifferentClearingHousesCount;
          explanations.add("FAIL because clearinghouses are different");
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

      var location = fsrLocation;
      if (location == null) {
        if (icsLocation != null && icsLocation.isValid()) {
          location = icsLocation;
        }
        location = LatLongPair.ZERO_ZERO;
      }

      var to = fsrClearinghouse != null ? fsrClearinghouse : icsClearinghouse;
      var entry = new Result(from, to, location, //
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
    sb.append("\n\nETO 2022-11-12 RMS aggregate results:\n");
    sb.append("total participants: " + ppCount + "\n");

    sb.append("participants with both required messages (may also fail): " + ppPassCount + "\n");
    sb.append(formatPP("  FSR received", ppFsrReceived, ppCount));
    sb.append(formatPP("  FSR setup Ok", ppFsrSetupOk, ppCount));

    sb.append(formatPP("  ICS received", ppIcsReceived, ppCount));
    sb.append(formatPP("  ICS setup Ok", ppIcsSetupOk, ppCount));
    sb.append(formatPP("  ICS image present", ppIcsImageAttachedOk, ppCount));
    sb.append(formatPP("  ICS image size Ok", ppIcsImageSizeOk, ppCount));

    sb.append("\nparticipants with automatic fails: " + ppFailCount + "\n");
    sb.append(formatPP("  FAIL because no FSR", ppFailNoFsrCount, ppFailCount));
    sb.append(formatPP("  FAIL because no ICS", ppFailNoIcsCount, ppFailCount));
    sb.append(formatPP("  FAIL because FSR Box 1", ppFailFcsBox1Count, ppFailCount));
    sb.append(formatPP("  FAIL because Location invalid", ppFailNoLocationCount, ppFailCount));
    sb.append(formatPP("  FAIL because different clearinghouses", ppFailDifferentClearingHousesCount, ppFailCount));

    var scores = new ArrayList<Integer>(ppScoreCountMap.keySet());
    Collections.sort(scores, Comparator.reverseOrder());
    sb.append("\nscores: \n");
    for (int score : scores) {
      var count = ppScoreCountMap.get(score);
      sb.append(" score: " + score + ", count: " + count + "\n");
    }

    logger.info(sb.toString());

    WriteProcessor.writeTable(makeAllList(passList, failList), Path.of(outputPathName.toString(), "aggregate-all.csv"));
    WriteProcessor.writeTable(passList, Path.of(outputPathName.toString(), "aggregate-pass.csv"));
    WriteProcessor.writeTable(failList, Path.of(outputPathName.toString(), "aggregate-fail.csv"));
  }

  private FieldSituationMessage getFsrMessage(Map<MessageType, List<ExportedMessage>> map) {
    var list = map.get(MessageType.FIELD_SITUATION);
    if (list != null) {
      Collections.reverse(list);
      var fsrMessage = (FieldSituationMessage) list.iterator().next();
      return fsrMessage;
    } else {
      return null;
    }
  }

  private Ics213Message getIcsMessage(Map<MessageType, List<ExportedMessage>> map) {
    var list = map.get(MessageType.ICS_213);
    if (list != null) {
      Collections.reverse(list);
      var icsMessage = (Ics213Message) list.iterator().next();
      return icsMessage;
    } else {
      return null;
    }
  }

  private List<IWritableTable> makeAllList(List<IWritableTable> passList, List<IWritableTable> failList) {
    var tmpList = new ArrayList<IWritableTable>(passList);
    tmpList.addAll(failList);

    return tmpList;
  }

  private String getImageFile(ExportedMessage m) {
    for (String key : m.attachments.keySet()) {
      var bytes = m.attachments.get(key);
      if (areBytesAnImage(bytes)) {
        logger.debug("image found for call: " + m.from + ", attachment: " + key + ", size:" + bytes.length);
        return key;
      }
    }
    return null;
  }

}
