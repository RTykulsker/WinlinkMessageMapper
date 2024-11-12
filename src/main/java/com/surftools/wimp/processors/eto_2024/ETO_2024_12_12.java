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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.processors.std.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-12-12: an ICS-213-RR and an ICS-214 that references the RR, themed around Santa's Wish List
 *
 * @author bobt
 *
 */
public class ETO_2024_12_12 extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_12_12.class);

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

    public Ics213RRMessage ics213RRMessage;
    public Ics214Message ics214Message;

    public String allRequests;
    public String allResources;
    public String allActivities;

    public int activityCount;
    public boolean isNice;
    public String sentiment;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list.addAll(Arrays.asList(new String[] { "Ics213RR", "Ics214", //
      }));
      return list.toArray(new String[0]);
    }

    private String s(int i) {
      return String.valueOf(i);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list.addAll(Arrays.asList(new String[] { mId(ics213RRMessage), mId(ics214Message),//
      }));

      return list.toArray(new String[0]);
    };
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    // #MM must define acceptableMessages
    var acceptableMessageTypesList = List.of(MessageType.ICS_213_RR, MessageType.ICS_214); // order matters, last
                                                                                           // location wins,
    acceptableMessageTypesSet.addAll(acceptableMessageTypesList);
  }

  @Override
  protected void beforeProcessingForSender(String sender) {
    super.beforeProcessingForSender(sender);

    // #MM must instantiate a derived Summary object
    iSummary = summaryMap.getOrDefault(sender, new Summary(sender));
    summaryMap.put(sender, iSummary);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var summary = (Summary) iSummary;

    var type = message.getMessageType();
    if (type == MessageType.PLAIN) {
      handle_PlainMessage(summary, (PlainMessage) message);
    } else if (type == MessageType.ICS_213_RR) {
      handle_Ics213RRMessage(summary, (Ics213RRMessage) message);
    } else if (type == MessageType.ICS_214) {
      handle_Ics214Message(summary, (Ics214Message) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_PlainMessage(Summary summary, PlainMessage message) {
    count(sts.test("Message could not be identified (missing form attachment?)", false, message.messageId));
  }

  private void handle_Ics213RRMessage(Summary summary, Ics213RRMessage m) {
    // TODO implement
    // count(sts.test("ICS-309 task number should be #EV", "241109", m.taskNumber));

    // #MM update summary
    summary.ics213RRMessage = m;
  }

  private void handle_Ics214Message(Summary summary, Ics214Message m) {

    // TODO implement
    // count(sts.test("OPTION (via comments) should be readable", option >= 1, m.comments));

    // TODO search for 213RR messageId

    // #MM update summary
    summary.ics214Message = m;
  }

  @Override
  protected String makeOutboundMessageFeedback(BaseSummary summary) {
    var outboundMessageFeedback = (summary.explanations.size() == 0) ? "Perfect messages!" + getNagString(2025)
        : String.join("\n", summary.explanations) + getNagString(2025) + FeedbackProcessor.OB_DISCLAIMER;
    return outboundMessageFeedback;
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("");

    var summary = (Summary) summaryMap.get(sender); // #MM
    if (summary.ics213RRMessage == null) {
      summary.explanations.add("No ICS-213-RR message received.");
    }

    if (summary.ics214Message == null) {
      summary.explanations.add("No ICS-214 message received.");
    }

    // TODO naught/nice, sentiment, nag

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM
  }
}
