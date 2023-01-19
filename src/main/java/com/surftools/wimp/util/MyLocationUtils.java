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

package com.surftools.wimp.util;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.message.ExportedMessage;

/**
 * used as a wrapped around the @{LocationUtils}
 *
 * @author bobt
 *
 */
public class MyLocationUtils {

  public static int computeDistanceMiles(ExportedMessage m1, ExportedMessage m2) {
    var latitude1 = Double.parseDouble(m1.mapLocation.getLatitude());
    var longitude1 = Double.parseDouble(m1.mapLocation.getLongitude());

    var latitude2 = Double.parseDouble(m2.mapLocation.getLatitude());
    var longitude2 = Double.parseDouble(m2.mapLocation.getLongitude());

    return LocationUtils.computeDistanceMiles(latitude1, longitude1, latitude2, longitude2);
  }

  public static int computeDistanceMiles(LatLongPair pair, ExportedMessage message) {
    return LocationUtils
        .computeDistanceMiles(pair.getLatitudeAsDouble(), pair.getLongitudeAsDouble(), //
            Double.parseDouble(message.mapLocation.getLatitude()),
            Double.parseDouble(message.mapLocation.getLongitude()));
  }

  public static int computeBearing(ExportedMessage m1, ExportedMessage m2) {
    var latitude1 = Double.parseDouble(m1.mapLocation.getLatitude());
    var longitude1 = Double.parseDouble(m1.mapLocation.getLongitude());

    var latitude2 = Double.parseDouble(m2.mapLocation.getLatitude());
    var longitude2 = Double.parseDouble(m2.mapLocation.getLongitude());

    return LocationUtils.computeBearing(latitude1, longitude1, latitude2, longitude2);
  }

  public static LatLongPair getPairFromGrid(String gridSquare) {
    var latitude = LocationUtils.getLatitudeFromMaidenhead(gridSquare);
    var longitude = LocationUtils.getLongitudeFromMaidenhead(gridSquare);
    var pair = new LatLongPair(latitude, longitude);
    return pair;
  }

  public static boolean isValid(LatLongPair pair) {
    var latitudeErrors = LocationUtils.validateLatLon(pair.getLatitude(), true);
    var longitudeErrors = LocationUtils.validateLatLon(pair.getLongitude(), false);
    return latitudeErrors.length() == 0 && longitudeErrors.length() == 0;
  }
}
