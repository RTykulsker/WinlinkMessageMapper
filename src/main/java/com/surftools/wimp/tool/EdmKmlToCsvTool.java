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
import java.util.List;
import java.util.TreeMap;

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

public class EdmKmlToCsvTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(EdmKmlToCsvTool.class);

  @Option(name = "--inputFileName", usage = "path to input kml file", required = false)
  private String inputFileName = "/home/bobt/Documents/edm/EDM Target Stations-2023-11-21.kml";

  public static void main(String[] args) {
    EdmKmlToCsvTool app = new EdmKmlToCsvTool();
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
      String team, String channel, String femaRegion, String centerFrequency, String band)
      implements Comparable<EdmTarget> {

    @Override
    public int compareTo(EdmTarget o) {
      var cmp = team.compareTo(o.team);
      if (cmp != 0) {
        return cmp;
      }
      return Integer.valueOf(channel).intValue() - Integer.valueOf(o.channel).intValue();
    }

    public static String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", "Location", //
          "First Name", "Last Name", "Email", //
          "Team", "Channel", "FEMA", "Center Freq", "Dial Freq", "Band" };
    }

    public String[] getValues() {
      var dialFrequency = String.valueOf(Double.valueOf(centerFrequency) - 1.5d);
      return new String[] { call, latitude, longitude, cityState, //
          firstName, lastName, email, team, channel, femaRegion, centerFrequency, dialFrequency, band };
    }
  }

  /**
   * main processing function
   */
  private void run() throws Exception {

    logger.info("begin");

    var content = Files.readString(Path.of(inputFileName));
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

    if (targets.size() > 0) {
      var outputFileName = inputFileName.replaceAll(".kml", ".csv");
      writeTargets(outputFileName, targets);

      // build targetMap
      var targetMap = new TreeMap<String, List<EdmTarget>>();
      for (var target : targets) {
        var team = target.team();
        var list = targetMap.getOrDefault(team, new ArrayList<EdmTarget>());
        list.add(target);
        targetMap.put(team, list);
      }

      // write targetMap values
      for (var team : targetMap.keySet()) {
        outputFileName = inputFileName.replaceAll(".kml", "-team-" + team + ".csv");
        writeTargets(outputFileName, targetMap.get(team));
      }

    } // end if output

    logger.info("exiting");
  }

  void writeTargets(String outputFileName, List<EdmTarget> targets) throws IOException {
    Collections.sort(targets);

    CSVWriter writer = new CSVWriter(new FileWriter(Path.of(outputFileName).toString()));
    writer.writeNext(EdmTarget.getHeaders());
    for (var m : targets) {
      writer.writeNext(m.getValues());
    }
    writer.close();
    logger.info("wrote " + targets.size() + " targets to " + outputFileName);
  }

  // this is AFTER pretty printing
  String example = """
      <Placemark>
        <styleUrl>#0</styleUrl>
        <name>Don</name>
        <ExtendedData>
          <Data name='Last Name'>
            <value>Rolph</value>
          </Data>
          <Data name='Call Sign'>
            <value>AB1PH</value>
          </Data>
          <Data name='FEMA'>
            <value>1</value>
          </Data>
          <Data name='Team'>
            <value>A</value>
          </Data>
          <Data name='Channel'>
            <value>6</value>
          </Data>
          <Data name='Center Freq.'>
            <value>3591.5</value>
          </Data>
          <Data name='Email'>
            <value>don.rolph@gmail.com</value>
          </Data>
        </ExtendedData>
        <address>East Walpole, MA </address>
        <Point>
          <coordinates>-71.2101068,42.1614952,0</coordinates>
        </Point>
      </Placemark>
            """;

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

    var target = new EdmTarget(call, latitude, longitude, cityState, firstName, lastName, email, team, channel,
        femaRegion, centerFrequency, band);
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
