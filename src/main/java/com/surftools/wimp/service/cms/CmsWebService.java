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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.wimp.configuration.Key;

/**
 * provide access to Winlink CMS apis. No business logic, only web service
 *
 * see: https://api.winlink.org/metadata
 *
 */
public class CmsWebService extends AbstractBaseCmsWebService {
  private static final Logger logger = LoggerFactory.getLogger(CmsWebService.class);

  private static final String DEFAULT_SERVICE_CODES = "PUBLIC,EMCOMM";

  public CmsWebService(IConfigurationManager cm) {
    super(cm);
  }

  @Override
  public List<TrafficRecord> trafficLocSenderGet(String sender) {
    List<TrafficRecord> trafficList = new ArrayList<TrafficRecord>();
    try {
      var client = HttpClient.newBuilder().build();

      Map<String, String> formData = new HashMap<>();
      formData.put("Sender", sender);
      formData.put("Key", callServiceMap.get(Key.CMS_AUTHORIZATION_KEY.name()));

      HttpRequest request = HttpRequest
          .newBuilder()
            .uri(URI.create("https://api.winlink.org/traffic/logs/sender/get"))
            .headers( //
                "Content-Type", "application/x-www-form-urlencoded", //
                "Accept", "application/json" //
            )
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      var statusCode = response.statusCode();
      logger.info("statusCode: " + statusCode);
      var body = response.body();

      trafficList = parseTrafficList(body);
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
    }
    return trafficList;
  }

  @Override
  public List<TrafficRecord> trafficLocSourceGet(String source) {
    List<TrafficRecord> trafficList = new ArrayList<TrafficRecord>();
    try {
      var client = HttpClient.newBuilder().build();

      Map<String, String> formData = new HashMap<>();
      formData.put("Source", source);
      formData.put("Key", callServiceMap.get(Key.CMS_AUTHORIZATION_KEY.name()));

      HttpRequest request = HttpRequest
          .newBuilder()
            .uri(URI.create("https://api.winlink.org/traffic/logs/source/get"))
            .headers( //
                "Content-Type", "application/x-www-form-urlencoded", //
                "Accept", "application/json" //
            )
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      var statusCode = response.statusCode();
      logger.info("statusCode: " + statusCode);
      var body = response.body();

      trafficList = parseTrafficList(body);
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
    }
    return trafficList;
  }

  @Override
  public List<ChannelRecord> channelList(List<Integer> modes, int historyHours, String serviceCodes) {
    List<ChannelRecord> list = new ArrayList<ChannelRecord>();
    try {
      var client = HttpClient.newBuilder().build();

      if (modes == null) {
        modes = new ArrayList<Integer>();
      }

      historyHours = Math.max(0, historyHours);

      if (serviceCodes == null || serviceCodes.isEmpty()) {
        serviceCodes = DEFAULT_SERVICE_CODES;
      }

      var intsAsStringList = modes.stream().map(i -> i.toString()).collect(Collectors.toList());

      Map<String, String> formData = new HashMap<>();
      formData.put("Modes", String.join(",", intsAsStringList));
      formData.put("HistoryHours", String.valueOf(historyHours));
      formData.put("ServiceCodes", serviceCodes);
      formData.put("Key", callServiceMap.get(Key.CMS_AUTHORIZATION_KEY.name()));

      HttpRequest request = HttpRequest
          .newBuilder()
            .uri(URI.create("https://api.winlink.org/channel/list"))
            .headers( //
                "Content-Type", "application/x-www-form-urlencoded", //
                "Accept", "application/json" //
            )
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      var statusCode = response.statusCode();
      logger.info("statusCode: " + statusCode);
      var body = response.body();

      list = parseChannelList(body);
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
    }
    return list;
  }

  @Override
  public List<ChannelRecord> channelList() {
    return channelList(new ArrayList<Integer>(), 0, DEFAULT_SERVICE_CODES);
  }

  @Override
  public List<GatewayChannelReportRecord> gatewayChannelReport(int frequencyMinimum, int frequencyMaximum,
      String serviceCodes, List<Integer> modes) {
    List<GatewayChannelReportRecord> list = new ArrayList<GatewayChannelReportRecord>();
    try {
      var client = HttpClient.newBuilder().build();

      frequencyMinimum = Math.max(0, frequencyMinimum);
      frequencyMaximum = Math.max(0, frequencyMaximum);

      if (modes == null) {
        modes = new ArrayList<Integer>();
      }

      if (serviceCodes == null || serviceCodes.isEmpty()) {
        serviceCodes = DEFAULT_SERVICE_CODES;
      }

      var intsAsStringList = modes.stream().map(i -> i.toString()).collect(Collectors.toList());

      Map<String, String> formData = new HashMap<>();

      formData.put("Modes", String.join(",", intsAsStringList));
      formData.put("ServiceCodes", serviceCodes);
      formData.put("FrequencyMinimum", String.valueOf(frequencyMinimum));
      formData.put("FrequencyMaximum", String.valueOf(frequencyMaximum));
      formData.put("Key", callServiceMap.get(Key.CMS_AUTHORIZATION_KEY.name()));

      HttpRequest request = HttpRequest
          .newBuilder()
            .uri(URI.create("https://api.winlink.org/gateway/channel/report"))
            .headers( //
                "Content-Type", "application/x-www-form-urlencoded", //
                "Accept", "application/json" //
            )
            .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
            .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      var statusCode = response.statusCode();
      logger.info("statusCode: " + statusCode);
      var body = response.body();

      list = parseGatewayList(body);
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
    }
    return list;
  }

  @Override
  public List<GatewayChannelReportRecord> gatewayChannelReport() {
    return gatewayChannelReport(0, Integer.MAX_VALUE, DEFAULT_SERVICE_CODES, new ArrayList<Integer>());
  }

  @Override
  public String getName() {
    return "CMSWebService";
  }

}
