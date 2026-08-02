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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.DyfiMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
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
                  "Quiz #Correct", "Quiz #Correct", "Quiz Answers", //
                  "Survey Basis Items", "Survey Additional Items", "Survey #Basic", "Survey #Additional" //
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
                  s(quizNCorrect), s(quizNAnswered), quizAnswers, //
                  String.join(",", surveyBasicItems), String.join(",", surveyAdditionalItems), s(surveyNBasicItems),
                  s(surveyNAdditionalItems)//
              }));

      return list.toArray(new String[0]);
    };
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
        handle_quiz(summary, m, attachmentName, value);
      } else if (attachmentName.toUpperCase().contains("PREPAREDNESS SURVEY")) {
        handle_survey(summary, m, attachmentName, value);
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

      count(sts.test(name + " CSV file number of columns should be #EV", refHeaders.length, headers.length));

      var n = Math.min(headers.length, refHeaders.length);
      for (var i = 0; i < n; ++i) {
        sts.test(name + " CSV header #" + i + ", should be #EV", refHeaders[i], headers[i]);
      }

      var fields = listOfFields.get(1);
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
    sts.testNotNull("SurveyMessage not received", summary.surveyMessage);

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    super.postProcess();// #MM

    writeTable("perfectMessages.csv", perfectMessages);
    writeTable("quizes.csv", quizes);
    writeTable("surveys.csv", surveys);
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
}
