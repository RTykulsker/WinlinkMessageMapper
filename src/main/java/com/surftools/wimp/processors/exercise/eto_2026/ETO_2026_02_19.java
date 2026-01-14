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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LocationUtils;
import com.surftools.utils.location.MgrsUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.baseExercise.SingleMessageFeedbackProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * a plain text, Request for Information Strip Reader
 *
 * @author bobt
 *
 */
public class ETO_2026_02_19 extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(ETO_2026_02_19.class);

  final String[] WX_OFFICES = new String[] { "ABQ", "ABR", "AFC", "AFG", "AJK", "AKQ", "ALY", "AMA", "APX", "ARX",
      "BGM", "BIS", "BMX", "BOI", "BOU", "BOX", "BRO", "BTV", "BUF", "BYZ", "CAE", "CAR", "CHS", "CLE", "CRP", "CTP",
      "CYS", "DDC", "DLH", "DMX", "DTX", "DVN", "EAX", "EKA", "EPZ", "EWX", "FFC", "FGF", "FGZ", "FSD", "FWD", "GGW",
      "GID", "GJT", "GLD", "GRB", "GRR", "GSP", "GUM", "GYX", "HFO", "HGX", "HNX", "HUN", "ICT", "ILM", "ILN", "ILX",
      "IND", "IWX", "JAN", "JAX", "JKL", "KEY", "LBF", "LCH", "LIX", "LKN", "LMK", "LOT", "LOX", "LSX", "LUB", "LWX",
      "LZK", "MAF", "MEG", "MFL", "MFR", "MHX", "MKX", "MLB", "MOB", "MPX", "MQT", "MRX", "MSO", "MTR", "OAX", "OHX",
      "OKX", "OTX", "OUN", "PAH", "PBZ", "PDT", "PHI", "PIH", "PPG", "PQR", "PSR", "PUB", "RAH", "REV", "RIW", "RLX",
      "RNK", "SEW", "SGF", "SGX", "SHV", "SJT", "SJU", "SLC", "STO", "TAE", "TBW", "TFX", "TOP", "TSA", "TWC", "UNR",
      "VEF", "CAN", "DX" };
  private final Set<String> WX_OFFICE_SET = new HashSet<>(Arrays.asList(WX_OFFICES));

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.PLAIN;
    doStsFieldValidation = false;
    var extraOutboundMessageText = "";
    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void specificProcessing(ExportedMessage m) {
    var lines = m.plainContent.split("\n");
    if (lines == null || lines.length == 0) {
      count(sts.test("Message content present", false));
    } else {
      count(sts.test("Message content present", true));

      var firstLine = lines[0];
      if (firstLine != null && !firstLine.isBlank()) {
        count(sts.test("First line should be present", true));
        count(sts.test("First line should be #EV", "Original Request Strip", firstLine));
      } else {
        count(sts.test("First line should be present", true));
      }

      var list = Arrays.asList(lines);
      var pred = list.contains("Original Request Strip");
      count(sts.test("Message content should contain \"Original Request Strip\"", pred));

      pred = list.contains("Response Strip:");
      count(sts.test("Message content should contain \"Response Strip:\"", pred));

      var lastLine = lines[lines.length - 1];
      if (lastLine != null && !lastLine.isBlank()) {
        count(sts.test("Last line should be present", true));
        count(sts
            .test("Last line should be #EV",
                "Senders Template Version: Request for Information Strip Reader and Response Creator v 1.4", lastLine));
        var fields = lastLine.split(" ");
        var lastField = fields[fields.length - 1];
        getCounter("version").increment(lastField);
      } else {
        count(sts.test("Last line should be present", true));
      }

      var src = List
          .of( //
              "ETO/CALL SIGN/SKYWARN ID(or NA)/CITY/STATE (AA)/MGRS (9 CHARACTERS)/NWS", //
              "CWA(AAA,NA)/ /OBSERVATION TIME Z (DDHHMMZ)/ /WIND DIR (AAA)/AVE SPEED MPH", //
              "(###)/GUSTS MPH (###)/ /CLOUDS (CLR,FEW,SKT,CB,OVC,TCU)/ /TEMP DEG F (###", //
              "M=MINUS)/ /BAROMETER MB (####R#)/BAROMETER 3 HR TREND (R,S,F)/ /PRECIP TYPE", //
              "(RA,SN,SL,PL,GR,NONE)/CURRENT PRECIP INS (###R##,NA)//COMMENTS,DAMAGE//");
      for (var srcIndex = 0; srcIndex < src.size(); ++srcIndex) {
        var srcLine = src.get(srcIndex);
        var srcLineNumber = srcIndex + 1;
        pred = list.contains(srcLine);
        count(sts.test("Message should contain original string line #" + srcLineNumber + ", " + srcLine, pred));
      }

      var seenResponse = false;
      var seenBlank = false;
      for (var line : lines) {
        if (seenBlank && seenResponse) {
          var fields = line.split("/");
          count(sts
              .test("Number of fields in response should be #EV", String.valueOf(25), String.valueOf(fields.length)));
          count(sts.test("Response field #1 should be #EV", "ETO", fields[0]));
          count(sts.test("Response field #2 should be <YOURCALL>", fields[1].equals(m.from)));
          count(sts.testIfPresent("Response field #3 should be present", fields[2]));
          count(sts.testIfPresent("Response field #4 should be present", fields[3]));
          count(sts.testIfPresent("Response field #5 should be present", fields[4]));

          count(sts.testIfPresent("Response field #6 should be present", fields[5]));
          var mgrs = fields[5];
          count(sts
              .test("Response field #6 should have #EV characters", String.valueOf(9), String.valueOf(mgrs.length())));
          try {
            count(sts.test("Response field #6 should be a valid MGRS location", true));
            var pair = MgrsUtils.mgrsToLatLongPair(mgrs);
            var distanceMiles = Math.round(LocationUtils.computeDistanceMiles(m.mapLocation, pair));
            pred = distanceMiles <= 5;
            count(sts
                .test("Response field #6 should be within 5 miles from message location", pred,
                    ", not " + distanceMiles));
            getCounter("mgrsDistanceFromMsgLocation").increment(distanceMiles);
          } catch (Exception e) {
            count(sts.test("Response field #6 should be a valid MGRS location", false));
          }

          count(sts.testIfPresent("Response field #7 should be present", fields[6]));
          var office = fields[6];
          if (office != null) {
            office = office.trim();
            pred = WX_OFFICE_SET.contains(office);
            getCounter("WX Office").increment(office);
          }

          count(sts.testIfEmpty("Response field #8 should be present", fields[7].trim()));
          count(sts.testIfPresent("Response field #9 should be present", fields[8]));
          count(sts.testIfEmpty("Response field #10 should be present", fields[9].trim()));
          count(sts.testIfPresent("Response field #11 should be present", fields[10]));
          count(sts.testIfPresent("Response field #12 should be present", fields[11]));
          count(sts.testIfPresent("Response field #13 should be present", fields[12]));
          count(sts.testIfEmpty("Response field #14 should be present", fields[13].trim()));
          count(sts.testIfPresent("Response field #15 should be present", fields[14]));
          count(sts.testIfEmpty("Response field #16 should be present", fields[15].trim()));
          count(sts.testIfPresent("Response field #17 should be present", fields[16]));
          count(sts.testIfEmpty("Response field #18 should be present", fields[17].trim()));
          count(sts.testIfPresent("Response field #19 should be present", fields[18]));
          count(sts.testIfPresent("Response field #20 should be present", fields[19]));
          count(sts.testIfEmpty("Response field #21 should be present", fields[20].trim()));
          count(sts.testIfPresent("Response field #22 should be present", fields[21]));
          count(sts.testIfPresent("Response field #23 should be present", fields[22]));
          count(sts.testIfEmpty("Response field #24 should be present", fields[23].trim()));
          count(sts.testIfPresent("Response field #25 should be present", fields[24]));
          break;
        }

        if (seenResponse && !seenBlank) {
          seenBlank = true;
          continue;
        }

        if (line.equals("Response Strip:")) {
          seenResponse = true;
          continue;
        }
      }
    }
  }

  @Override
  public void postProcess() {
    super.postProcess();
  }
}