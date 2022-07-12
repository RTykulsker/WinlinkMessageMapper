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

package com.surftools.winlinkMessageMapper.multiMessageMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class SimpleMultiMessageCommentProcessor extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(SimpleMultiMessageCommentProcessor.class);

  protected final Map<String, MultiMessageComment> mmcMessageMap;

  private static final String CONFIG_KEY_MIN_MESSAGE_COUNT = "minMessageCount";
  private int minMessageCount = Integer.MAX_VALUE;

  @SuppressWarnings("unchecked")
  public SimpleMultiMessageCommentProcessor(String mmCommentKey) {
    super(logger);
    super.setOutputFileName("mmComments.csv");
    mmcMessageMap = new HashMap<>();

    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

      var jsonMap = mapper.readValue(mmCommentKey, Map.class);
      minMessageCount = (int) jsonMap.getOrDefault(CONFIG_KEY_MIN_MESSAGE_COUNT, Integer.MAX_VALUE);
    } catch (Exception e) {
      logger.error("could not parse mmCommentKey: " + mmCommentKey + ", " + e.getLocalizedMessage());
    }
  }

  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap, String pathName) {
    super.aggregate(messageMap);

    for (var call : fromListMap.keySet()) {
      var messageList = fromListMap.get(call);
      if (messageList.size() < minMessageCount) {
        logger.debug("skipping call: " + call + " because only " + messageList.size() + " messages");
        continue;
      }

      var aggregateMessage = aggregateMessageMap.get(call);
      var latitude = (String) aggregateMessage.data().get(KEY_AVERAGE_LATITUDE);
      var longitude = (String) aggregateMessage.data().get(KEY_AVERAGE_LONGITUDE);
      var count = String.valueOf(messageList.size());

      var sb = new StringBuilder();
      for (ExportedMessage exportedMessage : fromListMap.get(call)) {
        var comments = exportedMessage.getMultiMessageComment();
        comments = comments == null ? "" : comments;
        comments = comments.replaceAll("\n", ",");
        sb.append(exportedMessage.getMessageType().toString() + ": " + comments + "\n");
      }
      var comments = sb.toString();

      var mmc = new MultiMessageComment(call, latitude, longitude, count, comments);
      mmcMessageMap.put(call, mmc);
    }
  }

  static record MultiMessageComment(String call, String latitude, String longitude, String count, String comments) {
    public String[] getValues() {
      return new String[] { call, latitude, longitude, count, comments };
    }
  };

  @Override
  public String[] getHeaders() {
    return new String[] { "call", "latitude", "longitude", "count", "comments" };
  }

  @Override
  public String[] getValues(AggregateMessage message) {
    var call = message.from();
    var mmc = mmcMessageMap.get(call);
    if (mmc == null) {
      return null;
    }
    return mmc.getValues();
  }

}
