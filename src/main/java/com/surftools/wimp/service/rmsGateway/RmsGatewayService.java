/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

package com.surftools.wimp.service.rmsGateway;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.IService;
import com.surftools.wimp.service.cms.ChannelRecord;
import com.surftools.wimp.service.cms.CmsWebService;
import com.surftools.wimp.service.cms.ICmsWebService;
import com.surftools.wimp.service.cms.TrafficRecord;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class RmsGatewayService implements IService {
  private static Logger logger = LoggerFactory.getLogger(RmsGatewayService.class);

  protected IConfigurationManager cm;
  protected ICmsWebService cmsService;

  private Map<TrafficKey, TrafficRecord> trafficMap = new HashMap<>();
  private Map<ChannelKey, ChannelRecord> channelMap = new HashMap<>();
  private Set<String> senderSet = new HashSet<>();
  private boolean isInitialized;

  private boolean isChannelDataCached = false;
  private boolean isTrafficDataCached = false;

  public RmsGatewayService(IConfigurationManager cm) {
    this.cm = cm;

    isChannelDataCached = cm.getAsBoolean(Key.CMS_CACHE_CHANNELS);
    isTrafficDataCached = cm.getAsBoolean(Key.CMS_CACHE_TRAFFIC);

    var authKey = cm.getAsString(Key.CMS_AUTHORIZATION_KEY);
    if (authKey != null && authKey.length() > 0) {
      try {
        cmsService = new CmsWebService(cm);
        if (isChannelDataCached) {
          readChannelData();
          isInitialized = true;
        }

        int dupCount = 0;
        if (!isChannelDataCached || (isChannelDataCached && channelMap.size() == 0)) {
          var channelList = cmsService.channelList();
          logger.info("received " + channelList.size() + " channel records");
          for (var cr : channelList) {
            var channelKey = new ChannelKey(cr.baseCallsign(), cr.frequency());
            channelMap.put(channelKey, cr);
          }
          logger.info("dupCount: " + dupCount);
          isInitialized = true;
          if (isChannelDataCached) {
            writeChannelData();
          }
        }

        if (isTrafficDataCached) {
          readTrafficData();

          for (var trafficKey : trafficMap.keySet()) {
            var sender = trafficKey.sender;
            senderSet.add(sender);
          }
          logger.info("sender set has " + senderSet.size() + " entries");

          isInitialized = true;
        }
      } catch (Exception e) {
        logger.error("Exception initializing: " + getName() + ", " + e.getLocalizedMessage());
      }
    }

  }

  public RmsGatewayResult getLocationOfRmsGateway(String sender, String messageId) {
    if (!isInitialized) {
      return new RmsGatewayResult(sender, messageId, false, null, null, 0, -1, null);
    }

    // assume our web service calls are expensive, so cache the results
    if (!senderSet.contains(sender)) {
      var trafficList = cmsService.trafficLocSenderGet(sender);
      processTrafficList(trafficList);

      if (isTrafficDataCached) {
        writeTrafficData();
      }
    }

    var trafficKey = new TrafficKey(sender, messageId);
    var tr = trafficMap.get(trafficKey);

    RmsGatewayResult result = null;
    if (tr == null) {
      result = new RmsGatewayResult(sender, messageId, false, null, null, 0, -1, null);
    } else {
      var channelKey = new ChannelKey(tr.gateway(), tr.frequency());

      LatLongPair location = null;
      var channelRecord = channelMap.get(channelKey);
      if (channelRecord != null) {
        var gridsquare = channelRecord.gridsquare();
        location = new LatLongPair(gridsquare);
        result = new RmsGatewayResult(sender, messageId, true, location, channelRecord.callsign(), //
            tr.frequency(), channelRecord.mode(), channelRecord.baseCallsign());
      } else {
        var index = tr.gateway().indexOf("-1");
        var baseCallSign = (index == -1) ? tr.gateway() : tr.gateway().substring(0, index - 1);
        result = new RmsGatewayResult(sender, messageId, true, location, tr.gateway(), //
            tr.frequency(), -1, baseCallSign);
      }

      logger.info(result.toString());
    }
    return result;
  }

  /**
   *
   * @param trafficList
   */
  private void processTrafficList(List<TrafficRecord> trafficList) {
    var acceptedCount = 0;
    var sender = "";
    for (var tr : trafficList) {
      if (tr.event().equals("Accepted")) {
        sender = tr.sender();
        var trafficKey = new TrafficKey(sender, tr.messageId());
        trafficMap.put(trafficKey, tr);
        logger.debug("adding: trafficKey: " + trafficKey.toString() + ", trafficRecord: " + tr.toString());
        senderSet.add(sender);
        ++acceptedCount;
      } else {
        var trafficKey = new TrafficKey(sender, tr.messageId());
        logger.debug("skipping: trafficKey: " + trafficKey.toString() + ", trafficRecord: " + tr.toString());
      }
    }
    logger
        .info("web service returns " + trafficList.size() + " traffic records, " + acceptedCount
            + " accepted traffic for sender " + sender);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  @Override
  public String getName() {
    return "RmsGatewayService";
  }

  record TrafficKey(String sender, String messageId) {
  }

  /**
   * ChannelKey *should* contain mode, but since the TrafficLogXXX calls don't return mode, why bother
   */
  record ChannelKey(String baseCallsign, int frequency) {
  }

  private void readChannelData() {
    var channelDataPath = Path.of(cm.getAsString(Key.PATH), "channelData.csv");
    var data = ReadProcessor.readCsvFileIntoFieldsArray(channelDataPath);
    for (var fields : data) {
      var channelRecord = ChannelRecord.fromFields(fields);
      var channelKey = new ChannelKey(channelRecord.baseCallsign(), channelRecord.frequency());
      channelMap.put(channelKey, channelRecord);
    }
    logger
        .info("read " + data.size() + " records from " + channelDataPath.toString() + ", stored " + channelMap.size()
            + " records into channelMap");
  }

  private void writeChannelData() {
    var oldPath = Path.of(cm.getAsString(Key.PATH), "channelData.csv");
    if (oldPath.toFile().exists()) {
      var timeStamp = LocalDateTime.now().toString();
      var newPath = Path.of(oldPath.toString() + "-" + timeStamp + ".csv");
      try {
        Files.move(oldPath, newPath); // rename
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    var values = new ArrayList<IWritableTable>(channelMap.values());
    WriteProcessor.writeTable(values, oldPath);
    logger.info("wrote " + channelMap.values().size() + " channel records from " + oldPath.toString());
  }

  private void readTrafficData() {
    var trafficDataPath = Path.of(cm.getAsString(Key.PATH), "trafficData.csv");
    var data = ReadProcessor.readCsvFileIntoFieldsArray(trafficDataPath);
    for (var fields : data) {
      var trafficRecord = TrafficRecord.fromFields(fields);
      var trafficKey = new TrafficKey(trafficRecord.sender(), trafficRecord.messageId());
      trafficMap.put(trafficKey, trafficRecord);
    }
    logger
        .info("read " + data.size() + " records from " + trafficDataPath.toString() + ", stored " + trafficMap.size()
            + " records into channelMap");
  }

  private void writeTrafficData() {
    var oldPath = Path.of(cm.getAsString(Key.PATH), "trafficData.csv");
    if (oldPath.toFile().exists()) {
      var timeStamp = LocalDateTime.now().toString();
      var newPath = Path.of(oldPath.toString() + "-" + timeStamp + ".csv");
      try {
        Files.move(oldPath, newPath); // rename
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    var values = new ArrayList<IWritableTable>(trafficMap.values());
    WriteProcessor.writeTable(values, oldPath);
    logger.info("wrote " + trafficMap.values().size() + " traffic records from " + oldPath.toString());
  }
}
