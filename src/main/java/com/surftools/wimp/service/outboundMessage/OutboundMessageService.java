/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

package com.surftools.wimp.service.outboundMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.service.IService;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class OutboundMessageService implements IService {
  private static final Logger logger = LoggerFactory.getLogger(OutboundMessageService.class);

  private IOutboundMessageEngine engine;

  public OutboundMessageService(IConfigurationManager cm) {
    this(cm, null, null, null);
  }

  public OutboundMessageService(IConfigurationManager cm, String fileName) {
    this(cm, null, null, fileName);
  }

  public OutboundMessageService(IConfigurationManager cm, IMessageManager mm) {
    this(cm, mm, null);
  }

  public OutboundMessageService(IConfigurationManager cm, IMessageManager mm, String extraContent) {
    this(cm, mm, extraContent, null);
  }

  public OutboundMessageService(IConfigurationManager cm, IMessageManager mm, String extraContent, String fileName) {
    var engineTypeName = cm.getAsString(Key.OUTBOUND_MESSAGE_ENGINE_TYPE, EngineType.PAT.name());
    var engineType = EngineType.valueOf(engineTypeName);
    if (engineType == null) {
      throw new RuntimeException("Could not find engineType for: " + engineTypeName);
    }

    switch (engineType) {
    case PAT:
      engine = new PatOutboundMessageEngine(cm, extraContent, fileName);
      break;

    case WINLINK_EXPRESS:
      engine = new WinlinkExpressOutboundMessageEngine(cm, extraContent, fileName);
      break;

    case WEB:
      engine = new WebOutboundMessageEngine(cm, mm);
      break;

    default:
      throw new RuntimeException("Could not find engine for " + engineType.name());
    }
  }

  public List<OutboundMessage> sendAll(List<OutboundMessage> inputMessageList) {
    var outputMessageList = new ArrayList<OutboundMessage>(inputMessageList.size());

    var toCountMap = new HashMap<String, Integer>();

    for (var inputMessage : inputMessageList) {
      if (inputMessage.to() == null) {
        continue;
      }
      var messageId = engine.send(inputMessage);
      if (messageId == null) {
        continue;
      }
      var outputMessage = new OutboundMessage(inputMessage, messageId);
      outputMessageList.add(outputMessage);

      var count = toCountMap.getOrDefault(inputMessage.to(), Integer.valueOf(0));
      toCountMap.put(inputMessage.to(), ++count);
    }

    var countToListMap = new TreeMap<Integer, List<String>>();
    for (var to : toCountMap.keySet()) {
      var count = toCountMap.get(to);
      var toList = countToListMap.getOrDefault(count, new ArrayList<String>());
      toList.add(to);
      countToListMap.put(count, toList);
    }

    logger.info("unique messages: " + inputMessageList.size());
    logger.info("unique recipients: " + toCountMap.size());
    for (var count : countToListMap.descendingKeySet()) {
      var toList = countToListMap.get(count);
      if (count > 1) {
        logger.info("count: " + count + ", list: " + String.join(",", toList));
      }
    }

    if (inputMessageList.size() > 1 && toCountMap.size() == 1) {
      logger.warn("#### WARNING: outbound messaging looks broken");
    }

    engine.finalizeSend();

    return outputMessageList;
  }

  @Override
  public String getName() {
    return "OutboundMessageService";
  }

}
