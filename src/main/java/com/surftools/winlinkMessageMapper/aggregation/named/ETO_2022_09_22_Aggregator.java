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

package com.surftools.winlinkMessageMapper.aggregation.named;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.dto.message.EtoCheckInV2Message;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.Ics213Message;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * https://docs.google.com/document/d/130IF-JgzakPpq4nzQxg-p_7DGaBo9SXIKWKAFLOvI1M/edit
 *
 * ETO Check-In form received 25%
 *
 * ICS-213 Message form received by the same ETO clearinghouse. 25%
 *
 * ICS-213 Populated as requested
 *
 * --Setup header is “ETO Winlink Thursday”: 5%
 *
 * --Incident Name is “ETO 09-22-2022 Exercise”: 5%
 *
 * --To field in form is “ETO-##/CLEARINGHOUSE” (without quotes, with ## replaced by your clearinghouse id 5%
 *
 * --From field is “<YOURCALL>/Winlink Thursday Participant”: 5%
 *
 * --Subject field is “ICS-213 & ETO Check-In Form Exercise 09-22-2022”: 5%
 *
 * ICS-213 Form’s message field contains the Message ID of the ETO Check-In form: 25%
 *
 *
 * @author bobt
 *
 */
public class ETO_2022_09_22_Aggregator extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_09_22_Aggregator.class);

  private Map<String, Entry> callEntryMap;

  static record Entry(String call, String clearinghouse, LatLongPair location, String checkInComment,
      String checkInMessageId, String icsSetupName, String icsIncidentName, String icsToAddress, String icsFromAddress,
      String icsSubject, String icsMessage, String grade, String explanation) {

    public Entry updateLocation(LatLongPair newLocation) {
      return new Entry(call, clearinghouse, newLocation, checkInComment, checkInMessageId, icsSetupName,
          icsIncidentName, icsToAddress, icsFromAddress, icsSubject, icsMessage, grade, explanation);
    }
  };

  public ETO_2022_09_22_Aggregator() {
    super(logger);

    callEntryMap = new HashMap<>();
  }

  @Override
  /**
   * include all DYFI, Check In, FSR and FSR_23 and Check Out
   *
   */
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    super.aggregate(messageMap);

    var newList = new ArrayList<AggregateMessage>();
    var noLocationEntries = new ArrayList<Entry>();

    var ppCount = 0;
    var ppCheckInReceived = 0;
    var ppIcsReceived = 0;
    var ppIcsSetupOk = 0;
    var ppIcsIncidentNameOk = 0;
    var ppIcsFromOk = 0;
    var ppIcsToOk = 0;
    var ppIcsSubjectOk = 0;
    var ppIcsMessageIdOk = 0;
    var ppClearinghouseMatchOk = 0;
    var ppClearinghouseCommentsEmptyOk = 0;

    var ppScoreCountMap = new HashMap<Integer, Integer>();

    for (var m : aggregateMessages) {

      var from = m.from();

      if (dumpIds.contains(from)) {
        logger.info("dump: " + from);
      }

      var map = fromMessageMap.get(from);

      EtoCheckInV2Message checkInMessage = null;
      Ics213Message icsMessage = null;

      String clearinghouse = null;
      LatLongPair location = null;
      String checkInComments = null;
      String checkInMessageId = null;
      String icsSetupName = null;
      String icsIncidentName = null;
      String icsTo = null;
      String icsFrom = null;
      String icsSubject = null;
      String icsMessageMessage = null;

      var points = 0;
      var explanations = new ArrayList<String>();

      String checkInClearinghouse = null;
      var checkInList = map.get(MessageType.ETO_CHECK_IN_V2);
      if (checkInList != null) {
        Collections.reverse(checkInList);
        checkInMessage = (EtoCheckInV2Message) checkInList.iterator().next();
        if (checkInMessage == null) {
          explanations.add("no ETO Check In message received");
        } else {
          points += 20;
          location = new LatLongPair(checkInMessage.latitude, checkInMessage.longitude);
          ++ppCheckInReceived;
          checkInClearinghouse = checkInMessage.to;
          checkInComments = checkInMessage.comments;
          checkInMessageId = checkInMessage.messageId;

          if (checkInComments == null || checkInComments.isBlank()) {
            points += 5;
            ++ppClearinghouseCommentsEmptyOk;
          } else {
            explanations.add("ETO Check In comments should be blank, not: " + checkInComments);
          }
        } // end if checkInMessage != null
      } else {
        explanations.add("no ETO Check In message received");
      } // end if checkInList != null

      var icsList = map.get(MessageType.ICS_213);
      if (icsList != null) {
        Collections.reverse(icsList);
        icsMessage = (Ics213Message) icsList.iterator().next();
        if (icsMessage == null) {
          explanations.add("no ICS-213 message received");
        } else {
          points += 25;
          ++ppIcsReceived;

          clearinghouse = icsMessage.to;
          icsSetupName = icsMessage.organization;
          icsIncidentName = icsMessage.incidentName;
          icsTo = icsMessage.icsTo;
          icsFrom = icsMessage.icsFrom;
          icsSubject = icsMessage.icsSubject;
          icsMessageMessage = icsMessage.message;

          var requiredSetup = "ETO Winlink Thursday";
          if (icsSetupName == null) {
            explanations.add("ICS setup missing required value of (" + requiredSetup + ")");
          } else {
            if (!icsSetupName.equalsIgnoreCase(requiredSetup)) {
              explanations.add("ICS setup (" + icsSetupName + ") != required value of (" + requiredSetup + ")");
            } else {
              points += 5;
              ++ppIcsSetupOk;
            }
          }

          var requiredIncidentName = "ETO 09-22-2022 Exercise";
          if (icsIncidentName == null) {
            explanations.add("ICS Incident Name missing required value of (" + requiredIncidentName + ")");
          } else {
            if (!icsIncidentName.equalsIgnoreCase(requiredIncidentName)) {
              explanations
                  .add(
                      "ICS Incident Name(" + icsIncidentName + ") != required value of (" + requiredIncidentName + ")");
            } else {
              points += 5;
              ++ppIcsIncidentNameOk;
            }
          }

          var requiredTo = icsMessage.to + "/CLEARINGHOUSE";
          if (icsTo == null) {
            explanations.add("ICS To field missing required value of (" + requiredTo + ")");
          } else {
            if (!icsTo.equalsIgnoreCase(requiredTo)) {
              explanations.add("ICS To field (" + icsTo + ") != required value of (" + requiredTo + ")");
            } else {
              points += 5;
              ++ppIcsToOk;
            }
          }

          var requiredFrom = from + "/Winlink Thursday Participant";
          if (icsFrom == null) {
            explanations.add("ICS From field missing required value of (" + requiredFrom + ")");
          } else {
            if (!icsFrom.equalsIgnoreCase(requiredFrom)) {
              explanations.add("ICS From field (" + icsFrom + ") != required value of (" + requiredFrom + ")");
            } else {
              points += 5;
              ++ppIcsFromOk;
            }
          }

          var requiredSubject = "ICS-213 & ETO Check-In Form Exercise 09-22-2022";
          if (icsSubject == null) {
            explanations.add("ICS Subject field missing required value of (" + requiredSubject + ")");
          } else {
            if (!icsSubject.equalsIgnoreCase(requiredSubject)) {
              explanations.add("ICS Subject field (" + icsSubject + ") != required value of (" + requiredSubject + ")");
            } else {
              points += 5;
              ++ppIcsSubjectOk;
            }
          }
        } // endif icsMessage != null
      } else {
        explanations.add("no ICS-213 message received");
      } // end if icsList != null

      if (checkInMessage != null && icsMessage != null) {
        if (!icsMessageMessage.equalsIgnoreCase(checkInMessageId)) {
          explanations.add("ICS message (" + icsMessageMessage + ") != required value of (" + checkInMessageId + ")");
        } else {
          points += 25;
          ++ppIcsMessageIdOk;
        }

        if (!checkInClearinghouse.equalsIgnoreCase(clearinghouse)) {
          points -= 25;
          explanations
              .add("ETO Check In clearinghouse (" + checkInClearinghouse //
                  + ") != ICS-213 clearinghouse (" + clearinghouse + ")");
        } else {
          ++ppClearinghouseMatchOk;
        }
      }

      // only want folks who sent at least one of the right type of messages
      if (checkInMessage != null || icsMessage != null) {
        ++ppCount;

        points = Math.min(100, points);
        points = Math.max(0, points);
        var grade = String.valueOf(points);
        var explanation = (points == 100) ? "Perfect Score!" : String.join("\n", explanations);

        var scoreCount = ppScoreCountMap.getOrDefault(points, Integer.valueOf(0));
        ++scoreCount;
        ppScoreCountMap.put(points, scoreCount);

        clearinghouse = clearinghouse == null ? checkInClearinghouse : clearinghouse;
        var entry = new Entry(from, clearinghouse, location, checkInComments, checkInMessageId, icsSetupName,
            icsIncidentName, icsTo, icsFrom, icsSubject, icsMessageMessage, grade, explanation);

        // we'll fix, via jitterate, after we've seen all
        if (location == null) {
          noLocationEntries.add(entry);
        }
        callEntryMap.put(from, entry);

        newList.add(m);
      }

    } // end loop over for

    jitterate(callEntryMap, noLocationEntries);

    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-09-22 aggregate results:\n");
    sb.append("participants with at least one required messaged: " + ppCount + "\n");

    sb.append("\nMessage counts\n");
    sb.append(" ETO Check In message present: " + ppCheckInReceived + formatPercent(ppCheckInReceived, ppCount) + "\n");
    sb.append(" ICS-213 message present: " + ppIcsReceived + formatPercent(ppIcsReceived, ppCount) + "\n");

    sb.append(" ICS Setup Ok: " + ppIcsSetupOk + formatPercent(ppIcsSetupOk, ppCount) + "\n");
    sb.append(" ICS Incident Name Ok: " + ppIcsIncidentNameOk + formatPercent(ppIcsIncidentNameOk, ppCount) + "\n");
    sb.append(" ICS To Ok: " + ppIcsToOk + formatPercent(ppIcsToOk, ppCount) + "\n");
    sb.append(" ICS From Ok: " + ppIcsFromOk + formatPercent(ppIcsFromOk, ppCount) + "\n");
    sb.append(" ICS Subject Ok: " + ppIcsSubjectOk + formatPercent(ppIcsSubjectOk, ppCount) + "\n");

    sb
        .append(" ICS Message contains ETO Check In Id Ok: " + ppIcsMessageIdOk
            + formatPercent(ppIcsMessageIdOk, ppCount) + "\n");

    sb
        .append(" Clearinghouses match Ok: " + ppClearinghouseMatchOk + formatPercent(ppClearinghouseMatchOk, ppCount)
            + "\n");

    sb
        .append(" Clearinghouse comments empty Ok: " + ppClearinghouseCommentsEmptyOk
            + formatPercent(ppClearinghouseCommentsEmptyOk, ppCount) + "\n");
    ;
    var scores = new ArrayList<Integer>(ppScoreCountMap.keySet());
    Collections.sort(scores, Comparator.reverseOrder());
    sb.append("\nscores: \n");
    for (int score : scores) {
      var count = ppScoreCountMap.get(score);
      sb.append(" score: " + score + ", count: " + count + "\n");
    }

    logger.info(sb.toString());

    aggregateMessages = newList;
  }

  /**
   * re-locate entries without location to around "Zero-Zero" Island
   *
   * @param callEntryMap
   * @param noLocationEntries
   */
  private void jitterate(Map<String, Entry> callEntryMap, ArrayList<Entry> noLocationEntries) {
    if (noLocationEntries.size() == 0) {
      logger.info("all entries have locations!");
      return;
    }

    var n = noLocationEntries.size();
    logger.info(n + " entries have no locations: fixing");
    var s = noLocationEntries.stream().map(Entry::call).collect(Collectors.toList());
    logger.info("entries to be fixed: " + s);
    var list = LocationUtils.jitter(n, null, 10000);

    for (var i = 0; i < n; ++i) {
      var entry = noLocationEntries.get(i);
      var location = list.get(i);
      var newEntry = entry.updateLocation(location);
      callEntryMap.put(entry.call, newEntry);
    }
  }

  private String formatPercent(int numerator, int denominator) {
    double percent = (100d * numerator) / denominator;
    return " (" + String.format("%.2f", percent) + "%)";
  }

  @Override
  public void output(String pathName) {
    super.output(pathName);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "Call", "Clearinghouse", //
        "Latitude", "Longitude", //
        "CheckInComment", "CheckInMessageId", //
        "IcsSetupName", "IcsIncidentName", "IcsToAddress", "IcsFromAddress", "IcsSubject", "IcsMessage", "Grade",
        "Explanation" };
  }

  @Override
  public String[] getValues(AggregateMessage m) {
    var call = m.from();
    var e = callEntryMap.get(call);
    return new String[] { call, e.clearinghouse, //
        (e.location == null) ? "0.0" : e.location.getLatitude(),
        (e.location == null) ? "0.0" : e.location.getLongitude(), //
        e.checkInComment, e.checkInMessageId, //
        e.icsSetupName, e.icsIncidentName, e.icsToAddress, e.icsFromAddress, e.icsSubject, e.icsMessage, e.grade,
        e.explanation };
  }

}
