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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.ContentParser;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.ContentParser.ParseResult;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.CheckOutMessage;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.message.PegelstandMessage;
import com.surftools.wimp.message.WelfareBulletinBoardMessage;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2025-03-xx: Tsunami-based drill, organized by LAX Northeast
 *
 * @author bobt
 *
 */
public class LAX_2025_03_29_Tsunami extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(LAX_2025_03_29_Tsunami.class);

  protected static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";

  protected static final DateTimeFormatter DYFI_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter DYFI_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static final String EXPECTED_DATE = "03/29/2025";
  protected static final String EXPECTED_TIME = "08:29";

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
    public PegelstandMessage pegelstandMessage;

    public int messageCount;
    public int feedbackBand;

    public String dyfiIsExercise;
    public String dyfiGroups;

    public String ciIsExercise;
    public String ciGroups;
    public String ciOperationalCapabilities;
    public String ciDeploymentPosture;
    public String ciInTsunamiZone;
    public String ciInFloodZone;

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
              .asList(new String[] { "DYFI", "Check In", "Welfare", "Ics213", "Check Out", "Ics214", "Pegelstand", //
                  "# Messages", "Feedback Band", //

                  "DYFI IsExercise", "DYFI Groups", //

                  "Check-In IsExercise", "Check-In Groups", "Operational Capabilities", "Deployment Posture",
                  "In Tsunami Zone", "In Flood Zone",//

              }));
      return list.toArray(new String[0]);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(Arrays
              .asList(new String[] { mId(dyfiMessage), mId(checkInMessage), //
                  mId(welfareMessage), mId(ics213Message), mId(checkOutMessage), mId(ics214Message),
                  mId(pegelstandMessage), //
                  s(messageCount), s(feedbackBand), //

                  dyfiIsExercise, dyfiGroups, //

                  ciIsExercise, ciGroups, ciOperationalCapabilities, ciDeploymentPosture, ciInTsunamiZone,
                  ciInFloodZone, //
              }));

      return list.toArray(new String[0]);
    };
  }

  final static List<MessageType> acceptableMessageTypesList = List
      .of( // order matters, last location wins,
          MessageType.DYFI, MessageType.CHECK_IN, MessageType.WELFARE_BULLETIN_BOARD, MessageType.ICS_213,
          MessageType.CHECK_OUT, MessageType.ICS_214, MessageType.PEGELSTAND);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    // #MM must define acceptableMessages
    acceptableMessageTypesSet.addAll(acceptableMessageTypesList);

    super.initialize(cm, mm, logger);
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

    accumulateAddresses("All addresses", message);

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
    } else if (type == MessageType.PEGELSTAND) {
      handlePegelstandMessage(summary, (PegelstandMessage) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void accumulateAddresses(String counterName, ExportedMessage message) {
    var counter = getCounter(counterName);

    // deduplicate addresses
    var addressSet = new HashSet<String>();
    for (var list : List.of(message.toList, message.ccList)) {
      var addresses = list.split(",");
      for (var address : addresses) {
        if (!isNull(address)) {
          addressSet.add(address);
        }
      }
    }

    for (var address : addressSet) {
      counter.increment(address);
    }

  }

  private void handle_DyFiMessage(Summary summary, DyfiMessage m) {
    sts.setExplanationPrefix("(dyfi) ");
    accumulateAddresses("DYFI addresses", m);

    var hasUSGSAddress = (m.toList + "," + m.ccList).toUpperCase().contains(REQUIRED_USGS_ADDRESS.toUpperCase());
    count(sts.test("To and/or CC addresses must contain " + REQUIRED_USGS_ADDRESS, hasUSGSAddress));
    count(sts.test("Event Type must be: EXERCISE", !m.isRealEvent));
    count(sts.test("Form Latitude and Longitude must be valid", m.formLocation.isValid(), m.formLocation.toString()));

    try {
      var intensity = Integer.parseInt(m.intensity);
      count(sts.test("Intensity must be >= 7", intensity >= 7, m.intensity));
    } catch (Exception e) {
      count(sts.test("Intensity must be >= 7", false, m.intensity));
    }

    var comments = m.comments;
    if (!isNull(comments)) {
      var cp = new ContentParser(comments);
      var pr = new ParseResult(null, null, null);

      pr = cp.isExerciseFirstWord("Comments", "EXERCISE");
      if (pr.error() == null) {
        count(sts.test("Comments first word is #EV", "EXERCISE", (String) pr.context()));
      } else {
        count(sts.test("Comments first word is EXERCISE", false, (String) pr.context()));
      }
      summary.dyfiIsExercise = pr.value();

      // comma-delimited, starting on 2nd word, stopping on first word that starts with digit
      pr = cp.getDYFIGroupAffiliation(",", 2, "\\d+.");
      if (pr.error() == null) {

        @SuppressWarnings("unchecked")
        var groupSet = (Set<String>) pr.context();
        for (var groupName : groupSet) {
          getCounter("DYFI Group Affiliation").increment(groupName);
        }
        summary.dyfiGroups = pr.value();
        count(sts.test("DYFI Comments Group Affiliation parsed ok", true));
      } else {
        summary.dyfiGroups = "";
        count(sts.test("DYFI Comments Group Affiliation parsed ok", false, (String) pr.context()));
      }

      // summary.dyfiGroups = processGroupAffiliation("Comments", comments, null);
      count(sts.test("Comments parsed", true));
    } else {
      count(sts.test("Comments first word is EXERCISE", false, "null comments"));
      summary.dyfiIsExercise = "(null)";

      count(sts.test("Comments parsed", false, "null comments"));
      summary.dyfiGroups = "(null)";
    }

    var dateTime = m.formDateTime;
    if (dateTime != null) {
      count(sts.test("Date of Earthquake should be #EV", EXPECTED_DATE, m.formDateTime.format(DYFI_DATE_FORMATTER)));
      count(sts.test("Time of Earthquake should be #EV", EXPECTED_TIME, m.formDateTime.format(DYFI_TIME_FORMATTER)));
    } else {
      count(sts.test("Date of Earthquake should be " + EXPECTED_DATE, false));
      count(sts.test("Time of Earthquake should be " + EXPECTED_TIME, false));
    }

    getCounter("Intensity").increment(m.intensity);
    getCounter("Form Version").increment(m.formVersion);

    // #MM update summary
    summary.dyfiMessage = m;
    ++summary.messageCount;
  }

  private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
    sts.setExplanationPrefix("(checkin) ");
    accumulateAddresses("Check In addresses", m);

    // TODO
    // var comments = m.comments;
    // if (!isNull(comments)) {
    // summary.ciIsExercise = isExercise("Comments", comments, 1);
    // summary.ciGroups = processGroupAffiliation("Comments", comments, 2);
    // summary.ciOperationalCapabilities = getCommentLineValues(comments, "Operational Capabilities", 3);
    // summary.ciDeploymentPosture = getCommentLineValues(comments, "Deployment POSTURE", 4);
    // summary.ciInTsunamiZone = getCommentLineValues(comments, "Station in Tsunami Zone", 5);
    // summary.ciInFloodZone = getCommentLineValues(comments, "Station in area prone to flooding", 6);
    // count(sts.test("Comments parsed", true));
    // } else {
    // summary.ciIsExercise = Boolean.FALSE;
    // summary.ciGroups = "";
    // summary.ciOperationalCapabilities = "";
    // summary.ciDeploymentPosture = "";
    // summary.ciInTsunamiZone = "";
    // summary.ciInFloodZone = "";
    // count(sts.test("Comments parsed", false));
    // }

    // count(sts.test("EXERCISE should be in first word of Comments", summary.ciIsExercise));
    // count(sts.test("Group Affiliation should be second line of Comments", summary.ciGroups.length() > 0));
    // count(sts
    // .test("Operational Capabilities should be third line of Comments",
    // summary.ciOperationalCapabilities.length() > 0));

    // #MM update summary
    summary.checkInMessage = m;

    ++summary.messageCount;
  }

  /**
   *
   * @param comments
   * @param key
   * @param lineNumber
   * @return
   */
  private String getCommentLineValues(String comments, String key, int lineNumber) {
    if (isNull(comments)) {
      return null;
    }

    var lines = comments.split("\n");
    if (lines == null || lines.length < lineNumber) {
      return null;
    }

    var line = lines[lineNumber - 1];
    if (isNull(line)) {
      return null;
    }

    var anwLine = sts.toAlphaNumericWords(line);
    var anwKey = sts.toAlphaNumericWords(key);
    if (!anwLine.toLowerCase().contains(anwKey.toLowerCase())) {
      return null;
    }

    var startIndex = anwLine.toLowerCase().indexOf(anwKey.toLowerCase());
    if (startIndex == -1) {
      return null;
    }

    var subString = line.substring(startIndex + key.length());
    var c = subString.charAt(0);
    if (c == ':') {
      subString = line.substring(startIndex + key.length() + 1);
    }

    return subString.strip();
  }

  private void handle_WelfareMessage(Summary summary, WelfareBulletinBoardMessage m) {
    sts.setExplanationPrefix("(welfare) ");
    accumulateAddresses("Welfare addresses", m);

    // #MM update summary
    summary.welfareMessage = m;
    ++summary.messageCount;
  }

  private void handle_Ics213Message(Summary summary, Ics213Message m) {
    sts.setExplanationPrefix("(ics213) ");
    accumulateAddresses("ICS-213 addresses", m);

    // #MM update summary
    summary.ics213Message = m;
    ++summary.messageCount;
  }

  private void handle_CheckOutMessage(Summary summary, CheckOutMessage m) {
    sts.setExplanationPrefix("(checkout) ");
    accumulateAddresses("Check Out addresses", m);

    // #MM update summary
    summary.checkOutMessage = m;
    ++summary.messageCount;
  }

  private void handle_Ics214Message(Summary summary, Ics214Message m) {
    sts.setExplanationPrefix("(ics214) ");
    accumulateAddresses("ICS-214 addresses", m);

    // #MM update summary
    summary.ics214Message = m;
    ++summary.messageCount;
  }

  private void handlePegelstandMessage(Summary summary, PegelstandMessage m) {
    sts.setExplanationPrefix("(pegelstand) ");
    accumulateAddresses("Pegelstand addresses", m);

    // #MM update summary
    summary.pegelstandMessage = m;
    ++summary.messageCount;
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("(summary) ");

    var summary = (Summary) summaryMap.get(sender); // #MM

    sts.testNotNull("DYFI message not received", summary.dyfiMessage);
    sts.testNotNull("CheckIn message not received", summary.checkInMessage);
    sts.testNotNull("Welfare message not received", summary.welfareMessage);
    sts.testNotNull("Ics213 message not received", summary.ics213Message);
    sts.testNotNull("Checkout message not received", summary.checkOutMessage);
    sts.testNotNull("Ics214 message not received", summary.ics214Message);

    checkAscendingCreationTimestamps(summary);

    summaryMap.put(sender, summary); // #MM
  }

  private void checkAscendingCreationTimestamps(Summary summary) {

  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM
  }
}
