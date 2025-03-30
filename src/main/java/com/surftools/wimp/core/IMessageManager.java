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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.surftools.wimp.message.ExportedMessage;

public interface IMessageManager {

  /**
   * retrieve arbitrary inter-process data
   *
   * @param key
   * @return
   */
  public Object getContextObject(String key);

  /**
   * store arbitrary inter-process data
   *
   * @param key
   * @param value
   */
  public void putContextObject(String key, Object value);

  /**
   * load all messages after reading from exported message files
   *
   * @param messages
   */
  public void load(List<ExportedMessage> messages);

  /**
   * return all messages, without explicit rejection or de-duplication, useful for classifier
   *
   * @return
   */
  public List<ExportedMessage> getOriginalMessages();

  /**
   * load all messages after classifying messages by type
   *
   * @param messages
   */
  public void load(Map<MessageType, List<ExportedMessage>> messages);

  /**
   * get a list of messages by sender, typically for processing/aggregating
   *
   * @param sender
   * @return
   */
  public Map<MessageType, List<ExportedMessage>> getMessagesForSender(String sender);

  /**
   * get a list of messages by sender, typically for processing/aggregating
   *
   * @param sender
   * @return
   */
  public List<ExportedMessage> getAllMessagesForSender(String sender);

  /**
   * store a list list of messages by sender, typically after de-duplicating
   *
   * @param sender
   * @param messages
   */
  public void putMessagesForSender(String sender, Map<MessageType, List<ExportedMessage>> messages);

  /**
   * remove all messages for sender, typically because of filtering
   *
   * @param sender
   */
  public void removeMessagesForSender(String sender);

  /**
   * remove all messages for listed senders, typically because of filtering
   *
   * @param removeList
   */
  public void removeMesseagesForSenders(ArrayList<String> removeList);

  /**
   * get list of messages for given type, typically for writing to file
   *
   * @param type
   * @return
   */
  public List<ExportedMessage> getMessagesForType(MessageType type);

  /**
   * get iterator of message types stored
   *
   * @return
   */
  public Iterator<MessageType> getMessageTypeIteror();

  /**
   * get an iterator of senders
   *
   * @return
   */
  public Iterator<String> getSenderIterator();

  /**
   * clear all messages
   */
  public void clear();

}
