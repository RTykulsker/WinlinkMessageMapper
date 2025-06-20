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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.processors.std.PipelineProcessor;
import com.surftools.wimp.utils.config.impl.MemoryConfigurationManager;

public class PracticeProcessorTool {
  public static final String REFERENCE_MESSAGE_KEY = "referenceMessage";

  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(PracticeProcessorTool.class);

  @Option(name = "--exerciseDate", usage = "date of practice exercise in yyyy-MM-dd format", required = true)
  private String exerciseDateString = null;

  @Option(name = "--pathName", usage = "path name where input file is located", required = false)
  private String pathName = null;

  @Option(name = "--referencePathName", usage = "path name where reference input file is located", required = false)
  private String referencePathName = null;

  public static void main(String[] args) {
    var tool = new PracticeProcessorTool();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() {
    logger.info("begin run");
    try {
      var exerciseDate = LocalDate.parse(exerciseDateString);
      if (exerciseDate.getDayOfWeek() != DayOfWeek.THURSDAY) {
        throw new RuntimeException("Exercise Date: " + exerciseDateString + " must be a THURSDAY");
      }

      var ord = PracticeUtils.getOrdinalDayOfWeek(exerciseDate);
      var ordinalList = new ArrayList<Integer>(PracticeGeneratorTool.VALID_ORDINALS);
      Collections.sort(ordinalList);
      var ordinalLabels = ordinalList.stream().map(i -> PracticeUtils.getOrdinalLabel(i)).toList();
      if (!PracticeGeneratorTool.VALID_ORDINALS.contains(ord)) {
        throw new RuntimeException("Exercise Date: " + exerciseDate.toString() + " is NOT one of "
            + String.join(",", ordinalLabels) + " THURSDAYS");
      }

      var messageType = PracticeGeneratorTool.MESSAGE_TYPE_MAP.get(ord);
      logger
          .info("Exercise Date: " + exerciseDate.toString() + ", " + PracticeUtils.getOrdinalLabel(ord)
              + " Thursday; exercise message type: " + messageType.toString());

      if (pathName == null) {
        pathName = System.getProperty("user.dir");
      }
      logger.info("pathName: " + pathName);

      // fail fast on reading reference
      if (referencePathName == null) {
        referencePathName = Path.of(pathName, "../reference/" + exerciseDateString).toString();
      }
      logger.info("referencePathName: " + referencePathName);
      var referencePath = Path.of(referencePathName, messageType.toString() + ".json");
      var jsonString = Files.readString(referencePath);
      var deserializer = new PracticeJsonMessageDeserializer();
      var referenceMessage = deserializer.deserialize(jsonString, messageType);

      var cm = new MemoryConfigurationManager(Key.values());

      // create our configuration on the fly
      cm.putString(Key.EXERCISE_DATE, exerciseDateString);
      cm.putString(Key.PATH, pathName);
      cm.putBoolean(Key.OUTPUT_PATH_CLEAR_ON_START, true);
      cm.putString(Key.EXPECTED_MESSAGE_TYPES, messageType.toString());
      // TODO EXERCISE_NAME, Windows, database
      // cm.putString(Key.NEW_DATABASE_PATH, newDatabasePath.toString());

      cm.putString(Key.PIPELINE_STDIN, "Read,Classifier,Deduplication");
      cm.putString(Key.PIPELINE_MAIN, "PracticeProcessor"); // exercise-specific processors go here!
      cm.putString(Key.PIPELINE_STDOUT, "Write");

      var mm = new MessageManager();
      mm.putContextObject(REFERENCE_MESSAGE_KEY, referenceMessage);

      var pipeline = new PipelineProcessor();
      pipeline.initialize(cm, mm);
      pipeline.process();
      pipeline.postProcess();

    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
      e.printStackTrace();
    }
    logger.info("end run");
  }
}
