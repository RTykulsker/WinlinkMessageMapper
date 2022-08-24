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

package com.surftools.winlinkMessageMapper.tool.p2p.pickup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;

public class KmlManager {
  private static final Logger logger = LoggerFactory.getLogger(KmlManager.class);

  private final String DASHES = "------------------------------------------------------------";

  private final IConfigurationManager cm;

  public KmlManager(IConfigurationManager cm) {
    this.cm = cm;
  }

  public void run(Map<String, Field> fieldMap, Map<String, Target> targetMap) {

    var templateText = readTemplateText(cm.getAsString(PickupKey.KML_TEMPLATE_PATH));
    if (templateText == null) {
      return;
    }

    var stationPlacemarks = makeStationPlacemarks(fieldMap, targetMap);
    var networkPlacements = makeNetworkPlacemarks(fieldMap, targetMap);
    var kmlText = processTemplate(templateText, stationPlacemarks, networkPlacements,
        cm.getAsString(PickupKey.MAP_NAME), cm.getAsString(PickupKey.MAP_DESCRIPTION));
    if (kmlText == null) {
      return;
    }

    writeKmlText(kmlText, cm.getAsString(PickupKey.OUTPUT_PATH));

  }

  /**
   * just kick the can ...
   *
   * @param fieldMap
   * @param targetMap
   * @return
   */
  private String makeStationPlacemarks(Map<String, Field> fieldMap, Map<String, Target> targetMap) {
    var sb = new StringBuilder();

    sb.append(makeFieldPlacemarks(fieldMap, targetMap));
    sb.append(makeTargetPlacemarks(fieldMap, targetMap));

    var s = sb.toString();
    return s;
  }

  private String makeFieldPlacemarks(Map<String, Field> fieldMap, Map<String, Target> targetMap) {
    var sb = new StringBuilder();

    for (var fieldCall : fieldMap.keySet()) {
      var field = fieldMap.get(fieldCall);

      if (field.reservedCalls.size() > 0) {
        sb.append("  <Placemark>\n");
        sb.append("    <name>" + fieldCall + "</name>\n");
        sb.append("    <styleUrl>#fieldpin</styleUrl>\n");
        sb.append("<description>");
        sb.append(makeFieldDescription(field, targetMap) + "\n");
        sb.append("</description>\n");
        sb.append("    <Point>\n");
        sb.append("    <coordinates>" + field.getLongitude() + "," + field.getLatitude() + "</coordinates>\n");
        sb.append("    </Point>\n");
        sb.append("  </Placemark>\n");
      }
    }

    var s = sb.toString();
    return s;
  }

  private String makeFieldDescription(Field field, Map<String, Target> targetMap) {
    var sb = new StringBuilder();

    sb.append("Outbound reservations: " + field.reservedCalls.size() + "\n");
    sb.append("Outbound completions:  " + field.completedCalls.size() + "\n");
    sb.append("Completion percentage: " + String.format("%.2f", 100d * field.completionPercent) + "%\n");
    sb.append("Completion score:      " + field.completionScore + "\n");

    sb.append(DASHES + "\n");
    for (var targetCall : field.reservedCalls) {
      var target = targetMap.get(targetCall);
      var isCompleted = field.completedCalls.contains(targetCall);
      var distanceMiles = field.latlong.computeDistanceMiles(target.latlong);
      var band = target.band;
      sb
          .append(target.call + ", (" + band + "m, " + distanceMiles + " miles) " + (isCompleted ? "completed!" : "")
              + "\n");
    }

    var s = sb.toString();
    return s;
  }

  private String makeTargetPlacemarks(Map<String, Field> fieldMap, Map<String, Target> targetMap) {
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
      sb.append("    <coordinates>" + target.getLongitude() + "," + target.getLatitude() + "</coordinates>\n");
      sb.append("    </Point>\n");
      sb.append("  </Placemark>\n");
    }

    var s = sb.toString();
    return s;
  }

  private String makeTargetDescription(Target target, Map<String, Field> fieldMap) {
    var sb = new StringBuilder();

    sb.append(target.dialFreq + " KHz dial, " + target.location + "\n");
    sb.append(DASHES + "\n");
    sb.append("Inbound reservations: " + target.reservedCalls.size() + "\n");
    sb.append("Inbound completions: " + target.completedCalls.size() + "\n");
    sb.append(DASHES + "\n");

    var nullFieldCount = 0;
    for (var fieldCall : target.reservedCalls) {
      var field = fieldMap.get(fieldCall);
      if (field != null) {
        var isCompleted = target.completedCalls.contains(fieldCall);
        var distanceMiles = target.latlong.computeDistanceMiles(field.latlong);
        sb.append(fieldCall + " (" + distanceMiles + " miles) " + (isCompleted ? "completed!" : "") + "\n");
      } else {
        ++nullFieldCount;
        logger.debug("null field for call: " + fieldCall);
      }
    }
    logger.info("Target: " + target.call + ", null field count: " + nullFieldCount);

    var s = sb.toString();
    return s;
  }

  private String makeNetworkPlacemarks(Map<String, Field> fieldMap, Map<String, Target> targetMap) {
    var sb = new StringBuilder();
    for (var targetCall : targetMap.keySet()) {
      var target = targetMap.get(targetCall);
      var completedCalls = target.completedCalls;
      for (var fieldCall : completedCalls) {
        var field = fieldMap.get(fieldCall);
        if (field != null) {
          sb.append("  <Placemark>\n");
          sb.append("  <name>" + fieldCall + "-" + targetCall + "</name>\n");
          sb.append("    <LineString>\n");
          sb
              .append("    <coordinates>" + field.getLongitude() + "," + field.getLatitude() //
                  + " " + target.getLongitude() + "," + target.getLatitude() + "</coordinates>\n");
          sb.append("    </LineString>\n");
          sb.append("  </Placemark>\n");
        }
      }
    }
    var s = sb.toString();
    return s;
  }

  /**
   * substitute for template variables
   *
   * @param templateText
   * @param stationPlacemarks
   * @param networkPlacements
   * @param mapName
   * @param mapDescription
   * @return
   */
  private String processTemplate(String templateText, String stationPlacemarks, String networkPlacements,
      String mapName, String mapDescription) {
    Map<String, String> variableMap = new HashMap<>();
    variableMap.put("mapName", cm.getAsString(PickupKey.MAP_NAME));
    variableMap.put("mapDescription", cm.getAsString(PickupKey.MAP_DESCRIPTION));
    variableMap.put("station-placemarks", stationPlacemarks);
    variableMap.put("network-placemarks", networkPlacements);

    final String regex = "\\$\\{([^}]++)\\}";
    final Pattern pattern = Pattern.compile(regex);
    // https://stackoverflow.com/questions/17462146
    Matcher matcher = pattern.matcher(templateText);
    String kmlText = templateText;
    while (matcher.find()) {
      String token = matcher.group(); // Ex: ${fizz}
      String tokenKey = matcher.group(1); // Ex: fizz
      String replacementValue = null;

      if (variableMap.containsKey(tokenKey)) {
        replacementValue = variableMap.get(tokenKey);
        try {
          kmlText = kmlText.replaceFirst(Pattern.quote(token), replacementValue);
        } catch (Exception e) {
          logger.error("Exception for token: " + token + ", " + e.getMessage());
        }
      } else {
        logger.error("String contained an unsupported token: " + token);
      }
    }

    return kmlText;
  }

  private String readTemplateText(String templateFileName) {
    try {
      return Files.readString(Path.of(templateFileName));
    } catch (Exception e) {
      logger.error("Exception reading template file: " + templateFileName + ", " + e.getLocalizedMessage());
      return null;
    }
  }

  private void writeKmlText(String kmlText, String kmlOutputPath) {
    try {
      File outputDirectory = new File(kmlOutputPath);
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      Path outputPath = Path.of(outputDirectory.getAbsolutePath(), "p2p.kml");
      BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()));
      writer.write(kmlText);

      writer.close();
      logger.info("wrote to: " + outputPath.toString());
    } catch (Exception e) {
      logger.error("Exception writing kml output file: " + e.getLocalizedMessage());
    }

  }

}
