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

package com.surftools.wimp.service.pieChart;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PlotlyPieChartService extends AbstractBasePieChartService {
  private static final Logger logger = LoggerFactory.getLogger(PlotlyPieChartService.class);

  public PlotlyPieChartService(IConfigurationManager cm, Map<String, Counter> counterMap) {
    super(cm, counterMap);
  }

  @Override
  public String getName() {
    return "PlotlyPieChartService";
  }

  @Override
  public void makePieCharts() {
    var htmlContent = makeHTMLContent();
    var scriptContent = makeScriptContent();

    var text = TEMPLATE.replace("HTML_CONTENT", htmlContent);
    text = text.replace("SCRIPT_CONTENT", scriptContent);

    var title = cm.getAsString(Key.EXERCISE_DESCRIPTION);
    text = text.replaceAll("TITLE", title);

    var filePath = Path.of(cm.getAsString(Key.PATH), "output", "plottly_pie_chart.html");

    try {
      Files.writeString(filePath, text);
      logger.info("wrote pie char page to: " + filePath);
    } catch (Exception e) {
      logger.error("Exception writing plotly output to: " + filePath + ", " + e.getLocalizedMessage());
    }
  }

  private String makeHTMLContent() {
    var sb = new StringBuilder();

    for (var counterLabel : counterMap.keySet()) {
      if (excludedCounterNames.contains(counterLabel)) {
        logger.info("skipping excluded counter: " + counterLabel);
        continue;
      }

      var counter = counterMap.get(counterLabel);

      var html = HTML_DIV.replaceAll("RAW_COUNTER_LABEL", counterLabel);
      html = html.replaceAll("COUNTER_LABEL", fix(counterLabel));
      html = html.replace("###", String.valueOf(counter.getValueTotal()));
      sb.append(html + "\n");
    }

    var s = sb.toString();
    return s;
  }

  private String fix(String string) {
    var s = string.replaceAll(" ", "_");
    s = s.replaceAll("-", "_");
    s = s.replaceAll("#", "Number_of");
    return s;
  }

  private String makeScriptContent() {
    var sb = new StringBuilder();
    for (var counterLabel : counterMap.keySet()) {
      if (excludedCounterNames.contains(counterLabel)) {
        logger.info("skipping excluded counter: " + counterLabel);
        continue;
      }

      var counter = counterMap.get(counterLabel);
      var labelStringBuilder = new StringBuilder();
      var valueStringBuilder = new StringBuilder();
      var iterator = counter.getDescendingCountIterator();
      while (iterator.hasNext()) {
        var entry = iterator.next();
        var label = entry.getKey().toString();
        labelStringBuilder.append("\"" + label + "\"" + ",");

        var value = entry.getValue() == null ? 0 : entry.getValue();
        valueStringBuilder.append(String.valueOf(value) + ",");
      }

      var labelString = labelStringBuilder.toString();
      var valueString = valueStringBuilder.toString();

      var data = SCRIPT.replaceAll("COUNTER_LABEL", fix(counterLabel));
      data = data.replace("LABELS", labelString.substring(0, labelString.length() - 1));
      data = data.replace("VALUES", valueString.substring(0, valueString.length() - 1));
      sb.append(data + "\n");
    }
    return sb.toString();
  }

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

  final String HTML_DIV = """
      <div>RAW_COUNTER_LABEL (### responses)
        <div id="COUNTER_LABEL"></div>
      </div>
      <hr>
      """;

  final String SCRIPT = """
      var data_COUNTER_LABEL = [{
        type: "pie",
        values: [VALUES],
        labels: [LABELS],
        textinfo: "label+percent",
        textposition: "outside",
        automargin: true }];

      Plotly.newPlot('COUNTER_LABEL', data_COUNTER_LABEL);
      """;

}
