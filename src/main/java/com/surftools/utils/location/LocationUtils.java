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

package com.surftools.utils.location;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * various static methods for location-based services
 *
 * @author bobt
 *
 */
public class LocationUtils {
  private static final Logger logger = LoggerFactory.getLogger(LocationUtils.class);

  // https://en.wikipedia.org/wiki/Great-circle_distance
  public static final double R_METERS = 6_371_009d;

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

    try {
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
    } catch (Exception e) {
      return "";
    }
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
   * compute the centroid or center-of-mass;
   *
   * @param list
   * @return
   */
  public static LatLongPair computeCentroid(List<LatLongPair> list) {
    if (list == null || list.size() == 0) {
      return new LatLongPair(0, 0);
    }

    if (list.size() == 1) {
      return new LatLongPair(list.get(0));
    }

    int n = 0;
    double sumLat = 0d;
    double sumLon = 0d;

    for (var pair : list) {
      if (pair == null) {
        continue;
      }

      ++n;
      sumLat += pair.getLatitudeAsDouble();
      sumLon += pair.getLongitudeAsDouble();
    }

    return new LatLongPair(sumLat / n, sumLon / n);
  }

  /**
   *
   * Returns the (x, y) coordinates on the unit circle for the given index. Uses binary angular subdivision: 0: 0° 1:
   * 180° 2: 90° 3: 270° 4: 45° 5: 135° 6: 225° 7: 315° 8+: recursively subdivide remaining arcs.
   *
   * @param index
   * @param center
   * @param distanceMeters
   * @return
   */

  public static LatLongPair binaryAngularSubdivision(int index, LatLongPair center, double distanceMeters) {
    if (index < 0) {
      throw new IllegalArgumentException("negative: " + index);
    }

    if (distanceMeters <= 0) {
      throw new IllegalArgumentException("non-positve distance:" + distanceMeters);
    }

    if (center == null) {
      center = new LatLongPair(0, 0);
    }

    var centerLat = center.getLatitudeAsDouble();
    var centerLon = center.getLongitudeAsDouble();

    var thetaDegrees = 0d;
    if (index == 1) {
      thetaDegrees = 180d;
    } else if (index == 2) {
      thetaDegrees = 90d;
    } else if (index == 3) {
      thetaDegrees = 270d;
    } else {
      // For index >= 4:
      // Determine which "layer" of binary subdivision this index belongs to.
      // Layer 0: 4 points (already handled)
      // Layer 1: 4 points (45°, 135°, 225°, 315°)
      // Layer 2: 8 points (22.5°, 67.5°, ...)
      // Layer 3: 16 points, etc.

      int layer = 1;
      int start = 4;
      int count = 4;

      while (index >= start + count) {
        start += count;
        count *= 2;
        layer++;
      }

      // Position inside this layer
      int pos = index - start;

      // Angle step for this layer
      double step = 360.0 / (4 * Math.pow(2, layer - 1));

      // First angle in this layer is step/2 offset from cardinal directions
      thetaDegrees = step / 2 + pos * step;
    }
    var thetaRadians = Math.toRadians(thetaDegrees);

    var lo = 0d;
    var hi = distanceMeters / 10_000d;
    var pair = (LatLongPair) null;
    var iterations = 0;
    var done = false;
    while (!done) {
      ++iterations;
      var mid = (lo + hi) / 2;
      var newLongitude = centerLon + (mid * Math.cos(thetaRadians));
      var newLatitude = centerLat + (mid * Math.sin(thetaRadians));
      pair = new LatLongPair(newLatitude, newLongitude);

      var computedDistance = computeDistanceMeters(center, pair);
      var delta = distanceMeters - computedDistance;
      if (Math.abs(delta) <= 10d || iterations >= 1000) {
        done = true;

        logger.debug("done, mid: " + mid + ", delta: " + delta + ", iterations: " + iterations);
        break;
      } else {
        logger.debug("iteration: " + iterations + ", delta: " + delta + ", lo: " + lo + ", hi: " + hi);
        if (computedDistance < distanceMeters) {
          lo = mid;
        } else {
          hi = mid;
        }
      }

    }

    return pair;
  }

  /**
   * return a list of LatLongPair points evenly spaced at a distance around the center
   *
   * @param n
   * @param center
   * @param distanceMeters
   * @return
   */
  public static List<LatLongPair> jitter(int n, LatLongPair center, double distanceMeters) {
    if (n <= 0) {
      throw new IllegalArgumentException("non-postive number of points: " + n);
    }

    if (distanceMeters <= 0) {
      throw new IllegalArgumentException("non-positve distance:" + distanceMeters);
    }

    if (center == null) {
      center = new LatLongPair(0, 0);
    }

    var list = new ArrayList<LatLongPair>(n);
    if (n == 1) {
      list.add(center);
      return list;
    }

    var centerLat = center.getLatitudeAsDouble();
    var centerLon = center.getLongitudeAsDouble();

    // if (centerLat > 89.6d || centerLat < -89.6d) {
    // // TODO fixme
    // throw new IllegalArgumentException("invalid latitude");
    // }

    for (int i = 0; i < n; ++i) {
      var thetaDegrees = 360d * i / n;
      var thetaRadians = Math.toRadians(thetaDegrees);
      var done = false;
      var lo = 0d;
      var hi = Math.PI * R_METERS;

      var iterations = 0;
      do {
        ++iterations;
        var mid = (lo + hi) / 2;
        var newLongitude = centerLon + (mid * Math.cos(thetaRadians));
        var newLatitude = centerLat + (mid * Math.sin(thetaRadians));
        var pair = new LatLongPair(newLatitude, newLongitude);

        var computedDistance = computeDistanceMeters(center, pair);
        var delta = distanceMeters - computedDistance;
        if (Math.abs(delta) <= 0.01 || iterations >= 1000) {
          done = true;
          list.add(pair);
          // logger.info("done, mid: " + mid + ", delta: " + delta + ", iterations:
          // " + iterations);
        } else {
          // logger.error("iteration: " + iterations + ", delta: " + delta + ", lo:
          // " + lo + ", hi: " + hi);
          if (computedDistance < distanceMeters) {
            lo = mid;
          } else {
            hi = mid;
          }
        }

      } while (!done);
    }

    return list;
  }

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

  public static double computeDistanceMeters(LatLongPair p1, LatLongPair p2) {
    return computeDistanceMeters(p1.getLatitudeAsDouble(), p1.getLongitudeAsDouble(), p2.getLatitudeAsDouble(),
        p2.getLongitudeAsDouble());
  }

  public static int computeDistanceMiles(double latitude1, double longitude1, double latitude2, double longitude2) {
    final double MILES_PER_METER = 0.000621371d;
    double distanceMeters = computeDistanceMeters(latitude1, longitude1, latitude2, longitude2);
    int distanceMiles = (int) Math.round(MILES_PER_METER * distanceMeters);
    return distanceMiles;
  }

  public static int computeDistanceMiles(LatLongPair p1, LatLongPair p2) {
    return computeDistanceMiles(p1.getLatitudeAsDouble(), p1.getLongitudeAsDouble(), p2.getLatitudeAsDouble(),
        p2.getLongitudeAsDouble());
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

  public static int computBearing(LatLongPair p1, LatLongPair p2) {
    return computeBearing(p1.getLatitudeAsDouble(), p1.getLongitudeAsDouble(), p2.getLatitudeAsDouble(),
        p2.getLongitudeAsDouble());
  }

  /**
   * convert strings representing lat/lon like 47-32.23N or 122-14.33W into decimal degree representation
   *
   * @param ddmm
   * @return
   */
  public static String convertToDecimalDegrees(String ddmm) {
    final var REDUNDANT_LAST_CHAR_SET = new HashSet<Character>(List.of('N', 'S', 'E', 'W'));

    if (ddmm == null || ddmm.length() == 0) {
      return "";
    } else {
      ddmm = ddmm.trim();
    }

    // it gets worse: 37.69250150N or -121.78913700W, but not 47-32.23N or
    // 122-14.33W
    var lastChar = ddmm.charAt(ddmm.length() - 1);
    if (REDUNDANT_LAST_CHAR_SET.contains(lastChar) && ((ddmm.startsWith("-")) || (ddmm.indexOf("-") == -1))) {
      ddmm = ddmm.substring(0, ddmm.length() - 1);
    }

    // apparently, sometimes, somehow, it's already in decimal degrees
    double decimalDegrees = 0d;
    try {
      decimalDegrees = Double.parseDouble(ddmm);
      return ddmm;
    } catch (Exception e) {
      ;
    }

    char direction = ddmm.charAt(ddmm.length() - 1);
    ddmm = ddmm.substring(0, ddmm.length() - 1);
    String[] fields = ddmm.split("-");
    if (fields == null || fields.length == 0 || fields[0].length() == 0) {
      return "";
    }
    double degrees = Double.parseDouble(fields[0]);
    double minutes = 0;
    try {
      minutes = Double.parseDouble(fields[1]);
    } catch (Exception e) {
      ;
    }
    decimalDegrees = degrees + (minutes / 60d);
    DecimalFormat df = new DecimalFormat("#.#####");
    df.setRoundingMode(RoundingMode.CEILING);
    String result = ((direction == 'S' || direction == 'W') ? "-" : "") + df.format(decimalDegrees);
    return result;
  }

}
