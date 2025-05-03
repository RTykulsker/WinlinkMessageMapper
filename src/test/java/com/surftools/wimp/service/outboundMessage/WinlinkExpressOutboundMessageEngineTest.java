/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

import org.junit.Test;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.utils.config.impl.MemoryConfigurationManager;

public class WinlinkExpressOutboundMessageEngineTest {

  @Test
  public void hello() {
    var cm = new MemoryConfigurationManager(Key.values());
    cm.putString(Key.OUTBOUND_MESSAGE_SENDER, "WIMP-TEST");
    cm.putString(Key.OUTBOUND_MESSAGE_SOURCE, "WIMP-TEST");
    cm.putString(Key.OUTPUT_PATH, "/tmp");
    cm.putString(Key.PATH, "/tmp");
    cm.putBoolean(Key.OUTPUT_PATH_CLEAR_ON_START, false);
    cm.putInt(Key.OUTBOUND_MESSAGE_EXTRA_CONTEXT, 1000);
    var writeProcessor = new WriteProcessor();
    writeProcessor.initialize(cm, null);
    String extraContent = null;
    String filenName = "test.xml";
    var engine = new WinlinkExpressOutboundMessageEngine(cm, extraContent, filenName);
    var messages = generateMessages(100);
    for (var m : messages) {
      engine.send(m);
    }
    engine.finalizeSend();
  }

  private List<OutboundMessage> generateMessages(int nMessagestoGenerate) {
    var list = new ArrayList<OutboundMessage>(nMessagestoGenerate);
    for (var i = 1; i <= nMessagestoGenerate; ++i) {
      String from = "test-from";
      String to = "test-to";
      String subject = "test-subject-" + i;
      String body = """
          This is the message body.

          It consists of sever lines.

          With embedded blank lines.

          """;
      String messageId = null;
      var m = new OutboundMessage(from, to, subject, body, messageId);
      list.add(m);
    }
    return list;
  }
}
