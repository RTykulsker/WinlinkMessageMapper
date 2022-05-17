/**

The MIT License (MIT)

Copyright (c) 2021, Robert Tykulsker

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.winlinkMessageMapper.dto.p2p.FieldStation;
import com.surftools.winlinkMessageMapper.dto.p2p.TargetStation;

/**
 * App to produce a KML file that shows the result of a ETO P2P exercise
 *
 * we need to run the WinlinkMessageMapper (wimp) to get a set of ExportedMessages we can parse
 *
 * we can run WinlinkMessageMapper either with or without deduplication
 *
 * http://kml4earth.appspot.com/icons.html
 *
 * @author bobt
 *
 */
public class P2PMapper {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(P2PMapper.class);

  @Option(name = "--excludeTargets", metaVar = "EXCLUDE_TARGETS", usage = "exclude target stations [call,[call...]]", required = false)
  private static String excludeTargetStations = null;

  @Option(name = "--excludeFields", metaVar = "EXCLUDE_FIELDS", usage = "exclude field stations [call,[call...]]", required = false)
  private static String excludeFieldStations = null;

  @Option(name = "--targetFileName", metaVar = "TARGET FILE", usage = "name of target CSV", required = true)
  private static String targetStationFileName = null;

  @Option(name = "--targetDirName", metaVar = "TARGET DIR", usage = "name of target directory", required = true)
  private static String targetDirName = null;

  @Option(name = "--fieldFileName", metaVar = "FIELD FILE", usage = "name of field CSV", required = true)
  private static String fieldStationFileName = null;

  @Option(name = "--templateFileName", metaVar = "TEMPLATE FILE", usage = "name of template KML", required = true)
  private static String templateFileName = null;

  @Option(name = "--outputDirName", metaVar = "OUTPUT DIR", usage = "output dir", required = true)
  private static String outputDirName = null;

  @Option(name = "--mapName", metaVar = "MAP NAME", usage = "map name", required = false)
  private static String mapName = "P2P Participipants";

  @Option(name = "--mapDescription", metaVar = "MAP Description", usage = "map description", required = false)
  private static String mapDescription = "ETO Description";

  public static void main(String[] args) {
    P2PMapper app = new P2PMapper();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() {
    try {
      List<TargetStation> targetStations = getTargetStations(targetStationFileName, excludeTargetStations);
      List<FieldStation> fieldStations = getFieldStations(fieldStationFileName, excludeFieldStations, targetStations);
      jitter(targetStations, fieldStations);
      String kmlText = makeKmlText(targetStations, fieldStations, templateFileName, mapName, mapDescription);
      writeTemplateFile(outputDirName, kmlText);

      // validate at end, so I don't have to scroll through output!
      validateTargetFiles(targetStations, targetDirName);

      logger.info("exiting");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  private List<TargetStation> getTargetStations(String targetFileName, String excludeTargetStations) throws Exception {
    var excludeSet = makeExcludeSet(excludeTargetStations);
    List<TargetStation> targetStations = new ArrayList<>();

    Reader reader = new FileReader(targetFileName);
    CSVParser parser = new CSVParserBuilder() //
        .withSeparator(',') //
          .withIgnoreQuotations(true) //
          .build();
    CSVReader csvReader = new CSVReaderBuilder(reader) //
        .withSkipLines(1)//
          .withCSVParser(parser)//
          .build();

    String[] fields = null;
    while ((fields = csvReader.readNext()) != null) {
      var targetStation = new TargetStation(fields);
      if (!excludeSet.contains(targetStation.call)) {
        targetStations.add(targetStation);
        logger.info("adding " + targetStation);
      } else {
        logger.info("excluding " + targetStation);
      }
    }

    logger.info("returning: " + targetStations.size() + " targets from: " + P2PMapper.targetStationFileName);
    return targetStations;
  }

  private Set<String> makeExcludeSet(String excludeStrings) {
    Set<String> set = new HashSet<>();

    if (excludeStrings != null) {
      String[] fields = excludeStrings.split(",");
      set.addAll(Arrays.asList(fields));
    }

    return set;
  }

  private List<FieldStation> getFieldStations(String fieldFileName, String excludeFieldStations,
      List<TargetStation> targetStations) throws Exception {
    var excludeSet = makeExcludeSet(excludeFieldStations);
    List<FieldStation> fieldStations = new ArrayList<>();

    Map<String, TargetStation> targetStationMap = new HashMap<>();
    for (TargetStation target : targetStations) {
      targetStationMap.put(target.call, target);
    }

    Reader reader = new FileReader(fieldFileName);
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
      var fieldStation = new FieldStation(fields);

      if (excludeSet.contains(fieldStation.from)) {
        logger.warn("excluding " + fieldStation);
        continue;
      }

      TargetStation targetStation = targetStationMap.get(fieldStation.to);
      if (targetStation == null) {
        logger.warn("skipping " + fieldStation.from + " because unsupported target: " + fieldStation.to);
        continue;
      }

      fieldStations.add(fieldStation);
      logger.info("adding " + fieldStation);
    }

    logger.info("returning: " + fieldStations.size() + " field Stations from: " + fieldFileName);
    return fieldStations;
  }

  /**
   * warn if we are missing any target files or have files from non-Targets
   *
   * @param targetStations
   * @param targetDirName
   */
  private void validateTargetFiles(List<TargetStation> targetStations, String targetDirName) {
    Map<String, TargetStation> targetMap = new HashMap<>();
    Map<String, Integer> targetCountMap = new TreeMap<>();
    for (TargetStation target : targetStations) {
      targetMap.put(target.call, target);
      targetCountMap.put(target.call, 0);
    }

    File targetDir = new File(targetDirName);
    for (File file : targetDir.listFiles()) {
      if (file.isFile()) {
        var fileName = file.getName();
        var fileNameUC = fileName.toUpperCase();
        if (!fileNameUC.endsWith(".XML")) {
          continue;
        }
        var fields = fileNameUC.split(" ");
        var call = fields[0];
        var target = targetMap.get(call);
        if (target == null) {
          logger.warn("no target for call: " + call + ", fileName: " + fileName);
        } else {
          int count = targetCountMap.get(call);
          targetCountMap.put(call, count + 1);
        }
      }
    }

    int validTargetCount = 0;
    for (String call : targetMap.keySet()) {
      int count = targetCountMap.get(call);
      if (count != 1) {
        logger.warn("call: " + call + ", fileCount: " + count);
      } else {
        ++validTargetCount;
      }
    }

    logger.info("received files for " + validTargetCount + " targets");
  }

  /**
   * Google mymaps doesn't facilitate viewing placemarks at the identical location
   *
   * solution is to move the Target by 0.00001 degrees, about 0.9 meters or 3 feet
   *
   * @param targetStations
   * @param fieldStations
   */
  private void jitter(List<TargetStation> targetStations, List<FieldStation> fieldStations) {
    Map<String, TargetStation> targetMap = new HashMap<>();
    for (TargetStation target : targetStations) {
      targetMap.put(target.call, target);
    }

    Map<String, FieldStation> fieldMap = new HashMap<>();
    for (FieldStation field : fieldStations) {
      fieldMap.put(field.from, field);
    }

    double distanceMeters = -1;
    for (String call : targetMap.keySet()) {
      FieldStation field = fieldMap.get(call);
      if (field == null) {
        continue;
      }
      TargetStation target = targetMap.get(call);
      distanceMeters = computeDistanceMeters(field, target);
      if (distanceMeters <= 1) {
        Double longitude = Double.parseDouble(target.getLongitude()) + 0.00001;
        TargetStation newTarget = new TargetStation(target, target.getLatitude(), String.valueOf(longitude));
        distanceMeters = computeDistanceMeters(field, newTarget);
        logger
            .info("target: " + call + ", jittered from: " + target.getLatLongPair() + " to: "
                + newTarget.getLatLongPair() + ", new distance: " + distanceMeters + "m");
        targetStations.remove(target);
        targetStations.add(newTarget);
      }
    }

  }

  private String makeKmlText(List<TargetStation> targetStations, List<FieldStation> fieldStations,
      String templateFileName, String mapName2, String mapDescription2) throws Exception {
    String templateText = Files.readString(Path.of(templateFileName));

    Map<String, String> variableMap = new HashMap<>();
    variableMap.put("mapName", mapName);
    variableMap.put("mapDescription", mapDescription);
    variableMap.put("station-placemarks", makeStationPlacemarks(targetStations, fieldStations));
    variableMap.put("network-placemarks", makeNetworkPlacemarks(targetStations, fieldStations));

    final String regex = "\\$\\{([^}]++)\\}";
    final Pattern pattern = Pattern.compile(regex);
    // https://stackoverflow.com/questions/17462146
    Matcher matcher = pattern.matcher(templateText);
    String result = templateText;
    while (matcher.find()) {
      String token = matcher.group(); // Ex: ${fizz}
      String tokenKey = matcher.group(1); // Ex: fizz
      String replacementValue = null;

      if (variableMap.containsKey(tokenKey)) {
        replacementValue = variableMap.get(tokenKey);
        try {
          result = result.replaceFirst(Pattern.quote(token), replacementValue);
        } catch (Exception e) {
          logger.error("Exception for token: " + token + ", " + e.getMessage());
        }
      } else {
        logger.error("String contained an unsupported token: " + token);
      }
    }

    return result;
  }

  private String makeNetworkPlacemarks(List<TargetStation> targetStations, List<FieldStation> fieldStations) {
    Map<String, TargetStation> targetMap = new HashMap<>();
    for (TargetStation target : targetStations) {
      targetMap.put(target.call, target);
    }

    StringBuilder sb = new StringBuilder();
    for (FieldStation fieldStation : fieldStations) {
      TargetStation targetStation = targetMap.get(fieldStation.to);
      sb.append("  <Placemark>\n");
      sb.append("  <name>" + fieldStation.from + "-" + fieldStation.to + "</name>\n");
      sb.append("    <LineString>\n");
      sb
          .append("    <coordinates>" + fieldStation.getLongitude() + "," + fieldStation.getLatitude() //
              + " " + targetStation.getLongitude() + "," + targetStation.getLatitude() + "</coordinates>\n");
      sb.append("    </LineString>\n");
      sb.append("  </Placemark>\n");
    }

    String s = sb.toString();
    return s;
  }

  private String makeStationPlacemarks(List<TargetStation> targetStations, List<FieldStation> fieldStations) {
    final var DASHES = "------------------------------------------------------------";
    Map<String, TargetStation> targetMap = new HashMap<>(); // target.call -> target

    // use TreeSet for ordered list; fieldFLM is call out from field, targetFLM is call in to target
    Map<String, TreeSet<FieldStation>> fieldOutboundFieldsMap = new HashMap<>(); // field call -> list of fields;
    Map<String, TreeSet<FieldStation>> targetInboundFieldsMap = new HashMap<>(); // target call -> list of fields
    StringBuilder sb = new StringBuilder();

    for (FieldStation field : fieldStations) {
      var outbounds = fieldOutboundFieldsMap.getOrDefault(field.from, new TreeSet<>());
      outbounds.add(field);
      fieldOutboundFieldsMap.put(field.from, outbounds);

      var inbounds = targetInboundFieldsMap.getOrDefault(field.to, new TreeSet<>());
      inbounds.add(field);
      targetInboundFieldsMap.put(field.to, inbounds);
    }

    targetStations.sort((TargetStation s1, TargetStation s2) -> s1.call.compareTo(s2.call));
    for (TargetStation station : targetStations) {
      targetMap.put(station.call, station);
      sb.append("  <Placemark>\n");
      sb.append("    <name>" + station.call + "</name>\n");
      sb.append("    <styleUrl>#targetpin</styleUrl>\n");
      sb.append("    <description>");
      sb.append(station.dialFrequency + " KHz dial, " + station.city + ", " + station.state + "\n");
      sb.append(DASHES + "\n");

      var inbounds = targetInboundFieldsMap.get(station.call);
      if (inbounds != null && inbounds.size() > 0) {
        sb.append("Inbound Connections: " + inbounds.size() + "\n");
        sb.append(DASHES + "\n");
        for (FieldStation field : inbounds) {
          sb.append(field.date + " " + field.time + ", " + field.from);
          sb.append(" (" + computeDistanceMiles(field, station) + " miles)");
          sb.append("\n");
        }
      }

      sb.append("    </description>\n");
      sb.append("    <Point>\n");
      sb.append("    <coordinates>" + station.getLongitude() + "," + station.getLatitude() + "</coordinates>\n");
      sb.append("    </Point>\n");
      sb.append("  </Placemark>\n");
    }

    Set<String> markedFieldStations = new HashSet<>();
    fieldStations.sort((FieldStation s1, FieldStation s2) -> s1.from.compareTo(s2.from));
    for (FieldStation station : fieldStations) {

      // don't allow more that one placemark for any field station
      if (markedFieldStations.contains(station.from)) {
        continue;
      } else {
        markedFieldStations.add(station.from);
      }

      sb.append("  <Placemark>\n");
      sb.append("    <name>" + station.from + "</name>\n");
      sb.append("    <styleUrl>#fieldpin</styleUrl>\n");
      sb.append("<description>");

      var outbounds = fieldOutboundFieldsMap.get(station.from);
      sb.append("Outbound Connections: " + outbounds.size() + "\n" + DASHES + "\n");
      for (FieldStation field : outbounds) {
        TargetStation target = targetMap.get(field.to);
        sb.append(field.date + " " + field.time + ", " + field.to + ", ");
        sb.append("(" + target.band + "m");
        sb.append(", " + computeDistanceMiles(field, target) + " miles)\n");
      }

      sb.append("</description>\n");
      sb.append("    <Point>\n");
      sb.append("    <coordinates>" + station.getLongitude() + "," + station.getLatitude() + "</coordinates>\n");
      sb.append("    </Point>\n");
      sb.append("  </Placemark>\n");
    }

    logger.info("created: " + markedFieldStations.size() + " field station placemarks");

    String s = sb.toString();
    return s;
  }

  private void writeTemplateFile(String outputDirName, String kmlText) throws Exception {
    File outputDirectory = new File(outputDirName);
    if (!outputDirectory.exists()) {
      outputDirectory.mkdir();
    }

    Path outputPath = Path.of(outputDirName, "p2p.kml");
    BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
    writer.write(kmlText);

    writer.close();
    logger.info("wrote to: " + outputPath.toString());
  }

  private int computeDistanceMiles(FieldStation fieldStation, TargetStation targetStation) {
    return fieldStation.getLatLongPair().computeDistanceMiles(targetStation.getLatLongPair());
  }

  private double computeDistanceMeters(FieldStation field, TargetStation target) {
    return field.getLatLongPair().computeDistanceMeters(target.getLatLongPair());
  }
}
