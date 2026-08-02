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

  protected static final String REQUIRED_USGS_ADDRESS = "dyfi_reports_automated@usgs.gov";

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
    count(sts.test("Plain attachment count should be #EV", "1", String.valueOf(attachmentCount)));

    for (var attachmentName : m.attachments.keySet()) {
      var value = new String(m.attachments.get(attachmentName));
      if (attachmentName.toUpperCase().contains("QUIZ ANSWERS")) {
        handle_quiz(summary, m, attachmentName, value);
      } else if (attachmentName.toUpperCase().contains("PREPAREDNESS SURVEY")) {
        handle_survey(summary, m, attachmentName, value);
      }
    }
  }

  private void handle_quiz(Summary summary, PlainMessage m, String attachmentName, String value) {
    sts.setExplanationPrefix("(quiz) ");
    var listOfFields = ReadProcessor.readCsvStringIntoFieldsArray(value, ',', false, 1);
    var fields = listOfFields.get(0);

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

    getCounter("Message Type").increment("QUIZ");

    summary.messageIds.add("quiz: " + m.messageId);
    summary.quizNCorrect = nCorrect;
    getCounter("Quiz #Correct").increment(nCorrect);
    summary.quizNAnswered = nAnswered;
    getCounter("Quiz #Answered").increment(nAnswered);
    summary.quizAnswers = String.join(",", answers);
  }

  private void handle_survey(Summary summary, PlainMessage m, String attachmentName, String value) {
    sts.setExplanationPrefix("(quiz) ");
    var listOfFields = ReadProcessor.readCsvStringIntoFieldsArray(value, ',', false, 1);
    var fields = listOfFields.get(0);

    // skip if we already have a quiz
    if (summary.surveyMessage != null) {
      return;
    }
    summary.surveyMessage = m;

    var basicList = List
        .of("water/food", "cell-charger", "weather radio", "flashlight", "first aid", "whistle", "dust mask",
            "sanitation", "wrench", "can opener", "maps");
    var additionalList = List
        .of("medications/glasses", "infant formual and diapers", "pet food", "family documents", "cash",
            "reference material", "sleeping bags", "_clothing", "fire extinquisher", "clothing", "matches",
            "feminine/hygiene", "mess kits", "paper and pencil", "books/games");

    /*
     * Basic fields start in column G or 6
     */
    for (var i = 0; i < basicList.size(); ++i) {
      var itemName = basicList.get(i);
      var fieldValue = fields[6 + i].equals("Yes");
      getCounter("Survey basic " + itemName).increment(fieldValue);
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
      getCounter("Survey additional " + itemName).increment(fieldValue);
      if (fieldValue) {
        summary.surveyAdditionalItems.add(itemName);
      }
    }

    summary.messageIds.add("survey: " + m.messageId);
    summary.surveyNAdditionalItems = summary.surveyAdditionalItems.size();
    summary.surveyNBasicItems = summary.surveyBasicItems.size();
    getCounter("Message Type").increment("SURVEY");
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

  }
}
