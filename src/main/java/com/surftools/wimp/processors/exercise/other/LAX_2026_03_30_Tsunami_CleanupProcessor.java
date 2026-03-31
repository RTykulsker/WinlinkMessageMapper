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

package com.surftools.wimp.processors.exercise.other;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * because too much stuff is getting published
 */
public class LAX_2026_03_30_Tsunami_CleanupProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(LAX_2026_03_30_Tsunami_CleanupProcessor.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
  }

  @Override
  public void process() {
  }

  @Override
  public void postProcess() {
    final var keepList = List
        .of("2026-03-30-chart.html", //
            "2026-03-30-map-DYFI Squeezed GroupCounts.html", //
            "2026-03-30-map-DYFI_IsFelt.html", //
            "2026-03-30-map-InFloodZone.html", //
            "2026-03-30-map-InTsunamiZone.html", //
            "2026-03-30-map-WelfareStatus.html", //
            "2026-03-30-map-messageCount.html");

    var publishedDir = new File(publishedPath.toString());
    var publishedFiles = publishedDir.listFiles();
    for (var file : publishedFiles) {
      if (!file.isFile()) {
        continue;
      }

      var fileName = file.getName();
      if (keepList.contains(fileName)) {
        logger.info("keeping: " + fileName);
        continue;
      } else {
        try {
          Files.move(file.toPath(), Path.of(outputPathName, fileName));
          logger.info("moved: " + fileName + " to: " + outputPathName);
        } catch (Exception e) {
          logger.error("Exception moving file: " + file.toString(), e.getMessage());
        }
        continue;
      } // end if
    } // end loop over files
  } // end postProcess
}