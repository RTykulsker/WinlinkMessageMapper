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

package com.surftools.wimp.service.map;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class LeafletMapEngine implements IMapService {
  private static final Logger logger = LoggerFactory.getLogger(LeafletMapEngine.class);

  @SuppressWarnings("unused")
  private IConfigurationManager cm;
  @SuppressWarnings("unused")
  private IMessageManager mm;

  public LeafletMapEngine(IConfigurationManager cm, IMessageManager mm) {
    this.cm = cm;
    this.mm = mm;
  }

  @Override
  public void makeMap(Path outputPath, MapHeader mapHeader, List<MapEntry> entries) {
    var sb = new StringBuilder();

    final Set<String> validColors = Set
        .of("blue", "gold", "red", "green", "orange", "yellow", "violet", "grey", "black");
    var labelIndex = 0;
    for (var entry : entries) {
      var color = entry.iconColor() == null ? "blue" : entry.iconColor();
      if (!validColors.contains(color)) {
        throw new RuntimeException("mapEntry: " + entry + ", invalid color: " + color);
      }
      var point = new String(POINT_TEMPLATE);
      point = point.replaceAll("#LABEL_INDEX#", "label_" + labelIndex++);
      point = point.replaceAll("#LABEL#", entry.label());
      point = point.replace("#LATITUDE#", entry.location().getLatitude());
      point = point.replace("#LONGITUDE#", entry.location().getLongitude());
      point = point.replace("#COLOR#", color);
      var message = entry.message().replaceAll("\n", "<br/>");
      point = point.replace("#CONTENT#", message);
      sb.append(point + "\n");
    }

    var fileContent = new String(FILE_TEMPLATE);
    fileContent = fileContent.replace("#TITLE#", mapHeader.title());
    fileContent = fileContent.replace("#POINTS#", sb.toString());

    var filePath = Path.of(outputPath.toString(), "leaflet-" + mapHeader.title() + ".html");
    try {
      Files.writeString(filePath, fileContent.toString());
      logger.info("wrote " + entries.size() + " entries to: " + filePath.toString());
    } catch (Exception e) {
      logger.error("Exception writing leaflet file: " + filePath.toString() + ", " + e.getMessage());
    }
  }

  private static final String POINT_TEMPLATE = """
      const #LABEL_INDEX# = L.marker([#LATITUDE#, #LONGITUDE#],{icon: #COLOR#Icon})
        .bindTooltip("#LABEL#",{permanent: true,direction: 'bottom', className: "my-labels"})
        .bindPopup('<b>#LABEL#</b><br/>#CONTENT#')
        .addTo(map);
      """;

  private static final String FILE_TEMPLATE = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <base target="_top">
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <title>#TITLE#</title>

          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
           integrity="sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY="
           crossorigin=""/>

          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"
           integrity="sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo="
           crossorigin=""></script>

        <style>
          html, body {
            height: 100%;
            width: 100%;
            margin: 0;
          }

          .leaflet-container {
              height: 1000px;
              width: 2000px;
              max-width: 100%;
              max-height: 100%;
          }

          .leaflet-tooltip.my-labels {
              background-color: transparent;
              border: transparent;
              box-shadow: none;
              font-weight: bold;
              font-size: 14px;
          }

          .leaflet-popup-tip {
              background: rgba(0, 0, 0, 0) !important;
              box-shadow: none !important;
          }

          .leaflet-tooltip-top:before,
          .leaflet-tooltip-bottom:before,
          .leaflet-tooltip-left:before,
          .leaflet-tooltip-right:before {
              border: none !important;
          }

        </style>

      </head>
      <body>

      <div id="map"></div>
      <script>

        const map = L.map('map').setView([40, -91], 4);

        const iconSizeX = [13,21];

        const blueIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png', iconSize: iconSizeX});
        const goldIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-gold.png', iconSize: iconSizeX});
        const redIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png', iconSize: iconSizeX});
        const greenIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png', iconSize: iconSizeX});
        const orangeIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-orange.png', iconSize: iconSizeX});
        const yellowIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-yellow.png', iconSize: iconSizeX});
        const violetIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-violet.png', iconSize: iconSizeX});
        const greyIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-grey.png', iconSize: iconSizeX});
        const blackIcon = new L.Icon({iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-black.png', iconSize: iconSizeX});

        const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          attribution: '&copy; <a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(map);

        #POINTS#

      </script>
      </body>
      </html>
            """;

  // public static void main(String[] args) throws Exception {
  // var outputPath = Path.of("2025-08-21/output");
  // var inputPath = Path.of(outputPath.toString(), "feedback-hics_259.csv");
  // var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(inputPath, ',', false, 1);
  // var mapEntries = new ArrayList<MapEntry>(fieldsArray.size());
  // for (var fieldEntry : fieldsArray) {
  // var pair = new LatLongPair(fieldEntry[1], fieldEntry[2]);
  // var content = "MessageId: " + fieldEntry[5] + "\n" + "Feedback Count: " + fieldEntry[3] + "\n" + "Feedback: "
  // + fieldEntry[4];
  // var mapEntry = new MapEntry(fieldEntry[0], pair, content);
  // mapEntries.add(mapEntry);
  // }
  // var mapService = new MapService(null, null);
  // mapService.makeMap(outputPath, new MapHeader("ETO-2025-08-21--HIC-259", ""), mapEntries);
  // }
}
