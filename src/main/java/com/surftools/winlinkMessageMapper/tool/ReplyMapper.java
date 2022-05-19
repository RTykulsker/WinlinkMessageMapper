/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.tool;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.surftools.winlinkMessageMapper.dto.other.LatLongPair;
import com.surftools.winlinkMessageMapper.reply.IReplyParser;
import com.surftools.winlinkMessageMapper.reply.ParsedReply;

/**
 * /**
 *
 * read a parsed message file of some type, for example ics-213_reply
 *
 * read a file of some type, for example participantHistory.csv to get calls and locations
 *
 * read a reply-splitting string to know how to parse
 *
 * parse the reply messagess
 *
 * output a new file
 *
 *
 * @author bobt
 *
 */

public class ReplyMapper {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(ReplyMapper.class);

  @Option(name = "--parsedMessageFileName", usage = "path to parsed message file", required = true)
  private String parsedMessageFileName = null;

  @Option(name = "--parsedMessageColumns", usage = "zero-based, comma-delimited column indices for call and reply", required = true)
  private String parsedMessageColumns = null;

  @Option(name = "--locationFileName", usage = "path to location file", required = true)
  private String locationFileName = null;

  @Option(name = "--locationColumns", usage = "zero-based, comma-delimited column indices for call, latitude and longitude", required = true)
  private String locationColumns = null;

  @Option(name = "--parserClassName", usage = "class name of IParser", required = true)
  private String parserClassName = null;

  @Option(name = "--outputFileName", usage = "output file name", required = true)
  private String outputFileName = null;

  public static void main(String[] args) {
    ReplyMapper app = new ReplyMapper();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  private void run() {
    try {
      logger.info("begin");

      IReplyParser parser = makeParser(parserClassName);
      var locationMap = readLocations(locationFileName, locationColumns);
      var replies = readReplies(parsedMessageFileName, parsedMessageColumns, locationMap);

      for (var reply : replies) {
        parser.parse(reply);
      }
      logger
          .info("good parseCount: " + parser.getGoodParseCount()
              + formatPercent(parser.getGoodParseCount(), replies.size()));
      logger
          .info("bad parseCount: " + parser.getBadParseCount()
              + formatPercent(parser.getBadParseCount(), replies.size()));

      write(replies, outputFileName, parser);

      logger.info("exiting");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  private String formatPercent(int count, int size) {
    double percent = (100d * count) / size;
    return " (" + String.format("%.2f", percent) + "%)";
  }

  private void write(List<ParsedReply> replies, String outputFileName, IReplyParser parser) {
    Path outputPath = Path.of(outputFileName);

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(parser.getHeaders());

      if (replies.size() > 0) {
        Collections.sort(replies);

        for (var reply : replies) {
          writer.writeNext(reply.getValues());
        }
      }

      writer.close();
      logger.info("wrote " + replies.size() + " replies to file: " + outputPath);
    } catch (

    Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  private IReplyParser makeParser(String parserClassName) {
    final var prefixes = new String[] { "", "com.surftools.winlinkMessageMapper.reply." };
    IReplyParser parser = null;
    for (var prefix : prefixes) {
      var className = prefix + parserClassName;
      try {
        var clazz = Class.forName(className);
        if (clazz != null) {
          parser = (IReplyParser) clazz.getDeclaredConstructor().newInstance();
          return parser;
        }
      } catch (Exception e) {
        ;
      }
    }

    throw new RuntimeException("could not find class for " + parserClassName);
  }

  private HashMap<String, LatLongPair> readLocations(String locationFileName, String locationColumns) {
    var map = new HashMap<String, LatLongPair>();

    var inputPath = Path.of(locationFileName);

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return map;
    }

    var indices = locationColumns.split(",");
    if (indices == null || indices.length != 3) {
      throw new RuntimeException(
          "can't parse locationColumns:" + locationColumns + ", Expecting call, latitude and longitude column indices");
    }

    var callIndex = -1;
    try {
      callIndex = Integer.parseInt(indices[0]);
    } catch (Exception e) {
      throw new RuntimeException("can't parse call index from: " + locationColumns);
    }

    var latitudeIndex = -1;
    try {
      latitudeIndex = Integer.parseInt(indices[1]);
    } catch (Exception e) {
      throw new RuntimeException("can't parse latitude index from: " + locationColumns);
    }

    var longitudeIndex = -1;
    try {
      longitudeIndex = Integer.parseInt(indices[2]);
    } catch (Exception e) {
      throw new RuntimeException("can't parse longitude index from: " + locationColumns);
    }

    try {
      var reader = new FileReader(inputPath.toString());
      var parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      var csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        var call = fields[callIndex];

        if (fields.length < longitudeIndex) {
          logger.info("skipping call: " + call + ", not enough fields");
          continue;
        }

        var latitude = fields[latitudeIndex];
        var longitude = fields[longitudeIndex];
        var pair = new LatLongPair(latitude, longitude);
        map.put(call, pair);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + map.size() + " location records from: " + inputPath.toString());

    return map;
  }

  private List<ParsedReply> readReplies(String parsedMessageFileName, String parsedMessageColumns,
      HashMap<String, LatLongPair> locationMap) {
    var list = new ArrayList<ParsedReply>();

    var inputPath = Path.of(parsedMessageFileName);

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var indices = parsedMessageColumns.split(",");
    if (indices == null || indices.length != 2) {
      throw new RuntimeException(
          "can't parse parsedMessageColumns:" + parsedMessageColumns + ", Expecting call and reply column indices");
    }

    var callIndex = -1;
    try {
      callIndex = Integer.parseInt(indices[0]);
    } catch (Exception e) {
      throw new RuntimeException("can't parse call index from: " + parsedMessageColumns);
    }

    var replyIndex = -1;
    try {
      replyIndex = Integer.parseInt(indices[1]);
    } catch (Exception e) {
      throw new RuntimeException("can't parse call index from: " + parsedMessageColumns);
    }

    var locatedCount = 0;
    var unlocatedCount = 0;
    try {
      var reader = new FileReader(inputPath.toString());
      var parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      var csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();

      String[] fields = null;

      while ((fields = csvReader.readNext()) != null) {
        var call = fields[callIndex];

        if (fields.length < replyIndex) {
          logger.info("skipping call: " + call + ", not enough fields");
          continue;
        }

        var reply = fields[replyIndex];
        var parsedReply = new ParsedReply(call, reply);
        var location = locationMap.get(call);
        if (location != null) {
          ++locatedCount;
          parsedReply.latitude = location.getLatitude();
          parsedReply.longitude = location.getLongitude();
        } else {
          logger.info("couldn't find location for call: " + call);
          ++unlocatedCount;
        }
        list.add(parsedReply);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", " + e.getLocalizedMessage());
    }

    logger.info("located: " + locatedCount + " replies");
    logger.info("couldn't locate " + unlocatedCount + " replies");
    logger.info("returning: " + list.size() + " parsedReply records from: " + inputPath.toString());

    return list;
  }

}