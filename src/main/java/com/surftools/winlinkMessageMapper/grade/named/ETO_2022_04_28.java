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

package com.surftools.winlinkMessageMapper.grade.named;

import java.util.List;

import com.surftools.winlinkMessageMapper.dto.message.CheckInMessage;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.grade.DefaultGrader;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

/**
 * alternative grader on a Winlink Check In message in lieu of Position Report
 *
 * @author bobt
 *
 */
public class ETO_2022_04_28 implements IGrader {

  @Override
  public GradeResult grade(GradableMessage gm) {
    if (!(gm instanceof CheckInMessage)) {
      return null;
    }

    CheckInMessage m = (CheckInMessage) gm;
    var grade = "";
    String explanation = null;

    var comments = m.comments;
    if (comments != null && comments.equals("Alternate Exercise for 4/28/2022")) {
      grade = "Alternative OK";
    } else {
      grade = "Not graded";
    }

    return new GradeResult(grade, explanation);
  }

  @Override
  public GraderType getGraderType() {
    return GraderType.WHOLE_MESSAGE;
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<ExportedMessage> messages) {
    return DefaultGrader.defaultPostProcessReport(messages);
  }

}