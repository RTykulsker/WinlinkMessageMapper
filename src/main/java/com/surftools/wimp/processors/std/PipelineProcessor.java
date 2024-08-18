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

package com.surftools.wimp.processors.std;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PipelineProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PipelineProcessor.class);

  private static final String[] PREFIXES = new String[] { //
      "com.surftools.wimp.processors.std.", //
      "com.surftools.wimp.processors.eto_2024.", //
      "com.surftools.wimp.processors.miro.", //
      "com.surftools.wimp.processors.other.", //
      "com.surftools.wimp.processors.dev.", //
      "com.surftools.wimp.processors.eto_2023.", //
      "com.surftools.wimp.processors.eto_2022.", //
      "" };

  private static final String[] SUFFIXES = new String[] { "Processor", "" };

  protected IConfigurationManager cm;
  protected IMessageManager mm;

  private List<IProcessor> processors;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    if (mm == null) {
      mm = new MessageManager();
    }
    super.initialize(cm, mm, logger);

    // fail fast: our working directory, where our input files are
    Path path = Paths.get(pathName);
    if (!Files.exists(path)) {
      logger.error("specified path: " + pathName + " does not exist");
      System.exit(1);
    } else {
      logger.info("WinlinkMessageMapper, starting with input path: " + path);
    }

    var dumpIdsSet = makeDumpIds(cm.getAsString(Key.DUMP_IDS));
    mm.putContextObject("dumpIds", dumpIdsSet);

    var stdin = cm.getAsString(Key.PIPELINE_STDIN, "Read,Classifier,ExplicitRejection,Deduplication");
    var main = cm.getAsString(Key.PIPELINE_MAIN);
    var stdout = cm
        .getAsString(Key.PIPELINE_STDOUT, "CsvColumnCutter,CsvColumnHeaderRename,Write,MissingDestination,Summary");

    var processorNames = new ArrayList<String>();
    for (var configName : new String[] { stdin, main, stdout }) {
      if (configName == null) {
        continue;
      }

      var fields = configName.split(",");
      for (var field : fields) {
        processorNames.add(field);
      }
    }

    processors = new ArrayList<IProcessor>(processorNames.size());
    for (var processorName : processorNames) {
      var processor = findProcessor(processorName);
      if (processor == null) {
        throw new RuntimeException("Could not find processor for " + processorName);
      } else {
        processor.initialize(cm, mm);
        processors.add(processor);
      }
    }

  }

  public Set<String> makeDumpIds(String dumpIdsString) {
    Set<String> set = new HashSet<>();
    if (dumpIdsString != null) {
      String[] fields = dumpIdsString.split(",");
      for (var field : fields) {
        set.add(field.toUpperCase());
      }
      logger.info("dumpIds: " + String.join(",", set));
    }
    return set;
  }

  private IProcessor findProcessor(String processorName) {
    IProcessor processor = null;
    for (var prefix : PREFIXES) {
      for (var suffix : SUFFIXES) {
        var className = prefix + processorName + suffix;
        logger.debug("searching for className: " + className);
        try {
          var clazz = Class.forName(className);
          if (clazz != null) {
            processor = (IProcessor) clazz.getDeclaredConstructor().newInstance();
            logger.debug("found  className: " + className);
            return processor;
          }
        } catch (Exception e) {
          ;
        }
      } // end loop over suffixes
    } // end loop over prefixes
    throw new RuntimeException("Could not find a processor for: " + processorName);
  }

  @Override
  public void process() {
    for (var processor : processors) {
      processor.process();
    }
  }

  @Override
  public void postProcess() {
    for (var processor : processors) {
      processor.postProcess();
    }
  }

}
