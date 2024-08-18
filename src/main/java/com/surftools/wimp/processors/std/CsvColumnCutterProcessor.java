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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.PageParser;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * cut columns from a CSV file, producing a new CSV file
 *
 * inFile1;outFile1;columnContext1|inFile2;outFile2;columnContext2 ...
 */
public class CsvColumnCutterProcessor implements IProcessor {
  private static final Logger logger = LoggerFactory.getLogger(CsvColumnCutterProcessor.class);

  static record Context(String inputFileName, String outputFileName, List<Integer> columnList) {
  };

  private static final String CONTEXT_DELIMITER = "\\|";
  private static final String FIELD_DELIMITER = ";";

  private static final List<Context> contexts = new ArrayList<>();

  private static String[] outputHeaders;
  private Set<Integer> indexSet;

  private boolean isInitialized;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

    var contextsString = cm.getAsString(Key.CSV_COLUMN_CUTTER_CONFIGURATION);
    if (contextsString == null) {
      logger.warn("no configuration for " + Key.CSV_COLUMN_CUTTER_CONFIGURATION);
      return;
    }

    contextsString = contextsString.replaceAll("\\$PATH", cm.getAsString(Key.PATH));
    if (contextsString != null && contextsString.length() > 0) {
      for (var contextString : contextsString.split(CONTEXT_DELIMITER)) {
        var fields = contextString.split(FIELD_DELIMITER);
        if (fields == null || fields.length != 3) {
          throw new IllegalArgumentException("can't parse CsvColumnCutter configuration for " + contextString);
        }
        var pageParser = new PageParser();
        var columnList = pageParser.parse(fields[2]);
        var context = new Context(fields[0], fields[1], columnList);
        contexts.add(context);
      }
      logger.info("initialized " + contexts.size() + " contexts");
    }

    isInitialized = true;
  }

  @Override
  public void process() {
    ;
  }

  @Override
  public void postProcess() {
    if (!isInitialized) {
      logger.warn("no configuration for " + Key.CSV_COLUMN_CUTTER_CONFIGURATION);
      return;
    }

    for (var context : contexts) {
      indexSet = new HashSet<Integer>(context.columnList().size());

      // adjust for zero-based indexes
      for (var columnIndex : context.columnList) {
        indexSet.add(columnIndex - 1);
      }

      outputHeaders = null;
      var inputLines = ReadProcessor.readCsvFileIntoFieldsArray(Path.of(context.inputFileName), ',', false, 0);
      var table = new ArrayList<IWritableTable>(inputLines.size());
      for (var inputLine : inputLines) {
        var outputLine = cutColumns(inputLine);

        if (outputHeaders == null) {
          outputHeaders = outputLine;
        } else {
          table.add(new Entry(outputLine));
        }
      }

      WriteProcessor.writeTable(table, Path.of(context.outputFileName));

      logger.info("read " + (inputLines.size() - 1) + " records plus header from: " + context.inputFileName);
      logger.info("cut columns: " + context.columnList.stream().map(String::valueOf).collect(Collectors.joining(",")));
      logger.info("wrote " + table.size() + " records plus header to: " + context.outputFileName);
    }
  }

  private String[] cutColumns(String[] inputLine) {
    var outputLine = new String[inputLine.length - indexSet.size()];
    var outputIndex = 0;
    for (var inputIndex = 0; inputIndex < inputLine.length; ++inputIndex) {
      if (indexSet.contains(inputIndex)) {
        continue;
      }
      outputLine[outputIndex] = inputLine[inputIndex];
      ++outputIndex;
    }
    return outputLine;
  }

  record Entry(String[] fields) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable o) {
      var other = (Entry) o;
      return fields[0].compareTo(other.fields[0]);
    }

    @Override
    public String[] getHeaders() {
      return outputHeaders;
    }

    @Override
    public String[] getValues() {
      return fields;
    }

  }

}
