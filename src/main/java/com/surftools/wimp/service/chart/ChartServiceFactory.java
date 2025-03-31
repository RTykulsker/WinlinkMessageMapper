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

package com.surftools.wimp.service.chart;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class ChartServiceFactory {
  private static final Logger logger = LoggerFactory.getLogger(ChartServiceFactory.class);

  /**
   * static factory method to get configuration-specified IChartService
   *
   * @param cm
   * @return
   */
  @SuppressWarnings({ "unchecked" })
  public static IChartService getChartService(IConfigurationManager cm) {
    IChartService service = null;
    var jsonString = cm.getAsString(Key.CHART_CONFIG, "").trim();
    if (jsonString.isEmpty()) {
      service = new PlotlyChartService();
      logger.info("No " + Key.CHART_CONFIG.toString() + ", returning default: " + service.getName());
      return new PlotlyChartService();
    }

    try {
      ObjectMapper mapper = new ObjectMapper();
      var jsonMap = mapper.readValue(jsonString, Map.class);

      var serviceName = (String) jsonMap.getOrDefault("serviceName", "Plotly");
      for (var prefix : List.of("com.surftools.wimp.service.chart.", "")) {
        for (var suffix : List.of("Service", "ChartService", "")) {
          var className = prefix + serviceName + suffix;
          logger.debug("searching for className: " + className);
          try {
            var clazz = Class.forName(className);
            if (clazz != null) {
              service = (IChartService) clazz.getDeclaredConstructor().newInstance();
              logger.info("For " + Key.CHART_CONFIG.toString() + ", returning: " + service.getName());
              return service;
            }
          } catch (Exception e) {
            ;
          }
        } // end loop over suffixes
      } // end loop over prefixes

    } catch (Exception e) {
      logger.error("can't parse " + Key.CHART_CONFIG.name() + ", " + e.getMessage());
      throw new RuntimeException(e);
    }

    return null;
  }

}
