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

package com.surftools.winlinkMessageMapper.grade.named;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.FileUtils;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.LocationUtils;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

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
public class ETO_2022_09_29 implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_09_29.class);

  private IConfigurationManager cm;

  private final int MAX_ATTACHMENT_SIZE = 5120;

  private Path outputPath;

  private Path imageAllPath;
  private Path imagePassPath;
  private Path imageBadNamePath;
  private Path imageBadSizePath;

  private Path textAllPath;
  private Path textPassPath;
  private Path textBadNamePath;
  private Path textBadContentPath;

  // for post processing
  private int ppCount;

  private int ppImagePresentOk;
  private int ppImageSizeOk;
  private int ppImageNameOk;

  private int ppTextPresentOk;
  private int ppTextContentOk;
  private int ppTextNameOk;

  private int ppCommentOk;
  private int ppOrganizationOk;

  private Set<String> dumpIds;

  private boolean isInitialized;
  private final List<PositionReport> allPositionReports;

  public ETO_2022_09_29() {
    // don't have a cm when we are constructed;
    isInitialized = false;
    allPositionReports = new ArrayList<>();
  }

  /**
   * we don't have access to our IConfigurationManager during construction, hence need to initialize
   */
  private void initialize() {
    outputPath = Path.of(cm.getAsString(Key.PATH), "output", "graded");
    FileUtils.deleteDirectory(outputPath);

    imageAllPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "image", "alll"));
    imagePassPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "image", "pass"));
    imageBadNamePath = FileUtils.createDirectory(Path.of(outputPath.toString(), "image", "badName"));
    imageBadSizePath = FileUtils.createDirectory(Path.of(outputPath.toString(), "image", "badSize"));

    textAllPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "text", "alll"));
    textPassPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "text", "pass"));
    textBadNamePath = FileUtils.createDirectory(Path.of(outputPath.toString(), "text", "badName"));
    textBadContentPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "text", "badContent"));

    isInitialized = true;
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
   * Comments are empty (Step 8.2.5) – 10 points
   *
   */
  public GradeResult grade(GradableMessage gm) {
    if (!isInitialized) {
      initialize();
      isInitialized = true;
    }

    if (!(gm instanceof CheckInMessage)) {
      return null;
    }

    CheckInMessage m = (CheckInMessage) gm;

    if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
      // logger.info("ETO_2022_09_29 grader: " + m);
    }

    ++ppCount;
    var points = 0;
    var explanations = new ArrayList<String>();

    var imageFileName = getImageFile(m);
    if (imageFileName != null) {
      var bytes = m.attachments.get(imageFileName);
      ++ppImagePresentOk;
      points += 10;

      var isImageNameOk = false;
      var requiredImageFileName = m.from + " Position Report 09-29-22.jpeg";
      if (imageFileName.equalsIgnoreCase(requiredImageFileName)) {
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
      var requiredTextFileName = m.from + " Position Report 09-29-22.txt";
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
      if (list.size() > 0) {
        allPositionReports.addAll(list);
        var pr = list.get(0);
        var comments = pr.comments;

        var requiredContent = "Winlink Thursday 09/29/2022 Challenge";
        if (comments.equalsIgnoreCase(requiredContent)) {
          points += 10;
          isTextContentOk = true;
        } else {
          explanations.add("required content: '" + requiredContent + "' not found as first comment");
        }

        if (list.size() == 30) {
          points += 10;
        } else {
          explanations.add("expected to find 30 Position Reports in text, only found " + list.size());
        }
      }

      writeText(m, textFileName, bytes, isTextNameOk, isTextContentOk);
    } else {
      explanations.add("no text attachment found");
    }
    var organization = m.organization;
    var requiredOrganization = "ETO Winlink Thursday";
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
        ++ppCommentOk;
        points += 10;
      } else {
        explanations.add("comments should not be present");
      }
    } else {
      ++ppCommentOk;
      points += 10;
    }

    points = Math.min(100, points);
    points = Math.max(0, points);
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

    gm.setIsGraded(true);
    gm.setGrade(grade);
    gm.setExplanation(explanation);

    return new GradeResult(grade, explanation);
  }

  private List<PositionReport> parse(ExportedMessage m, String content) {
    var list = new ArrayList<PositionReport>();
    if (!content.startsWith("CALL")) {
      logger.error("could not parse text from " + m.from + ", content: " + content);
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
    }
    return list;
  }

  private String getTextFile(CheckInMessage m) {
    var theImageFileName = getImageFile(m);
    for (String key : m.attachments.keySet()) {

      if (key.equals(theImageFileName)) {
        continue;
      }

      if (key.equalsIgnoreCase("formData.txt")) {
        continue;
      }

      if (key.equalsIgnoreCase(MessageType.CHECK_IN.attachmentName())) {
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

  private String formatPercent(Double d) {
    if (d == null) {
      return "";
    }

    return String.format("%.2f", 100d * d) + "%";
  }

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    if (messages == null || messages.size() == 0 || messages.get(0).getMessageType() != MessageType.CHECK_IN) {
      return null;
    }

    writeAllPositionReports(allPositionReports);

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);
    sb.append("\nETO-2022-09-29 Grading Report: graded " + ppCount + " Winlink Check In messages\n");
    sb.append(formatPP("image attached", ppImagePresentOk));
    sb.append(formatPP("image size", ppImageSizeOk));
    sb.append(formatPP("image name", ppImageNameOk));

    sb.append(formatPP("text report attached", ppTextPresentOk));
    sb.append(formatPP("text name", ppTextNameOk));
    sb.append(formatPP("text content", ppTextContentOk));

    sb.append(formatPP("agency/group name", ppOrganizationOk));
    sb.append(formatPP("no comments supplied", ppCommentOk));

    return sb.toString();
  }

  private void writeAllPositionReports(List<PositionReport> list) {
    var outputPath = Path.of(cm.getAsString(Key.PATH), "output", "graded", "positionReports.csv");

    try {
      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(PositionReport.getHeaders());
      for (var pr : list) {
        writer.writeNext(pr.getValues());
      }
      writer.close();
      logger.info("wrote " + list.size() + " positionReports to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
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

  private String formatPP(String label, int okCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = (double) okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
  }

  @Override
  public void setDumpIds(Set<String> dumpIds) {
    this.dumpIds = dumpIds;
  }

  @Override
  public void setConfigurationManager(IConfigurationManager cm) {
    this.cm = cm;
  }

  public static record PositionReport(String from, String to, String nauticalMiles, String bearing, String latitude,
      String longitude, String date, String time, String comments) {

    public static String[] getHeaders() {
      return new String[] { //
          "From", "To", "NauticalMiles", "Bearing", "Latitude", "Longitude", //
          "Date", "Time", "Comments" };
    }

    public String[] getValues() {
      return new String[] { //
          from, to, nauticalMiles, bearing, latitude, longitude, //
          date, time, comments };
    }
  }

}