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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * NEARBY Winlink Catalog Request Mapping Challenge
 *
 * Send a Position Report, Generate a WL2K_NEARBY Catalog Request
 *
 * Retrieve Catalog Request - create a text file, plot using Express
 *
 * Send Winlink Check In Message with text and image
 *
 * @author bobt
 *
 */
public class ETO_2022_12_15 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_12_15.class);

  private final int MAX_ATTACHMENT_SIZE = 5120;

  private Path gradedPath;
  private String gradedPathName;

  private Path imageAllPath;
  private Path imagePassPath;
  private Path imageBadNamePath;
  private Path imageBadSizePath;

  private Path textAllPath;
  private Path textPassPath;
  private Path textBadNamePath;
  private Path textBadContentPath;

  private List<IWritableTable> allPositionReports;

  private boolean doMatchResults = true;
  private StringBuilder mr = new StringBuilder(); // match results

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    allPositionReports = new ArrayList<>();

    gradedPath = Path.of(outputPathName, "graded");
    gradedPathName = gradedPath.toString();
    FileUtils.deleteDirectory(gradedPath);

    imageAllPath = FileUtils.createDirectory(Path.of(gradedPathName, "image", "all"));
    imagePassPath = FileUtils.createDirectory(Path.of(gradedPathName, "image", "pass"));
    imageBadNamePath = FileUtils.createDirectory(Path.of(gradedPathName, "image", "badName"));
    imageBadSizePath = FileUtils.createDirectory(Path.of(gradedPathName, "image", "badSize"));

    textAllPath = FileUtils.createDirectory(Path.of(gradedPathName, "text", "alll"));
    textPassPath = FileUtils.createDirectory(Path.of(gradedPathName, "text", "pass"));
    textBadNamePath = FileUtils.createDirectory(Path.of(gradedPathName, "text", "badName"));
    textBadContentPath = FileUtils.createDirectory(Path.of(gradedPathName, "text", "badContent"));

  }

  @Override
  /**
   * Alternate Scoring – proposed by Bob, not yet approved by Brian
   *
   * Attached JPEG file (40 points)
   *
   * JPEG present – 10 points
   *
   * JPEG correctly named - <YOURCALL> Position Report 09-29-22.jpeg – 10 points
   *
   * JPEG size less than or equal to 5 kbytes – 20 points
   *
   * Attached TXT file (40 points)
   *
   * TXT file present (and not FormData.txt) – 10 points
   *
   * TXT file correctly named <YOURCALL> Position Report 09-29-2022.txt – 10 points
   *
   * TXT file has 31 records (header and 30 amateur radio callsigns) – 10 points
   *
   * TXT file line 2 (<YOURCALL>) has comment Winlink Thursday 09/29/2022 Challenge – 10 points
   *
   * Form is a correctly populated Winlink Check-In template as specified in step 8. – 20 points
   *
   * Agency/Group name (step 8.2.2) is ETO Winlink Thursday – 10 points
   *
   * Comments match the count from step 2.1.6 – 10 points (Step 8.2.5) – 10 points
   *
   */
  public void process() {
    int ppCount = 0;

    int ppImagePresentOk = 0;
    int ppImageSizeOk = 0;
    int ppImageNameOk = 0;

    int ppTextPresentOk = 0;
    int ppTextContentOk = 0;
    int ppTextNameOk = 0;
    int ppTextLine1CallOk = 0;
    int ppTextLine1CommentOk = 0;

    int ppOrganizationOk = 0;
    int ppCommentOk = 0;

    Counter pointsCounter = new Counter();
    Counter commentMatchCounter = new Counter();
    Counter ppEtoNeighborCounter = new Counter();
    Counter ppAllNeighborCounter = new Counter();

    var results = new ArrayList<IWritableTable>();
    for (var gm : mm.getMessagesForType(MessageType.CHECK_IN)) {

      CheckInMessage m = (CheckInMessage) gm;

      if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
        logger.info("ETO_2022_12_15 grader: " + m);
      }

      ++ppCount;
      var points = 0;
      var explanations = new ArrayList<String>();
      var commentMatchCount = 0;

      var imageFileName = getFirstImageFile(m);
      if (imageFileName != null) {
        var bytes = m.attachments.get(imageFileName);
        ++ppImagePresentOk;
        points += 10;

        var isImageNameOk = false;
        var requiredImageFileName = m.from + " Position Report 12-15-2022.jpeg";
        var altRequiredImageFileName = requiredImageFileName.replaceAll("jpeg", "jpg");
        var requiredImageFileNameSet = new HashSet<String>();
        requiredImageFileNameSet.add(requiredImageFileName.toUpperCase());
        requiredImageFileNameSet.add(altRequiredImageFileName.toUpperCase());
        // if (imageFileName.equalsIgnoreCase(requiredImageFileName)) {
        if (requiredImageFileNameSet.contains(imageFileName.toUpperCase())) {
          ++ppImageNameOk;
          points += 10;
          isImageNameOk = true;
        } else {
          explanations.add("image file name: " + imageFileName + " != " + requiredImageFileName);
        }

        var isImageSizeOk = false;
        if (bytes.length <= MAX_ATTACHMENT_SIZE) {
          ++ppImageSizeOk;
          points += 20;
          isImageSizeOk = true;
        } else {
          explanations.add("image attachment larger than " + MAX_ATTACHMENT_SIZE + " bytes");
        }

        writeImage(m, imageFileName, bytes, isImageNameOk, isImageSizeOk);
      } else {
        explanations.add("no image attachment found");
      }

      var textFileName = getTextFile(m);
      if (textFileName != null) {
        ++ppTextPresentOk;
        points += 10;

        var isTextNameOk = false;
        var requiredTextFileName = m.from + " Position Report 12-15-2022.txt";
        if (textFileName.equalsIgnoreCase(requiredTextFileName)) {
          ++ppTextNameOk;
          points += 10;
          isTextNameOk = true;
        } else {
          explanations.add("text file name: " + textFileName + " != " + requiredTextFileName);
        }

        var bytes = m.attachments.get(textFileName);
        var content = new String(bytes);
        var list = parse(m, content);

        var isTextContentOk = false;
        // var originalRequiredContent = "ETO Winlink Thursday 12/15/2022 Challenge Exercise";
        var modifiedRequiredContent = "ETO Winlink Thursday 12/15/2022";
        // var altRequiredContent = "ETO Winlink Thursday";
        var requiredContent = modifiedRequiredContent.toUpperCase();

        if (list.size() > 0) {
          allPositionReports.addAll(list);
          var firstPR = list.get(0);
          var prCall = firstPR.to;
          var prComments = firstPR.comments;

          var isLine1CallOk = m.from.equalsIgnoreCase(prCall);
          var isLine1ContentOk = prComments.toUpperCase().startsWith(requiredContent);

          if (isLine1CallOk) {
            ++ppTextLine1CallOk;
          }

          if (isLine1ContentOk) {
            ++ppTextLine1CommentOk;
          }

          if (isLine1CallOk && isLine1ContentOk) {
            points += 10;
            isTextContentOk = true;
          } else {
            if (!isLine1CallOk && !isLine1ContentOk) {
            } else if (!isLine1CallOk) {
              explanations.add("Line 1 of Position Report should include <YOURCALL>");
            } else if (!isLine1ContentOk) {
              explanations.add("Line 1 of Position Report doesn't have required comment: " + requiredContent);
            }
          }

          if (list.size() == 30) {
            points += 10;
            ++ppTextContentOk;
          } else {
            explanations.add("expected to find 30 Position Reports in text, found " + list.size());
          }

          commentMatchCount = (int) list
              .stream()
                .filter(o -> o.comments.toUpperCase().startsWith(requiredContent))
                .count();
          commentMatchCounter.increment(commentMatchCount);

          if (doMatchResults) {
            var messageComment = m.comments;
            var reportedCount = 0;
            if (messageComment != null) {
              try {
                reportedCount = Integer.parseInt(messageComment.trim());
              } catch (Exception e) {
                ;
              }
            }

            mr.append("\ncall: " + m.from + //
                ", reported Count: " + reportedCount + ", actual Count: " + commentMatchCount + "\n");
            var lineNumber = 0;
            var matchCount = 0;
            for (var l : list) {
              ++lineNumber;
              var isMatched = l.comments.toUpperCase().startsWith(requiredContent);
              if (isMatched) {
                ++matchCount;
              }
              mr.append("   ");
              mr.append(isMatched ? String.format("(%-2d)", matchCount) : "    ");
              mr.append(" line: " + ((lineNumber <= 9) ? " " : "") + lineNumber);
              mr.append(", call: " + String.format("%-9s", l.to + ", "));
              mr.append("comment: " + l.comments);
              mr.append("\n");
            }
            mr.append("\n");
          }

          // to find the reportees that have the most neighbors
          for (var pr : list) {
            var isEto = pr.comments().toUpperCase().startsWith(requiredContent);
            if (isEto) {
              ppEtoNeighborCounter.increment(pr.to);
            }
            ppAllNeighborCounter.increment(pr.to);
          }
        }

        writeText(m, textFileName, bytes, isTextNameOk, isTextContentOk);
      } else {
        explanations.add("no text attachment found");
      }

      var organization = m.organization;
      var requiredOrganization = "ETO Winlink Thursday 12/15/2022 Challenge Exercise";
      if (organization != null && organization.equalsIgnoreCase(requiredOrganization)) {
        ++ppOrganizationOk;
        points += 10;
      } else {
        explanations.add("group/agency not '" + requiredOrganization + "'");
      }

      var comments = m.comments;
      if (comments != null) {
        comments = comments.trim();
        if (comments.isEmpty()) {
          explanations.add("no comments, should be: " + commentMatchCount);
        } else {
          try {
            var commentCount = Integer.valueOf(comments);
            if (commentCount == commentMatchCount) {
              ++ppCommentOk;
              points += 10;
            } else {
              explanations.add("comments should be: " + commentMatchCount + ", not " + commentCount);
            }
          } catch (Exception e) {
            explanations.add("comments should be: " + commentMatchCount + ", not " + comments);
          }
        }
      } else {
        explanations.add("no comments, should be: " + commentMatchCount);
      }

      points = Math.min(100, points);
      points = Math.max(0, points);
      pointsCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over messages

    writeTable("positionReports.csv", allPositionReports);
    WriteProcessor.writeCounter(ppEtoNeighborCounter, Path.of(outputPathName, "eto-neighbors.csv"));
    WriteProcessor.writeCounter(ppAllNeighborCounter, Path.of(outputPathName, "all-neighbors.csv"));

    if (doMatchResults) {
      writeContent(mr.toString().getBytes(), "match-results.txt", Path.of(outputPathName), null);
    }

    var sb = new StringBuilder();
    sb.append("\nETO-2022-12-15 Grading Report: graded " + ppCount + " Winlink Check In messages\n");

    sb.append(formatPP("agency/group name", ppOrganizationOk, ppCount));
    sb.append(formatPP("image attached", ppImagePresentOk, ppCount));
    sb.append(formatPP("image size", ppImageSizeOk, ppCount));
    sb.append(formatPP("image name", ppImageNameOk, ppCount));

    sb.append(formatPP("text report attached", ppTextPresentOk, ppCount));
    sb.append(formatPP("text name", ppTextNameOk, ppCount));
    sb.append(formatPP("text line1 call", ppTextLine1CallOk, ppCount));
    sb.append(formatPP("text line1 comment", ppTextLine1CommentOk, ppCount));
    sb.append(formatPP("text file content", ppTextContentOk, ppCount));
    sb.append(formatPP("Check-in comments count matches text count", ppCommentOk, ppCount));

    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));
    sb
        .append("\nCounts of nearby ETO participants: \n"
            + formatCounter(commentMatchCounter.getDescendingCountIterator(), "matches", "count"));

    logger.info(sb.toString());

    writeTable("graded-check_in.csv", results);
  }

  private List<PositionReport> parse(ExportedMessage m, String content) {
    content = content.trim();
    var list = new ArrayList<PositionReport>();
    if (!content.startsWith("CALL")) {
      logger.debug("could not parse text from " + m.from + ", content: " + content);
      return list;
    }

    var messageFrom = m.from;
    var lineCount = 0;
    var lines = content.split("\n");
    for (var line : lines) {
      ++lineCount;
      if (lineCount == 1) {
        continue;
      }
      line = line.trim();
      var tokens = line.split(" ");
      var fields = new ArrayList<String>();
      for (var token : tokens) {
        if (token.length() == 0) {
          continue;
        }
        fields.add(token);
      }

      // CALL Dist(nm @ DegT) POSITION REPORTED COMMENT
      // W7OWO 0.0 @ 000 45-17.80N 123-00.70W 2022/08/20 17:11 Winlink Thursday 09/29/2022 Challenge

      var from = messageFrom;

      if (fields.size() == 0) {
        continue;
      }
      var to = fields.get(0);
      var nauticalMilesString = fields.get(1);
      var statuteMilesString = "0";
      try {
        var nauticalMiles = Double.parseDouble(fields.get(1));
        var statuteMiles = (int) Math.round(1.15078 * nauticalMiles);
        statuteMilesString = String.valueOf(statuteMiles);
      } catch (Exception e) {
        logger
            .error("Exception parsing nautical miles for message from: " + messageFrom + " to " + to + ", value: "
                + nauticalMilesString);
      }

      var bearing = fields.get(3);
      var longitude = LocationUtils.convertToDecimalDegrees(fields.get(4));
      var latitude = LocationUtils.convertToDecimalDegrees(fields.get(5));
      var date = fields.get(6);
      var time = fields.get(7);

      var sb = new StringBuilder();
      for (int i = 8; i < fields.size(); ++i) {
        sb.append(fields.get(i) + " ");
      }
      var comment = sb.toString().trim();
      var pr = new PositionReport(from, to, statuteMilesString, bearing, latitude, longitude, date, time, comment);
      list.add(pr);
    } // end loop over lines
    return list;
  }

  private String getTextFile(CheckInMessage m) {
    var theImageFileName = getFirstImageFile(m);
    for (String key : m.attachments.keySet()) {

      if (key.equals(theImageFileName)) {
        continue;
      }

      if (key.equalsIgnoreCase("formData.txt")) {
        continue;
      }

      if (key.equalsIgnoreCase(MessageType.CHECK_IN.rmsViewerName())) {
        continue;
      }

      // if it's the last attachment, use it
      if (m.attachments.keySet().size() == 4) {
        return key;
      }

      var bytes = m.attachments.get(key);
      var content = new String(bytes);
      if (content.startsWith("CALL")) {
        return key;
      }
    }

    return null;

  }

  /**
   * write the image file, to the all/ directory
   *
   * write a link to pass, badName and/or badSize depending
   *
   * @param m
   * @param imageFileName
   * @param bytes
   * @param isImageNameOk
   * @param isImageSizeOk
   */
  private void writeImage(ExportedMessage m, String imageFileName, byte[] bytes, boolean isImageNameOk,
      boolean isImageSizeOk) {

    try {
      // write the file
      var allImagePath = Path.of(imageAllPath.toString(), imageFileName);
      Files.write(allImagePath, bytes);

      // create the link to pass or fail
      if (isImageNameOk && isImageSizeOk) {
        var passImagePath = Path.of(imagePassPath.toString(), imageFileName);
        Files.createLink(passImagePath, allImagePath);
      } else if (!isImageNameOk) {
        var badNamePath = Path.of(imageBadNamePath.toString(), imageFileName);
        Files.createLink(badNamePath, allImagePath);
      } else if (!isImageSizeOk) {
        var badSizePath = Path.of(imageBadSizePath.toString(), imageFileName);
        Files.createLink(badSizePath, allImagePath);
      }
    } catch (Exception e) {
      logger
          .error("Exception writing image file for call: " + m.from + ", messageId: " + m.messageId + ", "
              + e.getLocalizedMessage());
    }
  }

  /**
   * write the text file, to the all/ directory
   *
   * write a link to pass, badName and/or badContent depending
   *
   * @param m
   * @param textFileName
   * @param bytes
   * @param isTextNameOk
   * @param isTextContentOk
   */
  private void writeText(CheckInMessage m, String textFileName, byte[] bytes, boolean isTextNameOk,
      boolean isTextContentOk) {

    textFileName = m.from + " -- " + textFileName;
    try {
      // write the file
      var allTextPath = Path.of(textAllPath.toString(), textFileName);
      Files.write(allTextPath, bytes);

      // create the link to pass or fail
      if (isTextNameOk && isTextContentOk) {
        var passTextPath = Path.of(textPassPath.toString(), textFileName);
        Files.createLink(passTextPath, allTextPath);
      } else if (!isTextNameOk) {
        var badNamePath = Path.of(textBadNamePath.toString(), textFileName);
        Files.createLink(badNamePath, allTextPath);
      } else if (!isTextContentOk) {
        var badContentPath = Path.of(textBadContentPath.toString(), textFileName);
        Files.createLink(badContentPath, allTextPath);
      }
    } catch (Exception e) {
      logger
          .error("Exception writing image file for call: " + m.from + ", messageId: " + m.messageId + ", "
              + e.getLocalizedMessage());
    }

  }

  public static record PositionReport(String from, String to, String nauticalMiles, String bearing, String latitude,
      String longitude, String date, String time, String comments) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      return new String[] { //
          "From", "To", "NauticalMiles", "Bearing", "Latitude", "Longitude", //
          "Date", "Time", "Comments" };
    }

    @Override
    public String[] getValues() {
      return new String[] { //
          from, to, nauticalMiles, bearing, latitude, longitude, //
          date, time, comments };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (PositionReport) other;
      var cmp = from.compareTo(o.from);
      if (cmp != 0) {
        return cmp;
      }
      return to.compareTo(o.to);
    }
  }

}