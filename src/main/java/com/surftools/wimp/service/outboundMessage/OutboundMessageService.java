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
import java.util.List;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.service.IService;

public class OutboundMessageService implements IService {
  private IOutboundMessageEngine engine;
  private PatOutboundMessageEngine patEngine;

  public OutboundMessageService(IConfigurationManager cm) {

    var engineTypeString = cm.getAsString(Key.OUTBOUND_MESSAGE_ENGINE_TYPE, EngineType.PAT.name());
    var engineType = EngineType.valueOf(engineTypeString);
    if (engineType == null) {
      throw new RuntimeException(
          "Configuration key: " + Key.OUTBOUND_MESSAGE_ENGINE_TYPE.name() + ", could not find engine");
    }

    switch (engineType) {
    case PAT:
      engine = patEngine = new PatOutboundMessageEngine(cm);
      break;

    default:
      throw new RuntimeException("Could not find engine for " + engineType.name());
    }

  }

  public List<OutboundMessage> sendAll(List<OutboundMessage> inputMessageList) {
    var outputMessageList = new ArrayList<OutboundMessage>(inputMessageList.size());

    for (var inputMessage : inputMessageList) {
      var messageId = engine.send(inputMessage);
      var outputMessage = new OutboundMessage(inputMessage, messageId);
      outputMessageList.add(outputMessage);
    }

    if (engine.getEngineType() == EngineType.PAT) {
      patEngine.finalizeSend();
    }

    return outputMessageList;
  }

  @Override
  public String getName() {
    return "OutboundMessageService";
  }

}
