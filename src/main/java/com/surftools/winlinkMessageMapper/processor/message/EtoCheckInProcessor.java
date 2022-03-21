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

package com.surftools.winlinkMessageMapper.processor.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.EtoCheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.LatLongPair;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

public class EtoCheckInProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(EtoCheckInProcessor.class);

  @Override
  public ExportedMessage process(ExportedMessage message) {
    if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
      logger.info("exportedMessage: " + message);
    }

    String[] mimeLines = message.plainContent.split("\\n");
    var latLongString = getStringFromFormLines(mimeLines, ":", "GPS Coordinates");
    LatLongPair latLong = null;
    if (latLongString != null) {
      String[] fields = latLongString.split(" ");
      latLong = new LatLongPair(findLatLong("LAT", fields), findLatLong("LON", fields));
      if (latLong == null || !latLong.isValid()) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, latLongString);
      }
    } else {
      return reject(message, RejectType.CANT_PARSE_LATLONG, message.mime);
    }

    var status = getStringFromFormLines(mimeLines, ":", "Status");
    var band = getStringFromFormLines(mimeLines, ":", "Band Used");
    var mode = getStringFromFormLines(mimeLines, ":", "Session Type");
    var comments = getComments(mimeLines);
    if (comments.endsWith("\n")) {
      comments = comments.substring(0, comments.length() - 1);
    }

    var version = "";
    var versionString = getStringFromFormLines(mimeLines, ":", "Version");
    if (versionString != null) {
      var fields = versionString.split(" ");
      version = fields[fields.length - 1];
    }

    EtoCheckInMessage m = new EtoCheckInMessage(message, latLong.latitude(), latLong.longitude(), "", //
        comments, status, band, mode, version, MessageType.CHECK_IN);
    return m;
  }

  /**
   * return first non-empty String after key
   *
   * @param key
   * @param fields
   * @return
   */
  private String findLatLong(String key, String[] fields) {
    int n = fields.length;
    boolean isKeyFound = false;
    for (int i = 0; i < n; ++i) {
      String field = fields[i].trim();

      if (field.length() > 0) {
        char c = field.charAt(0);
        if (!Character.isLetterOrDigit(c) && c != '-') {
          field = field.substring(1);
        }
      }

      if (field.equals(key)) {
        isKeyFound = true;
        continue;
      }

      if (!isKeyFound) {
        continue;
      }

      field = field.trim();
      if (field.isEmpty()) {
        continue;
      }

      if (field.endsWith(",")) {
        field = field.substring(0, field.length() - 1);
      }

      int index = field.indexOf(".");
      if (index == -1) {
        field = field.replaceAll(",", ".");
        field = field.replaceAll("'", ".");
      }

      return field;
    }
    return null;
  }

  private String getComments(String[] mimeLines) {
    final var boundary = "----------";
    boolean inComment = false;
    StringBuilder sb = new StringBuilder();
    for (String line : mimeLines) {
      if (line.startsWith("Comments:")) {
        inComment = true;
        continue;
      }

      if (inComment) {
        if (line.startsWith(boundary)) {
          break;
        }
        line = line.trim();
        if (line.length() > 0) {
          sb.append(line);
          sb.append("\n");
        }
      }
    }
    return sb.toString();
  }

}
