/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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

package com.surftools.wimp.service.chart;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.utils.PageParser;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBaseChartService implements IChartService {
  private static final Logger logger = LoggerFactory.getLogger(AbstractBaseChartService.class);

  protected static IConfigurationManager cm;
  protected static Map<String, Counter> counterMap;
  protected static MessageType messageType;

  protected static Path fileOutputPath;
  protected static Map<Counter, Integer> minValuesMap;
  protected static String chartType;
  protected static String extraLayout;
  protected static boolean doSingleItemCharts;

  private static IChartService service;
  private static Set<Counter> excludedCounters = new HashSet<>();
  private static List<Counter> counterList = new ArrayList<>();

  /**
   * factory method to get configuration-specified IChartService for given messageType
   *
   * @param _cm
   * @param _counterMap
   * @param _messageType
   * @return
   */
  public static IChartService getChartService(IConfigurationManager _cm, Map<String, Counter> _counterMap,
      MessageType _messageType) {
    cm = _cm;
    counterMap = _counterMap;
    messageType = _messageType;

    if (counterMap.size() == 0) {
      logger.warn("### initializing with " + counterMap.size() + " potential charts");
    } else {
      logger.info("initializing with " + counterMap.size() + " potential charts");
    }

    for (var counterLabel : counterMap.keySet()) {
      var counter = counterMap.get(counterLabel);
      counterList.add(counter);
    }

    parseConfig();

    return service;
  }

  // https://jsonformatter.curiousconcept.com to format down to a single line
  @SuppressWarnings("unused")
  private String exampleConfig = """
      {
        "eto_resume":{
          "minValues":{
            "Agency":4
          },
          "fileOutputName":"/home/bobt/tmp/index.html",
          "excludeCounters":"versions",
          "serviceName":"Plotly"
        }
      }
            """;

  /**
   * set up static members with JSON-based configuration; or default values if none provided
   */
  private static void parseConfig() {
    makeDefaultConfig();

    var jsonString = cm.getAsString(Key.CHART_CONFIG, "").trim();
    if (jsonString.isEmpty()) {
      return; // the best config for a type is no config!
    }

    if (messageType == null) {
      return;
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      var jsonMap = mapper.readValue(jsonString, Map.class);

      @SuppressWarnings("rawtypes")
      var messageTypeMap = (Map) jsonMap.get(messageType.toString());
      if (messageTypeMap == null) {
        return;
      }

      var excludedCountersString = messageTypeMap.get("excludedCounters");
      var includedCountersString = messageTypeMap.get("includedCounters");

      if (includedCountersString != null && excludedCountersString != null) {
        throw new RuntimeException("can't specify both includedCounters(" + includedCountersString
            + ") and excludedCounters(" + excludedCountersString + ") for messageType: " + messageType);
      }

      parseMinValues(messageTypeMap.get("minValues"));
      parseFileOutputName(messageTypeMap.get("fileOutputName"));
      parseExcludedCounters(excludedCountersString);
      parseIncludedCounters(includedCountersString);
      parseServiceName(messageTypeMap.get("serviceName"));
      parseChartType(messageTypeMap.get("chartType"));
      parseExtraLayout(messageTypeMap.get("extraLayout"));
      parseDoSingleItemCharts(messageTypeMap.get("doSingleItemCharts"));
      parseShowCounterIndexes(messageTypeMap.get("showCounterIndexes"));
    } catch (Exception e) {
      logger.error("can't parse " + Key.CHART_CONFIG.name() + ", " + e.getMessage());
      throw new RuntimeException(e);
    }

  }

  private static void parseShowCounterIndexes(Object object) {
    if (object == null) {
      return;
    }

    var showCounterIndexes = Boolean.parseBoolean((String) object);
    if (showCounterIndexes) {
      var sb = new StringBuilder();
      sb.append("\n");
      var index = 0;
      for (var counter : counterList) {
        sb.append("index: " + index + ", label: " + counter.getName() + ", keyCount: " + counter.getKeyCount() + "\n");
        ++index;
      }

      logger.info("Counter indexes: " + sb);
    }

  }

  protected static void parseExtraLayout(Object object) {
    if (object == null) {
      return;
    }

    extraLayout = (String) object;
  }

  private static void parseDoSingleItemCharts(Object object) {
    if (object == null) {
      return;
    }
    doSingleItemCharts = (Boolean) object;
  }

  protected static void parseChartType(Object object) {
    if (object == null) {
      return;
    }

    chartType = (String) object;
  }

  private static void parseServiceName(Object object) {
    if (object == null) {
      return;
    }

    String serviceName = (String) object;
    for (var prefix : List.of("com.surftools.wimp.service.chart.", "")) {
      for (var suffix : List.of("Service", "ChartService", "")) {
        var className = prefix + serviceName + suffix;
        logger.debug("searching for className: " + className);
        try {
          var clazz = Class.forName(className);
          if (clazz != null) {
            service = (IChartService) clazz.getDeclaredConstructor().newInstance();
            logger.debug("found  className: " + className);
            return;
          }
        } catch (Exception e) {
          ;
        }
      } // end loop over suffixes
    } // end loop over prefixes
    throw new RuntimeException("Could not find a processor for: " + serviceName);
  }

  private static void parseExcludedCounters(Object object) {
    if (object == null) {
      return;
    }

    excludedCounters.clear();

    var counterNames = parseCounterNames((String) object);
    for (var counterName : counterNames) {
      var counter = counterMap.get(counterName);
      if (counter == null) {
        throw new RuntimeException("no Counter for " + counterName);
      }
      excludedCounters.add(counter);
    }
  }

  private static void parseIncludedCounters(Object object) {
    if (object == null) {
      return;
    }

    excludedCounters.addAll(counterMap.values());

    // var counterNames = ((String) object).split(",");
    var counterNames = parseCounterNames((String) object);
    for (var counterName : counterNames) {
      var counter = counterMap.get(counterName);
      if (counter == null) {
        throw new RuntimeException("no Counter for " + counterName);
      }
      excludedCounters.remove(counter);
    }
  }

  private static String[] parseCounterNames(String string) {
    if (string.startsWith("#")) {
      string = convertIndexesToSingleString(string.substring(1));
    }
    return string.split(",");
  }

  private static String convertIndexesToSingleString(String substring) {
    var pageParser = new PageParser();
    var intList = pageParser.parse(substring);
    var counterLabelList = new ArrayList<String>();
    for (var index : intList) {
      try {
        var counter = counterList.get(index);
        counterLabelList.add(counter.getName());
      } catch (Exception e) {
        throw new RuntimeException("Index : " + index + " out of bounds for counterList of size " + counterList.size());
      }
    }
    return String.join(",", counterLabelList);
  }

  private static void parseFileOutputName(Object object) {
    if (object == null) {
      return;
    }

    var fileOutputName = (String) object;
    fileOutputPath = Path.of(fileOutputName);
  }

  private static void parseMinValues(Object object) {
    if (object == null) {
      return;
    }

    @SuppressWarnings("unchecked")
    var map = (Map<String, Integer>) object;
    for (var key : map.keySet()) {
      var counter = counterMap.get(key);
      if (counter == null) {
        throw new RuntimeException("no Counter for " + key);
      }

      var value = map.get(key);
      minValuesMap.put(counter, value);
    }
  }

  /**
   * provide "sensible" values when no explicit configuration for a messageType is given
   */
  private static void makeDefaultConfig() {
    var fileName = messageType == null ? "summary" : messageType.name().toLowerCase();
    service = new PlotlyChartService();
    fileOutputPath = Path.of(cm.getAsString(Key.PATH), "output", fileName + "_" + "plottly_chart.html");

    excludedCounters = new HashSet<>();
    minValuesMap = new HashMap<>();
    chartType = "pie";
    extraLayout = "\nvar layout={};\n\n";
    doSingleItemCharts = false;
  }

  /**
   * must not explicitly excluded
   *
   * @param counterLabel
   * @return
   */
  protected boolean isExcluded(String counterLabel) {
    var counter = counterMap.get(counterLabel);
    if (counter == null) {
      return true;
    }

    if (excludedCounters.contains(counter)) {
      return true;
    }

    return false;
  }

}
