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

package com.surftools.wimp.service.cms;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * provide access to Winlink CMS apis. No business logic, only web service
 *
 * see: https://api.winlink.org/metadata
 *
 */
public abstract class AbstractBaseCmsWebService implements ICmsWebService {
  private static final Logger logger = LoggerFactory.getLogger(AbstractBaseCmsWebService.class);

  protected Map<String, String> callServiceMap = new HashMap<>();
  private final Map<Integer, String> modeNumberNameMap;

  public AbstractBaseCmsWebService(IConfigurationManager cm) {
    // In theory, we can be issued separate keys for each CMS service. In practice, we only have one key
    callServiceMap.put(Key.CMS_AUTHORIZATION_KEY.name(), cm.getAsString(Key.CMS_AUTHORIZATION_KEY));

    modeNumberNameMap = new HashMap<>();
    loadModeMap();
  }

  private void loadModeMap() {
    var lines = modeNames.split("\n");
    for (var line : lines) {
      var fields = line.split(":");
      if (fields.length >= 2) {
        var key = Integer.valueOf(fields[0].strip());
        var value = fields[1].strip();
        modeNumberNameMap.put(key, value);
      }
    }
  }

  /**
   * get mode name for a mode number
   *
   * @param modeNumber
   * @return name or UNKNOWN
   */
  @Override
  public String getModeName(int modeNumber) {
    return modeNumberNameMap.getOrDefault(modeNumber, "UNKNOWN");
  }

  @Override
  public abstract List<TrafficRecord> trafficLocSenderGet(String sender);

  @Override
  public abstract List<TrafficRecord> trafficLocSourceGet(String source);

  @Override
  public abstract List<ChannelRecord> channelList(List<Integer> modes, int historyHours, String serviceCodes);

  @Override
  public abstract List<ChannelRecord> channelList();

  @Override
  public abstract List<GatewayChannelReportRecord> gatewayChannelReport(int frequencyMinimum, int frequencyMaximum,
      String serviceCodes, List<Integer> modes);

  @Override
  public abstract List<GatewayChannelReportRecord> gatewayChannelReport();

  /**
   * parse json TrafficList response body
   *
   * @param body
   * @return
   */
  protected List<TrafficRecord> parseTrafficList(String body) {
    var list = new ArrayList<TrafficRecord>();

    try {
      var objectMapper = new ObjectMapper();
      var root = objectMapper.readTree(body);

      var array = (ArrayNode) root.get("TrafficList");
      logger.info("array size: " + array.size());

      for (var node : array) {
        var objectNode = (ObjectNode) node;
        var trafficRecord = new TrafficRecord(//
            convertTimestampToLocalDateTime(objectNode.get("Timestamp").asText()), //
            objectNode.get("Site").asText(), //
            objectNode.get("Event").asText(), //
            objectNode.get("MessageId").asText(), //
            objectNode.get("ClientType").asInt(), //
            objectNode.get("Callsign").asText(), //
            objectNode.get("Gateway").asText(), //
            objectNode.get("Source").asText(), //
            objectNode.get("Sender").asText(), //
            objectNode.get("Subject").asText(), //
            objectNode.get("Size").asInt(), //
            objectNode.get("Attachments").asInt(), //
            objectNode.get("Frequency").asInt() //
        );
        list.add(trafficRecord);
      }
    } catch (Exception e) {
      logger.error("exception parsing body, " + e.getMessage());
    }
    return list;
  }

  protected List<ChannelRecord> parseChannelList(String body) {
    var list = new ArrayList<ChannelRecord>();

    try {
      var objectMapper = new ObjectMapper();
      var root = objectMapper.readTree(body);

      var array = (ArrayNode) root.get("Channels");
      logger.info("array size: " + array.size());

      ObjectNode objectNode;
      for (var node : array) {
        objectNode = (ObjectNode) node;
        var channelRecord = new ChannelRecord(//
            convertTimestampToLocalDateTime(objectNode.get("Timestamp").asText()), //
            objectNode.get("Callsign").asText(), //
            objectNode.get("BaseCallsign").asText(), //
            objectNode.get("GridSquare").asText(), //
            objectNode.get("Frequency").asInt(), //
            objectNode.get("Mode").asInt(), //
            objectNode.get("Baud").asInt(), //
            objectNode.get("Power").asInt(), //
            objectNode.get("Height").asInt(), //
            objectNode.get("Gain").asInt(), //
            objectNode.get("Direction").asInt(), //
            objectNode.get("OperatingHours").asText(), //
            objectNode.get("ServiceCode").asText() //
        );
        list.add(channelRecord);
      }
    } catch (Exception e) {
      logger.error("exception parsing body, " + e.getMessage());
    }
    return list;
  }

  protected List<GatewayChannelReportRecord> parseGatewayList(String body) {
    var list = new ArrayList<GatewayChannelReportRecord>();

    try {
      var objectMapper = new ObjectMapper();
      var root = objectMapper.readTree(body);

      var array = (ArrayNode) root.get("Channels");
      logger.info("array size: " + array.size());

      ObjectNode objectNode;
      for (var node : array) {
        objectNode = (ObjectNode) node;
        var gatewayRecord = new GatewayChannelReportRecord(//
            objectNode.get("Callsign").asText(), //
            objectNode.get("Gridsquare").asText(), //
            objectNode.get("Frequency").asInt(), //
            objectNode.get("Mode").asInt(), //
            objectNode.get("Hours").asText(), //
            objectNode.get("ServiceCode").asText() //
        );
        list.add(gatewayRecord);
      }
    } catch (Exception e) {
      logger.error("exception parsing body, " + e.getMessage());
    }
    return list;
  }

  protected LocalDateTime convertTimestampToLocalDateTime(String timestamp) {
    // /Date(1694618417000)/
    // 01234567890123456789
    var utcMillis = Long.parseLong(timestamp.substring(6, 19));
    var dateTime = LocalDateTime.ofEpochSecond(utcMillis / 1000, 0, ZoneOffset.UTC);
    logger.debug("dateTime: " + dateTime);
    return dateTime;
  }

  protected String getFormDataAsString(Map<String, String> formData) {
    StringBuilder formBodyBuilder = new StringBuilder();
    for (Map.Entry<String, String> singleEntry : formData.entrySet()) {
      if (formBodyBuilder.length() > 0) {
        formBodyBuilder.append("&");
      }
      formBodyBuilder.append(URLEncoder.encode(singleEntry.getKey(), StandardCharsets.UTF_8));
      formBodyBuilder.append("=");
      formBodyBuilder.append(URLEncoder.encode(singleEntry.getValue(), StandardCharsets.UTF_8));
    }
    return formBodyBuilder.toString();
  }

  @Override
  public String getName() {
    return "AbstractBaseCMSWebService";
  }

  // https://github.com/ARSFI/winlink/wiki/Protocol-Mode-Mappings
  public static final String modeNames = """
      0: Packet 1200
      1: Packet 2400
      2: Packet 4800
      3: Packet 9600
      4: Packet 19200
      5: Packet 38400
      11: Pactor 1
      12: Pactor 1,2
      13: Pactor 1,2,3
      14: Pactor 2
      15: Pactor 2,3
      16: Pactor 3
      17: Pactor 1,2,3,4
      18: Pactor 2,3,4
      19: Pactor 3,4
      20: Pactor 4
      21: WINMOR 500
      22: WINMOR 1600
      30: Robust Packet
      40: ARDOP 200
      41: ARDOP 500
      42: ARDOP 1000
      43: ARDOP 2000
      44: ARDOP 2000 FM
      50: VARA
      51: VARA FM
      52: VARA FM WIDE
      53: VARA 500
      54: VARA 2750
           """;

}
