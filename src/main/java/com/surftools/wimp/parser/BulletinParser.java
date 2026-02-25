/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.BulletinMessage;
import com.surftools.wimp.message.ExportedMessage;

/**
 * parser for Bulletin
 *
 * @author bobt
 *
 */
public class BulletinParser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {

      String xmlString = new String(message.attachments.get(MessageType.BULLETIN.rmsViewerName()));

      makeDocument(message.messageId, xmlString);

      var organization = getStringFromXml("title");
      var forNameGroup = getStringFromXml("name");
      var fromNameGroup = getStringFromXml("from_name");
      var bulletinNumber = getStringFromXml("bullnr");
      var formDateTime = getStringFromXml("activitydatetime1");
      var precedence = getStringFromXml("level");
      var formSubject = getStringFromXml("subjectline");
      var bulletinText = getStringFromXml("message");

      var version = "";
      var templateVersion = getStringFromXml("templateversion");
      if (templateVersion != null) {
        var fields = templateVersion.split(" ");
        version = fields[fields.length - 1]; // last field
      }

      var m = new BulletinMessage(message, organization, //
          forNameGroup, fromNameGroup, bulletinNumber, //
          formDateTime, precedence, formSubject, bulletinText, //
          version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }
}
