/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

package com.surftools.wimp.processors.exercise.miro;

import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.MultiDateTimeParser;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.MiroCheckInMessage;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.service.rmsGateway.RmsGatewayService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * compute documented resilience, based on {@link com.surftools.wimp.message.MiroCheckinMessage}
 *
 *
 * @author bobt
 *
 */
public class MIRO_Check_In extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(MIRO_Check_In.class);

  private static final MultiDateTimeParser parser = new MultiDateTimeParser(
      List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm 'UTC'"));

  private static final boolean FLAG_INCLUDE_ANTENNA_IN_RESILIENCY = false;
  private static final boolean FLAG_INCLUDE_RMS_GATEWAY_IN_RESILIENCY = false;
  private static final boolean FLAG_INCLUDE_RF_POWER_IN_RESILIENCY = false;

  static {
    powerMap = Map.of("B", "Battery", "G", "Generator", "M", "Municipal");
    bandMap = Map.of("H", "HF", "T", "Telnet", "U", "UHF", "V", "VHF");
    modeMap = Map
        .of("A", "Ardop HF", "P", "Packet FM", "PT", "Pactor HF", "T", "Telnet", "VF", "Vara FM", "VH", "Vara HF");
    radioMap = Map.of("B", "Base", "H", "Handheld", "M", "Mobile");
    antennaMap = Map
        .of("B", "Beam", "D", "Dipole", "E", "Endfed", "H", "Handheld", "N", "NVIS", "O", "Other", "V", "Vertical");
    rfPowerMap = Map
        .of("P0", "n/a", "P1", "very low (0-5 W)", "P2", "low (5-30 W)", "P3", "medium (30-50 W)", "P4",
            "high (50-100 W)", "P5", "very high (100W to legal limit)");
  }

  private static final Map<String, String> powerMap;
  private static final Map<String, String> bandMap;
  private static final Map<String, String> modeMap;
  private static final Map<String, String> radioMap;
  private static final Map<String, String> antennaMap;
  private static final Map<String, String> rfPowerMap;

  public static class GradedMiroMessage implements IWritableTable {
    public String messageId;
    public String from;
    public LocalDateTime dateTime;
    public LatLongPair location;
    public String power;
    public String band;
    public String mode;
    public String radio;
    public String antenna;
    public String isPortable;
    public String rfPower;
    public String rmsGateway;
    public String distanceMiles;
    public String comments;
    public String version;
    public String isResilient;
    public String explanation;
    public int resiliencyCount;
    public int exerciseCount;
    public int resiliencyPercent;

    public GradedMiroMessage(MiroCheckInMessage m) {
      this.messageId = m.messageId;
      this.from = m.from;
      this.dateTime = m.sortDateTime;
      this.location = m.mapLocation;

      this.power = powerMap.getOrDefault(m.power, m.power);
      this.band = bandMap.getOrDefault(m.band, m.band);
      this.mode = modeMap.getOrDefault(m.mode, m.mode);
      this.radio = radioMap.getOrDefault(m.radio, m.radio);
      this.antenna = antennaMap.getOrDefault(m.antenna, m.antenna);
      this.isPortable = m.portable;
      this.rfPower = rfPowerMap.getOrDefault(m.rfPower, m.rfPower);
      this.rmsGateway = m.rmsGateway;
      this.distanceMiles = m.distanceMiles;
      this.comments = m.comments;
      this.version = m.version;

      if (mode.equalsIgnoreCase("Telnet") || band.equalsIgnoreCase("Telnet")) {
        final var na = "n/a";
        mode = "Telnet";
        band = na;
        radio = na;
        antenna = na;
        rfPower = na;
        rmsGateway = na;
        distanceMiles = na;
      }
    }

    public GradedMiroMessage(String[] fields) {
      this.messageId = fields[0];
      this.from = fields[1];

      var formDate = fields[2];
      var formTime = fields[3];
      this.dateTime = parser.parse(formDate + " " + formTime);

      var formLatitude = fields[4];
      var formLongitude = fields[5];
      this.location = new LatLongPair(formLatitude, formLongitude);

      this.power = fields[6];
      this.band = fields[7];
      this.mode = fields[8];
      this.radio = fields[9];
      this.isPortable = fields[10];
      this.antenna = fields[11];
      this.rfPower = fields[12];
      this.rmsGateway = fields[13];
      this.distanceMiles = fields[14];
      this.comments = fields[15];
      this.version = fields[16];
      this.isResilient = fields[17];
      this.explanation = fields[18];
      this.resiliencyCount = Integer.valueOf(fields[19]);
      this.exerciseCount = Integer.valueOf(fields[20]);
      this.resiliencyPercent = Integer.valueOf(fields[21]);
    }

    public boolean isResilientFrom(GradedMiroMessage other) {
      int cmp = power.compareTo(other.power);

      if (cmp == 0) {
        cmp = band.compareTo(other.band);
      }

      if (cmp == 0) {
        cmp = mode.compareTo(other.mode);
      }

      if (cmp == 0) {
        cmp = radio.compareTo(other.radio);
      }

      if (cmp == 0) {
        if (FLAG_INCLUDE_ANTENNA_IN_RESILIENCY) {
          cmp = antenna.compareTo(other.antenna);
        }
      }

      if (cmp == 0) {
        cmp = isPortable.compareTo(other.isPortable);
      }

      if (cmp == 0) {
        if (FLAG_INCLUDE_RF_POWER_IN_RESILIENCY) {
          cmp = rfPower.compareTo(other.rfPower);
        }
      }

      if (cmp == 0) {
        if (FLAG_INCLUDE_RMS_GATEWAY_IN_RESILIENCY) {
          cmp = rmsGateway.compareTo(other.rmsGateway);
        }
      }

      return (cmp != 0);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { //
          "MessageId", "From", "Date", "Time", "Latitude", "Longitude", //
          "POWER", "BAND", "MODE", "RADIO", "PORTABLE", //
          "Antenna", "RF Power", "RMS Gateway", "Distance (miles)", //
          "Comments", "Version", //
          "IsResilient?", "Explanation", "Resiliency Count", "Exercise Count", "Resiliency Percent" };
    }

    @Override
    public String[] getValues() {
      var date = dateTime == null ? "" : dateTime.toLocalDate().toString();
      var time = dateTime == null ? "" : dateTime.toLocalTime().toString();
      var lat = location == null ? "" : location.getLatitude();
      var lon = location == null ? "" : location.getLongitude();
      return new String[] { messageId, from, date, time, lat, lon, //
          power, band, mode, radio, isPortable, //
          antenna, rfPower, rmsGateway, distanceMiles, //
          comments, version, //
          isResilient, explanation, String.valueOf(resiliencyCount), String.valueOf(exerciseCount),
          String.valueOf(resiliencyPercent) };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (GradedMiroMessage) other;
      int cmp = from.compareTo(o.from);
      if (cmp != 0) {
        return cmp;
      }
      return dateTime.compareTo(o.dateTime);
    }

  }

  private Map<String, List<GradedMiroMessage>> oldMessages;
  private List<IWritableTable> results = new ArrayList<IWritableTable>();
  private RmsGatewayService rmsGatewayService;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    messageType = MessageType.MIRO_CHECK_IN;

    oldMessages = makeOldMessageMap();
    rmsGatewayService = new RmsGatewayService(cm);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    if (message.getMessageType() != MessageType.MIRO_CHECK_IN) {
      return;
    }

    MiroCheckInMessage m = (MiroCheckInMessage) message;
    ++ppCount;

    getCounter("versions").increment(m.version);

    getCounter("power").increment(powerMap.getOrDefault(m.power, m.power));
    getCounter("band").increment(bandMap.getOrDefault(m.band, m.band));
    getCounter("mode").increment(modeMap.getOrDefault(m.mode, m.mode));
    getCounter("radio").increment(radioMap.getOrDefault(m.radio, m.radio));
    getCounter("antenna").increment(antennaMap.getOrDefault(m.antenna, m.antenna));
    getCounter("portable").increment(m.portable);
    getCounter("rfPower").increment(rfPowerMap.getOrDefault(m.rfPower, m.rfPower));
    getCounter("rmsGateway").increment(m.rmsGateway);
    getCounter("distanceMiles").increment(m.distanceMiles);

    var newGradedMessage = new GradedMiroMessage(m);

    var from = message.from;
    var list = oldMessages.getOrDefault(from, new ArrayList<GradedMiroMessage>());
    var oldExerciseCount = list.size();
    long oldResilienceCount = list.stream().filter(msg -> msg.isResilient.equals("Yes")).count();

    var explanations = new ArrayList<String>();
    for (var old : list) {
      if (!newGradedMessage.isResilientFrom(old)) {
        explanations.add(old.messageId);
      }
    }

    var isResilient = "Yes";
    var explanation = "";
    if (explanations.size() != 0) {
      explanation = "same path as messageId(s): " + String.join(",", explanations);
      isResilient = "No";
    }

    var newExerciseCount = 1 + oldExerciseCount;
    var newResilientCount = (int) ((isResilient.equals("Yes") ? 1 : 0) + oldResilienceCount);
    var resiliencyPercent = (int) Math.round(100d * (newResilientCount) / newExerciseCount);

    getCounter("isResilient").increment(isResilient);

    newGradedMessage.isResilient = isResilient;
    newGradedMessage.explanation = explanation;
    newGradedMessage.resiliencyCount = newResilientCount;
    newGradedMessage.exerciseCount = newExerciseCount;
    newGradedMessage.resiliencyPercent = resiliencyPercent;
    list.add(newGradedMessage);
    Collections.sort(list);
    oldMessages.put(from, list);
    results.add(newGradedMessage);

    if (doOutboundMessaging) {
      var sb = new StringBuilder();
      sb.append("\n\n");
      sb.append("Thank you for participating in our monthly MIRO Winlink Wednesday exercise ");
      sb.append("and for submitting a MIRO Check In v2.0.0 message.\n\n");

      if (isResilient.equalsIgnoreCase("YES")) {
        sb.append("This month you used a 'resilient path' to send your messages. Well done!\n");
      } else {
        sb.append("This month you didn't use a 'resilient path'. Oh well, try again next month!\n");
      }
      sb.append("\n");
      sb.append("For 2024, you participated in " + newExerciseCount + " exercise");
      sb.append((newExerciseCount == 1 ? "" : "s") + ", ");
      sb.append("with a resilient count of " + newResilientCount);
      sb.append(", for a resiliency 'score' of " + resiliencyPercent + "%\n\n");

      var totalMessageContent = getTotalMessageContent(from, mm.getMessagesForSender(from));
      sb.append(totalMessageContent);
      var feedback = sb.toString();
      outboundMessageExtraContent = feedback;
    } // end doOutboundMessaging

  }

  private final DecimalFormat df = new DecimalFormat("#.000###");

  private String getTotalMessageContent(String call, Map<MessageType, List<ExportedMessage>> messagesForSender) {
    var sb = new StringBuilder();
    sb.append("\n");

    var totalMessages = 0;
    for (var type : messagesForSender.keySet()) {
      var list = messagesForSender.get(type);
      for (var m : list) {
        ++totalMessages;
        /**
         * public record RmsGatewayResult(String sender, String messageId, boolean isFound, LatLongPair location, String
         * gatewayCallsign, int frequency) {
         *
         */
        var serviceResult = rmsGatewayService.getLocationOfRmsGateway(call, m.messageId);
        if (serviceResult.isFound()) {
          sb
              .append(
                  "Your " + type + " message, mId(" + m.messageId + "), sent via: " + serviceResult.gatewayCallsign());
          if (!serviceResult.gatewayCallsign().equalsIgnoreCase("TELNET")) {
            if (serviceResult.isFound() && serviceResult.location() != null && serviceResult.location().isValid()) {
              var distanceMiles = LocationUtils.computeDistanceMiles(m.mapLocation, serviceResult.location());
              var bearing = LocationUtils.computBearing(m.mapLocation, serviceResult.location());
              sb.append(", distance: " + distanceMiles + " miles, bearing: " + bearing);
            }
            if (serviceResult.frequency() > 0) {
              var freqString = df.format(serviceResult.frequency() / 1_000_000d);
              sb.append(", freq: " + freqString + " MHz");
            }
          }
          sb.append("\n");
        } else {
          sb.append("Your " + type + " message, mId(" + m.messageId + ") not found in CMS database\n");
        } // end if serviceResult not found
      } // end loop over messages for type
    } // end loop over types;

    sb.append("\n" + "You sent " + totalMessages + " total messages" + "\n");
    var s = sb.toString();
    return s;
  }

  @Override
  public void postProcess() {
    super.postProcess();

    Collections.sort(results);
    writeTable("exercise-miro_check_in.csv", results);

    var allGradedMessages = new ArrayList<IWritableTable>();
    for (var list : oldMessages.values()) {
      allGradedMessages.addAll(list);
    }
    Collections.sort(allGradedMessages);
    var writables = new ArrayList<IWritableTable>();
    writables.addAll(allGradedMessages);
    writeTable("cumulative-miro_check_in.csv", writables);
  }

  private Map<String, List<GradedMiroMessage>> makeOldMessageMap() {
    var tmpMap = new HashMap<String, List<GradedMiroMessage>>();

    var databasePath = cm.getAsString(Key.DATABASE_PATH);
    if (databasePath == null || databasePath.length() == 0) {
      return tmpMap;
    }

    var path = Path.of(cm.getAsString(Key.DATABASE_PATH), "cumulative-miro_check_in.csv");
    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(path, ',', false, 1);
    for (var fields : fieldsArray) {
      var message = new GradedMiroMessage(fields);
      var sender = message.from;
      var list = tmpMap.getOrDefault(sender, new ArrayList<GradedMiroMessage>());
      list.add(message);
      tmpMap.put(sender, list);
    }
    return tmpMap;
  }

}
