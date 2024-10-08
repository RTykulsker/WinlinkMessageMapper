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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.FieldSituationMessage.ResourceType;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.processors.std.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-11-09: Semi-annual drill: Winlink Check In, 5 sequential FSR and a generated ICS-309
 *
 * @author bobt
 *
 */
public class ETO_2024_11_09 extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_11_09.class);

  private static final LocalDate FSR_FIRST_DATE = LocalDate.of(2024, 11, 5);
  private static final DateTimeFormatter FSR_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final int N_FSR_DAYS = 5;

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

    // we want these messages
    public int option;

    public CheckInMessage checkInMessage;
    public FieldSituationMessage[] fsrMessages = new FieldSituationMessage[N_FSR_DAYS + 1];
    public boolean[] fsrIsValid = new boolean[N_FSR_DAYS + 1];
    public Ics309Message ics309Message;

    public int checkInCount;
    public int validFsrMessageCount;
    public int totalFsrCount; // includes unexpected, does not include superseded
    public int ics309Count;
    public int totalValidCount; // all messageTypes

    public int exerciseMessagesNotInIcs309; // checkIn, last FSR: should be zero
    public int Ics309MessagesNotInExercise; // ICS-309 should only contain exercise messages

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
              .asList(new String[] { "Option", "Check In", //
                  "FSR Day 1", "FSR Day 2", "FSR Day 3", "FSR Day 4", "FSR Day 5", "ICS-309", //
                  "# CheckIn", "# valid FSR", "# all FSR", "# ICS-309", "# valid Messages", //
                  "Ex Msgs not in 309", "309 Msgs not in Ex"//
              }));
      return list.toArray(new String[0]);
    }

    private String mId(ExportedMessage m) {
      return m == null ? "" : m.messageId;
    }

    private String s(int i) {
      return String.valueOf(i);
    }

    private String optionName(int option) {
      switch (option) {
      case 0:
        return "No CheckIn received";
      case -1:
        return "No Option in CheckIn";
      case -2:
        return "Unparsable Option in CheckIn";
      default:
        return String.valueOf(option);
      }
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(Arrays
              .asList(new String[] { optionName(option), //
                  mId(checkInMessage), //
                  mId(fsrMessages[1]), mId(fsrMessages[2]), mId(fsrMessages[3]), mId(fsrMessages[4]),
                  mId(fsrMessages[5]), mId(ics309Message), //
                  s(checkInCount), s(validFsrMessageCount), s(totalFsrCount), s(ics309Count), s(totalValidCount), //
                  s(exerciseMessagesNotInIcs309), s(Ics309MessagesNotInExercise) }));
      return list.toArray(new String[0]);
    };
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Ics309Message.setNDisplayActivities(6);

    // #MM must define acceptableMessages
    var acceptableMessageTypesList = List
        .of( // order matters, last location wins,
            MessageType.CHECK_IN, MessageType.FIELD_SITUATION, MessageType.PDF_ICS_309);
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
    if (type == MessageType.CHECK_IN) {
      handle_CheckInMessage(summary, (CheckInMessage) message);
    } else if (type == MessageType.FIELD_SITUATION) {
      accumulate_FieldSituationMessage(summary, (FieldSituationMessage) message);
    } else if (type == MessageType.ICS_309) {
      handle_Ics309Message(summary, (Ics309Message) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_Ics309Message(Summary summary, Ics309Message m) {
    count(sts.test("ICS-309 task number should be #EV", "241109", m.taskNumber));
    count(sts.test("ICS-309 task name should be #EV", "Progression of Field Situation Reports Drill", m.taskName));
    count(sts.testIfPresent("ICS-309 operational period should be present", m.operationalPeriod));
    count(sts.testIfPresent("Operator Name should be present", m.operatorName));
    count(sts.testIfPresent("Station ID should be present", m.stationId));

    summary.ics309Message = m;
    ++summary.ics309Count;
    ++summary.totalValidCount;
  }

  private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
    summary.checkInMessage = m;

    var option = parseOptionFromCommentString(m.comments);
    count(sts.test("OPTION (via comments) should be readable", option >= 1, m.comments));

    // #MM update summary
    summary.option = option;
    summary.checkInMessage = m;
    ++summary.checkInCount;
    ++summary.totalValidCount;
  }

  /**
   * we can only accumulate the last fsr for a given date, since there can be multiple messages for the same day
   *
   * @param summary
   * @param m
   */
  private void accumulate_FieldSituationMessage(Summary summary, FieldSituationMessage m) {
    final var parsers = List
        .of(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'L'"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm'L'"));
    var isValidDate = false;
    String explanation = "";
    var formDateTimeString = m.formDateTime;

    long daysBetween = -1;
    if (formDateTimeString == null) {
      explanation = "FSR DATE/TIME is null";
    } else {
      try {
        var formDateTime = parse(formDateTimeString, parsers);
        var formDate = formDateTime.toLocalDate();

        daysBetween = ChronoUnit.DAYS.between(FSR_FIRST_DATE, formDate);
        if (daysBetween < 0) {
          explanation = "FSR DATE/TIME is before " + FSR_DATE_FORMATTER.format(FSR_FIRST_DATE);
        } else if (daysBetween >= N_FSR_DAYS) {
          var lastDate = FSR_FIRST_DATE.plusDays(N_FSR_DAYS - 1);
          explanation = "FSR DATE/TIME is afer " + FSR_DATE_FORMATTER.format(lastDate);
        } else {
          isValidDate = true;
        }

        int dayIndex = ((int) daysBetween) + 1;
        summary.fsrMessages[dayIndex] = m;
      } catch (Exception e) {
        explanation = "FSR DATE/TIME could not be parsed";
      }
    }

    count(sts.test("Form DATE/TIME value", isValidDate, formDateTimeString, explanation));
  }

  @Override
  protected void endProcessingForSender(String sender) {
    var summary = (Summary) summaryMap.get(sender); // #MM

    if (summary.checkInMessage == null) {
      summary.explanations.add("No CheckIn message received.");
    }

    for (var i = 1; i <= N_FSR_DAYS; ++i) {
      if (summary.fsrMessages[i] == null) {
        summary.explanations.add("No FSR Day " + i + " message received.");
      } else {
        handle_fsr(summary, i);
      }
    }

    if (summary.ics309Message == null) {
      summary.explanations.add("No ICS-309 message received.");
    }

    if (summary.ics309Message != null) {
      var activitiesSubjectSet = summary.ics309Message.activities
          .stream()
            .map(a -> a.subject())
            .collect(Collectors.toSet());
      summary.Ics309MessagesNotInExercise = activitiesSubjectSet.size();

      for (int i = 1; i <= N_FSR_DAYS; ++i) {
        var fsr = summary.fsrMessages[i];
        if (fsr != null && summary.fsrIsValid[i]) {
          var fsrSubject = fsr.subject;
          var isContained = activitiesSubjectSet.contains(fsrSubject);
          count(sts.test("FSR day " + (i) + " message should be in ICS-309 activities", isContained));
          summary.exerciseMessagesNotInIcs309 += isContained ? 0 : 1;
          summary.Ics309MessagesNotInExercise -= isContained ? 1 : 0;
        } else {
          ;
        }
      }

      if (summary.checkInMessage != null) {
        var isContained = activitiesSubjectSet.contains(summary.checkInMessage.subject);
        count(sts.test("Check-In message should be in ICS-309 activities", isContained));
        summary.exerciseMessagesNotInIcs309 += isContained ? 0 : 1;
        summary.Ics309MessagesNotInExercise -= isContained ? 1 : 0;
      } else {
        count(sts.test("Check-In message should be in ICS-309 activities", false));
        ++summary.exerciseMessagesNotInIcs309;
      }

    }

    checkAscendingCreationTimestamps(summary);

    summaryMap.put(sender, summary); // #MM
  }

  record MX(ExportedMessage m, String explanation) {
  };

  private void checkAscendingCreationTimestamps(Summary summary) {
    var list = new LinkedList<MX>();
    if (summary.checkInMessage != null) {
      list.add(new MX(summary.checkInMessage, "Check In"));
    }

    for (var iDay = 1; iDay <= N_FSR_DAYS; ++iDay) {
      if (summary.fsrMessages[iDay] != null && summary.fsrIsValid[iDay]) {
        list.add(new MX(summary.fsrMessages[iDay], "FSR Day " + iDay));
      }
    }

    if (summary.ics309Message != null) {
      list.add(new MX(summary.ics309Message, "ICS-309"));
    }

    while (list.size() > 1) {
      var first = list.remove(0);
      var second = list.get(0);

      var firstMsgDateTime = first.m.msgDateTime;
      var secondMsgDateTime = second.m.msgDateTime;
      var isBefore = firstMsgDateTime.isBefore(secondMsgDateTime);

      var explanation = first.explanation + "should have been created before " + second.explanation;
      count(sts.test("Messages should be created in sequence", isBefore, explanation));
    }

  }

  /**
   *
   * @param summary
   * @param iDay:
   *          1-based index
   */
  private void handle_fsr(Summary summary, int iDay) {
    var m = summary.fsrMessages[iDay];
    ++summary.totalFsrCount;
    var explanationPrefix = m.getMessageType().toString() + " (" + m.messageId + "): ";
    sts.setExplanationPrefix(explanationPrefix);

    var canSendMessage = canSendMessage(iDay, summary.option);
    var actualMessage = summary.fsrMessages[iDay] != null;
    var unexpectedMessageReceived = !canSendMessage && actualMessage;
    count(sts.test("Day " + iDay + " unexpected message received", !unexpectedMessageReceived));
    if (unexpectedMessageReceived) {
      return;
    }
    summary.fsrIsValid[iDay] = true;
    ++summary.validFsrMessageCount;
    ++summary.totalValidCount;

    count(sts.test("Agency Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Precedence should be #EV", "R/ Routine", m.precedence));
    count(sts.test("Task should be #EV", "241105", m.task));
    count(sts.test("EMERGENT/LIFE SAFETY Need should be #EV", "NO", m.isHelpNeeded));
    count(sts.testIfPresent("City should be present", m.city));

    var fsrOption = parseOptionFromCommentString(m.additionalComments);
    count(sts.test("FSR Comments/Option should match Check In Comments/Option", fsrOption == summary.option));

    var availableResourcesList = getResourcesForDayAndOption(iDay, summary.option);
    var availableResourceSet = new HashSet<FieldSituationMessage.ResourceType>(availableResourcesList);

    for (var resourceType : FieldSituationMessage.ResourceType.values()) {
      if (resourceType == ResourceType.NOAA_DEGRADED) {
        continue;
      }

      var resource = m.resourceMap.get(resourceType);
      var isResourceAvailable = availableResourceSet.contains(resourceType);
      if (isResourceAvailable) {
        count(sts.test("Day " + iDay + " " + resourceType + " status should be #EV", "YES", resource.status()));
        count(sts.testIfEmpty("Day " + iDay + " " + resourceType + " comments should be empty", resource.comments()));
      } else {
        count(sts.test("Day " + iDay + " " + resourceType + " status should be #EV", "NO", resource.status()));
        count(sts
            .testIfPresent("Day " + iDay + " " + resourceType + " comments NOT should be empty", resource.comments()));
      }
    }
  }

  private boolean canSendMessage(int iDay, int option) {
    if (iDay == 1 || iDay == 2 || iDay == 5) {
      return true;
    }

    if (iDay == 3 || iDay == 4) {
      if (option == 4 || option == 6 || option == 8) {
        return true;
      }
    }

    return false;
  }

  private List<FieldSituationMessage.ResourceType> getResourcesForDayAndOption(int iDay, int option) {
    var resources = new LinkedList<FieldSituationMessage.ResourceType>(Arrays.asList(ResourceType.values()));
    resources.remove(ResourceType.NOAA_DEGRADED);

    if (iDay == 1 || iDay == N_FSR_DAYS) {
      return resources;
    }

    if (iDay == 2) {
      resources.remove(ResourceType.COMMERCIAL_POWER_STABLE);
      return resources;
    }

    resources.clear(); // else days 3 and 4
    resources.add(ResourceType.WATER_WORKS);
    resources.add(ResourceType.NATURAL_GAS_SUPPLY);
    resources.add(ResourceType.NOAA_WEATHER_RADIO);
    var isEmergencyPowerAvailable = (option == 2 || option == 4 || option == 6 || option == 8);
    if (isEmergencyPowerAvailable) {
      resources.add(ResourceType.AM_FM_BROADCAST);
      resources.add(ResourceType.OTA_TV);
      resources.add(ResourceType.SATELLITE_TV);
      resources.add(ResourceType.WATER_WORKS);
      resources.add(ResourceType.NATURAL_GAS_SUPPLY);
      resources.add(ResourceType.NOAA_WEATHER_RADIO);
    }

    return resources;
  }

  private int parseOptionFromCommentString(String comments) {
    if (comments != null) {
      var fields = comments.split("\\w");
      if (fields.length >= 2 && fields[0].equalsIgnoreCase("OPTION")) {
        try {
          var option = Integer.parseInt(fields[1]);
          if (option >= 1 && option <= 8) {
            return option;
          }
        } catch (Exception e) {
          return -2;
        }
      } // fields.length >= 2 && OPTION
      return -2;
    } else {
      return -1;
    }
  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM
  }
}
