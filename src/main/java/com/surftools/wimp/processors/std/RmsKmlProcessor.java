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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.service.overrideLocationService.OverrideLocationService;
import com.surftools.wimp.service.rmsGateway.RmsGatewayResult;
import com.surftools.wimp.service.rmsGateway.RmsGatewayService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * processor to generate a KML file, based on various Winlink messages from "field" stations and RMS gateways being the
 * target stations.
 *
 * we can't just extend our AbstractBaseP2PProcessor, since we can have have multiple messages of same type, etc.
 *
 * @author bobt
 *
 */
public class RmsKmlProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(RmsKmlProcessor.class);
  private static DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");
  // private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected final String DASHES = "------------------------------------------------------------\n";

  // one entry for every message received
  public static record GatewayMessage(//
      String from, //
      String to, //
      String gateway, //
      int frequency, //
      String messageId, //
      LocalDateTime dateTime, //
      MessageType messageType, //
      String distanceMiles, //
      String bearingDegrees) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "To", "Gateway", "Frequency", "Band", "MiD", "DateTime", "Type", "Distance",
          "Bearing" };
    }

    @Override
    public String[] getValues() {
      return new String[] { from, to, gateway, String.valueOf(frequency), bandOf(frequency), messageId,
          dateTime.toString(), messageType.toString(), distanceMiles, bearingDegrees };
    }

    private String bandOf(int freq) {
      if (freq == 0 && gateway.equals("TELNET")) {
        return "TELNET";
      } else if (freq <= 30_000_000) {
        return "HF";
      } else if (freq <= 300_000_000) {
        return "VHF";
      } else if (freq <= Integer.MAX_VALUE) {
        return "UHF";
      } else {
        return "OTHER";
      }
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (GatewayMessage) other;

      var cmp = from.compareTo(o.from);
      if (cmp != 0) {
        return cmp;
      }

      cmp = to.compareTo(o.to);
      if (cmp != 0) {
        return cmp;
      }

      cmp = dateTime.compareTo(o.dateTime);
      if (cmp != 0) {
        return cmp;
      }

      return 0;
    }

  }

  static class Target implements IWritableTable {
    String call;
    LatLongPair location;
    List<ExportedMessage> messages;

    Target(String call, LatLongPair location) {

      this.call = call;
      this.location = location;

      messages = new ArrayList<>();
    }

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Station", "Latitude", "Longitude", "Message Count" };
    }

    @Override
    public String[] getValues() {
      var latitude = location != null && location.isValid() ? location.getLatitude() : "0.0";
      var longitude = location != null && location.isValid() ? location.getLongitude() : "0.0";
      return new String[] { call, latitude, longitude, String.valueOf(messages.size()) };
    }

    @Override
    public int compareTo(IWritableTable o) {
      var other = (Target) o;
      return call.compareTo(other.call);
    }
  }

  static class Field implements IWritableTable {
    String call;
    LatLongPair location;
    List<ExportedMessage> messages;

    Field(String call, LatLongPair location) {
      this.call = call;
      this.location = location;
      this.messages = new ArrayList<>();
    }

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", "Message Count" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, location.getLatitude(), location.getLongitude(), String.valueOf(messages.size()), };
    }

    @Override
    public int compareTo(IWritableTable o) {
      var other = (Field) o;
      return call.compareTo(other.call);
    }
  }

  private OverrideLocationService overrideLocationService;
  private RmsGatewayService rmsGatewayService;
  private Set<MessageType> requiredMessageTypeSet;
  private Map<String, Target> targetMap;
  private Map<String, Field> fieldMap;
  private List<IWritableTable> gatewayMessages;
  private Map<String, RmsGatewayResult> messageMap;

  private int ppMessageCount;
  private int ppNotFoundCount;

  private boolean showMessageTypes;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

    super.initialize(cm, mm, logger);

    overrideLocationService = new OverrideLocationService(cm);
    rmsGatewayService = new RmsGatewayService(cm);

    requiredMessageTypeSet = new HashSet<>();
    var rmsKmlMessageTypes = cm.getAsString(Key.RMS_KML_MESSAGE_TYPES);
    var typeFields = rmsKmlMessageTypes.split(",");
    for (var fieldTypeName : typeFields) {
      var type = MessageType.fromString(fieldTypeName);
      if (type == null) {
        throw new RuntimeException("unknown message type: " + fieldTypeName);
      }
      requiredMessageTypeSet.add(type);
    }

    showMessageTypes = cm.getAsBoolean(Key.RMS_KML_SHOW_MESSAGE_TYPES);

    var showDates = cm.getAsBoolean(Key.RMS_KML_SHOW_DATES, true);
    if (showDates) {
      DT_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm");
    } else {
      DT_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    }

    targetMap = new HashMap<>();
    fieldMap = new HashMap<>();
    gatewayMessages = new ArrayList<>();
    messageMap = new HashMap<>();
  }

  @Override
  public void process() {
    var typeIterator = requiredMessageTypeSet.iterator();
    while (typeIterator.hasNext()) {
      var type = typeIterator.next();
      var messages = mm.getMessagesForType(type);
      if (messages != null) {
        for (var m : mm.getMessagesForType(type)) {
          ++ppMessageCount;

          var rmsResult = rmsGatewayService.getLocationOfRmsGateway(m.from, m.messageId);
          if (rmsResult.isFound()) {
            var field = fieldMap.get(m.from);
            if (field == null) {
              field = new Field(m.from, m.mapLocation);
            }
            field.messages.add(m);
            fieldMap.put(m.from, field);

            var targetLocation = rmsResult.location();
            if (targetLocation == null || !targetLocation.isValid()) {
              logger.info("before overrideLocationService for call: " + rmsResult.gatewayCallsign());
              targetLocation = overrideLocationService.getLocation(rmsResult.gatewayCallsign());
              logger.info("after: location: " + targetLocation);
            }

            var target = targetMap.get(rmsResult.gatewayCallsign());
            if (target == null) {
              target = new Target(rmsResult.gatewayCallsign(), targetLocation);
            }
            target.messages.add(m);
            targetMap.put(rmsResult.gatewayCallsign(), target);

            var distanceMiles = "n/a";
            var bearingDegrees = "n/a";
            if (rmsResult != null && rmsResult.location() != null && rmsResult.location().isValid()
                && m.mapLocation != null) {
              distanceMiles = String.valueOf(LocationUtils.computeDistanceMiles(m.mapLocation, rmsResult.location()));
              bearingDegrees = String.valueOf(LocationUtils.computBearing(m.mapLocation, rmsResult.location()));
            }

            var gatewayMessage = new GatewayMessage(m.from, m.to, rmsResult.gatewayCallsign(), rmsResult.frequency(),
                m.messageId, m.msgDateTime, type, distanceMiles, bearingDegrees);
            gatewayMessages.add(gatewayMessage);

            messageMap.put(m.messageId, rmsResult);
          } else {// end if rmsResult.isFound
            ++ppNotFoundCount;
            logger.info("### no rmsEntry for call: " + m.from + ", messageId: " + m.messageId);
          } // end if rmsResult not found
        } // end loop over message of type
      } // end if messages != null
    } // end loop over types

  }

  @Override
  public void postProcess() {
    var fields = new ArrayList<IWritableTable>(fieldMap.values());
    Collections.sort(fields);
    writeTable("rmsFieldStations.csv", fields);

    var targets = new ArrayList<IWritableTable>(targetMap.values());
    Collections.sort(targets);
    writeTable("rmsTargetStations.csv", targets);

    Collections.sort(gatewayMessages);
    writeTable("rmsGateways.csv", gatewayMessages);

    var kmlText = makeKmlText();
    writeKml(kmlText);

    var sb = new StringBuilder();
    sb.append("\n\nSummary\n");
    sb.append("total messages received: " + ppMessageCount + "\n");
    sb.append("total messages with no RMS gateway: " + ppNotFoundCount + "\n");
    logger.info(sb.toString());
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

  protected void writeKml(String kmlText) {
    try {
      Path outputPath = Path.of(outputPathName, "rms.kml");

      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
      writer.write(kmlText);

      writer.close();
      logger.info("wrote to: " + outputPath.toString());
    } catch (Exception e) {
      logger.error("Exception writing kml output file: " + e.getLocalizedMessage());
    }
  }

  protected String makeStationPlacemarks() {
    var sb = new StringBuilder();

    sb.append(makeFieldPlacemarks(fieldMap, targetMap, messageMap));
    sb.append(makeTargetPlacemarks(fieldMap, targetMap, messageMap));

    var s = sb.toString();
    return s;
  }

  private String makeFieldPlacemarks(Map<String, Field> fieldMap, Map<String, Target> targetMap,
      Map<String, RmsGatewayResult> messageMap) {
    var sb = new StringBuilder();

    for (var fieldCall : fieldMap.keySet()) {
      var field = fieldMap.get(fieldCall);

      // var debug = false;
      // if (field.location == null) {
      // debug = true;
      // }

      if (field.messages.size() > 0 && field.location != null) {
        sb.append("  <Placemark>\n");
        sb.append("    <name>" + fieldCall + "</name>\n");
        sb.append("    <styleUrl>#fieldpin</styleUrl>\n");
        sb.append("<description>");
        sb.append(makeFieldDescription(field, targetMap, messageMap) + "\n");
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

  private String makeTargetPlacemarks(Map<String, Field> fieldMap, Map<String, Target> targetMap,
      Map<String, RmsGatewayResult> messageMap) {
    var sb = new StringBuilder();

    for (var targetCall : targetMap.keySet()) {
      var target = targetMap.get(targetCall);

      if (target.location == null) {
        continue;
      }

      sb.append("  <Placemark>\n");
      sb.append("    <name>" + targetCall + "</name>\n");
      sb.append("    <styleUrl>#targetpin</styleUrl>\n");
      sb.append("    <description>");
      sb.append(makeTargetDescription(target, fieldMap, messageMap) + "\n");
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
      if (target.location == null) {
        continue;
      }
      var messages = target.messages;
      for (var m : messages) {
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

  protected String makeFieldDescription(Field field, Map<String, Target> targetMap,
      Map<String, RmsGatewayResult> messageMap) {

    var sb = new StringBuilder();
    var messages = field.messages;
    Collections.sort(messages);

    sb.append("Outbound messages: " + field.messages.size() + "\n");
    sb.append(DASHES);
    for (var m : messages) {
      var time = DT_FORMATTER.format(m.msgDateTime);
      var to = m.to;
      var rmsResult = messageMap.get(m.messageId);

      sb.append(time + ", to: " + to + ", via: " + rmsResult.gatewayCallsign());
      var rmsLocation = rmsResult.location();
      if (field.location != null && rmsLocation != null && rmsLocation.isValid()
          && !rmsResult.gatewayCallsign().equalsIgnoreCase("TELNET")) {
        var distanceMiles = LocationUtils.computeDistanceMiles(field.location, rmsLocation);
        sb.append(" (" + distanceMiles + " mi, " + formatFrequency(rmsResult.frequency()) + ")");
      }

      if (showMessageTypes) {
        sb.append(", type: " + m.getMessageType().toString());
      }

      sb.append("\n");
    }

    return sb.toString();
  }

  protected String makeTargetDescription(Target target, Map<String, Field> fieldMap,
      Map<String, RmsGatewayResult> messageMap) {
    var messages = target.messages;
    Collections.sort(messages);
    var sb = new StringBuilder();
    sb.append("Inbound messages: " + messages.size() + "\n");
    sb.append(DASHES);
    for (var m : messages) {
      var time = DT_FORMATTER.format(m.msgDateTime);
      var from = m.from;
      var location = m.mapLocation;

      sb.append(time + " from: " + from);
      if (location != null && location.isValid() && target.location != null && target.location.isValid()) {
        var distanceMiles = LocationUtils.computeDistanceMiles(target.location, location);
        var rmsResult = messageMap.get(m.messageId);
        sb.append(" (" + distanceMiles + " mi, " + formatFrequency(rmsResult.frequency()) + ")");
      }

      sb.append(", to: " + m.to);

      if (showMessageTypes) {
        sb.append(", type: " + m.getMessageType().toString());
      }

      sb.append("\n");
    }
    return sb.toString();
  }

  private String formatFrequency(int frequency) {
    return String.format("%.3f", (frequency / 1_000_000d)) + " MHz";
  }

}
