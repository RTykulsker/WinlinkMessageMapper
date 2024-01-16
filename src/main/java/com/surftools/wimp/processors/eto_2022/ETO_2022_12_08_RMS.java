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

package com.surftools.wimp.processors.eto_2022;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;

/**
 * Processor for 2022-12-08 Exercise: one FSR and one ICS-213.
 *
 * NOTE WELL: This is for the graded/Part 1/RMS/Clearinghouse portion of the exercise
 *
 * FSR graded on:
 *
 * --- FSR populated as required
 *
 * --- required header: 20 points
 *
 * --- precedence set to priority: 10 points
 *
 * --- task number as required: 10 points
 *
 * --- To: set as required: 10 points
 *
 * --- automatic fail if Box 1 is Yes
 *
 * --- automatic fail if Box 3 (lat/long) is blank or not a valid location
 *
 * ICS-213 Populated as requested
 *
 * --- required header: 5 points
 *
 * --- incident name: 5 points
 *
 * --- to: 5 points
 *
 * --- from: 5 points, contains "Exercise Participant"
 *
 * --- subject: 5 points
 *
 * --- message:
 *
 * ------ exactly 10 lines: 10 points
 *
 * ------ line 1 has 4 fields: 5 points
 *
 * ------ lines 2-10 are all Yes/No/DNA: 5 points
 *
 * ------ one of line 2 and line 6 are Yes: 5 points
 *
 * --
 *
 * BOTH messages must be present (and not failed) to allow participation in P2P
 *
 * @author bobt
 *
 *
 *
 * @author bobt
 *
 */
public class ETO_2022_12_08_RMS extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_12_08_RMS.class);

  private List<IWritableTable> passList = new ArrayList<>(); // both messages, no failures
  private List<IWritableTable> failList = new ArrayList<>(); // one message OR automatic failure

  private String[] FIELDS = null;

  static record Result(String call, String to, LatLongPair location, //
      String fsrClearinghouse, String fsrMessageId, String fsrComment, //
      String icsClearinghouse, String icsMessageId, String icsMessageText, //
      String icsRMS, String opFromHome, String homeFM, String homeHF, String homeWL, //
      String opFromField, String fieldFM, String fieldHF, String fieldWL, String needSupplies, //
      String grade, String explanation) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "To", "Latitude", "Longitude", //
          "FsrTo", "FsrMiD", "FsrComment", //
          "IcsTo", "IcsMiD", "IcsMessage", //
          "RMS", "Op From Home?", "Home VHF/UHF", "Home HF", "Home Winlink", //
          "Op From Field", "Field VHF/UHF", "Field HF", "Field Winlink", "Need Support/Supplies", //
          "Grade", "Explanation" };
    }

    @Override
    public String[] getValues() {
      var aLocation = location != null ? location : LatLongPair.ZERO_ZERO;
      return new String[] { call, to, aLocation.getLatitude(), aLocation.getLongitude(), //
          fsrClearinghouse, fsrMessageId, fsrComment, //
          icsClearinghouse, icsMessageId, icsMessageText, //
          icsRMS, opFromHome, homeFM, homeHF, homeWL, //
          opFromField, fieldFM, fieldHF, fieldWL, needSupplies, //
          grade, explanation };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      var cmp = call.compareTo(o.call);
      if (cmp != 0) {
        return cmp;
      }
      return to.compareTo(o.to);
    }

  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {

    var ppCount = 0;
    var ppFsrReceived = 0;
    var ppIcsReceived = 0;
    var ppBothMessagesReceived = 0;

    var ppFsrSetupOk = 0;
    var ppFsrPrecedenceOk = 0;
    var ppFsrTaskNumberOk = 0;
    var ppFsrToOk = 0;

    var ppIcsSetupOk = 0;
    var ppIcsIncidentNameOk = 0;
    var ppIcsToOk = 0;
    var ppIcsFromOk = 0;
    var ppIcsSubjectOk = 0;

    var ppIcsMessage10Lines = 0;
    var ppIcsMessageLine1Has4Fields = 0;
    var ppIcsMessageLines2thru10AllYNDNA = 0;
    var ppIcsMessageLine2XorLine6IsTrue = 0;

    var ppPassCount = 0;
    var ppFailCount = 0;
    var ppFailNoFsrCount = 0;
    var ppFailNoIcsCount = 0;
    var ppFailFcsBox1Count = 0;
    var ppFailFcsBadLocationCount = 0;

    var ppScoreCounter = new Counter();

    var it = mm.getSenderIterator();
    while (it.hasNext()) {
      var from = it.next();
      if (dumpIds.contains(from)) {
        logger.info("dump: " + from);
      }

      var map = mm.getMessagesForSender(from);
      var fsrMessage = getFsrMessage(map);
      var icsMessage = getIcsMessage(map);
      if (fsrMessage == null && icsMessage == null) {
        logger.info("skipping messages from " + from + ", since no fsr or ics");
        continue;
      }

      ++ppCount;

      var isFailure = false; // either message missing or auto-fail

      String fsrMessageId = null;
      LatLongPair fsrLocation = null;
      String fsrClearinghouse = null;
      String fsrComment = null;
      String fsrSetup = null;
      String fsrPrecedence = null;
      String fsrTaskNumber = null;
      String fsrTo = null;

      String icsMessageId = null;
      String icsClearinghouse = null;
      String icsSetup = null;
      String icsIncidentName = null;
      String icsTo = null;
      String icsFrom = null;
      String icsSubject = null;
      String icsMessageText = null;

      var points = 0;
      var explanations = new ArrayList<String>();

      if (fsrMessage == null) {
        explanations.add("FAIL because no FSR message received");
        ++ppFailNoFsrCount;
        isFailure = true;
      } else {
        ++ppFsrReceived;
        fsrMessageId = fsrMessage.messageId;
        fsrClearinghouse = fsrMessage.to;
        fsrComment = fsrMessage.additionalComments;

        fsrSetup = fsrMessage.organization;
        var requiredFcsSetup = "EmComm Training Organization 12/08 THIS IS AN EXERCISE";
        if (fsrSetup == null || !fsrSetup.equalsIgnoreCase(requiredFcsSetup)) {
          explanations.add("FSR setup (" + fsrSetup + ") doesn't match (" + requiredFcsSetup + ")");
        } else {
          points += 20;
          ++ppFsrSetupOk;
        }

        fsrPrecedence = fsrMessage.precedence;
        var requiredFsrPrecedence = "P/ Priority";
        if (fsrPrecedence == null || !fsrPrecedence.equalsIgnoreCase(requiredFsrPrecedence)) {
          explanations.add("FSR precedence (" + fsrPrecedence + ") doesn't match (" + requiredFsrPrecedence + ")");
        } else {
          points += 10;
          ++ppFsrPrecedenceOk;
        }

        fsrTaskNumber = fsrMessage.task;
        var requiredFsrTaskNumber = "ETO 12/08 EXERCISE";
        if (fsrTaskNumber == null || !fsrTaskNumber.equalsIgnoreCase(requiredFsrTaskNumber)) {
          explanations.add("FSR task # (" + fsrTaskNumber + ") doesn't match (" + requiredFsrTaskNumber + ")");
        } else {
          points += 10;
          ++ppFsrTaskNumberOk;
        }

        fsrTo = fsrMessage.formTo;
        var requiredFsrTo = "JOHN SMITH/ETO HEADQUARTERS";
        if (fsrTo == null || !fsrTo.equalsIgnoreCase(requiredFsrTo)) {
          explanations.add("FSR To (" + fsrTo + ") doesn't match (" + requiredFsrTo + ")");
        } else {
          points += 10;
          ++ppFsrToOk;
        }

        fsrLocation = fsrMessage.formLocation;
        if (!fsrLocation.isValid()) {
          isFailure = true;
          ++ppFailFcsBadLocationCount;
          explanations.add("FAIL because FSR location(" + fsrLocation.toString() + ") is not valid");
          fsrLocation = LatLongPair.ZERO_ZERO;
        }

        var box1 = fsrMessage.isHelpNeeded;
        if (box1.equalsIgnoreCase("YES")) {
          isFailure = true;
          explanations.add("FAIL because FSR Box1 set to 'YES'");
          ++ppFailFcsBox1Count;
        }

      } // end if fsr != null

      if (icsMessage == null) {
        explanations.add("FAIL because no ICS-213 message received");
        isFailure = true;
        ++ppFailNoIcsCount;
      } else {
        ++ppIcsReceived;

        icsMessageId = icsMessage.messageId;
        icsClearinghouse = icsMessage.to;
        icsMessageText = icsMessage.formMessage;

        icsSetup = icsMessage.organization;
        var requiredIcsSetup = "EmComm Training Organization 12/08 THIS IS AN EXERCISE";
        if (icsSetup == null || !icsSetup.equalsIgnoreCase(requiredIcsSetup)) {
          explanations.add("ICS setup (" + icsSetup + ") doesn't match (" + requiredIcsSetup + ")");
        } else {
          points += 5;
          ++ppIcsSetupOk;
        }

        icsIncidentName = icsMessage.incidentName;
        var requiredIcsIncidentName = "ETO DECEMBER 8TH EXERCISE";
        if (icsIncidentName == null || !icsIncidentName.equalsIgnoreCase(requiredIcsIncidentName)) {
          explanations.add("ICS setup (" + icsIncidentName + ") doesn't match (" + requiredIcsIncidentName + ")");
        } else {
          points += 5;
          ++ppIcsIncidentNameOk;
        }

        icsTo = icsMessage.formTo;
        var requiredIcsTo = "JOHN SMITH/ETO HEADQUARTERS";
        if (icsTo == null || !icsTo.equalsIgnoreCase(requiredIcsTo)) {
          explanations.add("ICS to (" + icsTo + ") doesn't match (" + requiredIcsTo + ")");
        } else {
          points += 5;
          ++ppIcsToOk;
        }

        icsFrom = icsMessage.formFrom;
        var requiredIcsFrom = "EXERCISE PARTICIPANT";
        if (icsFrom == null || !icsFrom.toUpperCase().contains(requiredIcsFrom)) {
          explanations.add("ICS from (" + icsFrom + ") doesn't contain (" + requiredIcsFrom + ")");
        } else {
          points += 5;
          ++ppIcsFromOk;
        }

        icsSubject = icsMessage.formSubject;
        var requiredIcsSubject = "OPERATIONAL READINESS REPORT";
        if (icsSubject == null || !icsSubject.equalsIgnoreCase(requiredIcsSubject)) {
          explanations.add("ICS subject (" + icsTo + ") doesn't match (" + requiredIcsSubject + ")");
        } else {
          points += 5;
          ++ppIcsSubjectOk;
        }

        icsMessageText = icsMessage.formMessage;
        if (icsMessageText == null || icsMessageText.isBlank()) {
          explanations.add("ICS message text missing");
        }

        /**
         * --- exactly 10 lines: 10 points
         *
         * ------ line 1 has 4 fields: 5 points
         *
         * ------ lines 2-10 are all Yes/No/DNA: 5 points
         *
         * ------ one of line 2 and line 6 are Yes: 5 points
         *
         */
        var lines = icsMessageText.split("\n");
        FIELDS = lines;
        if (lines.length != 10) {
          explanations.add("ICS message text contains " + lines.length + " lines, not 10");
        } else {
          points += 10;
          ++ppIcsMessage10Lines;
        }

        var line1 = lines[0];
        var line1Fields = line1.split(",");
        if (line1Fields.length < 4) {
          explanations.add("ICS message line 1 not in correct format");
        } else {
          points += 5;
          ++ppIcsMessageLine1Has4Fields;
        }

        var allYNDNA = (lines.length > 1) ? true : false;
        var listOfBadIndexes = new ArrayList<String>();
        for (int i = 1; i < lines.length; ++i) {
          var line = lines[i].toUpperCase();
          if (line.equals("YES") || line.equals("NO") || line.equals("DNA")) {
            continue;
          } else {
            allYNDNA = false;
            listOfBadIndexes.add(String.valueOf(i + 1));
          }
        }
        if (!allYNDNA) {
          explanations.add("Non Yes/No/DNA response on line(s): " + String.join(",", listOfBadIndexes));
        } else {
          points += 5;
          ++ppIcsMessageLines2thru10AllYNDNA;
        }

        if (lines.length >= 6) {
          var opFromHome = lines[1].toUpperCase().equalsIgnoreCase("YES");
          var opFromField = lines[5].toUpperCase().equalsIgnoreCase("YES");
          if ((opFromHome && opFromField) || (!opFromHome && !opFromField)) {
            explanations.add("Must either operate from either home or field, but not both or neither");
          } else {
            points += 5;
            ++ppIcsMessageLine2XorLine6IsTrue;
          }
        }

        if (fsrMessage != null && icsMessage != null) {
          ++ppBothMessagesReceived;
        }
      } // endif ICS received

      points = Math.min(100, points);
      points = Math.max(0, points);

      if (isFailure) {
        points = 0;
      }

      var grade = String.valueOf(points);
      var explanation = (points == 100 && explanations.size() == 0) //
          ? "Perfect Score!"
          : String.join("\n", explanations);

      ppScoreCounter.increment(points);

      var to = fsrClearinghouse != null ? fsrClearinghouse : icsClearinghouse;
      var result = new Result(from, to, fsrLocation, //
          fsrClearinghouse, fsrMessageId, fsrComment, //
          icsClearinghouse, icsMessageId, icsMessageText, //
          fields(0), fields(1), fields(2), fields(3), fields(4), //
          fields(5), fields(6), fields(7), fields(8), fields(9), //
          grade, explanation);

      if (isFailure) {
        ++ppFailCount;
        failList.add(result);
      } else {
        ++ppPassCount;
        passList.add(result);
      }
    } // end loop over for

    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-12-08 RMS aggregate results:\n");
    sb.append("total participants: " + ppCount + "\n");
    sb.append("participants with both required messages (may also fail): " + ppBothMessagesReceived + "\n");

    sb.append(formatPP("  FSR messages received", ppFsrReceived, ppCount));
    sb.append(formatPP("  FSR Setup Ok", ppFsrSetupOk, ppFsrReceived));
    sb.append(formatPP("  FSR Precedence OK", ppFsrPrecedenceOk, ppFsrReceived));
    sb.append(formatPP("  FSR Task # OK", ppFsrTaskNumberOk, ppFsrReceived));
    sb.append(formatPP("  FSR To address OK", ppFsrToOk, ppFsrReceived));

    sb.append(formatPP("  ICS messages received", ppIcsReceived, ppCount));
    sb.append(formatPP("  ICS Setup Ok", ppIcsSetupOk, ppCount));
    sb.append(formatPP("  ICS Incident name Ok", ppIcsIncidentNameOk, ppCount));
    sb.append(formatPP("  ICS To address Ok", ppIcsToOk, ppCount));
    sb.append(formatPP("  ICS From address Ok", ppIcsFromOk, ppCount));
    sb.append(formatPP("  ICS Subject Ok", ppIcsSubjectOk, ppCount));
    sb.append(formatPP("  ICS message has 10 lines Ok", ppIcsMessage10Lines, ppCount));
    sb.append(formatPP("  ICS line 1 describes RMS Ok", ppIcsMessageLine1Has4Fields, ppCount));
    sb.append(formatPP("  ICS lines 2 thru 9 are Yes/No/DNA", ppIcsMessageLines2thru10AllYNDNA, ppCount));
    sb.append(formatPP("  ICS operating from Home or Field", ppIcsMessageLine2XorLine6IsTrue, ppCount));

    sb.append("\nparticipants passing: " + ppPassCount + "\n");
    sb.append("participants with automatic fails: " + ppFailCount + "\n");
    sb.append(formatPP("  FAIL because no FSR", ppFailNoFsrCount, ppFailCount));
    sb.append(formatPP("  FAIL because no ICS", ppFailNoIcsCount, ppFailCount));
    sb.append(formatPP("  FAIL because FSR Box 1", ppFailFcsBox1Count, ppFailCount));
    sb.append(formatPP("  FAIL because Location invalid", ppFailFcsBadLocationCount, ppFailCount));

    sb.append("\nScores: \n" + formatCounter(ppScoreCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());

    WriteProcessor.writeTable(makeAllList(passList, failList), Path.of(outputPathName, "aggregate-all.csv"));
    WriteProcessor.writeTable(passList, Path.of(outputPathName, "aggregate-pass.csv"));
    WriteProcessor.writeTable(failList, Path.of(outputPathName, "aggregate-fail.csv"));
  }

  private String fields(int i) {
    if (FIELDS == null || i >= FIELDS.length) {
      return null;
    }

    return FIELDS[i];
  }

  private FieldSituationMessage getFsrMessage(Map<MessageType, List<ExportedMessage>> map) {
    var list = map.get(MessageType.FIELD_SITUATION);
    if (list != null) {
      Collections.reverse(list);
      var fsrMessage = (FieldSituationMessage) list.iterator().next();
      return fsrMessage;
    } else {
      return null;
    }
  }

  private Ics213Message getIcsMessage(Map<MessageType, List<ExportedMessage>> map) {
    var list = map.get(MessageType.ICS_213);
    if (list != null) {
      Collections.reverse(list);
      var icsMessage = (Ics213Message) list.iterator().next();
      return icsMessage;
    } else {
      return null;
    }
  }

  private List<IWritableTable> makeAllList(List<IWritableTable> passList, List<IWritableTable> failList) {
    var tmpList = new ArrayList<IWritableTable>(passList);
    tmpList.addAll(failList);

    return tmpList;
  }

}
