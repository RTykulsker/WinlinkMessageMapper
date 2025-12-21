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

package com.surftools.wimp.processors.std;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class UploadProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(UploadProcessor.class);
  private String dateString = null;

  private String ftpLocalPathName;
  private Path ftpLocalPath;
  private boolean isInitialized = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    dateString = cm.getAsString(Key.EXERCISE_DATE);

    var allowFuture = cm.getAsBoolean(Key.PERSISTENCE_ALLOW_FUTURE, false);
    if (!allowFuture) {
      var exerciseDate = LocalDate.parse(dateString);
      var nowDate = LocalDate.now();
      if (exerciseDate.isAfter(nowDate)) {
        var message = "skipping upload because Exercise date: " + exerciseDate + " is in future of now: " + nowDate;
        logger.warn("### " + message);
        return;
      }
    }

    ftpLocalPathName = null;// cm.getAsString(Key.PRACTICE_PATH_UPLOAD_FTP_LOCAL);
    ftpLocalPath = Path.of(ftpLocalPathName);
    var ftpLocalDir = ftpLocalPath.toFile();
    if (!ftpLocalDir.exists()) {
      logger.error("FTP local dir: " + ftpLocalPathName + " doesn not exist");
      System.exit(1);
    }
    logger.info("ftpLocalPath: " + ftpLocalPath);

    isInitialized = true;
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    if (!isInitialized) {
      logger.info("NOT initiized. Nothing uploaded");
      return;
    }

    uploadToTemporary();
  } // end postProcess

  private void uploadToTemporary() {
    // copy the map, chart and summary files to the ftp content/year/date dir
    var thisDate = LocalDate.parse(dateString);
    var thisYear = String.valueOf((thisDate.getYear()));
    var contentPath = Path.of(ftpLocalPath.toString(), thisYear, dateString);
    FileUtils.makeDirIfNeeded(contentPath);
    var baseNames = List.of("map.html", "chart.html", "summary.csv");
    for (var baseName : baseNames) {
      var sourcePath = Path.of(outputPathName, dateString + "-" + baseName);
      var targetPath = Path.of(contentPath.toString(), dateString + "-" + baseName);
      try {
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        logger.info("copied " + baseName + " to " + targetPath);
      } catch (Exception e) {
        logger
            .error("Exception copying " + baseName + " to ftp content dir: " + contentPath.toString() + ", "
                + e.getLocalizedMessage());
      }
    } // end loop over files

    // copy index.html to backup dir
    var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    LocalDateTime now = LocalDateTime.now();
    var timestamp = now.format(formatter);
    var sourcePath = Path.of(ftpLocalPathName, "index.html");
    var targetPath = Path.of(ftpLocalPathName, "backup", "index-" + timestamp + ".html");
    try {
      Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
      logger.info("copied index.html  to " + targetPath);
    } catch (Exception e) {
      logger.error("Exception copying index.html to backup: " + targetPath.toString() + ", " + e.getLocalizedMessage());
    }

    // read index fix and edit it
    var nextDate = thisDate.plusWeeks(1);
    var nextDateString = nextDate.toString();
    var outputLines = new StringBuilder();
    var editPath = Path.of(ftpLocalPathName, "index.html");
    final var NL = "\n";
    try {
      var inputLines = Files.readAllLines(editPath);
      for (var line : inputLines) {

        // turn off highlighting for current week
        var expectedTR = "<tr id=\"" + dateString + "-row\" class=\"highlight-row\">";
        if (line.equals(expectedTR)) {
          line = "<tr id=\"" + dateString + "-row\" class=\"normal-row\">";
          outputLines.append(line + NL);
          continue;
        }

        // expand content
        var expectedTD = "<td id=\"" + dateString + "-content\"></td>";
        if (line.equals(expectedTD)) {
          var sb = new StringBuilder();
          final var format = "<a href=\"content/" + thisYear + "/" + dateString + "/" + dateString + "-%s\">%s</a>";
          sb.append("<td id=\"" + dateString + "-content\">" + NL);
          sb.append(String.format(format, "map.html", "map") + NL);
          sb.append("<span>/</span>" + NL);
          sb.append(String.format(format, "chart.html", "chart") + NL);
          sb.append("<span>/</span>" + NL);
          sb.append(String.format(format, "summary.csv", "table") + NL);
          sb.append("</td>" + NL);
          outputLines.append(sb.toString());
          continue;
        }

        // turn on highlighting for next week
        expectedTR = "<tr id=\"" + nextDateString + "-row\" class=\"normal-row\">";
        if (line.equals(expectedTR)) {
          line = "<tr id=\"" + nextDateString + "-row\" class=\"highlight-row\">";
          outputLines.append(line + NL);
          continue;
        }

        outputLines.append(line + NL);
      }
      Files.writeString(editPath, outputLines.toString());
    } catch (Exception e) {
      logger.error("Exception editing index.html: " + editPath.toString() + ", " + e.getLocalizedMessage());
    }

  } // end uploadToTemporary

}
