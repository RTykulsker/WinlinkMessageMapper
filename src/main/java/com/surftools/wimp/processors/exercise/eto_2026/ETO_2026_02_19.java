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
      "VEF", "CAN", "DX", "NA" };
  private final Set<String> WX_OFFICE_SET = new HashSet<>(Arrays.asList(WX_OFFICES));

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
    messageType = MessageType.PLAIN;

    var extraOutboundMessageText = """

        -----------------------------------------------------------------------------------------------------

        ETO Exercise Instructions for Thursday, 2026-02-26

        Task: Complete an ICS-205 Incident Radio Communications Plan Message

        Exercise window: Sat 2026-02-21 00:00 UTC - Fri 2026-02-27 08:00 UTC

        Use the following values when completing the form:
            Setup: agency or group name: EmComm Training Organization
            Incident name: ETO Weekly Practice
            Date/Time: (click in box and accept date/time)
            Operational Period Date From: 2026-02-21
            Operational Period Date To: 2026-02-27
            Operational Period Time From: 00:00 UTC
            Operational Period Time To: 08:00 UTC
            Basic Radio Channel Use:
                line 1
                    Ch #: 1
                    Function: Coordination
                    Channel Name: Repeater
                    Assignment: amateur
                    RX Freq: 444.250
                    RX N or W: W
                    RX Tone:
                    Tx Freq: 449.250
                    TX N or W: W
                    TX Tone: 210.7
                    Mode: A
                    Remarks: Primary repeater
                line 2
                    Ch #: 2
                    Function: Tactical
                    Channel Name: Simplex
                    Assignment: amateur
                    RX Freq: 146.430
                    RX N or W: N
                    RX Tone:
                    Tx Freq: 146.430
                    TX N or W: N
                    TX Tone:
                    Mode: A
                    Remarks: Primary simplex
                line 3
                    Ch #: 3
                    Function: Information
                    Channel Name: GMRS 11
                    Assignment: GMRS
                    RX Freq: 467.6375
                    RX N or W: N
                    RX Tone:
                    Tx Freq: 467.6375
                    TX N or W: N
                    TX Tone:
                    Mode: A
                    Remarks: Do not Transmit without GMRS license!
            Special Instructions: Exercise Id: 822-151-4886
            Approved by: Jackson Hale
            Approved Date/Time: (click in box and accept date/time)
            IAP Page: 6
            Attach CSV: (No)

        Send the message via the Session type of your choice to ETO-PRACTICE.

        Refer to https://emcomm-training.org/Winlink_Thursdays.html for further instructions
        about the weekly practice exercises and/or monthly training exercises.


                        """;

    outboundMessageExtraContent = OB_DISCLAIMER + extraOutboundMessageText;

    outboundMessageExtraContent = extraOutboundMessageText + OB_DISCLAIMER;
  }

  @Override
  protected void specificProcessing(ExportedMessage m) {
    var lines = m.plainContent.split("\n");
    if (lines == null || lines.length == 0) {
      count(sts.test("Message content present", false));
    } else {
      count(sts.test("Message content present", true));

      var list = Arrays.asList(lines);
      var pred = list.contains("Original Request Strip");
      count(sts.test("Message content should contain \"Original Request Strip\"", pred));

      pred = list.contains("Response Strip:");
      count(sts.test("Message content should contain \"Response Strip:\"", pred));

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
      var processedStrip = false;
      for (var line : lines) {
        if (line.contains("Senders Template Version: Request for Information Strip Reader")) {
          var fields = line.split(" ");
          var lastField = fields[fields.length - 1];
          getCounter("version").increment(lastField);
        }

        if (seenBlank && seenResponse && !processedStrip) {
          var fields = line.split("/");
          getCounter("Number of fields in response").increment(fields.length);
          count(sts
              .test("Number of fields in response should be #EV", String.valueOf(25), String.valueOf(fields.length)));

          if (fields.length <= 20) {
            getCounter("Insufficient fields in strip").increment(true);
            return;
          } else {
            getCounter("Insufficient fields in strip").increment(false);
          }

          count(sts.test("Strip Answer #1 should be #EV", "ETO", fields[0]));
          count(sts.test("Strip Answer #2 should be <YOURCALL>", fields[1].equals(m.from)));
          count(sts.testIfPresent("Strip Answer #3 (Skywarn ID) should be present", fields[2]));
          count(sts.testIfPresent("Strip Answer #4 (City) should be present", fields[3]));
          count(sts.testIfPresent("Strip Answer #5 (State) should be present", fields[4]));

          count(sts.testIfPresent("Strip Answer #6 (MGRS) should be present", fields[5]));
          var mgrs = fields[5];
          final var nMgrsChars = 9;
          count(sts
              .test("Strip Answer #6 (MGRS) should have #EV characters", String.valueOf(nMgrsChars),
                  String.valueOf(mgrs.length())));
          try {
            count(sts.test("Strip Answer #6 (MGRS) should be a valid MGRS location", true));
            var mgrsLoc = MgrsUtils.mgrsToLatLongPair(mgrs);
            var distanceMiles = Math.round(LocationUtils.computeDistanceMiles(m.mapLocation, mgrsLoc));
            final var maxDistanceMiles = 10;
            var msgMGRS = MgrsUtils.latLongPairToMgrs(m.mapLocation);
            pred = (!mgrs.equals(msgMGRS)) || (distanceMiles <= maxDistanceMiles);
            var extraText = "";
            if (!pred) {

              extraText = "(distMi: " + distanceMiles + ", msgLL: " + m.mapLocation + ", msgMgrs: " + msgMGRS
                  + ", stripLL: " + mgrsLoc + ", stripMGRS:" + mgrs + ")";
              logger.info(">>> call: " + m.from + ", " + extraText);
            }
            count(sts
                .test("Strip Answer #6 (MGRS) should be within " + maxDistanceMiles //
                    + " miles from message location", pred, String.valueOf(distanceMiles) + " miles " + extraText));

            getCounter("mgrsDistanceFromMsgLocation").increment(distanceMiles);
          } catch (Exception e) {
            count(sts.test("Strip Answer #6 (MGRS) should be a valid MGRS location", false));
          }

          count(sts.testIfPresent("Strip Answer #7 (NWS CWA) should be present", fields[6]));
          var office = fields[6];
          if (office != null) {
            office = office.trim();
            pred = WX_OFFICE_SET.contains(office);
            count(sts.test("Strip Answer #7 (NWS CWA) should be in list", pred, office));
            getCounter("WX Office").increment(office);
          }

          count(sts.testIfEmpty("Strip Answer #8 should be empty", fields[7].trim()));
          count(sts.testIfPresent("Strip Answer #9 (Observation Time) should be present", fields[8]));
          count(sts.testIfEmpty("Strip Answer #10 should be empty", fields[9].trim()));
          count(sts.testIfPresent("Strip Answer #11 (Wind Dir) should be present", fields[10]));
          count(sts.testIfPresent("Strip Answer #12 (Avg Speed) should be present", fields[11]));
          count(sts.testIfPresent("Strip Answer #13 (Gust Speed) should be present", fields[12]));
          count(sts.testIfEmpty("Strip Answer #14 should be empty", fields[13].trim()));
          count(sts.testIfPresent("Strip Answer #15 (Clouds) should be present", fields[14]));
          count(sts.testIfEmpty("Strip Answer #16 should be empty", fields[15].trim()));
          count(sts.testIfPresent("Strip Answer #17 (Temp) should be present", fields[16]));
          count(sts.testIfEmpty("Strip Answer #18 should be empty", fields[17].trim()));
          count(sts.testIfPresent("Strip Answer #19 (Baro) should be present", fields[18]));
          count(sts.testIfPresent("Strip Answer #20 (Baro Trend) should be present", fields[19]));
          count(sts.testIfEmpty("Strip Answer #21 should be empty", fields[20].trim()));
          count(sts.testIfPresent("Strip Answer #22 (Precip Type) should be present", fields[21]));
          count(sts.testIfPresent("Strip Answer #23 (Current Precip) should be present", fields[22]));
          count(sts.testIfEmpty("Strip Answer #24 should be empty", fields[23].trim()));
          if (fields.length >= 25) {
            count(sts.testIfPresent("Strip Answer #25 (Comments,Damage) should be present", fields[24]));
          } else {
            count(sts.testIfPresent("Strip Answer #25 (Comments,Damage) should be present", ""));
          }
          processedStrip = true;
          continue;
        }

        if (seenResponse && !seenBlank) {
          seenBlank = true;
          continue;
        }

        if (line.equals("Response Strip:")) {
          seenResponse = true;
          continue;
        }

      } // end loop over lines
    } // end if message content present
  }

  @Override
  public void postProcess() {
    counterMap.put("WX Office", getCounter("WX Office").squeeze(10, "(other)"));
    super.postProcess();
  }
}