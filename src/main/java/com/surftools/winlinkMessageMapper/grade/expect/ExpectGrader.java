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

package com.surftools.winlinkMessageMapper.grade.expect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

public class ExpectGrader implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ExpectGrader.class);

  private List<ExpectProcessor> processors = null;
  private final MessageType messageType;

  /**
   * gradeKey: <type>:expect:<comma-delimited-list-of-files>
   *
   * @param messageType
   *
   * @param gradeKey
   *
   */
  public ExpectGrader(MessageType messageType, String gradeKey) {
    this.messageType = messageType;

    var filenames = new ArrayList<String>();
    processors = new ArrayList<ExpectProcessor>();

    var fields = gradeKey.split(",");

    filenames.addAll(Arrays.asList(fields));

    logger.info("extracted " + filenames.size() + " filenames from gradeKey: " + gradeKey);

    for (var filename : filenames) {
      var dao = new CsvExpectDao(filename);
      var list = dao.readAll();
      var processor = new ExpectProcessor(filename, list);
      processors.add(processor);
    }
  }

  @Override
  public GradeResult grade(GradableMessage m) {
    if (m.getMessageType() != messageType || processors == null || processors.size() == 0) {
      return null;
    }
    var message = (ExpectGradableMessage) m;

    var bestScore = Integer.MIN_VALUE;
    GradeResult bestResult = null;
    ExpectProcessor bestProcessor = null;

    for (ExpectProcessor processor : processors) {
      var result = processor.grade(message);
      if (result != null) {
        var score = Integer.parseInt(result.grade());
        if (score > bestScore) {
          bestScore = score;
          bestResult = result;
          bestProcessor = processor;
        } // end if better
      } // end if result not null
    } // end loop over processors

    if (bestResult != null) {
      logger.info("best result from processor: " + bestProcessor.getName() + ", score: " + bestScore);
    }

    return bestResult;
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    return null;
  }

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }

  @Override
  public void setDumpIds(Set<String> dumpIds) {
  }

  @Override
  public void setConfigurationManager(IConfigurationManager cm) {
  }

}
