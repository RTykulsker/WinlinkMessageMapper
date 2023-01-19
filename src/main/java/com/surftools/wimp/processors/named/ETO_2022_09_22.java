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

package com.surftools.wimp.processors.named;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.EtoCheckInV2Message;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;

/**
 *
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
public class ETO_2022_09_22 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_09_22.class);

  static record Entry(String call, String clearinghouse, LatLongPair location, String checkInComment,
      String checkInMessageId, String icsSetupName, String icsIncidentName, String icsToAddress, String icsFromAddress,
      String icsSubject, String icsMessage, String grade, String explanation) implements IWritableTable {

    public Entry updateLocation(LatLongPair newLocation) {
      return new Entry(call, clearinghouse, newLocation, checkInComment, checkInMessageId, icsSetupName,
          icsIncidentName, icsToAddress, icsFromAddress, icsSubject, icsMessage, grade, explanation);
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Clearinghouse", "Latitude", "Longitude", //
          "CheckInComment", "CheckInMessageId", //
          "IcsSetupName", "IcsIncidentName", "IcsToAddress", "IcsFromAddress", "IcsSubject", "IcsMessage", //
          "Grade", "Explanation" };
    }

    @Override
    public String[] getValues() {
      var latitude = location == null || !location.isValid() ? "" : location.getLatitude();
      var longitude = location == null || !location.isValid() ? "" : location.getLongitude();
      return new String[] { call, clearinghouse, latitude, longitude, //
          checkInComment, checkInMessageId, //
          icsSetupName, icsIncidentName, icsToAddress, icsFromAddress, icsSubject, icsMessage, //
          grade, explanation };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Entry) other;
      return call.compareTo(o.call);
    }

  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @SuppressWarnings("unchecked")
  @Override

  public void process() {

    var entries = new ArrayList<Entry>();
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

    var scoreCounter = new Counter();

    var it = mm.getSenderIterator();
    while (it.hasNext()) {

      var from = it.next();
      var messageMap = mm.getMessagesForSender(from);
      if (dumpIds.contains(from)) {
        logger.info("dump: from: " + from + ", messages: " + messageMap.size());
      }

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
      var checkInList = messageMap.get(MessageType.ETO_CHECK_IN_V2);
      if (checkInList != null) {
        Collections.reverse(checkInList);
        checkInMessage = (EtoCheckInV2Message) checkInList.iterator().next();
        if (checkInMessage == null) {
          explanations.add("no ETO Check In message received");
        } else {
          points += 20;
          location = checkInMessage.mapLocation;
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

      var icsList = messageMap.get(MessageType.ICS_213);
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
          icsTo = icsMessage.formTo;
          icsFrom = icsMessage.formFrom;
          icsSubject = icsMessage.formSubject;
          icsMessageMessage = icsMessage.formMessage;

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

          // fallback
          if (location == null) {
            location = icsMessage.mapLocation;
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

        scoreCounter.increment(points);

        clearinghouse = clearinghouse == null ? checkInClearinghouse : clearinghouse;
        var entry = new Entry(from, clearinghouse, location, checkInComments, checkInMessageId, icsSetupName,
            icsIncidentName, icsTo, icsFrom, icsSubject, icsMessageMessage, grade, explanation);

        // we'll fix, via jitterate, after we've seen all
        if (location == null) {
          noLocationEntries.add(entry);
        }

        entries.add(entry);
      }

    } // end loop over for

    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-09-22 results:\n");
    sb.append("participants with at least one required messaged: " + ppCount + "\n");

    sb.append("\nMessage counts\n");
    sb.append(formatPP("ETO Check In message present", ppCheckInReceived, ppCount));
    sb.append(formatPP("ICS-213 message present", ppIcsReceived, ppCount));

    sb.append(formatPP("ICS Setup", ppIcsSetupOk, ppCount));
    sb.append(formatPP("ICS Incident Name Ok", ppIcsIncidentNameOk, ppCount));
    sb.append(formatPP("ICS To", ppIcsToOk, ppCount));
    sb.append(formatPP("ICS From", ppIcsFromOk, ppCount));
    sb.append(formatPP("ICS Subject", ppIcsSubjectOk, ppCount));
    sb.append(formatPP("ICS Message contains ETO Check In Id", ppIcsMessageIdOk, ppCount));
    sb.append(formatPP("Clearinghouses match", ppClearinghouseMatchOk, ppCount));
    sb.append(formatPP("Clearinghouse comments empty", ppClearinghouseCommentsEmptyOk, ppCount));

    sb.append("\nScores: \n" + formatCounter(scoreCounter.getDescendingKeyIterator(), "score", "count"));

    logger.info(sb.toString());

    var writables = jitterate(entries, noLocationEntries);
    WriteProcessor.writeTable(writables, Path.of(outputPathName, "gradedResults.csv"));
  }

  /**
   * re-locate entries without location to around "Zero-Zero" Island
   *
   * @param entries
   * @param noLocationEntries
   */
  private List<IWritableTable> jitterate(List<Entry> entries, List<Entry> noLocationEntries) {
    if (noLocationEntries.size() == 0) {
      logger.info("all entries have locations!");
      return new ArrayList<IWritableTable>(entries);
    }

    Map<String, Entry> callEntryMap = new HashMap<>();
    for (var entry : entries) {
      callEntryMap.put(entry.call, entry);
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

    return new ArrayList<IWritableTable>(callEntryMap.values());
  }

}
