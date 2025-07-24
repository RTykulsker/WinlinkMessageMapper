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
  private LocalDateTime now;

  private static String messagesTemplate = """
      <?xml version="1.0"?>
      <Winlink_Express_message_export>
        <export_parameters>
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
      <precedence></precedence>
      <peertopeer>False</peertopeer>
      <routingflag></routingflag>
      <sender>#SENDER#</sender>
      <source>#SOURCE#</source>
      <unread>False</unread>
      <flags>0</flags>
      <messageoptions></messageoptions>
      <mime>Message-ID: #MESSAGE_ID#
      Date: #MIME_TIME#
      From: #SENDER#@winlink.org
      Reply-To: #SENDER#@winlink.org
      Subject: #SUBJECT#
      To: #TO#
      X-Source: #SOURCE#
      MIME-Version: 1.0
      Content-Transfer-Encoding: quoted-printable

      #BODY#
      </mime>
      </message>
      """;

  public WinlinkExpressOutboundMessageEngine(IConfigurationManager cm, String extraContent, String fileName) {
    super(cm, extraContent, fileName);
    now = LocalDateTime.now();
  }

  @Override
  /**
   * generate text for a message
   */
  public String send(OutboundMessage m) {
    var allFeedbackText = m.body();
    if (allFeedbackTextEditor != null) {
      allFeedbackText = allFeedbackTextEditor.edit(allFeedbackText);
    }
    allOutput.append(m.to() + "\n" + allFeedbackText + "\n\n");

    /*
     * body is user-generated content. It could contain characters that could interfere with the XML wrapping around
     * messages
     */
    var body = m.body();

    if (extraContent != null) {
      body = body + "\n" + extraContent;
    }

    body = body.replaceAll("<", "&lt;");
    body = body.replaceAll("<=", "&lt;=3D");
    body = body.replaceAll(">", "&gt;");
    body = body.replaceAll(">=", "&gt;=3D");

    if (bodyTextEditor != null) {
      body = bodyTextEditor.edit(body);
    }

    var messageId = generateMid(m.toString());
    var text = new String(messageTemplate);
    text = text.replaceAll("#MESSAGE_ID#", messageId);
    text = text.replaceAll("#SOURCE#", sender);// or source to allow editing in WE
    text = text.replaceAll("#SENDER#", sender);
    text = text.replaceAll("#SUBJECT#", m.subject());
    text = text.replaceAll("#BODY#", body);
    text = text.replaceAll("#TO#", m.to());
    text = text.replaceAll("#MESSAGE_TIME#", DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").format(now));
    text = text
        .replaceAll("#MIME_TIME#",
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss +0000").format(UtcDateTime.ofNow()));

    allMessages.append(text);
    return messageId;
  }

  @Override
  public void finalizeSend() {
    super.finalizeSend();

    var text = new String(messagesTemplate);
    text = text.replaceAll("#SOURCE#", source);
    text = text
        .replaceAll("#EXPORT_DATETIME#", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(UtcDateTime.ofNow()));
    text = text.replaceAll("#MESSAGES#", allMessages.toString());
    text = text.replaceAll("\n", "\r\n");

    var aFileName = fileName == null ? "all-winlinkExpressOutboundMessages.xml" : fileName;
    WriteProcessor.writeString(text, aFileName);
    logger.info("Oubound message file " + aFileName + " written; use Winlink Express to send!");
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.WINLINK_EXPRESS;
  }

}
