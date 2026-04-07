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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.CustomizableFormMessage;
import com.surftools.wimp.message.ExportedMessage;

/**
 * parser for CustomizableFormMessage
 *
 *
 * @author bobt
 *
 */
public class CustomizableFormParser extends AbstractBaseParser {
  protected Logger logger = LoggerFactory.getLogger(CustomizableFormParser.class);

  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      String xmlString = new String(message.attachments.get(MessageType.CUSTOMIZABLE_FORM.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("title");
      var incidentName = getStringFromXml("incident_name");
      var formDateTimeString = getStringFromXml("activitydatetime1");
      var description = getStringFromXml("desc");
      var comments = getStringFromXml("comments");
      var columnAName = getStringFromXml("namea");
      var columnBName = getStringFromXml("nameb");
      var columnCName = getStringFromXml("namec");

      var entryArray = new CustomizableFormMessage.Entry[CustomizableFormMessage.MAX_ENTRIES + 1];
      for (int i = 1; i <= CustomizableFormMessage.MAX_ENTRIES; ++i) {
        var assignment = getStringFromXml("assignment" + i);
        var name = getStringFromXml("name" + i);
        var method = getStringFromXml("method" + i);
        var entry = new CustomizableFormMessage.Entry(i, assignment, name, method);
        entryArray[i] = entry;
      }

      var version = "";
      var templateVersion = getStringFromXml("templateversion");
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      var m = new CustomizableFormMessage(message, organization, incidentName, //
          formDateTimeString, description, //
          columnAName, columnBName, columnCName, //
          entryArray, comments, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
