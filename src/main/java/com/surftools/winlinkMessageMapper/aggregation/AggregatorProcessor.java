/**

The MIT License (MIT)

Copyright (c) 2022, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.aggregation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * instantiates the named IAggregator
 *
 * @author bobt
 *
 */
public class AggregatorProcessor {

  private IAggregator aggregator;
  private IConfigurationManager cm;

  public AggregatorProcessor(String aggregatorName, IConfigurationManager cm) {
    this.cm = cm;

    if (aggregatorName.equals("default")) {
      aggregator = new DefaultAggregator();
      return;
    }

    final var prefixes = new String[] { //
        "com.surftools.winlinkMessageMapper.aggregation.named.", //
        "com.surftools.winlinkMessageMapper.aggregation.p2p.named.", //
        "" };

    for (var prefix : prefixes) {
      var className = prefix + aggregatorName;
      try {
        var clazz = Class.forName(className);
        if (clazz != null) {
          aggregator = (IAggregator) clazz.getDeclaredConstructor().newInstance();
          break;
        }
      } catch (ClassNotFoundException e) {
        ;
      } catch (Exception e) {
        throw new RuntimeException("could not instantiate aggregator: " + e.getLocalizedMessage());
      }
    }

    if (aggregator == null) {
      throw new RuntimeException("could not find aggregator class for name: " + aggregatorName);
    }
  }

  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap, String pathName) {
    aggregator.setConfigurationManager(cm);
    aggregator.aggregate(messageMap);
    aggregator.output(pathName);
  }

  public void setDumpIds(Set<String> _dumpIds) {
    aggregator.setDumpIds(_dumpIds);
  }
}
