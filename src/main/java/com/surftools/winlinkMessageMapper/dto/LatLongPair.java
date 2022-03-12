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

package com.surftools.winlinkMessageMapper.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * record DTO for latitude/longitude pair
 *
 * @author bobt
 *
 */
public record LatLongPair(String latitude, String longitude) {
  private static final Logger logger = LoggerFactory.getLogger(LatLongPair.class);

  public boolean isValid() {
    if (latitude == null || longitude == null) {
      return false;
    }

    if (latitude.length() == 0 || longitude.length() == 0) {
      return false;
    }

    try {
      Double.parseDouble(latitude);
    } catch (Exception e) {
      logger.error("could not parse latitude from: " + latitude);
      return false;
    }

    try {
      Double.parseDouble(longitude);
    } catch (Exception e) {
      logger.error("could not parse longitude from: " + longitude);
      return false;
    }

    return true;
  }
};
