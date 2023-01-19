/**

The MIT License (MIT)

Copyright (c) 2021, Robert Tykulsker

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

package com.surftools.wimp.p2p_pickup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.config.impl.PropertyFileConfigurationManager;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageType;

/**
 * App to support ETO 2022-08-20 Exercise:
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
public class P2PPickupTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(P2PPickupTool.class);

  @Option(name = "--configurationFile", usage = "path to configuration file", required = false)
  private String configurationFileName = "configuration.txt";

  private IConfigurationManager cm;

  // from input files
  private Map<String, Target> targetMap;
  private Map<String, Field> fieldMap;
  private Map<String, Set<String>> beginMap; // reservations, can be back-fed into fields
  private Map<String, Set<String>> endMap;

  // result of set-difference
  private Map<String, Set<String>> completedMap; // back-feed into both targetMap and fieldMap

  public static void main(String[] args) {
    P2PPickupTool app = new P2PPickupTool();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() {
    try {
      logger.info("begin");

      cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
      var dao = new Dao(cm);
      targetMap = dao.getTargetMap();
      fieldMap = dao.getFieldMap();
      beginMap = dao.getFieldSetsFromTargets(cm.getAsString(Key.P2P_BEGIN_PATH), MessageType.CHECK_IN, false);
      endMap = dao.getFieldSetsFromTargets(cm.getAsString(Key.P2P_END_PATH), MessageType.ACK, true);

      completedMap = makeComplete(beginMap, endMap);
      updateTargetStations(targetMap, beginMap, completedMap);
      updateFieldStations(fieldMap, beginMap, completedMap);

      dao.writeFields(fieldMap);
      dao.writeTargets(targetMap);

      var kmlManager = new KmlManager(cm);
      kmlManager.run(fieldMap, targetMap);

      missingDataReport(targetMap, beginMap, endMap);

      logger.info("exiting, begins: " + beginMap.size() + " targets, ends: " + endMap.size() + " targets.");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  private void updateTargetStations(Map<String, Target> targetMap, //
      Map<String, Set<String>> beginMap, //
      Map<String, Set<String>> completedMap) {

    for (var targetCall : targetMap.keySet()) {
      var target = targetMap.get(targetCall);

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

  private void updateFieldStations(Map<String, Field> fieldMap, Map<String, //
      Set<String>> beginMap, //
      Map<String, Set<String>> completedMap) {

    for (var targetCall : beginMap.keySet()) {
      var nullFieldCount = 0;
      var fieldSet = beginMap.get(targetCall);
      if (fieldSet == null) {
        continue;
      }
      for (var fieldCall : fieldSet) {
        var field = fieldMap.get(fieldCall);
        if (field != null) {
          field.reservedCalls.add(targetCall);
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
        var field = fieldMap.get(fieldCall);
        if (field != null) {
          field.completedCalls.add(targetCall);
        }
      }
    }

    for (var fieldCall : fieldMap.keySet()) {
      var field = fieldMap.get(fieldCall);
      if (field != null) {
        var reservedCount = field.reservedCalls.size();
        var completedCount = field.completedCalls.size();
        field.completionPercent = (reservedCount == 0) ? 0 : completedCount / (double) reservedCount;
        field.completionScore = (int) (100 * field.completionPercent * completedCount);
      }
    }

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

  private void missingDataReport(Map<String, Target> targetMap, Map<String, Set<String>> beginMap,
      Map<String, Set<String>> endMap) {
    // logger.info("begins: " + beginMap.size() + " targets, ends: " + endMap.size() + " targets.");

    if (beginMap.size() == targetMap.size()) {
      logger.info("begins complete, " + beginMap.size() + " targets!");
    } else {
      Set<String> tempSet = new TreeSet<String>(targetMap.keySet());
      tempSet.removeAll(beginMap.keySet());
      logger.warn("begins missing " + tempSet.size() + " targets: " + String.join(",", tempSet));
    }

    if (endMap.size() == targetMap.size()) {
      logger.info("ends complete, " + endMap.size() + " targets!");
    } else {
      Set<String> tempSet = new TreeSet<String>(targetMap.keySet());
      tempSet.removeAll(endMap.keySet());
      logger.warn("ends missing " + tempSet.size() + " targets: " + String.join(",", tempSet));
    }

  }
}
