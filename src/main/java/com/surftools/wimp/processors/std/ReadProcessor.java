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

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Reads an "exported message" file, produced by Winlink, creates @{ExportedMessage} records
 *
 * @author bobt
 *
 */
public class ReadProcessor extends BaseReadProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ReadProcessor.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override
  public void process() {
    super.process();

    Path path = Paths.get(pathName);

    // read all Exported Messages from files
    List<ExportedMessage> exportedMessages = new ArrayList<>();
    for (File file : Arrays.asList(path.toFile().listFiles()).stream().sorted().toList()) {
      if (file.isFile()) {
        if (!file.getName().toLowerCase().endsWith(".xml")) {
          continue;
        }
        var fileExportedMessages = readAll(file.toPath());
        exportedMessages.addAll(fileExportedMessages);
      }
    }
    logger.info("read " + exportedMessages.size() + " exported messages from all files");

    mm.load(exportedMessages);
  }

  /**
   * reads a single file (from a clearinghouse), returns a list of ExportedMessage records
   *
   * @param filePath
   * @return
   */
  public List<ExportedMessage> readAll(Path filePath) {
    logger.debug("Processing file: " + filePath.getFileName());

    try {
      var messages = parseExportedMessages(Files.readAllLines(filePath), filePath.getFileName().toString());
      logger.info("extracted " + messages.size() + " exported messages from file: " + filePath.getFileName());
      return messages;
    } catch (Exception e) {
      logger.error("Exception processing file: " + filePath + ", " + e.getLocalizedMessage());
      return new ArrayList<ExportedMessage>();
    }

  }

  /**
   * semi-generic method to read a CSV s into a list of array of String fields
   *
   * @param inputPath
   * @return
   */
  public static List<String[]> readCsvStringIntoFieldsArray(String inputString) {
    return readCsvStringIntoFieldsArray(inputString, ',', true, 1);
  }

  /**
   * semi-generic method to read a CSV s into a list of array of String fields
   *
   * @param inputPath
   * @return
   */
  public static List<String[]> readCsvStringIntoFieldsArray(String inputString, char separator, boolean ignoreQuotes,
      int skipLines) {
    var list = new ArrayList<String[]>();

    var rowCount = -1;
    try {
      Reader reader = new StringReader(inputString);
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(separator) //
            .withIgnoreQuotations(ignoreQuotes) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(skipLines)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        list.add(fields);
      }
    } catch (Exception e) {
      logger.error("Exception processing " + inputString + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " records from " + inputString);
    return list;
  }

  /**
   * semi-generic method to read a CSV file into a list of array of String fields
   *
   * @param inputPath
   * @return
   */
  public static List<String[]> readCsvFileIntoFieldsArray(Path inputPath) {
    return readCsvFileIntoFieldsArray(inputPath, ',', false, 0);
  }

  /**
   * semi-generic method to read a CSV file into a list of array of String fields
   *
   * @param inputPath
   * @return
   */
  public static List<String[]> readCsvFileIntoFieldsArray(Path inputPath, char separator, boolean ignoreQuotes,
      int skipLines) {
    var list = new ArrayList<String[]>();

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(separator) //
            .withIgnoreQuotations(ignoreQuotes) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(skipLines)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        list.add(fields);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " records from: " + inputPath.toString());
    return list;
  }
}
