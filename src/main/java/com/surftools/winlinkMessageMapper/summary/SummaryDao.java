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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

public class SummaryDao {
  private static final Logger logger = LoggerFactory.getLogger(Summarizer.class);
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

  private final String inputPathName;
  private final String outputPathName;

  public SummaryDao(String inputPathName, String outputPathName) {
    this.inputPathName = inputPathName;
    this.outputPathName = outputPathName;
  }

  public void persistExerciseSummary(ExerciseSummary exerciseSummary) {
    List<ExerciseSummary> currentList = new ArrayList<>();
    currentList.add(exerciseSummary);
    writeExerciseSummary(currentList, true);

    var pastList = readExerciseSummaries();
    pastList.addAll(currentList);
    writeExerciseSummary(pastList, false);
  }

  public void persistParticipantSummary(HashMap<String, ParticipantSummary> callPartipantSummaryMap) {
    List<ParticipantSummary> currentList = new ArrayList<>();
    currentList.addAll(callPartipantSummaryMap.values());
    writeParticipantSummary(currentList, true);

    var pastList = readParticipantSummaries();
    pastList.addAll(currentList);
    writeParticipantSummary(pastList, false);
  }

  public void persistParticipantHistory(HashMap<String, ParticipantHistory> callParticipantHistoryMap) {
    List<ParticipantHistory> currentList = new ArrayList<>();
    currentList.addAll(callParticipantHistoryMap.values());
    writeParticipantHistory(currentList, true);

    // read, merge, write
    var pastList = readParticipantHistories();
    var pastMap = new HashMap<String, ParticipantHistory>();
    for (ParticipantHistory ph : pastList) {
      pastMap.put(ph.getCall(), ph);
    }

    List<ParticipantHistory> mergedList = new ArrayList<>();
    for (ParticipantHistory current : currentList) {
      var call = current.getCall();
      var past = pastMap.get(call);
      if (past != null) {
        LocalDate pastDate = LocalDate.parse(past.getLastDate(), FORMATTER);
        LocalDate currentDate = LocalDate.parse(current.getLastDate(), FORMATTER);
        if (pastDate.isAfter(currentDate)) {
          current.setLastDate(past.getLastDate());
          current.setLastLocation(past.getLastLocation());
        }
        current.setMessageCount(current.getMessageCount() + past.getMessageCount());
        current.setExerciseCount(current.getExerciseCount() + past.getExerciseCount());
        pastMap.remove(call);
      }
      mergedList.add(current);
    }
    mergedList.addAll(pastMap.values());
    writeParticipantHistory(mergedList, false);
  }

  private void writeExerciseSummary(List<ExerciseSummary> list, boolean isSnapshot) {
    var type = (isSnapshot) ? "snapshots" : "database";
    Path outputPath = Path.of(outputPathName, "output", type, "exerciseSummary.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      var comparator = new ExerciseSummaryComparator();
      Collections.sort(list, comparator);

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(ExerciseSummary.getHeaders());
      for (ExerciseSummary es : list) {
        writer.writeNext(es.getValues());
      }
      writer.close();
      logger.info("wrote " + list.size() + " exerciseSummaries to file: " + outputPath);
    } catch (

    Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  private void writeParticipantSummary(List<ParticipantSummary> list, boolean isSnapshot) {
    var type = (isSnapshot) ? "snapshots" : "database";
    Path outputPath = Path.of(outputPathName, "output", type, "participantSummary.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
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

  private void writeParticipantHistory(List<ParticipantHistory> list, boolean isSnapshot) {
    var type = (isSnapshot) ? "snapshots" : "database";
    Path outputPath = Path.of(outputPathName, "output", type, "participantHistory.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
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

  private List<ExerciseSummary> readExerciseSummaries() {
    var list = new ArrayList<ExerciseSummary>();
    Path inputPath = Path.of(inputPathName, "exerciseSummary.csv");

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

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        var exerciseSummary = new ExerciseSummary(fields);
        list.add(exerciseSummary);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " exerciseSummaries from: " + inputPath.toString());
    return list;
  }

  private List<ParticipantSummary> readParticipantSummaries() {
    var list = new ArrayList<ParticipantSummary>();
    Path inputPath = Path.of(inputPathName, "participantSummary.csv");

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

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        var participantSummary = new ParticipantSummary(fields);
        list.add(participantSummary);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " participantSummaries from: " + inputPath.toString());
    return list;
  }

  private List<ParticipantHistory> readParticipantHistories() {
    var list = new ArrayList<ParticipantHistory>();
    Path inputPath = Path.of(inputPathName, "participantHistory.csv");

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

      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        var participantHistory = new ParticipantHistory(fields);
        list.add(participantHistory);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " participantHistories from: " + inputPath.toString());
    return list;
  }

  static class ExerciseSummaryComparator implements Comparator<ExerciseSummary> {

    @Override
    public int compare(ExerciseSummary o1, ExerciseSummary o2) {
      int compare = o1.getDate().compareTo(o2.getDate());
      if (compare != 0) {
        return compare;
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
