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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * provide access to Winlink CMS apis. No business logic, only web service
 *
 * see: https://api.winlink.org/metadata
 *
 */
public class MockCmsWebService extends AbstractBaseCmsWebService {
  private static final Logger logger = LoggerFactory.getLogger(MockCmsWebService.class);

  private String mockSenderResponse;
  private String mockSourceResponse;
  private String mockChannelResponse;
  private String mockGatewayResponse;

  public MockCmsWebService(IConfigurationManager cm) throws IOException {
    super(cm);

    mockSenderResponse = Files.readString(Path.of(cm.getAsString(Key.CMS_MOCK_SENDER_PATH)));
    mockSourceResponse = Files.readString(Path.of(cm.getAsString(Key.CMS_MOCK_SOURCE_PATH)));
    mockChannelResponse = Files.readString(Path.of(cm.getAsString(Key.CMS_MOCK_CHANNEL_PATH)));
    mockGatewayResponse = Files.readString(Path.of(cm.getAsString(Key.CMS_MOCK_GATEWAY_PATH)));

    logger.info(getName() + " initialized");
  }

  @Override
  public List<TrafficRecord> trafficLocSenderGet(String sender) {
    return parseTrafficList(mockSenderResponse);
  }

  @Override
  public List<TrafficRecord> trafficLocSourceGet(String source) {
    return parseTrafficList(mockSourceResponse);
  }

  @Override
  public List<ChannelRecord> channelList(List<Integer> modes, int historyHours, String serviceCodes) {
    return parseChannelList(mockChannelResponse);
  }

  @Override
  public List<ChannelRecord> channelList() {
    return parseChannelList(mockChannelResponse);
  }

  @Override
  public List<GatewayChannelReportRecord> gatewayChannelReport(int frequencyMinimum, int frequencyMaximum,
      String serviceCodes, List<Integer> modes) {
    return parseGatewayList(mockGatewayResponse);
  }

  @Override
  public List<GatewayChannelReportRecord> gatewayChannelReport() {
    return parseGatewayList(mockGatewayResponse);
  }

  @Override
  public String getName() {
    return "MockCMSWebService";
  }

}
