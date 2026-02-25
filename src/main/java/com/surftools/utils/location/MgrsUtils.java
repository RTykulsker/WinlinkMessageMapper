/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

import java.text.ParseException;

import mil.nga.grid.features.Point;
import mil.nga.mgrs.MGRS;

/**
 * see https://github.com/ngageoint/mgrs-java
 */

public class MgrsUtils {
  public static LatLongPair mgrsToLatLongPair(String arg) {
    MGRS mgrs;
    try {
      mgrs = MGRS.parse(arg);
    } catch (ParseException e) {
      throw new RuntimeException("couldn't parse MGRS: " + arg + ", " + e.getMessage());
    }
    Point point = mgrs.toPoint();
    LatLongPair pair = new LatLongPair(point.getLatitude(), point.getLongitude());
    return pair;
  }

  public static String latLongPairToMgrs(LatLongPair pair) {
    Point point = Point.point(pair.getLongitudeAsDouble(), pair.getLatitudeAsDouble());
    MGRS mgrs = MGRS.from(point);
    return mgrs.toString();
  }

  // ---------------------------------------------------------------------
  // VALIDATION TESTS
  // ---------------------------------------------------------------------

  private static void testRoundTrip(String mgrs) {
    try {
      LatLongPair pair = mgrsToLatLongPair(mgrs);
      String back = latLongPairToMgrs(pair);
      var backPair = mgrsToLatLongPair(back);
      var distanceMiles = LocationUtils.computeDistanceMiles(pair, backPair);
      if (distanceMiles <= 10) {
        System.out
            .println(String
                .format("MGRS: %-20s -> LatLon: %-30s -> Back: %s, distMiles: %s", mgrs, pair, back,
                    String.valueOf(distanceMiles)));
        System.out.flush();
      } else {
        System.err
            .println(String
                .format("MGRS: %-20s -> LatLon: %-30s -> Back: %s, distMiles: %s", mgrs, pair, back,
                    String.valueOf(distanceMiles)));
        System.err.flush();
      }
    } catch (Exception e) {
      System.out.println("Error for " + mgrs + ": " + e.getMessage());
    }
  }

  private static void testLatLon(double lat, double lon) {
    try {
      var pair = new LatLongPair(lat, lon);
      String mgrs = latLongPairToMgrs(pair);
      LatLongPair back = mgrsToLatLongPair(mgrs);
      var distMiles = LocationUtils.computeDistanceMiles(pair, back);
      if (distMiles <= 10) {
        System.out
            .printf("LatLon: (%.6f, %.6f) -> MGRS: %-15s -> Back: %s, distMiles: %d\n", lat, lon, mgrs, back,
                distMiles);
        System.out.flush();
      } else {
        System.err
            .printf("LatLon: (%.6f, %.6f) -> MGRS: %-15s -> Back: %s, distMiles: %d\n", lat, lon, mgrs, back,
                distMiles);
        System.err.flush();

      }
    } catch (Exception e) {
      System.out.println("Error for LatLon test: " + e.getMessage());
    }
  }

  public static void main(String[] args) {

    System.out.println("=== MGRS → Lat/Lon → MGRS Round‑Trip Tests ===");

    // Known test cases (you can add your own)
    testRoundTrip("10TET5728565159");
    testRoundTrip("10TET5765");
    testRoundTrip("10TET1234567890"); // 15‑char
    testRoundTrip("10TET1234578"); // 9‑char
    testRoundTrip("33UXP04"); // 7‑char
    testRoundTrip("33UXP0404"); // 9‑char
    testRoundTrip("18SUJ22850705"); // 13‑char
    testRoundTrip("60HVD2624");

    System.out.println("\n=== Lat/Lon → MGRS → Lat/Lon Tests (CONUS) ===");

    double[][] conus = { //
        { 47.6062, -122.3321 }, // Seattle
        { 34.0522, -118.2437 }, // LA
        { 40.7128, -74.0060 }, // NYC
        { 39.7392, -104.9903 }, // Denver
        { 25.7617, -80.1918 }, // Miami
        { -37.728167, 176.124000 } // NZ
    };

    for (double[] s : conus) {
      testLatLon(s[0], s[1]);
      System.out.println();
    }
  }
}
