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

package com.surftools.wimp.processors.std.baseExercise;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * for simple, multiple choice exercises
 *
 * @author bobt
 *
 */
public class MultipleChoiceProcessor extends AbstractBaseProcessor {
  private final Logger logger = LoggerFactory.getLogger(MultipleChoiceProcessor.class);

  private static final Character[] STOP_CHARS_ARRAY = new Character[] { '.', ',', ';', ':' };

  final Set<Character> STOP_CHARS = new HashSet<Character>(Arrays.asList(STOP_CHARS_ARRAY));

  private boolean doDequote;
  private boolean doStopChars;
  private boolean doToUpper;
  private boolean doTrim;

  private MessageType messageType;

  private Set<String> validResponseSet = new HashSet<>();
  private Set<String> correctResponseSet = new HashSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var messageTypeString = cm.getAsString(Key.MULIIPLE_CHOICE_MESSAGE_TYPE);
    messageType = MessageType.fromString(messageTypeString);
    if (messageType == null) {
      throw new RuntimeException("Could not get message type from: " + messageTypeString);
    }

    doToUpper = cm.getAsBoolean(Key.MULTIPLE_CHOICE_FORCE_CASE, Boolean.TRUE);
    doDequote = cm.getAsBoolean(Key.MULITPLE_CHOICE_ALLOW_QUOTES, Boolean.FALSE);
    doStopChars = cm.getAsBoolean(Key.MULTIPLE_CHOICE_ALLOW_STOP_CHARS, Boolean.FALSE);
    doTrim = cm.getAsBoolean(Key.MULTIPLE_CHOICE_ALLOW_TRIM, Boolean.FALSE);

    var validResponseString = cm.getAsString(Key.MULTIPLE_CHOICE_VALID_RESPONSES);
    var fields = validResponseString.split(",");
    for (var field : fields) {
      if (doToUpper) {
        validResponseSet.add(field.trim().toUpperCase());
      } else {
        validResponseSet.add(field.trim());
      }
    }

    var correctResponseString = cm.getAsString(Key.MULTIPLE_CHOICE_CORRECT_RESPONSES);
    fields = correctResponseString.split(",");
    for (var field : fields) {
      if (doToUpper) {
        correctResponseSet.add(field.trim().toUpperCase());
      } else {
        correctResponseSet.add(field.trim());
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    var messages = mm.getMessagesForType(messageType);

    int ppCount = 0;
    int ppNoResponseCount = 0;
    int ppAnyResponseCount = 0;
    int ppValidCount = 0;
    int ppInvalidCount = 0;
    int ppCorrectCount = 0;
    int ppIncorrectCount = 0;

    Counter ppScoreCounter = new Counter();
    Counter ppAllCounter = new Counter();
    Counter ppInvalidCounter = new Counter();
    Counter ppValidCounter = new Counter();
    Counter ppCorrectCounter = new Counter();
    Counter ppIncorrectCounter = new Counter();

    var results = new ArrayList<IWritableTable>();

    if (messages != null) {
      for (var message : messages) {
        if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
          logger.debug("messageId: " + message.messageId + ", from: " + message.from);
        }

        if (message.getMessageType() != messageType) {
          continue;
        }

        var points = 0;
        var explanation = "";
        ++ppCount;

        var response = message.getMultiMessageComment();
        ppAllCounter.incrementNullSafe(response);

        if (response == null) {
          points = 0;
          explanation = "No gradable response present";
          ++ppNoResponseCount;
        } else {
          response = reformatResponse(response);
          ++ppAnyResponseCount;

          if (validResponseSet.contains(response)) {
            points = 50;
            ++ppValidCount;
            ppValidCounter.increment(response);

            if (correctResponseSet.contains(response)) {
              points = 100;
              ++ppCorrectCount;
              ppCorrectCounter.increment(response);
            } else {
              ++ppIncorrectCount;
              ppIncorrectCounter.increment(response);
              var isAre = (correctResponseSet.size() == 1) ? "response is: " : "responses are: ";
              explanation = "Response of " + response + " incorrect. Correct " + isAre
                  + String.join(",", correctResponseSet);
            }
          } else {
            ++ppInvalidCount;
            ppInvalidCounter.increment(response);
            explanation = "Response of " + response + " not valid. Valid responses are: "
                + String.join(",", validResponseSet);
          }
        } // endif not not response

        points = Math.min(100, points);
        points = Math.max(0, points);

        var grade = String.valueOf(points);
        ppScoreCounter.increment(points);
        explanation = (points == 100) ? "Perfect Score!" : explanation;
        var result = new GradedResult(message, grade, explanation);
        results.add(result);
      } // end loop over messages
    } else {
      logger.warn("no " + messageType.toString() + " messages found");
    } // end if messages != null

    var sb = new StringBuilder();
    sb.append("\n\nMultiple Choice results:\n");
    sb.append("Valid responses: " + String.join(",", validResponseSet) + "\n");
    sb.append("Correct responses: " + String.join(",", correctResponseSet) + "\n");
    sb.append("Force to UPPER CASE: " + doToUpper + "\n");
    sb.append("Dequote: " + doDequote + "\n");
    sb.append("Ignore stop characters: " + doStopChars + "\n\n");

    sb.append(formatPP("  Any response present", ppAnyResponseCount, ppCount));
    sb.append(formatPP("  No response present", ppNoResponseCount, ppCount));
    sb.append(formatPP("  Valid response", ppValidCount, ppCount));
    sb.append(formatPP("  Invalid response", ppInvalidCount, ppCount));
    sb.append(formatPP("  Correct response", ppCorrectCount, ppCount));
    sb.append(formatPP("  Incorrect response", ppIncorrectCount, ppCount));

    sb.append("\nScores: \n" + formatCounter(ppScoreCounter.getDescendingKeyIterator(), "score", "count"));

    sb.append("\nValid Responses: \n" + formatCounter(ppValidCounter.getAscendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());

    writeTable("multipleChoice-" + messageType.toString() + ".csv", results);
  }

  private String reformatResponse(String response) {
    response = (doTrim) ? response.trim() : response;

    response = (doDequote && response.startsWith("\"") && response.endsWith("\""))
        ? response.substring(1, response.length() - 1)
        : response;

    response = (doToUpper) ? response.toUpperCase() : response;

    response = (doStopChars && response.length() == 2 && STOP_CHARS.contains(response.charAt(1)))
        ? response.substring(0, 1)
        : response;

    response = (doTrim) ? response.trim() : response;
    return response;
  }

}
