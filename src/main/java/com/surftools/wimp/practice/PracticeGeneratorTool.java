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

package com.surftools.wimp.practice;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.surftools.utils.FileUtils;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;

/**
 * Program to generate many weeks work "data" for ETO weekly "practice" semi-automatic exercises
 *
 * NOTE WELL: since this program will be run about once per year, there's no need for data-driven configuration
 */
public class PracticeGeneratorTool {
  public final static Map<Integer, MessageType> MESSAGE_TYPE_MAP = Map
      .of(1, MessageType.ICS_213, 2, MessageType.ICS_213_RR, 4, MessageType.ICS_205_RADIO_PLAN, 5,
          MessageType.FIELD_SITUATION);
  public final static Set<Integer> VALID_ORDINALS = MESSAGE_TYPE_MAP.keySet();

  private static final Logger logger = LoggerFactory.getLogger(PracticeGeneratorTool.class);
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  @Option(name = "--outputDirName", usage = "path to output directory", required = true)
  private String outputDirName = null;

  @Option(name = "--rngSeed", usage = "random number generator seed", required = false)
  private Long rngSeed = null;

  private final DayOfWeek TARGET_DOW = DayOfWeek.THURSDAY;
  private final int N_TO_GENERATE = 50;

  private final String NA = "n/a";

  private Random rng;

  public static void main(String[] args) {
    var app = new PracticeGeneratorTool();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  private void run() {
    logger.info("begin run");
    logger.info("outputDir: " + outputDirName);
    FileUtils.deleteDirectory(Path.of(outputDirName));
    FileUtils.createDirectory(Path.of(outputDirName));

    if (rngSeed != null) {
      rng = new Random(rngSeed);
      logger.info("initializing rng with seed: " + rngSeed);
    } else {
      rng = new Random();
      logger.info("initializing rng randomly");
    }

    var startDate = LocalDate.now();
    var date = startDate;
    while (true) {
      var ord = PracticeUtils.getOrdinalDayOfWeek(date);
      if (date.getDayOfWeek() == TARGET_DOW && VALID_ORDINALS.contains(ord)) {
        break;
      }
      date = date.plusDays(1);
    }
    date = date.minusDays(7);

    for (var nGenerated = 1; nGenerated <= N_TO_GENERATE; ++nGenerated) {
      date = date.plusDays(7);
      var ord = PracticeUtils.getOrdinalDayOfWeek(date);
      if (ord == 3) {
        continue;
      }
      generate(date, ord);
    }
    logger.info("end run");
  }

  private void generate(LocalDate date, int ord) {
    var messageType = MESSAGE_TYPE_MAP.get(ord);
    var path = Path.of(outputDirName, date.toString());
    FileUtils.createDirectory(path);
    switch (messageType) {
    case ICS_213:
      handle_Ics213(date, ord, path);
      break;
    case ICS_213_RR:
      handle_Ics213RR(date, ord, path);
      break;
    case ICS_205_RADIO_PLAN:
      handle_Ics205(date, ord, path);
      break;
    case FIELD_SITUATION:
      handle_Fsr(date, ord, path);
      break;
    default:
      throw new RuntimeException("unsupported messageType: " + messageType.toString());
    }
  }

  private String makeMessageId(String prefix, LocalDateTime dateTime) {
    final var dtf = DateTimeFormatter.ofPattern("MMddHHmmss");
    return prefix + dtf.format(dateTime);
  }

  private ExportedMessage makeExportedMessage(LocalDate date) {
    LocalDateTime dateTime = LocalDateTime.of(date, LocalTime.of(12, 0));
    var messageId = makeMessageId("PX", dateTime);
    var from = NA;
    var source = NA;
    var to = NA;
    var toList = NA;
    var ccList = NA;
    var subject = NA;

    var msgLocation = LatLongPair.ZERO_ZERO;
    var locationSource = NA;
    var mime = NA;
    var plainContent = NA;
    Map<String, byte[]> attachments = new HashMap<String, byte[]>();
    boolean isP2p = false;
    String fileName = NA;
    List<String> lines = null;

    var exportedMessage = new ExportedMessage(messageId, from, source, to, toList, ccList, //
        subject, dateTime, //
        msgLocation, locationSource, //
        mime, plainContent, attachments, isP2p, fileName, lines);

    return exportedMessage;
  }

  private void handle_Ics213(LocalDate date, int ord, Path path) {
    var exportedMessage = makeExportedMessage(date);

    String organization = "EmComm Training Organization";
    String incidentName = NA; // // TODO fixme
    String formFrom = NA; // TODO fixme
    String formTo = NA; // TODO fixme
    String formSubject = NA; // TODO fixme
    String formDate = NA;
    String formTime = NA; //
    String formMessage = NA;// TODO fixme
    String approvedBy = NA;
    String position = NA; //
    boolean isExercise = true;
    LatLongPair formLocation = LatLongPair.ZERO_ZERO;
    String version = NA;
    String dataSource = NA;

    var m = new Ics213Message(exportedMessage, organization, incidentName, //
        formFrom, formTo, formSubject, formDate, formTime, //
        formMessage, approvedBy, position, //
        isExercise, formLocation, version, dataSource);

    var objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    try {
      var json = objectMapper.writeValueAsString(m);
      Files.writeString(Path.of(path.toString(), "ics_213.json"), json);
    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", ICS_213");
  }

  private void handle_Ics213RR(LocalDate date, int ord, Path path) {
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    var typeNameFile = new File(Path.of(path.toString(), "ics_213RR.txt").toString());
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", ICS_213RR");
  }

  private void handle_Ics205(LocalDate date, int ord, Path path) {
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    var typeNameFile = new File(Path.of(path.toString(), "ics_205.txt").toString());
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", ICS_205");
  }

  private void handle_Fsr(LocalDate date, int ord, Path path) {
    var ordName = PracticeUtils.getOrdinalLabel(ord);
    var typeNameFile = new File(Path.of(path.toString(), "fsr.txt").toString());
    logger.info("generated date: " + date + ", " + ordName + " " + date.getDayOfWeek().toString() + ", FSR");
  }
}
