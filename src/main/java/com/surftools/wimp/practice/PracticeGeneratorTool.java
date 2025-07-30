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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.surftools.utils.FileUtils;
import com.surftools.utils.WeightedRandomChooser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Ics205Message;
import com.surftools.wimp.message.Ics205Message.RadioEntry;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.practice.PracticeData.ExerciseIdMethod;
import com.surftools.wimp.practice.PracticeData.ListType;
import com.surftools.wimp.practice.PracticeHicsData.Types;

/**
 * Program to generate many weeks work "data" for ETO weekly "practice" semi-automatic exercises
 *
 * NOTE WELL: since this program will be run about once per year, there's no need for data-driven configuration
 */
public class PracticeGeneratorTool {

  public final static Map<Integer, MessageType> MESSAGE_TYPE_MAP = Map
      .of(//
          1, MessageType.ICS_213, //
          2, MessageType.ICS_213_RR, //
          3, MessageType.HICS_259, //
          4, MessageType.ICS_205, //
          5, MessageType.FIELD_SITUATION);

  public final static Set<Integer> VALID_ORDINALS = MESSAGE_TYPE_MAP.keySet();

  private static final Logger logger = LoggerFactory.getLogger(PracticeGeneratorTool.class);
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  @Option(name = "--outputDirName", usage = "path to output directory", required = true)
  private String outputDirName = null;
  private String referenceDirName = null;

  @Option(name = "--rngSeed", usage = "random number generator seed", required = false)
  private Long rngSeed = null;

  private final DayOfWeek TARGET_DOW = DayOfWeek.THURSDAY;

  private static final String NA = "n/a";
  private static final String NL = "\n";
  private static final String INDENT = "    ";
  private static final String INDENT2 = INDENT + INDENT;
  private static final String INDENT3 = INDENT + INDENT2;

  public static void main(String[] args) {
    var app = new PracticeGeneratorTool();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  private void run() {
    logger.info("begin run");
    logger.info("outputDir: " + outputDirName);

    referenceDirName = outputDirName + File.separator + "reference";
    FileUtils.deleteDirectory(Path.of(referenceDirName));
    FileUtils.createDirectory(Path.of(referenceDirName));

    rngSeed = rngSeed == null ? Long.valueOf(2025) : rngSeed;
    logger.info("rngSeed: " + String.valueOf(rngSeed));

    // generate 2 years worth, from beginning of "this" year, through end of "next" year
    var nowDate = LocalDate.now();
    var startDate = LocalDate.of(nowDate.getYear(), 1, 1);
    var date = startDate;
    while (true) {
      var ord = PracticeUtils.getOrdinalDayOfWeek(date);
      if (date.getDayOfWeek() == TARGET_DOW && VALID_ORDINALS.contains(ord)) {
        break;
      }
      date = date.plusDays(1);
    }

    while (date.getYear() <= nowDate.getYear() + 1) {
      var ord = PracticeUtils.getOrdinalDayOfWeek(date);
      generate(date, ord);
      date = date.plusDays(7);
    }
    logger.info("end run");

  }

  private void generate(LocalDate date, int ord) {
    var messageType = MESSAGE_TYPE_MAP.get(ord);
    var path = Path.of(referenceDirName, date.toString());
    FileUtils.createDirectory(path);
    switch (messageType) {
    case ICS_213:
      handle_Ics213(date, ord, path);
      break;
    case ICS_213_RR:
      handle_Ics213RR(date, ord, path);
      break;
    case HICS_259:
      handle_Hics259(date, ord, path);
      break;
    case ICS_205:
      handle_Ics205(date, ord, path);
      break;
    case FIELD_SITUATION:
      handle_Fsr(date, ord, path);
      break;
    default:
      throw new RuntimeException("unsupported messageType: " + messageType.toString());
    }
  }

  private String makeMessageId(String prefix, LocalDateTime dateTime) {
    final var dtf = DateTimeFormatter.ofPattern("MMddHHmmss");
    return prefix + dtf.format(dateTime);
  }

  private ExportedMessage makeExportedMessage(LocalDate date, String subject) {
    LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(12, 0));
    var messageId = makeMessageId("PX", dateTime);
    var from = NA;
    var source = NA;
    var to = NA;
    var toList = NA;
    var ccList = NA;

    var msgLocation = LatLongPair.ZERO_ZERO;
    var locationSource = NA;
    var mime = NA;
    var plainContent = NA;
    Map<String, byte[]> attachments = new HashMap<String, byte[]>();
    boolean isP2p = false;
    String fileName = NA;
    List<String> lines = new ArrayList<String>();

    var exportedMessage = new ExportedMessage(messageId, from, source, to, toList, ccList, //
        subject, dateTime, //
        msgLocation, locationSource, //
        mime, plainContent, attachments, isP2p, fileName, lines);

    return exportedMessage;
  }

  private void handle_Ics213(LocalDate date, int ord, Path path) {
    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var formSubject = "ETO Practice Exercise for " + dtf.format(date);
    var subject = "ICS-213: " + formSubject;
    var exportedMessage = makeExportedMessage(date, subject);

    var rng = new Random(rngSeed + date.toString().hashCode());
    var pd = new PracticeData(rng);

    var names = pd.getUniqueList(3, ListType.DOUBLED_NAMES);
    var positions = pd.getUniqueList(3, ListType.SHORT_EMERGENCY_ROLES);

    var dow_dtf = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd");

    var organization = "EmComm Training Organization";
    var incidentName = "ETO Weekly Practice";
    var formFrom = names.get(0) + " / " + positions.get(0);
    var formTo = names.get(1) + " / " + positions.get(1);

    var formDate = NA;
    var formTime = NA;
    var formMessage = "Exercise Id: " + pd.getExerciseId(ExerciseIdMethod.PHONE);
    var approvedBy = names.get(2);
    var position = positions.get(2); //
    var isExercise = true;
    var formLocation = LatLongPair.ZERO_ZERO;
    var version = NA;
    var dataSource = NA;

    var sb = new StringBuilder(); // exercise instructions
    sb.append("ETO Exercise Instructions for Thursday, " + dtf.format(date) + NL + NL);
    sb.append("Complete an ICS-213 General Message" + NL + NL);
    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);
    sb.append("Exercise window: " + dow_dtf.format(windowOpenDate) + " 00:00 UTC - " + //
        dow_dtf.format(windowCloseDate) + " 08:00 UTC" + NL + NL);
    sb.append("Use the following values when completing the form:" + NL);
    sb.append(INDENT + "Setup: agency or group name: " + organization + NL);
    sb.append(INDENT + "THIS IS AN EXERCISE: (checked)" + NL);
    sb.append(INDENT + "Incident Name: " + incidentName + NL);
    sb.append(INDENT + "To (Name/Position): " + formTo + NL);
    sb.append(INDENT + "From (Name/Position): " + formFrom + NL);
    sb.append(INDENT + "Subject: " + formSubject + NL);
    sb.append(INDENT + "Date: (click in box and accept date)" + NL);
    sb.append(INDENT + "Time: (click in box and accept time)" + NL);
    sb.append(INDENT + "Message: " + formMessage + NL);
    sb.append(INDENT + "Approved by: " + approvedBy + NL);
    sb.append(INDENT + "Position / Title: " + position + NL);
    sb.append(NL);
    sb.append("Ensure that you have a valid and appropriate Latitude and Longitude/" + NL);
    sb.append(NL);
    sb.append("Send the message via the Session type of your choice to ETO-PRACTICE." + NL);
    sb.append(NL);
    sb.append("Refer to https://Emcomm-Training.org/practice for further instructions " + NL);
    sb.append(" about the weekly practice exercises and/or monthly training exercises." + NL);

    var m = new Ics213Message(exportedMessage, organization, incidentName, //
        formFrom, formTo, formSubject, formDate, formTime, //
        formMessage, approvedBy, position, //
        isExercise, formLocation, version, dataSource);

    var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    try {
      var json = objectMapper.writeValueAsString(m);
      var typeName = m.getMessageType().toString();
      Files.writeString(Path.of(path.toString(), typeName + ".json"), json);
      Files
          .writeString(Path.of(path.toString(), dtf.format(date) + "-" + typeName + "_instructions.txt"),
              sb.toString());
    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", ICS_213");
  }

  private void handle_Ics213RR(LocalDate date, int ord, Path path) {
    final int nLineItems = 3;
    Ics213RRMessage.setLineItemsToDisplay(nLineItems);

    var rng = new Random(rngSeed + date.toString().hashCode());
    var pd = new PracticeData(rng);
    var prd = new PracticeResourceData(rng, outputDirName);

    var incidentName = "ETO Weekly Practice";
    var requestNumber = "Exercise Id: " + pd.getExerciseId(ExerciseIdMethod.PHONE);

    var subject = "ICS 213RR- " + incidentName + "- Request #:" + requestNumber;
    var exportedMessage = makeExportedMessage(date, subject);

    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var dow_dtf = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd");
    var names = pd.getUniqueList(2, ListType.DOUBLED_NAMES);
    var positions = pd.getUniqueList(1, ListType.SHORT_EMERGENCY_ROLES);

    var organization = "EmComm Training Organization";

    var lineItems = prd.getRandomResources(date, nLineItems, null, null);

    var delivery = (String) pd.deliveryChooser.next();
    var substitutes = rng.nextBoolean() ? "substitute as appropriate" : "no substitutes allowed";

    var requestedBy = names.get(0) + " / " + positions.get(0);
    var priority = (String) pd.priorityChooser.next();
    var approvedBy = names.get(1);

    var sb = new StringBuilder(); // exercise instructions
    sb.append("ETO Exercise Instructions for Thursday, " + dtf.format(date) + NL + NL);
    sb.append("Complete an ICS-213 Resource Request Message" + NL + NL);
    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);
    sb.append("Exercise window: " + dow_dtf.format(windowOpenDate) + " 00:00 UTC - " + //
        dow_dtf.format(windowCloseDate) + " 08:00 UTC" + NL + NL);

    sb.append("Use the following values when completing the form:" + NL);
    sb.append(INDENT + "Setup: agency or group name: " + organization + NL);
    sb.append(INDENT + "Incident name: " + incidentName + NL);
    sb.append(INDENT + "Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Resource Request Number: " + requestNumber + NL);
    sb.append(INDENT + "Order Items (leave Estimated and Cost empty)" + NL);

    var lineNumber = 0;
    for (var line : lineItems) {
      if (line.isEmpty()) {
        continue;
      }

      ++lineNumber;
      sb.append(INDENT2 + "line " + lineNumber + NL); //
      sb.append(INDENT3 + "Qty: " + line.quantity() + NL);
      sb.append(INDENT3 + "Kind: " + line.kind() + NL);
      sb.append(INDENT3 + "Type: " + line.type() + NL);
      sb.append(INDENT3 + "Description: " + line.item() + NL);
      sb.append(INDENT3 + "Requested Time: " + line.requestedDateTime() + NL);
    }

    sb.append(INDENT + "Delivery/Reporting Location: " + delivery + NL);
    sb.append(INDENT + "Substitutes: " + substitutes + NL);
    sb.append(INDENT + "Requested by Name/Position: " + requestedBy + NL);
    sb.append(INDENT + "Priority: " + priority + NL);
    sb.append(INDENT + "Section Chief Name for Approval: " + approvedBy + NL);

    sb.append(NL);
    sb.append("Send the message via the Session type of your choice to ETO-PRACTICE." + NL);
    sb.append(NL);
    sb.append("Refer to https://Emcomm-Training.org/practice for further instructions " + NL);
    sb.append("about the weekly practice exercises and/or monthly training exercises." + NL);

    var m = new Ics213RRMessage(exportedMessage, organization, incidentName, //
        NA, requestNumber, //
        lineItems, //
        delivery, substitutes, requestedBy, priority, approvedBy, //
        "", "", "", // logisticsOrderNumber, supplierInfo, supplierName, //
        "", "", "", // supplierPointOfContact, supplyNotes, logisticsAuthorizer, //
        "", "", // logisticsDateTime, orderedBy, //
        "", "", "" // financeComments, financeName, financeDateTime//
    );

    var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    try {
      var json = objectMapper.writeValueAsString(m);
      var typeName = m.getMessageType().toString();
      Files.writeString(Path.of(path.toString(), typeName + ".json"), json);
      Files
          .writeString(Path.of(path.toString(), dtf.format(date) + "-" + typeName + "_instructions.txt"),
              sb.toString());
    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", ICS_213RR");
  }

  private void handle_Hics259(LocalDate date, int ord, Path path) {

    var rng = new Random(rngSeed + date.toString().hashCode());
    var pd = new PracticeData(rng);
    var phd = new PracticeHicsData(rng);

    var incidentName = "Exercise Id: " + pd.getExerciseId(ExerciseIdMethod.PHONE);
    var facilityName = phd.get(Types.HOSPITAL_NAMES);
    var subject = "HICS-259 HOSPITAL CASUALTY/FATALITY REPORT-" + incidentName;
    var exportedMessage = makeExportedMessage(date, subject);

    var operationalPeriod = String.valueOf(rng.nextInt(1, 3));
    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);
    var opFrom = LocalDateTime.of(windowOpenDate, LocalTime.of(0, 0));
    var opTo = LocalDateTime.of(windowCloseDate, LocalTime.of(8, 0));
    var casualtyMap = phd.makeCasualtyMap();
    var patientTrackingManager = pd.getUniqueList(1, ListType.DOUBLED_NAMES).get(0);

    var date_dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var time_dtf = DateTimeFormatter.ofPattern("HH:mm");
    var dow_dtf = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd");
    var sb = new StringBuilder(); // exercise instructions
    sb.append("ETO Exercise Instructions for Thursday, " + date_dtf.format(date) + NL + NL);
    sb.append("Complete an HICS 259 Hospital Casualty/Fatality Report Message" + NL + NL);
    sb.append("Exercise window: " + dow_dtf.format(windowOpenDate) + " 00:00 UTC - " + //
        dow_dtf.format(windowCloseDate) + " 08:00 UTC" + NL + NL);
    sb.append("Use the following values when completing the form:" + NL);
    sb.append(INDENT + "Incident name: " + incidentName + NL);
    sb.append(INDENT + "Date: (click in box and accept date)" + NL);
    sb.append(INDENT + "Time: (click in box and accept time)" + NL);
    sb.append(INDENT + "Operational Period #: " + operationalPeriod + NL);
    sb.append(INDENT + "Operational Period Date From: " + date_dtf.format(opFrom) + NL);
    sb.append(INDENT + "Operational Period Date To: " + date_dtf.format(opTo) + NL);
    sb.append(INDENT + "Operational Period Time From: " + time_dtf.format(opFrom) + NL);
    sb.append(INDENT + "Operational Period Time To: " + time_dtf.format(opTo) + NL);

    sb.append("Number Of Casualties" + NL);

    for (var key : Hics259Message.CASUALTY_KEYS) {
      var entry = casualtyMap.get(key);
      sb.append(INDENT + key + NL);
      sb.append(INDENT2 + "Adult: " + entry.adultCount() + NL);
      sb.append(INDENT2 + "Pediatric: " + entry.childCount() + NL);
      sb.append(INDENT2 + "Comments: " + entry.comment() + NL);
    }
    sb.append("Prepared by: " + patientTrackingManager + NL);
    sb.append("Facility Name: " + facilityName + NL);

    sb.append(NL);
    sb.append("Send the message via the Session type of your choice to ETO-PRACTICE." + NL);
    sb.append(NL);
    sb.append("Refer to https://Emcomm-Training.org/practice for further instructions " + NL);
    sb.append("about the weekly practice exercises and/or monthly training exercises." + NL);

    var m = new Hics259Message(exportedMessage, //
        incidentName, LocalDateTime.of(1970, 1, 1, 0, 0), //
        operationalPeriod, opFrom, opTo, //
        casualtyMap, //
        patientTrackingManager, facilityName, NA);

    var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    try {
      var json = objectMapper.writeValueAsString(m);
      var typeName = m.getMessageType().toString();
      Files.writeString(Path.of(path.toString(), typeName + ".json"), json);
      Files
          .writeString(Path.of(path.toString(), date_dtf.format(date) + "-" + typeName + "_instructions.txt"),
              sb.toString());
    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", HICS_259");
  }

  private void handle_Ics205(LocalDate date, int ord, Path path) {
    final int nRadioEntries = 3;
    Ics205Message.setRadioEntriesToDisplay(nRadioEntries);

    var incidentName = "ETO Weekly Practice";
    var subject = "ICS 205 - " + incidentName;
    var exportedMessage = makeExportedMessage(date, subject);

    var rng = new Random(rngSeed + date.toString().hashCode());
    var pd = new PracticeData(rng);
    var prd = new PracticeRadioData(rng);

    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var dow_dtf = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd");
    var names = pd.getUniqueList(1, ListType.NAMES);

    var organization = "EmComm Training Organization";

    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);
    var dateFrom = dtf.format(windowOpenDate);
    var dateTo = dtf.format(windowCloseDate);
    var timeFrom = "00:00 UTC";
    var timeTo = "08:00 UTC";
    var radioItems = prd.makeRadioEntries(nRadioEntries);
    for (var i = nRadioEntries; i < Ics205Message.MAX_RADIO_ENTRIES; ++i) {
      radioItems.add(RadioEntry.EMPTY);
    }
    var specialInstructions = "Exercise Id: " + pd.getExerciseId(ExerciseIdMethod.PHONE);
    var approvedBy = names.get(0);
    var iapPage = String.valueOf(rng.nextInt(5, 10));

    var sb = new StringBuilder(); // exercise instructions
    sb.append("ETO Exercise Instructions for Thursday, " + dtf.format(date) + NL + NL);
    sb.append("Complete an ICS-205 Incident Radio Communications Plan Message" + NL + NL);
    sb.append("Exercise window: " + dow_dtf.format(windowOpenDate) + " 00:00 UTC - " + //
        dow_dtf.format(windowCloseDate) + " 08:00 UTC" + NL + NL);

    sb.append("Use the following values when completing the form:" + NL);
    sb.append(INDENT + "Setup: agency or group name: " + organization + NL);
    sb.append(INDENT + "Incident name: " + incidentName + NL);
    sb.append(INDENT + "Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Operational Period Date From: " + dateFrom + NL);
    sb.append(INDENT + "Operational Period Date To: " + dateTo + NL);
    sb.append(INDENT + "Operational Period Time From: " + timeFrom + NL);
    sb.append(INDENT + "Operational Period Time To: " + timeTo + NL);

    sb.append(INDENT + "Basic Radio Channel Use:" + NL);
    var lineNumber = 0;
    for (var item : radioItems) {
      if (item.isEmpty()) {
        break;
      }

      ++lineNumber;
      sb.append(INDENT2 + "line " + lineNumber + NL); //
      sb.append(INDENT3 + "Ch #: " + item.channelNumber() + NL);
      sb.append(INDENT3 + "Function: " + item.function() + NL);
      sb.append(INDENT3 + "Channel Name: " + item.channelName() + NL);
      sb.append(INDENT3 + "Assignment: " + item.assignment() + NL);
      sb.append(INDENT3 + "RX Freq: " + item.rxFrequency() + NL);
      sb.append(INDENT3 + "RX N or W: " + item.rxNarrowWide() + NL);
      sb.append(INDENT3 + "RX Tone: " + item.rxTone() + NL);
      sb.append(INDENT3 + "Tx Freq: " + item.txFrequency() + NL);
      sb.append(INDENT3 + "TX N or W: " + item.txNarrowWide() + NL);
      sb.append(INDENT3 + "TX Tone: " + item.txTone() + NL);
      sb.append(INDENT3 + "Mode: " + item.mode() + NL);
      sb.append(INDENT3 + "Remarks: " + item.remarks() + NL);
    }

    sb.append(INDENT + "Special Instructions: " + specialInstructions + NL);
    sb.append(INDENT + "Approved by: " + approvedBy + NL);
    sb.append(INDENT + "Approved Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "IAP Page: " + iapPage + NL);
    sb.append(INDENT + "Attach CSV: (No)" + NL);

    sb.append(NL);
    sb.append("Send the message via the Session type of your choice to ETO-PRACTICE." + NL);
    sb.append(NL);
    sb.append("Refer to https://Emcomm-Training.org/practice for further instructions " + NL);
    sb.append("about the weekly practice exercises and/or monthly training exercises." + NL);

    var m = new Ics205Message(exportedMessage, organization, incidentName, NA, //
        dateFrom, dateTo, timeFrom, timeTo, //
        specialInstructions, approvedBy, NA, iapPage, //
        radioItems, NA);

    var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    try {
      var json = objectMapper.writeValueAsString(m);
      var typeName = m.getMessageType().toString();
      Files.writeString(Path.of(path.toString(), typeName + ".json"), json);
      Files
          .writeString(Path.of(path.toString(), dtf.format(date) + "-" + typeName + "_instructions.txt"),
              sb.toString());
    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", ICS_205");
  }

  private void handle_Fsr(LocalDate date, int ord, Path path) {
    var subject = "//WL2K R/ Routine/ Field Situation Report";
    var exportedMessage = makeExportedMessage(date, subject);

    var rng = new Random(rngSeed + date.toString().hashCode());
    var pd = new PracticeData(rng);

    var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    var dow_dtf = DateTimeFormatter.ofPattern("EEE yyyy-MM-dd");
    var names = pd.getUniqueList(1, ListType.NAMES);

    var organization = "EmComm Training Organization";
    var precedence = "R/ Routine";
    var formDateTime = NA;
    var task = "ETO Weekly Practice";
    var formTo = "ETO-PRACTICE";
    var formFrom = NA;
    var isHelpNeeded = "NO";
    var neededHelp = "";

    var city = "Fort Collins";
    var county = "Larimer";
    var state = "CO";
    var territory = "";

    var formLocation = new LatLongPair("40.2503", "-103.7990");

    final var YES = "YES";
    final var NO = "NO";
    final var UNK = "Unknown - N/A";
    var chooser = new WeightedRandomChooser(List.of(YES, NO, UNK), rng);

    var landlineStatus = (String) chooser.next();
    var landlineComments = landlineStatus.equals(NO) ? "CenturyLink" : "";

    var voipStatus = UNK;
    var voipComments = "";

    var cellPhoneStatus = (String) chooser.next();
    var cellPhoneComments = cellPhoneStatus.equals(NO) ? "Verizon" : "";

    var cellTextStatus = cellPhoneStatus;
    var cellTextComments = cellPhoneComments;

    var radioStatus = (String) chooser.next();
    var radioComments = radioStatus.equals(NO) ? "KFRC FM" : "";

    var tvStatus = (String) chooser.next();
    var tvComments = tvStatus.equals(NO) ? "KUSA" : "";

    var satTvStatus = UNK;
    var satTvComments = "";

    var cableTvStatus = (String) chooser.next();
    var cableTvComments = cableTvStatus.equals(NO) ? "Xfinity" : "";

    var waterStatus = (String) chooser.next();
    var waterComments = waterStatus.equals(NO) ? "Fort Collins Utilities" : "";

    var powerStatus = (String) chooser.next();
    var powerComments = powerStatus.equals(NO) ? "Fort Collins Utilities" : "";

    var powerStable = (String) chooser.next();
    var powerStableComments = powerStable.equals(NO) ? "Fort Collins Utilities" : "";

    var naturalGasStatus = (String) chooser.next();
    var naturalGasComments = naturalGasStatus.equals(NO) ? "Xcel Energy" : "";

    var internetStatus = cableTvStatus;
    var internetComments = cableTvComments;

    var noaaStatus = YES;
    var noaaComments = "";

    var noaaAudioDegraded = NO;
    var noaaAudioDegradedComments = "";

    var additionalComments = "Exercise Id: " + pd.getExerciseId(ExerciseIdMethod.PHONE);
    var poc = names.get(0);
    var formVersion = NA;

    var windowOpenDate = date.minusDays(5);
    var windowCloseDate = date.plusDays(1);
    var sb = new StringBuilder();
    sb.append("ETO Exercise Instructions for Thursday, " + dtf.format(date) + NL + NL);
    sb.append("Complete a Field SituationReport Message" + NL + NL);
    sb.append("Exercise window: " + dow_dtf.format(windowOpenDate) + " 00:00 UTC - " + //
        dow_dtf.format(windowCloseDate) + " 08:00 UTC" + NL + NL);

    sb.append("Use the following values when completing the form:" + NL);
    sb.append(INDENT + "Setup: agency or group name: " + organization + NL);
    sb.append(INDENT + "Precedence: " + precedence + NL);
    sb.append(INDENT + "Date/Time: (click in box and accept date/time)" + NL);
    sb.append(INDENT + "Task #: " + task + NL);
    sb.append(INDENT + "From: <YOUR CALL>" + NL);
    sb.append(INDENT + "To: " + formTo + NL);
    sb.append(INDENT + "EMERGENT/LIFE SAFETY Need: " + isHelpNeeded + NL);

    sb.append(INDENT + "City: " + city + NL);
    sb.append(INDENT + "County: " + county + NL);
    sb.append(INDENT + "State: " + state + NL);
    sb.append(INDENT + "Latitude: " + formLocation.getLatitude() + NL);
    sb.append(INDENT + "Longitude: " + formLocation.getLongitude() + NL);

    sb.append(INDENT + "POTS landlines functioning: " + landlineStatus + NL);
    sb.append(INDENT + "POTS landlines provider if NO: " + landlineComments + NL);
    sb.append(INDENT + "VOIP landlines functioning: " + voipStatus + NL);
    sb.append(INDENT + "VOIP landlines provider if NO: " + voipComments + NL);
    sb.append(INDENT + "Cell phone voice calls functioning: " + cellPhoneStatus + NL);
    sb.append(INDENT + "Cell phone voice provider if NO: " + cellPhoneComments + NL);
    sb.append(INDENT + "Cell phone texts functioning: " + cellTextStatus + NL);
    sb.append(INDENT + "Cell phone texts provider if NO: " + cellTextComments + NL);

    sb.append(INDENT + "AM/FM Broadcast Stations functioning: " + radioStatus + NL);
    sb.append(INDENT + "Broadcast station callsign if NO: " + radioComments + NL);
    sb.append(INDENT + "OTA TV functioning: " + tvStatus + NL);
    sb.append(INDENT + "TV station if NO: " + tvComments + NL);
    sb.append(INDENT + "Satellite TV functioning: " + satTvStatus + NL);
    sb.append(INDENT + "Satellite TV provider if NO: " + satTvComments + NL);
    sb.append(INDENT + "Cable TV functioning: " + cableTvStatus + NL);
    sb.append(INDENT + "Cable TV provider if NO: " + cableTvComments + NL);

    sb.append(INDENT + "Public Water Works functioning: " + waterStatus + NL);
    sb.append(INDENT + "Public Water Works provider if NO: " + waterComments + NL);
    sb.append(INDENT + "Commercial Power functioning: " + powerStatus + NL);
    sb.append(INDENT + "Commercial Power provider if NO: " + powerComments + NL);
    sb.append(INDENT + "Commercial Power Stable: " + powerStable + NL);
    sb.append(INDENT + "Commercial Power provider if NO: " + powerStableComments + NL);
    sb.append(INDENT + "Natural Gas supply functioning: " + naturalGasStatus + NL);
    sb.append(INDENT + "Natural Gas supply provider if NO: " + naturalGasComments + NL);
    sb.append(INDENT + "Internet functioning: " + internetStatus + NL);
    sb.append(INDENT + "Internet provider if NO: " + internetComments + NL);

    sb.append(INDENT + "NOAA weather radio functioning: " + noaaStatus + NL);
    sb.append(INDENT + "NOAA transmitter/frequency if NO: " + noaaComments + NL);
    sb.append(INDENT + "NOAA weather radio audio degraded: " + noaaAudioDegraded + NL);
    sb.append(INDENT + "NOAA transmitter/frequency if NO: " + noaaAudioDegradedComments + NL);

    sb.append(INDENT + "Additional Comments: " + additionalComments + NL);
    sb.append(INDENT + "POC: " + poc + NL);

    var m = new FieldSituationMessage(//
        exportedMessage, organization, formLocation, //
        precedence, formDateTime, task, formTo, formFrom, isHelpNeeded, neededHelp, //
        city, county, state, territory, //
        landlineStatus, landlineComments, voipStatus, voipComments, //
        cellPhoneStatus, cellPhoneComments, cellTextStatus, cellTextComments, //
        radioStatus, radioComments, //
        tvStatus, tvComments, satTvStatus, satTvComments, cableTvStatus, cableTvComments, //
        waterStatus, waterComments, //
        powerStatus, powerComments, powerStable, powerStableComments, //
        naturalGasStatus, naturalGasComments, //
        internetStatus, internetComments, //
        noaaStatus, noaaComments, noaaAudioDegraded, noaaAudioDegradedComments, //
        additionalComments, poc, formVersion);

    var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    try {
      var json = objectMapper.writeValueAsString(m);
      var typeName = m.getMessageType().toString();
      Files.writeString(Path.of(path.toString(), typeName + ".json"), json);
      Files
          .writeString(Path.of(path.toString(), dtf.format(date) + "-" + typeName + "_instructions.txt"),
              sb.toString());
    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }

    var ordName = PracticeUtils.getOrdinalLabel(ord);
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", FSR");

  }
}
