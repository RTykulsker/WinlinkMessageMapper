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
import java.util.HashMap;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class LeafletMapEngine extends MapService {
  private static final Logger logger = LoggerFactory.getLogger(LeafletMapEngine.class);

  protected IConfigurationManager cm;

  public LeafletMapEngine(IConfigurationManager cm, IMessageManager mm) {
    this.cm = cm;
  }

  @Override
  public Set<String> getValidIconColors() {
    return VALID_ICON_COLORS;
  }

  @Override
  public String getInvalidIconColor() {
    return "black";
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
    // no double-quote characters
    escaped = escaped.replaceAll("\"", "&quot;");
    return escaped;
  }

  @Override
  public void makeMap(MapContext mapContext) {

    var colorLayerMap = new HashMap<String, String>();

    var sb = new StringBuilder();
    for (var layer : mapContext.layers()) {
      var text = LAYER_TEMPLATE;
      text = text.replace("#LAYER_NAME#", layer.name());
      text = text.replace("#LAYER_COLOR#", layer.color());
      sb.append(text);
      colorLayerMap.put(layer.color(), layer.name());
    }
    var layers = sb.toString();
    layers = layers.substring(0, layers.length() - 2);

    sb = new StringBuilder();
    for (var mapEntry : mapContext.mapEntries()) {
      var marker = MARKER_TEMPLATE;
      marker = marker.replace("#LATITUDE#", mapEntry.location().getLatitude());
      marker = marker.replace("#LONGITUDE#", mapEntry.location().getLongitude());
      marker = marker.replace("#LABEL#", mapEntry.label());
      var layerName = colorLayerMap.get(mapEntry.iconColor());
      if (layerName == null) {
        layerName = "";
        logger.error("### no layer name for mapEntry: " + mapEntry);
      }
      marker = marker.replace("#LAYER_NAME#", layerName);
      marker = marker.replace("#COLOR#", mapEntry.iconColor());

      var message = mapEntry.message().replaceAll("\n", "<br/>");
      message = escapeForJavaScript(message);
      marker = marker.replace("#POPUP#", message);
      sb.append(marker);
    }
    var markers = sb.toString();

    /**
     * the fast template is supposed to be faster for large (1500 markers than the
     * slow template. It is faster on one machine/browser, but not another
     */
    var mapTemplateMethod = cm.getAsString(Key.MAP_TEMPLATE_METHOD, "fast");
    var doFast = mapTemplateMethod.toLowerCase().equals("fast");
    logger.info("Using " + (doFast ? "fast" : "slow") + " file template");
    var template = doFast ? FAST_FILE_TEMPLATE : SLOW_FILE_TEMPLATE;
    var fileContent = new String(template);
    fileContent = fileContent.replace("#MAP_TITLE#", mapContext.mapTitle());
    fileContent = fileContent.replace("#LEGEND_TITLE#", mapContext.legendTitle());

    // TODO make this work!
    var makeThisWork = false;
    if (makeThisWork) {
      var geo = mapContext.mapGeometry();
      if (geo == null) {
        geo = MapService.DEFAULT_MAP_GEOMETRY;
      }
      fileContent.replace("#CENTER_LAT#", geo.centerLatitude());
      fileContent.replace("#CENTER_LNG#", geo.centerLatitude());
      fileContent.replaceAll("#BASE_ZOOM#", geo.baseZoom());
      fileContent.replace("#MAX_ZOOM#", geo.maxZoom());
    }

    fileContent = fileContent.replace("#LAYERS#", layers);
    fileContent = fileContent.replace("#MARKERS#", markers);

    var filePath = Path.of(mapContext.path().toString(), "leaflet-" + mapContext.fileName() + ".html");
    try {
      Files.writeString(filePath, fileContent.toString());
      logger.info("wrote " + mapContext.mapEntries().size() + " entries to: " + filePath.toString());
    } catch (Exception e) {
      logger.error("Exception writing leaflet file: " + filePath.toString() + ", " + e.getMessage());
    }
  }

  private static final String LAYER_TEMPLATE = """
        { name: "#LAYER_NAME#", color: "#LAYER_COLOR#"},
      """;

  private static final String MARKER_TEMPLATE = """
      markers.push({lat: #LATITUDE#,lng: #LONGITUDE#,name: "#LABEL#", layerName: "#LAYER_NAME#", color: "#COLOR#", popup: "#POPUP#"});
        """;

  private static String SLOW_FILE_TEMPLATE = """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8" />
        <title>#MAP_TITLE#</title>

        <link
          rel="stylesheet"
          href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
        />

        <style>
          #map {
            height: 100vh;
            width: 100vw;
          }

          /* CSS marker body (circle + pointer) */
          .hex-pin {
            position: relative;
            width: 13px;
            height: 13px;
            border-radius: 50%;
            border: 2px solid #333;
            transform: translate(-10px, -10px);
          }

          /* white dot in center */
          .hex-pin::before {
            content: "";
            position: absolute;
            width: 6px;
            height: 6px;
            background: white;
            border-radius: 50%;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
          }

          /* pointer triangle */
          .hex-pin::after {
            content: "";
            position: absolute;
            left: 50%;
            bottom: -10px;
            width: 0;
            height: 0;
            border-left: 6px solid transparent;
            border-right: 6px solid transparent;
            border-top: 10px solid #333;
            transform: translateX(-50%);
          }

          /* Marker label under marker */
          .marker-label {
            position: absolute;
            white-space: nowrap;
            padding: 2px 4px;
            border-radius: 3px;
            font-size: 12px;
            pointer-events: none;
            transform-origin: top center;
            transform: translate(-50%, 4px);
            z-index: 9999;
          }

          .legend-control {
            position: absolute;
            bottom: 5px;
            left: 10px;
            background: white;
            padding: 10px 12px;
            border-radius: 6px;
            box-shadow: 0 0 6px rgba(0,0,0,0.3);
            font-family: sans-serif;
            z-index: 9999;

            width: auto;              /* auto-size to content */
            max-width: none;          /* remove any implicit max */
            white-space: nowrap;      /* prevent wrapping */
          }

          .legend-control.collapsed #legendBody {
            display: none;
          }

          .legend-title {
            font-weight: bold;
            margin-bottom: 8px;
            border-bottom: 1px solid #ccc;
            padding-bottom: 4px;
            cursor: pointer;
            white-space: nowrap;
          }

          .legend-row {
            display: flex;
            align-items: center;
            margin-bottom: 6px;
            cursor: pointer;
            user-select: none;
            white-space: nowrap;      /* force single line */
          }

          .legend-row:last-child {
            margin-bottom: 0;
          }

          .legend-icon {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            border: 2px solid #333;
            margin-right: 8px;
            position: relative;
            flex-shrink: 0;           /* icon never shrinks */
          }

          .legend-icon::after {
            content: "";
            position: absolute;
            left: 50%;
            bottom: -8px;
            width: 0;
            height: 0;
            border-left: 5px solid transparent;
            border-right: 5px solid transparent;
            border-top: 8px solid #333;
            transform: translateX(-50%);
          }

          .legend-row span {
            white-space: nowrap;      /* force single line */
            overflow: visible;        /* allow full expansion */
          }

          .legend-row.disabled {
            opacity: 0.35;
          }

          #legendClose {
            float: right;
            cursor: pointer;
            font-weight: bold;
            margin-left: 10px;
          }
        </style>
      </head>

      <body>
        <div id="map"></div>

        <!-- Legend-control hybrid -->
        <div class="legend-control" id="legendControl">
          <div class="legend-title" id="legendTitle">
            #LEGEND_TITLE#
          <!-- span id="legendClose">▁</span> -->
          <span id="legendClose">_</span>
          </div>
          <div id="legendBody"></div>
        </div>

        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

        <script>
          // ------------------------------------------------------------
          // Initialize map
          // ------------------------------------------------------------

          const map = L.map("map").setView([40, -100], 4);
          // ------------------------------------------------------------
          // BASEMAPS
          // ------------------------------------------------------------
          const osm = L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
              maxZoom: 19,
              attribution: 'Map Tiles from <a href="http://openstreetmap.org/copyright">OpenStreetMap</a>'
            });

          const usgs = L.tileLayer('https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/tile/{z}/{y}/{x}',{
              maxZoom: 19,
              attribution: 'Tiles courtesy of the <a href="https://usgs.gov/">U.S. Geological Survey</a>'
            });

          const openTopo = L.tileLayer("https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png",{
              maxZoom: 17,
              attribution: "© OpenTopoMap (CC-BY-SA)"
          });

          const cartoLight = L.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",{
            maxZoom: 20,
            attribution: "© CARTO © OpenStreetMap contributors"
          });


          const cartoDark = L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",{
            maxZoom: 20,
            attribution: "© CARTO © OpenStreetMap contributors"
          });

          const stamenTerrain = L.tileLayer("https://tiles.stadiamaps.com/tiles/stamen_terrain/{z}/{x}/{y}.png",{
            maxZoom: 18,
            attribution: "Map tiles © Stamen; Data © OpenStreetMap"
          });

          const usgsImagery = L.tileLayer("https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryOnly/MapServer/tile/{z}/{y}/{x}",{
            maxZoom: 20,
            attribution: "USGS NAIP Imagery"
          });

          osm.addTo(map);

          // ------------------------------------------------------------
          // BASEMAP CONTROL
          // ------------------------------------------------------------
          const baseMaps = {
            "OpenStreetMap": osm,
            "USGS Topo": usgs,
            "OpenTopoMap": openTopo,
            "Carto Light": cartoLight,
            "Carto Dark": cartoDark,
            "Stamen Terrain": stamenTerrain,
            "USGS Imagery": usgsImagery
          };

          L.control.layers(baseMaps, null, { collapsed: false }).addTo(map);

          // ------------------------------------------------------------
          // CSS-based hex marker
          // ------------------------------------------------------------
          function hexMarker(hexColor) {
            return L.divIcon({
              className: "",
              iconSize: [20, 30],
              iconAnchor: [10, 30],
              html: `<div class="hex-pin" style="background:${hexColor};"></div>`
            });
          }

          // ------------------------------------------------------------
          // Label under marker
          // ------------------------------------------------------------
          function labelIcon(text) {
            return L.divIcon({
              className: "",
              iconSize: null,
              iconAnchor: [0, 30],
              html: `<div class="marker-label">${text}</div>`
            });
          }

          // ------------------------------------------------------------
          // 1. CREATE LAYERS FIRST
          // ------------------------------------------------------------
          const layerDefs = [
              #LAYERS#
          ];

          const layers = {};
          layerDefs.forEach(def => {
            layers[def.name] = L.layerGroup().addTo(map);
          });

          // ------------------------------------------------------------
          // 2. CREATE LEGEND SECOND
          // ------------------------------------------------------------
          const legend = document.getElementById("legendControl");
          const legendBody = document.getElementById("legendBody");
          const legendTitle = document.getElementById("legendTitle");
          const legendClose = document.getElementById("legendClose");

          layerDefs.forEach(def => {
            const row = document.createElement("div");
            row.className = "legend-row";
            row.dataset.layer = def.name;

            const icon = document.createElement("div");
            icon.className = "legend-icon";
            icon.style.background = def.color;

            const label = document.createElement("span");
            label.textContent = def.name;

            row.appendChild(icon);
            row.appendChild(label);
            legendBody.appendChild(row);

            // Toggle behavior
            row.addEventListener("click", () => {
              const layer = layers[def.name];

              if (map.hasLayer(layer)) {
                map.removeLayer(layer);
                row.classList.add("disabled");
              } else {
                map.addLayer(layer);
                row.classList.remove("disabled");
              }
            });
          });

          // Collapse button
          legendClose.addEventListener("click", (e) => {
            e.stopPropagation();
            legend.classList.add("collapsed");
          });

          // Clicking title re-opens
          legendTitle.addEventListener("click", () => {
            legend.classList.remove("collapsed");
          });

          // ------------------------------------------------------------
          // 3. CREATE MARKERS SEPARATELY
          // ------------------------------------------------------------
          const markers = [];

          layerDefs.forEach(def => {
          #MARKERS#
          });



          // ------------------------------------------------------------
          // 4. ADD MARKERS TO THEIR LAYERS
          // ------------------------------------------------------------
          markers.forEach(m => {
            const group = layers[m.layerName];

            L.marker([m.lat, m.lng], { icon: hexMarker(m.color) })
              .addTo(group)
              .bindPopup(`${m.popup == null ? m.name : m.popup}`);

            L.marker([m.lat, m.lng], {
              icon: labelIcon(m.name),
              interactive: false
            }).addTo(group);
          });

          // ------------------------------------------------------------
          // Zoom-scaling for markers + labels
          // ------------------------------------------------------------
          const baseZoom = 4;

          function updateScale() {
            const z = map.getZoom();
            const dz = z - baseZoom;

              // Hard cutoff: unreadable at zoom 4 or below
              if (z <= baseZoom) {
                scale = 0.05;   // microscopic
              } else {
                // Smooth sigmoid curve above zoom 4
                const minScale = 0.25;
                const maxScale = 2.8;
                const growth = 0.45;

                scale = minScale + (maxScale - minScale) * (1 / (1 + Math.exp(-growth * dz)));
              }

        //    document.querySelectorAll(".hex-pin").forEach(el => {
        //      el.style.transform = `translate(-10px, -10px) scale(${scale})`;
        //    });

            document.querySelectorAll(".marker-label").forEach(el => {
              el.style.transform = `translate(-50%, 4px) scale(${scale})`;
            });
          }

          map.on("zoom zoomend", updateScale);
          updateScale();
        </script>
      </body>
      </html>
            """;

  private static String FAST_FILE_TEMPLATE = """
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="utf-8" />
        <title#MAP_TITLE#</title>

        <link
          rel="stylesheet"
          href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"
        />

        <style>
          #map {
            height: 100vh;
            width: 100vw;
          }

          /* Legend-control hybrid */
          .legend-control {
            position: absolute;
            bottom: 5px;
            left: 5px;
            background: white;
            padding: 10px 12px;
            border-radius: 6px;
            box-shadow: 0 0 6px rgba(0,0,0,0.3);
            font-family: sans-serif;
            z-index: 9999;

            width: auto;
            max-width: none;
            white-space: nowrap;
          }

          .legend-control.collapsed #legendBody {
            display: none;
          }

          .legend-title {
            font-weight: bold;
            margin-bottom: 8px;
            border-bottom: 1px solid #ccc;
            padding-bottom: 4px;
            cursor: pointer;
            white-space: nowrap;
          }

          .legend-row {
            display: flex;
            align-items: center;
            margin-bottom: 6px;
            cursor: pointer;
            user-select: none;
            white-space: nowrap;
          }

          .legend-row:last-child {
            margin-bottom: 0;
          }

          .legend-icon {
            width: 20px;
            height: 20px;
            border-radius: 50%;
            border: 2px solid #333;
            margin-right: 8px;
            position: relative;
            flex-shrink: 0;
            background: gray;
          }

          .legend-icon::before {
            content: "";
            position: absolute;
            width: 6px;
            height: 6px;
            background: white;
            border-radius: 50%;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
          }

          .legend-icon::after {
            content: "";
            position: absolute;
            left: 50%;
            bottom: -8px;
            width: 0;
            height: 0;
            border-left: 5px solid transparent;
            border-right: 5px solid transparent;
            border-top: 8px solid #333;
            transform: translateX(-50%);
          }

          .legend-row.disabled {
            opacity: 0.35;
          }

          #legendClose {
            float: right;
            cursor: pointer;
            font-weight: bold;
            margin-left: 10px;
          }

          /* Tooltip labels — transparent background */
          :root {
            --label-font-size: 12px;
          }

          .marker-label-canvas {
            background: transparent !important;
            border: none !important;
            padding: 0;
            font-size: var(--label-font-size);
            white-space: nowrap;
            box-shadow: none !important;
          }

          /* Remove upward white tooltip arrow */
          .leaflet-tooltip-top:before,
          .leaflet-tooltip-bottom:before,
          .leaflet-tooltip-left:before,
          .leaflet-tooltip-right:before {
            display: none !important;
          }

        	.search-control {
        	  background: white;
        	  padding: 4px 6px;
        	  border-radius: 4px;
        	  box-shadow: 0 0 4px rgba(0,0,0,0.3);
        	  display: flex;
        	  align-items: center;
        	  gap: 4px;
        	}

        	.search-control input {
        	  width: 140px;
        	  border: none;
        	  outline: none;
        	  padding: 4px;
        	  background: #dddddd; /* requested */
        	  border-radius: 3px;
        	}

        	.search-icon {
        	  font-size: 16px;
        	  user-select: none;
        	}

        	#busy-overlay {
        	  position: fixed;
        	  inset: 0;
        	  background: transparent;
        	  cursor: wait;
        	  z-index: 999999; /* above Leaflet controls */
        	  display: none;
        	}

        </style>
      </head>

      <body>
        <div id="busy-overlay"></div>
        <div id="map"></div>

        <!-- Legend-control hybrid -->
        <div class="legend-control" id="legendControl">
          <div class="legend-title" id="legendTitle">
            #LEGEND_TITLE#
            <span id="legendClose">_</span>
          </div>
          <div id="legendBody"></div>
        </div>

        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

        <script>
          // ------------------------------------------------------------
          // Initialize map (Canvas renderer enabled)
          // ------------------------------------------------------------
          const map = L.map("map", { preferCanvas: true }).setView([50, -100], 4);

          // ------------------------------------------------------------
          // BASEMAPS (ONLY OSM + USGS)
          // ------------------------------------------------------------
          const osm = L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
            maxZoom: 19,
            attribution: "© OpenStreetMap contributors"
          });

          const usgs = L.tileLayer(
            "https://basemap.nationalmap.gov/arcgis/rest/services/USGSTopo/MapServer/tile/{z}/{y}/{x}",
            { maxZoom: 20, attribution: "USGS The National Map" }
          );

          const openTopo = L.tileLayer("https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png",{
              maxZoom: 17,
              attribution: "© OpenTopoMap (CC-BY-SA)"
          });

          const cartoLight = L.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",{
            maxZoom: 20,
            attribution: "© CARTO © OpenStreetMap contributors"
          });


          const cartoDark = L.tileLayer("https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",{
            maxZoom: 20,
            attribution: "© CARTO © OpenStreetMap contributors"
          });

          const usgsImagery = L.tileLayer("https://basemap.nationalmap.gov/arcgis/rest/services/USGSImageryOnly/MapServer/tile/{z}/{y}/{x}",{
            maxZoom: 20,
            attribution: "USGS NAIP Imagery"
          });

                    osm.addTo(map);

          L.control.layers(
            {
              "OpenStreetMap": osm,
              "USGS Topo": usgs,
              "OpenTopoMap": openTopo,
              "Carto Light": cartoLight,
              "Carto Dark": cartoDark,
              "USGS Imagery": usgsImagery
            },
            null,
            { collapsed: true }
          ).addTo(map);

          // ------------------------------------------------------------
          // CUSTOM CANVAS MARKER (circle + white dot + downward black triangle)
          // ------------------------------------------------------------
          const CanvasMarker = L.CircleMarker.extend({
            _updatePath: function () {
              const ctx = this._renderer._ctx;
              const p = this._point;
              const r = this._radius;

              ctx.save();

              // Outer circle
              ctx.beginPath();
              ctx.arc(p.x, p.y, r, 0, Math.PI * 2);
              ctx.fillStyle = this.options.fillColor;
              ctx.fill();
              ctx.lineWidth = this.options.weight;
              ctx.strokeStyle = this.options.color;
              ctx.stroke();

              // White dot
              ctx.beginPath();
              ctx.arc(p.x, p.y, r * 0.35, 0, Math.PI * 2);
              ctx.fillStyle = "#ffffff";
              ctx.fill();

              // Downward triangle (constant size)
              const triHeight = r * 1.2;
              const triWidth = r * 1.2;

              ctx.beginPath();
              ctx.moveTo(p.x - triWidth / 2, p.y + r);          // left
              ctx.lineTo(p.x + triWidth / 2, p.y + r);          // right
              ctx.lineTo(p.x, p.y + r + triHeight);             // bottom point
              ctx.closePath();

              ctx.fillStyle = this.options.color;
              ctx.fill();

              ctx.restore();
            }
          });

          // ------------------------------------------------------------
          // 1. CREATE LAYERS FIRST
          // ------------------------------------------------------------
          const layerDefs = [
            #LAYERS#
          ];

          const layers = {};
          layerDefs.forEach(def => {
            layers[def.name] = L.layerGroup().addTo(map);
          });

          // ------------------------------------------------------------
          // 2. CREATE LEGEND SECOND
          // ------------------------------------------------------------
          const legend = document.getElementById("legendControl");
          const legendBody = document.getElementById("legendBody");
          const legendTitle = document.getElementById("legendTitle");
          const legendClose = document.getElementById("legendClose");

          layerDefs.forEach(def => {
            const row = document.createElement("div");
            row.className = "legend-row";
            row.dataset.layer = def.name;

            const icon = document.createElement("div");
            icon.className = "legend-icon";
            icon.style.background = def.color;

            const label = document.createElement("span");
            label.textContent = def.name;

            row.appendChild(icon);
            row.appendChild(label);
            legendBody.appendChild(row);

            row.addEventListener("click", () => {
              const layer = layers[def.name];
              if (map.hasLayer(layer)) {
                map.removeLayer(layer);
                row.classList.add("disabled");
              } else {
                map.addLayer(layer);
                row.classList.remove("disabled");
              }
            });
          });

          legendClose.addEventListener("click", (e) => {
            e.stopPropagation();
            legend.classList.add("collapsed");
          });

          legendTitle.addEventListener("click", () => {
            legend.classList.remove("collapsed");
          });

          // ------------------------------------------------------------
          // 3. CREATE MARKERS SEPARATELY
          // ------------------------------------------------------------
          const markers = [];
          const markerByName = {};

          layerDefs.forEach(def => {
          #MARKERS#
          });

          // ------------------------------------------------------------
          // 4. ADD MARKERS TO THEIR LAYERS (Canvas) + POPUPS
          // ------------------------------------------------------------
          markers.forEach(m => {
            const group = layers[m.layerName];

            const marker = new CanvasMarker([m.lat, m.lng], {
              radius: 8,
              color: "#333",
              weight: 2,
              fillColor: m.color,
              fillOpacity: 1
            }).addTo(group);
            markerByName[m.name] = marker;

            // Popup on click
            marker.bindPopup(`${m.popup}`);
            marker.on("click", () => {
              marker.openPopup();
            });

            // Label
            L.tooltip({
              permanent: true,
              direction: "bottom",
              offset: [0, 10],
              className: "marker-label-canvas"
            })
              .setContent(m.name)
              .setLatLng([m.lat, m.lng])
              .addTo(group);
          });

          // ------------------------------------------------------------
          // HIDE LABELS AT LOW ZOOM (≤ 5)
          // ------------------------------------------------------------
          function updateLabelVisibility() {
            const show = map.getZoom() > 5;

            Object.values(layers).forEach(layer => {
              layer.eachLayer(l => {
                if (l instanceof L.Tooltip) {
                  if (show) map.addLayer(l);
                  else map.removeLayer(l);
                }
              });
            });
          }

          map.on("zoomend", updateLabelVisibility);
          updateLabelVisibility();

          // ------------------------------------------------------------
          // DYNAMIC LABEL FONT SCALING
          // ------------------------------------------------------------
          function updateLabelFont() {
            const z = map.getZoom();

            const minZoom = 4;
            const maxZoom = 12;

            const minSize = 10;
            const maxSize = 22;

            const t = Math.max(0, Math.min(1, (z - minZoom) / (maxZoom - minZoom)));

            const size = minSize + (maxSize - minSize) * (t * t * (3 - 2 * t));

            document.documentElement.style.setProperty("--label-font-size", size + "px");
          }

          // ---------------------------------------------------------------------
          // Search control (press Enter → zoom + open popup)
          // ---------------------------------------------------------------------
          const SearchControl = L.Control.extend({
            options: { position: "topleft" },

            onAdd: function () {
              const container = L.DomUtil.create("div", "search-control");

      		// magnifying glass icon
      		const icon = L.DomUtil.create("div", "search-icon", container);
      		icon.innerHTML = "🔍";

          const input = L.DomUtil.create("input", "", container);
          input.type = "text";
          input.placeholder = "Search by call sign…";

          L.DomEvent.disableClickPropagation(container);

          input.addEventListener("keydown", (e) => {
            if (e.key === "Enter") {
              const query = input.value.trim().toLowerCase();
              if (!query) return;

      			const overlay = document.getElementById("busy-overlay");
      			overlay.style.display = "block";   // show busy cursor immediately

      			// Force repaint before running the search
      			requestAnimationFrame(() => {

      				const match = markers.find(f =>
                f.name.toLowerCase() === query
              );

      				if (match) {
      				  const m = markerByName[match.name];
      				  map.setView(m.getLatLng(), 16);
      				  m.openPopup();
      				}

      				// restore cursor
      				overlay.style.display = "none";  // remove busy cursor

      			});
      		  }
      		});

              return container;
            }
          });
          map.addControl(new SearchControl());

          map.on("zoomend", updateLabelFont);
          updateLabelFont();
        </script>
      </body>
      </html>
                  """;

}
