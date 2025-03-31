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

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PlotlyChartService extends AbstractBaseChartService {
  private static final Logger logger = LoggerFactory.getLogger(PlotlyChartService.class);

  private List<String> skipFilteredList = new ArrayList<>();
  private List<String> skipMinCountList = new ArrayList<>();
  private List<String> skipMaxCountList = new ArrayList<>();
  private List<String> skipSingleItemList = new ArrayList<>();

  @Override
  public void initialize(IConfigurationManager cm, Map<String, Counter> counterMap, MessageType messageType) {
    super.initialize(cm, counterMap, messageType);
  }

  @Override
  public void makeCharts() {

    final String TEMPLATE = """
        <!DOCTYPE html>
        <html lang="en" class="">
        <head>
            <meta charset="UTF-8">
            <title>TITLE</title>
            <script src="https://cdn.plot.ly/plotly-2.34.0.min.js"></script>
        </head>

        <body>
          HTML_CONTENT
        <script>
          SCRIPT_CONTENT
        </script>
        </body>
        """;

    var html = makeHTMLContent();
    if (html.length() == 0) {
      logger.info("returning because no content!");
      return;
    }

    var text = TEMPLATE.replace("HTML_CONTENT", html);
    text = text.replace("SCRIPT_CONTENT", makeScriptContent());

    var messageTypeName = messageType == null ? "summary" : messageType.name().toLowerCase();
    var title = cm.getAsString(Key.EXERCISE_DESCRIPTION, messageTypeName + " histograms");
    text = text.replaceAll("TITLE", title);

    try {
      Files.writeString(fileOutputPath, text);
      logger.info("wrote chart page to: " + fileOutputPath);
    } catch (Exception e) {
      logger.error("Exception writing plotly output to: " + fileOutputPath + ", " + e.getLocalizedMessage());
    }

    final var f = "skipped %d charts for Counter: %s %s %s";
    var map = Map.of("filtered", skipFilteredList, "maxCount", skipMaxCountList, "single item", skipSingleItemList);
    map.keySet().stream().forEach(label -> {
      var list = map.get(label);
      logger.info(String.format(f, list.size(), label, (list.size() == 0 ? "" : ": "), String.join(",", list)));
    });
  }

  private String makeHTMLContent() {
    final String HTML_DIV = """
        <div>COUNTER_LABEL (### responses) DISPLAY_INDEX
          <div id="plotly_id_COUNTER_ID"></div>
        </div>
        <hr>
        """;

    var counterId = 0;
    var sb = new StringBuilder();

    var anyDoSingleItemCharts = configMap.values().stream().anyMatch(c -> c.doSingleItemCharts());
    if (!anyDoSingleItemCharts) {
      sb.append("<div><h3>(excluding single-item pie charts)</h3></div>\n");
    }

    for (var counterLabel : counterMap.keySet().stream().sorted().toList()) {
      var counter = counterMap.get(counterLabel);
      var config = configMap.get(counterLabel);
      if (config == null) {
        skipFilteredList.add(counterLabel);
        continue; // the counter has been filtered
      }

      var keyCount = counter.getKeyCount();
      if (!config.doSingleItemCharts() && keyCount == 1) {
        logger.debug("skipping counter: " + counterLabel + ", only one value");
        skipSingleItemList.add(counterLabel);
        continue;
      }

      if (keyCount >= config.maxValueCount()) {
        logger.debug("### skipping counter: " + counterLabel + ", too many values (" + keyCount + ")");
        skipMaxCountList.add(counterLabel);
        continue;
      }
      ++counterId;

      var html = HTML_DIV.replaceAll("COUNTER_LABEL", counterLabel);
      html = html.replaceAll("COUNTER_ID", String.valueOf(counterId));
      html = html.replace("###", String.valueOf(counter.getValueTotal()));
      html = html.replace("DISPLAY_INDEX", "[chart #" + String.valueOf(counterId) + "]");
      sb.append(html + "\n");
    }

    var s = sb.toString();
    return s;
  }

  private String makeScriptContent() {
    final String SCRIPT = """
        var layout={};
        // COUNTER_LABEL
        var data_COUNTER_ID = [{
          type: "CHART_TYPE",
          values: [VALUES],
          labels: [LABELS],
          textinfo: "label+percent",
          textposition: "outside",
          automargin: true }];

        Plotly.newPlot("plotly_id_COUNTER_ID", data_COUNTER_ID, layout);
        """;

    var counterId = 0;
    var sb = new StringBuilder();
    for (var counterLabel : counterMap.keySet().stream().sorted().toList()) {

      var counter = counterMap.get(counterLabel);
      var config = configMap.get(counterLabel);
      if (config == null) {
        continue; // the counter has been filtered
      }

      var keyCount = counter.getKeyCount();
      if (!config.doSingleItemCharts() && keyCount == 1) {
        logger.debug("skipping counter: " + counterLabel + ", only one value");
        continue;
      }

      if (keyCount >= config.maxValueCount()) {
        logger
            .debug("### skipping counter: " + counterLabel + ", too many values (" + keyCount + " >= "
                + config.maxValueCount() + ")");
        continue;
      }
      ++counterId;

      var labelStringBuilder = new StringBuilder();
      var valueStringBuilder = new StringBuilder();

      var minValue = config.minValueCount();
      var iterator = counter.getDescendingCountIterator();
      while (iterator.hasNext()) {
        var entry = iterator.next();

        var label = entry.getKey();
        label = label == null ? "" : label.toString();
        var value = entry.getValue() == null ? 0 : entry.getValue();

        if (value < minValue) {
          logger
              .debug("skipping counter values for: " + counterLabel + "/" + label + ", because value: " + value
                  + "< min value: " + minValue);
          skipMinCountList.add(counterLabel);
          continue;
        }

        labelStringBuilder.append("\"" + label + "\"" + ",");
        valueStringBuilder.append(String.valueOf(value) + ",");
      }

      var labelString = labelStringBuilder.toString();
      var valueString = valueStringBuilder.toString();

      // var data = SCRIPT.replaceAll("COUNTER_LABEL", fix(counterLabel));
      var data = SCRIPT.replaceAll("COUNTER_LABEL", counterLabel);
      data = data.replace("LABELS", labelString.substring(0, labelString.length() - 1));
      data = data.replace("VALUES", valueString.substring(0, valueString.length() - 1));
      data = data.replace("CHART_TYPE", config.chartTypes().get(0).toString());
      data = data.replaceAll("COUNTER_ID", String.valueOf(counterId));
      sb.append(data + "\n");
    }
    return sb.toString();
  }

  @Override
  public String getName() {
    return "PlotlyChartService";
  }

}
