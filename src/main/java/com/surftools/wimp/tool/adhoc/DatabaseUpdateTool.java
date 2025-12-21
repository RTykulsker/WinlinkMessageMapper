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

package com.surftools.wimp.tool.adhoc;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.databaseV2.CsvDatabaseEngine;
import com.surftools.wimp.processors.std.PipelineProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

public class DatabaseUpdateTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(DatabaseUpdateTool.class);

  @Option(name = "--configurationsFile", usage = "path to configuratiosn file", required = true)
  private String configurationsFileName = "configurations.txt";

  public static void main(String[] args) {
    var tool = new DatabaseUpdateTool();
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
    logger.info("begin update");
    try {
      var path = Path.of(configurationsFileName);
      var file = new File(path.toString());
      var parentDir = file.getParent();
      var configurations = Files.readAllLines(path);
      logger.info("read " + configurations.size() + " configuration file names from file " + configurationsFileName);
      for (var configurationFileName : configurations) {
        var configPath = Path.of(parentDir, configurationFileName);
        var cm = new PropertyFileConfigurationManager(configPath.toString(), Key.values());

        var inputPathName = AbstractBaseProcessor.inputPathName;
        logger.info("config file: " + configurationFileName + ", input path: " + inputPathName);
        var inputDir = new File(inputPathName);
        if (!inputDir.exists()) {
          logger.error("### ### ### configuration file: " + configurationFileName + ", inputPath: " + inputPathName);
        }

        var exerciseOutputPath = Path.of(parentDir, "exerciseDbOutput");
        var newDatabasePath = Path.of(parentDir, "database");

        // do some SERIOUS editing on the configuration

        cm.putString(Key.NEW_DATABASE_PATH, newDatabasePath.toString());

        cm.putString(Key.PIPELINE_STDIN, "Read,Classifier,Deduplication");
        cm.putString(Key.PIPELINE_MAIN, "DatabaseUpdate"); // exercise-specific processors go here!
        cm.putString(Key.PIPELINE_STDOUT, "Write");

        var pipeline = new PipelineProcessor();
        pipeline.initialize(cm, null);
        pipeline.process();
        pipeline.postProcess();

        // copy the 4 database files from the exercise output folder to the
        for (var filename : CsvDatabaseEngine.FILE_NAMES) {
          var sourcePath = Path.of(exerciseOutputPath.toString(), "newDatabase", filename);
          var targetPath = Path.of(newDatabasePath.toString(), filename);
          try {
            var targetFile = new File(targetPath.toString());
            if (targetFile.exists()) {
              Files.delete(targetPath);
            }
            Files.move(sourcePath, targetPath);
          } catch (Exception e) {
            e.printStackTrace();
          }
          logger.info("*** moved source: " + sourcePath.toString() + ", to target: " + targetPath.toString());
        }

      }
    } catch (Exception e) {
      logger.error("Exception: " + e.getLocalizedMessage());
    }
    logger.info("end update");
  }
}
