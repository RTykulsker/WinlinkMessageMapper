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

import java.util.Locale;

/**
 * Full Veness‑faithful UTM/MGRS converter (A3 variant): - WGS84 only - No UPS (polar) - No Norway/Svalbard special
 * zones - Supports all valid MGRS lengths (5,7,9,11,13,15) - Auto precision detection
 *
 * see: https://github.com/chrisveness/geodesy
 */

public class MgrsUtils {

  // ---------------------------------------------------------------------
  // UTM implementation (WGS84) — faithful Veness port (minus UPS + special zones)
  // UTM -> Universal Transverse Mercator -- a coordinate system
  // ---------------------------------------------------------------------

  private static record Utm(int zone, char hemisphere, double easting, double northing) {

    // WGS84 constants
    private static final double A = 6378137.0;
    private static final double F = 1 / 298.257223563;
    private static final double K0 = 0.9996;
    private static final double E2 = F * (2 - F);
    private static final double EP2 = E2 / (1 - E2);

    // -------------------------------------------------------------
    // Lat/Lon → UTM
    // -------------------------------------------------------------
    static Utm fromLatLon(double latDeg, double lonDeg) {
      if (latDeg < -80 || latDeg > 84) {
        throw new IllegalArgumentException("Latitude out of UTM/MGRS range");
      }

      double lat = Math.toRadians(latDeg);
      double lon = Math.toRadians(lonDeg);

      int zone = (int) Math.floor((lonDeg + 180) / 6) + 1;
      double lambda0 = Math.toRadians(-183 + 6 * zone);

      double sinLat = Math.sin(lat);
      double cosLat = Math.cos(lat);
      double tanLat = Math.tan(lat);

      double n = A / Math.sqrt(1 - E2 * sinLat * sinLat);
      double t = tanLat * tanLat;
      double c = EP2 * cosLat * cosLat;
      double a = (lon - lambda0) * cosLat;

      double m = A * ((1 - E2 / 4 - 3 * E2 * E2 / 64 - 5 * Math.pow(E2, 3) / 256) * lat
          - (3 * E2 / 8 + 3 * E2 * E2 / 32 + 45 * Math.pow(E2, 3) / 1024) * Math.sin(2 * lat)
          + (15 * E2 * E2 / 256 + 45 * Math.pow(E2, 3) / 1024) * Math.sin(4 * lat)
          - (35 * Math.pow(E2, 3) / 3072) * Math.sin(6 * lat));

      double easting = K0 * n
          * (a + (1 - t + c) * Math.pow(a, 3) / 6 + (5 - 18 * t + t * t + 72 * c - 58 * EP2) * Math.pow(a, 5) / 120)
          + 500000;

      double northing = K0 * (m + n * tanLat * (a * a / 2 + (5 - t + 9 * c + 4 * c * c) * Math.pow(a, 4) / 24
          + (61 - 58 * t + t * t + 600 * c - 330 * EP2) * Math.pow(a, 6) / 720));

      char hemi = latDeg >= 0 ? 'N' : 'S';
      if (latDeg < 0) {
        northing += 10000000;
      }

      return new Utm(zone, hemi, easting, northing);
    }

    // -------------------------------------------------------------
    // UTM → Lat/Lon
    // -------------------------------------------------------------
    LatLongPair toLatLongPair() {
      double x = easting - 500000;
      double y = northing;
      if (hemisphere == 'S') {
        y -= 10000000;
      }

      double lambda0 = Math.toRadians(-183 + 6 * zone);

      double m = y / K0;
      double mu = m / (A * (1 - E2 / 4 - 3 * E2 * E2 / 64 - 5 * Math.pow(E2, 3) / 256));

      double e1 = (1 - Math.sqrt(1 - E2)) / (1 + Math.sqrt(1 - E2));

      double j1 = 3 * e1 / 2 - 27 * Math.pow(e1, 3) / 32;
      double j2 = 21 * e1 * e1 / 16 - 55 * Math.pow(e1, 4) / 32;
      double j3 = 151 * Math.pow(e1, 3) / 96;
      double j4 = 1097 * Math.pow(e1, 4) / 512;

      double fp = mu + j1 * Math.sin(2 * mu) + j2 * Math.sin(4 * mu) + j3 * Math.sin(6 * mu) + j4 * Math.sin(8 * mu);

      double sinFp = Math.sin(fp);
      double cosFp = Math.cos(fp);
      double tanFp = Math.tan(fp);

      double c1 = EP2 * cosFp * cosFp;
      double t1 = tanFp * tanFp;
      double n1 = A / Math.sqrt(1 - E2 * sinFp * sinFp);
      double r1 = A * (1 - E2) / Math.pow(1 - E2 * sinFp * sinFp, 1.5);
      double d = x / (n1 * K0);

      double lat = fp
          - (n1 * tanFp / r1) * (d * d / 2 - (5 + 3 * t1 + 10 * c1 - 4 * c1 * c1 - 9 * EP2) * Math.pow(d, 4) / 24
              + (61 + 90 * t1 + 298 * c1 + 45 * t1 * t1 - 252 * EP2 - 3 * c1 * c1) * Math.pow(d, 6) / 720);

      double lon = lambda0 + (d - (1 + 2 * t1 + c1) * Math.pow(d, 3) / 6
          + (5 - 2 * c1 + 28 * t1 - 3 * c1 * c1 + 8 * EP2 + 24 * t1 * t1) * Math.pow(d, 5) / 120) / cosFp;

      return new LatLongPair(Math.toDegrees(lat), Math.toDegrees(lon));
    }
  }

  // ---------------------------------------------------------------------
  // MGRS implementation — faithful Veness port (minus UPS + special zones)
  // ---------------------------------------------------------------------
  private static record Mgrs(int zone, char band, char e100k, char n100k, String easting, String northing) {
    @Override
    public String toString() {
      return "" + zone + band + e100k + n100k + easting + northing;
    }

    // Veness tables
    private static final String LAT_BANDS = "CDEFGHJKLMNPQRSTUVWX";

    // 100km easting letters (3 sets of 8)
    private static final String[] E100K = { "ABCDEFGH", "JKLMNPQR", "STUVWXYZ" };

    // 100km northing letters (2 alternating sets)
    private static final String[] N100K = { "ABCDEFGHJKLMNPQRSTUV", // even zones
        "FGHJKLMNPQRSTUVABCDE" // odd zones
    };

    // -------------------------------------------------------------
    // Parsing
    // -------------------------------------------------------------
    static Mgrs parse(String mgrs) {
      if (mgrs == null) {
        throw new IllegalArgumentException("MGRS is null");
      }

      mgrs = mgrs.trim().toUpperCase().replaceAll("\\s+", "");
      if (mgrs.length() < 5) {
        throw new IllegalArgumentException("MGRS too short");
      }

      // zone (1–2 digits)
      int i = 0;
      while (i < mgrs.length() && Character.isDigit(mgrs.charAt(i))) {
        i++;
      }

      if (i == 0 || i > 2) {
        throw new IllegalArgumentException("Invalid MGRS zone");
      }

      int zone = Integer.parseInt(mgrs.substring(0, i));
      if (zone < 1 || zone > 60) {
        throw new IllegalArgumentException("Zone out of range");
      }

      if (i + 3 > mgrs.length()) {
        throw new IllegalArgumentException("Incomplete MGRS (missing band/grid)");
      }

      char band = mgrs.charAt(i++);
      if (LAT_BANDS.indexOf(band) == -1) {
        throw new IllegalArgumentException("Invalid latitude band: " + band);
      }

      char e100k = mgrs.charAt(i++);
      char n100k = mgrs.charAt(i++);

      // numeric part
      String remainder = (i < mgrs.length()) ? mgrs.substring(i) : "";
      if (!remainder.isEmpty()) {
        if (!remainder.matches("\\d+")) {
          throw new IllegalArgumentException("Numeric part must be digits");
        }

        if (remainder.length() % 2 != 0) {
          throw new IllegalArgumentException("Numeric part must be even length");
        }

        if (remainder.length() > 10) {
          throw new IllegalArgumentException("Max 5 digits per coordinate");
        }

      }

      String e = "";
      String n = "";
      if (!remainder.isEmpty()) {
        int half = remainder.length() / 2;
        e = remainder.substring(0, half);
        n = remainder.substring(half);
      }

      return new Mgrs(zone, band, e100k, n100k, e, n);
    }

    // -------------------------------------------------------------
    // MGRS → UTM
    // -------------------------------------------------------------
    Utm toUtm() {
      int zone = this.zone;

      // Easting 100km column
      int eSet = (zone - 1) % 3;
      String eCols = E100K[eSet];
      int col = eCols.indexOf(e100k);
      if (col == -1) {
        throw new IllegalArgumentException("Invalid e100k letter: " + e100k);
      }

      // Northing 100km row
      int nSet = (zone - 1) % 2;
      String nRows = N100K[nSet];
      int row = nRows.indexOf(n100k);
      if (row == -1) {
        throw new IllegalArgumentException("Invalid n100k letter: " + n100k);
      }

      double easting = (col + 1) * 100000;
      double northing = row * 100000;

      // numeric precision
      int digits = eastingDigits();
      if (digits > 0) {
        int scale = (int) Math.pow(10, 5 - digits);
        easting += Integer.parseInt(this.easting) * scale;
        northing += Integer.parseInt(this.northing) * scale;
      }

      boolean northHem = band >= 'N';

      // Fix northing cycle using band
      double latBand = (LAT_BANDS.indexOf(band) - 10) * 8.0;
      double minNorth = Utm.fromLatLon(latBand, (zone - 1) * 6 - 180 + 3).northing;

      while (northing < minNorth) {
        northing += 2000000;
      }

      if (!northHem) {
        northing += 10000000;
      }

      return new Utm(zone, northHem ? 'N' : 'S', easting, northing);
    }

    private int eastingDigits() {
      return easting.length();
    }

    // -------------------------------------------------------------
    // UTM → MGRS
    // -------------------------------------------------------------
    static Mgrs fromUtm(Utm utm, int digits) {
      int zone = utm.zone;

      // latitude band
      LatLongPair ll = utm.toLatLongPair();
      double lat = ll.getLatitudeAsDouble();
      char band = latToBand(lat);

      // easting 100km letter
      int eSet = (zone - 1) % 3;
      String eCols = E100K[eSet];
      int col = (int) Math.floor(utm.easting / 100000) - 1;
      if (col < 0) {
        col = 0;
      }

      if (col > 7) {
        col = 7;
      }

      char e100k = eCols.charAt(col);

      // northing 100km letter
      int nSet = (zone - 1) % 2;
      String nRows = N100K[nSet];
      int row = (int) Math.floor(utm.northing / 100000) % 20;
      if (row < 0) {
        row += 20;
      }

      char n100k = nRows.charAt(row);

      // relative offsets
      double eRel = utm.easting - (col + 1) * 100000;
      double nRel = utm.northing;
      while (nRel >= 2000000) {
        nRel -= 2000000;
      }
      nRel -= row * 100000;

      String eStr = "";
      String nStr = "";
      if (digits > 0) {
        double scale = Math.pow(10, 5 - digits);
        int eRounded = (int) Math.round(eRel / scale);
        int nRounded = (int) Math.round(nRel / scale);
        eStr = String.format(Locale.US, "%0" + digits + "d", eRounded);
        nStr = String.format(Locale.US, "%0" + digits + "d", nRounded);
      }

      return new Mgrs(zone, band, e100k, n100k, eStr, nStr);
    }

    private static char latToBand(double lat) {
      if (lat <= -80) {
        return 'C';
      }

      if (lat >= 84) {
        return 'X';
      }

      int i = (int) Math.floor((lat + 80) / 8);
      return LAT_BANDS.charAt(i);
    }
  }

  /**
   * Convert an MGRS string (any valid length: 5,7,9,11,13,15) to WGS84 Lat/Lon.
   */
  public static LatLongPair mgrsToLatLongPair(String mgrs) {
    Mgrs m = Mgrs.parse(mgrs);
    Utm utm = m.toUtm();
    return utm.toLatLongPair();
  }

  /**
   * Convert WGS84 Lat/Lon to MGRS.
   *
   * @param latLongPair
   *          latitude, longitude in degrees
   * @param digitsPerCoord
   *          0–5 digits per coordinate (0=100km, 5=1m)
   */
  public static String latLongPairToMgrs(LatLongPair pair, int digitsPerCoord) {
    if (digitsPerCoord < 0 || digitsPerCoord > 5) {
      throw new IllegalArgumentException("digitsPerCoord must be 0–5");
    }

    Utm utm = Utm.fromLatLon(pair.getLatitudeAsDouble(), pair.getLongitudeAsDouble());
    Mgrs m = Mgrs.fromUtm(utm, digitsPerCoord);
    return m.toString();
  }

  // ---------------------------------------------------------------------
  // VALIDATION TESTS
  // ---------------------------------------------------------------------
  //
  // private static void testRoundTrip(String mgrs) {
  // try {
  // LatLongPair pair = mgrsToLatLongPair(mgrs);
  // String back = latLongPairToMgrs(pair, mgrsPrecision(mgrs));
  // System.out.printf("MGRS: %-20s -> LatLon: %-30s -> Back: %s%n", mgrs, pair, back);
  // } catch (Exception e) {
  // System.out.println("Error for " + mgrs + ": " + e.getMessage());
  // }
  // }
  //
  // private static int mgrsPrecision(String mgrs) {
  // mgrs = mgrs.trim().replaceAll("\\s+", "");
  // int i = 0;
  // while (i < mgrs.length() && Character.isDigit(mgrs.charAt(i))) {
  // i++;
  // }
  //
  // int numericLen = mgrs.length() - (i + 3);
  // if (numericLen < 0) {
  // return 0;
  // }
  //
  // return numericLen / 2;
  // }
  //
  // private static void testLatLon(double lat, double lon, int digits) {
  // try {
  // var pair = new LatLongPair(lat, lon);
  // String mgrs = latLongPairToMgrs(pair, digits);
  // LatLongPair back = mgrsToLatLongPair(mgrs);
  // System.out.printf("LatLon: (%.6f, %.6f) -> MGRS: %-15s -> Back: %s%n", lat, lon, mgrs, back);
  // } catch (Exception e) {
  // System.out.println("Error for LatLon test: " + e.getMessage());
  // }
  // }
  //
  // ---------------------------------------------------------------------
  // MAIN — validation tests
  // ---------------------------------------------------------------------
  //
  // public static void main(String[] args) {
  //
  // System.out.println("=== MGRS → Lat/Lon → MGRS Round‑Trip Tests ===");
  //
  // // Known test cases (you can add your own)
  // testRoundTrip("10TET5728565159");
  // testRoundTrip("10TET5765");
  // testRoundTrip("10TET1234567890"); // 15‑char
  // testRoundTrip("10TET1234578"); // 9‑char
  // testRoundTrip("33UXP04"); // 7‑char
  // testRoundTrip("33UXP0404"); // 9‑char
  // testRoundTrip("18SUJ22850705"); // 13‑char
  //
  // System.out.println("\n=== Lat/Lon → MGRS → Lat/Lon Tests (CONUS) ===");
  //
  // double[][] conus = { //
  // { 47.6062, -122.3321 }, // Seattle
  // { 34.0522, -118.2437 }, // LA
  // { 40.7128, -74.0060 }, // NYC
  // { 39.7392, -104.9903 }, // Denver
  // { 25.7617, -80.1918 } // Miami
  // };
  //
  // int[] precisions = { 2, 3, 5 };
  //
  // for (double[] s : conus) {
  // for (int p : precisions) {
  // testLatLon(s[0], s[1], p);
  // }
  // System.out.println();
  // }
  // }
}
