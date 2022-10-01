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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * if no specific aggregator is to be instantiated
 *
 * @author bobt
 *
 */
public class DefaultAggregator extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(DefaultAggregator.class);

  public DefaultAggregator() {
    super(logger);
  }

  @Override
  /**
   * let's just include senders who sent more than one different type of message
   */
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    super.aggregate(messageMap);

    var newList = new ArrayList<AggregateMessage>();
    for (var m : aggregateMessages) {
      var messageTypeCount = (int) m.data().get(KEY_MESSAGE_TYPE_COUNT);
      if (messageTypeCount >= 2) {
        newList.add(m);
      }
    }
    aggregateMessages = newList;
  }

  @Override
  public void output(String pathName) {
    super.output(pathName);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { //
        "From", //
        "DateTime", //
        "Latitude", //
        "Longitude", //
        "Messages", //
        "MessageTypes" //
    };
  }

  @Override
  public String[] getValues(AggregateMessage m) {
    var ret = new String[] { //
        m.from(), //
        m.data().get(KEY_END_DATETIME).toString(), //
        String.valueOf(m.data().get(KEY_AVERAGE_LATITUDE)), //
        String.valueOf(m.data().get(KEY_AVERAGE_LONGITUDE)), //
        String.valueOf(m.data().get(KEY_MESSAGE_COUNT)), //
        String.valueOf(m.data().get(KEY_MESSAGE_TYPE_COUNT)), //
    };
    return ret;
  }

}
