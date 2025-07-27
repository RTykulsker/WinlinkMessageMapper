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

import java.util.Map;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.ExportedMessage.ExportedKey;
import com.surftools.wimp.message.Ics213Message;

public class Ics213Parser extends AbstractBaseParser {
  private static final String IS_EXERCISE = "** THIS IS AN EXERCISE **";
  private static final String[] mapLocationTags = new String[] { "maplat", "maplon" };

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      if (message.attachments.get(MessageType.ICS_213.rmsViewerName()) != null) {
        String xmlString = new String(message.attachments.get(MessageType.ICS_213.rmsViewerName()));
        makeDocument(message.messageId, xmlString);

        String organization = getStringFromXml("formtitle");
        var incidentName = getStringFromXml("inc_name");

        var formFrom = getStringFromXml("fm_name");
        var formTo = getStringFromXml("to_name");
        var formSubject = getStringFromXml("subjectline");
        var formDate = getStringFromXml("mdate");
        var formTime = getStringFromXml("mtime");

        // we want the value of the <message> element
        var formMessage = getStringFromXml("message");
        if (formMessage == null) {
          formMessage = getStringFromXml("Message");
        }

        var approvedBy = getStringFromXml("approved_name");
        var position = getStringFromXml("approved_postitle");

        var isExerciseString = getStringFromXml("isexercise");
        var isExercise = isExerciseString != null && isExerciseString.equals(IS_EXERCISE);
        var formLocation = getLatLongFromXml(mapLocationTags);
        var version = getStringFromXml("templateversion");
        if (version != null) {
          var fields = version.replaceAll("  ", " ").split(" ");
          if (fields.length >= 3) {
            version = fields[2];
          }
        }

        var m = new Ics213Message(message, organization, incidentName, //
            formFrom, formTo, formSubject, formDate, formTime, //
            formMessage, approvedBy, position, //
            isExercise, formLocation, version, DATA_SOURCE_RMS_VIEWER);
        return m;
      } else {
        @SuppressWarnings("unchecked")
        var formDataMap = (Map<ExportedKey, Map<String, String>>) mm.getContextObject("formDataMap");
        if (formDataMap == null) {
          return reject(message, RejectType.CANT_FIND_FORMDATA, "no FormData map");
        }

        var messageKey = new ExportedKey(message.from, message.messageId);
        var valueMap = formDataMap.get(messageKey);
        if (valueMap == null) {
          return reject(message, RejectType.CANT_FIND_FORMDATA, "no FormData entry for message: " + messageKey);
        }

        var organization = valueMap.get("Organization");
        var incidentName = valueMap.get("1. Incident Name");
        var formFrom = valueMap.get("3. From");
        var formTo = valueMap.get("2. To (Name/Position)");
        var formSubject = valueMap.get("4. Subject");
        var formDate = valueMap.get("5. Date");
        var formTime = valueMap.get("6. Time");
        var formMessage = valueMap.get("7. Message");
        var approvedBy = valueMap.get("8. Approved by");
        var position = valueMap.get("8b.Position / Title");

        var isExerciseString = valueMap.get("0. Form Note");
        var isExercise = isExerciseString != null && isExerciseString.equals(IS_EXERCISE);

        var formLocation = new LatLongPair(valueMap.get("Latitude"), valueMap.get("Longitude"));

        var version = "(unknown)";

        var m = new Ics213Message(message, organization, incidentName, //
            formFrom, formTo, formSubject, formDate, formTime, //
            formMessage, approvedBy, position, //
            isExercise, formLocation, version, DATA_SOURCE_FORM_DATA);
        return m;
      }
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
