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

package com.surftools.wimp.processors.exercise.eto_2024;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.processors.std.baseExercise.FeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for FSR with Severe weather
 *
 *
 * @author bobt
 *
 */
public class ETO_2024_08_15 extends FeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2024_08_15.class);

  private Set<String> clearinghouseSet = new HashSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    acceptableMessageTypesSet.add(MessageType.FIELD_SITUATION);

    var clearinghouses = cm.getAsString(Key.EXPECTED_DESTINATIONS);
    clearinghouseSet.addAll(Arrays.asList(clearinghouses.split(",")));
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    FieldSituationMessage m = (FieldSituationMessage) message;

    getCounter("Clearinghouse Count").increment(m.to);

    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));

    count(sts.test("Precedence should be #EV", "R/Routine", m.precedence));
    getCounter("Precedence").increment(m.precedence);

    count(sts.testIfPresent("Date/Time should be present", m.formDateTime));

    count(sts.test("Task should be #EV", "001", m.task));
    getCounter("Task").increment(m.task);

    count(sts.test("Form From should match call sign", m.formFrom.equalsIgnoreCase(m.from)));

    var isClearinghouseInFormTo = false;
    var formTo = m.formTo.toUpperCase();
    for (var clearinghouse : clearinghouseSet) {
      if (formTo.contains(clearinghouse)) {
        isClearinghouseInFormTo = true;
        break;
      }
    }
    count(sts.test("Form To should contain clearinghouse", isClearinghouseInFormTo));
    count(sts.test("Form To should contain ETO-BK", formTo.contains("ETO-BK")));

    count(sts.test("Box 1 Is there an EMERGENT/LIFE SAFETY Need should be #EV", "NO", m.isHelpNeeded));
    count(sts.test("911 box should be empty", m.neededHelp == null || m.neededHelp.isEmpty()));

    var nonFunctionalCount = 0;
    nonFunctionalCount += countAndTest("Box 4a POTS landlines functioning", m.landlineStatus, m.landlineComments);
    nonFunctionalCount += countAndTest("Box 4b VOIP landlines functioning", m.voipStatus, m.voipComments);
    nonFunctionalCount += countAndTest("Box 5a Cell phone voice functioning", m.cellPhoneStatus, m.cellPhoneComments);
    nonFunctionalCount += countAndTest("Box 5b Cell phone texts functioning", m.cellTextStatus, m.cellTextComments);
    nonFunctionalCount += countAndTest("Box 6 AM/FM Broadcast functioning", m.radioStatus, m.radioComments);
    nonFunctionalCount += countAndTest("Box 7a OTA TV functioning", m.tvStatus, m.tvComments);
    nonFunctionalCount += countAndTest("Box 7b Satellite TV functioning", m.satTvStatus, m.satTvComments);
    nonFunctionalCount += countAndTest("Box 7c Cable TV functioning", m.cableTvStatus, m.cableTvComments);
    nonFunctionalCount += countAndTest("Box 8 Public Water Works functioning", m.waterStatus, m.waterComments);
    nonFunctionalCount += countAndTest("Box 9a Commercial Power functioning", m.powerStatus, m.powerComments);
    nonFunctionalCount += countAndTest("Box 9b Commercial Power stable", m.powerStableStatus, m.powerStableComments);
    nonFunctionalCount += countAndTest("Box 9c Natural Gas Supply functioning", m.naturalGasStatus,
        m.naturalGasComments);
    nonFunctionalCount += countAndTest("Box 10 Internet functioning", m.internetStatus, m.internetComments);
    nonFunctionalCount += countAndTest("Box 11a NOAA weather radio functioning", m.noaaStatus, m.noaaComments);

    nonFunctionalCount += INVERTED_countAndTest("Box 11b NOAA weather audio degraded", m.noaaAudioDegraded,
        m.noaaAudioDegradedComments);

    sts.testIfPresent("Box 12 content should be present", m.additionalComments);
    count(sts.test("At least one infrastructure should be non-functional", nonFunctionalCount > 0));
    getCounter("Non-functional infrastructure items").increment(nonFunctionalCount);

    getCounter("Form Version").increment(m.formVersion);
    getCounter("Feedback Count").increment(sts.getExplanations().size());

    setExtraOutboundMessageText(sts.getExplanations().size() == 0 ? "" : OB_DISCLAIMER);
  }

  private int countAndTest(String label, String status, String comments) {
    return internalCountAndTest(label, status, comments, false);
  }

  private int INVERTED_countAndTest(String label, String status, String comments) {
    return internalCountAndTest(label, status, comments, true);
  }

  /**
   *
   * @param label
   * @param status
   * @param comments
   * @param invertLogic
   *
   * @return 1 if non-functional
   */
  private int internalCountAndTest(String label, String status, String comments, boolean invertLogic) {
    getCounter(label).increment(status);
    var STATUS = status.toUpperCase();
    var newLabel = label + " Comments should ONLY be present if not functioning";
    var returnValue = 0;

    var notFunctioning = (invertLogic) ? STATUS.equals("YES") : STATUS.equals("NO");
    var functioning = !notFunctioning;

    var commentsPresent = comments != null && !comments.isEmpty();
    if (functioning) {
      count(sts.test(newLabel, !commentsPresent));
    } else {
      returnValue = 1;
      count(sts.test(newLabel, commentsPresent));
    }
    return returnValue;
  }

}
