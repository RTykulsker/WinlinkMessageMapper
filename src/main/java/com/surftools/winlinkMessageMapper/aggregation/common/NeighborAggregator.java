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

package com.surftools.winlinkMessageMapper.aggregation.common;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.util.MyLocationUtils;

/**
 * a "toy" aggregator for nearest (and farthest) neighbors
 *
 * @author bobt
 *
 */
public class NeighborAggregator extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(NeighborAggregator.class);

  private static final String CONFIG_KEY_MAX_NEAREST = "maxNearest";
  private boolean doNearest;
  private int maxNearest = 10;
  private Map<String, TreeSet<NeighborRecord>> nearMap = new HashMap<>();

  private static final String CONFIG_KEY_MAX_FARTHEST = "maxFarthest";
  private boolean doFarthest;
  private int maxFarthest = 10;
  private Map<String, TreeSet<NeighborRecord>> farMap = new HashMap<>();

  @SuppressWarnings("unchecked")
  public NeighborAggregator(String configKey) {
    super(logger);
    super.setOutputFileName("neighbors.csv");

    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

      var jsonMap = mapper.readValue(configKey, Map.class);

      maxNearest = (int) jsonMap.getOrDefault(CONFIG_KEY_MAX_NEAREST, maxNearest);
      doNearest = maxNearest >= 1;

      maxFarthest = (int) jsonMap.getOrDefault(CONFIG_KEY_MAX_FARTHEST, maxFarthest);
      doFarthest = maxFarthest >= 1;
    } catch (Exception e) {
      logger.error("could not parse configuration: " + configKey + ", " + e.getLocalizedMessage());
    }
  }

  public NeighborAggregator() {
    this(10, 10);
  }

  public NeighborAggregator(int maxNearest, int maxFarthest) {
    super(logger);
    super.setOutputFileName("neighbors.csv");

    this.maxNearest = maxNearest;
    doNearest = maxNearest >= 1;

    this.maxFarthest = maxFarthest;
    doFarthest = maxFarthest >= 1;
  }

  @Override
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    logger.info("begin aggregation");
    super.aggregate(messageMap);

    var messageType = getMostCommonMessageType(messageMap);
    if (!messageType.isGisType()) {
      logger.warn("most common messageType: " + messageType.toString() + " is not GIS, returning");
      doOutput = false;
      return;
    }

    if (!doNearest && !doFarthest) {
      logger.warn("neither nearest nor farthest configured, returning");
      doOutput = false;
      return;
    }

    var list = messageMap.get(messageType);
    ExportedMessage[] array = new ExportedMessage[list.size()];
    list.toArray(array);
    var n = list.size();

    var callSet = new HashSet<String>();
    for (var i = 0; i < n; ++i) {
      var fromMessage = array[i];
      var from = fromMessage.from;
      callSet.add(from);

      for (var j = i + 1; j < n; ++j) {
        var toMessage = array[j];
        var to = toMessage.from;
        var distance = computeDistance(fromMessage, toMessage);
        var bearing = computeBearing(fromMessage, toMessage);

        var neighbor = new NeighborRecord(to, distance, bearing);

        if (doNearest) {
          var nearSet = nearMap
              .getOrDefault(from, new TreeSet<NeighborRecord>((a, b) -> a.distanceMiles - b.distanceMiles));
          nearSet.add(neighbor);
          if (nearSet.size() > maxNearest) {
            nearSet.remove(nearSet.last());
          }
          nearMap.put(from, nearSet);
        }

        if (doFarthest) {
          var farSet = farMap
              .getOrDefault(from, new TreeSet<NeighborRecord>((a, b) -> b.distanceMiles - a.distanceMiles));
          farSet.add(neighbor);
          if (farSet.size() > maxNearest) {
            farSet.remove(farSet.last());
          }
          farMap.put(from, farSet);
        }

        var reverseBearing = computeReverseBearing(bearing);
        var reverseNeighbor = new NeighborRecord(from, distance, reverseBearing);

        if (doNearest) {
          var nearSet = nearMap
              .getOrDefault(to, new TreeSet<NeighborRecord>((a, b) -> a.distanceMiles - b.distanceMiles));
          nearSet.add(reverseNeighbor);
          if (nearSet.size() > maxNearest) {
            nearSet.remove(nearSet.last());
          }
          nearMap.put(to, nearSet);
        }

        if (doFarthest) {
          var farSet = farMap
              .getOrDefault(to, new TreeSet<NeighborRecord>((a, b) -> b.distanceMiles - a.distanceMiles));
          farSet.add(reverseNeighbor);
          if (farSet.size() > maxNearest) {
            farSet.remove(farSet.last());
          }
          farMap.put(to, farSet);
        }

      } // end loop over j, inner loop over calls
    } // end loop over i, outer loop over calls

    logger.info("end aggregation");
  }

  private int computeDistance(ExportedMessage fromMessage, ExportedMessage toMessage) {
    return MyLocationUtils.computeDistanceMiles((GisMessage) fromMessage, (GisMessage) toMessage);
  }

  private int computeBearing(ExportedMessage fromMessage, ExportedMessage toMessage) {
    return MyLocationUtils.computeBearing((GisMessage) fromMessage, (GisMessage) toMessage);
  }

  private int computeReverseBearing(int bearing) {
    return (bearing + 180) % 360;
  }

  @Override
  public String[] getHeaders() {
    if (doNearest && doFarthest) {
      return new String[] { "call", "latitude", "longitude", "nearest", "farthest" };
    } else if (doNearest) {
      return new String[] { "call", "latitude", "longitude", "nearest" };
    } else {
      return new String[] { "call", "latitude", "longitude", "farthest" };
    }
  }

  @Override
  public String[] getValues(AggregateMessage message) {
    var from = message.from();
    var latitude = (String) message.data().get(KEY_END_LATITUDE);
    var longitude = (String) message.data().get(KEY_END_LONGITUDE);

    var nearest = "";
    var nearSet = nearMap.get(from);
    if (nearSet != null) {
      var iterator = nearSet.iterator();
      var sb = new StringBuilder();
      while (iterator.hasNext()) {
        var neighbor = iterator.next();
        sb.append(neighbor.to + ": dist: " + neighbor.distanceMiles + " miles, bearing: " + neighbor.bearing + "\n");
      }
      nearest = sb.toString();
    }

    var farthest = "";
    var farSet = farMap.get(from);
    if (farSet != null) {
      var iterator = farSet.iterator();
      var sb = new StringBuilder();
      while (iterator.hasNext()) {
        var neighbor = iterator.next();
        sb.append(neighbor.to + ": dist: " + neighbor.distanceMiles + " miles, bearing: " + neighbor.bearing + "\n");
      }
      farthest = sb.toString();
    }

    if (doNearest && doFarthest) {
      return new String[] { from, latitude, longitude, nearest, farthest };
    } else if (doNearest) {
      return new String[] { from, latitude, longitude, nearest };
    } else {
      return new String[] { from, latitude, longitude, farthest };
    }
  }

  static record NeighborRecord(String to, int distanceMiles, int bearing) {
  };
}
