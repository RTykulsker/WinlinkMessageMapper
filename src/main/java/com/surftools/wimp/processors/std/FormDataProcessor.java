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

package com.surftools.wimp.processors.std;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class FormDataProcessor extends AbstractBaseProcessor {

  protected List<FormDataSummary> summaries = new ArrayList<>();
  protected ClassifierProcessor myClassifier;

  record FormDataSummary(String from, String messageId, String subject, String msgDateTime, String type,
      String mapFileName, //
      String formDataPresent, String formDataContent, String sourceFile) implements IWritableTable {

    @Override
    public int compareTo(IWritableTable other) {
      var o = (FormDataSummary) other;
      int cmp = formDataPresent.compareTo(o.formDataPresent);
      if (cmp != 0) {
        return cmp;
      }

      cmp = from.compareTo(o.from);
      if (cmp != 0) {
        return cmp;
      }
      cmp = messageId.compareTo(o.messageId);
      return cmp;
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "MessageId", "Subject", "From", "Date-Time", "Type", "MapFileName", //
          "FormData?", "Content", "Source" };
    }

    @Override
    public String[] getValues() {
      return new String[] { messageId, subject, from, msgDateTime, type, mapFileName, //
          formDataPresent, formDataContent, sourceFile };

    }
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm);
    myClassifier = new ClassifierProcessor();
    myClassifier.initialize(cm, mm);
  }

  @Override
  public void process() {
    for (var m : mm.getOriginalMessages()) {
      FormDataSummary summary = null;
      var attachments = m.attachments;
      var content = "";
      var mapFileName = "";
      var dateTime = m.msgDateTime.toString();
      var type = myClassifier.findMessageType(m).toString().toLowerCase();
      if (attachments != null) {
        var hasFormData = false;
        for (var key : attachments.keySet()) {
          if (key.equals("FormData.txt")) {
            content = new String(attachments.get(key));
            var lines = content.split("\n");
            for (var line : lines) {
              if (line.startsWith("MapFileName")) {
                var fields = line.split("=");
                mapFileName = fields[1];
                break;
              }
            }
            hasFormData = true;
            break;
          }
        }
        if (hasFormData) {
          summary = new FormDataSummary(m.from, m.messageId, m.subject, dateTime, type, mapFileName, //
              "true", content, m.fileName);
        } else {
          summary = new FormDataSummary(m.from, m.messageId, m.subject, dateTime, type, mapFileName, //
              "false", content, m.fileName);
        }
      } else {
        summary = new FormDataSummary(m.from, m.messageId, m.subject, dateTime, type, mapFileName, //
            "false", content, m.fileName);
      }
      summaries.add(summary);
    }

  }

  @Override
  public void postProcess() {
    super.postProcess();
    Collections.sort(summaries);
    writeTable("formDataSummay.csv", summaries);
  }

}
