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

package com.surftools.winlinkMessageMapper.processor.message;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.WaResourceRequestMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;
import com.surftools.winlinkMessageMapper.grade.IGrader;
import com.surftools.winlinkMessageMapper.grade.expect.ExpectGrader;

public class WaResourceRequestProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WaResourceRequestProcessor.class);

  private final IGrader grader;

  public WaResourceRequestProcessor(String gradeKey) {

    if (gradeKey != null && gradeKey.startsWith(MessageType.WA_RR.toString() + ":" + "expect")) {
      grader = new ExpectGrader(gradeKey, MessageType.WA_RR);
    } else {
      grader = null;
    }
  }

  @Override
  public ExportedMessage process(ExportedMessage message) {
    try {

      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("exportedMessage: " + message);
      }

      String xmlString = new String(message.attachments.get(MessageType.WA_RR.attachmentName()));
      makeDocument(message.messageId, xmlString);

      var map = new HashMap<String, String>();

      for (String key : WaResourceRequestMessage.requestorTags) {
        String value = getStringFromXml(key);
        map.put(key, value);
      }

      for (String key : WaResourceRequestMessage.otherTags) {
        String value = getStringFromXml(key);
        map.put(key, value);
      }

      var m = new WaResourceRequestMessage(message, map);

      if (grader != null) {
        var result = grader.grade(m);
        if (result != null) {
          m.setIsGraded(true);
          m.setGrade(result.grade());
          m.setExplanation(result.explanation());
        }
      }

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
