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

package com.surftools.wimp.processors.exercise.eto_2025;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.MultiDateTimeParser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.HospitalBedMessage;
import com.surftools.wimp.message.WxLocalMessage;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor to compare Hospital Bed, FSR and Local WX sent by ETO team to ETO-DRILL to spreadsheet values Hospital Bed
 * from ETO team will also be used for 2025-04 exercise
 *
 * @author bobt
 *
 */
public class ETO_Spring_Precheck extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_Spring_Precheck.class);

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

    public HospitalBedMessage hospitalBedMessage;
    public WxLocalMessage wxLocalMessage;
    public FieldSituationMessage fsrMessage;
    @SuppressWarnings("unused")
    public HospitalBedMessage db_hospitalBedMessage;
    @SuppressWarnings("unused")
    public WxLocalMessage db_wxLocalMessage;
    @SuppressWarnings("unused")
    public FieldSituationMessage db_fsrMessage;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list.addAll(Arrays.asList(new String[] { "Hospital Bed", "Local WX", "FSR", //
      }));
      return list.toArray(new String[0]);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list.addAll(Arrays.asList(new String[] { mId(hospitalBedMessage), mId(wxLocalMessage), mId(fsrMessage), }));
      return list.toArray(new String[0]);
    };
  }

  final static Map<Integer, DecimalFormat> decimalFormatMap = Map
      .of(0, new DecimalFormat("0."), //
          1, new DecimalFormat("0.#"), //
          2, new DecimalFormat("0.##"), //
          3, new DecimalFormat("0.###"), //
          4, new DecimalFormat("0.####"));

  final static List<MessageType> acceptableMessageTypesList = List
      .of( // order matters, last location wins,
          MessageType.HOSPITAL_BED, //
          MessageType.WX_LOCAL, //
          MessageType.FIELD_SITUATION //
      );

  String editedSender = null;

  private Map<String, FieldSituationMessage> cityFsrMap = new HashMap<>();
  private Map<String, HospitalBedMessage> cityHbMap = new HashMap<>();
  private Map<String, WxLocalMessage> cityWxMap = new HashMap<>();

  record MissingRecord(String city, MessageType messageType) implements Comparable<MissingRecord> {

    @Override
    public int compareTo(MissingRecord o) {
      return city.compareTo(o.city);
    }

  };

  Set<MissingRecord> missingHBSet = new TreeSet<>();
  Set<MissingRecord> missingPerfectHBSet = new TreeSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    // #MM must define acceptableMessages
    acceptableMessageTypesSet.addAll(acceptableMessageTypesList);

    super.initialize(cm, mm, logger);

    loadHospitalBedMessages();
    loadWxLocalMessages();
    loadFsrMessages();
  }

  private void loadWxLocalMessages() {
    var inputPath = Path.of(cm.getAsString(Key.PATH), "lwx.csv");
    var skipLines = 2;
    var lines = ReadProcessor.readCsvFileIntoFieldsArray(inputPath, ',', false, skipLines);

    // Call Sign,Observer,Setup,Exercise,Date Time,Location,City,State,County,Latitude,Longitude,Sky Cover,Current
    // Conditions,Temperature,Humidity,Dew Point,Baro Press,Rise,Wind Speed,Direction,Gusts,Max Gust,Precipitation,NWS
    // Levels,Notes
    for (var line : lines) {
      var mId = makeMessageId("WX");
      var from = line[0];
      var to = "ETO-DRILL";
      var subject = "WX Report";

      var dtString = line[4];
      var dtFields = dtString.split(" ");
      var timeFields = dtFields[1].split(":");
      var localTime = LocalTime.of(12 + Integer.valueOf(timeFields[0]), Integer.valueOf(timeFields[1]));
      var dateTime = LocalDateTime.of(LocalDate.of(2025, 5, 5), localTime);

      var msgLocation = new LatLongPair(line[9], line[10]);
      var locationSource = "eto";
      var mime = "";
      var plainContent = String.join(",", line);
      var attachments = new HashMap<String, byte[]>();
      var isP2p = false;

      var exportedMessage = new ExportedMessage(mId, from, from, to, to, "", //
          subject, dateTime, //
          msgLocation, locationSource, //
          mime, plainContent, attachments, isP2p, "lwx.csv");

      var organization = line[2];
      var formLocation = msgLocation;
      var formDateTime = dateTime;
      var locationString = line[5];
      var city = line[6];
      var state = line[7];
      var county = line[8];
      var temperature = line[14] + " F";
      var windspeed = line[19] + " MPH";
      var range = "";
      var maxGusts = line[22] + " MPH";
      var nwsLevels = line[24];
      var comments = line[25];

      var warningType = "";
      var warningField = "";
      if (nwsLevels != null) {
        var fields = nwsLevels.split(" ");
        if (fields != null && fields.length >= 2) {
          warningType = fields[1];
          warningField = fields[0];
        }
      }

      var wxMessage = new WxLocalMessage(exportedMessage, organization, formLocation, //
          formDateTime, locationString, city, state, county, //
          temperature, windspeed, range, maxGusts, warningType, warningField, comments);

      cityWxMap.put(city.toUpperCase(), wxMessage);
    }

    writeTable("dbWx.cvs", cityWxMap.values());
    if (cityWxMap.size() == 0) {
      logger.error("### nothing read from WX spreadsheet. Check configuration. Exiting!!!");
      System.exit(1);
    }
  }

  private void loadHospitalBedMessages() {
    var inputPath = Path.of(cm.getAsString(Key.PATH), "hbr.csv");
    var skipLines = 2;
    var lines = ReadProcessor.readCsvFileIntoFieldsArray(inputPath, ',', false, skipLines);

    // Assignee, City, Setup, Exercise, Report Date Time, Reporting Facility, Street Address, City, State, Zip,
    // Latitude, Longitude, Contact Person, Contact Phone, Contact Email, Emergency Beds, Critical Care Beds, ALL Other
    // Beds, Additional Comments

    var mdtp = new MultiDateTimeParser(List.of("MM/dd/yy HH:mm a"));

    for (var line : lines) {
      var mId = makeMessageId("HB");
      var from = line[0].split(",")[1].strip();
      var to = "ETO-DRILL";

      var subject = "TBD";
      var dateTime = mdtp.parse(line[4]);
      var msgLocation = new LatLongPair(line[10], line[11]);
      var locationSource = "eto";
      var mime = "";
      var plainContent = String.join(",", line);
      var attachments = new HashMap<String, byte[]>();
      var isP2p = false;

      var exportedMessage = new ExportedMessage(mId, from, from, to, to, "", //
          subject, dateTime, //
          msgLocation, locationSource, //
          mime, plainContent, attachments, isP2p, "hbr.csv");

      var organization = line[2];
      var formLocation = msgLocation;
      var formDateTime = dateTime;

      var facility = line[5];
      var streetAddress = line[6];
      var city = line[7];
      var state = line[8];
      var zip = line[9];

      var contactPerson = line[12];
      var contactPhone = line[13];
      var contactEmail = line[14];

      var emergencyBedCount = line[15];
      var criticalBedCount = line[16];
      var allOtherBedCounts = line[17];
      var notes = "";
      var totalBedCount = String.valueOf(Integer.parseInt(emergencyBedCount) + Integer.parseInt(criticalBedCount));
      var comments = line[18];

      var hbMessage = new HospitalBedMessage(exportedMessage, formDateTime, formLocation, //
          organization, true, facility, //
          streetAddress, city, state, zip, //
          contactPerson, contactPhone, contactEmail, //
          emergencyBedCount, notes, //
          allOtherBedCounts, notes, //
          allOtherBedCounts, notes, //
          allOtherBedCounts, notes, //
          allOtherBedCounts, notes, //
          criticalBedCount, notes, //
          notes, allOtherBedCounts, notes, //
          notes, allOtherBedCounts, notes, //
          totalBedCount, comments, "ETO-001");

      cityHbMap.put(city.toUpperCase(), hbMessage);

      var missingRecord = new MissingRecord(city, MessageType.HOSPITAL_BED);
      missingHBSet.add(missingRecord);
      missingPerfectHBSet.add(missingRecord);
    }

    writeTable("dbHb.cvs", cityHbMap.values());
    if (cityHbMap.size() == 0) {
      logger.error("### nothing read from HB spreadsheet. Check configuration. Exiting!!!");
      System.exit(1);
    }
  }

  private void loadFsrMessages() {
    var inputPath = Path.of(cm.getAsString(Key.PATH), "fsr.csv");
    var skipLines = 2;
    var lines = ReadProcessor.readCsvFileIntoFieldsArray(inputPath, ',', false, skipLines);

    // Assignee, Setup / Agency, PRECEDENCE, DATE/TIME, TASK #, FROM, TO, INFO (CC), Safety Needed
    // City, County, State, LAT. LON, POTS, VOIP, Cell voice, Text Carrier, Cell text, Text Carrier,
    // Radio Station, OATV,Station, Satellite TV, Cable TV, Provider, Public Water, Commercial Power, Provider
    // Power Stable, Provider, Natural Gas, Internet, Provider, NOAA WX Functioning, NOAA WX Degraded,
    // Additional Comments, POC

    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'");
    for (var line : lines) {
      var mId = makeMessageId("FS");
      var from = line[0].split(",")[1].strip();
      var to = line[6];

      var subject = "TBD";
      var dateTime = LocalDateTime.parse(line[3], dtf).truncatedTo(ChronoUnit.MINUTES);
      var msgLocation = new LatLongPair(line[12], line[13]);
      var locationSource = "eto";
      var mime = "";
      var plainContent = String.join(",", line);
      var attachments = new HashMap<String, byte[]>();
      var isP2p = false;

      var exportedMessage = new ExportedMessage(mId, from, from, to, to, "", //
          subject, dateTime, //
          msgLocation, locationSource, //
          mime, plainContent, attachments, isP2p, "fsr.csv");

      var organization = line[1];
      var formLocation = msgLocation;
      var formDateTimeString = line[3];

      var precedence = line[2].replaceAll("/", "/ ");
      var task = line[4];
      var formTo = line[6];
      var formFrom = line[5];
      var safetyNeeded = line[8];

      var city = line[9];
      var county = line[10];
      var state = line[11];
      var territory = "";

      var landlineStatus = line[14].replace("Unknown", "Unknown - N/A");
      var landlineComments = "";
      var voipStatus = line[15].replace("Unknown", "Unknown - N/A");
      var voipComments = "";

      var cellPhoneStatus = line[16].replace("Unknown", "Unknown - N/A");
      var cellPhoneComments = line[17];
      var cellTextStatus = line[18].replace("Unknown", "Unknown - N/A");
      var cellTextComments = line[19];

      var radioStatus = line[20].replace("Unknown", "Unknown - N/A");
      var radioComments = line[21];
      var tvStatus = line[22].replace("Unknown", "Unknown - N/A");
      var tvComments = line[23];
      var satTvStatus = line[24].replace("Unknown", "Unknown - N/A");
      var satTvComments = "";
      var cableTvStatus = line[25].replace("Unknown", "Unknown - N/A");
      var cableTvComments = line[26];

      var waterStatus = line[27].replace("Unknown", "Unknown - N/A");
      var waterComments = "";

      var powerStatus = line[28].replace("Unknown", "Unknown - N/A");
      var powerComments = line[29];
      var powerStableStatus = line[30].replace("Unknown", "Unknown - N/A");
      var powerStableComments = line[31];

      var naturalGasStatus = line[32].replace("Unknown", "Unknown - N/A");
      var naturalGasComments = "";

      var internetStatus = line[33].replace("Unknown", "Unknown - N/A");
      var internetComments = line[34];

      var noaaStatus = line[35].replace("Unknown", "Unknown - N/A");
      var noaaComments = "";
      var noaaAudioDegraded = line[36].replace("Unknown", "Unknown - N/A");
      var noaaAudioDegradedComments = "";

      var additionalComments = line[37];
      var poc = line[38];

      var fsrMessage = new FieldSituationMessage(exportedMessage, organization, formLocation, //
          precedence, formDateTimeString, task, formTo, formFrom, //
          safetyNeeded, safetyNeeded, //
          city, county, state, territory, //
          landlineStatus, landlineComments, //
          voipStatus, voipComments, //
          cellPhoneStatus, cellPhoneComments, //
          cellTextStatus, cellTextComments, //
          radioStatus, radioComments, //
          tvStatus, tvComments, //
          satTvStatus, satTvComments, //
          cableTvStatus, cableTvComments, //
          waterStatus, waterComments, //
          powerStatus, powerComments, //
          powerStableStatus, powerStableComments, //
          naturalGasStatus, naturalGasComments, //
          internetStatus, internetComments, //
          noaaStatus, noaaComments, //
          noaaAudioDegraded, noaaAudioDegradedComments, //
          additionalComments, poc, "ETO-001");

      cityFsrMap.put(city.toUpperCase(), fsrMessage);
    }

    writeTable("dbFsr.cvs", cityFsrMap.values());
    if (cityFsrMap.size() == 0) {
      logger.error("### nothing read from FSR spreadsheet. Check configuration. Exiting!!!");
      System.exit(1);
    }
  }

  private String makeMessageId(String prefix) {
    final var dtf = DateTimeFormatter.ofPattern("MMddHHmmss");
    return prefix + dtf.format(LocalDateTime.now());
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

    summary.to = "ETO-DRILL";

    var type = message.getMessageType();
    if (type == MessageType.HOSPITAL_BED) {
      handle_HospitalBedMessage(summary, (HospitalBedMessage) message);
    } else if (type == MessageType.WX_LOCAL) {
      handle_WxLocalMessage(summary, (WxLocalMessage) message);
    } else if (type == MessageType.FIELD_SITUATION) {
      handle_FsrMessage(summary, (FieldSituationMessage) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_HospitalBedMessage(Summary summary, HospitalBedMessage m) {
    sts.setExplanationPrefix("(hb) ");
    summary.hospitalBedMessage = m;

    var db = cityHbMap.get(m.city.toUpperCase());
    if (db == null) {
      logger.warn("### NO HB in db for city: " + m.city + " from call: " + m.from);
      sts.getExplanations().add("no HB spreadsheet entry for city: " + m.city);
      return;
    }
    summary.db_hospitalBedMessage = db;
    dbMatch("Message Sender", db.from, m.from);

    dbMatch("Organization", db.organization, m.organization);
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    dbMatch("Form Date/Time", db.formDateTime.toString(), m.formDateTime.truncatedTo(ChronoUnit.MINUTES).toString());
    dbMatch("Reporting Facility", db.facility, m.facility);
    dbMatch("Street Address", db.streetAddress, m.streetAddress);
    dbMatch("City", db.city, m.city);
    dbMatch("State", db.state, m.state);
    dbMatch("Zip", db.zip, m.zip);

    dbMatchLatLon("Form Latitude", 3, db.formLocation.getLatitude(), m.formLocation.getLatitude());
    dbMatchLatLon("Form Longitude", 3, db.formLocation.getLongitude(), m.formLocation.getLongitude());

    dbMatch("Contact Person", db.contactPerson, m.contactPerson);
    dbMatch("Contact Phone Number", db.contactPhone, m.contactPhone);
    dbMatch("Contact Email", db.contactEmail, m.contactEmail);

    dbMatchBed("Emergency Bed Count", db.emergencyBedCount, m.emergencyBedCount);
    dbMatch("Emergency Bed Notes", db.emergencyBedNotes, m.emergencyBedNotes);

    dbMatchBed("Pediatrics Bed Count", db.pediatricsBedCount, m.pediatricsBedCount);
    dbMatch("Pediatrics Bed Notes", db.pediatricsBedNotes, m.pediatricsBedNotes);

    dbMatchBed("Medical/Surgical Bed Count", db.medicalBedCount, m.medicalBedCount);
    dbMatch("Medical/Surgical Bed Notes", db.medicalBedNotes, m.medicalBedNotes);

    dbMatchBed("Psychiatry Bed Count Bed Count", db.psychiatryBedCount, m.psychiatryBedCount);
    dbMatch("Psychiatry Bed Notes", db.psychiatryBedNotes, m.psychiatryBedNotes);

    dbMatchBed("Burn Bed Count", db.burnBedCount, m.burnBedCount);
    dbMatch("Burn Bed Notes", db.burnBedNotes, m.burnBedNotes);

    dbMatchBed("Critical Care Bed Count", db.criticalBedCount, m.criticalBedCount);
    dbMatch("Critical Care  Bed Notes", db.criticalBedNotes, m.criticalBedNotes);

    dbMatch("Other 1 Bed Name", db.other1Name, m.other1Name);
    dbMatchBed("Other 1 Bed Count", db.other1BedCount, m.other1BedCount);
    dbMatch("Other 1 Bed Notes", db.other1BedNotes, m.other1BedNotes);

    dbMatch("Other 2 Bed Name", db.other2Name, m.other2Name);
    dbMatchBed("Other 2 Bed Count", db.other2BedCount, m.other2BedCount);
    dbMatch("Other 2 Bed Notes", db.other2BedNotes, m.other2BedNotes);

    dbMatch("Total Bed Count", db.totalBedCount, m.totalBedCount);

    dbMatch("Comments", db.additionalComments, m.additionalComments);

    var isPerfect = isPerfectMessage(m);
    var notMissing = new MissingRecord(m.city, MessageType.HOSPITAL_BED);
    missingHBSet.remove(notMissing);
    if (isPerfect) {
      missingPerfectHBSet.remove(notMissing);
    }
  }

  private void dbMatch(String label, String dbValue, String mValue) {
    var predicate = dbValue.equals(mValue);
    dbValue = isNull(dbValue) ? "(blank)" : dbValue;
    mValue = isNull(mValue) ? "(blank)" : mValue;
    var reason = mValue + ", but spreadsheet value of: " + dbValue;
    count(sts.test(sms(label), predicate, reason));
  }

  private void dbMatchIgnoreCase(String label, String dbValue, String mValue) {
    var predicate = dbValue.equalsIgnoreCase(mValue);
    dbValue = isNull(dbValue) ? "(blank)" : dbValue;
    mValue = isNull(mValue) ? "(blank)" : mValue;
    var reason = mValue + ", but spreadsheet value of: " + dbValue;
    count(sts.test(sms(label), predicate, reason));
  }

  private void dbMatchLatLon(String label, int decimalPlaces, String dbValue, String mValue) {
    dbValue = decimalFormatMap.get(decimalPlaces).format(Double.parseDouble(dbValue));
    mValue = decimalFormatMap.get(decimalPlaces).format(Double.parseDouble(mValue));
    var predicate = dbValue.equals(mValue);
    var reason = mValue + ", but spreadsheet value of: " + dbValue;
    count(sts.test(sms(label), predicate, reason));
  }

  private void dbMatchBed(String label, String dbValue, String mValue) {
    var predicate = (dbValue.equals("0")) ? mValue.equals("0") || isNull(mValue) : dbValue.equals(mValue);
    dbValue = isNull(dbValue) ? "(blank)" : dbValue;
    mValue = isNull(mValue) ? "(blank)" : mValue;
    var reason = mValue + ", but spreadsheet value of: " + dbValue;
    count(sts.test(sms(label), predicate, reason));
  }

  private void dbMatchDateTime(String label, LocalDateTime dbValue, LocalDateTime mValue) {
    var predicate = dbValue
        .truncatedTo(ChronoUnit.MINUTES)
          .toString()
          .equals(mValue.truncatedTo(ChronoUnit.MINUTES).toString());
    var reason = mValue + ", but spreadsheet value of: " + dbValue;
    count(sts.test(sms(label), predicate, reason));
  }

  private String sms(String label) {
    return label + " should match spreadsheet";
  }

  private void handle_WxLocalMessage(Summary summary, WxLocalMessage m) {
    sts.setExplanationPrefix("(wx) ");
    summary.wxLocalMessage = m;

    var db = cityWxMap.get(m.city.toUpperCase());
    if (db == null) {
      logger.warn("### NO WX in db for city: " + m.city + " from call: " + m.from);
      sts.getExplanations().add("no WX spreadsheet entry for city: " + m.city);
      return;
    }
    summary.db_wxLocalMessage = db;
    dbMatch("Message Sender", db.from, m.from);

    count(sts.test("Organization should be #EV", db.organization, m.organization));
    dbMatchDateTime("Form Date/Time", db.formDateTime, m.formDateTime);

    dbMatch("Location", db.locationString, m.locationString);
    dbMatch("City", db.city, m.city);
    dbMatch("State", db.state, m.state);
    dbMatch("County", db.county, m.county);
    dbMatchLatLon("Form Latitude", 3, db.formLocation.getLatitude(), m.formLocation.getLatitude());
    dbMatchLatLon("Form Longitude", 3, db.formLocation.getLongitude(), m.formLocation.getLongitude());
    dbMatch("Temperature", db.temperature, m.temperature);
    dbMatch("Windspeed", db.windspeed, m.windspeed);
    dbMatch("Max Gusts", db.maxGusts, m.maxGusts);
    dbMatchIgnoreCase("Warning Type", db.warningType, m.warningType);
    dbMatchIgnoreCase("Warning Field", db.warningField, m.warningField);
    dbMatch("Comments", db.comments, m.comments);

    isPerfectMessage(m);
  }

  private void handle_FsrMessage(Summary summary, FieldSituationMessage m) {
    sts.setExplanationPrefix("(fsr) ");
    summary.fsrMessage = m;

    var db = cityFsrMap.get(m.city.toUpperCase());
    if (db == null) {
      logger.warn("### NO FSR in db for city: " + m.city + " from call: " + m.from);
      sts.getExplanations().add("no FSR spreadsheet entry for city: " + m.city);
      return;
    }
    summary.db_fsrMessage = db;
    dbMatch("Message Sender", db.from, m.from);

    count(sts.test("Organization should be #EV", db.organization, m.organization));
    dbMatch("Precedence", db.precedence, m.precedence);
    dbMatch("Form Date/Time", db.formDateTime, m.formDateTime);
    dbMatch("Task", db.task, m.task);
    dbMatch("Form From", db.formFrom, m.formFrom);
    dbMatch("Form To", db.formTo, m.formTo);
    dbMatch("City", db.city, m.city);
    dbMatch("County", db.county, m.county);
    dbMatch("State", db.state, m.state);
    dbMatch("Territory", db.territory, m.territory);
    dbMatchLatLon("Form Latitude", 3, db.formLocation.getLatitude(), m.formLocation.getLatitude());
    dbMatchLatLon("Form Longitude", 3, db.formLocation.getLongitude(), m.formLocation.getLongitude());

    dbMatch("POTS landlines functioning", db.landlineStatus, m.landlineStatus);
    dbMatch("POTS landlines provider", db.landlineComments, m.landlineComments);

    dbMatch("VOIP landlines functioning", db.voipStatus, m.voipStatus);
    dbMatch("VOIP landlines provider", db.voipComments, m.voipComments);

    dbMatch("Cell phone voice functioning", db.cellPhoneStatus, m.cellPhoneStatus);
    dbMatch("Cell phone voice provider", db.cellPhoneComments, m.cellPhoneComments);

    dbMatch("Cell phone text functioning", db.cellTextStatus, m.cellTextStatus);
    dbMatch("Cell phone text provider", db.cellTextComments, m.cellTextComments);

    dbMatch("AM/FM Broadcast stations functioning", db.radioStatus, m.radioStatus);
    dbMatch("AM/FM Broadcast stations off the air", db.radioComments, m.radioComments);

    dbMatch("OTA TV functioning", db.tvStatus, m.tvStatus);
    dbMatch("OTV TV stations off the air", db.tvComments, m.tvComments);

    dbMatch("Satellite TV functioning", db.satTvStatus, m.satTvStatus);
    dbMatch("Satellite TV provider", db.satTvComments, m.satTvComments);

    dbMatch("Cable TV functioning", db.cableTvStatus, m.cableTvStatus);
    dbMatch("Cable TV provider", db.cableTvComments, m.cableTvComments);

    dbMatch("Public Water Works functioning", db.waterStatus, m.waterStatus);
    dbMatch("Public Water Works provider", db.waterComments, m.waterComments);

    dbMatch("Commerical Power functioning", db.powerStatus, m.powerStatus);
    dbMatch("Commerical Power provider", db.powerComments, m.powerComments);

    dbMatch("Commercial Power stable", db.powerStableStatus, m.powerStableStatus);
    dbMatch("unstable Commerical Power provider", db.powerStableComments, m.powerStableComments);

    dbMatch("Internet functioning", db.internetStatus, m.internetStatus);
    dbMatch("Internet provider", db.internetComments, m.internetComments);

    dbMatch("Natural Gas Supply functioning", db.naturalGasStatus, m.naturalGasStatus);
    dbMatch("Natural Gas provider", db.naturalGasComments, m.naturalGasComments);

    dbMatch("NOAA weather radio functioning", db.noaaStatus, m.noaaStatus);
    dbMatch("NOAA weather radio by frequency, etc", db.noaaComments, m.noaaComments);

    dbMatch("NOAA weather radio audio degraded", db.noaaAudioDegraded, m.noaaAudioDegraded);
    dbMatch("degraded NOAA weather radio by frequency", db.noaaAudioDegradedComments, m.noaaAudioDegradedComments);

    dbMatch("Comments", db.additionalComments, m.additionalComments);
    dbMatch("POC", db.poc, m.poc);

    isPerfectMessage(m);
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("(summary) ");
    var summary = (Summary) summaryMap.get(sender); // #MM

    sts.testNotNull("Hospital Bed message not received", summary.hospitalBedMessage);
    sts.testNotNull("Local WX message not received", summary.wxLocalMessage);
    sts.testNotNull("FSR message not received", summary.fsrMessage);

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    var deleteList = new ArrayList<String>();
    for (var sender : summaryMap.keySet()) {
      var summary = (Summary) summaryMap.get(sender);
      if (summary.wxLocalMessage == null && summary.hospitalBedMessage == null && summary.fsrMessage == null) {
        deleteList.add(sender);
      }
    }

    logger.info("no exported messages received from: " + String.join(",", deleteList));
    for (var sender : deleteList) {
      summaryMap.remove(sender);
    }

    writeTable("perfectMessages.csv", perfectMessages);
    super.postProcess();// #MM

    logger.info("Missing Hospital Beds from: " + String.join(",", missingHBSet.stream().map(m -> m.city).toList()));
    logger
        .info("Missing perfect Hospital Beds from: "
            + String.join(",", missingPerfectHBSet.stream().map(m -> m.city).toList()));

  }

}
