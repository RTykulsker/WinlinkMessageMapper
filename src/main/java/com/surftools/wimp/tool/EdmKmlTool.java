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

package com.surftools.wimp.tool;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.opencsv.CSVWriter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;

/**
 * simple app to transform a Kml file from the EDM team into a flat, CSV file
 *
 * download the KML file from the EDM map site: https://batchgeo.com/map/c673e37e302f4f10fe0036dd28589562
 *
 * or just use: https://mygeodata.cloud/converter/kml-to-csv
 *
 * @author bobt
 *
 */

public class EdmKmlTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(EdmKmlTool.class);

  @Option(name = "--inputFileName", usage = "path to input kml file", required = false)
  private String inputFileName = "/home/bobt/Downloads/EDM Target Stations.kml";

  @Option(name = "--filterIncludeTeams", usage = "filter to INCLUDE only these teams", required = false)
  private String filterIncludeTeamsString = "B";

  @Option(name = "--filterTeams", usage = "filter to EXCLUDE these calls", required = false)
  private String filterExcludeCallsString = "W6RT,W1IZZ,N8AI,N1ROG,K9JEC,N7UWX,KJ4TKA,KQ4GIW";

  @Option(name = "--myGrid", usage = "maidenhead grid of my location", required = false)
  private String myGridString = "CN87vm";
  private LatLongPair myGrid;

  public static void main(String[] args) {
    EdmKmlTool app = new EdmKmlTool();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  static record EdmTarget(String call, String latitude, String longitude, String cityState, //
      String firstName, String lastName, String email, //
      String team, String channel, String femaRegion, String centerFrequency, //
      String band, int distanceMiles //
  ) implements Comparable<EdmTarget> {

    @Override
    public int compareTo(EdmTarget o) {
      var cmp = -1 * band.compareTo(o.band);
      if (cmp != 0) {
        return cmp;
      }

      cmp = distanceMiles - o.distanceMiles;
      if (cmp != 0) {
        return cmp;
      }

      return Double.valueOf(centerFrequency).compareTo(Double.valueOf(centerFrequency));
    }

    public static String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", "Location", //
          "First Name", "Last Name", "Email", //
          "Team", "Channel", "FEMA", "Center Freq", "Dial Freq", "Band", "Distance Mi" };
    }

    public String[] getValues() {
      var dialFrequency = String.valueOf(Double.valueOf(centerFrequency) - 1.5d);
      return new String[] { call, latitude, longitude, cityState, //
          firstName, lastName, email, team, channel, femaRegion, centerFrequency, dialFrequency, band,
          String.valueOf(distanceMiles) };
    }
  }

  /**
   * main processing function
   */
  private void run() throws Exception {

    logger.info("begin");

    myGrid = new LatLongPair(myGridString);
    var content = Files.readString(Path.of(inputFileName));
    var rawTargets = readTargets(content);
    var targets = filterTargets(rawTargets, filterIncludeTeamsString, filterExcludeCallsString);

    if (targets.size() > 0) {
      write(targets, Path.of(inputFileName).getParent());
    } else {
      logger.info("no targets to write");
    }

    logger.info("exiting");
  }

  private void write(List<EdmTarget> targets, Path parentPath) throws Exception {
    var outputPath = Path.of(parentPath.toString(), "EdmKmlTool-Output");
    var outputString = outputPath.toAbsolutePath().toString();
    Files.createDirectories(outputPath);

    writeTargets(Path.of(outputString, "targets.csv"), targets);
    writeCalls(Path.of(outputString, "targets.txt"), targets);
    writeP2pFavorites(Path.of(outputString, "Vara P2P Favorites.dat"), targets);
  }

  private List<EdmTarget> readTargets(String content) throws Exception {
    var targets = new ArrayList<EdmTarget>();

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(new InputSource(new StringReader(content)));
    doc.getDocumentElement().normalize();
    NodeList nodeList = doc.getElementsByTagName("Placemark");
    var nTargets = nodeList.getLength();
    logger.info("read " + nTargets + " targets from " + inputFileName);
    for (var iTarget = 0; iTarget < nTargets; ++iTarget) {
      targets.add(readPlacemark((Element) nodeList.item(iTarget)));
    } // end for over placemarks

    return targets;
  }

  private List<EdmTarget> filterTargets(List<EdmTarget> rawTargets, String filterIncludeTeamsString,
      String filterExcludeCallsString) {
    var targets = new ArrayList<EdmTarget>();

    Set<String> filterIncludeTeamsSet = new HashSet<>();
    if (filterIncludeTeamsString != null) {
      var fields = filterIncludeTeamsString.split(",");
      filterIncludeTeamsSet = Set.of(fields);
    }

    Set<String> filterExcludeCallSet = new HashSet<>();
    if (filterExcludeCallsString != null) {
      var fields = filterExcludeCallsString.split(",");
      filterExcludeCallSet = Set.of(fields);
    }

    for (var target : rawTargets) {
      if (filterIncludeTeamsSet.size() > 0) {
        if (!filterIncludeTeamsSet.contains(target.team)) {
          logger.info("dropping target because not a filtered team: " + target);
          continue;
        }
      }

      if (filterExcludeCallSet.contains(target.call)) {
        logger.info("dropping target because filtered call: " + target);
      }

      targets.add(target);
    }

    logger.info("filtered to " + targets.size() + " targets");
    return targets;
  }

  private void writeCalls(Path outputPath, List<EdmTarget> targets) throws IOException {
    Collections.sort(targets, (t1, t2) -> t1.call.compareTo(t2.call));
    var calls = targets.stream().map(EdmTarget::call).toList();
    Files.writeString(outputPath, String.join(";", calls));
  }

  /**
   * call|center-freq/bandwidth for example AH6T|7119.5/500
   *
   * @param outputPath
   * @param targets
   * @throws IOException
   */
  private void writeP2pFavorites(Path outputPath, List<EdmTarget> targets) throws IOException {
    Collections.sort(targets);
    Function<EdmTarget, String> lambda = (t) -> t.call + "|" + t.centerFrequency + "/500";
    var calls = targets.stream().map(lambda).toList();
    Files.writeString(outputPath, String.join("\n", calls));
  }

  void writeTargets(Path outputPath, List<EdmTarget> targets) throws IOException {
    Collections.sort(targets);

    CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
    writer.writeNext(EdmTarget.getHeaders());
    for (var m : targets) {
      writer.writeNext(m.getValues());
    }
    writer.close();
    logger.info("wrote " + targets.size() + " targets to " + outputPath.toString());
  }

  private EdmTarget readPlacemark(Element element) {
    // top level elements: name, ExtendedData, Point, styleUrl
    var firstName = element.getElementsByTagName("name").item(0).getTextContent();
    var coordinates = element.getElementsByTagName("coordinates").item(0).getTextContent().split(",");
    var cityState = element.getElementsByTagName("address").item(0).getTextContent();
    var extendedData = (Element) element.getElementsByTagName("ExtendedData").item(0);
    var dataList = extendedData.getElementsByTagName("Data");

    String call = null;
    String latitude = null;
    String lastName = null;
    String team = null;
    String email = null;
    String longitude = null;
    String channel = null;
    String femaRegion = null;
    String centerFrequency = null;
    String band = null;

    if (coordinates != null && coordinates.length >= 2) { // longitude, latitude, altitude
      latitude = coordinates[1];
      longitude = coordinates[0];
    }

    for (var i = 0; i < dataList.getLength(); ++i) {
      var data = (Element) dataList.item(i);
      var attributeName = data.getAttribute("name");
      var value = data.getElementsByTagName("value").item(0).getTextContent();
      if (attributeName.equals("Last Name")) {
        lastName = value;
      } else if (attributeName.equals("Call Sign")) {
        call = value;
      } else if (attributeName.equals("FEMA")) {
        femaRegion = value;
      } else if (attributeName.equals("Team")) {
        team = value;
      } else if (attributeName.equals("Channel")) {
        channel = value;
      } else if (attributeName.equals("Center Freq.")) {
        centerFrequency = value;
        band = getBand(centerFrequency);
      } else if (attributeName.equals("Email")) {
        email = value;
      } else {
        throw new RuntimeException("Unknown attribute: " + attributeName);
      }
    }

    var targetPair = new LatLongPair(latitude, longitude);
    var distanceMiles = LocationUtils.computeDistanceMiles(myGrid, targetPair);

    var target = new EdmTarget(call, latitude, longitude, cityState, //
        firstName, lastName, email, team, channel, femaRegion, centerFrequency, //
        band, distanceMiles);//

    logger.info(target.toString());
    return target;
  }

  public final String getBand(String freqString) {
    double d = Double.parseDouble(freqString);

    if (3500 <= d && d <= 4000) {
      return "80m";
    } else if (7000 <= d && d <= 7300) {
      return "40m";
    } else if (10100 <= d && d <= 10150) {
      return "30m";
    } else if (14000 <= d && d <= 14350) {
      return "20m";
    } else if (21000 <= d && d <= 21450) {
      return "15m";
    } else {
      throw new RuntimeException("no band defined for frequency: " + freqString);
    }
  }

}
