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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics309Message;
import com.surftools.wimp.message.Ics309Message.Activity;
import com.surftools.wimp.processors.std.FeedbackProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.service.rmsGateway.RmsGatewayService;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2024-05-16 Semi-Annual Drill: ICS-309 from WLE-generated CSV, messages to multiple RMS
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_05_11 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_05_11.class);

  private static final DecimalFormat MHZ_FORMATTER = new DecimalFormat("#.000###");
  private static final DateTimeFormatter KML_FORMATTER = DateTimeFormatter.ofPattern("EEE HH:mm");

  private static final String SUBJECT_START = "ETO 2024 Spring Drill - ";

  private static final Set<String> TELNET_MODES = Set.of("Telnet", "Mesh");
  private static final Set<String> FM_MODES = Set.of("Packet", "VARA FM");
  private static final Set<String> HF_MODES = Set.of("Pactor", "Ardop", "VARA HF", "Robust Packet");

  private class Summary implements IWritableTable {
    public String sender;
    public LatLongPair location;

    public int feedbackCount;
    public String feedback;

    public List<CheckInMessage> checkInMessages = new ArrayList<>();
    public List<Detail> details = new ArrayList<>();
    public List<Ics309Message> ics309Messages = new ArrayList<>();

    public int checkInReceiveCount; // number of check in messages received
    public int ics309ReceiveCount; // number of ics-309 messages received (0 or 1)
    public int ics309LogCount; // number of non-empty ics-309 activities
    public int ics309CheckInSubjectCount; // number of valid Check In subjects

    public Map<Zone, List<String>> zoneMIdListMap = new LinkedHashMap<>();

    public Summary() {
      for (var zone : Zone.values()) {
        // skip over synthetic zones
        if (zone.modeSet == null) {
          continue;
        }
        zoneMIdListMap.put(zone, new ArrayList<String>());
      }
      feedback = "";
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Summary) other;
      return sender.compareTo(o.sender);
    }

    @Override
    public String[] getHeaders() {

      var list = new ArrayList<String>(List
          .of("From", "Latitude", "Longitude", "Feedback Count", "Feedback", //
              "Check In Count", "Ics309 Count", "Ics309 Activities", "Valid Check In Zones"));

      for (var zone : zoneMIdListMap.keySet()) {
        list.add(zone.toString());
      }
      return list.toArray(new String[list.size()]);
    }

    @Override
    public String[] getValues() {
      var latitude = (location != null && location.isValid()) ? location.getLatitude() : "0.0";
      var longitude = (location != null && location.isValid()) ? location.getLongitude() : "0.0";

      var list = new ArrayList<String>(List
          .of(sender, latitude, longitude, String.valueOf(feedbackCount), feedback.trim(), //
              String.valueOf(checkInReceiveCount), String.valueOf(ics309ReceiveCount), String.valueOf(ics309LogCount),
              String.valueOf(ics309CheckInSubjectCount)));

      for (var zoneList : zoneMIdListMap.values()) {
        list.add(String.valueOf(zoneList.size()));
      }

      return list.toArray(new String[list.size()]);
    }

    public void aggregate(Detail detail, CheckInMessage m, SimpleTestService sts) {

      if (checkInReceiveCount == 0) {
        // first check in, assert that there is an ICS-309 while still in process()/specificProcessing()
        var ics309MessageList = mm.getMessagesForSender(detail.sender).get(MessageType.ICS_309);
        sts.test("ICS-309 message should be sent", ics309MessageList != null);
      }

      // last wins
      sender = m.from;
      location = m.mapLocation;

      details.add(detail);
      checkInMessages.add(m);
      ++checkInReceiveCount;

      var zone = Zone.fromId(detail.apiZoneId);
      if (zone.modeSet != null) {
        var list = zoneMIdListMap.get(zone);
        list.add(detail.messageId);
        // check for multiple messages to same ZoneId
        if (list.size() > 1) {
          sts.test("Zone id: " + detail.apiZoneId + " has multiple messages", false, String.join(", ", list));
        } else {
          sts.test("Zone id: " + detail.apiZoneId + " has multiple messages", true);
        }
      }

      if (detail.feedbackCount > 0) {
        feedbackCount += detail.feedbackCount;
        feedback += "\n" + detail.feedback;
      }

      var gatewayMap = baseGatewayMap
          .getOrDefault(detail.baseGatewayCall, new LinkedHashMap<GatewayKey, List<Detail>>());
      var key = new GatewayKey(detail.frequency, detail.rmsGateway);
      var list = gatewayMap.getOrDefault(key, new ArrayList<Detail>());
      list.add(detail);
      gatewayMap.put(key, list);
      baseGatewayMap.put(detail.baseGatewayCall, gatewayMap);

      // last wins
      var baseLocation = detail.gatewayLocation;
      baseGatewayLocationMap.put(detail.baseGatewayCall, baseLocation);
    }

    public void aggregate(Ics309Message m, ActivitySummary activitySummary, SimpleTestService sts) {
      ics309Messages.add(m);

      sts.test("At least 1 Winlink Check In message sent", checkInReceiveCount > 0);

      ++ics309ReceiveCount;
      ics309LogCount += activitySummary.validActivityCount;
      ics309CheckInSubjectCount += activitySummary.validCheckInSubjectCount;

      // no check ins, wrong count of check ins
      sts
          .test("ICS-309 log count of activities should match # of Check In messages received",
              activitySummary.validCheckInSubjectCount == checkInReceiveCount,
              "ICS-309 log has " + activitySummary.validCheckInSubjectCount
                  + " messages with valid ZoneId in Subject, but " + checkInReceiveCount
                  + " Check In messages received");

      if (sts.getExplanations().size() > 0) {
        feedbackCount += sts.getExplanations().size();
        feedback += "\n" + String.join("\n", sts.getExplanations());
      }

    }

  }

  private static class Detail implements IWritableTable {
    public String sender;
    public String messageId;
    public LocalDateTime dateTime;
    public LatLongPair senderLocation;
    public int feedbackCount;
    public String feedback;
    public String apiZoneId;
    public String commentsZoneId;
    public String subjectZoneId;
    public String rmsGateway;
    public int frequency;
    public String distanceMiles;
    public String baseGatewayCall;
    public LatLongPair gatewayLocation;

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Detail) other;
      var cmp = sender.compareTo(o.sender);
      if (cmp != 0) {
        return cmp;
      }

      return dateTime.compareTo(o.dateTime);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "MId", "Date/Time", "Sender Latitude", "Sender Longitude", //
          "Feedback Count", "Feedback", "API Zone Id", "Comments Zone Id", "Subject Zone Id", //
          "RMS Gateway", "Frequency", "Distance (MI)", //
          "Base Gateway", "Gateway Latitude", "Gateway Longitude" };
    }

    @Override
    public String[] getValues() {
      var senderLatitude = senderLocation.isValid() ? senderLocation.getLatitude() : "0.0";
      var senderLongitude = senderLocation.isValid() ? senderLocation.getLongitude() : "0.0";
      var gatewayLatitude = gatewayLocation.isValid() ? gatewayLocation.getLatitude() : "0.0";
      var gatewayLongitude = gatewayLocation.isValid() ? gatewayLocation.getLongitude() : "0.0";
      var freqString = (frequency == -1) ? "n/a" : MHZ_FORMATTER.format(frequency / 1_000_000d) + " MHz";

      return new String[] { sender, messageId, DTF.format(dateTime), senderLatitude, senderLongitude, //
          String.valueOf(feedbackCount), feedback, apiZoneId, commentsZoneId, subjectZoneId, //
          rmsGateway, freqString, distanceMiles, //
          baseGatewayCall, gatewayLatitude, gatewayLongitude };
    }
  }

  private class GatewayKey implements Comparable<GatewayKey> {
    public final int frequency;
    public final String callsign;

    public GatewayKey(int frequency, String callsign) {
      this.frequency = frequency;
      this.callsign = callsign;
    }

    @Override
    public int compareTo(GatewayKey o) {
      var cmp = frequency - o.frequency;
      if (cmp != 0) {
        return cmp;
      }
      return callsign.compareTo(o.callsign);

    }
  };

  private RmsGatewayService rmsGatewayService;
  private Map<String, Summary> summaryMap = new HashMap<>();
  private List<IWritableTable> detailList = new ArrayList<IWritableTable>();
  private Map<String, List<IWritableTable>> detailListMap = new HashMap<>();

  private Map<String, Map<GatewayKey, List<Detail>>> baseGatewayMap = new HashMap<>();
  private Map<String, LatLongPair> baseGatewayLocationMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    doStsFieldValidation = false;

    messageTypesRequiringSecondaryAddress = Set.of(MessageType.ICS_309);

    rmsGatewayService = new RmsGatewayService(cm);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var type = message.getMessageType();
    if (type == MessageType.CHECK_IN) {
      handleCheckInPayload((CheckInMessage) message);
    } else if (type == MessageType.ICS_309) {
      handleIcs309Message((Ics309Message) message);
    } else {
      logger.warn("Unexpected message type: " + message.getMessageType() + " for messageId: " + message.messageId);
    }
  }

  protected void handleCheckInPayload(CheckInMessage m) {
    outboundMessageSubject = "Feedback on your ETO Spring Drill 2024 Check In message, mId: ";

    // box 0: meta
    getCounter("versions").increment(m.version);
    sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization);

    // box 4: comments; should be a zoneId
    var comments = m.comments;
    sts.testIfPresent("Comments should not be null or empty", comments);
    var commentsZone = Zone.fromId(comments);
    sts.test("Comments should be a valid Zone ID", commentsZone != Zone.INVALID_ID, comments);

    var subject = m.subject;
    sts.testIfPresent("Subject should not be null or empty", subject);
    sts
        .test("Subject should start with " + SUBJECT_START,
            subject.toLowerCase().startsWith(SUBJECT_START.toLowerCase()), subject);

    var subjectZoneId = subject.substring(SUBJECT_START.length());
    var subjectZone = Zone.fromId(subjectZoneId);
    sts.test("Subject properly formatted", subjectZone != Zone.INVALID_ID, subject);

    if (commentsZone != null && subjectZone != null) {
      sts.test("Zone ID from Subject should match Zone ID from Comments", commentsZone.equals(subjectZone));
    } else {
      sts.test("Zone ID from Subject should match Zone ID from Comments", false);
    }

    // box 1: station
    sts.testOnOrAfter("Message should be composed on or after #EV", windowOpenDT, m.formDateTime, DTF);
    sts.testOnOrBefore("Message should be composed on or before #EV", windowCloseDT, m.formDateTime, DTF);
    sts.testIfPresent("Contact name should not be null or empty", m.contactName);
    sts
        .test("Initial operators should be same as sender",
            m.initialOperators != null && m.initialOperators.equalsIgnoreCase(m.from), m.initialOperators);

    // box 2: session
    sts.test("Session Type should be #EV", "Exercise", m.status);

    // box 3: location
    if (!m.formLocation.isValid()) {
      m.mapLocation = m.msgLocation;
    }

    sts.testIfPresent("Location should not be null or empty", m.locationString);

    var apiZone = Zone.NOT_FOUND;
    var rmsGateway = "n/a";
    var baseRmsGateway = "n/a";
    var gatewayLocation = LatLongPair.ZERO_ZERO;
    var distanceMilesString = "n/a";
    var frequency = -1;

    if (rmsGatewayService != null) {
      var serviceResult = rmsGatewayService.getLocationOfRmsGateway(m.from, m.messageId);
      if (serviceResult.isFound()) {
        if (!serviceResult.gatewayCallsign().equalsIgnoreCase("TELNET")) {
          if (serviceResult.location() != null && serviceResult.location().isValid()) {
            rmsGateway = serviceResult.gatewayCallsign();
            var distanceMiles = LocationUtils.computeDistanceMiles(m.mapLocation, serviceResult.location());
            apiZone = Zone.idOf(distanceMiles, serviceResult.frequency());
            distanceMilesString = String.valueOf(distanceMiles);
            gatewayLocation = serviceResult.location();
            baseRmsGateway = serviceResult.baseGatewayCallsign();
          }
          if (serviceResult.frequency() > 0) {
            frequency = serviceResult.frequency();
          }
        } else {
          apiZone = Zone.Telnet;
        }
      } else { // not found in CMS api
        apiZone = Zone.NOT_FOUND;
        rmsGateway = "unknown";
        distanceMilesString = "unknown";
      } // end if serviceResult not found
    } // end if rmsGatewayService != null

    // correlation between api band/mode versus check-in band/mode
    if (apiZone != Zone.NOT_FOUND && apiZone != Zone.BAD_CMS) {
      sts.test("band should match Zone's band", apiZone.band.equals(m.band), m.band);
      sts.test("mode should be in Zone's mode", apiZone.modeSet.contains(m.mode), m.mode);
    } else {
      sts.test("band should match Zone's band", false, m.band);
      sts.test("mode should be in Zone's mode", false, m.mode);
    }

    var detail = new Detail();
    detail.sender = m.from;
    detail.messageId = m.messageId;
    detail.dateTime = m.formDateTime;
    detail.senderLocation = m.mapLocation;
    detail.feedbackCount = sts.getExplanations().size();
    detail.feedback = String.join("\n", sts.getExplanations());
    detail.apiZoneId = apiZone.id;
    detail.commentsZoneId = commentsZone.id;
    detail.subjectZoneId = subjectZoneId;
    detail.rmsGateway = rmsGateway;
    detail.distanceMiles = distanceMilesString;
    detail.frequency = frequency;
    detail.baseGatewayCall = baseRmsGateway;
    detail.gatewayLocation = gatewayLocation;

    detailList.add(detail);

    var list = detailListMap.getOrDefault(m.from, new ArrayList<IWritableTable>());
    list.add(detail);
    detailListMap.put(m.from, list);

    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(detail, m, sts);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  protected void handleIcs309Message(Ics309Message m) {
    outboundMessageSubject = "Feedback on your ETO Spring Drill 2024 ICS-309 message, mId: ";

    getCounter("ICS-309 versions").increment(m.version);

    if (dumpIds.contains(m.messageId) || dumpIds.contains(m.from)) {
      logger.info("### call: " + m.from + "\n" + sts.toString());
    }

    sts.test("Agency/Group name should be #EV", "EmComm Training Organization", m.organization);
    sts.test("Task # should be #EV", "240511", m.taskNumber);

    try {
      sts.test("Date/Time Prepared properly formatted", true);
      sts
          .testOnOrAfter("Date/Time Prepared should be on or after #EV", windowOpenDT,
              LocalDateTime.from(DTF.parse(m.dateTimePrepared)), DTF);
      sts
          .testOnOrBefore("Date/Time Prepared should be on or before #EV", windowCloseDT,
              LocalDateTime.from(DTF.parse(m.dateTimePrepared)), DTF);
    } catch (Exception e) {
      sts.test("Date/Time Prepared properly formatted", false);
    }

    sts.test("Operational Period should be #EV", "05/07-05/11", m.operationalPeriod);
    sts.test("Task Name should be #EV", "Carrington Event Preparation Drill", m.taskName);
    sts.testIfPresent("Operator Name should be provided", m.operatorName);
    sts.test("Station Id should be #EV", m.from, m.stationId);
    sts.test("Page # should be #EV", "1", m.page);

    var activitySummary = validateActivities(sender, sts, m.activities);
    sts.setExplanationPrefix("");
    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(m, activitySummary, sts);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  static record ActivitySummary(int validActivityCount, int validCheckInSubjectCount) {
  }

  /**
   *
   * @param sender
   * @param sts
   * @param activities
   * @return ActivitySummary
   */
  public ActivitySummary validateActivities(String sender, SimpleTestService sts, List<Activity> activities) {

    var lastDT = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    LocalDateTime dt = null;
    var lineNumber = 0;
    var validLineCount = 0;
    var validCheckInSubject = 0;
    for (var a : activities) {
      ++lineNumber;

      if (a == null || !a.isValid()) {
        continue;
      }
      ++validLineCount;

      sts.setExplanationPrefix("line " + lineNumber + ": ");

      try {
        dt = parse(a.dateTimeString().trim());
        sts.test("Should have valid activity Date/Time" + lineNumber, true);

        if (lineNumber > 1) {
          sts.testOnOrAfter("Should be ascending Date/Time on line: " + lineNumber, lastDT, dt, DTF);
        }

        sts
            .testOnOrAfter("Activity Date/Time should be on or after #EV", windowOpenDT, dt, DTF,
                ", not " + DTF.format(dt) + ", on line: " + lineNumber);

        sts
            .testOnOrBefore("Activity Date/Time should be on or before #EV", windowCloseDT, dt, DTF,
                ", not " + DTF.format(dt) + ", on line: " + lineNumber);

      } catch (Exception e) {
        sts.test("Should have valid activity Date/Time", false, "'" + a.dateTimeString() + "', on line: " + lineNumber);
      }

      if (dt != null) {
        lastDT = dt;
      }

      sts.test("From should be #EV", sender, a.from());
      sts.test("To should contain: ETO-DRILL", a.to().contains("ETO-DRILL"), a.to());
      sts.testIfPresent("Subject should not be null or empty", a.subject());
      sts
          .test("Subject should start with " + SUBJECT_START,
              a.subject().toLowerCase().startsWith(SUBJECT_START.toLowerCase()), a.subject());
      var subjectZoneId = a.subject().substring(SUBJECT_START.length());
      var subjectZone = Zone.fromId(subjectZoneId);
      sts.test("Subject properly formatted", subjectZone != Zone.INVALID_ID, a.subject());
      if (subjectZone != Zone.INVALID_ID) {
        ++validCheckInSubject;
      }

    } // end loop over lines
    return new ActivitySummary(validLineCount, validCheckInSubject);
  }

  static enum Zone {
    Telnet("Telnet", TELNET_MODES, "Telnet", null, null), //
    VHF("VHF", FM_MODES, "VHF", null, null), //
    UHF("UHF", FM_MODES, "UHF", null, null), //
    HF_1("HF-1", HF_MODES, "HF", 0, 101), //
    HF_101("HF-101", HF_MODES, "HF", 101, 301), //
    HF_301("HF-301", HF_MODES, "HF", 301, 600), //
    HF_601("HF-601", HF_MODES, "HF", 601, 1200), //
    HF_1201("HF-1201", HF_MODES, "HF", 1201, 2000), //
    HF_2001("HF-2001", HF_MODES, "HF", 2000, null), //
    NOT_FOUND("not found in CMS", null, null, null, null), // api didn't return
    BAD_CMS("bad CMS data", null, null, null, null), // api returned, but could not match
    INVALID_ID("invalid id in comments/subject", null, null, null, null), // comments/subject id did not match
    ;

    private final String id;
    private final Set<String> modeSet;
    private final String band;
    private final Integer minDistance;
    private final Integer maxDistance;

    private Zone(String id, Set<String> modeSet, String band, Integer minDistance, Integer maxDistance) {
      this.id = id;
      this.modeSet = modeSet;
      this.band = band;
      this.minDistance = minDistance;
      this.maxDistance = maxDistance;
    }

    @Override
    public String toString() {
      return this.id;
    }

    public static Zone fromId(String string) {
      if (string == null) {
        return INVALID_ID;
      }

      for (Zone key : Zone.values()) {
        if (key.toString().equals(string)) {
          return key;
        }
      }
      return INVALID_ID;
    }

    public static Zone idOf(int distanceMiles, int frequency) {
      if (frequency >= 30_000_000 && frequency <= 300_000_000) {
        return VHF;
      }

      if (frequency >= 300_000_000) {
        return UHF;
      }

      for (var zone : values()) {
        if (zone.id.equals("Telnet") || zone.minDistance == null) {
          continue;
        }

        var min = zone.minDistance;
        var max = zone.maxDistance;

        if (min <= distanceMiles && (max == null || distanceMiles <= max)) {
          return zone;
        }
      }
      return BAD_CMS;
    }
  }

  @Override
  public void postProcess() {
    super.postProcess();

    WriteProcessor.writeTable(detailList, Path.of(outputPathName, "aggregate-details.csv"));
    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(summaryMap.values()),
            Path.of(outputPathName, "aggregate-summary.csv"));

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);

    doKml();
  }

  record KmlEntry(Supplier<String> contentMaker, String fileName) {
  }

  private void doKml() {
    final var KML_TEXT = """
        <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2" xmlns:gx="http://www.google.com/kml/ext/2.2" xmlns:kml="http://www.opengis.net/kml/2.2" xmlns:atom="http://www.w3.org/2005/Atom">
        <Document id="MAP_NAME">
          <name>MAP_NAME</name>
          <description>MAP_DESCRIPTION</description>
          <Style id="senderpin">
              <IconStyle>
                <Icon>
                  <href>http://maps.google.com/mapfiles/kml/paddle/blu-blank.png</href>
                </Icon>
              </IconStyle>
            </Style>
            <Style id="gatewaypin">
              <IconStyle>
                <Icon>
                  <href>http://maps.google.com/mapfiles/kml/paddle/red-stars.png</href>
                </Icon>
              </IconStyle>
            </Style>
            CONTENT
        </Document>
        </kml>
           """;

    final List<KmlEntry> entries = List
        .of(new KmlEntry(makeSenderPlacemarks, "kml-stations.kml"),
            new KmlEntry(makeGatewayPlacemarks, "kml-gateways.kml"),
            new KmlEntry(makeNetworkPlacemarks, "kml-network.kml"));

    for (var entry : entries) {
      var kmlText = KML_TEXT;
      kmlText = kmlText.replaceAll("MAP_NAME", cm.getAsString(Key.EXERCISE_NAME));
      kmlText = kmlText.replaceAll("MAP_DESCRIPTION", cm.getAsString(Key.EXERCISE_DESCRIPTION));
      kmlText = kmlText.replaceAll("CONTENT", entry.contentMaker().get());

      var kmlFile = Path.of(outputPath.toString(), entry.fileName).toFile();
      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(kmlFile));
        writer.write(kmlText);
        writer.close();
        logger.info("wrote KML to: " + kmlFile.toString());
      } catch (Exception e) {
        logger.error("Exception writing kml file: " + kmlFile.toString() + e.getLocalizedMessage());
      }
    }
  }

  Supplier<String> makeSenderPlacemarks = () -> {
    var sb = new StringBuilder();

    for (var summary : summaryMap.values()) {
      var sender = summary.sender;
      var details = summary.details;

      if (details.size() > 0 && summary.location != null && summary.location.isValid()) {

        Collections.sort(details);
        var d = new StringBuilder();
        for (var detail : details) {
          if (detail.apiZoneId == Zone.Telnet.toString()) {
            d.append(KML_FORMATTER.format(detail.dateTime) + " via Telnet" + "\n");
          } else {
            d
                .append(KML_FORMATTER.format(detail.dateTime) + " via " + detail.rmsGateway + " (" + detail.apiZoneId
                    + ", " + detail.distanceMiles + " mi)" + "\n");
          }
        }
        var description = d.toString();

        sb.append(" <Placemark>\n");
        sb.append(" <name>" + sender + "</name>\n");
        sb.append(" <styleUrl>#senderpin</styleUrl>\n");
        sb.append("<description>\n");
        sb.append(description);
        sb.append("</description>\n");
        sb.append(" <Point>\n");
        sb
            .append(" <coordinates>" + summary.location.getLongitude() + "," + summary.location.getLatitude()
                + "</coordinates>\n");
        sb.append(" </Point>\n");
        sb.append(" </Placemark>\n");
      }
    }

    var s = sb.toString();
    return s;
  };

  private Supplier<String> makeGatewayPlacemarks = () -> {
    var sb = new StringBuilder();

    for (var baseGateway : baseGatewayMap.keySet()) {
      if (baseGateway.equals("n/a")) {
        continue;
      }

      var gatewayMap = baseGatewayMap.get(baseGateway);
      var baseLocation = baseGatewayLocationMap.get(baseGateway);
      var longitude = baseLocation.isValid() ? baseLocation.getLongitude() : "0.0";
      var latitude = baseLocation.isValid() ? baseLocation.getLatitude() : "0.0";

      var gatewayKeys = new ArrayList<GatewayKey>(gatewayMap.keySet());
      Collections.sort(gatewayKeys);

      var d = new StringBuilder();
      for (var key : gatewayKeys) {
        d.append("Frequency: " + MHZ_FORMATTER.format(key.frequency) + "MHz\n");
        var details = gatewayMap.get(key);
        Collections.sort(details);
        for (var detail : details) {
          d
              .append("   " + KML_FORMATTER.format(detail.dateTime) + ", from: " + detail.sender + " ("
                  + detail.apiZoneId.toString() + ", " + detail.distanceMiles + " mi)" + "\n");
        }
      }

      var description = d.toString();

      sb.append(" <Placemark>\n");
      sb.append(" <name>" + baseGateway + "</name>\n");
      sb.append(" <styleUrl>#gatewaypin</styleUrl>\n");
      sb.append(" <description>\n");

      sb.append(description);

      sb.append(" </description>\n");
      sb.append(" <Point>\n");
      sb.append(" <coordinates>" + longitude + "," + latitude + "</coordinates>\n");
      sb.append(" </Point>\n");
      sb.append(" </Placemark>\n");
    }

    var s = sb.toString();
    return s;
  };

  private Supplier<String> makeNetworkPlacemarks = () -> {
    var drawnSet = new HashSet<String>();
    var sb = new StringBuilder();

    for (var d : detailList) {
      var detail = (Detail) d;
      var sender = detail.sender;
      var gateway = detail.baseGatewayCall;

      if (gateway.equals("n/a")) {
        continue;
      }

      var drawnKey = sender + "-" + gateway;
      if (drawnSet.contains(drawnKey)) {
        continue;
      }
      drawnSet.add(drawnKey);
      sb.append(" <Placemark>\n");
      sb.append(" <name>" + drawnKey + "</name>\n");
      sb.append(" <LineString>\n");

      sb.append(" <description>\n");
      sb.append(drawnKey + " (" + detail.distanceMiles + " mi)");
      sb.append(" </description>\n");

      sb
          .append(" <coordinates>" + detail.senderLocation.getLongitude() + "," + detail.senderLocation.getLatitude() //
              + " " + detail.gatewayLocation.getLongitude() + "," + detail.gatewayLocation.getLatitude()
              + "</coordinates>\n");
      sb.append(" </LineString>\n");
      sb.append(" </Placemark>\n");
    }

    var s = sb.toString();
    return s;
  };

}
