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

package com.surftools.winlinkMessageMapper.tool.neighborTool;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.utils.FileUtils;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.LatLongPair;

public class Dao {

  private static final Logger logger = LoggerFactory.getLogger(Dao.class);

  private final IConfigurationManager cm;
  private final Path path;

  public Dao(IConfigurationManager cm) {
    this.cm = cm;

    var pathName = cm.getAsString(NeighborKey.PATH);
    path = Paths.get(pathName);
  }

  public Map<String, Position> makePositionMap() {
    var positionMap = new TreeMap<String, Position>();
    var inputSourceString = cm.getAsString(NeighborKey.INPUT);
    var inputArray = inputSourceString.split(";");

    List<Position> positions = null;
    for (var inputString : inputArray) {
      var inputFields = inputString.split(",");
      var fileNameString = inputFields[0];
      var fileTypeString = inputFields[1];
      var fileType = FileType.fromString(fileTypeString);
      if (fileType == null) {
        logger.error("unsupported fileType: " + fileTypeString);
        System.exit(1);
      }

      switch (fileType) {
      case CSV:
        positions = readCSV(path, fileNameString, inputFields);
        break;

      default:
        logger.error("unhandled fileType: " + fileType);
      }

      merge(positionMap, positions);
    }

    logger.info("returning positionMap with " + positionMap.size() + " entries");
    return positionMap;
  }

  /**
   *
   * @param positionMap
   * @param positions
   */
  private void merge(TreeMap<String, Position> positionMap, List<Position> positions) {
    for (var position : positions) {
      // TODO more sophisticated merge: average locations, etc
      positionMap.put(position.call(), position);
    }

  }

  private List<Position> readCSV(Path inputPath, String fileNameString, String[] inputFields) {
    var list = new ArrayList<Position>();
    var path = Path.of(inputPath.toString(), fileNameString);
    var pathName = path.toString();
    var callIndex = Integer.valueOf(inputFields[2]);
    var latitudeIndex = Integer.valueOf(inputFields[3]);
    var longitudeIndex = Integer.valueOf(inputFields[4]);
    try {
      Reader reader = new FileReader(pathName);
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      String[] fields;
      while ((fields = csvReader.readNext()) != null) {
        var call = fields[callIndex];
        var latitude = fields[latitudeIndex];
        var longitude = fields[longitudeIndex];
        var location = new LatLongPair(latitude, longitude);

        var position = new Position(call, location);
        logger.debug(position.toString());
        list.add(position);
      }
      logger.info("read " + list.size() + " positions from " + fileNameString);
    } catch (Exception e) {
      logger.error("exception processing targets: " + pathName + ", " + e.getLocalizedMessage());
    }

    return list;
  }

  public void output(Position targetPosition, LinkedHashMap<DistanceBound, Set<RangedPosition>> bins) {
    var outputPath = Path.of(path.toString(), "output", targetPosition.call());
    FileUtils.deleteDirectory(outputPath);
    FileUtils.createDirectory(outputPath);

    outputText(cm, outputPath, targetPosition, bins);

    var kml = new KmlManager(cm);
    kml.output(outputPath, targetPosition, bins);
  }

  private void outputText(IConfigurationManager cm, Path outputPath, Position targetPosition,
      LinkedHashMap<DistanceBound, Set<RangedPosition>> bins) {
    var sb = new StringBuilder();
    sb.append("target: " + targetPosition);
    sb.append("\n");
    for (var bound : bins.keySet()) {
      var set = bins.get(bound);
      sb.append(bound.toString() + "(including " + set.size() + " stations)" + "\n");
      for (var rp : set) {
        sb.append("  " + rp + "\n");
      }
    }

    var textPath = Path.of(outputPath.toString(), targetPosition.call() + ".txt");
    try {
      Files.writeString(textPath, sb.toString());
      logger.info("created text output: " + textPath);
    } catch (IOException e) {
      logger.info("Exception writing file: " + textPath.toString() + ", " + e.getLocalizedMessage());
    }

  }
}
