/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.service.kml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.service.IService;

public class KmlService implements IService {
  private static final Logger logger = LoggerFactory.getLogger(KmlService.class);

  // see https://kml4earth.appspot.com/icons.html
  private static final String ICON_PATH = "http://maps.google.com/mapfiles/kml";

  public record KmlStyle(String id, String url) {
    public static final KmlStyle BLUE_PIN = new KmlStyle("bluePin", ICON_PATH + "paddle/blu-blank.png");
    public static final KmlStyle RED_STAR = new KmlStyle("redStar", ICON_PATH + "paddle/red-stars.png");
  }

  private final String mapName; // layer name
  private final String mapDescription;
  private final Set<KmlStyle> styleSet;
  private final StringBuilder content;

  public KmlService(String mapName, String mapDescription) {
    this.mapName = mapName;
    this.mapDescription = mapDescription;
    styleSet = new HashSet<>();
    content = new StringBuilder();
  }

  public void addPin(LatLongPair location, String name, String description) {
    addPin(location, name, description, KmlStyle.BLUE_PIN);
  }

  public void addPin(LatLongPair location, String name, String description, KmlStyle style) {
    styleSet.add(style);

    // see https://developers.google.com/kml/documentation/extendeddata for ExtendedData

    var sb = new StringBuilder();
    sb.append("<Placemark>\n");
    sb.append("<name>" + name + "</name>\n");
    sb.append("<styleUrl>#" + style.id + "</styleUrl>\n");
    sb.append("<description>\n");
    sb.append(description);
    sb.append("</description>\n");
    sb.append("<Point>\n");
    sb.append("<coordinates>" + location.getLongitude() + "," + location.getLatitude() + "</coordinates>\n");
    sb.append("</Point>\n");
    sb.append("</Placemark>\n");
    content.append(sb);
  }

  public void finalize(Path kmlFilePath) {
    var text = new String(KML_TEXT);
    text = text.replaceAll("MAP_NAME", mapName);
    text = text.replaceAll("MAP_DESCRIPTION", mapDescription);
    text = text.replaceAll("STYLE", makeStyles());
    text = text.replaceAll("CONTENT", content.toString());

    try {
      Files.writeString(kmlFilePath, text.toString());
      logger.info("wrote KML file to " + kmlFilePath);
    } catch (Exception e) {
      logger.error("Exception writing KML file " + kmlFilePath + ", " + e.getMessage());
    }
  }

  private String makeStyles() {
    var sb = new StringBuilder();

    for (var style : styleSet) {
      sb.append("<Style id=\"" + style.id + "\">\n");
      sb.append("  <IconStyle>\n");
      sb.append("    <Icon>\n");
      sb.append("        <href>" + style.url + "</href>\n");
      sb.append("    </Icon>\n");
      sb.append("  </IconStyle>\n");
      sb.append("</Style>\n");
    }

    return sb.toString();
  }

  @Override
  public String getName() {
    return "KmlService";
  }

  final String KML_TEXT = """
      <?xml version="1.0" encoding="UTF-8"?>
      <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
      <Document id="MAP_NAME">
        <name>MAP_NAME</name>
        <description>MAP_DESCRIPTION</description>
          STYLE
          CONTENT
      </Document>
      </kml>
         """;
}
