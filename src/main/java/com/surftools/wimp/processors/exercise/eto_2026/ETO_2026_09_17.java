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

package com.surftools.wimp.processors.exercise.eto_2026;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IExportedMessageEditor;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * a Winlink ICS-213 message, with a CSV and an image attached
 *
 * @author bobt
 *
 */
public class ETO_2026_09_17 extends SingleMessageFeedbackProcessor implements IExportedMessageEditor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_09_17.class);

  private String[] referenceHeaders;
  private String referenceHeadersString;
  private Map<String, String> referenceMap = new LinkedHashMap<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_213;
    var extraOutboundMessageText = getNextExerciseInstructions();
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;

    var refCsvPath = Path.of(inputPathName, "reference.csv");
    try {
      var refList = ReadProcessor.readCsvFileIntoFieldsArray(refCsvPath, ',', false, 0);
      referenceHeaders = refList.get(0);
      referenceHeadersString = String.join(",", referenceHeaders);
      for (var i = 1; i < refList.size(); ++i) {
        var values = refList.get(i);
        referenceMap.put(values[0], String.join(",", values));
      }
      logger.info("read " + referenceMap.size() + " entries from reference file: " + refCsvPath.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Ics213Message) message;
    // the usual stuff
    count(sts
        .testStartsWith("Message Subject should start with #EV", "ICS-213: ETO Winlink Thursday Participant",
            m.subject));
    count(sts.test("Message Location should be valid", m.msgLocation.isValid(), m.msgLocation.toString()));
    count(sts.test("Form Location should be valid", m.formLocation.isValid(), m.formLocation.toString()));
    count(sts.test("Organization Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Is Exercise should be checked", m.isExercise));
    count(sts.test("Incident Name should be #EV", "ETO Blood Availability", m.incidentName));
    count(sts.test("Form To should be #EV", m.to, m.formTo));
    count(sts.test("Form From should be #EV", m.from + " / ETO Winlink Thursday Participant", m.formFrom));
    count(sts.test("Form Subject should be #EV", "ETO Exercise Sept 17, 2026", m.formSubject));
    count(sts.testIfPresent("Form Date should be present", m.formDate));
    count(sts.testIfPresent("Form Time should be present", m.formTime));
    count(sts.testIfPresent("Message body should be present", m.formMessage));
    count(sts.testEndsWith("Approved by should end with #EV", " / " + m.from, m.approvedBy));
    count(sts.test("Position/Title should be #EV", "ETO participant", m.position));

    // message body
    if (m.formMessage != null) {
      var lines = m.formMessage.split("\n");
      count(sts.test("Form Message body should contain exactly #EV lines", "2", String.valueOf(lines.length)));

      if (lines.length >= 1) {
        count(sts.test("Form Message body line 1 should be #EV", "3", lines[0]));
      } else {
        count(sts.test("Form Message body line 1 should be 3", false));
      }

      if (lines.length >= 2) {
        count(sts.test("Form Message body line 2 should be #EV", "225", lines[1]));
      } else {
        count(sts.test("Form Message body line 2 should be 225", false));
      }
    }

    // attachments
    int nJpgs = 0;
    int nCsvs = 0;
    for (var attachmentName : m.attachments.keySet()) {
      var lcName = attachmentName.toLowerCase();

      if (lcName.endsWith(".jpg") || lcName.endsWith(".jpeg")) {
        ++nJpgs;
        var nBytes = m.attachments.get(attachmentName).length;
        count(sts
            .test("JPG attachment size should be <= 50,000 bytes", (nBytes <= 1.05 * 50_000), String.valueOf(nBytes)));
      }

      if (lcName.endsWith("csv")) {
        ++nCsvs;
        var value = new String(m.attachments.get(attachmentName));
        var list = ReadProcessor.readCsvStringIntoFieldsArray(value, ',', false, 0);

        var headers = list.get(0);
        var headersString = String.join(",", headers);
        count(sts.test("CSV attachment headers should match #EV", referenceHeadersString, headersString));

        var localMap = new HashMap<String, String>();
        for (var i = 1; i < list.size(); ++i) {
          var values = list.get(i);
          var key = values[0];
          localMap.put(key, String.join(",", values));
        }

        for (var key : referenceMap.keySet()) {
          var refValues = referenceMap.get(key);
          var values = localMap.get(key);

          if (values != null) {
            count(sts.test("CSV line should match #EV", refValues, values));
          } else {
            count(sts.test("CSV line should match " + refValues, false, values));
          }
        } // end loop over keys in referenceMap
      } // end if CSV attachment
    } // end loop over attachments

    count(sts.test("Number of JPG attachments should be #EV", "1", String.valueOf(nJpgs)));
    count(sts.test("Number of CSV attachments should be #EV", "1", String.valueOf(nCsvs)));

  }

}