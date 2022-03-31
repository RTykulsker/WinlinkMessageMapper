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

package com.surftools.winlinkMessageMapper.dto.other;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * record DTO for latitude/longitude pair
 *
 * @author bobt
 *
 */
public class LatLongPair {
  private static final Logger logger = LoggerFactory.getLogger(LatLongPair.class);

  private final String latitude;
  private final String longitude;

  private double dLatitude;
  private double dLongitude;

  public LatLongPair(String latitude, String longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public LatLongPair(double lat, double lon) {
    this(String.valueOf(lat), String.valueOf(lon));
  }

  public LatLongPair(LatLongPair other) {
    this.latitude = other.latitude;
    this.longitude = other.longitude;
  }

  @Override
  public String toString() {
    return "{lat: " + latitude + ", lon:" + longitude + "}";
  }

  public String getLatitude() {
    return latitude;
  }

  public String getLongitude() {
    return longitude;
  }

  public double getLatitudeAsDouble() {
    isValid();
    return dLatitude;
  }

  public double getLongitudeAsDouble() {
    isValid();
    return dLongitude;
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
    final double MILES_PER_METER = 0.000621371;
    double distanceMeters = computeDistanceMeters(other);
    double distanceMiles = distanceMeters * MILES_PER_METER;
    return (int) Math.round(distanceMiles);
  }

  public double computeDistanceMeters(LatLongPair other) {
    // https://gist.github.com/vananth22/888ed9a22105670e7a4092bdcf0d72e4
    final double R_METERS = 6_371_000d;
    double lat1 = (Math.PI / 180d) * Double.parseDouble(this.latitude);
    double lon1 = (Math.PI / 180d) * Double.parseDouble(this.longitude);
    double lat2 = (Math.PI / 180d) * Double.parseDouble(other.latitude);
    double lon2 = (Math.PI / 180d) * Double.parseDouble(other.longitude);

    double latDistance = lat2 - lat1;
    double lonDistance = lon2 - lon1;
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(lat1) * Math.cos(lat2) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R_METERS * c;
  }

};
