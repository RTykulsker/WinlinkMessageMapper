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

package com.surftools.wimp.processors.std.baseExercise;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.ICounter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.service.outboundMessage.OutboundMessage;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBaseProcessor implements IProcessor {
  protected static Logger logger;

  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter ALT_DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING.replaceAll("-", "/"));
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static IConfigurationManager cm;
  protected static IMessageManager mm;

  protected static Set<String> dumpIds;
  protected static String pathName;
  protected static String outputPathName;
  protected static Path outputPath;

  protected static List<OutboundMessage> outboundMessageList;
  protected static String outboundMessageSender;
  protected static String outboundMessageSubject;
  protected static boolean doOutboundMessaging;

  protected static boolean isInitialized = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    initialize(cm, mm, LoggerFactory.getLogger(AbstractBaseProcessor.class));
  }

  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    logger = _logger;
    if (!isInitialized) {
      doInitialization(cm, mm);
      isInitialized = true;
    }
  }

  @SuppressWarnings("unchecked")
  protected void doInitialization(IConfigurationManager _cm, IMessageManager _mm) {
    cm = _cm;
    mm = _mm;

    pathName = cm.getAsString(Key.PATH);

    // allow overriding of outputPathName!
    outputPathName = cm.getAsString(Key.OUTPUT_PATH);
    if (outputPathName == null) {
      outputPath = Path.of(pathName.toString(), "output");
      outputPathName = outputPath.toString();
    } else {
      outputPath = Path.of(outputPathName);
    }

    if (cm.getAsBoolean(Key.OUTPUT_PATH_CLEAR_ON_START, true)) {
      FileUtils.deleteDirectory(outputPath);
    }
    FileUtils.makeDirIfNeeded(outputPath.toString());

    dumpIds = (Set<String>) mm.getContextObject("dumpIds");
    if (dumpIds == null) {
      dumpIds = new HashSet<>();
    }

    outboundMessageSender = cm.getAsString(Key.OUTBOUND_MESSAGE_SENDER);
    outboundMessageSubject = cm.getAsString(Key.OUTBOUND_MESSAGE_SUBJECT);
    doOutboundMessaging = outboundMessageSender != null && outboundMessageSubject != null
        && !outboundMessageSender.isEmpty() && !outboundMessageSubject.isEmpty();
    outboundMessageList = new ArrayList<>();
  }

  @Override
  public abstract void process();

  @Override
  public void postProcess() {
  }

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

  protected String formatPP(String label, int count, boolean invert, int N) {
    var value = invert ? N - count : count;
    return formatPP("  " + label, value, N);
  }

  protected String formatField(FormFieldManager ffm, String key, boolean invert, int N) {
    var field = ffm.get(key);
    var value = invert ? N - field.count : field.count;
    // return (value == N) ? "" : formatPP(" " + field.label, value, N);
    return formatPP("  " + field.label, value, N);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  protected String formatCounter(String label, ICounter counter) {
    return ("\n" + label + ":\n" + formatCounter(counter.getDescendingCountIterator(), "value", "count"));
  }

  protected String formatCounter(FormFieldManager ffm, String key) {
    var field = ffm.get(key);
    return "\n" + field.label + ":\n" + formatCounter(field.counter.getDescendingCountIterator(), "value", "count");
  }

  public static void writeTable(String pathName, String fileName, List<IWritableTable> entries) {
    var myDirPath = FileUtils.makeDirIfNeeded(pathName);
    var myFilePath = Path.of(myDirPath.toString(), fileName);
    var messageCount = 0;
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(myFilePath.toString()));
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
      logger.info("wrote " + messageCount + " results to file: " + myFilePath.toString());
    } catch (Exception e) {
      logger.error("Exception writing file: " + myFilePath.toString() + ", " + e.getLocalizedMessage());
    }
  }

  public static void writeTable(String fileName, List<IWritableTable> entries) {
    writeTable(outputPathName, fileName, entries);
  }

  public static void writeTable(String fileName, Collection<? extends IWritableTable> entries) {
    var list = new ArrayList<IWritableTable>();
    list.addAll(entries);
    writeTable(outputPathName, fileName, list);
  }

  static record EntryRecord(String key, String value) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable o) {
      return 0;
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Name", "Value" };
    }

    @Override
    public String[] getValues() {
      return new String[] { key, value };
    }

    public static List<IWritableTable> mapToList(Map<String, String> map) {
      var list = new ArrayList<IWritableTable>();
      for (var entry : map.entrySet()) {
        list.add(new EntryRecord(entry.getKey(), entry.getValue()));
      }
      return list;
    }

  }

  protected void writeTable(String fileName, Map<String, String> map) {
    writeTable(outputPathName, fileName, EntryRecord.mapToList(map));
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

  public boolean startsWithAnyOf(String needle, List<String> list) {
    if (needle == null) {
      return false;
    }

    for (var s : list) {
      if (s == null) {
        return false;
      }

      if (needle.toLowerCase().startsWith(s.toLowerCase())) {
        return true;
      }
    }

    return false;
  }

  public static final String OB_DISCLAIMER = """
      =====================================================================================================

      DISCLAIMER: This feedback is provided for your consideration. We use the results to improve future
      exercises. Differences in spelling, numbers or omitting whitespace  will trigger this automated message.
      Differences in capitalization, punctuation and extra whitespace are generally ignored. You may
      think that some of our feedback is "nit picking" and that your responses would be understood by any
      reasonable person -- and you'd be correct! You're welcome to disagree with any or all of our feedback.
      You're also welcome to reply via Winlink to this message or send an email to
      ETO.Technical.Team@emcomm-training.groups.io. In any event, thank you for participating
      in this exercise. We look forward to seeing you at our next Winlink Thursday Exercise!
      """;

  public String getNagString(int year, int textId) {
    final String text_2023 = """

        =====================================================================================================

        ETO needs sponsors to be able to renew our groups.io subscription for $YEAR.
        By sponsoring this group, you are helping pay the Groups.io hosting fees.
        Here is the link to sponsor our group:  https://emcomm-training.groups.io/g/main/sponsor
        Any amount you sponsor will be held by Groups.io and used to pay hosting fees as needed.
        The minimum sponsorship is $5.00.

        Thank you for your support!

        """;

    final String text_2024 = """

        =========================================================================================================

         Please consider supporting ETO operations by making a cash donation and/or sponsoring
         our groups.io site. Our goal for cash donations is $500 which would be used to cover
         the hosting fees for our website, annual governmental fees, bank fees and a post office box.
         The goal for sponsorship of our groups.io site is $220 which covers the fees for 2025.

         You can make cash donations on our website by using this link:
             https://emcomm-training.org/Donate.html
         Note: Cash donations are tax-deductible as allowed by law.

         You can use this link to sponsor our group:
             https://emcomm-training.groups.io/g/main/sponsor
         Note:  Sponsorships are not tax deductible since the money is paid to groups.io.
         The minimum sponsorship is $5.00.

         If you want to donate using another payment method, please send an email to
         emcommtrainingorg@gmail.com.

         Thank you for your support !!!!!!

         EmComm Training Organization is a nonprofit, tax-exempt charitable organization
         (tax ID number 92-2282844) under Section 501(c)(3) of the Internal Revenue Code.
         Donations are tax-deductible as allowed by law.

         """;

    final var texts = new String[] { null, text_2023, text_2024 };
    var text = texts[textId];
    var result = text.replaceAll("\\$YEAR", String.valueOf(year));
    return result;
  }

  public static final String OB_REQUEST_FEEDBACK = """

      =====================================================================================================

      ETO would love to hear from you! Would you please take a few minutes to answer the following questions:
      1. Were the exercise instructions clear? If not, where did they need improvement?
      2. Did you find the exercise useful?
      3. Did you find the above feedback useful?
      4. What did you dislike about the exercise?
      5. Any additional comments?

      Please reply to this Winlink message or to ETO.Technical.Team@EmComm-Training.groups.io. Thank you!
      """;

}
