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

import static java.time.temporal.ChronoUnit.DAYS;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor.BaseSummary;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;
import com.surftools.wimp.service.simpleTestService.TestResult;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * parent of our two "primary" standard processors, SingleMessageFeedbackProcessor, MultiMessageFeedbackProcessor
 */
public abstract class AbstractBaseFeedbackProcessor extends AbstractBaseProcessor {
  protected static Logger logger;
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  protected LocalDateTime windowOpenDT = null;
  protected LocalDateTime windowCloseDT = null;

  protected SimpleTestService sts = new SimpleTestService();
  protected ExportedMessage message;
  protected String sender;
  protected Set<MessageType> messageTypesRequiringSecondaryAddress = new HashSet<>();
  protected Set<String> secondaryDestinations = new LinkedHashSet<>();

  public int ppMessageCount = 0;
  public int ppParticipantCount = 0;
  public int ppParticipantCorrectCount = 0;

  public Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

  protected String outboundMessageExtraContent = FeedbackProcessor.OB_DISCLAIMER;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);
    logger = _logger;

    var windowOpenString = cm.getAsString(Key.EXERCISE_WINDOW_OPEN);
    if (windowOpenString != null) {
      windowOpenDT = LocalDateTime.from(DTF.parse(windowOpenString));
    }

    var windowCloseString = cm.getAsString(Key.EXERCISE_WINDOW_CLOSE);
    if (windowCloseString != null) {
      windowCloseDT = LocalDateTime.from(DTF.parse(windowCloseString));
    }

    var secondaryDestinationsString = cm.getAsString(Key.SECONDARY_DESTINATIONS);
    if (secondaryDestinationsString != null) {
      var fields = secondaryDestinationsString.split(",");
      for (var field : fields) {
        secondaryDestinations.add(field.toUpperCase());
      }
    }
  }

  /**
   * derived class really must implement this
   *
   * @param sender
   */
  protected abstract void endProcessingForSender(String sender);

  /**
   * before all messageTypes for a given sender, a chance to look at cross-message relations
   */
  protected void beforeProcessingForSender(String sender) {
    ++ppParticipantCount;
    sts.reset(sender);
  }

  /**
   * after all messageTypes for a given sender, a chance to look at cross-message relations
   */

  protected void beginCommonProcessing(ExportedMessage message) {

    if (secondaryDestinations.size() > 0) {
      if (messageTypesRequiringSecondaryAddress.size() == 0
          || messageTypesRequiringSecondaryAddress.contains(message.getMessageType())) {
        var addressList = (message.toList + "," + message.ccList).toUpperCase();
        for (var ev : secondaryDestinations) {
          count(sts.test("To and/or CC addresses should contain " + ev, addressList.contains(ev)));
        }
      }
    }

    if (windowOpenDT != null && windowCloseDT != null) {
      sts.testOnOrAfter("Message should be posted on or after #EV", windowOpenDT, message.msgDateTime, DTF);
      sts.testOnOrBefore("Message should be posted on or before #EV", windowCloseDT, message.msgDateTime, DTF);

      var daysAfterOpen = DAYS.between(windowOpenDT, message.msgDateTime);
      getCounter("Message sent days after window opens").increment(daysAfterOpen);
    }
  }

  protected void endCommonProcessing(ExportedMessage message) {

  }

  /**
   * get Counter for label, create if needed
   *
   * @param label
   * @return
   */
  protected Counter getCounter(String label) {
    var counter = counterMap.get(label);
    if (counter == null) {
      counter = new Counter(label);
      counterMap.put(label, counter);
    }

    return counter;
  }

  /**
   * get a TestResult!
   *
   * @param testResult
   */
  protected void count(TestResult testResult) {
    var label = testResult.key();
    var result = testResult.ok();
    getCounter(label).increment(result ? "correct" : "incorrect");
  }

  /**
   * this is the exercise-specific method to be implemented
   *
   * @param message
   */
  protected abstract void specificProcessing(ExportedMessage message);

  protected String makeOutboundMessageFeedback(BaseSummary summary) {
    var outboundMessageFeedback = (summary.explanations.size() == 0) ? "Perfect messages!"
        : String.join("\n", summary.explanations);
    return outboundMessageFeedback;
  }

  protected String makeOutboundMessageSubject(Object object) {
    return cm.getAsString(Key.OUTBOUND_MESSAGE_SUBJECT);
  }

  @Override
  public void postProcess() {

  }

  protected LocalDate parseDate(String s, List<DateTimeFormatter> formatters) {
    if (s == null || formatters == null || formatters.size() == 0) {
      return null;
    }

    s = s.trim().replaceAll(" +", " ");
    LocalDate dt = null;
    for (var f : formatters) {
      try {
        dt = LocalDate.from(f.parse(s.trim()));
        break;
      } catch (Exception e) {
        ;
      }
    }

    return dt;
  }

  protected LocalDateTime parseDateTime(String s, List<DateTimeFormatter> formatters) {
    if (s == null || formatters == null || formatters.size() == 0) {
      return null;
    }

    s = s.trim().replaceAll(" +", " ");
    LocalDateTime dt = null;
    for (var f : formatters) {
      try {
        dt = LocalDateTime.from(f.parse(s.trim()));
        break;
      } catch (Exception e) {
        ;
      }
    }

    return dt;
  }

  protected boolean isNull(String s) {
    return s == null || s.isEmpty();
  }

  protected String mId(ExportedMessage m) {
    return m == null ? "" : m.messageId;
  }

  protected String toKey(String s) {
    if (s == null) {
      return "";
    }
    return s.toLowerCase().replaceAll("[^A-Za-z0-9]", "");
  }

  protected boolean isFull(String s) {
    return s != null && !s.isEmpty();
  }

  protected static String s(int i) {
    return String.valueOf(i);
  }

  protected static String s(boolean b) {
    return String.valueOf(b);
  }
}
