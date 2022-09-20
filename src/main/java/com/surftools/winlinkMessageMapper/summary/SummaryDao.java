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

package com.surftools.winlinkMessageMapper.summary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

public class SummaryDao {
  private static final Logger logger = LoggerFactory.getLogger(Summarizer.class);

  private final String inputPathName;
  private final String outputPathName;

  public SummaryDao(String inputPathName, String outputPathName) {
    this.inputPathName = inputPathName;
    this.outputPathName = outputPathName;
  }

  public void writeExerciseSummary(List<ExerciseSummary> list, boolean isSnapshot) {
    var type = (isSnapshot) ? "snapshots" : "database";
    Path outputPath = Path.of(outputPathName, "output", type, "exerciseSummary.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(ExerciseSummary.getHeaders());

      if (list.size() > 0) {
        var comparator = new ExerciseSummaryComparator();
        Collections.sort(list, comparator);

        for (ExerciseSummary es : list) {
          writer.writeNext(es.getValues());
        }
      }

      writer.close();
      logger.info("wrote " + list.size() + " exerciseSummaries to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public void writeParticipantSummary(List<ParticipantSummary> list, boolean isSnapshot) {
    var type = (isSnapshot) ? "snapshots" : "database";
    Path outputPath = Path.of(outputPathName, "output", type, "participantSummary.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdirs();
      }

      var comparator = new ParticipantSummaryComparator();
      Collections.sort(list, comparator);

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(ParticipantSummary.getHeaders());
      for (ParticipantSummary ps : list) {
        writer.writeNext(ps.getValues());
      }
      writer.close();
      logger.info("wrote " + list.size() + " participantSummaries to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public void writeParticipantHistory(List<ParticipantHistory> list, boolean isSnapshot) {
    var type = (isSnapshot) ? "snapshots" : "database";
    Path outputPath = Path.of(outputPathName, "output", type, "participantHistory.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdirs();
      }

      var comparator = new ParticipantHistoryComparator();
      Collections.sort(list, comparator);

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(ParticipantHistory.getHeaders());
      for (ParticipantHistory ph : list) {
        writer.writeNext(ph.getValues());
      }
      writer.close();
      logger.info("wrote " + list.size() + " participantHistories to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public void writeExerciseParticipantHistory(List<ParticipantHistory> list, String exerciseDate) {
    Path outputPath = Path.of(outputPathName, "output", "database", "exercise-participantHistory.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      var comparator = new ParticipantHistoryComparator();
      Collections.sort(list, comparator);

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(ParticipantHistory.getHeaders());
      var messageCount = 0;
      for (ParticipantHistory ph : list) {
        if (ph.getLastDate().equals(exerciseDate)) {
          ++messageCount;
          writer.writeNext(ph.getValues());
        }
      }
      writer.close();
      logger.info("wrote " + messageCount + " exercise-participantHistories to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public void writeFirstTimers(List<ParticipantHistory> list, String exerciseDate, MessageType messageType,
      Map<String, ExportedMessage> exerciseCallMessageMap) {

    if (messageType == MessageType.UNKNOWN || list.size() == 0 || exerciseCallMessageMap.size() == 0) {
      logger.info("wrote zero firstTimer messages");
      return;
    }

    Path outputPath = Path.of(outputPathName, "output", "database", "firstTimers-" + messageType.toString() + ".csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      var comparator = new ParticipantHistoryComparator();
      Collections.sort(list, comparator);

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      var aMessage = exerciseCallMessageMap.values().iterator().next();
      var messageCount = 0;
      writer.writeNext(aMessage.getHeaders());

      for (ParticipantHistory ph : list) {
        if (ph.getLastDate().equals(exerciseDate) && ph.getExerciseCount() == 1) {
          var call = ph.getCall();
          var message = exerciseCallMessageMap.get(call);
          if (message == null) {
            continue;
          }

          writer.writeNext(message.getValues());
          ++messageCount;
        }
      }
      writer.close();
      logger
          .info("wrote " + messageCount + " first time " + messageType.toString() + " message to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public List<ExerciseSummary> readExerciseSummaries() {
    var list = new ArrayList<ExerciseSummary>();
    Path inputPath = Path.of(inputPathName, "exerciseSummary.csv");

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;

        var exerciseSummary = new ExerciseSummary(fields);
        list.add(exerciseSummary);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " exerciseSummaries from: " + inputPath.toString());
    return list;
  }

  public List<ParticipantSummary> readParticipantSummaries() {
    var list = new ArrayList<ParticipantSummary>();
    Path inputPath = Path.of(inputPathName, "participantSummary.csv");

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new BufferedReader(new FileReader(inputPath.toString()));
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(true) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        var participantSummary = new ParticipantSummary(fields);
        list.add(participantSummary);
      }
    } catch (Exception e) {
      logger
          .error(
              "Exception reading " + inputPath.toString() + ", rowCount: " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " participantSummaries from: " + inputPath.toString());
    return list;
  }

  public List<CallSignChange> readCallSignChanges() {
    var list = new ArrayList<CallSignChange>();
    Path inputPath = Path.of(inputPathName, "callSignChange.csv");

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new BufferedReader(new FileReader(inputPath.toString()));
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(true) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        var callSignChange = CallSignChange.parse(fields);
        list.add(callSignChange);
      }
    } catch (Exception e) {
      logger
          .error(
              "Exception reading " + inputPath.toString() + ", rowCount: " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " callSignChanges from: " + inputPath.toString());
    return list;
  }

  public void writeCallSignChanges(List<CallSignChange> list) {
    Path outputPath = Path.of(outputPathName, "callSignChange.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(CallSignChange.getHeaders());
      var messageCount = 0;
      for (CallSignChange csc : list) {
        writer.writeNext(csc.getValues());
      }
      writer.close();
      logger.info("wrote " + messageCount + " callSignChanges to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public List<ParticipantHistory> readParticipantHistories() {
    var list = new ArrayList<ParticipantHistory>();
    Path inputPath = Path.of(inputPathName, "participantHistory.csv");

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(true) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        var participantHistory = new ParticipantHistory(fields);
        list.add(participantHistory);
      }
    } catch (Exception e) {
      logger
          .error(
              "Exception reading " + inputPath.toString() + ", rowCount: " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " participantHistories from: " + inputPath.toString());
    return list;
  }

  static class ExerciseSummaryComparator implements Comparator<ExerciseSummary> {

    @Override
    public int compare(ExerciseSummary o1, ExerciseSummary o2) {
      if (o2.getDate() == null) {
        return 1;
      }

      int compare = o1.getDate().compareTo(o2.getDate());
      if (compare != 0) {
        return compare;
      }

      if (o2.getName() == null) {
        return 1;
      }
      compare = o1.getName().compareTo(o2.getName());
      return compare;
    }

  }

  static class ParticipantSummaryComparator implements Comparator<ParticipantSummary> {

    @Override
    public int compare(ParticipantSummary o1, ParticipantSummary o2) {
      int compare = o1.getDate().compareTo(o2.getDate());
      if (compare != 0) {
        return compare;
      }

      compare = o1.getName().compareTo(o2.getName());
      if (compare != 0) {
        return compare;
      }

      compare = o1.getCall().compareTo(o2.getCall());
      return compare;
    }

  }

  static class ParticipantHistoryComparator implements Comparator<ParticipantHistory> {

    @Override
    public int compare(ParticipantHistory o1, ParticipantHistory o2) {
      int compare = o1.getCall().compareTo(o2.getCall());
      return compare;
    }

  }

}
