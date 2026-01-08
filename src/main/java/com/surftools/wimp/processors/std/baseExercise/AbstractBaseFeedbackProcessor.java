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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
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
  protected Set<String> unexpectedDestinations = new LinkedHashSet<>();

  public int ppMessageCount = 0;
  public int ppParticipantCount = 0;
  public int ppParticipantCorrectCount = 0;

  public Map<String, Counter> counterMap = new LinkedHashMap<String, Counter>();

  protected String outboundMessageExtraContent = FeedbackProcessor.OB_DISCLAIMER;

  protected final int FEEDBACK_MAP_N_LAYERS = 6;
  protected Map<Integer, String> gradientMap;

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
        secondaryDestinations.add(field.toUpperCase().trim());
      }
    }

    var unexpectedDestinationsString = cm.getAsString(Key.UNEXPECTED_DESTINATIONS);
    if (unexpectedDestinationsString != null) {
      var fields = unexpectedDestinationsString.split(",");
      for (var field : fields) {
        unexpectedDestinations.add(field.toUpperCase().trim());
      }
    }

    gradientMap = new MapService(cm, mm).makeGradientMap(120, 0, FEEDBACK_MAP_N_LAYERS);
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

    var addressList = (message.toList + "," + message.ccList).toUpperCase();

    if (secondaryDestinations.size() > 0) {
      if (messageTypesRequiringSecondaryAddress.size() == 0
          || messageTypesRequiringSecondaryAddress.contains(message.getMessageType())) {
        for (var ev : secondaryDestinations) {
          count(sts.test("To and/or CC addresses should contain " + ev, addressList.contains(ev)));
        }
      }
    }

    if (unexpectedDestinations.size() > 0) {
      for (var ev : unexpectedDestinations) {
        count(sts.test("To and/or CC addresses should NOT contain " + ev, !addressList.contains(ev)));
      }
    }

  }

  protected void endCommonProcessing(ExportedMessage message) {
    if (windowOpenDT != null && windowCloseDT != null) {
      sts.testOnOrAfter("Message should be posted on or after #EV", windowOpenDT, message.msgDateTime, DTF);
      sts.testOnOrBefore("Message should be posted on or before #EV", windowCloseDT, message.msgDateTime, DTF);

      var daysAfterOpen = DAYS.between(windowOpenDT, message.msgDateTime);
      getCounter("Message sent days after window opens").increment(daysAfterOpen);
    }

    sts.setExplanationPrefix("");
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

  protected void makeFeedbackMap(Map<Integer, Integer> truncatedCountMap, List<MapEntry> mapEntries) {
    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);
    var nLayers = FEEDBACK_MAP_N_LAYERS;

    var layers = new ArrayList<MapLayer>();
    var countLayerNameMap = new HashMap<Integer, String>();
    for (var i = 0; i < nLayers; ++i) {
      var value = String.valueOf(i);
      var count = truncatedCountMap.getOrDefault(i, Integer.valueOf(0));
      if (i == nLayers - 1) {
        value = i + " or more";
      }
      var layerName = "value: " + value + ", count: " + count;
      countLayerNameMap.put(i, layerName);

      var color = gradientMap.get(i);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var legendTitle = dateString + " Feedback Counts (" + mapEntries.size() + " total)";
    var context = new MapContext(outputPath, //
        dateString + "-map-feedbackCount", // file name
        dateString + " Feedback Counts", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
  }

  protected void makeClearinghouseMap(Map<String, Integer> clearinghouseCountMap, Map<String, String> colorMap,
      List<MapEntry> mapEntries) {
    var dateString = cm.getAsString(Key.EXERCISE_DATE);
    var mapService = new MapService(cm, mm);

    var names = new ArrayList<String>(clearinghouseCountMap.keySet());
    Collections.sort(names);
    var layers = new ArrayList<MapLayer>();
    for (var name : names) {
      var count = clearinghouseCountMap.getOrDefault(name, Integer.valueOf(0));
      var layerName = name + ": " + count + " participants";
      var color = colorMap.get(name);
      var layer = new MapLayer(layerName, color);
      layers.add(layer);
    }

    var legendTitle = dateString + " By Clearinghouse (" + mapEntries.size() + " total)";
    var context = new MapContext(outputPath, //
        dateString + "-map-byClearinghouse", // file name
        dateString + " By Clearinghouse", // map title
        null, legendTitle, layers, mapEntries);
    mapService.makeMap(context);
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
