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

package com.surftools.wimp.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.surftools.wimp.message.ExportedMessage;

public class MessageManager implements IMessageManager {

  private final Map<String, Object> contextMap = new HashMap<>();

  private List<ExportedMessage> messageList = new ArrayList<>();
  private List<ExportedMessage> originalMessageList = new ArrayList<>();

  // source of truth
  private final Map<String, Map<MessageType, List<ExportedMessage>>> senderMap = new HashMap<>();

  // convenience object, once senderMap is not dirty
  private final Map<MessageType, List<ExportedMessage>> messageMap = new HashMap<>();
  private boolean isSenderMapDirty = false;

  public MessageManager() {
    clear();
  }

  @Override
  public void clear() {
    contextMap.clear();
    messageList.clear();
    senderMap.clear();
    messageMap.clear();
    isSenderMapDirty = false;
  }

  @Override
  public Object getContextObject(String key) {
    return contextMap.get(key);
  }

  @Override
  public void putContextObject(String key, Object value) {
    contextMap.put(key, value);
  }

  @Override
  public void load(List<ExportedMessage> messages) {
    messageList.clear();
    messageList.addAll(messages);

    if (originalMessageList.size() == 0) {
      originalMessageList.addAll(messages);
    }

    // convert to our source of truth!
    senderMap.clear();
    for (var message : messages) {
      var from = message.from;
      var type = message.getMessageType();
      var map = senderMap.getOrDefault(from, new HashMap<MessageType, List<ExportedMessage>>());
      var list = map.getOrDefault(type, new ArrayList<ExportedMessage>());
      list.add(message);
      map.put(type, list);
      senderMap.put(from, map);
    }
    isSenderMapDirty = true;
    rebuildMessageMap();
  }

  @Override
  public List<ExportedMessage> getOriginalMessages() {
    return originalMessageList;
  }

  @Override
  public Map<MessageType, List<ExportedMessage>> getMessagesForSender(String sender) {
    return senderMap.get(sender);
  }

  @Override
  public void putMessagesForSender(String sender, Map<MessageType, List<ExportedMessage>> messages) {
    senderMap.put(sender, messages);
    isSenderMapDirty = true;
    rebuildMessageMap();
  }

  @Override
  public void removeMessagesForSender(String sender) {
    senderMap.remove(sender);
    isSenderMapDirty = true;
  }

  @Override
  public void removeMesseagesForSenders(ArrayList<String> removeList) {
    for (var sender : removeList) {
      removeMessagesForSender(sender);
    }
    rebuildMessageMap();
  }

  /**
   * if senderMap is dirty, then we must rebuild the messageMap before using it
   */
  private void rebuildMessageMap() {
    messageMap.clear();
    for (var containedMap : senderMap.values()) {
      for (var entry : containedMap.entrySet()) {
        var entryMessageType = entry.getKey();
        var entryList = entry.getValue();
        var messageList = messageMap.getOrDefault(entryMessageType, new ArrayList<ExportedMessage>());
        messageList.addAll(entryList);
        Collections.sort(messageList);
        messageMap.put(entryMessageType, messageList);
      }
    }
    isSenderMapDirty = false;
  }

  @Override
  public List<ExportedMessage> getMessagesForType(MessageType type) {
    if (isSenderMapDirty) {
      rebuildMessageMap();
    }

    return messageMap.get(type);
  }

  @Override
  public Iterator<MessageType> getMessageTypeIteror() {
    if (isSenderMapDirty) {
      rebuildMessageMap();
    }

    return messageMap.keySet().iterator();
  }

  @Override
  public Iterator<String> getSenderIterator() {
    return senderMap.keySet().iterator();
  }

  @Override
  public void load(Map<MessageType, List<ExportedMessage>> messages) {
    senderMap.clear();
    for (var messageType : messages.keySet()) {
      var typeList = messages.get(messageType);
      for (var message : typeList) {
        var sender = message.from;
        var tmpSenderMap = senderMap.getOrDefault(sender, new HashMap<MessageType, List<ExportedMessage>>());
        var tmpSenderList = tmpSenderMap.getOrDefault(messageType, new ArrayList<ExportedMessage>());

        /**
         * NOTE WELL: possible duplication here
         */
        tmpSenderList.add(message);
        tmpSenderMap.put(messageType, tmpSenderList);
        senderMap.put(sender, tmpSenderMap);
      } // end loop over messages in typeList
    } // end loop over messageTypes
    rebuildMessageMap();
    isSenderMapDirty = false;
  }

  @Override
  public List<ExportedMessage> getAllMessagesForSender(String sender) {
    var list = new ArrayList<ExportedMessage>();
    var map = senderMap.get(sender);
    if (map != null) {
      for (var sublist : map.values()) {
        list.addAll(sublist);
      }
    }
    return list;
  }

}
