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

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
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

  private static final DecimalFormat df = new DecimalFormat("#.000###");
  private static final String SUBJECT_START = "ETO 2024 Spring Drill - ";

  private static final Set<String> TELNET_MODES = Set.of("Telnet", "Mesh");
  private static final Set<String> FM_MODES = Set.of("Packet", "VARA FM");
  private static final Set<String> HF_MODES = Set.of("Pactor", "Ardop", "VARA HF", "Robust Packet");

  private static class Summary implements IWritableTable {
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

    public Map<Zone, List<String>> zoneMIdListMap = new HashMap<>();

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Summary) other;
      return sender.compareTo(o.sender);
    }

    @Override
    public String[] getHeaders() {
      return new String[] {};
    }

    @Override
    public String[] getValues() {
      return new String[] {};
    }

    public void aggregate(Detail detail, CheckInMessage m) {
      details.add(detail);
      checkInMessages.add(m);

      ++checkInReceiveCount;
      // TODO

    }

    public void aggregate(Ics309Message m, int validActivityCount) {
      ics309Messages.add(m);

      ++ics309ReceiveCount;
      ics309LogCount += validActivityCount;

      // TODO
    }

  }

  private static class Detail implements IWritableTable {
    public String sender;
    public String messageId;
    public LocalDateTime dateTime;
    public LatLongPair location;
    public int feedbackCount;
    public String feedback;
    public String apiZoneId;
    public String commentsZoneId;
    public String subjectZoneId;
    public String rmsGateway;
    public String frequency;
    public String distanceMiles;

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
      return new String[] { "From", "MId", "Date/Time", "Latitude", "Longitude", //
          "Feedback Count", "Feedback", "API Zone Id", "Comments Zone Id", "Subject Zone Id", //
          "RMS Gateway", "Frequency", "Distance (MI)" };
    }

    @Override
    public String[] getValues() {
      var lat = location.isValid() ? location.getLatitude() : "0.0";
      var lon = location.isValid() ? location.getLongitude() : "0.0";
      return new String[] { sender, messageId, DTF.format(dateTime), lat, lon, //
          String.valueOf(feedbackCount), feedback, apiZoneId, commentsZoneId, subjectZoneId, //
          rmsGateway, frequency, distanceMiles };
    }
  }

  private RmsGatewayService rmsGatewayService;
  private Map<String, Summary> summaryMap = new HashMap<>();
  private List<IWritableTable> detailList = new ArrayList<IWritableTable>();
  private Map<String, List<IWritableTable>> detailListMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    Ics309Message.setNDisplayActivities(8);
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

    // var apiZoneId = Zone.NOT_FOUND.id;
    var apiZone = Zone.NOT_FOUND;
    var rmsGateway = "n/a";
    var distanceMilesString = "n/a";
    var freqString = "n/a";

    if (rmsGatewayService != null) {
      var serviceResult = rmsGatewayService.getLocationOfRmsGateway(m.from, m.messageId);
      if (serviceResult.isFound()) {
        if (!serviceResult.gatewayCallsign().equalsIgnoreCase("TELNET")) {
          if (serviceResult.location() != null && serviceResult.location().isValid()) {
            rmsGateway = serviceResult.gatewayCallsign();
            var distanceMiles = LocationUtils.computeDistanceMiles(m.mapLocation, serviceResult.location());
            apiZone = Zone.idOf(distanceMiles, serviceResult.frequency());
            distanceMilesString = String.valueOf(distanceMiles);
          }
          if (serviceResult.frequency() > 0) {
            freqString = df.format(serviceResult.frequency() / 1_000_000d) + " MHz";
          }
        } else {
          apiZone = Zone.Telnet;
        }
      } else { // not found in CMS api
        apiZone = Zone.NOT_FOUND;
        rmsGateway = "unknown";
        distanceMilesString = "unknown";
        freqString = "unknown";
      } // end if serviceResult not found
    } // end if rmsGatewayService != null

    // correlation between api band/mode versus check-in band/mode
    if (apiZone != Zone.NOT_FOUND && apiZone != Zone.BAD_CMS) {
      sts.test("band should match Zone's band", apiZone.band.equals(m.band), m.band);
      // TODO fixme
      sts.test("mode should be in Zone's mode", apiZone.modeSet.contains(m.mode), m.mode);
    } else {
      sts.test("band should match Zone's band", false, m.band);
      sts.test("mode should be in Zone's mode", false, m.mode);
    }

    var detail = new Detail();
    detail.sender = m.from;
    detail.messageId = m.messageId;
    detail.dateTime = m.formDateTime;
    detail.location = m.mapLocation;
    detail.feedbackCount = sts.getExplanations().size();
    detail.feedback = String.join("\n", sts.getExplanations());
    detail.apiZoneId = apiZone.id;
    detail.commentsZoneId = commentsZone.id;
    detail.subjectZoneId = subjectZoneId;
    detail.rmsGateway = rmsGateway;
    detail.distanceMiles = distanceMilesString;
    detail.frequency = freqString;
    detailList.add(detail);

    var list = detailListMap.getOrDefault(m.from, new ArrayList<IWritableTable>());
    list.add(detail);
    detailListMap.put(m.from, list);

    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(detail, m);
    summaryMap.put(m.from, summary);

    // TODO RMS map

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

    var validActivityCount = validateActivities(sender, sts, m.activities);
    var summary = summaryMap.getOrDefault(m.from, new Summary());
    summary.aggregate(m, validActivityCount);
    summaryMap.put(m.from, summary);

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  /**
   * @param sender
   * @param sts
   * @param activities
   */
  public int validateActivities(String sender, SimpleTestService sts, List<Activity> activities) {

    var lastDT = LocalDateTime.of(2024, 1, 1, 0, 0, 0);
    LocalDateTime dt = null;
    var lineNumber = 0;
    var validLineCount = 0;
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

    } // end loop over lines
    return validLineCount;
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

    // TODO write detailList, summaryMap, make descriptions from detailList

    WriteProcessor.writeTable(detailList, Path.of(outputPathName, "aggregate-details.csv"));

    // TODO RMS map

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

}
