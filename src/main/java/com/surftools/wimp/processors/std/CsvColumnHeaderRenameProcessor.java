/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * rename column headers from a CSV file, producing a new CSV file
 *
 * inFile1;outFile1;oldName1>newName1|oldName2>newName2...
 */
public class CsvColumnHeaderRenameProcessor implements IProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CsvColumnHeaderRenameProcessor.class);

  static record Context(String oldName, String newName) {
  };

  private static final String PAIR_DELIMITER = "\\|";
  private static final String FIELD_DELIMITER = ";";
  private static final String HEADER_DELIMITER = ">";

  private static final Map<String, String> renameMap = new HashMap<>();

  private String inputFileName;
  private String outputFileName;

  private boolean isInitialized = false;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

    var configString = cm.getAsString(Key.CSV_COLUMN_HEADER_RENAME_CONFIGURATION);
    if (configString == null) {
      logger.warn("no configuration for " + Key.CSV_COLUMN_HEADER_RENAME_CONFIGURATION);
      return;
    }

    var fields = configString.split(FIELD_DELIMITER);
    if (fields.length != 3) {
      throw new IllegalArgumentException(
          "can't parse CsvHeaderRename configuration for " + configString + ", must have exactly 3 fields");
    }
    inputFileName = fields[0].trim().replaceAll("\\$PATH", AbstractBaseProcessor.inputPathName);
    outputFileName = fields[1].trim().replaceAll("\\$PATH", AbstractBaseProcessor.inputPathName);
    var pairs = fields[2].split(PAIR_DELIMITER);
    for (var pair : pairs) {
      var strings = pair.split(HEADER_DELIMITER);
      if (!(strings.length == 2)) {
        throw new IllegalArgumentException(
            "can't parse CsvHeaderRename configuration for " + pair + ", must have exactly 2 fields");
      }
      renameMap.put(strings[0].trim(), strings[1].trim());
    }

    logger.info("initialized " + renameMap.size() + " rename pairs");
    isInitialized = true;
  }

  @Override
  public void process() {
    ;
  }

  @Override
  public void postProcess() {
    if (!isInitialized) {
      logger.warn("no configuration for " + Key.CSV_COLUMN_HEADER_RENAME_CONFIGURATION);
      return;
    }

    try {
      var lines = Files.readAllLines(Path.of(inputFileName));

      var oldHeader = lines.get(0);
      var newHeader = oldHeader;

      for (var entry : renameMap.entrySet()) {
        newHeader = newHeader.replace(entry.getKey(), entry.getValue());
      }

      lines.set(0, newHeader);

      Files.write(Path.of(outputFileName), lines);
      logger.info("read " + (lines.size() - 1) + " records plus header from: " + inputFileName);
      logger.info("wrote " + lines.size() + " records plus header to: " + outputFileName);
    } catch (Exception e) {
      logger.error("Exception in rename: " + e.getLocalizedMessage());
    }

  }
}
