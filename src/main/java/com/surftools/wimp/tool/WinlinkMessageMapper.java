/**

The MIT License (MIT)

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

package com.surftools.wimp.tool;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.processors.std.PipelineProcessor;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

/**
 * This is the *main* class
 *
 * read a bunch of Winlink "Exported Message" files of messages, output single CSV message file
 *
 * optionally de-duplicate, grade, aggregate, summarize
 *
 * @author bobt
 *
 */

public class WinlinkMessageMapper {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(WinlinkMessageMapper.class);

  @Option(name = "--configurationFile", usage = "path to configuration file", required = false)
  private String configurationFileName = "configuration.txt";

  public static void main(String[] args) {
    WinlinkMessageMapper app = new WinlinkMessageMapper();
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
   * main processing function
   */
  private void run() {
    try {
      logger.info("begin");

      var pipeline = new PipelineProcessor();
      pipeline
          .initialize(new PropertyFileConfigurationManager(configurationFileName, Key.values()), new MessageManager());
      pipeline.process();
      pipeline.postProcess();

      logger.info("exiting");
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

}
