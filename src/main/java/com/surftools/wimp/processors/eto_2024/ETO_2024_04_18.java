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

package com.surftools.wimp.processors.eto_2024;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.HospitalStatusMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-04-18 Exercise: Hospital Status
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_04_18 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_04_18.class);

  private Counter ppVersionCounter = new Counter();
  private Counter ppAddressCounter = new Counter();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    counterMap.put("H5 Address", ppAddressCounter);
    counterMap.put("Versions", ppVersionCounter);
  }

  /**
   * this is the money shot
   *
   * @param m
   */
  private void specificProcessing(HospitalStatusMessage m) {
    ppVersionCounter.increment(m.version);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    switch (message.getMessageType()) {
    case HOSPITAL_STATUS:
      var m = (HospitalStatusMessage) message;
      specificProcessing(m);
      break;

    default:
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    } // end switch
  }

}
