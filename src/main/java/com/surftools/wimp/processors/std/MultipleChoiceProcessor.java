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

package com.surftools.wimp.processors.std;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * a more modern MultipleChoiceProcessor
 *
 * @author bobt
 *
 */
public class MultipleChoiceProcessor extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(MultipleChoiceProcessor.class);

  protected Set<String> validResponses = new HashSet<>();
  protected Set<String> correctResponses = new HashSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    outboundMessageExtraContent = OB_DISCLAIMER;

    var messageTypeString = cm.getAsString(Key.MULTIPLE_CHOICE_MESSAGE_TYPE);
    messageType = MessageType.fromString(messageTypeString);

    var validResponsesString = cm.getAsString(Key.MULTIPLE_CHOICE_VALID_RESPONSES);
    for (var field : validResponsesString.split(",")) {
      validResponses.add(toKey(field));
    }

    var correctResponsesString = cm.getAsString(Key.MULTIPLE_CHOICE_CORRECT_RESPONSES);
    for (var field : correctResponsesString.split(",")) {
      correctResponses.add(toKey(field));
    }
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {

    var response = getResponse(message);
    if (response == null) {
      sts.test("Multiple Choice response correct", false, "response is null");
      getCounter("Multiple Choice response class").increment("(null)");
    } else {
      var keyedResponse = toKey(response);
      var isValid = validResponses.contains(keyedResponse);
      var isCorrect = correctResponses.contains(keyedResponse);
      if (isCorrect) {
        sts.test("Multiple Choice response correct", true, response);
        getCounter("Multiple Choice response class").increment("correct");
      } else if (isValid) {
        sts.test("Multiple Choice response correct", false, response, "valid, but incorrect");
        getCounter("Multiple Choice response class").increment("valid but incorrect");
      } else {
        sts.test("Multiple Choice response correct", false, response, "invalid and incorrect");
        getCounter("Multiple Choice response class").increment("invalid");
      }
    }
  }

  protected String getResponse(ExportedMessage message) {
    return message.getMultiMessageComment();
  }

}