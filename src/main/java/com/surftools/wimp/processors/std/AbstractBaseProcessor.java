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

package com.surftools.wimp.processors.std;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;

import com.opencsv.CSVWriter;
import com.surftools.utils.FileUtils;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;

public abstract class AbstractBaseProcessor implements IProcessor {
  protected Logger logger;

  protected IConfigurationManager cm;
  protected IMessageManager mm;

  protected Set<String> dumpIds;
  protected String pathName;
  protected String outputPathName;
  protected Path outputPath;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

  }

  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger logger) {
    this.logger = logger;
    doInitialization(cm, mm);
  }

  @SuppressWarnings("unchecked")
  protected void doInitialization(IConfigurationManager cm, IMessageManager mm) {
    this.cm = cm;
    this.mm = mm;

    pathName = cm.getAsString(Key.PATH);
    outputPath = Path.of(pathName.toString(), "output");
    outputPathName = outputPath.toString();

    dumpIds = (Set<String>) mm.getContextObject("dumpIds");
    if (dumpIds == null) {
      dumpIds = new HashSet<>();
    }
  }

  @Override
  public abstract void process();

  protected String formatPercent(Double d) {
    if (d == null) {
      return "";
    }

    return String.format("%.2f", 100d * d) + "%";
  }

  protected String formatPP(String label, int okCount, int ppCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
  }

  @SuppressWarnings("rawtypes")
  protected String formatCounter(Iterator<Entry<Comparable, Integer>> it, String keyLabel, String countLabel) {
    return formatCounter(it, keyLabel, countLabel, Integer.MAX_VALUE);
  }

  @SuppressWarnings("rawtypes")
  protected String formatCounter(Iterator<Entry<Comparable, Integer>> it, String keyLabel, String countLabel,
      int maxItems) {
    var sb = new StringBuilder();
    int count = 0;
    while (it.hasNext()) {
      var entry = it.next();
      sb.append(" " + keyLabel + ": " + entry.getKey() + ", " + countLabel + ": " + entry.getValue() + "\n");
      ++count;
      if (count == maxItems) {
        break;
      }
    }
    return sb.toString();
  }

  protected void writeTable(String fileName, List<IWritableTable> entries) {
    Path outputPath = Path.of(pathName, "output", fileName);
    FileUtils.makeDirIfNeeded(outputPath.toString());

    var messageCount = 0;
    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));

      if (entries.size() > 0) {
        writer.writeNext(entries.get(0).getHeaders());
        for (IWritableTable e : entries) {
          if (e != null) {
            var values = e.getValues();
            if (values != null) {
              writer.writeNext(values);
              ++messageCount;
            }
          } else {
            continue;
          }
        }
      } else {
        writer.writeNext(new String[] { "No Data" });
      }

      writer.close();
      logger.info("wrote " + messageCount + " results to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * write content, typically an image file
   *
   * @param bytes
   *          -- bytes to be written
   * @param fileName
   *          -- of file to be written
   * @param path
   *          -- of output dir, must exist
   * @param linkPaths
   *          -- optional, if present, list of directory paths where file will be linked to
   */
  protected void writeContent(byte[] bytes, String fileName, Path path, List<Path> linkPaths) {

    try {
      // write the file
      var filePath = Path.of(path.toString(), fileName);
      Files.write(filePath, bytes);

      // create links
      if (linkPaths != null) {
        for (var linkDirPath : linkPaths) {
          var linkPath = Path.of(linkDirPath.toString(), fileName);
          Files.createLink(linkPath, filePath);
        }
      }
    } catch (Exception e) {
      logger.error("Exception writing file for:  " + fileName + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * return the name of the first attachment that represents an image
   *
   * @param m
   * @return
   */
  protected String getFirstImageFile(ExportedMessage m) {
    for (var entry : m.attachments.entrySet()) {
      var fileName = entry.getKey();
      var bytes = entry.getValue();
      if (areBytesAnImage(bytes)) {
        return fileName;
      }
    }
    return null;
  }

  /**
   * determine is a byte array represents an image
   *
   * @param bytes
   * @return
   */
  protected boolean areBytesAnImage(byte[] bytes) {
    try {
      var bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
      return bufferedImage != null;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * return the most common messageType
   *
   * @return
   */
  protected MessageType getMostCommonMessageType() {
    var it = mm.getMessageTypeIteror();
    var maxCount = -1;
    MessageType maxMessageType = null;
    while (it.hasNext()) {
      var messageType = it.next();
      var list = mm.getMessagesForType(messageType);
      if (list.size() > maxCount) {
        maxCount = list.size();
        maxMessageType = messageType;
      }
    }
    return maxMessageType;
  }

}