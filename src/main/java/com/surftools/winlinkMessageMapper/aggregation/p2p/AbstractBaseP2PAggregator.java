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

package com.surftools.winlinkMessageMapper.aggregation.p2p;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.surftools.utils.location.LocationUtils;
import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * P2P processing is just aggregation processing ...
 *
 * do common aggregation tasks, such as grouping by type, finding location, etc
 *
 * @author bobt
 *
 */
public abstract class AbstractBaseP2PAggregator extends AbstractBaseAggregator {
  protected final String DASHES = "------------------------------------------------------------\n";

  public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected Map<String, BaseField> fieldMap = new HashMap<>();
  protected Map<String, BaseTarget> targetMap = new HashMap<>();

  protected Set<MessageType> requiredMessageTypeSet = new HashSet<>();

  public AbstractBaseP2PAggregator(Logger logger) {
    super(logger);
  }

  public void initialize() {
    importFieldData();
    importTargetData();
  }

  @Override
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    super.aggregate(messageMap);

    checkForCoLocation();

    var countMessageTypesSkipped = 0;
    var countMessagesSkippedForType = 0;
    var countMessagesSkippedNotValidTarget = 0;
    var countMessagesSkippedNotValidField = 0;

    // build to/from lists
    for (var messageType : messageMap.keySet()) {
      var exportedMessages = messageMap.get(messageType);
      if (!requiredMessageTypeSet.contains(messageType)) {
        logger.warn("### skipping " + exportedMessages.size() + " messages of type: " + messageType);
        ++countMessageTypesSkipped;
        countMessagesSkippedForType += exportedMessages.size();
      }

      for (var m : exportedMessages) {

        var from = m.from;
        var to = m.to;

        if (dumpIds.contains(from) || dumpIds.contains(to) || dumpIds.contains(m.messageId)) {
          logger.info("dump: from " + from + ", to: " + to + ", mId: " + m.messageId);
        }

        // if (!m.isP2P) {
        // logger.warn("skipping message: " + m.messageId + ", from: " + from + ", to: " + to + " because not P2P");
        // continue;
        // }

        var field = fieldMap.get(from);
        if (field == null) {
          logger.warn("### unexpected message from: " + from + ", not in field map");
          continue;
        }

        var target = targetMap.get(to);
        if (target == null) {
          logger.warn("### unexpected message to: " + to + ", not in target map");
          continue;
        }

        var toList = field.toList;
        if (targetMap.containsKey(to)) {
          toList.add(m);
        } else {
          logger.warn("### skipping message: " + m + " because to(" + to + ") not in targets");
          ++countMessagesSkippedNotValidTarget;
        }

        var fromList = target.fromList;
        if (fieldMap.containsKey(from)) {
          fromList.add(m);
        } else {
          logger.warn("### skipping message: " + m + " because from(" + from + ") not in fields");
          ++countMessagesSkippedNotValidField;
        }
      } // end loop over messages in exportedMesssage list
    } // end loop over messageTypes

    logger.info("messageTypes skipped: " + countMessageTypesSkipped);
    logger.info("messages skipped for type: " + countMessagesSkippedForType);
    logger.info("messages skipped for unsupported Field station: " + countMessagesSkippedNotValidField);
    logger.info("messages skipped for unsupported Target station: " + countMessagesSkippedNotValidTarget);

    // sort all the to/from lists
    var timeComparator = new ExportedMessageDateTimeComparator();
    for (var field : fieldMap.values()) {
      field.toList.sort(timeComparator);
    }

    for (var target : targetMap.values()) {
      target.fromList.sort(timeComparator);
    }

    displayMissingTargets();
  }

  /**
   * a Target and a Field at the same location is possible, but not desirable
   *
   * Multiple Targets at the same location is probably an error
   *
   * Multiple Fields at the same location is possible, but not desirable
   */
  protected void checkForCoLocation() {
    var thresholdMeters = Double.valueOf(cm.getAsString(Key.DEDUPLICATION_THRESHOLD_Meters, "10"));

    var fields = fieldMap.values();
    var targets = targetMap.values();

    // Target vs Target
    for (var aTarget : targets) {
      for (var bTarget : targets) {

        if (aTarget.call.equals(bTarget.call)) {
          continue;
        }

        var distanceMeters = LocationUtils.computeDistanceMeters(aTarget.location, bTarget.location);
        if (distanceMeters < thresholdMeters) {
          logger.warn("### targets too close: " + aTarget + " and " + bTarget);
        }
      }
    }

    // Field vs Field
    for (var aField : fields) {
      for (var bField : fields) {

        if (aField.call.equals(bField.call)) {
          continue;
        }

        var distanceMeters = LocationUtils.computeDistanceMeters(aField.location, bField.location);
        if (distanceMeters < thresholdMeters) {
          logger.warn("### fields too close: " + aField + " and " + bField);
        }
      }
    }

    // Field vs Target
    for (var field : fields) {
      for (var target : targets) {

        var distanceMeters = LocationUtils.computeDistanceMeters(field.location, target.location);
        if (distanceMeters < thresholdMeters) {
          logger.warn("### field and target too close: " + field + " and " + target);
        }
      }
    }

  }

  /**
   * which Targets have submitted their results and which have not!
   */
  private void displayMissingTargets() {
    var missingSet = new TreeSet<String>();
    var completedSet = new TreeSet<String>();

    for (var targetCall : targetMap.keySet()) {
      var target = targetMap.get(targetCall);
      var fromList = target.fromList;
      if (fromList.size() == 0) {
        missingSet.add(targetCall);
      } else {
        completedSet.add(targetCall);
      }
    }

    var fieldCount = 0;
    for (var fieldCall : fieldMap.keySet()) {
      var field = fieldMap.get(fieldCall);
      if (field.toList.size() > 0) {
        ++fieldCount;
      }
    }

    logger.warn("### missing messages to " + missingSet.size() + " targets: " + String.join(",", missingSet));
    logger.info("received messages to " + completedSet.size() + " targets: " + String.join(",", completedSet));
    logger.info("received messages from " + fieldCount + " of " + fieldMap.size() + " field stations");
  }

  static class ExportedMessageDateTimeComparator implements Comparator<ExportedMessage> {

    @Override
    public int compare(ExportedMessage o1, ExportedMessage o2) {
      return o1.dateTime.compareTo(o2.dateTime);
    }

  }

  protected void writeKml(String kmlText) {
    try {
      Path outputPath = Path.of(cm.getAsString(Key.PATH), "output", "p2p.kml");
      BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
      writer.write(kmlText);

      writer.close();
      logger.info("wrote to: " + outputPath.toString());
    } catch (Exception e) {
      logger.error("Exception writing kml output file: " + e.getLocalizedMessage());
    }
  }

  public void writeUpdatedTargets() {
    Path outputPath = Path.of(cm.getAsString(Key.PATH), "output", "updated-targets.csv");

    var messageCount = 0;
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      var targetList = new ArrayList<>(targetMap.values());
      targetList.sort((t1, t2) -> t2.fromList.size() - t1.fromList.size());

      var firstTarget = targetList.iterator().next();
      writer.writeNext(firstTarget.getHeaders());

      for (var target : targetList) {
        if (target.fromList.size() > 0) {
          writer.writeNext(target.getValues());
          ++messageCount;
        }
      }

      writer.close();
      logger.info("wrote " + messageCount + " updated P2P targets to file: " + outputPath);
    } catch (

    Exception e) {
      logger.error("Exception writing file: " + outputPath.toString() + ", " + e.getLocalizedMessage());
    }

  }

  public void writeUpdatedFields() {
    Path outputPath = Path.of(cm.getAsString(Key.PATH), "output", "updated-fields.csv");

    var messageCount = 0;
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      var fieldList = new ArrayList<>(fieldMap.values());
      fieldList.sort((t1, t2) -> t2.toList.size() - t1.toList.size());

      var firstField = fieldList.iterator().next();
      writer.writeNext(firstField.getHeaders());

      for (var field : fieldList) {
        if (field.toList.size() > 0) {
          writer.writeNext(field.getValues());
          ++messageCount;
        }
      }

      writer.close();
      logger.info("wrote " + messageCount + " updated P2P fields to file: " + outputPath);
    } catch (

    Exception e) {
      logger.error("Exception writing file: " + outputPath.toString() + ", " + e.getLocalizedMessage());
    }

  }

  protected String makeKmlText() {
    var mapName = cm.getAsString(Key.EXERCISE_NAME);
    var mapDescription = cm.getAsString(Key.EXERCISE_DESCRIPTION);
    var stationPlacemarks = makeStationPlacemarks();
    var networkPlacemarks = makeNetworkPlacemarks();

    var kmlText = """
        <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
        <Document id="MAP_NAME">
          <name>MAP_NAME</name>
          <description>MAP_DESCRIPTION</description>
          <Style id="fieldpin">
              <IconStyle>
                <Icon>
                  <href>http://maps.google.com/mapfiles/kml/paddle/blu-blank.png</href>
                </Icon>
              </IconStyle>
            </Style>
            <Style id="targetpin">
              <IconStyle>
                <Icon>
                  <href>http://maps.google.com/mapfiles/kml/paddle/red-stars.png</href>
                </Icon>
              </IconStyle>
            </Style>
          STATION_PLACEMARKS
          NETWORK_PLACEMARKS
        </Document>
        </kml>
                """;

    kmlText = kmlText.replaceAll("MAP_NAME", mapName);
    kmlText = kmlText.replaceAll("MAP_DESCRIPTION", mapDescription);
    kmlText = kmlText.replaceAll("STATION_PLACEMARKS", stationPlacemarks);
    kmlText = kmlText.replaceAll("NETWORK_PLACEMARKS", networkPlacemarks);
    return kmlText;
  }

  protected String makeStationPlacemarks() {
    var sb = new StringBuilder();

    sb.append(makeFieldPlacemarks(fieldMap, targetMap));
    sb.append(makeTargetPlacemarks(fieldMap, targetMap));

    var s = sb.toString();
    return s;
  }

  private String makeFieldPlacemarks(Map<String, BaseField> fieldMap, Map<String, BaseTarget> targetMap) {
    var sb = new StringBuilder();

    for (var fieldCall : fieldMap.keySet()) {
      var field = fieldMap.get(fieldCall);

      if (field.toList.size() > 0) {
        sb.append("  <Placemark>\n");
        sb.append("    <name>" + fieldCall + "</name>\n");
        sb.append("    <styleUrl>#fieldpin</styleUrl>\n");
        sb.append("<description>");
        sb.append(makeFieldDescription(field, targetMap) + "\n");
        sb.append("</description>\n");
        sb.append("    <Point>\n");
        sb
            .append("    <coordinates>" + field.location.getLongitude() + "," + field.location.getLatitude()
                + "</coordinates>\n");
        sb.append("    </Point>\n");
        sb.append("  </Placemark>\n");
      }
    }

    var s = sb.toString();
    return s;
  }

  private String makeTargetPlacemarks(Map<String, BaseField> fieldMap2, Map<String, BaseTarget> targetMap2) {
    var sb = new StringBuilder();

    for (var targetCall : targetMap.keySet()) {
      var target = targetMap.get(targetCall);

      sb.append("  <Placemark>\n");
      sb.append("    <name>" + targetCall + "</name>\n");
      sb.append("    <styleUrl>#targetpin</styleUrl>\n");
      sb.append("    <description>");
      sb.append(makeTargetDescription(target, fieldMap) + "\n");
      sb.append("    </description>\n");
      sb.append("    <Point>\n");
      sb
          .append("    <coordinates>" + target.location.getLongitude() + "," + target.location.getLatitude()
              + "</coordinates>\n");
      sb.append("    </Point>\n");
      sb.append("  </Placemark>\n");
    }

    var s = sb.toString();
    return s;
  }

  protected String makeNetworkPlacemarks() {
    var drawnSet = new HashSet<String>();
    var sb = new StringBuilder();
    for (var targetCall : targetMap.keySet()) {
      var target = targetMap.get(targetCall);
      var fromList = target.fromList;
      for (var m : fromList) {
        var fieldCall = m.from;
        var field = fieldMap.get(fieldCall);
        if (field != null) {
          // add the link once, independent of number of messages from field to target
          var drawnKey = field.call + "-" + target.call;
          if (drawnSet.contains(drawnKey)) {
            continue;
          }
          drawnSet.add(drawnKey);
          sb.append("  <Placemark>\n");
          sb.append("  <name>" + fieldCall + "-" + targetCall + "</name>\n");
          sb.append("    <LineString>\n");
          sb
              .append("    <coordinates>" + field.location.getLongitude() + "," + field.location.getLatitude() //
                  + " " + target.location.getLongitude() + "," + target.location.getLatitude() + "</coordinates>\n");
          sb.append("    </LineString>\n");
          sb.append("  </Placemark>\n");
        }
      }
    }
    var s = sb.toString();
    return s;
  }

  protected void importTargetData() {
    File inputFile = new File(cm.getAsString(Key.P2P_TARGET_PATH));

    if (!inputFile.exists()) {
      logger.warn("file: " + inputFile + " not found");
      return;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputFile);
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;

        var target = makeTarget(fields);
        if (target.isActive) {
          targetMap.put(target.call, target);
        } else {
          logger.debug("skipping inactive target: " + target);
        }
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputFile + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("read " + targetMap.size() + " targets from: " + inputFile);
  }

  protected void importFieldData() {
    File inputFile = new File(cm.getAsString(Key.P2P_FIELD_PATH));
    if (!inputFile.exists()) {
      logger.warn("file: " + inputFile + " not found");
      return;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputFile);
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        var field = makeField(fields);
        if (field != null) {
          fieldMap.put(field.call, field);
          ++rowCount;
        }
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputFile + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("read " + fieldMap.size() + " fields from: " + inputFile);
  }

  protected LocalDateTime makeDateTime(String date, String time) {
    var localDate = LocalDate.parse(date, DATE_FORMATTER);
    var localTime = LocalTime.parse(time, TIME_FORMATTER);
    var dateTime = LocalDateTime.of(localDate, localTime);
    return dateTime;
  }

  /*
   * abstract method to be implemented by (named) P2P implementations
   */

  protected abstract BaseField makeField(String[] fields);

  protected abstract BaseTarget makeTarget(String[] fields);

  protected abstract String makeFieldDescription(BaseField field, Map<String, BaseTarget> targetMap2);

  protected abstract String makeTargetDescription(BaseTarget target, Map<String, BaseField> fieldMap2);

}
