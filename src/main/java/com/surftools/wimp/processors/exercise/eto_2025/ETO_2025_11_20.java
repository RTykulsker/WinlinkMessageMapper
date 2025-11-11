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

package com.surftools.wimp.processors.exercise.eto_2025;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RejectionMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * a plain text, Winlink Template questionnaire about Practice; but with no formal messageType
 *
 * @author bobt
 *
 */
public class ETO_2025_11_20 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_11_20.class);
  private List<Questionnaire> questionnaires = new ArrayList<>();
  private List<RejectionMessage> rejects = new ArrayList<>();

  private record Questionnaire(String from, String messageId, String dateTime, String latitude, String longitude, //
      String pref1, String pref2, String pref3, String pref4, String prefOther, //
      String style, String level, String comments) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Questionnaire) other;
      var cmp = from.compareTo(o.from);
      return cmp;
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "From", "mId", "Date/Time", "Latitude", "Longitude", //
          "Pref1", "Pref2", "Pref3", "Pref4", "OtherPref", //
          "Style", "Level" };
    }

    @Override
    public String[] getValues() {
      return new String[] { from, messageId, dateTime, latitude, longitude, //
          pref1, pref2, pref3, pref4, prefOther, //
          style, level };
    }

    private static String parseBetween(String s, String begin, String end) {
      return s.substring(s.indexOf(begin) + begin.length(), s.indexOf(end)).trim();
    }

    @SuppressWarnings("unchecked")
    public static Questionnaire parse(ExportedMessage m) {
      var content = m.plainContent;

      final var error = "Error parsing json from " + m.from + " in file: " + m.fileName + ", content: " + content
          + ", error: ";

      final var BEGIN_JSON = "--- begin json ---";
      final var END_JSON = "--- end json ---";
      final var BEGIN_COMMENTS = "--- begin comments ---";
      final var END_COMMENTS = "--- end comments ---";
      var tags = List.of(BEGIN_JSON, END_JSON, BEGIN_COMMENTS, END_COMMENTS);
      for (var tag : tags) {
        if (!content.contains(tag)) {
          logger.error(error + " NO " + tag);
          return null;
        }
      }

      String jsonString = parseBetween(content, BEGIN_JSON, END_JSON);
      ObjectMapper mapper = new ObjectMapper();
      Map<String, String> map = null;
      try {
        map = mapper.readValue(jsonString, Map.class);
      } catch (Exception e) {
        logger.error(error + e.getMessage());
        return null;
      }

      var dateTime = m.msgDateTime != null ? m.msgDateTime.toString() : "";
      var lat = m.msgLocation != null ? m.msgLocation.getLatitude() : "0.0";
      var lon = m.msgLocation != null ? m.msgLocation.getLongitude() : "0.0";
      var pref1 = map.getOrDefault("First-Preferred-Form", "");
      var pref2 = map.getOrDefault("Second-Preferred-Winlink-Form", "");
      var pref3 = map.getOrDefault("Third-Preferred-Winlink-Form", "");
      var pref4 = map.getOrDefault("Fourth-Preferred-Winlink-Form", "");
      var prefOther = map.getOrDefault("Other-Winlink-Form", "");
      var style = map.getOrDefault("Data-Presentation-Style", "");
      var level = map.getOrDefault("Participation-Level", "");
      var comments = parseBetween(content, BEGIN_COMMENTS, END_COMMENTS);
      var questionnaire = new Questionnaire(m.from, m.messageId, dateTime, lat, lon, //
          pref1, pref2, pref3, pref4, prefOther, //
          style, level, comments);
      return questionnaire;
    }

  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.PLAIN;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var q = Questionnaire.parse(message);
    if (q != null) {
      count(sts.testIfPresent("1st Preferred Exercise should be present", q.pref1));
      getCounter("1st Preferred Exercise").increment(q.pref1);
      getCounter("Any Preferred Exercise").increment(q.pref1);

      count(sts.testIfPresent("2nd Preferred Exercise should be present", q.pref2));
      getCounter("2nd Preferred Exercise").increment(q.pref2);
      getCounter("Any Preferred Exercise").increment(q.pref2);

      count(sts.testIfPresent("3rd Preferred Exercise should be present", q.pref3));
      getCounter("3rd Preferred Exercise").increment(q.pref3);
      getCounter("Any Preferred Exercise").increment(q.pref3);

      count(sts.testIfPresent("4th Preferred Exercise should be present", q.pref4));
      getCounter("4th Preferred Exercise").increment(q.pref4);
      getCounter("Any Preferred Exercise").increment(q.pref4);

      getCounter("Other Preferred Exercise").increment(q.prefOther);
      getCounter("Any Preferred Exercise").increment(q.prefOther);

      count(sts.testIfPresent("Data Presentation Style should be present", q.style));
      getCounter("Data Presentation Style").increment(q.style);

      count(sts.testIfPresent("Frequency should be present", q.level));
      getCounter("Frequency").increment(q.level);

      questionnaires.add(q);
    } else { // end if valid questionnaire
      var reject = new RejectionMessage(message, RejectType.CANT_PARSE_ETO_JSON, message.plainContent);
      rejects.add(reject);
    }
  }

  @Override
  public void postProcess() {
    super.postProcess();
    writeTable("questionnaires.csv", questionnaires);
    writeTable("rejects.csv", rejects);
  }
}