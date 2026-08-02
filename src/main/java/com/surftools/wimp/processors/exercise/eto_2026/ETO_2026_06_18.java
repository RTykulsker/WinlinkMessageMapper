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

package com.surftools.wimp.processors.exercise.eto_2026;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.RenewableBag;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.service.map.IMapService;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-213, Field Day warm-up
 *
 * @author bobt
 *
 */
public class ETO_2026_06_18 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_06_18.class);

  private record FieldDayEntry(Ics213Message m, String clubName, Integer participantCount, String fdLocation,
      Integer aresCount) {

  };

  private Map<String, FieldDayEntry> fdMap = new HashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213Message) message;
    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("THIS IS AN EXERCISE should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", "ARRL Field Day 2026", m.incidentName));

    var fromPredicate = m.formFrom.toLowerCase().endsWith("ETO Winlink Thursday Participant".toLowerCase());
    count(sts.test("Form From should end with ETO Winlink Thursday Participant", fromPredicate, m.formFrom));

    count(sts.test("Form Subject should be #EV", "ARRL Field Day 2026 Participation", m.formSubject));
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));

    var fdEntry = (FieldDayEntry) null;
    var msg = m.formMessage;
    var msgOk = msg != null;
    if (msgOk) {
      var clubName = "";
      var participantCount = Integer.valueOf(0);
      var location = "";
      var aresCount = Integer.valueOf(0);

      var lines = msg.split("\n");
      if (lines.length >= 4) {
        var clubLine = lines[0];
        var testResult = sts.testStartsWith("Message line #1 should start with #EV", "Club:", clubLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        clubName = clubLine.substring(clubLine.indexOf(" ") + 1);
        clubName = clubName == null ? "" : clubName;
        getCounter("Club Name").increment(clubName);

        var participantsLine = lines[1];
        testResult = sts.testStartsWith("Message line #2 should start with #EV", "Participants:", participantsLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        var participantCountString = participantsLine.substring(participantsLine.indexOf(" ") + 1).strip();
        try {
          participantCount = Integer.parseInt(participantCountString);
          count(sts.test("Message line #2 should end with a number", true));
          getCounter("Participant Count").increment(participantCount);
        } catch (Exception e) {
          count(sts.test("Message line #2 should end with a number", false, participantCountString));
        }

        var locationLine = lines[2];
        testResult = sts.testStartsWith("Message line #3 should start with #EV", "Field Day Location:", locationLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        var locationFields = locationLine.split(":");
        if (locationFields.length >= 2) {
          location = locationFields[1].strip();
          getCounter("Field Day Location").increment(location);
          count(sts.test("Message line 3 should be parsable", true, locationLine));
        } else {
          count(sts.test("Message line 3 should be parsable", false, locationLine));
        }

        var aresLine = lines[3];
        testResult = sts.testStartsWith("Message line #4 should start with #EV", "ARES Participants:", aresLine);
        count(testResult);
        msgOk = msgOk && testResult.ok();
        var aresFields = aresLine.split(":");
        if (aresFields.length >= 2) {
          var aresCountString = aresFields[1].strip();
          try {
            aresCount = Integer.parseInt(aresCountString);
            getCounter("ARES Count").increment(aresCount);
            count(sts.test("Message line #4 should end with a number", true));
          } catch (Exception e) {
            count(sts.test("Message line #4 should end with a number", false, aresCountString));
          }
          count(sts.test("Message line 4 should be parsable", true, aresLine));
        } else {
          count(sts.test("Message line 4 should be parsable", false, aresLine));
        }

        fdEntry = new FieldDayEntry(m, clubName, participantCount, location, aresCount);
      } else {
        msgOk = false;
        fdEntry = new FieldDayEntry(m, clubName, participantCount, location, aresCount);
      }
    }
    fdMap.put(m.from, fdEntry);
    count(sts.test("Message text should be correctly formatted", msgOk, m.formMessage));

    count(sts.testIfPresent("Approved by should be present", m.approvedBy));

    var posTitlePredicate = m.position.toLowerCase().endsWith("ETO participant".toLowerCase());
    count(sts.test("Position/Title should be ETO participant", posTitlePredicate, m.position));
  }

  @Override
  public void postProcess() {
    super.postProcess();
    // mapMaps(fdMap.values());
  }

  @SuppressWarnings("unused")
  private void mapMaps(Collection<FieldDayEntry> values) {
    Function<FieldDayEntry, String> getClubName = (s) -> s.clubName;
    Function<FieldDayEntry, String> getLocation = s -> s.fdLocation;
    Function<FieldDayEntry, String> getParticipantCount = s -> s.participantCount == null ? null
        : String.valueOf(s.participantCount);
    Function<FieldDayEntry, String> getAresCount = s -> s.aresCount == null ? null : String.valueOf(s.aresCount);

    makeSqueezedMap(values, getClubName, "club", "Club", true);
    makeSqueezedMap(values, getLocation, "location", "Location", true);
    makeSqueezedMap(values, getParticipantCount, "participants", "Participants", false);
    makeSqueezedMap(values, getAresCount, "ARES Particpants", "ARES-Participant", false);
  }

  private void makeSqueezedMap(Collection<FieldDayEntry> values, Function<FieldDayEntry, String> getValue,
      String lcLabel, String ucLabel, boolean showPopupCount) {

    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);
    var desiredLayers = 10;
    var minimumGroupSize = 2;

    var nameCallListMap = new HashMap<String, List<String>>();
    for (var fdEntry : values) {

      var name = getValue.apply(fdEntry);
      name = (name == null || name.strip().isEmpty()) ? "(none)" : name.toUpperCase();
      name = name.trim().replaceAll("\n", "").replaceAll("\"", "");
      var list = nameCallListMap.getOrDefault(name, new ArrayList<String>());
      var call = fdEntry.m.from;
      list.add(call);
      nameCallListMap.put(name, list);
    }

    final var otherName = "other";
    if (nameCallListMap.containsKey(otherName)) {
      throw new RuntimeException("Need a new squeezed name");
    }

    var squeezedList = new ArrayList<String>();
    var sizeNameListMap = new TreeMap<Integer, List<String>>();
    var names = new ArrayList<String>(nameCallListMap.keySet());
    for (var name : names) {
      var callList = nameCallListMap.get(name);
      var size = callList.size();
      if (size < minimumGroupSize) {
        squeezedList.addAll(callList);
        nameCallListMap.remove(name);
        continue;
      }
      var nameList = sizeNameListMap.getOrDefault(size, new ArrayList<String>());
      nameList.add(name);
      sizeNameListMap.put(size, nameList);
    }
    nameCallListMap.put(otherName, squeezedList);
    sizeNameListMap.put(squeezedList.size(), squeezedList);

    names.clear();
    for (var groupSize : sizeNameListMap.descendingKeySet()) {
      var groupList = sizeNameListMap.get(groupSize);
      names.addAll(groupList);
      if (names.size() >= desiredLayers) {
        break;
      }
    }

    var rng = new Random(2025);
    var colorBag = new RenewableBag<>(IMapService.etoColorMap.values(), rng);
    var nameColorMap = new HashMap<String, String>();
    for (var group : nameCallListMap.keySet()) {
      var color = colorBag.next();
      nameColorMap.put(group, color);
    }

    var mapEntries = new ArrayList<MapEntry>(values.size());
    for (var fdEntry : values) {
      var name = getValue.apply(fdEntry);
      name = (name == null || name.strip().isEmpty()) ? "(none)" : name;
      name = name.trim().replaceAll("\n", "").replaceAll("\"", "");
      var callList = nameCallListMap.get(name);
      var nameForColor = name;
      if (callList == null || callList.size() < minimumGroupSize) {
        nameForColor = otherName;
      }

      var mapLocation = fdEntry.m.mapLocation;
      var color = nameColorMap.get(nameForColor);
      var prefix = "<b>" + fdEntry.m.from + "</b><hr>";

      var nameSize = callList == null ? 1 : callList.size();
      var content = prefix //
          + ucLabel + ": " + name + "\n" //
          + ((showPopupCount) ? "Count: " + nameSize + "\n" : "");
      var mapEntry = new MapEntry(fdEntry.m.from, null, mapLocation, content, color);
      mapEntries.add(mapEntry);
    }

    var xnames = new ArrayList<String>(
        nameCallListMap.keySet().stream().filter(g -> nameCallListMap.get(g) != null).toList());
    Collections.sort(xnames, (g1, g2) -> nameCallListMap.get(g2).size() - nameCallListMap.get(g1).size());
    var layers = new ArrayList<MapLayer>(names.size());
    for (var name : xnames) {
      var callList = nameCallListMap.get(name);
      var count = callList.size();
      var layerName = ucLabel + ": " + name + ", size: " + count;
      var color = nameColorMap.get(name);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var legendTitle = ucLabel + " Counts (" + mapEntries.size() + " messages, min size: " + minimumGroupSize + ")";
    var context = new MapContext(outputPath, //
        dateString + "-map-" + ucLabel + "-Counts", // file name
        dateString + ucLabel + "-Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

}