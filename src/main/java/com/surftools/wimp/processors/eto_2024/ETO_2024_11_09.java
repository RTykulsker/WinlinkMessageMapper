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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.processors.std.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-11-09: Semi-annual drill: Winlink Check, 4 sequential FSR and a generated ICS-309
 *
 * @author bobt
 *
 */
public class ETO_2024_11_09 extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_11_09.class);

  private static record FsrKey(String from, long dayIndex) {
  };

  private static final int N_FSR_DAYS = 4;
  private Map<FsrKey, FieldSituationMessage> callDayFsrMap = new HashMap<>();
  private static final String FSR_FIRST_DATE_STRING = "2024-11-05";
  private static final DateTimeFormatter FSR_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter FSR_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm'L'");
  private static final DateTimeFormatter FSR_DATE_TIME_FORMATTOR = DateTimeFormatter
      .ofPattern("yyyy-MM-dd  HH:mm:ss'L'"); // 2024-09-19
                                             // 23:17:07Z
  private static final LocalDate FSR_FIRST_DATE = LocalDate.parse(FSR_FIRST_DATE_STRING, FSR_DATE_FORMATTER);

  private class Summary extends BaseSummary {

    // we want these messages
    public int option;
    public boolean optionIsDefault;

    public CheckInMessage checkInMessage;
    public FieldSituationMessage[] fsrMessages = new FieldSituationMessage[N_FSR_DAYS];
    public LocalDateTime[] fsrDateTimes = new LocalDateTime[N_FSR_DAYS];
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
                  "FSR Day 1", "FSR Day 2", "FSR Day 3", "FSR Day 4", //
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
                  mId(ics309Message), String.valueOf(fsrMessageCount)//
              }));
      return list.toArray(new String[0]);
    };
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Ics309Message.setNDisplayActivities(6);

    var acceptableMessageTypesList = List
        .of( // order matters, last location wins,
            MessageType.CHECK_IN, MessageType.FIELD_SITUATION, MessageType.PDF_ICS_309);
    acceptableMessageTypesSet.addAll(acceptableMessageTypesList);
  }

  @Override
  protected void beforeProcessingForSender(String sender) {
    super.beforeProcessingForSender(sender);

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
      handle_FieldSituationMessage(summary, (FieldSituationMessage) message);
    } else if (type == MessageType.ICS_309) {
      handle_Ics309Message(summary, (Ics309Message) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }

    // last valid location wins; order of messageTypes matters
    iSummary.location = (message.mapLocation != null && message.mapLocation.isValid()) ? message.mapLocation
        : LatLongPair.INVALID;
    iSummary.explanations.addAll(sts.getExplanations());

    summaryMap.put(sender, iSummary);
  }

  private void handle_Ics309Message(Summary summary, Ics309Message m) {
    // summary.Ics309Message = m;
    count(sts.test("ICS-309 task number should be #EV", "01 Sep", m.taskNumber));
    count(sts.test("ICS-309 task name should be #EV", "RRI Welfare Message Exercise", m.taskName));
    count(sts.test("ICS-309 operationsal period should be #EV", "191500-201500 UTC Sep 24", m.operationalPeriod));
    count(sts.testIfPresent("Operator Name should be present", m.operatorName));
    count(sts.testIfPresent("Station ID should be present", m.stationId));

    var activitiesSubjectSet = m.activities.stream().map(a -> a.subject()).collect(Collectors.toSet());

  }

  private void handle_CheckInMessage(Summary summary, CheckInMessage m) {
    summary.checkInMessage = m;

    var option = 1;
    var optionIsDefault = true;
    var comments = m.comments;
    if (comments != null) {
      var fields = comments.split("\\w");
      if (fields.length >= 2) {
        try {
          option = Integer.parseInt(fields[1]);
          if (fields[0].equalsIgnoreCase("OPTION") && option >= 1 && option <= 8) {
            optionIsDefault = false;
          } else {
            option = 1;
          }
        } catch (Exception e) {
        }
      } // fields.length >= 2
    }

    logger.debug("sender: " + m.from + ", option: " + option + ", is default: " + optionIsDefault);
    var debug = true;

    // TODO fixme
    summary.option = option;
    summary.optionIsDefault = optionIsDefault;
  }

  private void handle_FieldSituationMessage(Summary summary, FieldSituationMessage m) {
    // TODO TEST THIS!!!
    var isValidDate = false;
    String explanation = "";
    var formDateTimeString = m.formDateTime;
    long daysBetween = -1;
    if (formDateTimeString == null) {
      explanation = "FSR DATE/TIME is null";
    } else {
      try {
        var fields = formDateTimeString.split(" ");
        if (fields != null && fields.length >= 2) {
          var dateString = fields[0];
          var formDate = LocalDate.parse(dateString, FSR_DATE_FORMATTER);

          daysBetween = ChronoUnit.DAYS.between(FSR_FIRST_DATE, formDate);
          if (daysBetween < 0) {
            explanation = "FSR DATE/TIME is before " + FSR_FIRST_DATE_STRING;
          } else if (daysBetween >= N_FSR_DAYS) {
            var lastDate = FSR_FIRST_DATE.plusDays(3);
            explanation = "FSR DATE/TIME is afer " + FSR_DATE_FORMATTER.format(lastDate);
          } else {
            isValidDate = true;
          }
        }
      } catch (Exception e) {
        explanation = "FSR DATE/TIME could not be parsed";
      }
    }

    count(sts.test("Form DATE/TIME value", isValidDate, formDateTimeString, explanation));

    var isDirty = false;
    if (isValidDate) {
      var fsrKey = new FsrKey(m.from, daysBetween);
      var cachedMessage = callDayFsrMap.get(fsrKey);
      if (cachedMessage == null) {
        isDirty = true;
      } else {
        var cachedDateTime = LocalDateTime.parse(cachedMessage.formDateTime, FSR_DATE_TIME_FORMATTOR);
        var formDateTime = LocalDateTime.parse(m.formDateTime, FSR_DATE_TIME_FORMATTOR);
        if (formDateTime.isBefore(cachedDateTime)) {
          // this should not happen
          logger.warn("fsr from: " + m.from + ", mId: " + m.messageId + ", d/t: " + m.formDateTime + //
              " is before mId: " + cachedMessage.messageId + ", d/t: " + cachedMessage.formDateTime);
        }
      }

      if (isDirty) {
        callDayFsrMap.put(fsrKey, m);
      }
    }
  }

  // TODO delete

  // @Override
  // protected void beforeCommonProcessing(String sender) {
  // // TODO Auto-generated method stub
  // var summary = summaryMap.get(sender, new Summary(sender));
  // }
  //

  @Override
  protected void endProcessingForSender(String sender) {
    var summary = (Summary) summaryMap.get(sender);

    if (summary == null) {
      return;
    }

    if (summary.checkInMessage == null) {
      summary.explanations.add("No CheckIn message received.");
    }

    for (var i = 0; i < N_FSR_DAYS; ++i) {
      if (summary.fsrMessages[i] == null) {
        summary.explanations.add("No FSR Day " + (i + i) + " message received.");
      }
    }

    if (summary.ics309Message == null) {
      summary.explanations.add("No ICS-309 message received.");
    }

    // TODO other inter-message relationships

    // TODO histograms for this? VirtualMessage for histograms? globalCounter
    // exerciseCounter, ExerciseMessage
    // SummaryMessage

    summaryMap.put(sender, summary);
  }

  @Override
  public void postProcess() {
    // don't do any outbound messaging for individual messageTypes
    var cachedOutboundMessaging = doOutboundMessaging;
    doOutboundMessaging = false;
    outboundMessageList.clear();

    super.postProcess();

    // fix bad locations
    var badLocationSenders = new ArrayList<String>();
    var summaries = summaryMap.values();
    for (var iSummary : summaries) {
      var summary = (Summary) iSummary;
      if (!summary.location.isValid()) {
        badLocationSenders.add(summary.from);
      }
    }

    if (badLocationSenders.size() > 0) {
      logger
          .info("adjusting lat/long for " + badLocationSenders.size() + " summaries: "
              + String.join(",", badLocationSenders));
      var newLocations = LocationUtils.jitter(badLocationSenders.size(), LatLongPair.ZERO_ZERO, 10_000);
      for (int i = 0; i < badLocationSenders.size(); ++i) {
        var sender = badLocationSenders.get(i);
        var summary = (Summary) summaryMap.get(sender);
        summary.location = newLocations.get(i);
        summaryMap.put(sender, summary);
      }
    }

    // write outbound messages, but only for summary; change subject
    // TODO fixme
    // if (cachedOutboundMessaging) {
    // setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
    // for (var summary : summaryMap.values()) {
    // var outboundMessageFeedback = (summary.explanations.size() == 0) ? "Perfect messages!"
    // : String.join("\n", summary.explanations) + OB_DISCLAIMER;
    // var outboundMessage = new OutboundMessage(outboundMessageSender, sender,
    // "Feedback on ETO " + cm.getAsString(Key.EXERCISE_DATE) + " exercise", //
    // outboundMessageFeedback, null);
    // outboundMessageList.add(outboundMessage);
    //
    // var service = new OutboundMessageService(cm);
    // outboundMessageList = service.sendAll(outboundMessageList);
    // writeTable("outBoundMessages.csv", new ArrayList<IWritableTable>(outboundMessageList));
    // }
    // }

  }

}
