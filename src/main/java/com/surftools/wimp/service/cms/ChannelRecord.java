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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.surftools.wimp.core.IWritableTable;

public record ChannelRecord(LocalDateTime dateTime, String callsign, String baseCallsign, String gridsquare,
    int frequency, int mode, int baud, int power, int height, int gain, int direction, String operatingHours,
    String serviceCode) implements IWritableTable {

  final static String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  final static DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);

  @Override
  public String[] getHeaders() {
    return new String[] { "DateTime", "Callsign", "BaseCallsign", "Gridsquare", //
        "Frequency", "Mode", "Baud", //
        "Power", "Height", "Gain", //
        "Direction", "Hours", "ServiceCode" };
  }

  @Override
  public String[] getValues() {
    return new String[] { DTF.format(dateTime), callsign, baseCallsign, gridsquare, //
        String.valueOf(frequency), String.valueOf(mode), String.valueOf(baud), //
        String.valueOf(power), String.valueOf(height), String.valueOf(gain), //
        String.valueOf(direction), operatingHours, serviceCode };
  }

  public static ChannelRecord fromFields(String[] f) {
    return new ChannelRecord(LocalDateTime.parse(f[0], DTF), f[1], f[2], f[3], //
        Integer.valueOf(f[4]), Integer.valueOf(f[5]), Integer.valueOf(f[6]), //
        Integer.valueOf(f[7]), Integer.valueOf(f[8]), Integer.valueOf(f[9]), //
        Integer.valueOf(f[10]), f[11], f[12]);
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ChannelRecord) other;
    return dateTime.compareTo(o.dateTime);
  }
}
