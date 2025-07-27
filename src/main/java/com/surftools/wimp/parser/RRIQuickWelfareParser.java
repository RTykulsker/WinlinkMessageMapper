/**

 - DO NOT REPLY!The MIT License (MIT)

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

import java.io.ByteArrayInputStream;

import javax.mail.internet.MimeUtility;

import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RRIQuickWelfareMessage;

public class RRIQuickWelfareParser extends AbstractBaseParser {

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {
      var mime = message.plainContent;

      var inputStream = MimeUtility.decode(new ByteArrayInputStream(mime.getBytes()), "quoted-printable");
      var demimed = new String(inputStream.readAllBytes());
      String[] mimeLines = demimed.split("\\n");

      var formFrom = parseFormFrom(message.subject);
      var formDateTime = parseFormDateTime(mimeLines);
      var incidentName = parseIncidentName(mimeLines);
      var text = parseText(mimeLines);
      var version = parseVersion(mimeLines);

      var m = new RRIQuickWelfareMessage(message, //
          formFrom, formDateTime, incidentName, text, version);

      return m;
    } catch (

    Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  // I Am Safe Message From XXX - DO NOT REPLY!
  private String parseFormFrom(String subject) {
    var prefix = "I Am Safe Message From ";
    var suffix = " - DO NOT REPLY!";
    var startIndex = prefix.length();
    var endIndex = subject.indexOf(suffix);
    return subject.substring(startIndex, endIndex);
  }

  private String parseFormDateTime(String[] mimeLines) {
    var prefix = "Original Message Created: ";
    for (var line : mimeLines) {
      if (line.startsWith(prefix)) {
        return line.substring(prefix.length());
      }
    }
    return "";
  }

  private String parseIncidentName(String[] mimeLines) {
    var prefix = "It Was Sent From: ";
    for (var line : mimeLines) {
      if (line.startsWith(prefix)) {
        return line.substring(prefix.length());
      }
    }
    return "";
  }

  private String parseVersion(String[] mimeLines) {
    var prefix = "Template Version: Quick Welfare Message. ";
    for (var line : mimeLines) {
      if (line.startsWith(prefix)) {
        return line.substring(prefix.length());
      }
    }
    return "";
  }

  private String parseText(String[] mimeLines) {
    var sb = new StringBuilder();
    for (var lineNumber = 2; lineNumber < mimeLines.length; ++lineNumber) {
      var line = mimeLines[lineNumber].trim();
      if (line.isEmpty()) {
        break;
      }
      sb.append(line);
    }
    return sb.toString();
  }

}
