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

package com.surftools.wimp.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.PositionMessage;

public class PositionParser extends AbstractBaseParser {
  private static final Logger logger = LoggerFactory.getLogger(PositionParser.class);

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    var mime = message.plainContent;
    String[] mimeLines = mime.split("\\n");

    var latitude = LocationUtils.convertToDecimalDegrees(getStringFromMime("Latitude: ", mimeLines));
    var longitude = LocationUtils.convertToDecimalDegrees(getStringFromMime("Longitude: ", mimeLines));
    var comments = getComments(mimeLines);

    if (latitude.length() == 0 || longitude.length() == 0) {
      logger
          .warn("### From: " + message.from + ", mId: " + message.messageId //
              + ", can't parse lat/lon, lat: " + latitude + ", lon: " + longitude);
    }

    PositionMessage m = new PositionMessage(message, latitude, longitude, comments);
    return m;
  }

  private String getStringFromMime(String needle, String[] mimeLines) {
    for (String line : mimeLines) {
      if (!line.startsWith(needle)) {
        continue;
      }

      String result = line.substring(line.indexOf(": ") + 2);
      return result;
    }
    return null;
  }

  public String getComments(String[] mimeLines) {
    String comments = "";
    boolean isFound = false;
    for (String line : mimeLines) {
      if (!isFound) {
        if (line.startsWith("Comment: ") || line.startsWith("Comments: ")) {
          isFound = true;
          line = line.substring(line.indexOf(" ") + 1);
        }
      }
      if (isFound) {
        if (line.endsWith("=")) {
          comments += line.substring(0, line.length() - 1);
        } else {
          comments += line;
          break;
        }
      }
    }
    return comments;
  }

}
