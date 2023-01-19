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

package com.surftools.wimp.processors.named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.GradedResult;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;

/**
 * Operation Ashfall (247 lines)
 *
 * @param m
 * @return
 */
public class ETO_2022_05_14_RMS extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_05_14_RMS.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {

    var pointsCounter = new Counter();
    List<IWritableTable> results = new ArrayList<IWritableTable>();
    for (var gm : mm.getMessagesForType(MessageType.FIELD_SITUATION)) {

      var m = (FieldSituationMessage) gm;

      // participation points
      var points = 20;
      var<String> explanations = new ArrayList<String>();

      var expected = "";
      var automaticFail = false;

      // AGENCY or GROUP NAME: Must match exactly EmComm Training Organization 0514 THIS IS A DRILL
      expected = "EmComm Training Organization 0514 THIS IS A DRILL";
      if (m.organization != null && m.organization.equalsIgnoreCase(expected)) {
        points += 5;
      } else {
        explanations.add("Agency/Group name not: " + expected + ".");
      }

      // Precedence: MUST be Priority
      expected = "(P) - Priority";
      if (m.precedence != null && m.precedence.equals(expected)) {
        points += 5;
      } else {
        automaticFail = true;
        explanations.add("Precedence must be: " + expected + ".");
      }

      // TASK # must be: 0514
      expected = "0514";
      var task = m.task;
      if (task != null && task.equals(expected)) {
        points += 5;
      } else {
        explanations.add("TASK # must be: " + expected + ".");
      }

      // Box1: MUST be checked No and contain no comments
      if (m.isHelpNeeded.equals("NO")) {
        points += 5;
      } else {
        automaticFail = true;
        explanations.add("Box 1 must be NO.");
      }

      if (m.neededHelp == null) {
        points += 5;
      } else {
        automaticFail = true;
        explanations.add("Box 1 comments must be empty.");
      }

      // Box 2: CITY must be filled in along with COUNTY and STATE for the RMS gateway part of the drill. NO TERRITORY
      // should be specified. For international participants sending to ETO-CAN or ETO-DX your information under
      // Territory
      // should be specified. For the P2P part of the drill, CITY must be filled in.
      var isCity = m.city != null && m.city.length() > 0;
      var isCounty = m.county != null && m.county.length() > 0;
      var isState = m.state != null && m.state.length() > 0;
      var isTerritory = m.territory != null && m.territory.length() > 0;
      var isRMS = m.to.startsWith("ETO");
      if (isRMS) {
        var isDomestic = m.to.startsWith("ETO-0") || m.to.equals("ETO-10");
        if (isDomestic) {
          if (isCity && isCounty && isState) {
            points += 5;
          } else {
            explanations.add("must supply City, County and State for US RMS.");
          } // endif fail domestic
        } else { // endif domestic
          if (isCity && isTerritory) {
            points += 5;
          } else {
            explanations.add("must supply City and Territory for non-US RMS.");
          }
        } // endif not domestic
      } else { // endif RMS
        if (isCity) {
          points += 5;
        } else {
          explanations.add("must supply City for P2P.");
        }
      } // endif not RMS

      // Boxes 4-11 must contain comments if the answer is “NO”, and must not contain comments if the answer is “YES” or
      // “Unknown - N/A.”

      var<List> fieldsList = Arrays
          .asList(Arrays.asList(m.landlineStatus, m.landlineComments, "landline"), //
              Arrays.asList(m.cellPhoneStatus, m.cellPhoneComments, "cellPhone"), //
              Arrays.asList(m.radioStatus, m.radioComments, "radio"), //
              Arrays.asList(m.tvStatus, m.tvComments, "tv"), //
              Arrays.asList(m.waterStatus, m.waterComments, "water"), //
              Arrays.asList(m.powerStatus, m.powerComments, "power"), //
              Arrays.asList(m.internetStatus, m.internetComments, "Internet"), //
              Arrays.asList(m.noaaStatus, m.noaaComments, "NOAA/Weather")//
          );//

      var boxIndex = 3;
      for (var fields : fieldsList) {
        ++boxIndex;
        var status = fields.get(0);
        var comments = fields.get(1);
        var label = fields.get(2);

        if (status.equals("YES") || status.equals("Unknown - N/A")) {
          if (comments == null || comments.equals("")) {
            points += 5;
          } else {
            explanations.add("box " + boxIndex + " must NOT contain comments for " + label + ".");
          }
        } else {
          if (comments == null || comments.equals("")) {
            explanations.add("box " + boxIndex + " must contain comments for " + label + ".");
          } else {
            points += 5;
          }
        }
      }

      // Box 12 must contain ashfall mounts in inches if present or “NO ASHFALL” if none is present
      if (m.additionalComments != null) {
        var comments = m.additionalComments.toLowerCase();
        if (comments.contains("NO ASHFALL".toLowerCase())) {
          points += 5;
        } else {
          if (comments.contains("inch") || comments.matches(".*\\d.*")) {
            points += 5;
          } else {
            explanations
                .add("Box 12 must contain ashfall amounts in inches if present or “NO ASHFALL” if none is present.");
          }
        }

        // additional activity option -- no original credit, so now it's extra credit
        {
          final var<String> jetSet = Set.of("jet stream over", "jet stream near", "jetstream over", "jetstream near");
          for (var string : jetSet) {
            if (comments.contains(string)) {
              points += 5;
              explanations.add("extra credit for Jet Stream");
            } // end if contains
          } // end for
        } // end block additional activity
      } else {
        explanations.add("Box 12 must contain ashfall mounts in inches if present or “NO ASHFALL” if none is present.");
      }

      // Box 13 must be filled.
      if (m.poc != null) {
        points += 5;
      } else {
        explanations.add("Box 13 must be filled.");
      }

      points = Math.min(100, points);
      points = Math.max(0, points);
      pointsCounter.increment(points);
      var grade = String.valueOf(points);
      var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

      if (automaticFail) {
        grade = "Automatic Fail";
      }

      var result = new GradedResult(m, grade, explanation);
      results.add(result);
    } // end loop over messages

    var sb = new StringBuilder();
    sb.append("\nScores: \n" + formatCounter(pointsCounter.getDescendingKeyIterator(), "score", "count"));
    logger.info(sb.toString());
    writeTable("graded-fsr.csv", results);
  }

}