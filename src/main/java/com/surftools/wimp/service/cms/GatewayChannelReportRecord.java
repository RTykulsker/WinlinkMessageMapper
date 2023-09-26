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

/**
 * response for GatewayChannelReport
 */
public record GatewayChannelReportRecord(String callsign, String gridsquare, int frequency, int mode, String hours,
    String serviceCode) implements Comparable<GatewayChannelReportRecord> {

  @Override
  public int compareTo(GatewayChannelReportRecord o) {
    var cmp = callsign.compareTo(o.callsign);
    if (cmp != 0) {
      return cmp;
    }

    cmp = gridsquare.compareTo(o.gridsquare);
    if (cmp != 0) {
      return cmp;
    }

    cmp = frequency - o.frequency;
    if (cmp != 0) {
      return cmp;
    }
    return serviceCode.compareTo(o.serviceCode);
  }

}
