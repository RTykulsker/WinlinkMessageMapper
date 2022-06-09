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

package com.surftools.winlinkMessageMapper.aggregation.named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class ETO_2022_05_14_Aggregator extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_05_14_Aggregator.class);

  private static final String KEY_RMS_COUNT = "rmsCount";
  private static final String KEY_P2P_COUNT = "p2pCount";
  private static final String KEY_DESTINATIONS = "destinations";

  public ETO_2022_05_14_Aggregator() {
    super(logger);
  }

  @Override
  /**
   * include all fsr reports, with RMS and P2P counts
   *
   */
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    super.aggregate(messageMap);

    var newList = new ArrayList<AggregateMessage>();

    for (var m : aggregateMessages) {
      int rmsCount = 0;
      int p2pCount = 0;
      var from = m.from();
      var list = fromListMap.get(from);
      for (var message : list) {
        var messageType = message.getMessageType();
        if (messageType != MessageType.FIELD_SITUATION_REPORT) {
          continue;
        }

        var to = message.to;
        if (to.startsWith("ETO")) {
          ++rmsCount;
        } else {
          ++p2pCount;
        }
      } // end for over exported messages

      if (rmsCount == 0 && p2pCount == 0) {
        continue;
      }

      m.data().put(KEY_RMS_COUNT, rmsCount);
      m.data().put(KEY_P2P_COUNT, p2pCount);
      m.data().put(KEY_DESTINATIONS, makeDestination(rmsCount, p2pCount));

      newList.add(m);
    } // end for over aggregate
    aggregateMessages = newList;
  }

  private Object makeDestination(int rmsCount, int p2pCount) {
    if (rmsCount > 0 && p2pCount > 0) {
      return "both RMS and P2P";
    } else if (rmsCount == 0) {
      return "P2P only";
    } else {
      return "RMS only";
    }
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
        "RMS Messages", //
        "P2P Messages", //
        "Destinations", //
    };
  }

  @Override
  public String[] getValues(AggregateMessage m) {
    var ret = new String[] { //
        m.from(), //
        m.data().get(KEY_END_DATETIME).toString(), //
        String.valueOf(m.data().get(KEY_AVERAGE_LATITUDE)), //
        String.valueOf(m.data().get(KEY_AVERAGE_LONGITUDE)), //
        String.valueOf(m.data().get(KEY_RMS_COUNT)), //
        String.valueOf(m.data().get(KEY_P2P_COUNT)), //
        (String) m.data().get(KEY_DESTINATIONS) //
    };
    return ret;
  }

}
