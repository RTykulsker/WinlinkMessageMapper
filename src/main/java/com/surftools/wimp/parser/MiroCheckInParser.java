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

package com.surftools.wimp.parser;

import java.io.Reader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.MiroCheckInMessage;

public class MiroCheckInParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(MiroCheckInParser.class);
  private final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    String contentLine = message.plainContent.trim();
    if (contentLine != null) {
      var fields = splitLine(contentLine);
      if (fields != null) {
        var formDate = fields[1];
        var formTime = fields[2];
        var formLatitude = fields[4];
        var formLongitude = fields[5];
        var power = fields[6];
        var band = fields[7];
        var mode = fields[8];
        var radio = fields[9];
        var antenna = fields[10];
        var portable = fields[11];
        var comments = fields[12];
        var version = fields[13];

        var rfPower = "";
        var rmsGateway = "";
        var distanceMiles = "";
        if (fields.length >= 17) {
          rfPower = fields[14];
          rmsGateway = fields[15];
          distanceMiles = fields[16];
        }

        var formDateTime = LocalDateTime.parse(formDate + " " + formTime, DT_FORMATTER);
        var formLocation = new LatLongPair(formLatitude, formLongitude);

        var m = new MiroCheckInMessage(message, formDateTime, formLocation, //
            power, band, mode, radio, antenna, portable, comments, version, rfPower, rmsGateway, distanceMiles);

        return m;
      } else {
        return reject(message, RejectType.CANT_PARSE_MIME, contentLine);
      }
    } else {
      return reject(message, RejectType.CANT_PARSE_MIME, message.plainContent);
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.MIRO_CHECK_IN;
  }

  private String[] splitLine(String line) {
    Reader reader = new StringReader(line);
    CSVParser parser = new CSVParserBuilder() //
        .withSeparator(',') //
          .withIgnoreQuotations(false) //
          .build();
    CSVReader csvReader = new CSVReaderBuilder(reader) //
        .withSkipLines(0)//
          .withCSVParser(parser)//
          .build();
    try {
      var fields = csvReader.readNext();
      return fields;
    } catch (Exception e) {
      return null;
    }
  }

}
