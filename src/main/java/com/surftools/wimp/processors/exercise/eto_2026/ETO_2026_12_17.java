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

package com.surftools.wimp.processors.exercise.eto_2026;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IExportedMessageEditor;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.feedback.FeedbackMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * A Winlink ICS-213 message, with a recipe CSV
 *
 * Produce an aggregate CSV of all recipes. Read a separate, extended participant history CSV. The ICS-213 message
 * contains a participant count of number of 2026 exercises. Compare and map.
 *
 * @author bobt
 *
 */
public class ETO_2026_12_17 extends SingleMessageFeedbackProcessor implements IExportedMessageEditor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_12_17.class);

  record RecipeRecord(String contributor, String recipeName, String reason, String ingredients, String instructions,
      String callsign, String submitDate) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (RecipeRecord) other;
      return contributor.compareTo(o.contributor);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Contributor", "RecipeName", "Reason", "Ingredients", "Instructions", "Callsign",
          "Submit Date" };
    }

    @Override
    public String[] getValues() {
      return new String[] { contributor, recipeName, reason, ingredients, instructions };
    }

    public static RecipeRecord from(String[] values) {
      if (values.length == 7) {
        return new RecipeRecord(values[0], values[1], values[2], values[3], values[4], values[5], values[6]);
      } else {
        return new RecipeRecord(values[0], values[1], values[2], values[3], values[4], "n/a", "n/a");
      }
    }

    public static RecipeRecord from(String[] values, String callsign, String exerciseDateString) {
      return new RecipeRecord(values[0], values[1], values[2], values[3], values[4], callsign, exerciseDateString);
    }
  }

  record ParticipantHistory(String call, String countString, String firstDate, String lastDate) {
    public int getCount() {
      return Integer.valueOf(countString);
    }

    public static ParticipantHistory fromValues(String[] values) {
      return new ParticipantHistory(values[0], values[1], values[2], values[3]);
    }
  }

  private Map<String, RecipeRecord> recipeMap;
  private Map<String, ParticipantHistory> historyMap;
  private Map<String, Integer> reportedExerciseCountMap;
  private Map<String, Integer> actualExerciseCountMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213;
    var extraOutboundMessageText = getNextExerciseInstructions();
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

    // load historic/reference recipes
    recipeMap = new HashMap<>();
    var referenceRecipeMapPath = Path.of(inputPathName, "reference-recipes.csv");
    var referenceRecipeList = ReadProcessor.readCsvFileIntoFieldsArray(referenceRecipeMapPath, ',', false, 1);
    for (var recipeFields : referenceRecipeList) {
      var recipe = RecipeRecord.from(recipeFields);
      recipeMap.put(recipe.contributor, recipe);
    }

    reportedExerciseCountMap = new HashMap<>();
    actualExerciseCountMap = new HashMap<>();

    // load history map
    historyMap = new HashMap<>();
    var historyMapPath = Path.of(inputPathName, "2026-09-10-participantHistory.csv");

    var historyList = ReadProcessor.readCsvFileIntoFieldsArray(historyMapPath, ',', false, 1);
    var maxDate = LocalDate.MIN;
    for (var historyValues : historyList) {
      var historyEntry = ParticipantHistory.fromValues(historyValues);
      historyMap.put(historyEntry.call, historyEntry);

      var lastDate = LocalDate.parse(historyEntry.lastDate);
      if (lastDate.isAfter(maxDate)) {
        maxDate = lastDate;
      }
    }

    logger.info("read " + historyMap.size() + " entries from file: " + historyMapPath);
    logger.info("max date: " + maxDate);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213Message) message;
    // the usual stuff
    count(sts.testStartsWith("Message Subject should start with #EV", "ICS-213: ETO Exercise Dec 17, 2026", m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));
    count(sts.test("Form Location should be valid", m.formLocation.isValid(), m.formLocation.toString()));
    count(sts.test("Organization Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Is Exercise should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", "ETO Recipe Cookbook", m.incidentName));
    getCounter("Incident Name").increment(m.incidentName);

    // ETO-10;ETO-BK comes in as one word, must be split, etc. just test for presence?
    var formToSet = new HashSet<String>(Arrays.asList(m.formTo.split(";")));
    formToSet.retainAll(expectedDestinations);
    var isExpectedDestination = formToSet.size() > 0;
    count(sts.test("Form To should be a Clearinghouse", isExpectedDestination, m.formTo));
    getCounter("Form To").increment(m.formTo);
    // count(sts.test("Form To should be #EV", m.to, m.formTo));

    count(sts.testEndsWith("Form From should end with #EV", " / ETO Winlink Thursday Participant", m.formFrom));
    // count(sts.test("Form From should be #EV", m.from + " / ETO Winlink Thursday Participant", m.formFrom));

    count(sts.test("Form Subject should be #EV", "ETO Exercise Dec 17, 2026", m.formSubject));
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));
    count(sts.testIfPresent("Message body should be present", m.formMessage));

    var approvedByEndsWithCall = m.approvedBy.toLowerCase().endsWith(m.from.toLowerCase());
    count(sts.test("Approved by should end with sender's call sign", approvedByEndsWithCall));
    // count(sts.testEndsWith("Approved by should end with #EV", " / " + m.from, m.approvedBy));

    count(sts.test("Position/Title should be #EV", "ETO participant", m.position));

    // message body
    if (m.formMessage != null) {
      var lines = m.formMessage.split("\n");
      count(sts.test("Form Message body should contain exactly #EV lines", "1", String.valueOf(lines.length)));

      var fields = lines[0].split(" ");
      count(sts.test("Form Message body line 1 should contain #EV fields", "3", String.valueOf(fields.length)));

      if (fields.length >= 1) {
        var countString = fields[0];

        try {
          var count = Integer.parseInt(countString);
          count(sts.test("Form Message body line 1 field 1 should be a number", true, countString));
          reportedExerciseCountMap.put(m.from, count);
          getCounter("2026 Exercise Counts (reported)").increment(count);

          var historyEntry = historyMap.get(m.from);
          var actualCount = (historyEntry == null) ? 1 : historyEntry.getCount() + 1;
          getCounter("2026 Exercise Counts (actual)").increment(actualCount);
          actualExerciseCountMap.put(m.from, actualCount);

        } catch (Exception e) {
          count(sts.test("Form Message body line 1 field 1 should be a number", false, countString));
        }
      } else {
        count(sts.test("Form Message body line 1 field 1 should be a number", false));
      }

      if (fields.length >= 2) {
        var etoString = fields[1];
        count(sts.test("Form Message body line 1 field 2 should be #EV", "ETO", etoString));
      } else {
        count(sts.test("Form Message body line 1 field 2 should be ETO", false));
      }

      if (fields.length >= 3) {
        var exerciseString = fields[2];
        count(sts.testStartsWith("Form Message body line 1 field 3 should start with #EV", "exercise", exerciseString));
      } else {
        count(sts.test("Form Message body line 1 field 3 should start with exercise", false));

      }
    }

    // attachments
    var nCsvs = 0;
    var maxDateTime = LocalDateTime.MIN;
    String recipeFileName = null;
    final var dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    for (var attachmentName : m.attachments.keySet()) {
      var lcName = attachmentName.toLowerCase();
      try {
        if (lcName.contains("holiday-recipe-") && lcName.endsWith("csv") && lcName.length() >= 19) {
          ++nCsvs;
          var index = lcName.indexOf(".csv");
          if (index >= 0) {
            var dtString = lcName.substring(0, index);
            var sb = new StringBuilder(dtString).reverse().toString().substring(0, 19);
            dtString = new StringBuilder(sb).reverse().toString();
            LocalDateTime dt = null;
            try {
              dt = LocalDateTime.parse(dtString, dtf);
              if (dt.isAfter(maxDateTime) || recipeFileName == null) {
                maxDateTime = dt;
                recipeFileName = attachmentName;
              }
            } catch (Exception e) {
              recipeFileName = attachmentName;
            }
          }
        }
      } catch (Exception e) {
        continue;
      }
    } // end for over attachments

    if (recipeFileName != null) {
      count(sts.test("valid recipe attachments found", true));
      count(sts.test("Number of holiday recipe attachments should be #EV", "1", String.valueOf(nCsvs)));
      getCounter("Number of holiday recipe attachments").increment(nCsvs);
      var value = new String(m.attachments.get(recipeFileName));
      var recipeValues = ReadProcessor.readCsvStringIntoFieldsArray(value, ',', false, 1);
      var recipeRecord = RecipeRecord.from(recipeValues.get(0), m.from, dateString);
      recipeMap.put(m.from, recipeRecord);
    } else {
      count(sts.test("valid recipe attachments found", false));
    }

    getCounter("Number of CSV attachments").increment(nCsvs);

    count(sts.test("Number of CSV attachments should be #EV", "1", String.valueOf(nCsvs)));
  }

  @Override
  public void postProcess() {
    super.postProcess();

    var recipePath = Path.of(publishedPathName, "2026-12-17-holiday-recipes.csv");
    var recipeList = new ArrayList<IWritableTable>(recipeMap.values());
    WriteProcessor.writeTable(recipeList, recipePath);

    makeMaps();

    var browserName = "eto-holiday-recipe-browser.html";
    var fromFile = Path.of(inputPathName, browserName).toFile();
    var toFile = Path.of(publishedPathName, browserName).toFile();
    try {

      Files.copy(fromFile, toFile);
    } catch (IOException e) {
      logger.error("### Error copying browser file: " + toFile.getAbsolutePath() + ", " + e.getLocalizedMessage());
    }
  }

  public void makeMaps() {
    makeNutMap();
    makeChocolateMap();
    makeParticipationCountMap(true);
    makeParticipationCountMap(false);
    makeCountDifferenceMap();
  }

  private void makeNutMap() {
    final List<String> TREE_NUTS = List
        .of("almond", "walnut", "pecan", "hazelnut", "cashew", "pistachio", "macadamia nut", "brazil nut", "chestnut",
            "pine nut", "coconut", "gingko", "beechnut", "butternut", "hickory nut", "candlenut");

    enum NutStatus {
      NO_NUTS, PEANUT_ONLY, TREE_ONLY, BOTH
    }

    final var nutStatusCountMap = new HashMap<NutStatus, Integer>();
    final var legendNames = Map
        .of(NutStatus.NO_NUTS, "No Nuts", //
            NutStatus.PEANUT_ONLY, "Peanuts Only", //
            NutStatus.TREE_ONLY, "Tree Nuts Only", //
            NutStatus.BOTH, "Both Peanuts and Tree Nuts");

    final var legendColors = Map
        .of( //
            NutStatus.NO_NUTS, "#3cb44b", // green
            NutStatus.PEANUT_ONLY, "#9a6324", // brown
            NutStatus.TREE_ONLY, "#4363d8", // //blue
            NutStatus.BOTH, "#e6194b"); // red

    final var mapEntries = new ArrayList<MapEntry>(recipeMap.size());
    for (var messageId : mIdFeedbackMap.keySet()) {
      var feedbackMessage = (FeedbackMessage) mIdFeedbackMap.get(messageId);
      var m = feedbackMessage.message();

      var recipe = recipeMap.get(m.from);
      if (recipe == null) {
        continue;
      }

      var ingredients = recipe.ingredients;
      if (ingredients == null) {
        continue;
      }

      var ingredientsLC = ingredients.toLowerCase();
      var hasPeanut = ingredientsLC.contains("peanut");
      var hasTreeNut = false;
      for (var treeNut : TREE_NUTS) {
        if (ingredientsLC.contains(treeNut)) {
          hasTreeNut = true;
          break;
        }
      }

      NutStatus nutStatus = NutStatus.NO_NUTS;
      if (hasPeanut && !hasTreeNut) {
        nutStatus = NutStatus.PEANUT_ONLY;
      } else if (hasTreeNut && !hasPeanut) {
        nutStatus = NutStatus.TREE_ONLY;
      } else if (hasPeanut && hasTreeNut) {
        nutStatus = NutStatus.BOTH;
      }
      var count = nutStatusCountMap.getOrDefault(nutStatus, Integer.valueOf(0));
      ++count;
      nutStatusCountMap.put(nutStatus, count);

      var location = m.mapLocation;
      var color = legendColors.get(nutStatus);
      var prefix = "<b>" + m.from + "</b><hr>";
      var content = prefix //
          + "has peanuts: " + hasPeanut + "\n" + "has tree nuts: " + hasTreeNut + "\n" + "ingredients: \n" //
          + ingredients.replaceAll(";", "\n") + "\n";
      var mapEntry = new MapEntry(m.from, null, location, content, color);
      mapEntries.add(mapEntry);

    }

    var layers = new ArrayList<MapLayer>(NutStatus.values().length);
    for (var nutStatus : NutStatus.values()) {
      var count = nutStatusCountMap.getOrDefault(nutStatus, Integer.valueOf(0));
      var layerName = legendNames.get(nutStatus) + ", count: " + count;
      var color = legendColors.get(nutStatus);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);

    var legendTitle = "Recipe Nut Counts (" + mapEntries.size() + " recipes)";
    var context = new MapContext(outputPath, //
        dateString + "-map-RecipeNutCounts", // file name
        dateString + " Recipe Nut Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  private void makeChocolateMap() {
    enum ChocolateStatus {
      NO_CHOCOLATE, HAS_CHOCOLATE
    }

    final var chocolateStatusCountMap = new HashMap<ChocolateStatus, Integer>();
    final var legendNames = Map
        .of(ChocolateStatus.NO_CHOCOLATE, "No Chocolate", //
            ChocolateStatus.HAS_CHOCOLATE, "Has Chocolate");

    final var legendColors = Map
        .of(ChocolateStatus.NO_CHOCOLATE, "#e6194b", // red
            ChocolateStatus.HAS_CHOCOLATE, "#9a6324"); // brown

    final var mapEntries = new ArrayList<MapEntry>(recipeMap.size());
    for (var messageId : mIdFeedbackMap.keySet()) {
      var feedbackMessage = (FeedbackMessage) mIdFeedbackMap.get(messageId);
      var m = feedbackMessage.message();

      var recipe = recipeMap.get(m.from);
      if (recipe == null) {
        continue;
      }

      var ingredients = recipe.ingredients;
      if (ingredients == null) {
        continue;
      }

      var ingredientsLC = ingredients.toLowerCase();
      var hasChocolate = ingredients.contains("chocolate");

      ChocolateStatus chocolateStatus = ingredientsLC.contains("chocolate") ? ChocolateStatus.HAS_CHOCOLATE
          : ChocolateStatus.NO_CHOCOLATE;

      var count = chocolateStatusCountMap.getOrDefault(chocolateStatus, Integer.valueOf(0));
      ++count;
      chocolateStatusCountMap.put(chocolateStatus, count);

      var location = m.mapLocation;
      var color = legendColors.get(chocolateStatus);
      var prefix = "<b>" + m.from + "</b><hr>";
      var content = prefix //
          + "has chocolate: " + hasChocolate + "\ningredients: \n" //
          + ingredients.replaceAll(";", "\n") + "\n";
      var mapEntry = new MapEntry(m.from, null, location, content, color);
      mapEntries.add(mapEntry);

    }

    var layers = new ArrayList<MapLayer>(ChocolateStatus.values().length);
    for (var nutStatus : ChocolateStatus.values()) {
      var count = chocolateStatusCountMap.getOrDefault(nutStatus, Integer.valueOf(0));
      var layerName = legendNames.get(nutStatus) + ", count: " + count;
      var color = legendColors.get(nutStatus);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);

    var legendTitle = "Recipe Chocolate Counts (" + mapEntries.size() + " recipes)";
    var context = new MapContext(outputPath, //
        dateString + "-map-RecipeChocolateCounts", // file name
        dateString + " Recipe Chocolate Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  private void makeParticipationCountMap(boolean isActual) {
    var participationMap = isActual ? actualExerciseCountMap : reportedExerciseCountMap;

    var mapService = new MapService(cm, mm);

    final var tensCountMap = new HashMap<Integer, Integer>();
    final var legendNames = Map
        .of(0, "1 - 9 exercises", //
            1, "10 - 19 exercises", //
            2, "20 - 29 exercises", //
            3, "30 - 39 exercises", //
            4, "40 - 49 exercises", //
            5, "50 or more exercises");

    var legendColors = mapService.makeGradientMap(0, 120, 6); // red -> green

    final var mapEntries = new ArrayList<MapEntry>(recipeMap.size());
    for (var messageId : mIdFeedbackMap.keySet()) {
      var feedbackMessage = (FeedbackMessage) mIdFeedbackMap.get(messageId);
      var m = feedbackMessage.message();

      var recipe = recipeMap.get(m.from);
      if (recipe == null) {
        continue;
      }

      var particpationCount = participationMap.get(m.from);
      var tensCount = particpationCount / 10;
      var actualCount = actualExerciseCountMap.get(m.from);
      var reportedCount = reportedExerciseCountMap.get(m.from);
      var deltaCount = actualCount - reportedCount;

      var mapCount = tensCountMap.getOrDefault(tensCount, Integer.valueOf(0));
      ++mapCount;
      tensCountMap.put(tensCount, mapCount);

      var location = m.mapLocation;
      var color = legendColors.get(tensCount);
      var prefix = "<b>" + m.from + "</b><hr>";
      var content = prefix //
          + "actual exercise count: " + actualCount + "\n" //
          + "reported exercise count: " + reportedCount + "\n" //
          + "difference: " + deltaCount + "\n";
      var mapEntry = new MapEntry(m.from, null, location, content, color);
      mapEntries.add(mapEntry);
    }

    var layers = new ArrayList<MapLayer>();
    for (var tensCount = 0; tensCount <= 5; ++tensCount) {
      var count = tensCountMap.getOrDefault(tensCount, Integer.valueOf(0));
      var layerName = legendNames.get(tensCount) + ", count: " + count;
      var color = legendColors.get(tensCount);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var dateString = cm.getAsString(Key.EXERCISE_DATE);

    var actualVsReported = isActual ? "Actual" : "Reported";
    var legendTitle = actualVsReported + " 2026 Participation Counts (" + mapEntries.size() + " messages)";
    var context = new MapContext(outputPath, //
        dateString + "-map-" + actualVsReported + "Counts", // file name
        dateString + actualVsReported + " Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);

  }

  private void makeCountDifferenceMap() {
    // var participationMap = isActual ? actualExerciseCountMap : reportedExerciseCountMap;

    var mapService = new MapService(cm, mm);

    final var deltaCountMap = new HashMap<Integer, Integer>();
    final var legendNames = Map
        .of(0, "Actual - Reported >= 10", //
            1, "Actual - Reported >= 5", //
            2, "Actual - Reported >= 1", //
            3, "Actual - Reported == 0", //
            4, "Actual - Reported <= -1", //
            5, "Actual - Reported <= -5", //
            6, "Actual - Reported <= -10");

    final var legendColors = Map
        .of(0, MapService.rgbToHex(0, 0, 255), // blue
            1, MapService.rgbToHex(0, 128, 128), //
            2, MapService.rgbToHex(0, 255, 128), //
            3, MapService.rgbToHex(0, 255, 0), // green
            4, MapService.rgbToHex(85, 170, 0), //
            5, MapService.rgbToHex(170, 85, 0), //
            6, MapService.rgbToHex(255, 0, 0)); // red

    final var mapEntries = new ArrayList<MapEntry>(recipeMap.size());
    for (var messageId : mIdFeedbackMap.keySet()) {
      var feedbackMessage = (FeedbackMessage) mIdFeedbackMap.get(messageId);
      var m = feedbackMessage.message();

      var recipe = recipeMap.get(m.from);
      if (recipe == null) {
        continue;
      }

      var actualCount = actualExerciseCountMap.get(m.from);
      var reportedCount = reportedExerciseCountMap.get(m.from);
      var deltaCount = actualCount - reportedCount;
      var deltaIndex = -1;
      if (deltaCount >= 10) {
        deltaIndex = 0;
      } else if (deltaCount >= 5) {
        deltaIndex = 1;
      } else if (deltaCount > 0) {
        deltaIndex = 2;
      } else if (deltaCount == 0) {
        deltaIndex = 3;
      } else if (deltaCount <= -10) {
        deltaIndex = 6;
      } else if (deltaCount <= -5) {
        deltaIndex = 5;
      } else if (deltaCount < 0) {
        deltaIndex = 4;
      }

      var mapCount = deltaCountMap.getOrDefault(deltaIndex, Integer.valueOf(0));
      ++mapCount;
      deltaCountMap.put(deltaIndex, mapCount);

      var location = m.mapLocation;
      var color = legendColors.get(deltaIndex);
      var prefix = "<b>" + m.from + "</b><hr>";
      var content = prefix //
          + "actual exercise count: " + actualCount + "\n" //
          + "reported exercise count: " + reportedCount + "\n" //
          + "difference: " + deltaCount + "\n";
      var mapEntry = new MapEntry(m.from, null, location, content, color);
      mapEntries.add(mapEntry);
    }

    var layers = new ArrayList<MapLayer>();
    for (var deltaIndex = 0; deltaIndex <= 6; ++deltaIndex) {
      var count = deltaCountMap.getOrDefault(deltaIndex, Integer.valueOf(0));
      var layerName = legendNames.get(deltaIndex) + ", count: " + count;
      var color = legendColors.get(deltaIndex);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var dateString = cm.getAsString(Key.EXERCISE_DATE);

    var legendTitle = "Actual vs Reported 2026 Participation Counts (" + mapEntries.size() + " messages)";
    var context = new MapContext(outputPath, //
        dateString + "-map-actual-vs-reported-Counts", // file name
        dateString + "Actual vs Reported Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);

  }
}