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
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.processors.std.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-11-09: Semi-annual drill: Winlink Check, 5 sequential FSR and a generated ICS-309
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
    public boolean optionIsDefault;

    public CheckInMessage checkInMessage;
    public FieldSituationMessage[] fsrMessages = new FieldSituationMessage[N_FSR_DAYS];
    public Ics309Message ics309Message;

    public int fsrMessageCount;

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
              .asList(new String[] { "Option", "Option Valid", "Check In", //
                  "FSR Day 1", "FSR Day 2", "FSR Day 3", "FSR Day 4", "FSR Day 5", //
                  "ICS-309", "FSR Count" //
              }));
      return list.toArray(new String[0]);
    }

    private String mId(ExportedMessage m) {
      return m == null ? "" : m.messageId;
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(Arrays
              .asList(new String[] { String.valueOf(option), String.valueOf(!optionIsDefault), //
                  mId(checkInMessage), //
                  mId(fsrMessages[0]), mId(fsrMessages[1]), mId(fsrMessages[2]), mId(fsrMessages[3]),
                  mId(fsrMessages[4]), mId(ics309Message), String.valueOf(fsrMessageCount)//
              }));
      return list.toArray(new String[0]);
    };
  }

  static record Option(int id, boolean hasPower) {
  };

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

    // we can do this here and now, since all FSR messages have been accumulated (but not otherwise processed)
    var activitiesSubjectSet = m.activities.stream().map(a -> a.subject()).collect(Collectors.toSet());
    for (int i = 0; i < N_FSR_DAYS; ++i) {
      var fsr = summary.fsrMessages[i];
      if (fsr != null) {
        var fsrSubject = fsr.subject;
        count(sts
            .test("FSR day " + (i + 1) + " message should be in ICS-309 activities",
                activitiesSubjectSet.contains(fsrSubject)));
      }
    }

    if (summary.checkInMessage != null) {
      count(sts
          .test("Check-In message should be in ICS-309 activities",
              activitiesSubjectSet.contains(summary.checkInMessage.subject)));
    } else {
      count(sts.test("Check-In message should be in ICS-309 activities", false));
    }

    // TODO in summary countOfExMessagesNotInIcs309, countOf309ActivitiesNotInEx, both should be ZERO!

    summary.ics309Message = m;
  }

  private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
    summary.checkInMessage = m;

    var option = getOptionIdFromString(m.comments);
    var optionIsDefault = false;
    if (option == -1) {
      option = 1;
      optionIsDefault = true;
    }

    count(sts.test("OPTION (via comments) should be readable", !optionIsDefault, m.comments));

    // #MM update summary
    summary.option = option;
    summary.optionIsDefault = optionIsDefault;
    summary.checkInMessage = m;
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

    ++summary.fsrMessageCount;

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

        int dayIndex = (int) daysBetween;
        summary.fsrMessages[dayIndex] = m;
      } catch (Exception e) {
        explanation = "FSR DATE/TIME could not be parsed";
      }
    }

    count(sts.test("Form DATE/TIME value", isValidDate, formDateTimeString, explanation));
  }

  @Override
  protected void endProcessingForSender(String sender) {
    var summary = (Summary) summaryMap.get(sender);

    if (summary.checkInMessage == null) {
      summary.explanations.add("No CheckIn message received.");
    }

    for (var i = 0; i < N_FSR_DAYS; ++i) {
      if (summary.fsrMessages[i] == null) {
        summary.explanations.add("No FSR Day " + (i + 1) + " message received.");
      } else {
        handle_fsr(summary, i);
      }
    }

    if (summary.ics309Message == null) {
      summary.explanations.add("No ICS-309 message received.");
    }

    // TODO other inter-message relationships

    // #MM
    summaryMap.put(sender, summary);
  }

  /**
   *
   * @param summary
   * @param i:
   *          0-based day index
   */
  private void handle_fsr(Summary summary, int i) {
    var m = summary.fsrMessages[i];
    int iDay = i + 1;

    // TODO validate all static fields
    count(sts.test("Agency Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Precedence should be #EV", "R/ Routine", m.precedence));
    count(sts.test("Task should be #EV", "241105", m.task));
    count(sts.test("EMERGENT/LIFE SAFETY Need should be #EV", "NO", m.isHelpNeeded));
    count(sts.testIfPresent("City should be present", m.city));

    var checkInOptionsString = summary.optionIsDefault ? summary.option + " (default)" : "" + summary.option;
    var additionalComments = m.additionalComments;
    var fsrOption = getOptionIdFromString(additionalComments);
    var fsrOptionString = fsrOption == -1 ? "1 (default)" : String.valueOf(fsrOption);
    count(sts
        .test("FSR Comments/Option should match Check In (#EV)", checkInOptionsString.equalsIgnoreCase(fsrOptionString),
            fsrOptionString));

    // TODO validate fields for given day
    // for example on day 1, everything works!
  }

  private int getOptionIdFromString(String comments) {
    if (comments != null) {
      var fields = comments.split("\\w");
      if (fields.length >= 2) {
        if (fields[0].equalsIgnoreCase("OPTION")) {
          try {
            var option = Integer.parseInt(fields[1]);
            if (option >= 1 && option <= 8) {
              return option;
            }
          } catch (Exception e) {
            ;
          }
        } // endif OPTION
      } // fields.length >= 2
    }
    return -1;
  }

  @Override
  public void postProcess() {
    // #MM
    super.postProcess();
  }
}
