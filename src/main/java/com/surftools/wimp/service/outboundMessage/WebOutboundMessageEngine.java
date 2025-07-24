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

import java.util.HashMap;
import java.util.Map;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class WebOutboundMessageEngine implements IOutboundMessageEngine {
  private Map<String, String> fromMessageMap;
  private IMessageManager mm;

  public WebOutboundMessageEngine(IConfigurationManager cm, IMessageManager mm) {
    this.fromMessageMap = new HashMap<>();
    this.mm = mm;
  }

  @Override
  /**
   * add to map
   */
  public String send(OutboundMessage m) {
    fromMessageMap.put(m.to(), m.body());
    return "";
  }

  @Override
  public void finalizeSend() {
    mm.putContextObject("webOutboundMessage", fromMessageMap);
  }

  @Override
  public boolean isReady() {
    return true;
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.WEB;
  }

}
