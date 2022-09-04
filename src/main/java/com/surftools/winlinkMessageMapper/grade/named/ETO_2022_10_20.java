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

package com.surftools.winlinkMessageMapper.grade.named;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.DyfiMessage;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;

/**
 * DYFI for Shakeout 2022
 *
 * @author bobt
 *
 */
public class ETO_2022_10_20 extends DefaultGrader {
  private static Logger logger = LoggerFactory.getLogger(ETO_2022_10_20.class);

  // for post processing
  private int ppIsExerciseOk;
  private int ppAddressToUSGSOk;
  private int ppOrganizationOk;
  private int ppResponseOk;

  private Map<Integer, Integer> intensityCountMap;
  private Map<String, Integer> responseCountMap;
  private Map<String, Integer> organizationCountMap;

  private Set<String> dumpIds;

  public ETO_2022_10_20() {
    super(logger);
    intensityCountMap = new TreeMap<>();
    responseCountMap = new TreeMap<>();
    organizationCountMap = new TreeMap<>();
  }

  @Override

  /**
   * This Earthquake report is an: check Exercise. In case of a “real world event”, do not send messages to your ETO
   * clearinghouse. We are a training organization, not a response organization. Your message may sit, unread, for days.
   * For this exercise, if you check REAL EVENT, you will earn a score of 0 points for this exercise, regardless of
   * subsequent answers.
   *
   * Optional Exercise ID: ETO Winlink Thursday SHAKEOUT 2022 (50 points)
   *
   * How did you respond? Check Dropped and Covered (50 points). Drop, cover and hold is the recommended guideline for
   * how to respond to an earthquake. Even if that is not what you actually did at 10:20 AM or if there was no place to
   * cover yourself, Dropped and Covered is the answer we want.
   */
  public GradeResult grade(GradableMessage gm) {
    if (!(gm instanceof DyfiMessage)) {
      return null;
    }

    DyfiMessage m = (DyfiMessage) gm;
    var call = m.from;
    var messageId = m.messageId;

    if (dumpIds != null && (dumpIds.contains(m.messageId) || dumpIds.contains(m.from))) {
      // logger.info("ETO_2022_10_20 grader: " + m);
    }

    ++ppCount;
    var points = 0;
    var explanations = new ArrayList<String>();
    var autoFail = false;

    var isRealEvent = m.isRealEvent;
    if (isRealEvent) {
      explanations.add("FAIL: must not select 'Real Event'");
      autoFail = true;
    } else {
      ++ppIsExerciseOk;
    }

    var addresses = new TreeSet<String>();
    addresses.addAll(Arrays.asList(m.toList.split(",")));
    addresses.addAll(Arrays.asList(m.ccList.split(",")));
    var requiredUSGSAddress = "dyfi_reports_automated@usgs.gov";
    if (!addresses.contains(requiredUSGSAddress)) {
      explanations.add("FAIL: To: and Cc: addresses don't contain required address: " + requiredUSGSAddress);
      autoFail = true;
    } else {
      ++ppAddressToUSGSOk;
    }

    var organization = m.organization;
    var requiredOrganization = "ETO Winlink Thursday SHAKEOUT 2022";
    if (organization != null) {
      if (organization.equalsIgnoreCase(requiredOrganization)) {
        points += 50;
        ++ppOrganizationOk;
      } else {
        explanations.add("organization not '" + requiredOrganization + "'");
      }

      var count = organizationCountMap.getOrDefault(organization, Integer.valueOf(0));
      ++count;
      organizationCountMap.put(organization, count);
    } else {
      explanations.add("organization not '" + requiredOrganization + "'");
    }

    var response = m.response;
    var requiredResponse = "duck";
    if (response != null) {
      if (response.equalsIgnoreCase(requiredResponse)) {
        points += 50;
        ++ppResponseOk;
      } else {
        explanations.add("response not '" + requiredResponse + "'");
      }
      var count = responseCountMap.getOrDefault(response, Integer.valueOf(0));
      ++count;
      responseCountMap.put(response, count);
    } else {
      explanations.add("response not '" + requiredResponse + "'");
    }

    var intensityString = m.intensity;
    var intensity = 0;
    try {
      intensity = Integer.valueOf(intensityString);
      var count = intensityCountMap.getOrDefault(intensity, Integer.valueOf(0));
      ++count;
      intensityCountMap.put(intensity, count);
    } catch (Exception e) {
      logger.warn("could not parse intensity for call: " + call + ", messageId: " + messageId);
    }

    points = Math.min(100, points);
    points = Math.max(0, points);
    points = autoFail ? 0 : points;
    var grade = String.valueOf(points);
    var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

    gm.setIsGraded(true);
    gm.setGrade(grade);
    gm.setExplanation(explanation);

    return new GradeResult(grade, explanation);
  }

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    if (messages == null || messages.size() == 0) {
      return null;
    }

    var defaultReport = DefaultGrader.defaultPostProcessReport(messages);
    var sb = new StringBuilder(defaultReport);
    sb.append("\nETO-2022-10-20 Grading Report: graded " + ppCount + " DYFI messages\n");
    sb.append(formatPP("Organization ok", ppOrganizationOk));
    sb.append(formatPP("Action ok", ppResponseOk));
    sb.append(formatPP("Is Exerercise", ppIsExerciseOk));
    sb.append(formatPP("Addressed to USGS", ppAddressToUSGSOk));

    sb.append("\nCounts by response:\n");
    var sumCounts = responseCountMap.values().stream().reduce(0, Integer::sum);
    for (var key : responseCountMap.keySet()) {
      var count = responseCountMap.get(key);
      sb.append(formatCounts(key, count, sumCounts));
    }

    sb.append("\nCounts by organization:\n");
    sumCounts = organizationCountMap.values().stream().reduce(0, Integer::sum);
    for (var key : organizationCountMap.keySet()) {
      var count = organizationCountMap.get(key);
      sb.append(formatCounts(key, count, sumCounts));
    }

    sb.append("\nCounts by intensity:\n");
    sumCounts = intensityCountMap.values().stream().reduce(0, Integer::sum);
    for (var key : intensityCountMap.keySet()) {
      var count = intensityCountMap.get(key);
      sb.append(formatCounts(key, count, sumCounts));
    }

    return sb.toString();
  }

}