/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

package com.surftools.wimp.service.outboundMessage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.UtcDateTime;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class WinlinkExpressOutboundMessageEngine extends AbstractBaseOutboundMessageEngine {
  private static final Logger logger = LoggerFactory.getLogger(WinlinkExpressOutboundMessageEngine.class);

  private StringBuilder allMessages = new StringBuilder();
  private StringBuilder selfMessage = new StringBuilder();
  private LocalDateTime now;
  private LocalDateTime utcNow;

  private static String messagesTemplate = """
      <?xml version="1.0"?>
      <Winlink_Express_message_export>
        <export_parameters>
          <xml_file_version>1.0</xml_file_version>
          <winlink_express_version>1.7.22.0</winlink_express_version>
          <export_datetime_utc>#EXPORT_DATETIME#</export_datetime_utc>
          <callsign>#SOURCE#</callsign>
        </export_parameters>
        <message_list>
          #MESSAGES#
        </message_list>
      </Winlink_Express_message_export>
            """;

  private static String messageTemplate = """
      <message>
            <id>#MESSAGE_ID#</id>
            <foldertype>Fixed</foldertype>
            <folder>Outbox</folder>
            <subject>#SUBJECT#</subject>
            <time>#MESSAGE_TIME#</time>
            <sender>#SENDER#</sender>
            <acknowledged></acknowledged>
            <attachmentsopened></attachmentsopened>
            <replied></replied>
            <rmsoriginator></rmsoriginator>
            <rmsdestination></rmsdestination>
            <rmspath></rmspath>
            <csize></csize>
            <downloadserver></downloadserver>
            <forwarded></forwarded>
            <messageserver></messageserver>
            <precedence>2</precedence>
            <peertopeer>False</peertopeer>
            <routingflag>C</routingflag>
            <replied></replied>
            <source>#SOURCE#</source>
            <unread>False</unread>
            <flags>0</flags>
            <messageoptions>False|False||||True|</messageoptions>
            <mime>Date: #MIME_TIME#
      From: #SENDER#@winlink.org
      Reply-To: #SENDER#@winlink.org
      Subject: #SUBJECT#
      To: #TO#
      Message-ID: #MESSAGE_ID#
      X-Source: #SOURCE#
      MIME-Version: 1.0
      Content-Type: multipart/mixed; boundary="#BOUNDARY_ID#=="

      --#BOUNDARY_ID#==
      Content-Type: text/plain; charset="iso-8859-1"
      Content-Transfer-Encoding: quoted-printable

      #BODY#
      --#BOUNDARY_ID#==--</mime>
      </message>
      """;

  public WinlinkExpressOutboundMessageEngine(IConfigurationManager cm, String extraContent) {
    super(cm, extraContent);
    now = LocalDateTime.now();
    utcNow = UtcDateTime.ofNow();

    isReady = true;
  }

  @Override
  /**
   * generate text for a message
   */
  public String send(OutboundMessage m) {
    allOutput.append(m.to() + "\n" + m.body() + "\n\n");

    var messageId = generateMid(m.toString());
    var boundaryId = generateBoundaryId(messageId);

    var text = new String(messageTemplate);
    text = text.replaceAll("#MESSAGE_ID#", messageId);
    text = text.replaceAll("#BOUNDARY_ID#", boundaryId);
    text = text.replaceAll("#SOURCE#", sender);
    text = text.replaceAll("#SENDER#", sender);
    text = text.replaceAll("#SUBJECT#", m.subject());
    text = text.replaceAll("#BODY#", m.body());
    text = text.replaceAll("#TO#", m.to());

    text = text.replaceAll("#MESSAGE_TIME#", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").format(now));
    text = text
        .replaceAll("#MIME_TIME#", DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000").format(utcNow));

    allMessages.append(text);
    if (m.to().equals(source)) {
      selfMessage.append(text);
    }

    return messageId;
  }

  @Override
  public void finalizeSend() {
    super.finalizeSend();

    if (selfMessage.length() > 0) {
      finalizeForMessages(selfMessage.toString(), source + "-winlinkExpressOutboundMessages.xml");
      logger.info("Oubound message for " + source + " generated; use Winlink Express to send!");
    } else {
      logger.info("No Oubound messages for " + source + " generated");
    }

    finalizeForMessages(allMessages.toString(), "all-winlinkExpressOutboundMessages.xml");
    logger.info("Oubound messages for all generated; use Winlink Express to send!");

  }

  private void finalizeForMessages(String content, String fileName) {
    final var timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    var text = new String(messagesTemplate);
    text = text.replaceAll("#SOURCE#", source);
    text = text.replaceAll("#EXPORT_DATETIME#", timestampFormatter.format(now));
    text = text.replaceAll("#MESSAGES#", content);
    text = text.replaceAll("\n", "\r\n");
    WriteProcessor.writeString(text, fileName);
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.WINLINK_EXPRESS;
  }

}
