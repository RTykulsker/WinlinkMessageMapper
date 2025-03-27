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

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.ContentParser;
import com.surftools.utils.ContentParser.ParseResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.CheckOutMessage;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.message.PegelstandMessage;
import com.surftools.wimp.message.WelfareBulletinBoardMessage;
import com.surftools.wimp.message.WelfareBulletinBoardMessage.DataType;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2025-03-29: Tsunami-based drill, organized by LAX Northeast
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

    public String allGroups;

    public String dyfiIsExercise;
    public String dyfiGroups;

    public String ciIsExercise;
    public String ciGroups;
    public String ciOperationalCapabilities;
    public String ciDeploymentPosture;
    public String ciInTsunamiZone;
    public String ciInFloodZone;

    public String welfareIsExercise;
    public String welfareGroups;
    public String welfareType;
    public String welfareStatus;
    public String welfareMyStatus;
    public String welfareMessageText;

    public String ics213IsExercise;
    public String ics213Groups;
    public String ics213Resources;

    public String coIsExercise;
    public String coGroups;
    public String coMostImportantThing;

    public String ics214Position;
    public String ics214ResourceCount;
    public String ics214ActivityCount;

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

                  "All Groups",

                  "DYFI IsExercise", "DYFI Groups", //

                  "Check-In IsExercise", "Check-In Groups", "Operational Capabilities", "Deployment Posture",
                  "In Tsunami Zone", "In Flood Zone", //

                  "Welfare IsExercise", "Welfare Groups", "Type", "Status", "My Status", "Message",

                  "ICS-213 IsExercise", "ICS-Groups", "ICS Resources", //

                  "Check-Out IsExercise", "Check-Out Groups", "Most Important Thing Learned", //

                  "ICS-214 Position", "ICS-214 Resource Count", "ICS-214 Activity Count"

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

                  allGroups,

                  dyfiIsExercise, dyfiGroups, //

                  ciIsExercise, ciGroups, ciOperationalCapabilities, ciDeploymentPosture, ciInTsunamiZone,
                  ciInFloodZone, //

                  welfareIsExercise, welfareGroups, welfareType, welfareStatus, welfareMyStatus, welfareMessageText, //

                  ics213IsExercise, ics213Groups, ics213Resources, //

                  coIsExercise, coGroups, coMostImportantThing, //

                  ics214Position, ics214ResourceCount, ics214ActivityCount }));

      return list.toArray(new String[0]);
    };
  }

  final static LinkedHashSet<String> senderGroupSet = new LinkedHashSet<>();
  final static Map<String, String> senderMostImportantThingMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    // #MM must define acceptableMessages
    acceptableMessageTypesSet.addAll(getExpectedMessageTypes());

    super.initialize(cm, mm, logger);

    allowPerfectMessageReporting = true;
  }

  @Override
  protected void beforeProcessingForSender(String sender) {
    super.beforeProcessingForSender(sender);

    // #MM must instantiate a derived Summary object
    iSummary = summaryMap.getOrDefault(sender, new Summary(sender));
    summaryMap.put(sender, iSummary);

    senderGroupSet.clear();
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
        groupSet.stream().forEach(gn -> accumulateGroupName("DYFI", gn));
        summary.dyfiGroups = pr.value();
        count(sts.test("DYFI Comments Group Affiliation parsed ok", true));
      } else {
        summary.dyfiGroups = "";
        count(sts.test("DYFI Comments Group Affiliation parsed ok", false, (String) pr.context()));
      }

      count(sts.test("DYFI Comments parsed", true));
    } else {
      count(sts.test("Comments first word is EXERCISE", false, "null comments"));
      summary.dyfiIsExercise = "(null)";

      count(sts.test("DYFI Comments parsed", false, "null comments"));
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
    isPerfectMessage(m);
  }

  private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
    sts.setExplanationPrefix("(checkin) ");
    accumulateAddresses("Check In addresses", m);

    var comments = m.comments;
    if (!isNull(comments)) {
      var cp = new ContentParser(comments);
      var lines = cp.getLines();

      summary.ciIsExercise = (lines.length >= 1 && lines[0].equalsIgnoreCase("EXERCISE")) ? "YES" : "NO";

      summary.ciGroups = lines.length >= 2 ? lines[1] : "";
      var groupSet = stringToSet(summary.ciGroups, ",");
      groupSet.stream().forEach(gn -> accumulateGroupName("Check In", gn));
      summary.ciOperationalCapabilities = getCommentLineValues(cp, "Operational Capabilities: ");
      summary.ciDeploymentPosture = getCommentLineValues(cp, "Deployment POSTURE: ");
      summary.ciInTsunamiZone = getCommentLineValues(cp, "Station in Tsunami Zone: ");
      summary.ciInFloodZone = getCommentLineValues(cp, "Station in area prone to flooding: ");
      count(sts.test("Check In Comments parsed", true));
    } else {
      summary.ciIsExercise = "(null)";
      summary.ciGroups = "";
      summary.ciOperationalCapabilities = "";
      summary.ciDeploymentPosture = "";
      summary.ciInTsunamiZone = "";
      summary.ciInFloodZone = "";
      count(sts.test("Check In Comments parsed", false));
    }

    count(sts.test("Check In Type should be EXERCISE", m.status.equals("EXERCISE"), m.status));

    getCounter("Check In Status").increment(m.status);
    getCounter("Check In Service").increment(m.service);
    getCounter("Check In Band").increment(m.band);
    getCounter("Check In Mode").increment(m.mode);
    getCounter("Check In Data Source").increment(m.dataSource);

    // #MM update summary
    summary.checkInMessage = m;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_WelfareMessage(Summary summary, WelfareBulletinBoardMessage m) {
    sts.setExplanationPrefix("(welfare) ");
    accumulateAddresses("Welfare addresses", m);

    var comments = m.getDataAsString(DataType.FORM_MESSAGE);
    if (!isNull(comments)) {
      var cp = new ContentParser(comments);
      var pr = cp.isExerciseFirstWord("Comments", "EXERCISE");
      if (pr.error() == null) {
        count(sts.test("Comments first word is #EV", "EXERCISE", (String) pr.context()));
      } else {
        count(sts.test("Comments first word is EXERCISE", false, (String) pr.context()));
      }
      summary.welfareIsExercise = pr.value();

      count(sts.test("Welfare Comments parsed", true));
    } else {
      summary.welfareIsExercise = "(null)";
      summary.welfareGroups = "";
      summary.welfareMessageText = "";
      count(sts.test("Welfare Comments parsed", false));
    }

    summary.welfareType = m.getDataAsString(DataType.MESSAGE_TYPE);
    summary.welfareStatus = m.getDataAsString(DataType.MESSAGE_STATUS);
    summary.welfareMyStatus = m.getDataAsString(DataType.MY_STATUS);

    count(sts.test("Welfare Message line 1 should be EXERCISE", summary.welfareIsExercise.equals("YES")));
    count(sts.test("Welfare Type should be EXERCISE", "EXERCISE", summary.welfareType));
    count(sts.test("Welfare Message Status should be #EV", "INITIAL", summary.welfareStatus));

    getCounter("Welfare Type").increment(summary.welfareType);
    getCounter("Welfare Status").increment(summary.welfareStatus);
    getCounter("Welfare My Status").increment(summary.welfareMyStatus);

    // #MM update summary
    summary.welfareMessage = m;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_Ics213Message(Summary summary, Ics213Message m) {
    sts.setExplanationPrefix("(ics213) ");
    accumulateAddresses("ICS-213 addresses", m);

    count(sts.test("Subject should be #EV", "TSUNAMI DISASTER RESOURCES", m.formSubject));

    var comments = m.formMessage;
    if (!isNull(comments)) {
      var cp = new ContentParser(comments);
      var lines = cp.getLines();

      summary.ics213IsExercise = (lines.length >= 1 && lines[0].equalsIgnoreCase("EXERCISE")) ? "YES" : "NO";

      summary.ics213Groups = lines.length >= 2 ? lines[1] : "";
      var groupSet = stringToSet(summary.ics213Groups, ",");
      groupSet.stream().forEach(gn -> accumulateGroupName("ICS-213", gn));

      summary.ics213Resources = "";
      if (lines.length >= 3 && lines[2].toUpperCase().startsWith("Here are three resources".toUpperCase())) {
        var sb = new StringBuilder();
        for (int i = 3; i < lines.length; ++i) {
          var line = lines[i];
          sb.append(line + "\n");
          getCounter("ICS-213 Resources").increment(line.strip());
        }
        summary.ics213Resources = sb.toString().strip();
      }

      count(sts.test("ICS-213 Comments parsed", true));
    } else {
      summary.ics213IsExercise = "(null)";
      summary.ics213Groups = "";
      summary.ics213Resources = "";
      count(sts.test("ICS-213 Comments parsed", false));
    }

    getCounter("ICS-213 Data Source").increment(m.dataSource);

    // #MM update summary
    summary.ics213Message = m;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_CheckOutMessage(Summary summary, CheckOutMessage m) {
    sts.setExplanationPrefix("(checkout) ");
    accumulateAddresses("Check Out addresses", m);

    var comments = m.comments;
    if (!isNull(comments)) {
      var cp = new ContentParser(comments);
      var lines = cp.getLines();

      summary.coIsExercise = (lines.length >= 1 && lines[0].equalsIgnoreCase("EXERCISE")) ? "YES" : "NO";

      summary.coGroups = lines.length >= 2 ? lines[1] : "";
      var groupSet = stringToSet(summary.ciGroups, ",");
      groupSet.stream().forEach(gn -> accumulateGroupName("Check Out", gn));

      summary.coMostImportantThing = "";
      if (lines.length >= 4
          && lines[2].toUpperCase().startsWith("Most important thing I have learned in this exercise".toUpperCase())) {
        var sb = new StringBuilder();
        for (int i = 3; i < lines.length; ++i) {
          var line = lines[i];
          sb.append(line + "\n");
        }
        summary.coMostImportantThing = sb.toString().strip();
        senderMostImportantThingMap.put(m.from, summary.coMostImportantThing);
      }

      count(sts.test("Check Out Comments parsed", true));
    } else

    {
      summary.coIsExercise = "(null)";
      summary.coGroups = "";
      summary.coMostImportantThing = "";
      count(sts.test("Check Out Comments parsed", false));
    }

    count(sts.test("Check In Type should be EXERCISE", m.status.equals("EXERCISE"), m.status));

    getCounter("Check Out Status").increment(m.status);
    getCounter("Check Out Service").increment(m.service);
    getCounter("Check Out Band").increment(m.band);
    getCounter("Check Out Mode").increment(m.mode);
    getCounter("Check Out Data Source").increment(m.dataSource);

    // #MM update summary
    summary.checkOutMessage = m;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_Ics214Message(Summary summary, Ics214Message m) {
    sts.setExplanationPrefix("(ics214) ");
    accumulateAddresses("ICS-214 addresses", m);

    summary.ics214Position = m.selfResource.icsPosition();
    summary.ics214ResourceCount = String.valueOf(m.assignedResources.stream().filter(r -> !r.isEmpty()).count());
    summary.ics214ActivityCount = String.valueOf(m.activities.stream().filter(r -> !r.isEmpty()).count());

    count(sts.test("Incident Name should be #EV", "TSUNAMI", m.incidentName));

    getCounter("ICS-214 Position").increment(m.selfResource.icsPosition());
    getCounter("ICS-214 Resources").increment(summary.ics214ResourceCount);
    getCounter("ICS-214 Activities").increment(summary.ics214ActivityCount);

    // #MM update summary
    summary.ics214Message = m;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handlePegelstandMessage(Summary summary, PegelstandMessage m) {
    sts.setExplanationPrefix("(pegelstand) ");
    accumulateAddresses("Pegelstand addresses", m);

    // #MM update summary
    summary.pegelstandMessage = m;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("(summary) ");

    var summary = (Summary) summaryMap.get(sender); // #MM

    summary.allGroups = String.join(",", senderGroupSet);

    sts.testNotNull("DYFI message not received", summary.dyfiMessage);
    sts.testNotNull("CheckIn message not received", summary.checkInMessage);
    sts.testNotNull("Welfare message not received", summary.welfareMessage);
    sts.testNotNull("Ics213 message not received", summary.ics213Message);
    sts.testNotNull("Checkout message not received", summary.checkOutMessage);
    sts.testNotNull("Ics214 message not received", summary.ics214Message);

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM

    getCounter("Summary Group Affiliation").write(Path.of(outputPathName, "groupCounts.csv"));
    getCounter("All addresses").write(Path.of(outputPathName, "emailAddresses.csv"));
    getCounter("ICS-213 Resources").write(Path.of(outputPathName, "ics213Resources.csv"));
    writeTable("mostImportantThingLearned.csv", senderMostImportantThingMap);
    getCounter("ICS-214 Position").write(Path.of(outputPathName, "ics213Resources.csv"));

    writeTable("perfectMessages.csv", perfectMessages);
  }

  private void accumulateAddresses(String counterName, ExportedMessage message) {
    var counter = getCounter(counterName);

    // deduplicate addresses
    var addressSet = new LinkedHashSet<String>();
    for (var list : List.of(message.toList, message.ccList)) {
      var addresses = list.split(",");
      for (var address : addresses) {
        if (!isNull(address)) {
          addressSet.add(address);
        }
      }
    }

    addressSet.stream().forEach(a -> counter.increment(a.toUpperCase()));
  }

  private void accumulateGroupName(String label, String groupName) {
    if (groupName.length() > 0) {
      if (groupName.length() > 0) {
        getCounter(label + " Group Affiliation").increment(groupName.toUpperCase());
        getCounter("Summary Group Affiliation").increment(groupName.toUpperCase());
        senderGroupSet.add(groupName.toUpperCase());
      }
    }
  }

  private Set<String> stringToSet(String inputString, String regexDelimiter) {
    var set = new LinkedHashSet<String>();
    if (inputString == null) {
      return set;
    }

    var fields = inputString.split(regexDelimiter);
    for (var field : fields) {
      field = ((field == null) ? "" : field).strip();
      set.add(field);
    }

    return set;
  }

  /**
   * find the remainder of line (in lines) after a key (which has delimiter)
   *
   * @param contentParser
   * @param key
   * @return stripped remainder of line or empty string
   */
  private String getCommentLineValues(ContentParser cp, String key) {
    var anwKey = sts.toAlphaNumericWords(key).toLowerCase();
    for (var line : cp.getLines()) {
      var anwLine = sts.toAlphaNumericWords(line).toLowerCase();
      if (anwLine.startsWith(anwKey)) {
        return line.substring(key.length()).strip();
      }
    }
    return "";
  }
}
