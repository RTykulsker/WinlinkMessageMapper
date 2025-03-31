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
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBaseChartService implements IChartService {
  private static final Logger logger = LoggerFactory.getLogger(AbstractBaseChartService.class);

  // input
  protected IConfigurationManager cm;
  protected Map<String, Counter> counterMap;
  private List<Counter> counterList;
  protected MessageType messageType; // will be null for MultiMessageFeedbackProcessor; needed to set file name

  // global config
  protected Path fileOutputPath;
  protected Map<String, ChartConfig> configMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, Map<String, Counter> counterMap, MessageType messageType) {
    this.cm = cm;
    this.counterMap = counterMap;
    this.messageType = messageType;

    parseConfig();
  }

  // https://jsonformatter.curiousconcept.com to format down to a single line
  @SuppressWarnings("unused")
  private String exampleConfig = """
      {
        "serviceName": "Plotly",
        "excludeCounters":["Counter3"],
        "includedCounters":["Counter1","Counter2"],
        "fileOutputName":"/tmp/index.html",
        "serviceName":"Plotly",
        "doSingleItemCharts":true,
        "minValues":3,
        "maxValues":10,
        "Counter1":{
          "minValues":10,
          "maxValues":40,
          "doSingleItemCharts":true
        },
      }
      """;

  /**
   * set up with JSON-based configuration; or default values if none provided
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void parseConfig() {
    var fileName = messageType == null ? "summary" : messageType.name().toLowerCase();
    fileOutputPath = Path.of(cm.getAsString(Key.PATH), "output", fileName + "_" + "plottly_chart.html");

    var defaultConfig = new ChartConfig(List.of(ChartType.PIE), false, 0, 10);
    var jsonString = cm.getAsString(Key.CHART_CONFIG, "").trim();
    if (jsonString.isEmpty()) {
      // since there is no override configuration, must go with defaults for everything
      for (var counter : counterMap.values()) {
        configMap.put(counter.getName(), defaultConfig);
      }
      return;
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      var jsonMap = mapper.readValue(jsonString, Map.class);

      var excludedCountersString = (List<String>) jsonMap.get("excludedCounters");
      var includedCountersString = (List<String>) jsonMap.get("includedCounters");
      counterList = filterCounters(includedCountersString, excludedCountersString, counterMap);

      var safeKeys = Set
          .of("includedCounters", "excludedCounters", "doSingleItemCharts", "minValues", "maxValues", "serviceName");
      validateJson("Global", jsonMap, safeKeys);

      // get "global" variables from config so that we can build default ChartConfig
      var doSingleItemCharts = (Boolean) jsonMap.getOrDefault("doSingleItemCharts", defaultConfig.doSingleItemCharts());
      var minValueCount = (Integer) jsonMap.getOrDefault("minValues", defaultConfig.minValueCount());
      var maxValueCount = (Integer) jsonMap.getOrDefault("maxValues", defaultConfig.maxValueCount());

      var chartConfig = new ChartConfig(List.of(ChartType.PIE), doSingleItemCharts, minValueCount, maxValueCount);
      for (var counter : counterList) {
        var name = counter.getName();
        var jsonMapForCounter = (Map) jsonMap.get(name);
        if (jsonMapForCounter == null) {
          configMap.put(name, chartConfig);
          logger.debug("no json for :" + name + ", using default config");
        } else {
          validateJson(name, jsonMapForCounter, Set.of("doSingleItemCharts", "minValues", "maxValues"));
          var a_doSingleItemCharts = (Boolean) jsonMapForCounter
              .getOrDefault("doSingleItemCharts", defaultConfig.doSingleItemCharts());
          var a_minValueCount = (Integer) jsonMapForCounter.getOrDefault("minValues", defaultConfig.minValueCount());
          var a_maxValueCount = (Integer) jsonMapForCounter.getOrDefault("maxValues", defaultConfig.maxValueCount());
          var a_chartConfig = new ChartConfig(List.of(ChartType.PIE), a_doSingleItemCharts, a_minValueCount,
              a_maxValueCount);
          configMap.put(name, a_chartConfig);
          logger.info("custom config for :" + name + ", " + a_chartConfig.toString());
        }
      }

    } catch (Exception e) {
      logger.error("can't parse " + Key.CHART_CONFIG.name() + ", " + e.getMessage());
      throw new RuntimeException(e);
    }

  }

  @SuppressWarnings("rawtypes")
  private void validateJson(String label, Map jsonMap, Set<String> safeKeys) {
    var badKeys = new ArrayList<String>();
    for (var jsonKey : jsonMap.keySet()) {
      var jsonKeyName = (String) jsonKey;
      if (counterMap.get(jsonKeyName) == null && !safeKeys.contains(jsonKeyName)) {
        badKeys.add(jsonKeyName);
      }
    }
    if (badKeys.size() > 0) {
      throw new RuntimeException("Unknown Counter names in " + label + " configuration: " + String.join(",", badKeys));
    }
  }

  protected List<Counter> filterCounters(List<String> includedCountersString, List<String> excludedCountersString,
      Map<String, Counter> counterMap) {
    if (includedCountersString == null && excludedCountersString == null) {
      return counterMap.values().stream().sorted().toList();
    }

    var includedSet = new HashSet<String>();
    if (includedCountersString != null) {
      for (var name : includedCountersString) {
        if (!counterMap.containsKey(name)) {
          throw new RuntimeException("Counter map doesn't contain includedCounter: " + name);
        }
        includedSet.add(name);
      }
      logger.info("included Counters: " + String.join(",", includedSet.stream().sorted().toList()));
    }

    var excludedSet = new HashSet<String>();
    if (excludedCountersString != null) {
      for (var name : excludedCountersString) {
        if (!counterMap.containsKey(name)) {
          throw new RuntimeException("Counter map doesn't contain excludedCounter: " + name);
        }
        excludedSet.add(name);
      }
      logger.info("excluded Counters: " + String.join(",", excludedSet.stream().sorted().toList()));
    }
    var intersectionSet = new HashSet<String>(includedSet);
    intersectionSet.retainAll(excludedSet);
    if (intersectionSet.size() > 0) {
      throw new RuntimeException("Counter config included and excludes the following counters: "
          + String.join(",", intersectionSet.stream().sorted().toList()));
    }

    var tmpList = new ArrayList<Counter>();
    for (var name : counterMap.keySet()) {
      var isCandidate = includedSet.size() == 0 ? true : includedSet.contains(name);
      isCandidate = excludedSet.size() == 0 ? isCandidate : !excludedSet.contains(name);
      if (isCandidate) {
        tmpList.add(counterMap.get(name));
      }
    }

    var ret = tmpList.stream().sorted().toList();
    logger.info("filtering returning " + ret.size() + " counters");
    return ret;
  }

}
