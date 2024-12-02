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

package com.surftools.wimp.processors.eto_2025;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.radioUtils.Frequency;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics205RadioPlanMessage;
import com.surftools.wimp.message.Ics205RadioPlanMessage.RadioEntry;
import com.surftools.wimp.processors.std.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * ICS-205 for Winter Field Day (WFD)
 *
 * @author bobt
 *
 */
public class ETO_2025_01_16 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2025_01_16.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.ICS_205_RADIO_PLAN;
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    Ics205RadioPlanMessage m = (Ics205RadioPlanMessage) message;

    count(sts.test("Agency/Group Name should be #EV", "EmComm Training Organization", m.organization));
    count(sts.test("Incident Name should be #EV", "Winter Field Day 2025", m.incidentName));

    var dateTimePrepared = parseDateTime(m.dateTimePrepared, List.of(DTF, ALT_DTF));
    if (dateTimePrepared == null) {
      count(sts.test("Date/Time Prepared should be present and in exercise window", false));
    } else {
      count(sts.testOnOrAfter("Date/Time Prepared should be on or after", windowOpenDT, dateTimePrepared, DTF));
      count(sts.testOnOrBefore("Date/Time Prepared should be on or before", windowCloseDT, dateTimePrepared, DTF));
    }

    count(sts.test("Operational Period Date From should be #EV", "January 25", m.dateFrom));
    count(sts.test("Operational Period Date To should be #EV", "January 26", m.dateTo));
    count(sts.test("Operational Period Time From should be #EV", "1600 UTC", m.timeFrom));
    count(sts.test("Operational Period Time To should be #EV", "2159 UTC", m.timeTo));

    handleRadioEntries(m, m.radioEntries);
    sts.setExplanationPrefix("");

    var specialInstructions = m.specialInstructions == null ? "" : m.specialInstructions;
    var specialInstructionFields = specialInstructions.split(" ");
    var nSpecialInstructionFields = specialInstructionFields.length;
    count(sts
        .test("Special Instructions should have 4 fields", nSpecialInstructionFields == 4,
            String.valueOf(nSpecialInstructionFields)));
    count(sts
        .test("Special Instructions Field #1 should match call sign",
            specialInstructionFields[0].equalsIgnoreCase(m.from)));

    var approvedBy = m.approvedBy == null ? "" : m.approvedBy;
    var approvedByFields = approvedBy.split(",| "); // comma OR space
    var nApprovedByFields = approvedByFields.length;
    var areThere2Fields = approvedByFields.length == 2;
    count(
        sts.test("Approved By should have two fields", areThere2Fields, String.valueOf(nApprovedByFields), approvedBy));
    count(sts.test("Approved By Field #2 should match call sign", approvedByFields[1].equalsIgnoreCase(m.from)));

    var dateTimeApproved = parseDateTime(m.dateTimePrepared, List.of(DTF, ALT_DTF));
    if (dateTimeApproved == null) {
      count(sts.test("Date/Time Approved should be present and in exercise window", false));
    } else {
      count(sts.testOnOrAfter("Date/Time Approved should be on or after", windowOpenDT, dateTimeApproved, DTF));
      count(sts.testOnOrBefore("Date/Time Approved should be on or before", windowCloseDT, dateTimeApproved, DTF));
    }

    count(sts.test("IAP Page should be #EV", "5", m.iapPage));
  }

  private void handleRadioEntries(Ics205RadioPlanMessage m, List<RadioEntry> entries) {
    count(sts.test("Number of Radio Channels defined should be #EV", "10", String.valueOf(entries.size())));

    Frequency vhfRepeaterOffset = null;
    for (var index = 0; index < entries.size(); ++index) {
      var entry = entries.get(index);
      var lineNumber = index + 1;
      var baseExplanation = "entry #" + lineNumber;

      count(sts.testIfEmpty("Zone/Group should be empty", entry.zoneGroup()));
      count(sts.testIfEmpty("Channel # should be empty", entry.channelNumber()));

      if (lineNumber == 1 || lineNumber == 2) {
        sts.setExplanationPrefix(baseExplanation + " (" + (lineNumber == 1 ? "V" : "U") + "HF repeater): ");
        count(sts.test("Function should be #EV", "Talk-In Repeater", entry.function()));
        count(sts.testIfPresent("Channel Name should be present", entry.channelName()));
        count(sts.test("Assignment should be #EV", "Local amateurs", entry.assignment()));

        var rxFreq = new Frequency(entry.rxFrequency());
        count(sts.test("RX freq should be parsable", rxFreq.isValid(), entry.rxFrequency()));
        if (rxFreq.isValid()) {
          var rxBand = rxFreq.bandOf();
          if (rxBand != null) {
            if (lineNumber == 1) {
              count(sts.test("RX freq should be in VHF band", rxBand.isVHF(), entry.rxFrequency()));
            } else {
              count(sts.test("RX freq should be in UHF band", rxBand.isUHF(), entry.rxFrequency()));
            }
          }
        }
        var rxIsNarrowOrWide = isNarrowOrWide(entry.rxNarrowWide());
        count(sts.test("RX Narrow or Wide should be N or W", rxIsNarrowOrWide, entry.rxNarrowWide()));

        var txFreq = new Frequency(entry.txFrequency());
        count(sts.test("TX freq should be parsable", txFreq.isValid(), entry.txFrequency()));
        if (txFreq.isValid()) {
          var txBand = txFreq.bandOf();
          if (txBand != null) {
            if (lineNumber == 1) {
              count(sts.test("TX freq should be in VHF band", txBand.isVHF(), entry.txFrequency()));
            } else {
              count(sts.test("TX freq should be in UHF band", txBand.isUHF(), entry.txFrequency()));
            }
          }
        }

        var txIsNarrowOrWide = isNarrowOrWide(entry.txNarrowWide());
        count(sts.test("TX Narrow or Wide should be N or W", txIsNarrowOrWide, entry.txNarrowWide()));

        count(sts.test("Mode should be A, D, or M", isModeValid(entry.mode()), entry.mode()));
        count(sts.testIfEmpty("Remarks should be empty", entry.remarks()));

        if (lineNumber == 1) {
          if (rxFreq.bandOf().isVHF() && txFreq.bandOf().isVHF()) {
            vhfRepeaterOffset = txFreq.subtract(rxFreq);
          }
        }

        if (lineNumber == 2) {
          if (rxFreq.bandOf().isUHF() && txFreq.bandOf().isUHF()) {
            count(sts.test("UHF RX and TX frequency should be different", rxFreq != txFreq));
            if (vhfRepeaterOffset != null) {
              Frequency uhfRepeaterOffset = txFreq.subtract(rxFreq);
              var sum = Math.signum(vhfRepeaterOffset.getFrequencyHz())
                  + Math.signum(uhfRepeaterOffset.getFrequencyHz());
              count(sts.test("VHF and UHF repeater offset directions should be different", sum == 0));
            }
          }
        }

      } // end if VHF or UHF repeaters

      if (lineNumber == 3) {
        sts.setExplanationPrefix(baseExplanation + " (WX): ");
        var isCanadian = isCanadian(m.from);

        count(sts
            .test("Function should be #EV", isCanadian ? "ECCC Weatheradio Info" : "NWS Weather Info",
                entry.function()));
        count(sts.testIfPresent("Channel Name should be present", entry.channelName()));
        count(sts.test("Assignment should be #EV", "All units/agencies", entry.assignment()));
        count(sts.test("RX freq should be a WX frequency", isWxFrequency(entry.rxFrequency()), entry.rxFrequency()));
        count(sts.test("RX Narrow or Wide should be #EV", "W", entry.rxNarrowWide()));
        count(sts.testIfEmpty("RX Tone should be blank", entry.rxTone()));
        count(sts.testIfEmpty("TX Freq should be present", entry.txFrequency()));
        count(sts.testIfEmpty("TX N or W should be blank", entry.txNarrowWide()));
        count(sts.testIfEmpty("TX Tone should be blank", entry.txTone()));
        count(sts.test("Mode should be #EV", "A", (entry.mode() == null ? "(blank)" : entry.mode())));

        if (isCanadian) {
          count(sts.test("Remarks should be #EV", "ECCC Weatheradio", entry.remarks()));
        } else {
          var endsWithNoaa = toKey(entry.remarks()).endsWith(toKey("NOAA Weather Radio"));
          count(sts.test("Remarks should end with NOAA Weather Radio", endsWithNoaa, entry.remarks()));
        }

      } // endif NOAA

      if (lineNumber == 4) {
        sts.setExplanationPrefix(baseExplanation + " (WL P2P): ");
        count(sts.test("Function should be #EV", "WL2K P2P", entry.function()));
        count(sts.testIfPresent("Channel Name should be present", entry.channelName()));
        count(sts.testIfPresent("Assignment should be present", entry.assignment()));
        var rxFreq = new Frequency(entry.rxFrequency());
        count(sts
            .test("RX freq should be an HF frequency", rxFreq.isValid() && rxFreq.bandOf().isHF(),
                entry.rxFrequency()));
        count(sts.testIfEmpty("RX Narrow or Wide should be blank", entry.rxNarrowWide()));
        count(sts.testIfEmpty("RX Tone should be blank", entry.rxTone()));
        var txFreq = new Frequency(entry.txFrequency());
        count(sts
            .test("TX Freq should be should be an HF frequency", txFreq.isValid() && txFreq.bandOf().isHF(),
                entry.txFrequency()));
        count(sts.testIfEmpty("TX Narrow or Wide should be blank", entry.txNarrowWide()));
        count(sts.testIfEmpty("TX Tone should be blank", entry.txTone()));
        count(sts.testIfEmpty("Remarks should be empty", entry.remarks()));
      } // endif HF Winlink P2P

      if (lineNumber >= 5 && lineNumber <= 10) {
        sts.setExplanationPrefix(baseExplanation + " (WFD HF Frequencies): ");
        count(sts
            .test("Function should start with Amateur", entry.function().toLowerCase().startsWith("amateur"),
                entry.function()));
        count(sts.testIfEmpty("Channel Name should be blank", entry.channelName()));
        count(sts.testIfPresent("Assignment should be present", entry.assignment()));
        var rxFreq = new Frequency(entry.rxFrequency());
        count(sts
            .test("RX freq should be an HF frequency", rxFreq.isValid() && rxFreq.bandOf().isHF(),
                entry.rxFrequency()));
        count(sts.testIfEmpty("RX Narrow or Wide should be blank", entry.rxNarrowWide()));
        count(sts.testIfEmpty("RX Tone should be blank", entry.rxTone()));
        var txFreq = new Frequency(entry.txFrequency());
        count(sts
            .test("TX Freq should be should be an HF frequency", txFreq.isValid() && txFreq.bandOf().isHF(),
                entry.txFrequency()));
        count(sts.testIfEmpty("TX Narrow or Wide should be blank", entry.txNarrowWide()));
        count(sts.testIfEmpty("TX Tone should be blank", entry.txTone()));
        count(sts.test("Mode should be A, D, or M", isModeValid(entry.mode()), entry.mode()));
        count(sts.testIfPresent("Remarks should be present", entry.remarks()));
      } // endif basic

    } // end loop over lines
  }

  private boolean isWxFrequency(String freqString) {
    final List<Double> WX_FREQUENCIES = List.of(162.400, 162.425, 162.450, 162.475, 162.500, 162.525, 162.550);
    final Set<Double> WX_FREQUENCY_SET = new HashSet<Double>(WX_FREQUENCIES);

    var freq = new Frequency(freqString);
    if (!freq.isValid()) {
      return false;
    }

    Double doubleFreq = Double.valueOf(freq.getFrequencyHz());

    if (!WX_FREQUENCY_SET.contains(doubleFreq)) {
      return false;
    }

    return true;
  }

  private boolean isCanadian(String from) {
    final var canadianPrefixes = List
        .of("VE", "VA", "VO", "VY", //
            "CF", "CG", "CI", "CJ", "CK", "CY", "CZ", "VB", "VC", "VD", "VF", "VG", "VX", "XL", "XM", "XN", "XO"); // special
                                                                                                                   // event
    for (var prefix : canadianPrefixes) {
      if (from.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private boolean isModeValid(String mode) {
    if (mode == null || mode.isEmpty()) {
      return false;
    }

    if (mode.equalsIgnoreCase("A") || mode.equalsIgnoreCase("D") || mode.equalsIgnoreCase("M")) {
      return true;
    }

    return false;
  }

  private boolean isNarrowOrWide(String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    if (value.equalsIgnoreCase("N") || value.equalsIgnoreCase("W")) {
      return true;
    }

    return false;
  }

}