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

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DTO for latitude/longitude pair
 *
 * The "natural" type is the String, not double. The default precision is 4 decimal digits. This is 111 m at the
 * equator, etc. We are primarily interested in people's locations, and will be storing in text, CSV files.
 *
 * https://en.wikipedia.org/wiki/Decimal_degrees
 *
 *
 * @author bobt
 *
 */
public class LatLongPair {
  private static final Logger logger = LoggerFactory.getLogger(LatLongPair.class);

  public static final LatLongPair ZERO_ZERO = new LatLongPair(0, 0);
  public static final LatLongPair NORTH_POLE = new LatLongPair(90, 0);
  public static final LatLongPair SOUTH_POLE = new LatLongPair(-90, 0);
  public static final LatLongPair INVALID = new LatLongPair(null, null);

  private final String latitude;
  private final String longitude;

  private double dLatitude;
  private double dLongitude;

  private static int precision = 4;

  public LatLongPair(String latitude, String longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public LatLongPair(String grid) {
    this(String.valueOf(LocationUtils.getLatitudeFromMaidenhead(grid)),
        String.valueOf(LocationUtils.getLongitudeFromMaidenhead(grid)));
  }

  @Override
  public String toString() {
    return "lat:" + latitude + ", lon: " + longitude;
  }

  private static double centeredModulus(double dividend, double divisor) {
    double ret = dividend % divisor;
    if (ret <= 0) {
      ret += divisor;
    }
    if (ret > divisor / 2) {
      ret -= divisor;
    }
    return ret;
  }

  public static double normalizeLatitude(double lat) {
    lat = centeredModulus(lat, 360);
    if (lat < -90) {
      lat = -180 - lat;
    } else if (lat > 90) {
      lat = 180 - lat;
    }
    return lat;
  }

  public static double normalizeLongitude(double lon) {
    return centeredModulus(lon, 360);
  }

  /**
   * round a double to given number of places
   *
   * https://www.baeldung.com/java-round-decimal-number
   *
   * @param value
   * @param places
   * @return
   */
  private static String round(double value, int places) {
    var bd = new BigDecimal(value);
    bd = bd.setScale(places, RoundingMode.HALF_UP);
    return bd.toString();
  }

  public LatLongPair(double lat, double lon) {
    this(round(normalizeLatitude(lat), precision), round(normalizeLongitude(lon), precision));
  }

  public LatLongPair(LatLongPair other) {
    this(other.latitude, other.longitude);
  }

  public String getLatitude() {
    return latitude;
  }

  public String getLatitude(int decimalDigits) {
    if (decimalDigits < 0) {
      return getLatitude();
    }
    return round(normalizeLatitude(getLatitudeAsDouble()), decimalDigits);
  }

  public String getLongitude() {
    return longitude;
  }

  public String getLongitude(int decimalDigits) {
    if (decimalDigits < 0) {
      return getLongitude();
    }
    return round(normalizeLongitude(getLongitudeAsDouble()), decimalDigits);
  }

  public double getLatitudeAsDouble() {
    isValid();
    return dLatitude;
  }

  public double getLongitudeAsDouble() {
    isValid();
    return dLongitude;
  }

  public boolean isValidNotZero() {
    return isValid() && !getLatitude(0).equals("0") && !getLongitude(0).equals("0");
  }

  public boolean isValid() {
    if (latitude == null || longitude == null) {
      return false;
    }

    if (latitude.length() == 0 || longitude.length() == 0) {
      return false;
    }

    try {
      dLatitude = Double.parseDouble(latitude);
      if (Math.abs(dLatitude) > 90d) {
        return false;
      }
    } catch (Exception e) {
      logger.error("could not parse latitude from: " + latitude);
      return false;
    }

    try {
      dLongitude = Double.parseDouble(longitude);
      if (Math.abs(dLongitude) > 180d) {
        return false;
      }
    } catch (Exception e) {
      logger.error("could not parse longitude from: " + longitude);
      return false;
    }

    return true;
  }

  public int computeDistanceMiles(LatLongPair other) {
    return LocationUtils.computeDistanceMiles(this, other);
  }

  public double computeDistanceMeters(LatLongPair other) {
    return LocationUtils.computeDistanceMeters(this, other);
  }

  public static int getPrecision() {
    return precision;
  }

  public static void setPrecision(int precision) {
    LatLongPair.precision = precision;
  }

};
