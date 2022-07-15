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

import com.surftools.winlinkMessageMapper.dto.message.GisMessage;

public class LocationUtils {

  public static boolean isValidMaidenhead(String grid) {
    if (grid == null) {
      return false;
    }

    // final var pattern = "[a-rA-R]{2}[0-9]{2}[a-xA-X]{2}[0-9]{2}[a-xA-X]{2}";
    final var pattern = "[a-rA-R]{2}[0-9]{2}[a-xA-X]{2}";
    return grid.matches(pattern);
  }

  public static double getLatitudeFromMaidenhead(String grid) {
    if (!isValidMaidenhead(grid)) {
      throw new IllegalArgumentException("grid: " + grid + " is not a valid Maidenhead grid string");
    }

    grid = grid.toUpperCase();
    double latitude = -90 + 10 * (grid.charAt(1) - 'A') + (grid.charAt(3) - '0') + 2.5 / 60 * (grid.charAt(5) - 'A')
        + 2.5 / 60 / 2;
    return latitude;
  }

  public static double getLongitudeFromMaidenhead(String grid) {
    grid = grid.toUpperCase();
    double longitude = -180 + 20 * (grid.charAt(0) - 'A') + 2 * (grid.charAt(2) - '0')
        + 5.0 / 60 * (grid.charAt(4) - 'A') + 5.0 / 60 / 2;
    return longitude;
  }

  public static String computeGrid(double lat, double lon) {
    final var UPPER = "ABCDEFGHIJKLMNOPQRSTUVWX";
    final var LOWER = "abcdefghijklmnopqrstuvwx";

    var adjustedLon = lon + 180d;
    var adjustedLat = lat + 90d;

    var c1 = UPPER.charAt((int) adjustedLon / 20);
    var c2 = UPPER.charAt((int) adjustedLat / 10);

    var c3 = (int) ((adjustedLon / 2) % 10);
    var c4 = (int) (adjustedLat % 10);

    var remainderLon = (adjustedLon - (int) (adjustedLon / 2) * 2) * 60;
    var remainderLat = (adjustedLat - (int) adjustedLat) * 60;

    var c5 = LOWER.charAt((int) (remainderLon / 5));
    var c6 = LOWER.charAt((int) (remainderLat / 2.5));

    var sb = new StringBuilder().append(c1).append(c2).append(c3).append(c4).append(c5).append(c6);
    return sb.toString();
  }

  public static String validateLatLon(String latlong, boolean isLat) {
    var ll = (isLat) ? "latitude" : "longitude";

    if (latlong == null) {
      return "missing " + ll + " query parameter\n";
    }

    double d = 0d;
    try {
      d = Double.parseDouble(latlong);
    } catch (Exception e) {
      return "can't parse " + ll + " query parameter: " + e.getLocalizedMessage() + "\n";
    }

    if (isLat) {
      if (d < -90d || d > 90d) {
        return "latitude(" + d + ") must be between -90 and 90 degrees\n";
      }
    } else {
      if (d < -180d || d > 180d) {
        return "longitude(" + d + ") must be between -180 and 180 degrees\n";
      }
    } // end if longitude
    return "";
  } // end validateLatLon

  /**
   * see: https://gist.github.com/vananth22/888ed9a22105670e7a4092bdcf0d72e4
   *
   * @param latitude1
   * @param longitude1
   * @param latitude2
   * @param longitude2
   * @return
   */
  public static double computeDistanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
    final double R_METERS = 6_371_000d;
    double lat1 = (Math.PI / 180d) * latitude1;
    double lon1 = (Math.PI / 180d) * longitude1;
    double lat2 = (Math.PI / 180d) * latitude2;
    double lon2 = (Math.PI / 180d) * longitude2;

    double latDistance = lat2 - lat1;
    double lonDistance = lon2 - lon1;
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    double distanceMeters = R_METERS * c;
    return distanceMeters;
  }

  public static int computeDistanceMiles(GisMessage m1, GisMessage m2) {
    var latitude1 = Double.parseDouble(m1.latitude);
    var longitude1 = Double.parseDouble(m1.longitude);

    var latitude2 = Double.parseDouble(m2.latitude);
    var longitude2 = Double.parseDouble(m2.longitude);

    return computeDistanceMiles(latitude1, longitude1, latitude2, longitude2);
  }

  public static int computeDistanceMiles(double latitude1, double longitude1, double latitude2, double longitude2) {
    final double MILES_PER_METER = 0.000621371d;
    double distanceMeters = computeDistanceMeters(latitude1, longitude1, latitude2, longitude2);
    int distanceMiles = (int) Math.round(MILES_PER_METER * distanceMeters);
    return distanceMiles;
  }

  /**
   * see https://stackoverflow.com/questions/9457988/bearing-from-one-coordinate-to-another
   *
   * @param lat1
   * @param lon1
   * @param lat2
   * @param lon2
   * @return
   */
  public static int computeBearing(double lat1, double lon1, double lat2, double lon2) {
    double longitude1 = lon1;
    double longitude2 = lon2;
    double latitude1 = Math.toRadians(lat1);
    double latitude2 = Math.toRadians(lat2);
    double longDiff = Math.toRadians(longitude2 - longitude1);
    double y = Math.sin(longDiff) * Math.cos(latitude2);
    double x = Math.cos(latitude1) * Math.sin(latitude2)
        - Math.sin(latitude1) * Math.cos(latitude2) * Math.cos(longDiff);

    double bearing = (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    int intBearing = (int) Math.round(bearing);
    return intBearing;
  }

  public static int computeBearing(GisMessage m1, GisMessage m2) {
    var latitude1 = Double.parseDouble(m1.latitude);
    var longitude1 = Double.parseDouble(m1.longitude);

    var latitude2 = Double.parseDouble(m2.latitude);
    var longitude2 = Double.parseDouble(m2.longitude);

    return computeBearing(latitude1, longitude1, latitude2, longitude2);
  }
}
