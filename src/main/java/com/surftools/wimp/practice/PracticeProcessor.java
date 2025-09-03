/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.practice;

import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Ics205Message;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.AcknowledgementProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.service.kml.KmlService;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapHeader;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PracticeProcessor extends SingleMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PracticeProcessor.class);
  private LocalDate date;
  private String dateString;
  private MessageType messageType;
  private ExportedMessage referenceMessage;

  private List<Summary> summaries = new ArrayList<>();

  protected Map<String, String> ackTextMap;

  protected String nextInstructions;

  protected final List<String> clearinghouseList = new ArrayList<String>();

  private KmlService kmlService;

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

    dateString = cm.getAsString(Key.EXERCISE_DATE);
    date = LocalDate.parse(dateString);
    var ord = PracticeUtils.getOrdinalDayOfWeek(date);
    var dow = date.getDayOfWeek();
    if (dow != DayOfWeek.THURSDAY) {
      throw new RuntimeException("Exercise Date: " + dateString + " is NOT a THURSDAY, but is " + dow.toString());
    }

    var ordinalList = new ArrayList<Integer>(PracticeGeneratorTool.VALID_ORDINALS);
    Collections.sort(ordinalList);
    var ordinalLabels = ordinalList.stream().map(i -> PracticeUtils.getOrdinalLabel(i)).toList();
    if (!PracticeGeneratorTool.VALID_ORDINALS.contains(ord)) {
      throw new RuntimeException(
          "Exercise Date: " + dateString + " is NOT one of " + String.join(",", ordinalLabels) + " THURSDAYS");
    }

    messageType = PracticeGeneratorTool.MESSAGE_TYPE_MAP.get(ord);
    if (messageType == null) {
      throw new RuntimeException("No messageType for ordinal: " + ord);
    }

    referenceMessage = (ExportedMessage) mm.getContextObject(PracticeProcessorTool.REFERENCE_MESSAGE_KEY);

    super.initialize(cm, mm, logger);
    ackTextMap = (Map<String, String>) mm.getContextObject(AcknowledgementProcessor.ACK_TEXT_MAP);

    nextInstructions = (String) mm.getContextObject(PracticeProcessorTool.INSTRUCTIONS_KEY);

    for (var i = 1; i <= 9; ++i) {
      clearinghouseList.add("ETO-0" + i + "@winlink.org");
    }
    for (var extra : List.of("ETO-10", "ETO-BK", "ETO-CAN", "ETO-DX")) {
      clearinghouseList.add(extra + "@winlink.org");
    }

    kmlService = new KmlService("ETO Practice Exercise for " + dateString,
        "ETO Practice for " + dateString + " using " + messageType.toString() + " messages");
  }

  @Override
  protected void beginCommonProcessing(ExportedMessage m) {
    super.beginCommonProcessing(m);
    var addressesString = m.toList + "," + m.ccList;
    var addressesList = Arrays.asList(addressesString.split(","));

    count(sts.testList("Addresses should contain ETO-PRACTICE@winlink.org", addressesList, "ETO-PRACTICE@winlink.org"));

    var result = addressesList.stream().distinct().filter(clearinghouseList::contains).toList();
    var intersection = String.join(",", result);
    var pred = intersection.length() == 0;
    count(
        sts.test("To and Cc list should not contain \"monthly/training/clearinghouse\" addresses", pred, intersection));
  }

  @Override
  protected void specificProcessing(ExportedMessage m) {
    switch (messageType) {
    case ICS_213:
      handle_Ics213(m);
      break;
    case ICS_213_RR:
      handle_Ics213RR(m);
      break;
    case HICS_259:
      handle_Hics259(m);
      break;
    case ICS_205:
      handle_Ics205(m);
      break;
    case FIELD_SITUATION:
      handle_Fsr(m);
      break;
    default:
      throw new RuntimeException("unsupported messageType: " + messageType.toString() + " for sender: " + m.from);
    }

    var summary = new Summary(m);
    summaries.add(summary);

    var values = summary.getValues();
    var description = "Feedback count: " + values[6] + "\n\n" + values[7];
    kmlService.addPin(summary.location, summary.from, description);
  }

  private void handle_Ics213(ExportedMessage message) {
    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    var m = (Ics213Message) message;
    var ref = (Ics213Message) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", referenceMessage.subject, m.subject));

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", ref.incidentName, m.incidentName));
    count(sts.test("Form To should be #EV", ref.formTo, m.formTo));
    count(sts.test("Form From should be #EV", ref.formFrom, m.formFrom));
    count(sts.test("Form Subject should be #EV", ref.formSubject, m.formSubject));

    var formDateTime = LocalDateTime.parse(m.formDate + " " + m.formTime, dtf);
    count(sts.testOnOrAfter("Form Date and Time should be on or after #EV", windowOpenDT, formDateTime, dtf));
    count(sts.testOnOrBefore("Form Date and Time should be on or before #EV", windowCloseDT, formDateTime, dtf));

    count(sts.test("Message should be #EV", ref.formMessage, m.formMessage));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));
    count(sts.test("Position/Title should be #EV", ref.position, m.position));
  }

  private void handle_Ics213RR(ExportedMessage message) {
    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    var m = (Ics213RRMessage) message;
    var ref = (Ics213RRMessage) referenceMessage;

    count(sts.test_2line("Message Subject should be #EV", ref.subject, m.subject));

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("Incident Name should be #EV", ref.incidentName, m.incidentName));

    var formDateTime = LocalDateTime.parse(m.activityDateTime, dtf);
    count(sts.testOnOrAfter("Form Date and Time should be on or after #EV", windowOpenDT, formDateTime, dtf));
    count(sts.testOnOrBefore("Form Date and Time should be on or before #EV", windowCloseDT, formDateTime, dtf));

    count(sts.test("Resource Request Number should be #EV", ref.requestNumber, m.requestNumber));

    var maxLineItems = Math.min(ref.lineItems.size(), m.lineItems.size());
    if (m.lineItems.size() != ref.lineItems.size()) {
      logger
          .warn("### from: " + m.from + ",mId: " + m.messageId + ", m.items: " + m.lineItems.size() + ", ref.items: "
              + ref.lineItems.size());
    }
    for (var i = 0; i < maxLineItems; ++i) {
      var lineNumber = i + 1;
      sts.setExplanationPrefix("(line " + lineNumber + ")");
      var item = m.lineItems.get(i);
      var refItem = ref.lineItems.get(i);
      if (refItem.isEmpty()) {
        count(sts.testIfEmpty("Quantity should be empty", item.quantity()));
        count(sts.testIfEmpty("Kind should be empty", item.kind()));
        count(sts.testIfEmpty("Type should be empty", item.type()));
        count(sts.testIfEmpty("Item should be empty", item.item()));
        count(sts.testIfEmpty("Requested Date/Time should be empty", item.requestedDateTime()));
        count(sts.testIfEmpty("Estimated Date/Time should be empty", item.estimatedDateTime()));
        count(sts.testIfEmpty("Cost should be empty", item.cost()));
      } else {
        count(sts.test("Quantity should be #EV", refItem.quantity(), item.quantity()));

        if (refItem.kind().isEmpty()) {
          count(sts.testIfEmpty("Kind should be empty", item.kind()));
        } else {
          count(sts.test("Kind should be #EV", refItem.kind(), item.kind()));
        }

        if (refItem.type().isEmpty()) {
          count(sts.testIfEmpty("Type should be empty", item.type()));
        } else {
          count(sts.test("Type should be #EV", refItem.type(), item.type()));
        }

        count(sts.test_2line("Item should be #EV", refItem.item(), item.item()));

        count(sts.test("Requested Date/Time should be #EV", refItem.requestedDateTime(), item.requestedDateTime()));

        count(sts.testIfEmpty("Estimated Date/Time should be empty", item.estimatedDateTime()));
        count(sts.testIfEmpty("Cost should be empty", item.cost()));
      }
    }

    sts.setExplanationPrefix("");

    count(sts.test("Delivery/Reporting Location should be #EV", ref.delivery, m.delivery));
    count(sts.test("Substitutes should be #EV", ref.substitutes, m.substitutes));
    count(sts.test("Requested by should be #EV", ref.requestedBy, m.requestedBy));
    count(sts.test("Priority should be #EV", ref.priority, m.priority));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));

    count(sts.testIfEmpty("Logistics Order Number should be empty", m.logisticsOrderNumber));
    count(sts.testIfEmpty("Supplier Phone Number should be empty", m.supplierInfo));
    count(sts.testIfEmpty("Supplier Name should be empty", m.supplierName));
    count(sts.testIfEmpty("Supplier POC should be empty", m.supplierPointOfContact));
    count(sts.testIfEmpty("Supply Notes should be empty", m.supplyNotes));
    count(sts.testIfEmpty("Logistics Authorizer should be empty", m.logisticsAuthorizer));
    count(sts.testIfEmpty("Logistics Date/Time should be empty", m.logisticsDateTime));
    count(sts.testIfEmpty("Logistics Ordered by should be empty", m.orderedBy));
    count(sts.testIfEmpty("inance Comments should be empty", m.financeComments));
    count(sts.testIfEmpty("Finance Section Chief Name should be #EV", m.financeName));
    count(sts.testIfEmpty("Finance Date/Time should be empty", m.financeDateTime));
  }

  private void handle_Ics205(ExportedMessage message) {
    var m = (Ics205Message) message;
    var ref = (Ics205Message) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", referenceMessage.subject, m.subject));

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("Incident Name should be #EV", ref.incidentName, m.incidentName));

    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    var dateTimePrepared = LocalDateTime.parse(m.dateTimePrepared, dtf);
    count(sts.testOnOrAfter("Date/Time prepared should be on or after #EV", windowOpenDT, dateTimePrepared, dtf));
    count(sts.testOnOrBefore("Date/Time prepared should be on or before #EV", windowCloseDT, dateTimePrepared, dtf));
    count(sts.test("Op Period Date From should be #EV", ref.dateFrom, m.dateFrom));
    count(sts.test("Op Period Date To should be #EV", ref.dateTo, m.dateTo));
    count(sts.test("Op Period Time From should be #EV", ref.timeFrom, m.timeFrom));
    count(sts.test("Op Period Time To should be #EV", ref.timeTo, m.timeTo));

    count(sts.test("Special Intructions should be #EV", ref.specialInstructions, m.specialInstructions));
    count(sts.test("Approved by should be #EV", ref.approvedBy, m.approvedBy));

    var maxRadioEntries = Math.min(ref.radioEntries.size(), m.radioEntries.size());
    if (m.radioEntries.size() != ref.radioEntries.size()) {
      logger
          .warn("### from: " + m.from + ",mId: " + m.messageId + ", m.radioEntries: " + m.radioEntries.size()
              + ", ref.radioEntries: " + ref.radioEntries.size());
    }
    for (var i = 0; i < maxRadioEntries; ++i) {
      var lineNumber = i + 1;
      sts.setExplanationPrefix("  channel use (line " + lineNumber + ") ");
      var entry = m.radioEntries.get(i);
      var refEntry = ref.radioEntries.get(i);
      if (refEntry.isEmpty()) {
        count(sts.testIfEmpty("Zone/Group should be empty", entry.zoneGroup()));
        count(sts.testIfEmpty("Channel # should be empty", entry.channelNumber()));
        count(sts.testIfEmpty("Function be empty", entry.function()));
        count(sts.testIfEmpty("Channel Name should be empty", entry.channelName()));
        count(sts.testIfEmpty("Assignment should be empty", entry.assignment()));
        count(sts.testIfEmpty("RX Freq should be empty", entry.rxFrequency()));
        count(sts.testIfEmpty("RX N or W should be empty", entry.rxNarrowWide()));
        count(sts.testIfEmpty("RX Tone should be empty", entry.rxTone()));
        count(sts.testIfEmpty("TX Freq should be empty", entry.txFrequency()));
        count(sts.testIfEmpty("TX N or W should be empty", entry.txNarrowWide()));
        count(sts.testIfEmpty("TX Tone should be empty", entry.txTone()));
        count(sts.testIfEmpty("Mode should be empty", entry.mode()));
        count(sts.testIfEmpty("Remarks should be empty", entry.remarks()));
      } else {
        count(sts.testIfEmpty("Zone/Group should be empty", entry.zoneGroup()));
        count(sts.test("Channel # should be #EV", refEntry.channelNumber(), entry.channelNumber()));
        count(sts.test("Function should be #EV", refEntry.function(), entry.function()));
        count(sts.test("Channel Name should be #EV", refEntry.channelName(), entry.channelName()));
        count(sts.test("Assignment should be #EV", refEntry.assignment(), entry.assignment()));
        count(sts.test("RX Freq should be #EV", refEntry.rxFrequency(), entry.rxFrequency()));

        if (refEntry.rxNarrowWide().isEmpty()) {
          count(sts.testIfEmpty("RX N or W should be empty", entry.rxNarrowWide()));
        } else {
          count(sts.test("RX N or W should be #EV", refEntry.rxNarrowWide(), entry.rxNarrowWide()));
        }

        if (refEntry.rxTone().isEmpty()) {
          count(sts.testIfEmpty("RX Tone should be empty", entry.rxTone()));
        } else {
          count(sts.test("RX Tone should be #EV", refEntry.rxTone(), entry.rxTone()));
        }

        if (refEntry.txFrequency().isEmpty()) {
          count(sts.testIfEmpty("TX Freq should be empty", entry.txFrequency()));
        } else {
          count(sts.test("TX Freq should be #EV", refEntry.txFrequency(), entry.txFrequency()));
        }

        if (refEntry.txNarrowWide().isEmpty()) {
          count(sts.testIfEmpty("TX N or W should be empty", entry.txNarrowWide()));
        } else {
          count(sts.test("TX N or W should be #EV", refEntry.txNarrowWide(), entry.txNarrowWide()));
        }

        if (refEntry.txTone().isEmpty()) {
          count(sts.testIfEmpty("TX Tone should be empty", entry.txTone()));
        } else {
          count(sts.test("TX Tone should be #EV", refEntry.txTone(), entry.txTone()));
        }

        count(sts.test("Mode should be #EV", refEntry.mode(), entry.mode()));
        count(sts.test("Remarks should be #EV", refEntry.remarks(), entry.remarks()));
      }
    }

    sts.setExplanationPrefix("");
    var dateTimeApproved = LocalDateTime.parse(m.approvedDateTime, dtf);
    count(sts.testOnOrAfter("Date/Time approved should be on or after #EV", windowOpenDT, dateTimeApproved, dtf));
    count(sts.testOnOrBefore("Date/Time approved should be on or before #EV", windowCloseDT, dateTimeApproved, dtf));
    count(sts.test("IAP Page should be #EV", ref.iapPage, m.iapPage));
  }

  private void handle_Fsr(ExportedMessage message) {
    var m = (FieldSituationMessage) message;
    var ref = (FieldSituationMessage) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", ref.subject, m.subject));

    count(sts.test("Organization Name should be #EV", ref.organization, m.organization));
    count(sts.test("Precedence should be #EV", ref.precedence, m.precedence));

    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'");
    var formDateTime = LocalDateTime.parse(m.formDateTime.replaceAll("  ", " "), dtf);
    count(sts.testOnOrAfter("Form Date/Time should be on or after #EV", windowOpenDT, formDateTime, dtf));
    count(sts.testOnOrBefore("Form Date/Time should be on or before #EV", windowCloseDT, formDateTime, dtf));
    count(sts.test("Task # should be #EV", ref.task, m.task));
    count(sts.test("Emergent/Life Safety need should be #EV", ref.isHelpNeeded, m.isHelpNeeded));
    count(sts.test("City should be #EV", ref.city, m.city));
    count(sts.test("County should be #EV", ref.county, m.county));
    count(sts.testIfEmpty("Territory should be empty", m.territory));
    count(sts.test("LAT should be #EV", ref.formLocation.getLatitude(), m.formLocation.getLatitude()));
    count(sts.test("LON should be #EV", ref.formLocation.getLongitude(), m.formLocation.getLongitude()));

    count(sts.test("POTS landlines functioning should be #EV", ref.landlineStatus, m.landlineStatus));
    if (ref.landlineStatus.equals("NO")) {
      count(sts.test("POTS provider should be #EV", ref.landlineComments, m.landlineComments));
    } else {
      count(sts.testIfEmpty("POTS provider should be empty", m.landlineComments));
    }

    count(sts.test("VOIP landlines functioning should be #EV", ref.voipStatus, m.voipStatus));
    if (ref.voipStatus.equals("NO")) {
      count(sts.test("VOIP provider should be #EV", ref.voipComments, m.voipComments));
    } else {
      count(sts.testIfEmpty("VOIP provider should be empty", m.voipComments));
    }

    count(sts.test("Cell phone voice functioning should be #EV", ref.cellPhoneStatus, m.cellPhoneStatus));
    if (ref.cellPhoneStatus.equals("NO")) {
      count(sts.test("Cell phone voice provider should be #EV", ref.cellPhoneComments, m.cellPhoneComments));
    } else {
      count(sts.testIfEmpty("Cell phone voice provider should be empty", m.cellPhoneComments));
    }

    count(sts.test("Cell phone text functioning should be #EV", ref.cellTextStatus, m.cellTextStatus));
    if (ref.cellTextStatus.equals("NO")) {
      count(sts.test("Cell phone text provider should be #EV", ref.cellTextComments, m.cellTextComments));
    } else {
      count(sts.testIfEmpty("Cell text voice provider should be empty", m.cellTextComments));
    }

    count(sts.test("AM/FM Broadcast functioning should be #EV", ref.radioStatus, m.radioStatus));
    if (ref.radioStatus.equals("NO")) {
      count(sts.test("AM/FM stations should be #EV", ref.radioComments, m.radioComments));
    } else {
      count(sts.testIfEmpty("AM/FM stations should be empty", m.radioComments));
    }

    count(sts.test("OTA TV functioning should be #EV", ref.tvStatus, m.tvStatus));
    if (ref.tvStatus.equals("NO")) {
      count(sts.test("OTA TV stations should be #EV", ref.tvComments, m.tvComments));
    } else {
      count(sts.testIfEmpty("OTA TV stations should be empty", m.tvComments));
    }

    count(sts.test("Satellite TV functioning should be #EV", ref.satTvStatus, m.satTvStatus));
    if (ref.satTvStatus.equals("NO")) {
      count(sts.test("Satellite TV provider should be #EV", ref.satTvComments, m.satTvComments));
    } else {
      count(sts.testIfEmpty("Satellite TV provider should be empty", m.satTvComments));
    }

    count(sts.test("Cable TV functioning should be #EV", ref.cableTvStatus, m.cableTvStatus));
    if (ref.cableTvStatus.equals("NO")) {
      count(sts.test("Cable TV provider should be #EV", ref.cableTvComments, m.cableTvComments));
    } else {
      count(sts.testIfEmpty("Cable TV provider should be empty", m.cableTvComments));
    }

    count(sts.test("Public Water Works functioning should be #EV", ref.waterStatus, m.waterStatus));
    if (ref.waterStatus.equals("NO")) {
      count(sts.test("Public Water Works provider should be #EV", ref.waterComments, m.waterComments));
    } else {
      count(sts.testIfEmpty("Public Water Works provider should be empty", m.waterComments));
    }

    count(sts.test("Commercial Power functioning should be #EV", ref.powerStatus, m.powerStatus));
    if (ref.powerStatus.equals("NO")) {
      count(sts.test("Commercial Power provider should be #EV", ref.powerComments, m.powerComments));
    } else {
      count(sts.testIfEmpty("Commercial Power provider should be empty", m.powerComments));
    }

    count(sts.test("Commercial Power stable should be #EV", ref.powerStableStatus, m.powerStableStatus));
    if (ref.powerStableStatus.equals("NO")) {
      count(sts.test("Commercial Power Stable provider should be #EV", ref.powerStableComments, m.powerStableComments));
    } else {
      count(sts.testIfEmpty("Commercial Power Stable provider should be empty", m.powerStableComments));
    }

    count(sts.test("Natural Gas supply functioning be #EV", ref.naturalGasStatus, m.naturalGasStatus));
    if (ref.naturalGasStatus.equals("NO")) {
      count(sts.test("Natural Gas provider should be #EV", ref.naturalGasComments, m.naturalGasComments));
    } else {
      count(sts.testIfEmpty("Natural Gas provider should be empty", m.naturalGasComments));
    }

    count(sts.test("Internet functioning be #EV", ref.internetStatus, m.internetStatus));
    if (ref.internetStatus.equals("NO")) {
      count(sts.test("Internet provider should be #EV", ref.internetComments, m.internetComments));
    } else {
      count(sts.testIfEmpty("Internet provider should be empty", m.internetComments));
    }

    count(sts.test("NOAA Weather Radio functioning be #EV", ref.noaaStatus, m.noaaStatus));
    if (ref.noaaStatus.equals("NO")) {
      count(sts.test("NOAA Weather Radio station should be #EV", ref.noaaComments, m.noaaComments));
    } else {
      count(sts.testIfEmpty("NOAA Weather Radio station provider should be empty", m.noaaComments));
    }

    count(sts.test("NOAA Weather audio degraded be #EV", ref.noaaAudioDegraded, m.noaaAudioDegraded));
    if (ref.noaaAudioDegraded.equals("YES")) {
      count(sts
          .test("NOAA Weather Radio degraded station should be #EV", ref.noaaAudioDegradedComments,
              m.noaaAudioDegradedComments));
    } else {
      count(
          sts.testIfEmpty("NOAA Weather Radio degraded station provider should be empty", m.noaaAudioDegradedComments));
    }

    count(sts.test("Additional comments should be #EV", ref.additionalComments, m.additionalComments));
    count(sts.test("POC should be #EV", ref.poc, m.poc));
  }

  private void handle_Hics259(ExportedMessage message) {
    var m = (Hics259Message) message;
    var ref = (Hics259Message) referenceMessage;

    count(sts.testStartsWith("Message Subject should start with #EV", ref.subject, m.subject));
    count(sts.test("Incident name should be #EV", ref.incidentName, m.incidentName));

    // rely on gateways to filter to window
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));
    count(sts.test("Operational Period # should be #EV", ref.operationalPeriod, m.operationalPeriod));
    count(sts.test("Operational Date From should be #EV", ref.opFromDate, m.opFromDate));
    count(sts.test("Operational Date To should be #EV", ref.opToDate, m.opToDate));
    count(sts.test("Operational Time From should be #EV", ref.opFromTime, m.opFromTime));
    count(sts.test("Operational Time To should be #EV", ref.opToTime, m.opToTime));

    var refMap = ref.casualtyMap;
    var mMap = m.casualtyMap;
    for (var key : Hics259Message.CASUALTY_KEYS) {
      var refEntry = refMap.get(key);
      var mEntry = mMap.get(key);
      count(sts.test(key + " Adult Count should be #EV", refEntry.adultCount(), mEntry.adultCount()));
      count(sts.test(key + " Pediatric Count should be #EV", refEntry.childCount(), mEntry.childCount()));
      count(sts.test(key + " Comment should be #EV", refEntry.comment(), mEntry.comment()));
    }

    count(sts.test("Patient Tracking Manager should be #EV", ref.patientTrackingManager, m.patientTrackingManager));
    count(sts.test("Facility Name should be #EV", ref.facilityName, m.facilityName));
  }

  @Override
  protected String makeOutboundMessageSubject(Object object) {
    return "ETO Practice Exercise Feedback for " + date;
  }

  @Override
  protected void endCommonProcessing(ExportedMessage m) {
    var ackText = ackTextMap.get(sender);
    var sb = new StringBuilder();
    sb.append("ACKNOWLEDGEMENTS" + "\n");
    sb.append(ackText);
    sb.append("FEEDBACK" + "\n");
    outboundMessagePrefixContent = sb.toString();

    outboundMessagePostfixContent = nextInstructions;
    super.endCommonProcessing(m);
  }

  @Override
  public void postProcess() {
    WriteProcessor.writeTable("practice-summary.csv", summaries);
    super.postProcess();

    kmlService.finalize(Path.of(outputPath.toString(), "feedback.kml"));

    var mapEntries = summaries.stream().map(s -> MapEntry.fromSummary(s)).toList();
    var mapService = new MapService(null, null);
    mapService.makeMap(outputPath, new MapHeader("ETO-" + dateString + "--" + messageType.name(), ""), mapEntries);
  }

  public class Summary implements IWritableTable {
    public String from;
    public String to;
    public LatLongPair location;
    public LocalDateTime dateTime;
    public int feedbackCount;
    public List<String> explanations;
    public String messageId;
    public String messageType;

    public static final String perfectMessageText = "Perfect messages!";
    public static final int perfectMessageCount = 0; // in case we need to adjust

    public Summary(ExportedMessage m) {
      this.from = m.from;
      this.to = m.to;
      this.location = (m.getMessageType() == MessageType.FIELD_SITUATION) ? m.msgLocation : m.mapLocation;
      this.dateTime = m.sortDateTime;
      this.messageId = m.messageId;
      this.explanations = sts.getExplanations();
    }

    @Override
    public int compareTo(IWritableTable o) {
      var other = (Summary) o;
      return from.compareTo(other.from);
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>(
          List.of("From", "To", "Latitude", "Longitude", "Date", "Time", "Feedback Count", "Feedback", "Message Id"));
      return list.toArray(new String[list.size()]);
    }

    public int getFeedbackCount() {
      return explanations.size() - perfectMessageCount;
    }

    public String getFeedback() {
      if (explanations.size() > 0) {
        return String.join("\n", explanations);
      } else {
        return perfectMessageText;
      }
    }

    @Override
    public String[] getValues() {
      var latitude = location == null ? "0.0" : location.getLatitude();
      var longitude = location == null ? "0.0" : location.getLongitude();
      var date = dateTime == null ? "" : dateTime.toLocalDate().toString();
      var time = dateTime == null ? "" : dateTime.toLocalTime().toString();
      // var feedbackCount = "0";
      // var feedback = perfectMessageText;

      // if (explanations.size() > 0) {
      // feedbackCount = String.valueOf(explanations.size() - perfectMessageCount);
      // feedback = String.join("\n", explanations);
      // }

      var nsTo = to == null ? "(null)" : to;

      var list = new ArrayList<String>(List
          .of(from, nsTo, latitude, longitude, date, time, String.valueOf(getFeedbackCount()), getFeedback(),
              messageId));
      return list.toArray(new String[list.size()]);
    }
  }

}
