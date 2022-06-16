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

package com.surftools.winlinkMessageMapper.aggregation.named;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * After submitting the content of a Form, when reviewing your information in the New Message window, you may see an
 * error. While you may be tempted to just change the content in the New Message windows text box, This is not the
 * correct way to make a change when using Form Templates. Form information is encapsulated differently than the
 * information in the message field. Changes made in the message body of the New Message window are not reflected in the
 * content of the Form’s fields. The following exercise is to demonstrate the differences.
 *
 * @author bobt
 *
 */
public class ETO_XXXX_XX_XX_Edits_On_Form_After_Submit extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(ETO_XXXX_XX_XX_Edits_On_Form_After_Submit.class);

  private static final String KEY_AB_MESSAGES = "abMessages";
  private static final String KEY_C_MESSAGES = "cMessages";
  private static final String KEY_OTHER_MESSAGES = "otherMessages";
  private static final String KEY_GRADE = "grade";
  private static final String KEY_EXPLANATION = "explanation";

  public ETO_XXXX_XX_XX_Edits_On_Form_After_Submit() {
    super(logger);
  }

  @Override
  /**
   * include all Check In reports, with RMS and P2P counts
   *
   */
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    super.aggregate(messageMap);

    var newList = new ArrayList<AggregateMessage>();

    var ppCount = 0;
    var ppScore_100 = 0;
    var ppScore_50 = 0;
    var ppScore_0 = 0;

    for (var m : aggregateMessages) {
      ++ppCount;
      var from = m.from();
      var map = fromMessageMap.get(from);
      var list = map.get(MessageType.CHECK_IN);

      int abCount = 0;
      int cCount = 0;
      int otherCount = 0;

      for (var message : list) {
        var type = getType(message);
        switch (type) {
        case AB:
          ++abCount;
          break;

        case C:
          ++cCount;
          break;

        case Unknown:
          ++otherCount;
          break;
        }

      } // end for over exported messages

      m.data().put(KEY_AB_MESSAGES, abCount);
      m.data().put(KEY_C_MESSAGES, cCount);
      m.data().put(KEY_OTHER_MESSAGES, otherCount);

      var points = 0;
      var explanations = new ArrayList<String>();

      if (abCount >= 1) {
        points += 50;
      } else {
        explanations.add("no message with 'A' in form Comments and 'B' in text Comments");
      }

      if (cCount >= 1) {
        points += 50;
      } else {
        explanations.add("no message with 'C' in both form and text Comments");
      }

      points = Math.min(points, 100);
      m.data().put(KEY_GRADE, points);
      if (points == 100) {
        m.data().put(KEY_EXPLANATION, "Perfect score!");
        ++ppScore_100;
      } else {
        m.data().put(KEY_EXPLANATION, String.join("\n", explanations));
        if (points == 50) {
          ++ppScore_50;
        } else {
          ++ppScore_0;
        }
      }

      newList.add(m);
    } // end for over aggregate

    var sb = new StringBuilder();
    sb.append("\n\nWinlink Check In aggregate results:\n");
    sb.append("participants: " + ppCount + "\n");
    sb.append("score of 100: " + ppScore_100 + formatPercent(ppScore_100, ppCount) + "\n");
    sb.append("score of  50: " + ppScore_50 + formatPercent(ppScore_50, ppCount) + "\n");
    sb.append("score of   0: " + ppScore_0 + formatPercent(ppScore_0, ppCount) + "\n");
    logger.info(sb.toString());

    aggregateMessages = newList;
  }

  private String formatPercent(int numerator, int denominator) {
    double percent = (100d * numerator) / denominator;
    return " (" + String.format("%.2f", percent) + "%)";
  }

  private Type getType(ExportedMessage m) {
    var message = (CheckInMessage) m;
    var formComments = message.comments;
    var lines = message.getMimeLines();

    var foundCommentLine = false;
    var bodyComments = new ArrayList<String>();
    var consecutiveEmptyLines = 0;
    for (var line : lines) {
      if (line.startsWith("Comments:")) {
        foundCommentLine = true;
        continue;
      }
      if (foundCommentLine) {
        if (line.length() == 0) {
          ++consecutiveEmptyLines;
          if (consecutiveEmptyLines == 2) {
            break;
          }
        }
        bodyComments.add(line);
      }
    }

    var bodyCommentsString = String.join("\n", bodyComments).replaceAll("=20", " ").trim();

    Type type = null;
    if (formComments.equals("C") && bodyCommentsString.equals("C")) {
      type = Type.C;
    } else if (formComments.equals("A") && bodyCommentsString.equals("B")) {
      type = Type.AB;
    } else {
      type = Type.Unknown;
    }

    return type;

  }

  @Override
  public void output(String pathName) {
    super.output(pathName);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { //
        "From", //
        "DateTime", //
        "Latitude", //
        "Longitude", //
        "AB Messages", //
        "C Messages", //
        "Other", //
        "Grade", //
        "Explanation" };
  }

  @Override
  public String[] getValues(AggregateMessage m) {
    var ret = new String[] { //
        m.from(), //
        m.data().get(KEY_END_DATETIME).toString(), //
        String.valueOf(m.data().get(KEY_AVERAGE_LATITUDE)), //
        String.valueOf(m.data().get(KEY_AVERAGE_LONGITUDE)), //
        String.valueOf(m.data().get(KEY_AB_MESSAGES)), //
        String.valueOf(m.data().get(KEY_C_MESSAGES)), //
        String.valueOf(m.data().get(KEY_OTHER_MESSAGES)), //
        String.valueOf(m.data().get(KEY_GRADE)), //
        String.valueOf(m.data().get(KEY_EXPLANATION)), //
    };
    return ret;
  }

  static enum Type {
    Unknown, AB, C
  };
}
