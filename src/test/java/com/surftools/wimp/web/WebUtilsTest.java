/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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

package com.surftools.wimp.web;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch.Operation;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

public class WebUtilsTest {

  private static final Logger logger = LoggerFactory.getLogger(WebUtilsTest.class);

  @Test
  public void test_makeInitialPageHtml() {
    var textFromFile = "";
    var textFromProgram = WebUtils.makeInitialPageHtml();
    logger.info("read " + textFromProgram.length() + " characters from internal");

    try {
      var cm = new PropertyFileConfigurationManager("webConfig.txt", Key.values());
      var pathName = cm.getAsString(Key.PATH);
      var htmlPath = Path.of(pathName, "index.html");
      textFromFile = Files.readString(htmlPath);

      logger.info("read " + textFromFile.length() + " characters from file");

      // https://www.baeldung.com/java-difference-between-two-strings
      DiffMatchPatch dmp = new DiffMatchPatch();
      var diffs = dmp.diffMain(textFromFile, textFromProgram, false);
      var outputLineCount = 0;
      for (var diff : diffs) {

        if (diff.operation.equals(Operation.EQUAL)) {
          continue;
        }

        if (diff.operation.equals(Operation.DELETE) || diff.operation.equals(Operation.INSERT)) {
          var text = diff.text;
          if (text.isBlank()) {
            continue;
          }
        }
        System.out.println(diff.toString());
        ++outputLineCount;
      }

      assertEquals(0, outputLineCount);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
