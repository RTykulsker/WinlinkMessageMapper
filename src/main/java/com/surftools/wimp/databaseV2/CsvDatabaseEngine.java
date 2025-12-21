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

package com.surftools.wimp.databaseV2;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.databaseV2.entity.ExerciseSummary;
import com.surftools.wimp.databaseV2.entity.OrganizationSummary;
import com.surftools.wimp.databaseV2.entity.ParticipantDetail;
import com.surftools.wimp.databaseV2.entity.ParticipantSummary;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * load and store database from CSV files
 */
public class CsvDatabaseEngine extends AbstractInMemoryDatabaseEngine {
  private static final Logger logger = LoggerFactory.getLogger(CsvDatabaseEngine.class);

  public static final String ORGANIZATION_SUMMARY_FILENAME = "organizationSummary.csv";
  public static final String EXERCISE_SUMMARY_FILENAME = "exerciseSummary.csv";
  public static final String PARTICIPANT_SUMMARY_FILENAME = "participantSummary.csv";
  public static final String PARTICIPANT_DETAIL_FILENAME = "participantDetail.csv";
  public static final List<String> FILE_NAMES = List
      .of(ORGANIZATION_SUMMARY_FILENAME, EXERCISE_SUMMARY_FILENAME, PARTICIPANT_SUMMARY_FILENAME,
          PARTICIPANT_DETAIL_FILENAME);

  private String inputDbPathName;
  private Path outputDbPath;
  private String outputDbPathName;

  public CsvDatabaseEngine(IConfigurationManager cm) {
    super(logger);
    inputDbPathName = cm.getAsString(Key.NEW_DATABASE_PATH);

    var outputPathName = AbstractBaseProcessor.outputPathName;
    outputDbPath = Path.of(outputPathName, "newDatabase");
    if (!outputDbPath.toFile().exists()) {
      outputDbPath.toFile().mkdirs();
      if (!outputDbPath.toFile().exists()) {
        throw new RuntimeException("Output database directory: " + outputDbPath.toString() + " can't be created");
      }
    }
    outputDbPathName = outputDbPath.toString();
  }

  @Override
  public void load() {
    var map = participantDetailMap;
    var path = Path.of(inputDbPathName, PARTICIPANT_DETAIL_FILENAME);
    var fieldsArray = ReadProcessor.readCsvFileIntoFieldsArray(path, ',', false, 1);
    for (var fields : fieldsArray) {
      var pd = ParticipantDetail.make(fields);
      var exerciseId = pd.exerciseId();
      var list = map.getOrDefault(exerciseId, new ArrayList<ParticipantDetail>());
      list.add(pd);
      map.put(exerciseId, list);
    }
    logger
        .info("read " + map.entrySet().size() + " records for " + map.size() + " exercises from file:  "
            + path.toString());
  }

  @Override
  public void store() {
    LocalDate firstDate = null;
    LocalDate lastDate = null;
    var exerciseSummaryList = new ArrayList<ExerciseSummary>();
    var participantSummaryMap = new HashMap<String, ParticipantSummary>();
    var participantDetailList = new ArrayList<ParticipantDetail>();
    var totalMessageCount = 0;

    for (var exerciseId : participantDetailMap.keySet()) {
      var exerciseMessageCount = 0;
      var participantDetails = participantDetailMap.get(exerciseId);
      participantDetailList.addAll(participantDetails);
      for (var pd : participantDetails) {
        var date = pd.exerciseId().date();
        firstDate = firstDate == null ? date : date.isBefore(firstDate) ? date : firstDate;
        lastDate = lastDate == null ? date : date.isAfter(lastDate) ? date : lastDate;
        exerciseMessageCount += pd.messageCount();
        totalMessageCount += pd.messageCount();
        var ps = participantSummaryMap.getOrDefault(pd.call(), new ParticipantSummary(null, null, null, null, 0, 0));
        ps = ps.update(pd);
        participantSummaryMap.put(ps.call(), ps);
      } // end loop over participantDetail
      var exerciseSummary = new ExerciseSummary(exerciseId, exerciseMessageCount, participantDetails.size());
      exerciseSummaryList.add(exerciseSummary);
    } // end loop over exerciseIds in participantDetailMap

    var orgSummary = new OrganizationSummary(exerciseSummaryList.size(), participantSummaryMap.size(),
        totalMessageCount, firstDate, lastDate);
    var orgTableList = new ArrayList<IWritableTable>(List.of(orgSummary));
    WriteProcessor.writeTable(orgTableList, Path.of(outputDbPathName, ORGANIZATION_SUMMARY_FILENAME));
    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(exerciseSummaryList),
            Path.of(outputDbPathName, EXERCISE_SUMMARY_FILENAME));
    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(participantSummaryMap.values()),
            Path.of(outputDbPathName, PARTICIPANT_SUMMARY_FILENAME));
    WriteProcessor
        .writeTable(new ArrayList<IWritableTable>(participantDetailList),
            Path.of(outputDbPathName, PARTICIPANT_DETAIL_FILENAME));
  }

}
