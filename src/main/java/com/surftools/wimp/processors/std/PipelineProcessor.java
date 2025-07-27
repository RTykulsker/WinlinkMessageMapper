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

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

public class PipelineProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(PipelineProcessor.class);

  // the processors that make up the pipeline
  private List<IProcessor> processors;

  // default no-args constructor
  public PipelineProcessor() {
  }

  // code-golfing constructor
  public PipelineProcessor(String configurationFileName) throws Exception {
    initialize(new PropertyFileConfigurationManager(configurationFileName, Key.values()), null);
    process();
    postProcess();
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager _mm) {
    if (_mm == null) {
      _mm = new MessageManager();
    }
    super.initialize(cm, _mm, logger);

    var stdin = cm.getAsString(Key.PIPELINE_STDIN, "Read,Classifier,Acknowledgement,Deduplication,Filter");
    var main = cm.getAsString(Key.PIPELINE_MAIN, ""); // exercise-specific processors go here!
    var stdout = cm.getAsString(Key.PIPELINE_STDOUT, "Write,MissingDestination,Summary");
    var processorString = String.join(",", List.of(stdin, main, stdout));
    var processorNames = Arrays.stream(processorString.split(",")).filter(s -> isValidProcessorName(s)).toList();
    processors = processorNames.stream().map(pn -> findProcessor(pn)).toList();
    logger.info("Processors: " + String.join(",", processorNames));

    processors.stream().forEach(p -> p.initialize(cm, mm));
  }

  @Override
  public void process() {
    processors.stream().forEach(p -> p.process());
  }

  @Override
  public void postProcess() {
    processors.stream().forEach(p -> p.postProcess());
  }

  private boolean isValidProcessorName(String s) {
    return s != null && !s.isEmpty() && !s.equals("(null");
  }

  private IProcessor findProcessor(String processorName) {
    // this seems a good balance between streams and code-golfing
    final var PREFIXES = List
        .of( //
            "com.surftools.wimp.processors.std.", //
            "com.surftools.wimp.processors.exercise.eto_2025.", //
            "com.surftools.wimp.practice.", //
            "com.surftools.wimp.processors.exercise.miro.", //
            "com.surftools.wimp.processors.exercise.other.", //
            "com.surftools.wimp.processors.lineBased.", //
            "com.surftools.wimp.processors.dev.", //
            "com.surftools.wimp.processors.exercise.eto_2024.", //
            "com.surftools.wimp.processors.exercise.eto_2023.", //
            "com.surftools.wimp.processors.exercise.eto_2022.", //
            "");
    final var SUFFIXES = List.of("Processor", "");

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
}
