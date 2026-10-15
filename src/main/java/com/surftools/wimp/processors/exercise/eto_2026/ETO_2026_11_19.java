/**

StatusTypeThe MIT License (MIT)

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

package com.surftools.wimp.processors.exercise.eto_2026;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Hics251Message;
import com.surftools.wimp.message.Hics251Message.StatusEntry;
import com.surftools.wimp.message.Hics251Message.StatusType;
import com.surftools.wimp.message.Hics251Message.SystemType;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * HICS-251
 *
 * @author bobt
 *
 */
public class ETO_2026_11_19 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_11_19.class);

  Map<String, StatusEntry> refStatusEntryMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.HICS_251;

    var refList = List
        .of(//
            new StatusEntry(SystemType.POWER.toString(), StatusType.PART,
                "Wide spread power line damage due to wind. 3 days for repair per Jo-Carroll Energy. On backup power."), //
            new StatusEntry(SystemType.LIGHT.toString(), StatusType.PART, "Backup power limits."), //
            new StatusEntry(SystemType.WATER.toString(), StatusType.FULL, "Using water storage tanks."), //
            new StatusEntry(SystemType.SEWER.toString(), StatusType.FULL, ""), //
            new StatusEntry(SystemType.NURSE.toString(), StatusType.NON,
                "Network cooling system disrupted. 1 day to fix cooling system power."), //
            new StatusEntry(SystemType.GAS.toString(), StatusType.FULL, ""), //
            new StatusEntry(SystemType.COMMS.toString(), StatusType.PART,
                "Network cooling system disrupted. 1 day to fix cooling system power. "));

    refStatusEntryMap = new HashMap<>();
    for (var entry : refList) {
      refStatusEntryMap.put(entry.system(), entry);
    }
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var m = (Hics251Message) message;

    count(sts.test("Incident Name should be #EV", "Derecho 26", m.incidentName));
    count(sts.test("Page Number should be #EV", "1", m.pageNumber));
    count(sts.test("Page Total should be #EV", "1", m.pageTotal));
    count(sts.test("Operational Period should be #EV", "1", m.operationalPeriod));
    count(sts.test("Operational Date From should be #EV", "2026-11-14", m.opFromDate));
    count(sts.test("Operational Date To should be #EV", "2026-11-20", m.opToDate));
    count(sts.test("Operational Time From should be #EV", "00:00", m.opFromTime));
    count(sts.test("Operational Time To should be #EV", "24:00", m.opToTime));

    count(sts.test("Department Name should be #EV", "Operations Section", m.departmentName));
    count(sts.test("Contact Number should be #EV", "815-777-1340", m.contactNumber));
    count(sts.test("Street Address should be #EV", "One Medical Center Drive", m.streetAddress));
    count(sts.test("City should be #EV", "Galena", m.city));
    count(sts.test("State should be #EV", "IL", m.state));
    count(sts.test("Zip Code should be #EV", "61036", m.zip));

    // status entries
    var mStatusEntryMap = new HashMap<String, StatusEntry>();
    for (var entry : m.statusEntries) {
      mStatusEntryMap.put(entry.system(), entry);
    }

    for (var systemName : Hics251Message.SYSTEM_NAMES) {
      var refEntry = refStatusEntryMap.get(systemName);
      var mEntry = mStatusEntryMap.get(systemName);
      count(sts.test(systemName + " Status should be #EV", refEntry.status().toString(), mEntry.status().toString()));
      if (refEntry.comments().isEmpty()) {
        count(sts.testIfEmpty(systemName + " Comments should be empty", mEntry.comments()));
      } else {
        count(sts.test_2line(systemName + " Comments should be #EV", refEntry.comments(), mEntry.comments()));
      }
    }

    count(sts
        .test_2line("Remarks should be #EV",
            "The facility management team discovered all outages including exhaust fan and duct work damage on the roof.",
            m.remarks));

    count(sts.test("Prepared By should be #EV", "Mathew Monker", m.preparedBy));
    count(sts.testIfPresent("Form Date/Time should be present", m.formDateTime));
    count(sts.test("Facility Name should be #EV", "Midwest Medical Center", m.facilityName));

    count(sts.test("Radio Operator should be present", "Rob Lowe", m.radioOperator));
    count(sts.test("Form Latitude should be #EV", "42.449085", m.formLocation.getLatitude()));
    count(sts.test("Form Longitude should be #EV", " -90.457806", m.formLocation.getLongitude()));

    // attached CSV
    var csvCount = 0;
    var attachments = m.attachments;
    for (var attachmentName : attachments.keySet()) {
      if (attachmentName.toLowerCase().strip().endsWith(".csv")) {
        ++csvCount;
        var bytes = attachments.get(attachmentName);
        var string = new String(bytes);
        var lines = string.split("\n");
        count(sts.test("attachment: " + attachmentName + " should have #EV lines", 2, lines.length));
        if (lines.length >= 1) {
          var header = lines[0];
          var fields = header.split(",");
          count(sts.test("attachments: " + attachmentName + " number of fields should be #EV", 30, fields.length));
        }
      }
    }
    count(sts.test("Number of CSV attachments should be #EV", 1, csvCount));
  }

  @Override
  public void postProcess() {
    super.postProcess();
  }

}