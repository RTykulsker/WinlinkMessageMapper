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

package com.surftools.wimp.processors.exercise.eto_2024;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.message.EtoResumeMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for ETO Resume
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_07_25 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_07_25.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    EtoResumeMessage m = (EtoResumeMessage) message;
    getCounter("versions").increment(m.version);

    getCounter("IS-100").increment(m.hasIs100);
    getCounter("IS-200").increment(m.hasIs200);
    getCounter("IS-700").increment(m.hasIs700);
    getCounter("IS-800").increment(m.hasIs800);
    getCounter("IS-2200").increment(m.hasIs2200);

    getCounter("EC-001").increment(m.hasEc001);
    getCounter("EC-016").increment(m.hasEc016);
    getCounter("OR ACES").increment(m.hasAces);
    getCounter("Skywarn").increment(m.hasSkywarn);

    getCounter("AuxCom").increment(m.hasAuxComm);
    getCounter("COM-T").increment(m.hasComT);
    getCounter("COM-L").increment(m.hasComL);

    getCounter("# Served Agencies").increment(m.agencies.size());

    for (var agency : m.agencies) {
      getCounter("Agency").increment(agency);
    }

    getCounter("Comments present").increment(m.comments.isBlank() ? 0 : 1);
    getCounter("Feedback Count").increment(sts.getExplanations().size());
    getCounter("Clearinghouse Count").increment(m.to);

    var blurb = """

        Thank you for participating in this month's exercise. We will use your
        information to help us shape future training exercises and drills. We
        will respect your privacy by not sharing any details of your information
        with any other organization, but we may share aggregated anonymous data.
        Thanks again, and we look forward to seeing you participate again next
        month!

         """;

    setExtraOutboundMessageText(blurb);
  }

}
