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

package com.surftools.wimp.processors.exercise.eto_2022;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.processors.std.ClassifierProcessor;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseP2PProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor to support ETO 2022-08-20 Exercise:
 *
 * 1: two days before exercise, Thursday, 2022-08-18, a "conventional RMS exercise is run; each station that wants to
 * participate sends a Winlink Check In to the Target Station. Multiple messages allowed, last one wins.
 *
 * Field stations can "reserve" zero or more Target stations. Fields will be evaluated based on total number of P2P
 * connections, ratio of connections/reservations and a "success ratio" of ratio * number of P2P connections. Only one
 * connection between Field and Target counts, regardless of frequency
 *
 * 2: one day before exercise, Friday, 2022-08-19, each Target will export the received check-in messages as their
 * "BEGIN" messages and send to me
 *
 * 3: Targets generate "acknowledge" messages, and changes type to P2P
 *
 * 4: on day of exercise, Saturday, 2022-08-20, at end of exercise, targets export their remaining, unsent, P2P
 * acknowledgments sitting in their outbox and export as the "END" messages and send to me
 *
 * Theory of operation:
 *
 * 1: Use the check-in.csv file produced by normal WIMP processing ONLY for Field Station location
 *
 * 2: Use target.csv file ONLY for Target Station location
 *
 * 3: Use BEGIN messages from Targets to build list of reservations, both for Field Station as well as Target
 *
 * 4: Use END messages from Target to compute SET DIFFERENCE and hence successful completion
 *
 * 5: Produce three output files:
 *
 * Output 1: kml file with Field and Target nodes and link to show completions
 *
 * Output 2: fields.csv; with call, lat, long, reservations, completions, counts and ratios
 *
 * Output 3: targets.csv; with call, lat, long, reservations, completions, counts and ratios
 *
 *
 * http://kml4earth.appspot.com/icons.html
 *
 * @author bobt
 *
 */
public class ETO_2022_08_18_P2P extends AbstractBaseP2PProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_08_18_P2P.class);

  private static class Target extends BaseTarget implements IWritableTable {
    @SuppressWarnings("unused")
    public String channel;
    @SuppressWarnings("unused")
    public String region;
    public String locationName;

    public Set<String> reservedCalls;
    public Set<String> completedCalls;
    public double completionPercent;
    public int completionScore;

    public Target(String[] fields) {
      this.isActive = Boolean.valueOf(fields[0]);
      this.channel = fields[1];
      this.band = fields[2];
      this.centerFreq = fields[3];
      this.dialFreq = fields[4];
      this.region = fields[5];
      this.call = fields[6];
      this.locationName = fields[7];
      this.location = new LatLongPair(fields[8], fields[9]);

      fromList = new ArrayList<>();
      reservedCalls = new HashSet<>();
      completedCalls = new HashSet<>();
    }

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "call", "location", "latitude", "longitude", //
          "completedPercent", "completedScore", //
          "reservedCount", "completedCount", //
          "reserved", "completed" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, locationName, location.getLatitude(), location.getLongitude(), //
          String.format("%.2f", 100d * completionPercent) + "%", String.valueOf(completionScore), //
          String.valueOf(reservedCalls.size()), String.valueOf(completedCalls.size()), //
          String.join(",", reservedCalls), String.join(",", completedCalls) };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Target) other;
      return call.compareTo(o.call);
    }
  }

  private static class Field extends BaseField implements IWritableTable {
    public Set<String> reservedCalls;
    public Set<String> completedCalls;
    public double completionPercent;
    public int completionScore;

    public Field(String[] fields) {
      call = fields[1];
      location = new LatLongPair(fields[6], fields[7]);

      toList = new ArrayList<>();
      reservedCalls = new HashSet<>();
      completedCalls = new HashSet<>();
    }

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "call", "latitude", "longitude", //
          "completedPercent", "completedScore", //
          "reservedCount", "completedCount", //
          "reserved", "completed" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, location.getLatitude(), location.getLongitude(), //
          String.format("%.2f", 100d * completionPercent) + "%", String.valueOf(completionScore), //
          String.valueOf(reservedCalls.size()), String.valueOf(completedCalls.size()), //
          String.join(",", reservedCalls), String.join(",", completedCalls) };

    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Field) other;
      return call.compareTo(o.call);
    }
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    p2p_initialize();
  }

  @Override
  public void process() {
    p2p_process();

    targetMap = makeTargetMap();
    fieldMap = makeFieldMap();

    var beginMap = getMapFromExportedMessages(cm.getAsString(Key.P2P_BEGIN_PATH), MessageType.CHECK_IN);
    var endMap = getMapFromExportedMessages(cm.getAsString(Key.P2P_END_PATH), MessageType.ACK);

    var completedMap = makeComplete(beginMap, endMap);
    updateTargetStations(targetMap, beginMap, completedMap);
    updateFieldStations(fieldMap, beginMap, completedMap);

    writeFields(fieldMap);
    writeTargets(targetMap);
    //
    // var kmlManager = new KmlManager(cm);
    // kmlManager.run(fieldMap, targetMap);
    //
    // missingDataReport(targetMap, beginMap, endMap);

    summarize();
  }

  /**
   * read Exported Message files in path, return a map(target -> set of message call signs
   *
   * @param pathName
   * @param expectedMessageType
   * @return
   */
  public Map<String, Set<String>> getMapFromExportedMessages(String pathName, MessageType expectedMessageType) {
    var map = new HashMap<String, Set<String>>();
    var path = Path.of(pathName);

    // read all Exported Messages from files
    var files = path.toFile().listFiles();
    if (files == null) {
      return map;
    }

    var readProcessor = new ReadProcessor();
    readProcessor.initialize(cm, mm);

    var classifierProcessor = new ClassifierProcessor();
    classifierProcessor.initialize(cm, mm);

    // var reader = new ReadProcessor();
    // reader.initialize(cm, mm);
    for (File file : path.toFile().listFiles()) {
      if (file.isFile()) {
        if (!file.getName().toLowerCase().endsWith(".xml")) {
          continue;
        }

        var targetCall = getTargetCallFromFileName(file.getName());
        if (targetCall == null) {
          logger.error("could not find target name from file name: " + file.getName());
          continue;
        }
        var fileExportedMessages = readProcessor.readAll(file.toPath());
        var set = map.getOrDefault(targetCall, new TreeSet<String>());
        for (var message : fileExportedMessages) {

          // begin vs end? let's autosense it!
          var fieldCall = message.from;
          if (fieldCall.equalsIgnoreCase(targetCall)) {
            fieldCall = message.to;
          }
          // set.add(fieldCall);

          // check if right message type
          var messageTypeName = "";
          boolean fireWrongType = false;
          if (expectedMessageType != null) {
            var messageType = classifierProcessor.findMessageType(message);
            if (messageType != expectedMessageType) {
              fireWrongType = true;
              messageTypeName = messageType.toString();
            }
          }

          // check if field in fieldMap
          boolean fireFieldNotInMap = false;

          /*
           * In a perfect world, all the normal ETO clearinghouses will report promptly and the fieldMap contains all
           * potential Field stations.
           *
           * In reality, some clearinghouses take days to report, so that the fieldMap is not complete until then and
           * hence, can't really be used to warn about targets having non-P2P messages to Field stations in their out
           * boxes
           */
          // var requireFieldCallInFieldMap = cm.getAsBoolean(Key.REQUIRE_FIELD_CALL_IN_FIELD_MAP);
          var requireFieldCallInFieldMap = true;
          if (fieldCall != null) {
            set.add(fieldCall);
            if (requireFieldCallInFieldMap) {
              var field = fieldMap.get(fieldCall);
              if (field == null) {
                fireFieldNotInMap = true;
              }
            }
          } // end fieldCall != null

          if (fireFieldNotInMap || fireWrongType) {
            var sb = new StringBuilder();
            sb.append("### to target: " + targetCall + " from field: " + fieldCall + ": ");
            if (fireFieldNotInMap) {
              sb.append(" field not in fieldMap, ");
            }

            if (fireWrongType) {
              sb.append(" wrong message type " + messageTypeName + ", ");
            }
            var s = sb.toString();
            s = s.substring(0, s.length() - 1);

            logger.warn(s);
          }

        } // end loop over messages
        map.put(targetCall, set);
      } // end file
    } // end file in directory

    logger.info("returning map with " + map.size() + " entries from directory:" + pathName);
    return map;
  }

  private Map<String, Set<String>> makeComplete(Map<String, Set<String>> beginMap, //
      Map<String, Set<String>> endMap) {
    if (beginMap.size() != endMap.size()) {
      logger.warn("begin/end map sizes differ! Begin size: " + beginMap.size() + ". End size: " + endMap.size() + ".");
    }

    var map = new HashMap<String, Set<String>>();

    for (var beginTargetCall : beginMap.keySet()) {

      var beginSet = beginMap.get(beginTargetCall);
      var endSet = endMap.get(beginTargetCall);
      if (endSet == null) {
        logger.warn("no end map found for begin target call: " + beginTargetCall);
      }

      Set<String> completedSet = new TreeSet<String>();
      if (endSet != null) {
        completedSet.addAll(beginSet);
        completedSet.removeAll(endSet);
      } else {
        endSet = new TreeSet<String>();
      }
      map.put(beginTargetCall, completedSet);

      var debugCompleted = true;
      if (debugCompleted) {
        var sb = new StringBuilder();
        sb.append("target call: " + beginTargetCall + "\n");
        sb.append("  beginSet:     " + String.join(",", beginSet) + "\n");
        sb.append("  endSet:       " + String.join(",", endSet) + "\n");
        sb.append("  completedSet: " + String.join(",", completedSet) + "\n");

        logger.info(sb.toString());
      }
    }

    return map;
  }

  private void updateTargetStations(Map<String, BaseTarget> targetMap, //
      Map<String, Set<String>> beginMap, //
      Map<String, Set<String>> completedMap) {

    for (var targetCall : targetMap.keySet()) {
      var target = (Target) targetMap.get(targetCall);

      var beginSet = beginMap.getOrDefault(targetCall, new TreeSet<String>());
      target.reservedCalls = beginSet;

      var completedSet = completedMap.getOrDefault(targetCall, new TreeSet<String>());
      target.completedCalls = completedSet;

      var reservedCount = target.reservedCalls.size();
      var completedCount = target.completedCalls.size();
      target.completionPercent = (reservedCount == 0) ? 0 : completedCount / (double) reservedCount;
      target.completionScore = (int) (100 * target.completionPercent * completedCount);

      targetMap.put(targetCall, target);
    }
  }

  private void updateFieldStations(Map<String, BaseField> fieldMap, //
      Map<String, Set<String>> beginMap, //
      Map<String, Set<String>> completedMap) {

    for (var targetCall : beginMap.keySet()) {
      var nullFieldCount = 0;
      var fieldSet = beginMap.get(targetCall);
      if (fieldSet == null) {
        continue;
      }
      for (var fieldCall : fieldSet) {
        var field = (Field) fieldMap.get(fieldCall);
        if (field != null) {
          field.reservedCalls.add(targetCall);
          field.toList.add(null);// needed to generate field placemarks
        } else {
          logger.debug("null field for call: " + fieldCall);
          ++nullFieldCount;
        }
      }
      logger.info("Target: " + targetCall + ", null field count: " + nullFieldCount);
    }

    for (var targetCall : completedMap.keySet()) {
      var fieldSet = completedMap.get(targetCall);
      if (fieldSet == null) {
        continue;
      }
      for (var fieldCall : fieldSet) {
        var field = (Field) fieldMap.get(fieldCall);
        if (field != null) {
          field.completedCalls.add(targetCall);
        }
      }
    }

    for (var fieldCall : fieldMap.keySet()) {
      var field = (Field) fieldMap.get(fieldCall);
      if (field != null) {
        var reservedCount = field.reservedCalls.size();
        var completedCount = field.completedCalls.size();
        field.completionPercent = (reservedCount == 0) ? 0 : completedCount / (double) reservedCount;
        field.completionScore = (int) (100 * field.completionPercent * completedCount);
      }
    }

  }

  public Map<String, BaseTarget> makeTargetMap() {
    var map = new TreeMap<String, BaseTarget>();
    var list = readTargets();
    for (var target : list) {
      var call = target.call;
      if (call == null || call.isBlank()) {
        continue;
      }
      map.put(target.call, target);
    }
    logger.info("created targetMap with " + map.size() + " target entries");

    return map;
  }

  public List<Target> readTargets() {
    var list = new ArrayList<Target>();
    String targetFilePathName = cm.getAsString(Key.P2P_TARGET_PATH);

    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(Path.of(cm.getAsString(Key.P2P_TARGET_PATH)));
    for (var fields : fieldsArray) {
      var target = new Target(fields);
      list.add(target);
    }

    logger.info("read " + list.size() + " target entries from " + targetFilePathName);
    return list;
  }

  public Map<String, BaseField> makeFieldMap() {
    var map = new HashMap<String, BaseField>();
    var list = readFields();
    for (var field : list) {
      var call = field.call;
      if (call == null || call.isBlank()) {
        continue;
      }
      map.put(field.call, field);
    }
    logger.info("created fieldMap with " + map.size() + " field entries");

    return map;
  }

  public List<Field> readFields() {
    var list = new ArrayList<Field>();
    String fieldFilePathName = cm.getAsString(Key.P2P_FIELD_PATH);

    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(Path.of(cm.getAsString(Key.P2P_FIELD_PATH)));
    for (var fields : fieldsArray) {
      var field = new Field(fields);
      list.add(field);
    }

    logger.info("read " + list.size() + " field entries from " + fieldFilePathName);
    return list;
  }

  /**
   * produce needed (and un-needed) summary output
   */
  public void summarize() {
    var kmlText = makeKmlText();
    writeKml(kmlText);

    var sb = new StringBuilder();
    sb.append("\n\nSummary\n");
    logger.info(sb.toString());
  }

  @Override
  protected BaseTarget makeTarget(String[] fields) {
    Target target = new Target(fields);

    return target;
  }

  @Override
  protected BaseField makeField(String[] fields) {
    Field field = new Field(fields);

    return field;
  }

  @Override
  protected String makeFieldDescription(BaseField baseField, Map<String, BaseTarget> targetMap) {
    var field = (Field) baseField;
    var sb = new StringBuilder();

    sb.append("Outbound reservations: " + field.reservedCalls.size() + "\n");
    sb.append("Outbound completions:  " + field.completedCalls.size() + "\n");
    sb.append("Completion percentage: " + String.format("%.2f", 100d * field.completionPercent) + "%\n");
    sb.append("Completion score:      " + field.completionScore + "\n");

    sb.append(DASHES + "\n");
    for (var targetCall : field.reservedCalls) {
      var target = (Target) targetMap.get(targetCall);
      var isCompleted = field.completedCalls.contains(targetCall);
      var distanceMiles = field.location.computeDistanceMiles(target.location);
      var band = target.band;
      sb
          .append(target.call + ", (" + band + "m, " + distanceMiles + " miles) " + (isCompleted ? "completed!" : "")
              + "\n");
    }

    var s = sb.toString();
    return s;
  }

  @Override
  protected String makeTargetDescription(BaseTarget baseTarget, Map<String, BaseField> fieldMap) {
    var target = (Target) baseTarget;
    var sb = new StringBuilder();

    sb.append(target.dialFreq + " KHz dial, " + target.location + "\n");
    sb.append(DASHES + "\n");
    sb.append("Inbound reservations: " + target.reservedCalls.size() + "\n");
    sb.append("Inbound completions: " + target.completedCalls.size() + "\n");
    sb.append(DASHES + "\n");

    var nullFieldCount = 0;
    for (var fieldCall : target.reservedCalls) {
      var field = (Field) fieldMap.get(fieldCall);
      if (field != null) {
        var isCompleted = target.completedCalls.contains(fieldCall);
        var distanceMiles = target.location.computeDistanceMiles(field.location);
        sb.append(fieldCall + " (" + distanceMiles + " miles) " + (isCompleted ? "completed!" : "") + "\n");
      } else {
        ++nullFieldCount;
        logger.debug("null field for call: " + fieldCall);
      }
    }
    logger.info("Target: " + target.call + ", null field count: " + nullFieldCount);

    var s = sb.toString();
    return s;
  }

  private String getTargetCallFromFileName(String fileName) {
    var candidates = new HashSet<String>();
    for (var call : targetMap.keySet()) {
      if (fileName.toUpperCase().contains(call)) {
        candidates.add(call);
      }
    }

    if (candidates.size() == 0) {
      logger.info("no candidate targets for file: " + fileName);
      return null;
    } else if (candidates.size() > 1) {
      logger.info(("multiple candidate targets for file: " + fileName + ", " + String.join(",", candidates)));
      return null;
    } else {
      return candidates.iterator().next();
    }
  }

  public void writeFields(Map<String, BaseField> fieldMap) {
    Path outputPath = Path.of(cm.getAsString(Key.PATH), "output", "updated-fields.csv");
    var list = new ArrayList<IWritableTable>();
    for (var field : fieldMap.values()) {
      list.add((Field) field);
    }
    WriteProcessor.writeTable(list, outputPath);
  }

  public void writeTargets(Map<String, BaseTarget> targetMap) {
    Path outputPath = Path.of(cm.getAsString(Key.PATH), "output", "updated-targets.csv");
    var list = new ArrayList<IWritableTable>();
    for (var target : targetMap.values()) {
      list.add((Target) target);
    }
    WriteProcessor.writeTable(list, outputPath);
  }

  @Override
  // must override cuz we don't got messages, only calls
  protected String makeNetworkPlacemarks() {
    var drawnSet = new HashSet<String>();
    var sb = new StringBuilder();
    for (var targetCall : targetMap.keySet()) {
      var target = (Target) targetMap.get(targetCall);
      var fromList = target.completedCalls;
      for (var fieldCall : fromList) {
        var field = (Field) fieldMap.get(fieldCall);
        if (field != null) {
          // add the link once, independent of number of messages from field to target
          var drawnKey = field.call + "-" + target.call;
          if (drawnSet.contains(drawnKey)) {
            continue;
          }
          drawnSet.add(drawnKey);
          sb.append("  <Placemark>\n");
          sb.append("  <name>" + fieldCall + "-" + targetCall + "</name>\n");
          sb.append("    <LineString>\n");
          sb
              .append("    <coordinates>" + field.location.getLongitude() + "," + field.location.getLatitude() //
                  + " " + target.location.getLongitude() + "," + target.location.getLatitude() + "</coordinates>\n");
          sb.append("    </LineString>\n");
          sb.append("  </Placemark>\n");
        }
      }
    }
    var s = sb.toString();
    return s;
  }
}
