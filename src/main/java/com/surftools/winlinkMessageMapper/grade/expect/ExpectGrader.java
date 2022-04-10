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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.IGrader;

public class ExpectGrader implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ExpectGrader.class);

  private List<ExpectProcessor> processors = null;
  private final MessageType messageType;

  /**
   * gradeKey: <type>:expect:<comma-delimited-list-of-files>;<another blob>;<another blob>
   *
   * @param gradeKey
   * @param messageType
   */
  public ExpectGrader(String gradeKey, MessageType messageType) {
    this.messageType = messageType;

    var filenames = new ArrayList<String>();
    processors = new ArrayList<ExpectProcessor>();

    var blobs = gradeKey.split(";");
    for (String blob : blobs) {
      if (!blob.contains("expect")) {
        logger.info("skipping blob: " + blob + ", not an expect blob");
        continue;
      }

      var fields = blob.split(":");
      if (fields.length != 3) {
        logger.error("skipping blob: " + blob + ", wrong number of fields");
        continue;
      }

      var blobMessageType = MessageType.fromString(fields[0]);
      if (blobMessageType == null) {
        logger.info("skipping blob: " + blob + ", unsuppported messageType");
      }

      if (blobMessageType != messageType) {
        logger.info("skipping blob: " + blob + ", wrong messageType");
      }

      filenames.addAll(Arrays.asList(fields[2]));
      logger.info("processing blob: " + blob);
    } // end loop over blobs
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
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getPostProcessReport(List<ExportedMessage> messages) {
    // TODO Auto-generated method stub
    return null;
  }

}
