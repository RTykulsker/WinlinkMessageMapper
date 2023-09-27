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

package com.surftools.wimp.processors.named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;

/**
 * development effort for ICS-309
 *
 *
 * @author bobt
 *
 */
public class Dev_ICS_309 extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(Dev_ICS_309.class);

  private int ppCount = 0;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings({ "unused" })
  @Override
  public void process() {
    var messages = mm.getMessagesForType(MessageType.ICS_309);

    if (messages != null) {
      for (var message : messages) {
        Ics309Message m = (Ics309Message) message;
        ++ppCount;
      } // end loop over messages
    } // end messages not null
  }

  @Override
  public void postProcess() {
    var sb = new StringBuilder();
    sb.append("\nICS_309 messages: " + ppCount + "\n");
    logger.info(sb.toString());
  }

}
