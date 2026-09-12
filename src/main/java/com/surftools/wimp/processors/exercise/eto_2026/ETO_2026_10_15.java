/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.RenewableBag;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.service.map.IMapService;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2026-10-15: Shakeout 2026 drill, organized by LAX Northeast
 *
 * One DYFI, two (or more) Plain, one with a quiz, one with a survey
 *
 * @author bobt
 *
 */
public class ETO_2026_10_15 extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2026_10_15.class);

  private static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";
  private static final String QUIZ_HEADERS = "Timestamp (UTC),Participant Callsign,Street Address,Latitude,Longitude,Score,Total Questions,Q1_Question,Q1_User_Answer,Q1_Correct_Answer,Q1_Status,Q2_Question,Q2_User_Answer,Q2_Correct_Answer,Q2_Status,Q3_Question,Q3_User_Answer,Q3_Correct_Answer,Q3_Status,Q4_Question,Q4_User_Answer,Q4_Correct_Answer,Q4_Status,Q5_Question,Q5_User_Answer,Q5_Correct_Answer,Q5_Status,Q6_Question,Q6_User_Answer,Q6_Correct_Answer,Q6_Status,Q7_Question,Q7_User_Answer,Q7_Correct_Answer,Q7_Status,Q8_Question,Q8_User_Answer,Q8_Correct_Answer,Q8_Status,Q9_Question,Q9_User_Answer,Q9_Correct_Answer,Q9_Status,Q10_Question,Q10_User_Answer,Q10_Correct_Answer,Q10_Status,Q11_Question,Q11_User_Answer,Q11_Correct_Answer,Q11_Status,Q12_Question,Q12_User_Answer,Q12_Correct_Answer,Q12_Status";
  private static final String SURVEY_HEADERS = "Timestamp (UTC),Participant Callsign,Operator Last Name,Location / Street Address,Latitude,Longitude,Basic_water-food,Basic_cell-charger,Basic_weather-radio,Basic_flashlight,Basic_first-aid,Basic_whistle,Basic_dust-mask,Basic_sanitation,Basic_wrench,Basic_can-opener,Basic_maps,Add_medications,Add_infant,Add_pet,Add_documents,Add_cash,Add_reference,Add_sleeping-bag,Add_clothing,Add_fire-ext,Add_matches,Add_feminine,Add_mess-kit,Add_paper-pencil,Add_books-games,Basic_Items_Checked_Count,Additional_Items_Checked_Count,Total_Items_Checked";

  private List<QuizEntry> quizes = new ArrayList<>();
  private List<SurveyEntry> surveys = new ArrayList<>();

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

    public DyfiMessage dyfiMessage;
    public PlainMessage quizMessage;
    public PlainMessage surveyMessage;

    public List<String> plainMessageIds = new ArrayList<>();

    public boolean dyfiIsExercise;
    public boolean dyfiIsFelt;
    public String dyfiResponse;
    public String dyfiIntensity;
    public boolean dyfiIntensityAbove5;

    public String dyfiRawComments;
    public String dyfiAffiliation;
    public String dyfiOrganization;
    public String dyfiMode;
    public String dyfiBand;
    public String dyfiRemainingComments;

    public int quizNCorrect;
    public int quizNAnswered;
    public String quizAnswers;

    public List<String> surveyBasicItems = new ArrayList<>();
    public List<String> surveyAdditionalItems = new ArrayList<>();
    public int surveyNBasicItems;
    public int surveyNAdditionalItems;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list
          .addAll(Arrays
              .asList(new String[] { //
                  "DYFI", "Quiz", "Survey", "# Plain", //
                  "MessageIds", "Plain MessageIds", //
                  "DYFI IsExercise", "DYFI IsFelt", "DYFI Response", "DYFI Intensity", "DYFI Intensity > 5", //
                  "DYFI Raw Comments", "DYFI Affiliation", "DYFI Organization", "DYFI Mode", "DYFI Band,",
                  "DYFI Comments", //
                  "Quiz #Correct", "Quiz #Correct", "Quiz Answers", //
                  "Survey Basic Items", "Survey Additional Items", "Survey #Basic", "Survey #Additional" //
              }));
      return list.toArray(new String[0]);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(Arrays
              .asList(new String[] { //
                  mId(dyfiMessage), mId(quizMessage), mId(surveyMessage), s(plainMessageIds.size()), //
                  String.join(",", messageIds), String.join(",", plainMessageIds), //
                  s(dyfiIsExercise), s(dyfiIsFelt), dyfiResponse, dyfiIntensity, s(dyfiIntensityAbove5), //
                  dyfiRawComments, dyfiAffiliation, dyfiOrganization, dyfiMode, dyfiBand, dyfiRemainingComments, //
                  s(quizNCorrect), s(quizNAnswered), quizAnswers, //
                  String.join(",", surveyBasicItems), String.join(",", surveyAdditionalItems), s(surveyNBasicItems),
                  s(surveyNAdditionalItems)//
              }));

      return list.toArray(new String[0]);
    };

    public int getMessageCount() {
      var messageCount = dyfiMessage != null ? 1 : 0;
      messageCount += quizMessage != null ? 1 : 0;
      messageCount += surveyMessage != null ? 1 : 0;

      return messageCount;
    }

    public String getMessageSummary() {
      if (getMessageCount() == 3) {
        return "<b>" + from + "</b><hr>\n" + "All messages received!";
      } else {
        var messages = new ArrayList<ExportedMessage>();
        messages.add(dyfiMessage);
        messages.add(quizMessage);
        messages.add(surveyMessage);

        final var labels = List.of("dyfi", "quiz", "survey");

        var rList = new ArrayList<String>();
        var mList = new ArrayList<String>();
        for (var i = 0; i < messages.size(); ++i) {
          var message = messages.get(i);
          var label = labels.get(i);
          if (message == null) {
            mList.add(label);
          } else {
            rList.add(label);
          }
        }

        var sb = new StringBuilder();
        sb.append("<b>" + from + "</b><hr>\n");
        sb.append("received: " + String.join(",", rList) + "\n");
        sb.append("missing:  " + String.join(",", mList));
        var ret = sb.toString();
        return ret;
      }
    }
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    // #MM must define acceptableMessages
    acceptableMessageTypesSet.addAll(getExpectedMessageTypes());

    super.initialize(cm, mm, logger);

    allowPerfectMessageReporting = false;

    var extraOutboundMessageText = """

        -----------------------------------------------------------------------------

        Thank you for your participation. The final results will shortly be posted at
        https://emcomm-training.org/Non-ETO_Exercises.html

        We invite you to continue to participate in our regular weekly exercises.
        See our site at: https://emcomm-training.org/Winlink_Thursdays.html

        """;
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

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

    var type = message.getMessageType();
    if (type == MessageType.DYFI) {
      handle_DyFiMessage(summary, (DyfiMessage) message);
    } else if (type == MessageType.PLAIN) {
      handle_PlainMessage(summary, (PlainMessage) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_PlainMessage(Summary summary, PlainMessage m) {
    sts.setExplanationPrefix("(plain) ");
    summary.plainMessageIds.add(m.messageId);
    var attachmentCount = m.attachments.size();
    count(sts.test("Plain attachment count should be #EV", 1, attachmentCount));

    for (var attachmentName : m.attachments.keySet()) {
      var value = new String(m.attachments.get(attachmentName));
      if (attachmentName.toUpperCase().contains("QUIZ ANSWERS")) {
        count(sts.test("Attachment Name valid", true, attachmentName));
        handle_quiz(summary, m, attachmentName, value);
      } else if (attachmentName.toUpperCase().contains("PREPAREDNESS SURVEY")) {
        count(sts.test("Attachment Name valid", true, attachmentName));
        handle_survey(summary, m, attachmentName, value);
      } else {
        count(sts.test("Attachment Name valid", false, attachmentName));
      }
    }
  }

  private enum PlainType {
    Quiz, Survey
  };

  private void handle_quiz(Summary summary, PlainMessage m, String attachmentName, String value) {
    sts.setExplanationPrefix("(quiz) ");
    var fields = getFields(value, PlainType.Quiz);

    // skip if we already have a quiz
    if (summary.quizMessage != null) {
      return;
    }
    summary.quizMessage = m;

    /*
     * 12 questions
     *
     * q and a start on zero-based field 7, column h
     *
     * 4 fields per: question, user_answer, correct answer, status
     */

    var nCorrect = 0;
    var nAnswered = 0;
    var answers = new ArrayList<String>();

    if (fields != null) {
      quizes.add(new QuizEntry(fields));
      var fIndex = 7;
      for (var qIndex = 1; qIndex <= 12; ++qIndex) {
        @SuppressWarnings("unused")
        var question = fields[fIndex];
        var userAnswer = fields[fIndex + 1];
        var correctAnswer = fields[fIndex + 2];
        var status = fields[fIndex + 3];

        count(sts.test("Q" + String.format("%02d", qIndex) + " answer should be #EV", correctAnswer, userAnswer));
        getCounter("Quiz Q" + String.format("%02d", qIndex)).increment(userAnswer);

        answers.add(userAnswer);

        if (status.equals("Correct")) {
          ++nCorrect;
        }

        if (!userAnswer.equals("No answer")) {
          ++nAnswered;
        }

        fIndex += 4;
      }
    }

    getCounter("Message Type").increment("QUIZ");

    summary.messageIds.add("quiz: " + m.messageId);
    summary.quizNCorrect = nCorrect;
    getCounter("Quiz #Correct").increment(nCorrect);
    summary.quizNAnswered = nAnswered;
    getCounter("Quiz #Answered").increment(nAnswered);
    summary.quizAnswers = String.join(",", answers);
  }

  private void handle_survey(Summary summary, PlainMessage m, String attachmentName, String value) {
    sts.setExplanationPrefix("(survey) ");
    var fields = getFields(value, PlainType.Survey);

    // skip if we already have a quiz
    if (summary.surveyMessage != null) {
      return;
    }
    summary.surveyMessage = m;

    final var basicList = List
        .of("water-food", "cell-charger", "weather-radio", "flashlight", "first-aid", "whistle", "dust-mask",
            "sanitation", "wrench", "can-opener", "maps");
    final var additionalList = List
        .of("medications", "infant", "pet", "documents", "cash", "reference", "sleeping-bag", "clothing", "fire-ext",
            "matches", "feminine", "mess-kit", "paper-pencil", "books-games");

    if (fields != null) {
      surveys.add(new SurveyEntry(fields));
      /*
       * Basic fields start in column G or 6
       */
      for (var i = 0; i < basicList.size(); ++i) {
        var itemName = basicList.get(i);
        var fieldValue = fields[6 + i].equals("Yes");
        getCounter("Survey basic " + itemName + " checked").increment(fieldValue);
        if (fieldValue) {
          summary.surveyBasicItems.add(itemName);
        }
        getCounter("Survey Basic Items").increment(itemName, fieldValue ? 1 : 0);
      }

      /*
       * Additional fields start in column R or 17
       */
      for (var i = 0; i < additionalList.size(); ++i) {
        var itemName = additionalList.get(i);
        var fieldValue = fields[17 + i].equals("Yes");
        getCounter("Survey additional " + itemName + " checked").increment(fieldValue);
        if (fieldValue) {
          summary.surveyAdditionalItems.add(itemName);
        }
        getCounter("Survey Additional Items").increment(itemName, fieldValue ? 1 : 0);
      }
    }

    summary.messageIds.add("survey: " + m.messageId);
    summary.surveyNAdditionalItems = summary.surveyAdditionalItems.size();
    summary.surveyNBasicItems = summary.surveyBasicItems.size();
    getCounter("Message Type").increment("SURVEY");
  }

  /**
   * check for
   *
   * @param value
   * @param quiz
   * @return
   */
  private String[] getFields(String value, PlainType plainType) {

    var name = plainType.toString();
    var listOfFields = ReadProcessor.readCsvStringIntoFieldsArray(value, ',', false, 0);
    count(sts.test(name + " CVS file number of rows should be #EV", 2, listOfFields.size()));
    if (listOfFields.size() == 2) {
      var headers = listOfFields.get(0);
      String[] refHeaders = null;
      if (plainType == PlainType.Quiz) {
        refHeaders = QUIZ_HEADERS.split(",");
      } else {
        refHeaders = SURVEY_HEADERS.split(",");
      }

      count(sts.test(name + " CSV file number of header columns should be #EV", refHeaders.length, headers.length));

      var n = Math.min(headers.length, refHeaders.length);
      for (var i = 0; i < n; ++i) {
        sts.test(name + " CSV header #" + (i + 1) + ", should be #EV", refHeaders[i], headers[i]);
      }

      var fields = listOfFields.get(1);
      count(sts.test(name + " CSV file number of data columns should be #EV", refHeaders.length, fields.length));

      if (fields.length != refHeaders.length) {
        fields = null;
      }
      return fields;
    } else {
      return null;
    }

  }

  private void handle_DyFiMessage(Summary summary, DyfiMessage m) {
    sts.setExplanationPrefix("(dyfi) ");

    var hasUSGSAddress = (m.toList + "," + m.ccList).toUpperCase().contains(REQUIRED_USGS_ADDRESS.toUpperCase());
    count(sts.test("DYFI To and/or CC addresses must contain " + REQUIRED_USGS_ADDRESS, hasUSGSAddress));
    count(sts.test("DYFI Event Type must be: EXERCISE", !m.isRealEvent));
    count(sts
        .test("DYFI Form Latitude and Longitude must be valid", m.formLocation.isValid(), m.formLocation.toString()));
    count(sts.test("DYFI Did you feel it? should be Yes", m.isFelt));

    final var responseMap = Map
        .of("", "Not specified", "no_action", "Took no action", "doorway", "Moved to doorway", "duck",
            "Dropped and covered", "ran_outside", "Ran Outside", "other", "Other");
    count(sts.test("DYFI Response should be #EV", responseMap.get("duck"), responseMap.get(m.response)));
    getCounter("DYFI Repsonse").increment(responseMap.get(m.response));

    try {
      var intensity = Integer.parseInt(m.intensity);
      count(sts.test("DYFI Intensity must be >= 5", intensity >= 5, m.intensity));
      getCounter("DYFI Intensity").increment(m.intensity);
      summary.dyfiIntensityAbove5 = intensity >= 5;
    } catch (Exception e) {
      count(sts.test("DYFI Intensity must be >= 5", false, m.intensity));
      summary.dyfiIntensityAbove5 = false;
    }

    // comments
    summary.dyfiRawComments = m.comments;
    if (summary.dyfiRawComments != null) {
      count(sts.test("DYFI comments should be present", true));
      var fields = summary.dyfiRawComments.split(","); // AFFILIATION, ORGANIZATION, MODE, BAND, COMMENTS

      if (fields.length >= 1) {
        summary.dyfiAffiliation = fields[0].strip();
        count(sts.testIfPresent("DYFI Comment field #1 (Affiliation) should be present", summary.dyfiAffiliation));
        getCounter("DYFI Comment Affiliation").increment(summary.dyfiAffiliation);
      } else {
        count(sts.testIfPresent("DYFI Comment field #1 (Affiliation) should be present", null));
      }

      if (fields.length >= 2) {
        summary.dyfiOrganization = fields[1].strip();
        count(sts.testIfPresent("DYFI Comment field #2 (Organization) should be present", summary.dyfiOrganization));
        getCounter("DYFI Comment Organization").increment(summary.dyfiOrganization);
      } else {
        count(sts.testIfPresent("DYFI Comment field #2 (Organization) should be present", null));
      }

      if (fields.length >= 3) {
        summary.dyfiMode = fields[2].strip();
        count(sts.testIfPresent("DYFI Comment field #3 (Mode) should be present", summary.dyfiMode));
        getCounter("DYFI Comment Mode").increment(summary.dyfiMode);
      } else {
        count(sts.testIfPresent("DYFI Comment field #3 (Mode) should be present", null));
      }

      if (fields.length >= 4) {
        summary.dyfiBand = fields[3].strip();
        count(sts.testIfPresent("DYFI Comment field #4 (Bamd) should be present", summary.dyfiBand));
        getCounter("DYFI Comment Band").increment(summary.dyfiBand);
      } else {
        count(sts.testIfPresent("DYFI Comment field #4 (Band) should be present", null));
      }

      if (fields.length >= 5) {
        var newList = new ArrayList<String>();
        for (var i = 4; i < fields.length; ++i) {
          newList.add(fields[i]);
        }
        summary.dyfiRemainingComments = String.join(", ", newList);
        count(sts.testIfPresent("DYFI Comment field #5 (Comments) should be present", summary.dyfiRemainingComments));
        getCounter("DYFI Comment Affiliation").increment(summary.dyfiRemainingComments);
      } else {
        count(sts.testIfPresent("DYFI Comment field #5 (Comments) should be present", null));
      }

    } else {
      count(sts.test("DYFI comments should be present", false));
    }

    getCounter("DYFI Form Version").increment(m.formVersion);

    // #MM update summary
    getCounter("Message Type").increment("DYFI");
    summary.dyfiMessage = m;
    summary.dyfiIsExercise = !m.isRealEvent;
    summary.dyfiIsFelt = m.isFelt;
    summary.dyfiIntensity = m.intensity;
    summary.dyfiResponse = m.response;
    summary.messageIds.add("dyfi: " + m.messageId);

    isPerfectMessage(m);
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("(summary) ");

    var summary = (Summary) summaryMap.get(sender); // #MM

    sts.testNotNull("DYFI message not received", summary.dyfiMessage);
    sts.testNotNull("Quiz message not received", summary.quizMessage);
    sts.testNotNull("Survey Message not received", summary.surveyMessage);

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM

    writeTable("perfectMessages.csv", perfectMessages);
    writeTable("quizes.csv", quizes);
    writeTable("surveys.csv", surveys);

    makeCharts();

    @SuppressWarnings("unchecked")
    var summaries = new ArrayList<Summary>((Collection<Summary>) (Object) summaryMap.values());
    makeMaps(summaries);
  }

  private void makeCharts() {
    var sb = new StringBuilder();
    var iterator = getCounter("Survey Basic Items").getAscendingEntryOrderIterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      sb.append("{name: \"" + entry.getKey() + "\", value: " + entry.getValue() + "},\n");
    }
    var basicData = sb.toString();

    sb = new StringBuilder();
    iterator = getCounter("Survey Additional Items").getAscendingEntryOrderIterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      sb.append("{name: \"" + entry.getKey() + "\", value: " + entry.getValue() + "},\n");
    }
    var additionalData = sb.toString();

    final var template = """
        <!DOCTYPE html>
        <html>
        <head>
          <script src="https://cdn.plot.ly/plotly-3.6.0.min.js" charset="utf-8"></script>
          <style>
            h1 { text-align: center; font-family: Arial, sans-serif; margin-top: 20px; margin-bottom: 30px; font-size: 32px; color: #333; }
          </style>
        </head>
        <body>

        <h1>Shakeout 2026 Survey Results</h1>

        <div id="basicPlot" style="width:800px;height:450px;"></div>
        <hr>
        <div id="additionalPlot" style="width:800px;height:450px;margin-top:40px;"></div>

        <script>
        const basicData = [
        #BASIC_DATA#
        ];

        const additionalData = [
        #ADDITIONAL_DATA#
        ];

        function renderHistogram(targetDiv, data, title, barColor) {
          const names  = data.map(d => d.name);
          const values = data.map(d => d.value);

          // --- Trace 1: actual bars ---
          const bars = {
            x: values,
            y: names,
            type: "bar",
            orientation: "h",
            marker: { color: barColor },
            text: values,
            textposition: "outside",
            showlegend: false,
            textfont: { color: "black", size: 14 }
          };

          // --- Trace 2: transparent overlay for inside names ---
          const insideNames = {
            x: values.map(v => v / 2),   // midpoint of each bar
            y: names,
            type: "scatter",
            mode: "text",
            text: names,
            textposition: "middle center",
            hoverinfo: "skip",
            showlegend: false,
            textfont: { color: "white", size: 14 }
          };

          const layout = {
            title: { text: title, font: { size: 28 } },
            margin: { l: 20, r: 60, t: 60, b: 40 },
            yaxis: { showticklabels: false, showgrid: false },
            xaxis: { title: "Count" },
            barmode: "overlay"
          };

          Plotly.newPlot(targetDiv, [bars, insideNames], layout);
        }

        renderHistogram("basicPlot", basicData, "Basic Data", "steelblue");
        renderHistogram("additionalPlot", additionalData, "Additional Data", "seagreen");
        </script>
        </body>
        </html>
                """;
    var content = new String(template);
    content = content.replace("#BASIC_DATA#", basicData);
    content = content.replace("#ADDITIONAL_DATA#", additionalData);

    WriteProcessor.writeString(content, Path.of(publishedPathName, dateString + "-survey-histograms.html"));
  }

  private record QuizEntry(String[] values) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (QuizEntry) other;
      return values[1].compareTo(o.values[1]);
    }

    @Override
    public String[] getHeaders() {
      return QUIZ_HEADERS.split(",");
    }

    @Override
    public String[] getValues() {
      return values;
    }

  }

  private record SurveyEntry(String[] values) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (SurveyEntry) other;
      return values[1].compareTo(o.values[1]);
    }

    @Override
    public String[] getHeaders() {
      return SURVEY_HEADERS.split(",");
    }

    @Override
    public String[] getValues() {
      return values;
    }

  }

  private void makeMaps(List<Summary> summaries) {
    makeDyfiGroupCountMap(summaries, true);
    makeSqueezedDyfiGroupCountMap(summaries, true);

    final var green = IMapService.rgbMap.get("green");
    final var yellow = IMapService.rgbMap.get("yellow");
    final var red = IMapService.rgbMap.get("red");
    final var black = IMapService.rgbMap.get("black");

    Function<Summary, String> popup = (s -> s.getMessageSummary());

    makeMakeViaLegends(summaries, "Message Counts", "messageCount", publishedPath, List
        .of(//
            new Legend("messages: 3", green, (s -> s.getMessageCount() == 3), popup), //
            new Legend("messages: 2", yellow, (s -> s.getMessageCount() == 2), popup), //
            new Legend("messages: 1", red, (s -> s.getMessageCount() == 1), popup) //
        ));

    popup = (s -> "<b>" + s.from + "</b><hr>"
        + ((s.dyfiMessage == null) ? "no DYFI message" : "DYFI Is Exercise: " + s.dyfiIsExercise));
    makeMakeViaLegends(summaries, "DYFI Is Exercise Counts", "DYFI_IsExercise", publishedPath, List
        .of(//
            new Legend("Is Exercise", green, (s -> s.dyfiMessage != null && s.dyfiIsExercise), popup), //
            new Legend("Real Event", red, (s -> s.dyfiMessage != null && !s.dyfiIsExercise), popup), //
            new Legend("No DYFI Message", black, (s -> s.dyfiMessage == null), popup)));

    popup = (s -> "<b>" + s.from + "</b><hr>"
        + ((s.dyfiMessage == null) ? "no DYFI message" : "DYFI Is Felt: " + s.dyfiIsFelt));
    makeMakeViaLegends(summaries, "DYFI Is Felt Counts", "DYFI_IsFelt", publishedPath, List
        .of(//
            new Legend("Is Felt", green, (s -> s.dyfiMessage != null && s.dyfiIsFelt), popup), //
            new Legend("Not Felt", red, (s -> s.dyfiMessage != null && !s.dyfiIsFelt), popup), //
            new Legend("No DYFI Message", black, (s -> s.dyfiMessage == null), popup)));

    popup = (s -> "<b>" + s.from + "</b><hr>" + "DYFI Intensity: " + ((s.dyfiMessage == null) ? "no DYFI message"
        : ((s.dyfiMessage.intensity == null) ? "unknown" : s.dyfiMessage.intensity)));
    makeMakeViaLegends(summaries, "DYFI Intensity >= 5 Counts", "DYFI_IntensityIsEnough", publishedPath, List
        .of(//
            new Legend("Intensity >= 5", green,
                (s -> s.dyfiMessage != null && s.dyfiMessage.intensity != null
                    && Integer.valueOf(s.dyfiMessage.intensity) >= 5),
                popup), //
            new Legend("Intensity unknown", yellow, (s -> s.dyfiMessage != null && s.dyfiMessage.intensity == null),
                popup), //
            new Legend("Intensity < 5", red,
                (s -> s.dyfiMessage != null && s.dyfiMessage.intensity != null
                    && Integer.valueOf(s.dyfiMessage.intensity) < 5),
                popup), //
            new Legend("No DYFI Message", black, (s -> s.dyfiMessage == null), popup)));

  }

  private void makeDyfiGroupCountMap(List<Summary> summaries, boolean doPublish) {
    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);
    var desiredLayers = 10;
    var minimumGroupSize = 2;

    var groupCallListMap = new HashMap<String, List<String>>();
    for (var summary : summaries) {
      if (summary.dyfiMessage == null || summary.dyfiAffiliation == null) {
        continue;
      }
      var group = summary.dyfiAffiliation;
      group = group.trim().replaceAll("\n", "").replaceAll("\"", "");
      var list = groupCallListMap.getOrDefault(group, new ArrayList<String>());
      var call = summary.from;
      list.add(call);
      groupCallListMap.put(group, list);
    }

    var groupSizeGroupListMap = new TreeMap<Integer, List<String>>();
    var groups = new ArrayList<String>(groupCallListMap.keySet());
    for (var group : groups) {
      var callList = groupCallListMap.get(group);
      var groupSize = callList.size();
      if (groupSize < minimumGroupSize) {
        continue;
      }
      var groupList = groupSizeGroupListMap.getOrDefault(groupSize, new ArrayList<String>());
      groupList.add(group);
      groupSizeGroupListMap.put(groupSize, groupList);
    }

    groups.clear();
    for (var groupSize : groupSizeGroupListMap.descendingKeySet()) {
      var groupList = groupSizeGroupListMap.get(groupSize);
      groups.addAll(groupList);
      if (groups.size() >= desiredLayers) {
        break;
      }
    }

    var rng = new Random(2025);
    var colorBag = new RenewableBag<>(IMapService.etoColorMap.values(), rng);
    var groupColorMap = new HashMap<String, String>();
    for (var group : groups) {
      var color = colorBag.next();
      groupColorMap.put(group, color);
    }

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    for (var summary : summaries) {
      if (summary.dyfiMessage == null || summary.dyfiAffiliation == null) {
        continue;
      }
      var group = summary.dyfiAffiliation;
      group = group.trim().replaceAll("\n", "").replaceAll("\"", "");
      var callList = groupCallListMap.get(group);
      if (callList == null || callList.size() < minimumGroupSize) {
        continue;
      }
      var location = summary.location;
      var color = groupColorMap.get(group);
      var prefix = "<b>" + summary.from + "</b><hr>";
      var content = prefix //
          + "Group Name: " + group + "\n" //
          + "Group Size: " + callList.size() + "\n";
      var mapEntry = new MapEntry(summary.from, null, location, content, color);
      mapEntries.add(mapEntry);
    }

    var layers = new ArrayList<MapLayer>(groups.size());
    for (var group : groups) {
      var callList = groupCallListMap.get(group);
      if (callList == null) {
        continue;
      }
      var count = callList.size();
      var layerName = "group: " + group + ", size: " + count;
      var color = groupColorMap.get(group);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    if (layers.size() == 0) {
      logger.warn("### DYFI Group Count layers: size 0. No map produced");
      return;
    }

    var legendTitle = "DYFI Group Counts (" + mapEntries.size() + " messages, min group size: " + minimumGroupSize
        + ")";
    var path = doPublish ? publishedPath : outputPath;
    var context = new MapContext(path, //
        dateString + "-map-DYFI GroupCounts", // file name
        dateString + " Group Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  /**
   * any group smaller than minGroupSize gets lumped into Other
   *
   * @param summaries
   */
  private void makeSqueezedDyfiGroupCountMap(List<Summary> summaries, boolean doPublish) {
    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);
    var desiredLayers = 10;
    var minimumGroupSize = 2;

    var groupCallListMap = new HashMap<String, List<String>>();
    for (var summary : summaries) {
      if (summary.dyfiMessage == null || summary.dyfiAffiliation == null) {
        continue;
      }
      var group = summary.dyfiAffiliation;
      group = group.trim().replaceAll("\n", "").replaceAll("\"", "");
      var list = groupCallListMap.getOrDefault(group, new ArrayList<String>());
      var call = summary.from;
      list.add(call);
      groupCallListMap.put(group, list);
    }

    final var otherGroup = "other";
    if (groupCallListMap.containsKey(otherGroup)) {
      throw new RuntimeException("Need a new squeezed name");
    }

    var squeezedCallList = new ArrayList<String>();
    var groupSizeGroupListMap = new TreeMap<Integer, List<String>>();
    var groups = new ArrayList<String>(groupCallListMap.keySet());
    for (var group : groups) {
      var callList = groupCallListMap.get(group);
      var groupSize = callList.size();
      if (groupSize < minimumGroupSize) {
        squeezedCallList.addAll(callList);
        groupCallListMap.remove(group);
        continue;
      }
      var groupList = groupSizeGroupListMap.getOrDefault(groupSize, new ArrayList<String>());
      groupList.add(group);
      groupSizeGroupListMap.put(groupSize, groupList);
    }
    groupCallListMap.put(otherGroup, squeezedCallList);
    groupSizeGroupListMap.put(squeezedCallList.size(), squeezedCallList);

    groups.clear();
    for (var groupSize : groupSizeGroupListMap.descendingKeySet()) {
      var groupList = groupSizeGroupListMap.get(groupSize);
      groups.addAll(groupList);
      if (groups.size() >= desiredLayers) {
        break;
      }
    }

    var rng = new Random(2025);
    var colorBag = new RenewableBag<>(IMapService.etoColorMap.values(), rng);
    var groupColorMap = new HashMap<String, String>();
    for (var group : groupCallListMap.keySet()) {
      var color = colorBag.next();
      groupColorMap.put(group, color);
    }

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    for (var summary : summaries) {
      if (summary.dyfiMessage == null || summary.dyfiAffiliation == null) {
        continue;
      }
      var group = summary.dyfiAffiliation;
      group = group.trim().replaceAll("\n", "").replaceAll("\"", "");
      var callList = groupCallListMap.get(group);
      var groupForColor = group;
      if (callList == null || callList.size() < minimumGroupSize) {
        groupForColor = otherGroup;
      }

      var location = summary.location;
      var color = groupColorMap.get(groupForColor);
      var prefix = "<b>" + summary.from + "</b><hr>";

      var groupSize = callList == null ? 1 : callList.size();
      var content = prefix //
          + "Group Name: " + group + "\n" //
          + "Group Size: " + groupSize + "\n";
      var mapEntry = new MapEntry(summary.from, null, location, content, color);
      mapEntries.add(mapEntry);
    }

    var xgroups = new ArrayList<String>(
        groupCallListMap.keySet().stream().filter(g -> groupCallListMap.get(g) != null).toList());
    Collections.sort(xgroups, (g1, g2) -> groupCallListMap.get(g2).size() - groupCallListMap.get(g1).size());
    var layers = new ArrayList<MapLayer>(groups.size());
    for (var group : xgroups) {
      var callList = groupCallListMap.get(group);
      var count = callList.size();
      var layerName = "group: " + group + ", size: " + count;
      var color = groupColorMap.get(group);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var legendTitle = "DYFI Group Counts (" + mapEntries.size() + " messages, min group size: " + minimumGroupSize
        + ")";

    var path = doPublish ? publishedPath : outputPath;
    var context = new MapContext(path, //
        dateString + "-map-DYFI Squeezed GroupCounts", // file name
        dateString + " Group Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  record Legend(String label, String color, Predicate<Summary> predicate, Function<Summary, String> popupGenerator) {
  };

  private void makeMakeViaLegends(List<Summary> summaries, String legendTitle, String fileName, Path path,
      List<Legend> legends) {
    var colorCountMap = new HashMap<String, Integer>();

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    for (var s : summaries) {
      var found = false;
      for (var legend : legends) {
        if (legend.predicate.test(s)) {
          var count = colorCountMap.getOrDefault(legend.color, Integer.valueOf(0));
          ++count;
          colorCountMap.put(legend.color, count);
          var mapEntry = new MapEntry(s.from, s.to, s.location, legend.popupGenerator.apply(s), legend.color);
          mapEntries.add(mapEntry);
          found = true;
          break;
        } // endif predicate matches
      } // end loop over legend entries
      if (!found) {
        logger.debug("not found");
      }
    } // end loop of mapEntries

    var layers = new ArrayList<MapLayer>();
    for (var legend : legends) {
      var color = legend.color;
      var label = legend.label;
      var count = colorCountMap.getOrDefault(color, Integer.valueOf(0));
      layers.add(new MapLayer(label + ", count: " + count, color));
    }

    legendTitle = "ShakeOut 2026 " + legendTitle + " (" + summaries.size() + " total)";
    var context = new MapContext(path, //
        dateString + "-map-" + fileName, // file name
        dateString + legendTitle, // map title
        null, legendTitle, layers, mapEntries);
    var mapService = new MapService(cm, mm);
    mapService.makeMap(context);
  }
}
