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

package com.surftools.wimp.processors.std.baseExercise;

import java.nio.file.Path;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * an Processor that combines all the "comments" fields from all the messages, etc.
 *
 * @author bobt
 *
 */
public class MultiMessageCommentProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(MultiMessageCommentProcessor.class);

  private int minMessageCount;
  private boolean joinWithNewLines;

  static record MMC(String call, LatLongPair location, int count, String comments) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", "Count", "Comments" };
    }

    @Override
    public String[] getValues() {
      var latitude = location == null ? "" : location.getLatitude();
      var longitude = location == null ? "" : location.getLongitude();
      return new String[] { call, latitude, longitude, String.valueOf(count), comments };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (MMC) other;
      return call.compareTo(o.call);
    }

  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    // TODO consider making these full configuration values if I ever use this processor
    minMessageCount = Integer.MAX_VALUE;
    joinWithNewLines = false;
  }

  @Override
  public void process() {
    var it = mm.getSenderIterator();
    var results = new ArrayList<IWritableTable>();
    while (it.hasNext()) {
      var call = it.next();
      var mapLocation = LatLongPair.ZERO_ZERO;
      var commentsList = new ArrayList<String>();
      var messageCountForSender = 0;
      var messageMap = mm.getMessagesForSender(call);
      for (var entry : messageMap.entrySet()) {
        var messageList = entry.getValue();
        messageCountForSender += messageList.size();
        for (var message : messageList) {
          if (!mapLocation.isValid() && message.mapLocation.isValid()) {
            mapLocation = message.mapLocation;
          }
          var mmc = message.getMultiMessageComment();
          if (mmc != null && !mmc.isBlank()) {
            commentsList.add(mmc);
          }
        }
      }

      if (messageCountForSender >= minMessageCount) {
        var comments = "";
        if (joinWithNewLines) {
          comments = String.join("\n", commentsList);
        } else {
          comments = String.join(",", commentsList);
        }
        var mmc = new MMC(call, mapLocation, messageCountForSender, comments);
        results.add(mmc);
      } else {
        logger.debug("skipping call: " + call + " because only " + messageCountForSender + " messages");
        continue;
      }
    } // end loop over senders

    WriteProcessor.writeTable(results, Path.of(outputPathName, "mmc.csv"));
  }

}
