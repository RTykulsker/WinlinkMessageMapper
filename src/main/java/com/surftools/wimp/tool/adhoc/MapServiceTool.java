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

package com.surftools.wimp.tool.adhoc;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;

/**
 * read a spreadsheet (or maybe a few), make map
 */
public class MapServiceTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(MapServiceTool.class);

  @Option(name = "--inputFileName", usage = "path to input file", required = true)
  private String inputFileName = "standard-summary.csv";

  public static void main(String[] args) {
    var tool = new MapServiceTool();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() {
    logger.info("begin run");
    try {
      var summaries = makeSummaries();
      var dateString = LocalDate.now().toString();
      var mapService = new MapService(null, null);

      doFeedbackMap(mapService, summaries, dateString);
      doClearinghouseMap(mapService, summaries, dateString);

    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }
    logger.info("end run");
  }

  private void doFeedbackMap(MapService mapService, List<Summary> summaries, String dateString) {
    var feedbackCounter = new Counter();
    final int nLayers = 6;

    var truncatedCountMap = new HashMap<Integer, Integer>(); // 9 -> 9 or more
    for (var summary : summaries) {
      var key = Math.min(nLayers - 1, Integer.parseInt(summary.feedbackCount));
      var value = truncatedCountMap.getOrDefault(key, Integer.valueOf(0));
      ++value;
      truncatedCountMap.put(key, value);
    }

    var gradientMap = mapService.makeGradientMap(120, 0, nLayers);
    var layers = new ArrayList<MapLayer>();
    var countLayerNameMap = new HashMap<Integer, String>();
    for (var i = 0; i < nLayers; ++i) {
      var value = String.valueOf(i);
      var count = truncatedCountMap.get(i);
      if (i == nLayers - 1) {
        value = i + " or more";
      }
      var layerName = "value: " + value + ", count: " + count;
      countLayerNameMap.put(i, layerName);

      var color = gradientMap.get(i);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    for (var summary : summaries) {
      mapEntries.add(makeMapEntry(summary, gradientMap));
      feedbackCounter.increment(Integer.parseInt(summary.feedbackCount));
    }

    var legendTitle = "2025-12-18 Feedback Counts (" + summaries.size() + " total)";
    var outputPath = Path.of(".");
    var context = new MapContext(outputPath, //
        "NEW-" + dateString + "-map-feedbackCount", // file name
        "MapServiceTool - Test", // title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  private void doClearinghouseMap(MapService mapService, List<Summary> summaries, String dateString) {
    final var CLEARINGHOUSE_NAMES = "ETO-01,ETO-02,ETO-03,ETO-04,ETO-05,ETO-06,ETO-07,ETO-08,ETO-09,ETO-10,ETO-CAN,ETO-DX";
    var clearinghouseNames = new ArrayList<String>(Arrays.asList(CLEARINGHOUSE_NAMES.split(",")));
    clearinghouseNames.add("unknown");
    var clearinghouseCountMap = new LinkedHashMap<String, Integer>();
    for (var s : summaries) {
      var to = s.to;
      if (!clearinghouseNames.contains(to)) {
        to = "unknown";
      }
      var count = clearinghouseCountMap.getOrDefault(to, Integer.valueOf(0));
      ++count;
      clearinghouseCountMap.put(to, count);
    }

    var layers = new ArrayList<MapLayer>();
    for (var name : clearinghouseNames) {
      var count = clearinghouseCountMap.get(name);
      var layerName = name + ": " + count + " participants";

      var color = MapService.etoColorMap.get(name);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    for (var s : summaries) {
      var to = s.to;
      var clearinghouseName = clearinghouseNames.contains(to) ? to : "unknown";
      var location = new LatLongPair(s.latitude, s.longitude);
      var color = MapService.etoColorMap.get(clearinghouseName);
      var prefix = "<b>From: " + s.from + "<br>To: " + to + "</b><hr>";
      var messageIds = "";
      var content = (messageIds == null) ? "" : "MessageIds: " + messageIds + "\n";
      content = content + "Feedback Count: " + s.feedbackCount + "\n" + "Feedback: " + s.Feedback;
      content = prefix + content;
      var mapEntry = new MapEntry(s.from, s.to, location, content, color);
      mapEntries.add(mapEntry);
    }

    var legendTitle = dateString + " By Clearinghouse (" + summaries.size() + " total)";
    var outputPath = Path.of(".");
    var context = new MapContext(outputPath, //
        "NEW-" + dateString + "-map-byClearinghouse", // file name
        dateString + " By Clearinghouse", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);

  }

  private MapEntry makeMapEntry(Summary s, Map<Integer, String> gradientMap) {
    var count = Integer.valueOf(s.feedbackCount);

    final var lastColorMapIndex = gradientMap.size() - 1;
    final var lastColor = gradientMap.get(lastColorMapIndex);

    var location = new LatLongPair(s.latitude, s.longitude);
    var color = gradientMap.getOrDefault(count, lastColor);
    var prefix = "<b>" + s.from + "</b><hr>";
    var content = prefix + "Feedback Count: " + s.feedbackCount + "\n" + "Feedback: " + s.Feedback();
    return new MapEntry(s.from, s.to, location, content, color);
  }

  List<Summary> makeSummaries() {
    var lines = ReadProcessor.readCsvFileIntoFieldsArray(Path.of(inputFileName), ',', false, 1);
    var summaries = new ArrayList<Summary>();
    for (var line : lines) {
      summaries.add(Summary.fromLine(line));
    }
    logger.info("read " + summaries.size() + " summaries from " + inputFileName);
    return summaries;
  }

  record Summary(String from, String to, String latitude, String longitude, String date, String time,
      String feedbackCount, String Feedback) {

    static Summary fromLine(String[] fields) {
      return new Summary(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6], fields[7]);
    }
  }
}
