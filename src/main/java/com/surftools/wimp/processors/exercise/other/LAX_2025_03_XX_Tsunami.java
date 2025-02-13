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

package com.surftools.wimp.processors.exercise.other;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.CheckOutMessage;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.message.WelfareBulletinBoardMessage;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2025-03-xx: Tsunami-based drill, organized by LAX Northeast
 *
 * @author bobt
 *
 */
public class LAX_2025_03_XX_Tsunami extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(LAX_2025_03_XX_Tsunami.class);


  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

		public DyfiMessage dyfiMessage;
		public CheckInMessage checkInMessage;
		public WelfareBulletinBoardMessage welfareMessage;
		public Ics213Message ics213Message;
		public CheckOutMessage checkOutMessage;
		public Ics214Message ics214Message;

		public int messageCount;
		public int feedbackBand;


    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list
          .addAll(Arrays
						.asList(new String[] { "DYFI", "Check In", "Welfare", "Ics213", "Check Out", "Ics214", //
								"# Messages", "Feedback Band"
              }));
      return list.toArray(new String[0]);
    }



    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(Arrays
						.asList(new String[] { mId(dyfiMessage),
                  mId(checkInMessage), //
								mId(welfareMessage), mId(ics213Message), mId(checkOutMessage), mId(ics214Message), //
								s(messageCount), s(feedbackBand) }));
      return list.toArray(new String[0]);
    };
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);


    // #MM must define acceptableMessages
    var acceptableMessageTypesList = List
        .of( // order matters, last location wins,
					MessageType.DYFI,
            MessageType.CHECK_IN, MessageType.WELFARE_BULLETIN_BOARD, 
            MessageType.ICS_213, MessageType.CHECK_OUT,
            MessageType.ICS_214);
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

	if (type == MessageType.DYFI) {
		handle_DyFiMessage(summary, (DyfiMessage) message);
	} else if (type == MessageType.CHECK_IN) {
      handle_CheckInMessage(summary, (CheckInMessage) message);
	} else if (type == MessageType.WELFARE_BULLETIN_BOARD) {
		handle_WelfareMessage(summary, (WelfareBulletinBoardMessage) message);
	} else if (type == MessageType.ICS_213) {
		handle_Ics213Message(summary, (Ics213Message) message);
	} else if (type == MessageType.CHECK_OUT) {
		handle_CheckOutMessage(summary, (CheckOutMessage) message);
	} else if (type == MessageType.ICS_214) {
		handle_Ics214Message(summary, (Ics214Message) message);
    }

    summaryMap.put(sender, iSummary);
  }

	private void handle_DyFiMessage(Summary summary, DyfiMessage m) {
		sts.setExplanationPrefix("(dyfi");

		// #MM update summary
		summary.dyfiMessage = m;
		++summary.messageCount;
	}

	private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
		sts.setExplanationPrefix("(checkin");

		// #MM update summary
		summary.checkInMessage = m;
		++summary.messageCount;
	}

	private void handle_WelfareMessage(Summary summary, WelfareBulletinBoardMessage m) {
		sts.setExplanationPrefix("(welfare");

		// #MM update summary
		summary.welfareMessage = m;
		++summary.messageCount;
	}

	private void handle_Ics213Message(Summary summary, Ics213Message m) {
		sts.setExplanationPrefix("(ics213");

		// #MM update summary
		summary.ics213Message = m;
		++summary.messageCount;
	}

	private void handle_CheckOutMessage(Summary summary, CheckOutMessage m) {
		sts.setExplanationPrefix("(checkout");

		// #MM update summary
		summary.checkOutMessage = m;
		++summary.messageCount;
	}

	private void handle_Ics214Message(Summary summary, Ics214Message m) {
		sts.setExplanationPrefix("(ics214");

		// #MM update summary
		summary.ics214Message = m;
		++summary.messageCount;
	}

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("(summary)");

    var summary = (Summary) summaryMap.get(sender); // #MM
    
	sts.testNotNull("DYFI message received", summary.dyfiMessage);
	sts.testNotNull("DYFI message received", summary.checkInMessage);
	sts.testNotNull("DYFI message received", summary.welfareMessage);
	sts.testNotNull("DYFI message received", summary.ics213Message);
	sts.testNotNull("DYFI message received", summary.checkOutMessage);
	sts.testNotNull("DYFI message received", summary.ics214Message);
    
    checkAscendingCreationTimestamps(summary);

    summaryMap.put(sender, summary); // #MM
  }

  record MX(ExportedMessage m, String explanation) {
  };

  private void checkAscendingCreationTimestamps(Summary summary) {


  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM
  }
}
