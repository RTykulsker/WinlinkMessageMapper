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

package com.surftools.winlinkMessageMapper.grade.expect;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

public class CsvExpectDao implements IExpectDao {
  private static final Logger logger = LoggerFactory.getLogger(CsvExpectDao.class);

  private final String filename;

  public CsvExpectDao(String filename) {
    this.filename = filename;
  }

  @Override
  public List<ExpectRecord> readAll() {
    var list = new ArrayList<ExpectRecord>();
    Path inputPath = Path.of(filename);

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        var expectRecord = ExpectRecord.parse(fields);
        list.add(expectRecord);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " exerciseSummaries from: " + inputPath.toString());
    return list;
  }

}
