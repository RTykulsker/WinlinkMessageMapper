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

package com.surftools.winlinkMessageMapper.processor;

import com.surftools.winlinkMessageMapper.dto.EtoPositionMessage;
import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.LatLongPair;
import com.surftools.winlinkMessageMapper.reject.MessageOrRejectionResult;
import com.surftools.winlinkMessageMapper.reject.RejectType;

public class EtoPositionProcessor extends AbstractBaseProcessor {

  @Override
  public MessageOrRejectionResult process(ExportedMessage message) {
    var mime = message.mime;

    String[] mimeLines = mime.split("\\n");
    String comments = "";
    LatLongPair latLongPair = null;
    boolean inComments = false;
    for (String line : mimeLines) {
      if (line.startsWith("Comments:")) {
        inComments = true;
        continue;
      }

      if (inComments) {
        if (line.startsWith("----------")) {
          break;
        } else {
          comments += comments + "\n";
        }
      }

      if (line.startsWith("GPS Coordinates: ")) {
        try {
          var fields = line.split(" ");
          latLongPair = new LatLongPair(fields[0], fields[1]);
          if (!latLongPair.isValid()) {
            return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_LATLONG, line);
          }
        } catch (Exception e) {
          return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_LATLONG, line);
        }
      }
    } // end loop over lines

    if (comments != null && comments.endsWith("\n")) {
      comments = comments.substring(0, comments.length() - 1);
    }

    EtoPositionMessage m = new EtoPositionMessage(message, //
        latLongPair.latitude(), latLongPair.longitude(), comments);
    return new MessageOrRejectionResult(m, null);
  }

}
