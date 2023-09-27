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

import java.util.List;

import com.surftools.wimp.service.IService;

public interface ICmsWebService extends IService {

  /**
   * return a list of TrafficRecords for sender
   *
   * @param sender
   * @return
   */
  public List<TrafficRecord> trafficLocSenderGet(String sender);

  /**
   * return a list of TrafficRecords for source
   *
   * @param sender
   * @return
   */
  public List<TrafficRecord> trafficLocSourceGet(String source);

  /**
   * Returns a list of channel records matching the request criteria.
   *
   * @param modes:
   *          One or more operating modes (default: all)
   * @param historyHours:
   *          Number of hours of history to include (default: 6)
   * @param serviceCodes:
   *          One or more service codes (default: PUBLIC)
   * @return
   */
  public List<ChannelRecord> channelList(List<Integer> modes, int historyHours, String serviceCodes);

  /**
   * Convenience method to return a list of channel records matching the request criteria, for all modes, 6 hours and
   * PUBLIC,EMCOMM service codes
   *
   * @return
   */
  public List<ChannelRecord> channelList();

  /**
   * Convenience method to returns a list of all channel records, all frequencies, PUBLIC,EMCOMM service codes and all
   * modes
   *
   * @param frequencyMinimum:
   *          The minimum frequency (default: 0)
   * @param frequencyMaximum:
   *          The maximum frequency (default: max int)
   * @param serviceCodes:
   *          One or more service codes -- to filter the response to just those service codes (default: PUBLIC)
   * @param modes:
   *          Zero or more modes -- to filter the response for just those modes. (default: empty (all modes))
   * @return
   */
  public List<GatewayChannelReportRecord> gatewayChannelReport(int frequencyMinimum, int frequencyMaximum,
      String serviceCodes, List<Integer> modes);

  /**
   * Convenience method to returns a list of all GatewayChanelReportRecords
   * 
   * @return
   */
  public List<GatewayChannelReportRecord> gatewayChannelReport();

  /**
   * get mode name for a mode number
   *
   * @param modeNumber
   * @return name or UNKNOWN
   */
  public String getModeName(int modeNumber);
}