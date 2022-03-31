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

package com.surftools.winlinkMessageMapper.summary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class MessageCounts {
  private Map<MessageType, Integer> countMap;
  private int totalCount;

  public MessageCounts() {
    countMap = new HashMap<>();
    totalCount = 0;
  }

  public MessageCounts(String[] fields, int index) {
    this();
    var values = MessageType.values();
    int n = values.length;
    for (int i = 0; i < n; ++i) {
      MessageType messageType = values[i];
      String valueString = fields[i + index];
      int value = Integer.valueOf(valueString);
      countMap.put(messageType, value);
      totalCount += value;
    }
  }

  public MessageType increment(ExportedMessage message) {
    MessageType messageType = message.getMessageType();
    Integer count = countMap.getOrDefault(messageType, Integer.valueOf(0));
    ++count;
    ++totalCount;
    countMap.put(messageType, count);
    return messageType;
  }

  public int getTotalCount() {
    return totalCount;
  }

  @Override
  public String toString() {
    return getText();
  }

  public Map<MessageType, Integer> getCountMap() {
    return countMap;
  }

  public static List<String> getHeaders() {
    var list = new ArrayList<String>();
    var values = MessageType.values();
    int n = values.length;
    for (int i = 0; i < n; ++i) {
      MessageType messageType = values[i];
      list.add(messageType.toString());
    }
    return list;
  }

  public List<String> getValues() {
    var list = new ArrayList<String>();
    var values = MessageType.values();
    int n = values.length;
    for (int i = 0; i < n; ++i) {
      MessageType messageType = values[i];
      int count = countMap.getOrDefault(messageType, Integer.valueOf(0));
      list.add(String.valueOf(count));
    }
    return list;
  }

  public String getText() {
    List<MessageType> messageTypeList = new ArrayList<>(countMap.keySet().size());
    messageTypeList.addAll(countMap.keySet());
    Collections.sort(messageTypeList, new MessageTypeComparator(countMap));

    StringBuilder sb = new StringBuilder();
    for (MessageType messageType : messageTypeList) {
      Integer count = countMap.get(messageType);
      sb.append("message type: " + messageType.toString() + ", count: " + count + "\n");
    }

    String s = sb.toString();
    return s;
  }

  static class MessageTypeComparator implements Comparator<MessageType> {
    private Map<MessageType, Integer> countMap;

    MessageTypeComparator(Map<MessageType, Integer> countMap) {
      this.countMap = countMap;
    }

    @Override
    public int compare(MessageType o1, MessageType o2) {
      int i1 = countMap.get(o1);
      int i2 = countMap.get(o2);
      return i2 - i1;
    }

  }

}
