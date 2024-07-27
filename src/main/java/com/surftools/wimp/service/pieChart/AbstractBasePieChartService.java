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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBasePieChartService implements IPieChartService {

  protected IConfigurationManager cm;
  protected Map<String, Counter> counterMap;
  protected Set<String> excludedCounterNames;

  public static AbstractBasePieChartService getService(IConfigurationManager cm, Map<String, Counter> counterMap) {
    return new PlotlyPieChartService(cm, counterMap);
  }

  public AbstractBasePieChartService(IConfigurationManager cm, Map<String, Counter> counterMap) {
    this.cm = cm;
    this.counterMap = counterMap;
    this.excludedCounterNames = new HashSet<>();
  }

  @Override
  public void excludeCounter(String counterName) {
    excludedCounterNames.add(counterName);
  }

  @Override
  public void excludeCounters(List<String> counterNames) {
    excludedCounterNames.addAll(counterNames);
  }

}
