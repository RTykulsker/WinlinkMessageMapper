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

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RRIWelfareRadiogramMessage;

public class RRIWelfareRadiogramParser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {
    try {
      var mime = message.plainContent;

      var inputStream = MimeUtility.decode(new ByteArrayInputStream(mime.getBytes()), "quoted-printable");
      var demimed = new String(inputStream.readAllBytes());
      String[] mimeLines = demimed.split("\\n");

      var header = mimeLines[0];
      var address = parseAddress(mimeLines);
      var body = parseBody(mimeLines);
      var formFrom = parseFormFrom(mimeLines);
      var version = parseVersion(mimeLines);

      var m = new RRIWelfareRadiogramMessage(message, //
          header, address, body, formFrom, version);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.RRI_WELFARE_RADIOGRAM;
  }

  /**
   * all lines after first line and before first <BT>
   *
   * @param mimeLines
   * @return
   */
  private String parseAddress(String[] mimeLines) {
    var sb = new StringBuilder();
    for (var lineNumber = 1; lineNumber < mimeLines.length; ++lineNumber) {
      var line = mimeLines[lineNumber].trim();
      if (line.equals("BT")) {
        break;
      }
      sb.append(line + "\n");
    }
    return sb.toString();
  }

  /**
   * all lines after first <BT> and before second <BT>
   *
   * @param mimeLines
   * @return
   */
  private String parseBody(String[] mimeLines) {
    var btCount = 0;
    var sb = new StringBuilder();
    for (var line : mimeLines) {
      if (line.equals("BT")) {
        ++btCount;
        continue;
      }
      if (btCount == 1) {
        sb.append(line + "\n");
      }
      if (btCount == 2) {
        break;
      }
    }
    return sb.toString();
  }

  /**
   * after second <BT> and before <AR>
   *
   * @param mimeLines
   * @return
   */
  private String parseFormFrom(String[] mimeLines) {
    var btCount = 0;
    var sb = new StringBuilder();
    for (var line : mimeLines) {
      if (line.equals("BT")) {
        ++btCount;
        continue;
      }
      if (line.equals("AR")) {
        break;
      }
      if (btCount == 2) {
        sb.append(line + "\n");
      }
    }
    return sb.toString();
  }

  private String parseVersion(String[] mimeLines) {
    var prefix = "Template Version: RRI Welfare Radiogram v. ";
    for (var line : mimeLines) {
      if (line.startsWith(prefix)) {
        return line.substring(prefix.length());
      }
    }
    return "";
  }

}
