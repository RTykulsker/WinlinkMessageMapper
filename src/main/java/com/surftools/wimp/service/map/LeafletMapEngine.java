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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class LeafletMapEngine implements IMapService {
  private static final Logger logger = LoggerFactory.getLogger(LeafletMapEngine.class);

  private IConfigurationManager cm;

  private String exerciseOrganization;
  private Map<String, String> colorMap;

  final Map<String, String> etoColorMap = Map
      .ofEntries(//
          Map.entry("ETO-01", "yellow"), //
          Map.entry("ETO-02", "blue"), //
          Map.entry("ETO-03", "red"), //
          Map.entry("ETO-04", "green"), //
          Map.entry("ETO-05", "orange"), //
          Map.entry("ETO-06", "violet"), //
          Map.entry("ETO-07", "yellow"), //
          Map.entry("ETO-08", "green"), //
          Map.entry("ETO-09", "red"), //
          Map.entry("ETO-10", "blue"), //
          Map.entry("ETO-CAN", "violet"), //
          Map.entry("ETO-DX", "gold"), //
          Map.entry("unknown", "grey"));

  final Map<String, String> rgbMap = Map
      .ofEntries( //
          Map.entry("blue", "2a81cb"), //
          Map.entry("gold", "ffd326"), //
          Map.entry("red", "cb2b32"), //
          Map.entry("green", "2aad27"), //
          Map.entry("orange", "cb8427"), //
          Map.entry("yellow", "cac428"), //
          Map.entry("violet", "9c2bcb"), //
          Map.entry("grey", "7b7b7b"), //
          Map.entry("black", "3d3d3d") //
      );

  final Set<String> ALL_ICON_COLORS = Set
      .of("blue", "gold", "red", "green", "orange", "yellow", "violet", "grey", "black");

  final Set<String> VALID_ICON_COLORS = Set // no grey
      .of("blue", "gold", "red", "green", "orange", "yellow", "violet", "black");

  public LeafletMapEngine(IConfigurationManager cm, IMessageManager mm) {
    this.cm = cm;
    this.exerciseOrganization = cm.getAsString(Key.EXERCISE_ORGANIZATION);
  }

  public static String escapeForJavaScript(String input) {
    if (input == null) {
      return null;
    }
    // First escape backslashes to avoid double escaping
    String escaped = input.replace("\\", "\\\\");
    // Then escape apostrophes (0x27)
    escaped = escaped.replace("'", "\\'");
    // Optionally escape newlines if embedding directly
    escaped = escaped.replace("\n", "\\n").replace("\r", "\\r");
    return escaped;
  }

  @Override
  public void makeMap(Path outputPath, MapHeader mapHeader, List<MapEntry> entries) {
    var sb = new StringBuilder();

    var labelIndex = 0;
    for (var entry : entries) {
      var color = entry.iconColor() == null ? "blue" : entry.iconColor();
      if (!ALL_ICON_COLORS.contains(color)) {
        throw new RuntimeException("mapEntry: " + entry + ", invalid color: " + color);
      }
      var point = new String(POINT_TEMPLATE);
      point = point.replaceAll("#LABEL_INDEX#", "label_" + labelIndex++);
      point = point.replaceAll("#LABEL#", entry.label());
      point = point.replace("#LATITUDE#", entry.location().getLatitude());
      point = point.replace("#LONGITUDE#", entry.location().getLongitude());
      point = point.replace("#COLOR#", color);
      var message = entry.message().replaceAll("\n", "<br/>");
      message = escapeForJavaScript(message);
      point = point.replace("#CONTENT#", message);
      sb.append(point + "\n");
    }

    var legendHTML = mapHeader.legendHTML();

    var fileContent = new String(FILE_TEMPLATE);
    fileContent = fileContent.replaceAll("#TITLE#", mapHeader.mapTitle());
    fileContent = fileContent.replace("#POINTS#", sb.toString());
    fileContent = fileContent.replace("#LEGEND_HTML#", legendHTML);

    var filePath = Path.of(outputPath.toString(), "leaflet-" + mapHeader.fileName() + ".html");
    try {
      Files.writeString(filePath, fileContent.toString());
      logger.info("wrote " + entries.size() + " entries to: " + filePath.toString());
    } catch (Exception e) {
      logger.error("Exception writing leaflet file: " + filePath.toString() + ", " + e.getMessage());
    }
  }

  private static final String POINT_TEMPLATE = """
      addMarkerWithLabel(#LATITUDE#, #LONGITUDE#, "#LABEL#", "#CONTENT#","#COLOR#");
            """;

  private static final String FILE_TEMPLATE = """
      <!DOCTYPE html>
      <html lang="en">
      <head>
      <meta charset="utf-8" />
      <title>#TITLE#</title>
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />

      <link
        rel="stylesheet"
        href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
      />

      <style>
        html, body { height: 100%; margin: 0; }
        #map { height: 100%; }

        .label-text {
          font-family: system-ui, sans-serif;
          font-weight: 600;
          color: #1a237e;
          white-space: nowrap;
          text-shadow: 0 0 2px white, 0 0 4px white;
          transform-origin: left center;
          pointer-events: none;
        }

        .leaflet-custom-legend {
        background: white;
        padding: 8px;
        border-radius: 4px;
        box-shadow: 0 0 8px rgba(0,0,0,0.3);
        font-family: sans-serif;
        font-size: 13px;
        resize: both;
        overflow: auto;
        max-width: 260px;
        max-height: 300px;
        position: relative;
        }

        .leaflet-custom-legend-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: bold;
        margin-bottom: 6px;
        }

        .leaflet-custom-legend-close {
        cursor: pointer;
        border: none;
        background: transparent;
        font-size: 16px;
        line-height: 1;
        padding: 0 4px;
        }

        .leaflet-custom-legend-close:hover {
        background: #eee;
        }

        .legend-toggle-btn {
        background: white;
        padding: 4px 6px;
        border-radius: 4px;
        box-shadow: 0 0 6px rgba(0,0,0,0.3);
        cursor: pointer;
        font-size: 12px;
        }

        .legend .box {
        display: inline-block;
        width: 14px;
        height: 14px;
        margin-right: 6px;
        vertical-align: middle;
        }

      </style>
      </head>
      <body>
      <div id="map"></div>

      <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

      <script>
      // ------------------------------------------------------------
      // Map
      // ------------------------------------------------------------
      const map = L.map('map').setView([40, -91], 4);

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 19
      }).addTo(map);

      // ------------------------------
      // Legend Control
      // ------------------------------
      let legendContainer;  // <-- store reference so toggle can reopen it

      const LegendControl = L.Control.extend({
        options: { position: 'bottomleft' },

        onAdd: function (map) {
        legendContainer = L.DomUtil.create('div', 'leaflet-custom-legend');

        L.DomEvent.disableClickPropagation(legendContainer);
        L.DomEvent.disableScrollPropagation(legendContainer);

        // Header
        const header = L.DomUtil.create('div', 'leaflet-custom-legend-header', legendContainer);
        header.innerHTML = `<span>#TITLE#</span>`;

        const closeBtn = L.DomUtil.create('button', 'leaflet-custom-legend-close', header);
        closeBtn.innerHTML = '&times;';

        closeBtn.addEventListener('click', () => {
          legendContainer.style.display = 'none';
          toggleButton.style.display = 'block';
        });

        // Body
        const body = L.DomUtil.create('div', '', legendContainer);
        body.innerHTML = `#LEGEND_HTML#`;

        return legendContainer;
        }
      });

      map.addControl(new LegendControl());

      // ------------------------------
      // Toggle Button Control
      // ------------------------------
      let toggleButton;

      const ToggleLegendControl = L.Control.extend({
        options: { position: 'bottomleft' },

        onAdd: function (map) {
        toggleButton = L.DomUtil.create('div', 'legend-toggle-btn');
        toggleButton.innerHTML = '#TITLE#';

        L.DomEvent.disableClickPropagation(toggleButton);

        toggleButton.addEventListener('click', () => {
          legendContainer.style.display = 'block';
          toggleButton.style.display = 'none';
        });

        return toggleButton;
        }
      });

      map.addControl(new ToggleLegendControl());

      // Hide toggle button initially
      toggleButton.style.display = 'none';

      // ------------------------------------------------------------
      // Google Maps–style prominence curve
      // ------------------------------------------------------------
      function computeScale(z) {
        const minZoom = 8;
        const maxZoom = 18;
        const minScale = 0.4;
        const maxScale = 2.8;

        const t = Math.min(1, Math.max(0, (z - minZoom) / (maxZoom - minZoom)));
        const eased = 1 - Math.pow(1 - t, 3);

        return minScale + eased * (maxScale - minScale);
      }

      // ------------------------------------------------------------
      // Marker icon color map (no shadow)
      // ------------------------------------------------------------
      const iconColors = {
        blue:  "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png",
        red:   "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png",
        green: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png",
        orange:"https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-orange.png",
        yellow:"https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-yellow.png",
        violet:"https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-violet.png",
        grey:  "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-grey.png",
        black: "https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-black.png"
      };

      // ------------------------------------------------------------
      // Marker + scalable label + optional popup + icon color
      // NEW SIGNATURE: addMarkerWithLabel(lat, lng, labelText, popupText, color)
      // ------------------------------------------------------------
      function addMarkerWithLabel(lat, lng, labelText, popupText = null, color = "blue") {
        const iconUrl = iconColors[color] || iconColors.blue;

        const markerIcon = L.icon({
          iconUrl,
          iconSize: [13, 21],
          iconAnchor: [-5, 10],
          popupAnchor: [1, -10]
        });

        const marker = L.marker([lat, lng], { icon: markerIcon }).addTo(map);

        if (popupText) {
          marker.bindPopup(popupText);
        }

        L.marker([lat, lng], {
          interactive: false,
          icon: L.divIcon({
            className: "",
            iconAnchor: [-5, -10],
            html: `<div class="label-text">${labelText}</div>`
          })
        }).addTo(map);
      }

      // ------------------------------------------------------------
      // Base markers
      // ------------------------------------------------------------
      #POINTS#

      // ------------------------------------------------------------
      // Update label scale on zoom
      // ------------------------------------------------------------
      map.on("zoomend", () => {
        const scale = computeScale(map.getZoom());

        map.eachLayer(layer => {
          if (layer instanceof L.Marker && layer.options.icon instanceof L.DivIcon) {
            const el = layer.getElement();
            if (!el) return;

            const text = el.querySelector(".label-text");
            if (text) {
              text.style.transform = `scale(${scale})`;
            }
          }
        });
      });
      </script>
      </body>
      </html>

                       """;

  @Override
  public Set<String> getValidIconColors() {
    return VALID_ICON_COLORS;
  }

  @Override
  public String getInvalidIconColor() {
    return "black";
  }

  @Override
  public String makeLegendForFeedbackCount(int participantCount, Counter counter) {
    var legendHTML = "For " + participantCount + " participants";

    var it = counter.getAscendingKeyIterator();
    var sb = new StringBuilder();
    while (it.hasNext()) {
      var entry = it.next();
      sb.append("value" + ": " + entry.getKey() + ", " + "count" + ": " + entry.getValue() + "<br>");
    }
    legendHTML += "<br><br>Feedback Count:<br>" + sb.toString();
    return legendHTML;
  }

  @Override
  public String makeLegendForRecipients(List<MapEntry> colorizedMapEntries) {
    var countMap = new HashMap<String, Integer>();
    for (var entry : colorizedMapEntries) {
      var to = entry.to();
      to = (to == null || to.length() == 0) ? "unknown" : to;
      var count = countMap.getOrDefault(to, Integer.valueOf(0));
      ++count;
      countMap.put(to, count);
    }

    var sb = new StringBuilder();
    sb.append("For " + colorizedMapEntries.size() + " participants<br>" + "\n");
    var keys = new ArrayList<String>(colorMap.keySet());
    Collections.sort(keys);
    for (var to : keys) {
      var colorName = colorMap.get(to);
      var rgb = rgbMap.get(colorName);
      rgb = (rgb != null) ? rgb : "ffffff";
      var count = countMap.get(to);
      sb
          .append("<div><span class=\"box\" style=\"background:#" + rgb + "\">&nbsp;&nbsp;&nbsp;</span> " + to + ": "
              + count + " participants</div>" + "\n");
    }
    return sb.toString();
  }

  @Override
  public List<MapEntry> makeColorizedEntriesByRecipients(List<MapEntry> mapEntries) {
    List<MapEntry> newMapEntries = new ArrayList<MapEntry>();
    var tempColorMap = new HashMap<String, String>();
    var validColorIndex = 0;
    var expectedDestinationsString = cm.getAsString(Key.EXPECTED_DESTINATIONS);
    var expectedDestinationsSet = Stream.of(expectedDestinationsString.split(",")).collect(Collectors.toSet());
    var validColorList = new ArrayList<String>(getValidIconColors());

    var colorName = getInvalidIconColor();
    for (var mapEntry : mapEntries) {
      var to = mapEntry.to();
      to = (to != null) ? to : "";
      if (exerciseOrganization.equals("ETO")) {
        colorName = etoColorMap.getOrDefault(to, getInvalidIconColor());
      } else {
        var isValid = expectedDestinationsSet.contains(to);
        if (isValid) {
          colorName = tempColorMap.get(to);
          if (colorName == null) {
            colorName = validColorList.get(validColorIndex);
            ++validColorIndex;
            if (validColorIndex == validColorList.size()) {
              validColorIndex = 0;
            }
            tempColorMap.put(to, colorName);
          }
        } else {// end isValid
          colorName = getInvalidIconColor();
          tempColorMap.put(to, colorName);
        } // end if !isValid expectedDestination
      } // end not eto
      var newMessage = "From: " + mapEntry.label() + "\n" + "To: " + to + "\n" + mapEntry.message();
      var newMapEntry = new MapEntry(mapEntry.label(), to, mapEntry.location(), newMessage, colorName);
      newMapEntries.add(newMapEntry);
    } // end loop over mapEntries

    colorMap = tempColorMap.size() > 0 ? tempColorMap : etoColorMap;
    return newMapEntries;
  }
}
