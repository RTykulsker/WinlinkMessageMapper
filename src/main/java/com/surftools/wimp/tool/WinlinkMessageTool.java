package com.surftools.wimp.tool;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.processors.std.PipelineProcessor;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

/**
 * This is the *main* class read multiple Winlink "Exported Message" files of messages, output parsed CSV message file
 * optionally de-duplicate, grade, aggregate, summarize
 *
 * Copyright (c) 2024, Robert Tykulsker
 */

public class WinlinkMessageTool {
  public static void main(String[] args) throws Exception {
    var pipeline = new PipelineProcessor();
    pipeline.initialize(new PropertyFileConfigurationManager(args[0], Key.values()), new MessageManager());
    pipeline.process();
    pipeline.postProcess();
  }
}
