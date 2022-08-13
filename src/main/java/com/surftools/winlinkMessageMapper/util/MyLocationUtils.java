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

package com.surftools.winlinkMessageMapper.util;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.winlinkMessageMapper.dto.message.GisMessage;

public class MyLocationUtils {

  public static int computeDistanceMiles(GisMessage m1, GisMessage m2) {
    var latitude1 = Double.parseDouble(m1.latitude);
    var longitude1 = Double.parseDouble(m1.longitude);

    var latitude2 = Double.parseDouble(m2.latitude);
    var longitude2 = Double.parseDouble(m2.longitude);

    return LocationUtils.computeDistanceMiles(latitude1, longitude1, latitude2, longitude2);
  }

  public static int computeDistanceMiles(LatLongPair pair, GisMessage message) {
    return LocationUtils
        .computeDistanceMiles(pair.getLatitudeAsDouble(), pair.getLongitudeAsDouble(), //
            Double.parseDouble(message.latitude), Double.parseDouble(message.longitude));
  }

  public static int computeBearing(GisMessage m1, GisMessage m2) {
    var latitude1 = Double.parseDouble(m1.latitude);
    var longitude1 = Double.parseDouble(m1.longitude);

    var latitude2 = Double.parseDouble(m2.latitude);
    var longitude2 = Double.parseDouble(m2.longitude);

    return LocationUtils.computeBearing(latitude1, longitude1, latitude2, longitude2);
  }

}
