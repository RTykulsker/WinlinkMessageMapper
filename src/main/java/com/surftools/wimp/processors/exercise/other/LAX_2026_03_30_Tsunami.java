/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.ContentParser;
import com.surftools.utils.MultiDateTimeParser;
import com.surftools.utils.RenewableBag;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.BulletinMessage;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.CheckOutMessage;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics214Message;
import com.surftools.wimp.message.WelfareBulletinBoardMessage;
import com.surftools.wimp.message.WelfareBulletinBoardMessage.DataType;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.service.map.IMapService;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2026-03-30: Tsunami-based drill, organized by LAX Northeast
 *
 * Mega_Tsunami is focused on feedback; Mini-Tsunami is focused on aggregating responses
 *
 * @author bobt
 *
 */
public class LAX_2026_03_30_Tsunami extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(LAX_2026_03_30_Tsunami.class);

  protected static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";

  protected static final DateTimeFormatter ICS_214_DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

    public DyfiMessage dyfiMessage;
    public CheckInMessage checkInMessage;
    public WelfareBulletinBoardMessage welfareMessage;
    public Ics213Message ics213Message;
    public BulletinMessage bulletinMessage;
    public CheckOutMessage checkOutMessage;
    public Ics214Message ics214Message;

    public int messageCount;
    public int feedbackBand;

    public String allGroups;

    public String dyfiIsExercise;
    public String dyfiGroup;

    public String ciIsExercise;
    public String ciGroups;
    public String ciOperationalCapabilities;
    public String ciDeploymentPosture;
    public String ciInTsunamiZone;
    public String ciInFloodZone;

    public String welfareIsExercise;
    public String welfareType;
    public String welfareStatus;
    public String welfareMyStatus;
    public String welfareMessageText;

    public String ics213_school1_risk;
    public String ics213_school2_risk;

    public String bulletinFor;
    public String bulletinFrom;
    public String bulletinPrecedence;
    public String bulletinSubject;
    public String bulletinIsExercise;

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
              .asList(new String[] { "DYFI", "Check In", "Welfare", "Ics213", "Bulletin", "Check Out", "Ics214", //
                  "# Messages", "Feedback Band", //

                  "All Groups",

                  "DYFI IsExercise", "DYFI Group", //

                  "Check-In IsExercise", "Check-In Groups", "Operational Capabilities", "Deployment Posture",
                  "In Tsunami Zone", "In Flood Zone", //

                  "Welfare IsExercise", "Welfare Type", "Welfare Status", "My Status", "Welfare Message",

                  "ICS-213 School 1 Risk", "ICS School 2 Risk", //

                  "Bulletin For", "Bulletin From", "Bulletin Precedence", "Bulletin Subject", "Bulletin Is Exercise", //

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
                  mId(welfareMessage), mId(ics213Message), mId(bulletinMessage), mId(checkOutMessage),
                  mId(ics214Message),

                  s(messageCount), s(feedbackBand), //

                  allGroups,

                  dyfiIsExercise, dyfiGroup, //

                  ciIsExercise, ciGroups, ciOperationalCapabilities, ciDeploymentPosture, ciInTsunamiZone,
                  ciInFloodZone, //

                  welfareIsExercise, welfareType, welfareStatus, welfareMyStatus, welfareMessageText, //

                  ics213_school1_risk, ics213_school2_risk, //

                  bulletinFor, bulletinFrom, bulletinPrecedence, bulletinSubject, bulletinIsExercise, //

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
    iSummary.messageIds = "";
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
    } else if (type == MessageType.BULLETIN) {
      handle_BulletinMessage(summary, (BulletinMessage) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_DyFiMessage(Summary summary, DyfiMessage m) {
    sts.setExplanationPrefix("(dyfi) ");
    accumulateAddresses("DYFI addresses", m);

    var hasUSGSAddress = (m.toList + "," + m.ccList).toUpperCase().contains(REQUIRED_USGS_ADDRESS.toUpperCase());
    count(sts.test("DYFI To and/or CC addresses must contain " + REQUIRED_USGS_ADDRESS, hasUSGSAddress));
    count(sts.test("DYFI Event Type must be: EXERCISE", !m.isRealEvent));
    count(sts
        .test("DYFI Form Latitude and Longitude must be valid", m.formLocation.isValid(), m.formLocation.toString()));

    count(sts.test("DYFI Did you feel it? should be Yes", m.isFelt));
    getCounter("DYFI response").increment(m.experienceResponse);

    try {
      var intensity = Integer.parseInt(m.intensity);
      count(sts.test("DYFI Intensity must be >= 7", intensity >= 7, m.intensity));
    } catch (Exception e) {
      count(sts.test("DYFI Intensity must be >= 7", false, m.intensity));
    }

    var comments = m.comments;
    count(sts.testIfPresent("DYFI Comments should be present", comments));
    if (!isNull(comments)) {
      var fields = comments.split(",");
      if (fields.length >= 1) {
        var isExercise = fields[0];
        count(sts.test("DYFI 1st Comment fields should be #EV", "Exercise", isExercise));
      }

      if (fields.length >= 2) {
        summary.dyfiGroup = fields[1];
        getCounter("DYFI Group").increment(summary.dyfiGroup);
      }
    }

    getCounter("DYFI Intensity").increment(m.intensity);
    getCounter("DYFI Form Version").increment(m.formVersion);

    // #MM update summary
    summary.dyfiMessage = m;
    summary.messageIds += "dyfi: " + m.messageId;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
    sts.setExplanationPrefix("(check_in) ");
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
      summary.ciDeploymentPosture = getCommentLineValues(cp, "Deployment Posture: ");

      var inZoneString = getCommentLineValues(cp, "Station in Tsunami Zone: ").toUpperCase();
      summary.ciInTsunamiZone = inZoneString.startsWith("YES") ? "YES"
          : inZoneString.startsWith("NO") ? "NO" : inZoneString.strip().length() == 0 ? "(empty)" : "(other)";

      var inFloodString = getCommentLineValues(cp, "Station in area prone to flooding: ").toUpperCase();
      summary.ciInFloodZone = inFloodString.startsWith("YES") ? "YES"
          : inFloodString.startsWith("NO") ? "NO" : inFloodString.strip().length() == 0 ? "(empty)" : "(other)";
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
    getCounter("Check In -- In Tsunami Zone").increment(summary.ciInTsunamiZone);
    getCounter("Check In -- In Area Prone to Flooding").increment(summary.ciInFloodZone);

    // #MM update summary
    summary.checkInMessage = m;
    ++summary.messageCount;
    summary.messageIds += "\ncheck in: " + m.messageId;
    isPerfectMessage(m);
  }

  private void handle_WelfareMessage(Summary summary, WelfareBulletinBoardMessage m) {
    sts.setExplanationPrefix("(welfare_bulletin_board) ");
    accumulateAddresses("Welfare addresses", m);

    var comments = m.getDataAsString(DataType.FORM_MESSAGE);
    if (!isNull(comments)) {
      var cp = new ContentParser(comments);
      var pr = cp.isExerciseFirstWord("Comments", "EXERCISE");
      if (pr.error() == null) {
        count(sts.test("Welfare Comments first word is EXERCISE", true, (String) pr.context()));
      } else {
        count(sts.test("Welfare Comments first word is EXERCISE", false, (String) pr.context()));
      }
      summary.welfareIsExercise = pr.value();

      count(sts.test("Welfare Comments provided", true));
      summary.welfareMessageText = m.getDataAsString(DataType.FORM_MESSAGE);
    } else {
      summary.welfareIsExercise = "(null)";
      summary.welfareMessageText = "";
      count(sts.test("Welfare Comments provided", false));
    }

    summary.welfareType = m.getDataAsString(DataType.MESSAGE_TYPE);
    summary.welfareStatus = m.getDataAsString(DataType.MESSAGE_STATUS);
    summary.welfareMyStatus = m.getDataAsString(DataType.MY_STATUS);

    count(sts.test("Welfare Message line 1 should be EXERCISE", summary.welfareIsExercise.equals("YES"), comments));
    count(sts.test("Welfare Type should be EXERCISE", "EXERCISE", summary.welfareType));
    count(sts.test("Welfare Message Status should be #EV", "INITIAL", summary.welfareStatus));

    getCounter("Welfare Type").increment(summary.welfareType);
    getCounter("Welfare Status").increment(summary.welfareStatus);
    getCounter("Welfare My Status").increment(summary.welfareMyStatus);

    // #MM update summary
    summary.welfareMessage = m;
    summary.messageIds += "\nwelfare: " + m.messageId;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_Ics213Message(Summary summary, Ics213Message m) {
    sts.setExplanationPrefix("(ics_213) ");
    accumulateAddresses("ICS-213 addresses", m);

    count(sts.test("ICS_213 Incident Name should be #EV", "TSUNAMI", m.incidentName));
    count(sts.test("ICS-213 Form To should be #EV", "TSUNAMI;ETO-DRILL", m.formTo));

    var ev = "Flood Zone Risk for Schools in";
    count(sts.testStartsWith("ICS-213 Subject should start with #EV", ev, m.formSubject));

    var comments = m.formMessage;
    if (!isNull(comments)) {
      var cp = new ContentParser(comments);
      var lines = cp.getLines();
      count(sts.test("ICS-213 messsage lines should be #EV", "2", String.valueOf(lines.length)));

      if (lines.length >= 1) {
        var line1 = lines[0];
        var fields = line1.split(",");
        summary.ics213_school1_risk = fields[fields.length - 1];
        getCounter("ICS-213 School Risk").increment(summary.ics213_school1_risk.toLowerCase());
      }

      if (lines.length >= 2) {
        var line2 = lines[1];
        var fields = line2.split(",");
        summary.ics213_school2_risk = fields[fields.length - 1];
        getCounter("ICS-213 School Risk").increment(summary.ics213_school2_risk.toLowerCase());
      }

      count(sts.test("ICS-213 Comments parsed", true));
    } else {
      summary.ics213_school1_risk = "";
      summary.ics213_school2_risk = "";
      count(sts.test("ICS-213 Comments parsed", false));
    }

    getCounter("ICS-213 Data Source").increment(m.dataSource);

    // #MM update summary
    summary.ics213Message = m;
    summary.messageIds += "\nics_213: " + m.messageId;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_CheckOutMessage(Summary summary, CheckOutMessage m) {
    sts.setExplanationPrefix("(check_out) ");
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
      if (lines.length >= 3) {
        var sb = new StringBuilder();
        var fields = lines[2].split(":");
        if (fields.length >= 2) {
          for (var iField = 1; iField < fields.length; ++iField) {
            sb.append(fields[iField].strip() + " ");
          }
          sb.append("\n");
        } else {
          sb.append(lines[2] + "\n");
        }
        summary.coMostImportantThing = sb.toString().strip();
        senderMostImportantThingMap.put(m.from, summary.coMostImportantThing);
      }

      count(sts.test("Check Out Comments parsed", true));
    } else {
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
    summary.messageIds += "\ncheck out: " + m.messageId;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_Ics214Message(Summary summary, Ics214Message m) {
    sts.setExplanationPrefix("(ics_214) ");
    accumulateAddresses("ICS-214 addresses", m);

    summary.ics214Position = m.selfResource.icsPosition();
    summary.ics214ResourceCount = String.valueOf(m.assignedResources.stream().filter(r -> !r.isEmpty()).count());
    summary.ics214ActivityCount = String.valueOf(m.activities.stream().filter(r -> !r.isEmpty()).count());

    count(sts.test("ICS-214 Incident Name should be #EV", "TSUNAMI", m.incidentName));

    getCounter("ICS-214 Position").increment(m.selfResource.icsPosition());
    getCounter("ICS-214 Resources").increment(summary.ics214ResourceCount);
    getCounter("ICS-214 Activities").increment(summary.ics214ActivityCount);

    var dtf = new MultiDateTimeParser(List.of("yyyy-MM-dd HH:mm"));
    final var prefix = "ICS-214 Activity date/time should be on or ";
    for (var activity : m.activities) {
      var dtString = activity.dateTimeString();
      try {
        var dt = dtf.parse(dtString);
        count(sts.testOnOrAfter(prefix + "after #EV", windowOpenDT, dt, ICS_214_DT_FORMATTER));
        count(sts.testOnOrBefore(prefix + "before #EV", windowCloseDT, dt, ICS_214_DT_FORMATTER));
      } catch (Exception e) {
        logger.warn("### could not parse activity date/time: " + dtString + " for call: " + m.from);
      }
    }

    var activitiesList = m.activities.stream().map(a -> a.activities()).toList();
    var activitiesString = String.join(",", activitiesList).toUpperCase();
    if (summary.dyfiMessage != null) {
      var hasDYFI = activitiesString.contains("DYFI");
      getCounter("ICS-214 activities has DYFI").increment(hasDYFI);
    }

    if (summary.checkInMessage != null) {
      var hasCheckIn = activitiesString.contains("CHECK IN") || activitiesString.contains("CHECK-IN");
      getCounter("ICS-214 activities has Check-In").increment(hasCheckIn);
    }

    if (summary.welfareMessage != null) {
      var hasWelfare = activitiesString.contains("WELFARE");
      getCounter("ICS-214 activities has Welfare").increment(hasWelfare);
    }

    if (summary.ics213Message != null) {
      var hasIcs213 = activitiesString.contains("ICS 213") || activitiesString.contains("ICS-213");
      getCounter("ICS-214 activities has ICS-213").increment(hasIcs213);
    }

    if (summary.bulletinMessage != null) {
      var hasBulletin = activitiesString.contains("BULLETIN");
      getCounter("ICS-214 activities has Bulletin").increment(hasBulletin);
    }

    if (summary.checkOutMessage != null) {
      var hasCheckOut = activitiesString.contains("CHECK OUT") || activitiesString.contains("CHECK-OUT");
      getCounter("ICS-214 activities has Check-Out").increment(hasCheckOut);
    }

    // #MM update summary
    summary.ics214Message = m;
    summary.messageIds += "\nics 214: " + m.messageId;
    ++summary.messageCount;
    isPerfectMessage(m);
  }

  private void handle_BulletinMessage(Summary summary, BulletinMessage m) {
    sts.setExplanationPrefix("(bulletin) ");
    accumulateAddresses("bulletin addresses", m);

    count(sts.test("Bulletin For should be #EV", "Tsunami Exercise Net", m.forNameGroup));
    count(sts.test("Bulletin Form From should contain call sign", m.fromNameGroup.contains(m.from)));
    count(sts.test("Bulletin Precedence should be #EV", "Routine", m.precedence));
    getCounter("Bulletin Precedence").increment(m.precedence);
    count(sts.testContains("Bulletin Subject should contain #EV", "TSUNAMI EXERCISE NET BULLETIN", m.formSubject));
    count(sts.testStartsWith("Bulletin message should start with #EV", "THIS IS AN EXERCISE", m.bulletinText));

    // #MM update summary
    summary.bulletinMessage = m;
    ++summary.messageCount;
    summary.bulletinFor = m.forNameGroup;
    summary.bulletinFrom = m.fromNameGroup;
    summary.bulletinPrecedence = m.precedence;
    summary.bulletinSubject = m.formSubject;
    summary.bulletinIsExercise = String.valueOf(m.bulletinText.toUpperCase().startsWith("THIS IS AN EXERCISE"));
    summary.messageIds += "\nbulletin: " + m.messageId;
    isPerfectMessage(m);
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("(summary) ");

    var summary = (Summary) summaryMap.get(sender); // #MM

    summary.allGroups = String.join(",", senderGroupSet);
    senderGroupSet.stream().forEach(gn -> getCounter("Summary Group Affiliation").increment(gn.toUpperCase()));

    sts.testNotNull("DYFI message not received", summary.dyfiMessage);
    sts.testNotNull("Check In message not received", summary.checkInMessage);
    sts.testNotNull("Welfare message not received", summary.welfareMessage);
    sts.testNotNull("Ics-213 message not received", summary.ics213Message);
    sts.testNotNull("Bulletin message not received", summary.bulletinMessage);
    sts.testNotNull("Check Out message not received", summary.checkOutMessage);
    sts.testNotNull("Ics-214 message not received", summary.ics214Message);

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

    @SuppressWarnings("unchecked")
    var summaries = new ArrayList<Summary>((Collection<Summary>) (Object) summaryMap.values());
    makeMaps(summaries);

  }

  private void makeMaps(List<Summary> summaries) {
    makeMessageCountMap(summaries);
    makeDyfiGroupCountMap(summaries);

    makeTrinaryMap(summaries, (s -> s.dyfiMessage == null), // Predicate<Summary> isUnknownPredicate, //
        (s -> !s.dyfiMessage.isRealEvent), // Predicate<Summary> isGoodPredicate, //
        null, // Function<Summary, String> popupFunction, //
        null, // List<String> legendNames, //
        true, // boolean doUnknownStatus, //
        "DYFI Is Exercise", // String legendName, //
        "DYFI Is Exercise", // String attributeName, //
        "DYFI-IsExercise" // String fileName) {
    );

    makeTrinaryMap(summaries, (s -> s.dyfiMessage == null), // Predicate<Summary> isUnknownPredicate, //
        (s -> !s.dyfiMessage.isFelt), // Predicate<Summary> isGoodPredicate, //
        null, // Function<Summary, String> popupFunction, //
        null, // List<String> legendNames, //
        true, // boolean doUnknownStatus, //
        "DYFI Is Felt", // String legendName, //
        "DYFI Is Felt", // String attributeName, //
        "DYFI-IsFelt" // String fileName) {
    );

    makeTrinaryMap(summaries, (s -> s.dyfiMessage == null), // Predicate<Summary> isUnknownPredicate, //
        (s -> s.dyfiMessage.intensity != null && Integer.valueOf(s.dyfiMessage.intensity) >= 5), //
        (s -> "intensity: " + s.dyfiMessage.intensity), // Function<Summary, String> popupFunction, //
        null, // List<String> legendNames, //
        true, // boolean doUnknownStatus, //
        "DYFI Intensity >= 5", // String legendName, //
        "DYFI Intensity >= 5", // String attributeName, //
        "DYFI-IntensityIsEnough" // String fileName) {
    );

    makeTrinaryMap(summaries, (s -> s.checkInMessage == null), // Predicate<Summary> isUnknownPredicate, //
        (s -> s.ciInTsunamiZone.toUpperCase().equals("NO")), //
        null, // Function<Summary, String> popupFunction, //
        List.of("Out of Zone", "Unknown", "In Zone"), // List<String> legendNames, //
        true, // boolean doUnknownStatus, //
        "In Tsunami Zone", // String legendName, //
        "", // String attributeName, //
        "InTsunamiZone" // String fileName) {
    );

    makeTrinaryMap(summaries, (s -> s.checkInMessage == null), // Predicate<Summary> isUnknownPredicate, //
        (s -> s.ciInFloodZone.toUpperCase().equals("NO")), //
        null, // Function<Summary, String> popupFunction, //
        List.of("Out of Zone", "Unknown", "In Zone"), // List<String> legendNames, //
        true, // boolean doUnknownStatus, //
        "In Flood Zone", // String legendName, //
        "", // String attributeName, //
        "InFloodZone" // String fileName) {
    );
  }

  private void makeMessageCountMap(List<Summary> summaries) {
    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);
    var nLayers = 7;
    var myGradientMap = new MapService(cm, mm).makeGradientMap(0, 120, nLayers);

    var messageCountMap = new HashMap<Integer, Integer>();
    for (var summary : summaries) {
      var key = summary.messageCount;
      var value = messageCountMap.getOrDefault(key, Integer.valueOf(0));
      ++value;
      messageCountMap.put(key, value);
    }

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    final var lastColorMapIndex = myGradientMap.size() - 1;
    final var lastColor = gradientMap.get(lastColorMapIndex);
    for (var s : summaries) {
      var count = s.messageCount;
      var location = s.location;
      var color = myGradientMap.getOrDefault(count, lastColor);
      var prefix = "<b>" + s.from + "</b><hr>";
      var content = prefix //
          + "Message Count: " + count + "\n" //
          + "Feedback Count: " + s.getFeedbackCountString() + "\n" //
          + "Feedback: " + s.getFeedback();

      var mapEntry = new MapEntry(s.from, s.to, location, content, color);
      mapEntries.add(mapEntry);
    }

    var layers = new ArrayList<MapLayer>();
    var countLayerNameMap = new HashMap<Integer, String>();
    for (var i = 1; i <= nLayers; ++i) {
      var value = String.valueOf(i);
      var count = messageCountMap.getOrDefault(i, Integer.valueOf(0));
      var layerName = "# messages: " + value + ", count: " + count;
      countLayerNameMap.put(i, layerName);

      var color = myGradientMap.get(i - 1);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var legendTitle = "TSUNAMI 2026 Message Counts (" + mapEntries.size() + " total)";
    var context = new MapContext(outputPath, //
        dateString + "-map-MessageCount", // file name
        dateString + " Message Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  private void makeDyfiGroupCountMap(List<Summary> summaries) {
    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);
    var desiredLayers = 10;

    var groupCallListMap = new HashMap<String, List<String>>();
    for (var summary : summaries) {
      if (summary.dyfiMessage == null || summary.dyfiGroup == null) {
        continue;
      }
      var group = summary.dyfiGroup;
      var list = groupCallListMap.getOrDefault(group, new ArrayList<String>());
      var call = summary.from;
      list.add(call);
      groupCallListMap.put(group, list);
    }

    var groupSizeGroupListMap = new TreeMap<Integer, List<String>>();
    var groups = new ArrayList<String>(groupCallListMap.keySet());
    for (var group : groups) {
      var callList = groupCallListMap.get(group);
      var groupSize = callList.size();
      var groupList = groupSizeGroupListMap.getOrDefault(groupSize, new ArrayList<String>());
      groupList.add(group);
      groupSizeGroupListMap.put(groupSize, groupList);
    }

    groups.clear();
    for (var groupSize : groupSizeGroupListMap.descendingKeySet()) {
      var groupList = groupSizeGroupListMap.get(groupSize);
      groups.addAll(groupList);
      if (groups.size() >= desiredLayers) {
        break;
      }
    }

    var rng = new Random(2025);
    var colorBag = new RenewableBag<>(IMapService.etoColorMap.values(), rng);
    var groupColorMap = new HashMap<String, String>();
    for (var group : groups) {
      var color = colorBag.next();
      groupColorMap.put(group, color);
    }

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    for (var summary : summaries) {
      if (summary.dyfiMessage == null || summary.dyfiGroup == null) {
        continue;
      }
      var group = summary.dyfiGroup;
      var callList = groupCallListMap.get(group);
      var location = summary.location;
      var color = groupColorMap.get(group);
      var prefix = "<b>" + summary.from + "</b><hr>";
      var content = prefix //
          + "Group Name: " + group + "\n" //
          + "Group Size: " + callList.size() + "\n";
      var mapEntry = new MapEntry(summary.from, null, location, content, color);
      mapEntries.add(mapEntry);
    }

    var layers = new ArrayList<MapLayer>(groups.size());
    for (var group : groups) {
      var callList = groupCallListMap.get(group);
      var count = callList.size();
      var layerName = "group: " + group + ", size: " + count;
      var color = groupColorMap.get(group);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var legendTitle = "TSUNAMI 2026 Group Counts (" + summaries.size() + " total)";
    var context = new MapContext(outputPath, //
        dateString + "-map-GroupCounts", // file name
        dateString + " Group Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  private enum MapStatus {
    Green, Yellow, Red
  }

  private void makeTrinaryMap(List<Summary> summaries, //
      Predicate<Summary> isUnknownPredicate, //
      Predicate<Summary> isGoodPredicate, //
      Function<Summary, String> popupFunction, //
      List<String> legendNames, //
      boolean doUnknownStatus, //
      String legendName, //
      String attributeName, //
      String fileName) {

    var mapService = new MapService(cm, mm);
    var statusColorMap = Map
        .of(MapStatus.Green, IMapService.rgbMap.get("green"), //
            MapStatus.Yellow, IMapService.rgbMap.get("yellow"), //
            MapStatus.Red, IMapService.rgbMap.get("red") //
        );
    var statusCountMap = new HashMap<MapStatus, Integer>();

    var mapEntries = new ArrayList<MapEntry>();
    for (var summary : summaries) {
      MapStatus status = null;
      if (isUnknownPredicate.test(summary)) {
        status = MapStatus.Yellow;
      } else if (isGoodPredicate.test(summary)) {
        status = MapStatus.Green;
      } else {
        status = MapStatus.Red;
      }
      var color = statusColorMap.get(status);
      var count = statusCountMap.getOrDefault(status, Integer.valueOf(0));
      ++count;
      statusCountMap.put(status, count);
      var popupText = "<b>" + summary.from + "</b><hr>";
      if (popupFunction != null) {
        popupText = popupText + popupFunction.apply(summary);
      }
      mapEntries.add(new MapEntry(summary.from, null, summary.location, popupText, color));
    }

    if (legendNames == null) {
      legendNames = List.of("Correct", "Unknown", "Incorrect");
    }
    var layers = new ArrayList<MapLayer>();
    for (var status : MapStatus.values()) {
      var count = statusCountMap.getOrDefault(status, Integer.valueOf(0));
      var color = statusColorMap.get(status);
      var ordinal = status.ordinal();
      var prefix = legendNames.get(ordinal);
      if (status == MapStatus.Yellow && !doUnknownStatus) {
        continue;
      }
      layers.add(new MapLayer(prefix + " " + attributeName + " messages, count: " + count, color));
    }
    var legendTitle = "TSUNAMI 2026 " + legendName + " Counts (" + summaries.size() + " total)";

    if (fileName == null || fileName.isBlank()) {
      var fields = legendName.split(" ");
      fields[0] = fields[0].toLowerCase();
      fileName = String.join("", fields);
    }
    var context = new MapContext(outputPath, //
        dateString + "-map-" + fileName, // file name
        dateString + " " + legendName + "  correct", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
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
