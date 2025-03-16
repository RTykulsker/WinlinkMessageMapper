package com.surftools.wimp.tool;

import com.surftools.wimp.processors.std.PipelineProcessor;

/**
 * This is the *main* class read multiple Winlink "Exported Message" files of messages, output parsed CSV message file
 * optionally de-duplicate, grade, aggregate, summarize
 *
 * Copyright (c) 2024, Robert Tykulsker
 */

public class WinlinkMessageMiniTool {
  public static void main(String[] args) throws Exception {
    new PipelineProcessor(args[0]);
  }
}
