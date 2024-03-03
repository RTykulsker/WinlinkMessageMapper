/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

@Deprecated
public class RmsLocationProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(RmsLocationProcessor.class);

  public static final String RMS_LOCATION_MANAGER_CONTEXT = "rmsLocationManager";

  private Map<String, LatLongPair> callLocationMap;

  /**
   * this is our public interface
   */

  public LatLongPair getLocationForRms(String rmsCall) {
    var result = callLocationMap.get(rmsCall);
    logger.debug("rms location of " + rmsCall + " is " + result);

    return result;
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    callLocationMap = new HashMap<>();
    callLocationMap.putAll(load(Key.RMS_HF_GATEWAYS_FILE_NAME));
    callLocationMap.putAll(load(Key.RMS_VHF_GATEWAYS_FILE_NAME));
    logger.info("after initialization, map has " + callLocationMap.size() + " unique RMS locations");

    mm.putContextObject(RMS_LOCATION_MANAGER_CONTEXT, this);
  }

  private Map<String, LatLongPair> load(Key key) {
    return load(cm.getAsString(key), key.toString());
  }

  private Map<String, LatLongPair> load(String fileNameString, String tag) {
    var results = new HashMap<String, LatLongPair>();
    if (fileNameString == null || fileNameString.isEmpty()) {
      logger.debug("no key: " + tag + " in configuration");
      return results;
    }

    var fieldsList = readCsvFileIntoFieldsArray(Path.of(fileNameString));
    logger.debug("read " + fieldsList.size() + " gateway records from " + fileNameString);
    for (var fields : fieldsList) {
      if (fields.length >= 2) {
        var call = fields[0];
        var grid = fields[1];
        var pair = new LatLongPair(grid);
        results.put(call, pair);
      }
    }

    logger.debug("returning " + results.size() + " RMS locations for " + fileNameString);
    return results;
  }

  /**
   * semi-generic method to read a CSV file into a list of array of String fields
   *
   * @param inputPath
   * @return
   */
  public static List<String[]> readCsvFileIntoFieldsArray(Path inputPath) {
    var list = new ArrayList<String[]>();

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator('|') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(0)//
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

    logger.debug("returning: " + list.size() + " records from: " + inputPath.toString());
    return list;
  }

  @Override
  public void process() {
    // nothing to see here
  }

}
