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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  private static IChartService service;
  private static Set<Counter> excludedCounters = new HashSet<>();

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

    try {
      ObjectMapper mapper = new ObjectMapper();
      var jsonMap = mapper.readValue(jsonString, Map.class);
      @SuppressWarnings("rawtypes")
      var messageTypeMap = (Map) jsonMap.get(messageType.toString());
      if (messageTypeMap == null) {
        return;
      }
      parseMinValues(messageTypeMap.get("minValues"));
      parseFileOutputName(messageTypeMap.get("fileOutputName"));
      parseExcludedCounters(messageTypeMap.get("excludedCounters"));
      parseServiceName(messageTypeMap.get("serviceName"));
      parseChartType(messageTypeMap.get("chartType"));
      parseExtraLayout(messageTypeMap.get("extraLayout"));
    } catch (Exception e) {
      logger.error("can't parse " + Key.CHART_CONFIG.name() + ", " + e.getMessage());
      throw new RuntimeException(e);
    }

  }

  protected static void parseExtraLayout(Object object) {
    if (object == null) {
      return;
    }

    extraLayout = (String) object;
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

    var counterNames = ((String) object).split(",");
    for (var counterName : counterNames) {
      var counter = counterMap.get(counterName);
      if (counter == null) {
        throw new RuntimeException("no Counter for " + counterName);
      }
      excludedCounters.add(counter);
    }
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
    service = new PlotlyChartService();
    fileOutputPath = Path
        .of(cm.getAsString(Key.PATH), "output", messageType.name().toLowerCase() + "_" + "plottly_chart.html");

    excludedCounters = new HashSet<>();
    minValuesMap = new HashMap<>();
    chartType = "pie";
    extraLayout = "var layout={};";
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
