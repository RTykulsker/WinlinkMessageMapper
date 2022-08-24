/**

lswartz780@yahoo.comThe MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.tool;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.winlinkMessageMapper.summary.CallSignChange;
import com.surftools.winlinkMessageMapper.summary.HistoryCategory;
import com.surftools.winlinkMessageMapper.summary.ParticipantHistory;
import com.surftools.winlinkMessageMapper.summary.ParticipantSummary;
import com.surftools.winlinkMessageMapper.summary.SummaryDao;

/**
 * /**
 *
 * Participant change their call signs
 *
 * Participants like their ParticipantHistory
 *
 * Participants want their old call sign data rolled into their new call sign
 *
 * Keep the participants happy
 *
 * @author bobt
 *
 */

public class CallSignChangeTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(CallSignChangeTool.class);

  @Option(name = "--oldCallSign", usage = "old call sign", required = true)
  private String oldCallSign = null;

  @Option(name = "--newCallSign", usage = "new call sign", required = true)
  private String newCallSign = null;

  @Option(name = "--inputPathName", usage = "path to input directory", required = true)
  private String inputPathName = null;

  public static void main(String[] args) {
    CallSignChangeTool app = new CallSignChangeTool();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  /**
   *
   * I'm not planning on touching/editing any of the input files that I receive from the clearinghouses. I never do
   * that.
   *
   * I'm not planning on updating any of the maps that we've published either. Once we publish to the web, those maps
   * locked.
   *
   * I do produce several "summary" files for each exercise. Those summary files from a previous exercise become part of
   * the input for the current exercise.
   *
   * The relevant input files are
   *
   * -- participantHistory -- one row per participant, lifetime to date (or at least 2022 to date). There are two rows
   * for you one for KD0ONE and one for K0EAV
   *
   * -- participantSummary -- one row per participant per exercise. There are 5 rows for KD0ONE and 14 rows for K0EAV
   * The relevant output files are exercise-participantHistory -- one row per participant, but only if they participated
   * on a given week. If you take a vacation, you don't get mapped for the current week, but your history will show up
   * the next time you "come out to play"
   *
   * So it seems that all I need to do is
   *
   * -- modify all the rows in the participantHistory with the old call sign, and replace with the new call sign. That's
   * easy.
   *
   * -- in participantSummary, add the data from the old call sign to the new call sign and replace the data for the row
   * with the new call sign. Delete the row with the old call sign
   *
   * -- create a new file, callsignChanges and write a row stating that I changed old call sign to new call on on a
   * given date.
   *
   *
   */
  private void run() {
    try {
      logger.info("begin");

      var outputPathName = Path.of(inputPathName, "callsignChange-" + oldCallSign + "-" + newCallSign).toString();
      FileUtils.makeDirIfNeeded(outputPathName);

      logger.info("old call sign:  " + oldCallSign);
      logger.info("new call sign:  " + newCallSign);
      logger.info("inputPathName:  " + inputPathName);
      logger.info("outputPathName: " + outputPathName);

      var dao = new SummaryDao(inputPathName, outputPathName);

      var result = validateAndMerge(dao, oldCallSign, newCallSign);

      var updatedHistories = updateHistory(oldCallSign, newCallSign, result);
      dao.writeParticipantHistory(updatedHistories, false);

      var updatedSummaries = updateSummaries(oldCallSign, newCallSign, result);
      dao.writeParticipantSummary(updatedSummaries, false);

      updateCallSignChangeLog(dao, oldCallSign, newCallSign);

      logger.info("exiting");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  private void updateCallSignChangeLog(SummaryDao dao, String oldCallSign2, String newCallSign2) {
    var callSignChanges = dao.readCallSignChanges();
    if (callSignChanges.size() == 0) {
      callSignChanges.add(new CallSignChange(LocalDate.now(), LocalTime.now(), oldCallSign, newCallSign));
    } else {
      var last = callSignChanges.get(callSignChanges.size() - 1);
      if ( //
      !last.date().equals(LocalDate.now()) //
          || !last.newCall().equals(newCallSign) //
          || !last.oldCall().equals(oldCallSign)) {
        callSignChanges.add(new CallSignChange(LocalDate.now(), LocalTime.now(), oldCallSign, newCallSign));
      }
    }
    dao.writeCallSignChanges(callSignChanges);
  }

  private List<ParticipantSummary> updateSummaries(String oldCallSign, String newCallSign, ValidateMergeResult result) {
    var updatedList = new ArrayList<ParticipantSummary>();
    for (var ps : result.psList) {
      if (ps.getCall().equals(oldCallSign)) {
        ps.setCall(newCallSign);
      }
      updatedList.add(ps);
    }
    return updatedList;
  }

  private List<ParticipantHistory> updateHistory(String oldCallSign, String newCallSign, ValidateMergeResult result) {
    var updatedList = new ArrayList<ParticipantHistory>();
    for (var ph : result.phList) {
      if (ph.getCall().equals(oldCallSign)) {
        // skip over old call sign, effectively deleting it
        continue;
      }

      if (ph.getCall().equals(newCallSign)) {
        // substitute the merged value
        updatedList.add(result.mergedHistory);
      } else {
        updatedList.add(ph);
      }
    }
    return updatedList;
  }

  private ValidateMergeResult validateAndMerge(SummaryDao dao, String oldCallSign, String newCallSign) {
    var histories = dao.readParticipantHistories();
    var summaries = dao.readParticipantSummaries();

    var oldParticipantHistory = getHistory(histories, oldCallSign);
    if (oldParticipantHistory == null) {
      logger.error("no participantHistory found for oldCallSign: " + oldCallSign + ", exiting");
      System.exit(1);
    } else {
      logger.info("found old participantHistory with " + oldParticipantHistory.getExerciseCount() + " exercises");
    }

    var newParticipantHistory = getHistory(histories, newCallSign);
    if (newParticipantHistory == null) {
      logger.error("no participantHistory found for newCallSign: " + newCallSign + ", exiting");
      System.exit(1);
    } else {
      logger.info("found new participantHistory with " + newParticipantHistory.getExerciseCount() + " exercises");
    }

    var oldParticipantSummaries = getSummaries(summaries, oldCallSign);
    if (oldParticipantSummaries.size() == 0) {
      logger.error("no participantSummaries found for oldCallSign: " + oldCallSign + ", exiting");
      System.exit(1);
    } else {
      logger.info("found " + oldParticipantSummaries.size() + " old participantSummaries");
    }

    var newParticipantSummaries = getSummaries(summaries, newCallSign);
    if (newParticipantSummaries.size() == 0) {
      logger.error("no participantSummaries found for newCallSign: " + newCallSign + ", exiting");
      System.exit(1);
    } else {
      logger.info("found " + newParticipantSummaries.size() + " new participantSummaries");
    }

    if (oldParticipantHistory.getExerciseCount() != oldParticipantSummaries.size()) {
      logger
          .error("exercise count mismatch for old call: " + oldCallSign + "; history: "
              + oldParticipantHistory.getExerciseCount() + " , summaries: " + oldParticipantSummaries.size()
              + ", exiting");
      System.exit(1);
    }

    if (newParticipantHistory.getExerciseCount() != newParticipantSummaries.size()) {
      logger
          .error("exercise count mismatch for new call: " + newCallSign + "; history: "
              + newParticipantHistory.getExerciseCount() + " , summaries: " + newParticipantSummaries.size()
              + ", exiting");
      System.exit(1);
    }

    var mergedHistory = new ParticipantHistory(newParticipantHistory);
    mergedHistory.setExerciseCount(newParticipantHistory.getExerciseCount() + oldParticipantHistory.getExerciseCount());
    mergedHistory.setMessageCount(newParticipantHistory.getMessageCount() + oldParticipantHistory.getExerciseCount());

    var currentExerciseSummaryList = dao.readExerciseSummaries();
    var totalExerciseCount = currentExerciseSummaryList.size();
    double percent = Math.round(100d * mergedHistory.getExerciseCount() / totalExerciseCount);
    if (percent >= 90d) {
      mergedHistory.setCategory(HistoryCategory.HEAVEY_HITTER);
    } else if (percent >= 50d) {
      mergedHistory.setCategory(HistoryCategory.GOING_STRONG);
    } else {
      mergedHistory.setCategory(HistoryCategory.NEEDS_ENCOURAGEMENT);
    }

    logger.info("mergedHistory: " + mergedHistory);

    var result = new ValidateMergeResult(histories, summaries, mergedHistory);
    return result;
  }

  static record ValidateMergeResult(List<ParticipantHistory> phList, List<ParticipantSummary> psList,
      ParticipantHistory mergedHistory) {
  };

  private ParticipantHistory getHistory(List<ParticipantHistory> histories, String callSign) {
    for (var ph : histories) {
      if (ph.getCall().equals(callSign)) {
        return ph;
      }
    }
    return null;
  }

  private List<ParticipantSummary> getSummaries(List<ParticipantSummary> summaries, String callSign) {
    var list = new ArrayList<ParticipantSummary>();
    for (var ps : summaries) {
      if (ps.getCall().equals(callSign)) {
        list.add(ps);
      }
    }
    return list;
  }

}