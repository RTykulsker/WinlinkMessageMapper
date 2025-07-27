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

package com.surftools.wimp.parser;

import java.util.ArrayList;
import java.util.List;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.WindshieldDamageAssessmentMessage;
import com.surftools.wimp.message.WindshieldDamageAssessmentMessage.DamageEntry;

/**
 * parser for Windshield Damage
 *
 * @author bobt
 *
 */
public class WindshieldDamageParser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {

      String xmlString = new String(message.attachments.get(MessageType.DAMAGE_ASSESSMENT.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("title");
      var jurisdiction = getStringFromXml("jur");
      var missionIncidentId = getStringFromXml("misnum");
      var formType = getStringFromXml("status");
      var eventType = getStringFromXml("event");
      var description = getStringFromXml("other");
      var surveyArea = getStringFromXml("surarea");
      var surveyTeam = getStringFromXml("surteam");
      var eventStartDate = getStringFromXml("datetime1");
      var surveyDate = getStringFromXml("date");
      var totalLossString = getStringFromXml("dollar16");

      var damageEntries = makeDamageEntries();

      var comments = getStringFromXml("comments");

      var version = "";
      var templateVersion = getStringFromXml("templateversion");

      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      WindshieldDamageAssessmentMessage m = new WindshieldDamageAssessmentMessage(message, organization, jurisdiction,
          missionIncidentId, formType, eventType, description, surveyArea, surveyTeam, eventStartDate, surveyDate,
          damageEntries, totalLossString, comments, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private List<DamageEntry> makeDamageEntries() {

    var list = new ArrayList<DamageEntry>();
    for (int i = 1; i <= 15; ++i) {
      var description = "";
      if (i >= 1 && i <= 12) {
        description = DamageEntry.getName(i);
      } else {
        description = getStringFromXml("other" + i);
      }
      var affected = getStringFromXml("aff" + i);
      var minor = getStringFromXml("min" + i);
      var major = getStringFromXml("maj" + i);
      var destroyed = getStringFromXml("des" + i);
      var total = getStringFromXml("total" + i);
      var lossString = getStringFromXml("dollar" + i);
      var lossAmount = getStringFromXml("hdollar" + i);
      var entry = new DamageEntry(description, affected, minor, major, destroyed, total, //
          lossString, lossAmount);
      list.add(entry);
    }
    return list;
  }

}
