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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;

/**
 * produce information about "neighbors" for selected targets
 *
 * @author bobt
 *
 */
public class NeighborProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(NeighborProcessor.class);

  public static final String NL = "\n";
  public static final String INDENT1 = "  ";
  public static final String INDENT2 = INDENT1 + INDENT1;
  public static final String INDENT3 = INDENT2 + INDENT1;
  public static final String INDENT4 = INDENT3 + INDENT1;

  // from http://kml4earth.appspot.com/icons.html#paddle
  public static final String[] COLOR_NAMES = new String[] { "blu", "grn", "pink", "ylw", "wht", "ltblu", "purple",
      "red" };

  private final String DASHES = "------------------------------------------------------------";

  public static record Position(String call, LatLongPair location) {
  }

  public static record RangedPosition(Position position, int distanceMiles, int bearing)
      implements Comparable<RangedPosition> {

    @Override
    public int compareTo(RangedPosition o) {
      return this.distanceMiles - o.distanceMiles;
    }
  }

  public static record DistanceBound(int lower, int upper, String label) {

    public String getRange() {
      return "[" + lower + " - " + upper + ")";
    }

    @Override
    public String toString() {
      return getRange() + ((label == null) ? "" : " " + label);
    }

    /**
     * lower inclusive, upper exclusive
     *
     * @param distance
     * @return
     */
    public boolean contains(int distance) {
      return lower <= distance && distance < upper;
    }
  }

  private List<DistanceBound> boundsList;
  private Map<String, Position> positionMap;
  private Set<String> targetSet;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    // TODO make configurable
    var distanceBoundsString = "0,50,FM P2P;50,150,80m NVIS;150,300,40m NVIS;300,900,40m/30m/20m day - 80m/40m night ;900,2000,20m/15m day - 40m/30m night";
    boundsList = makeBounds(distanceBoundsString);

    // TODO make configurable
    var inputFileName = "2022-participantHistory.csv";
    var positionList = makePositionList(inputFileName);
    positionMap = new HashMap<>();
    for (var position : positionList) {
      positionMap.put(position.call, position);
    }

    // TODO make configurable
    var targetString = "KM6SO,W7OWO,W0LEN";
    targetSet = new TreeSet<String>(Arrays.asList(targetString.split(",")));
  }

  private List<Position> makePositionList(String inputFileName) {
    var list = new ArrayList<Position>();
    var path = Path.of(pathName, inputFileName);
    var fieldsList = ReadProcessor.readCsvFileIntoFieldsArray(path);
    var skipCount = 0;
    for (var fields : fieldsList) {
      var call = fields[0];
      var location = new LatLongPair(fields[3], fields[4]);
      if (!location.isValid() || (location.computeDistanceMeters(LatLongPair.ZERO_ZERO) == 0)) {
        logger.debug("skipping call: " + call + " because invalid location");
        ++skipCount;
        continue;
      }
      var position = new Position(call, location);
      list.add(position);
    }
    logger.info("found " + list.size() + " targets, skipped: " + skipCount);
    return list;
  }

  @Override
  public void process() {
    logger.info("begin");

    // the money shot
    for (var call : targetSet) {
      var targetPosition = positionMap.get(call);
      logger.info("targetCall: " + call + ", position: " + targetPosition);

      // set up the bins in "definition" order
      var bins = new LinkedHashMap<DistanceBound, Set<RangedPosition>>();
      for (var bound : boundsList) {
        bins.put(bound, new TreeSet<RangedPosition>());
      }

      for (var position : positionMap.values()) {
        var distance = LocationUtils.computeDistanceMiles(targetPosition.location(), position.location());
        var bearing = LocationUtils.computBearing(targetPosition.location(), position.location());
        var rangedPosition = new RangedPosition(position, distance, bearing);

        for (var bound : boundsList) {
          if (bound.contains(distance)) {
            var set = bins.get(bound);
            set.add(rangedPosition);
            bins.put(bound, set);
          } // end if bound contains
        } // end loop over bounds
      } // end loop over positions
      output(targetPosition, bins);
    } // end loop over targets

    logger.info("end");
  }

  public void output(Position targetPosition, LinkedHashMap<DistanceBound, Set<RangedPosition>> bins) {
    var outputPath = Path.of(pathName, "output", targetPosition.call());
    FileUtils.deleteDirectory(outputPath);
    FileUtils.createDirectory(outputPath);

    outputText(outputPath, targetPosition, bins);

    outputKml(outputPath, targetPosition, bins);
  }

  private void outputKml(Path outputPath, Position targetPosition,
      LinkedHashMap<DistanceBound, Set<RangedPosition>> bins) {

    var headerText = makeHeaderText(targetPosition, bins);
    var folderText = makeFolderText(targetPosition, bins);
    var footerText = makeFooterText();

    var sb = new StringBuilder();
    sb.append(headerText);
    sb.append(folderText);
    sb.append(footerText);
    var kmlText = sb.toString();
    writeKmlText(kmlText, targetPosition.call());
  }

  private String makeHeaderText(Position targetPosition, LinkedHashMap<DistanceBound, Set<RangedPosition>> bins) {
    var mapName = cm.getAsString(Key.EXERCISE_NAME, "Neighbor Map for ");
    mapName += targetPosition.call();
    var mapDescription = cm.getAsString(Key.EXERCISE_DESCRIPTION, "\"binned\" neighbors");

    var styleTemplate = """
          <Style id="COLOR-rangepin">
              <IconStyle>
                <Icon>
                  <href>http://maps.google.com/mapfiles/kml/paddle/COLOR-blank.png</href>
                </Icon>
              </IconStyle>
            </Style>
        """;
    var sb = new StringBuilder();
    for (var color : COLOR_NAMES) {
      var style = styleTemplate.replaceAll("COLOR", color);
      sb.append(style + NL);
    }
    var rangeStyles = sb.toString();

    var text = """
        <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
        <Document id="MAP_NAME">
          <name>MAP_NAME</name>
          <description>MAP_DESCRIPTION</description>
            RANGESTYLES
            <Style id="targetpin">
              <IconStyle>
                <Icon>
                  <href>http://maps.google.com/mapfiles/kml/paddle/red-stars.png</href>
                </Icon>
              </IconStyle>
            </Style>
                """;
    text = text.replaceAll("RANGESTYLES", rangeStyles);
    text = text.replaceAll("MAP_NAME", mapName);
    text = text.replaceAll("MAP_DESCRIPTION", mapDescription);
    return text;
  }

  private String makeFolderText(Position targetPosition, LinkedHashMap<DistanceBound, Set<RangedPosition>> bins) {
    var sb = new StringBuilder();

    var binIndex = -1;
    for (var bin : bins.keySet()) {
      ++binIndex;
      var list = bins.get(bin);

      sb.append(INDENT1 + "<Folder>" + NL);
      sb
          .append(INDENT2 + "<name>" + bin.label() //
              + ", from: " + bin.lower() + " to " + bin.upper() + " miles</name>" + NL);

      var targetPlacemark = makeTargetPlacemark(targetPosition, bin, list);
      sb.append(INDENT2 + targetPlacemark + NL);

      for (var ranged : list) {
        if (ranged.position().call().equalsIgnoreCase(targetPosition.call())) {
          continue;
        }
        String rangedPlacemarks = makeRangedPlacemarks(targetPosition, ranged, binIndex);
        sb.append(INDENT2 + rangedPlacemarks + NL);
      }

      sb.append(INDENT1 + "</Folder>" + NL);

    }

    var s = sb.toString();
    return s;
  }

  private String makeRangedPlacemarks(Position targetPosition, RangedPosition ranged, int binIndex) {
    StringBuilder sb = new StringBuilder();

    var rangedLon = ranged.position().location().getLongitude();
    var rangedLat = ranged.position().location().getLatitude();
    var rangedCall = ranged.position().call();

    var targetLon = targetPosition.location().getLongitude();
    var targetLat = targetPosition.location().getLatitude();
    var targetCall = targetPosition.call();

    var color = COLOR_NAMES[binIndex];

    // point
    sb.append(INDENT2 + "<Placemark>" + NL);
    sb.append(INDENT3 + "<name>" + ranged.position().call() + "</name>" + NL);
    sb.append(INDENT3 + "<styleUrl>#" + color + "-rangepin</styleUrl>" + NL);
    sb.append(INDENT3 + "<description>" + NL);
    sb.append(INDENT4 + "distance: " + ranged.distanceMiles() + ", miles" + ", bearing: " + ranged.bearing() + NL);
    sb.append(INDENT3 + "</description>" + NL);
    sb.append(INDENT3 + "<Point>" + NL);
    sb.append(INDENT3 + "<coordinates>" + rangedLon + "," + rangedLat + "</coordinates>" + NL);
    sb.append(INDENT3 + "</Point>" + NL);
    sb.append(INDENT2 + "</Placemark>" + NL);

    // line
    sb.append(INDENT2 + "<Placemark>" + NL);
    sb.append(INDENT3 + "<name>" + targetCall + "-" + rangedCall + "</name>" + NL);
    sb.append(INDENT3 + "<LineString>" + NL);
    sb
        .append(INDENT4 + "<coordinates>" + targetLon + "," + targetLat //
            + " " + rangedLon + "," + rangedLat + "</coordinates>" + NL);
    sb.append(INDENT3 + "</LineString>" + NL);
    sb.append(INDENT2 + "</Placemark>" + NL);

    var s = sb.toString();
    return s;
  }

  private String makeTargetPlacemark(Position targetPosition, DistanceBound bin, Set<RangedPosition> list) {
    var targetCall = targetPosition.call();
    var targetLon = targetPosition.location().getLongitude();
    var targetLat = targetPosition.location().getLatitude();

    var count = list.stream().filter(rp -> !rp.position().call().equals(targetCall)).count();
    // point
    StringBuilder sb = new StringBuilder();
    sb.append(INDENT2 + "<Placemark>" + NL);
    sb.append(INDENT3 + "<name>" + targetCall + "</name>" + NL);
    sb.append(INDENT3 + "<styleUrl>#targetpin</styleUrl>" + NL);
    sb.append(INDENT3 + "<description>" + NL);
    sb
        .append("from: " + bin.lower() + " to " + bin.upper() + " miles, (" + bin.label() + ") + including " + count
            + " stations" + NL);
    sb.append(DASHES + NL);
    for (var ranged : list) {
      if (ranged.position().call().equalsIgnoreCase(targetPosition.call())) {
        continue;
      }

      var distance = ranged.distanceMiles();
      var bearing = ranged.bearing();
      var call = ranged.position().call();
      sb.append(INDENT4 + "call: " + call + ", distance: " + distance + ", miles" + ", bearing: " + bearing + NL);
    }
    sb.append(INDENT3 + "</description>" + NL);
    sb.append(INDENT3 + "<Point>" + NL);
    sb.append(INDENT3 + "<coordinates>" + targetLon + "," + targetLat + "</coordinates>" + NL);
    sb.append(INDENT3 + "</Point>" + NL);
    sb.append(INDENT2 + "</Placemark>" + NL);

    var s = sb.toString();
    return s;
  }

  private String makeFooterText() {
    return """
        </Document>
        </kml>
                """;
  }

  private void writeKmlText(String kmlText, String call) {
    Path kmlPath = Path.of(outputPath.toString(), call, call + ".kml");
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(kmlPath.toFile()));
      writer.write(kmlText);
      writer.close();
      logger.info("wrote to: " + outputPath.toString());
    } catch (Exception e) {
      logger.error("Exception writing kml output file: " + kmlPath + ", " + e.getLocalizedMessage());
    }
  }

  private void outputText(Path outputPath, Position targetPosition,
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

  /**
   * bins can be overlapping and/or not-contiguous. Definitely not mutually exclusive and completely exhaustive
   *
   * @param distanceBoundsString
   * @return
   */
  private List<DistanceBound> makeBounds(String distanceBoundsString) {
    List<DistanceBound> list = new ArrayList<>();
    var distanceBoundsArray = distanceBoundsString.split(";");
    for (var distanceBoundString : distanceBoundsArray) {
      try {
        var fields = distanceBoundString.split(",");
        var lower = Integer.valueOf(fields[0]);
        var upper = Integer.valueOf(fields[1]);
        var label = fields.length >= 3 ? fields[2] : null;
        var bound = new DistanceBound(lower, upper, label);
        logger.info("bound: " + bound);
        list.add(bound);
      } catch (Exception e) {
        logger.error("Exception parsing distance bound: " + distanceBoundsString + ", " + e.getLocalizedMessage());
        System.exit(1);
      }
    }
    return list;
  }
}
