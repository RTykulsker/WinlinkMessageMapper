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

package com.surftools.wimp.processors.named;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.MiroCheckInMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.ReadProcessor;

/**
 * compute documented resilience, based on {@link com.surftools.wimp.message.MiroCheckinMessage}
 *
 *
 * @author bobt
 *
 */
public class MiroProcessor extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(MiroProcessor.class);

  private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private static final boolean FLAG_INCLUDE_ANTENNA_IN_RESILIENCY = false;
  private static final boolean FLAG_INCLUDE_RMS_GATEWAY_IN_RESILIENCY = false;
  private static final boolean FLAG_INCLUDE_RF_POWER_IN_RESILIENCY = false;

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

      if (mode.equalsIgnoreCase("Telnet")) {
        final var na = "n/a";
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
      this.dateTime = LocalDateTime.parse(formDate + " " + formTime, DT_FORMATTER);

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
          "IsResilient?", "Explanation", "Exercise Count", "Resiliency Score", "Resiliency Percent" };
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

    static {
      powerMap = Map.of("B", "Battery", "G", "Generator", "M", "Municipal");
      bandMap = Map.of("H", "HF", "T", "Telnet", "U", "UHF", "V", "VHF");
      modeMap = Map
          .of("A", "Ardop HF", "P", "Packet FM", "PT", "Pactor HF", "T", "Telnet", "VF", "Vara FM", "VH", "Vara HF");
      radioMap = Map.of("B", "Base", "H", "Handheld", "M", "Mobile");
      antennaMap = Map
          .of("B", "Beam", "D", "Dipole", "E", "Endfed", "H", "Handheld", "N", "NVIS", "O", "Other", "V", "Vertical");
      rfPowerMap = Map
          .of("P0", "n/a", "P1", "very low (0-5 W)", "P2", "low (5-30 W)", "P3", "medium (30-50 W)", "high",
              "(50-100 W)", "P4", "(100W to legal limit)");
    }

    private static final Map<String, String> powerMap;
    private static final Map<String, String> bandMap;
    private static final Map<String, String> modeMap;
    private static final Map<String, String> radioMap;
    private static final Map<String, String> antennaMap;
    private static final Map<String, String> rfPowerMap;

  }

  private Map<String, List<GradedMiroMessage>> oldMessages;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    oldMessages = makeOldMessageMap();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    var results = new ArrayList<IWritableTable>();
    var scoreCounter = new Counter();
    var ppCount = 0;
    var ppResilienceCount = 0;

    var messages = mm.getMessagesForType(MessageType.MIRO_CHECK_IN);
    if (messages != null) {
      for (var message : messages) {
        MiroCheckInMessage m = (MiroCheckInMessage) message;
        ++ppCount;

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

        newGradedMessage.isResilient = isResilient;
        newGradedMessage.explanation = explanation;
        newGradedMessage.resiliencyCount = newResilientCount;
        newGradedMessage.exerciseCount = newExerciseCount;
        newGradedMessage.resiliencyPercent = resiliencyPercent;
        list.add(newGradedMessage);
        Collections.sort(list);
        oldMessages.put(from, list);
        results.add(newGradedMessage);
      }
    }

    var sb = new StringBuilder();
    sb.append("\nMiro Check In messages: " + ppCount + "\n");
    sb.append(formatPP("Messages with resilient channel", ppResilienceCount, ppCount));

    sb.append("\nResiliency: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "resiliency", "count"));
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

    var path = Path.of(cm.getAsString(Key.DATABASE_PATH), "cumulative-miro_check_in.csv");
    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(path);
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
