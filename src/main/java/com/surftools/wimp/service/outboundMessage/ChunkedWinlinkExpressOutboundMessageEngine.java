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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.UtcDateTime;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * like the old, WinlinkExpressOutboundMessageEngine, but output "chunks",
 *
 * since I erroneously believed that chunking would solve problems caused by writing message bodies with '>' or '<'
 * characters that caused Winlink Express to hang
 *
 */
public class ChunkedWinlinkExpressOutboundMessageEngine extends AbstractBaseOutboundMessageEngine {
  private static final Logger logger = LoggerFactory.getLogger(ChunkedWinlinkExpressOutboundMessageEngine.class);

  private Map<Integer, StringBuilder> chunkMap = new HashMap<>();
  private int chunkIndex = 1;
  private StringBuilder currentChunk = new StringBuilder();
  private final int MAX_MESSAGES_PER_CHUNK = Integer.MAX_VALUE;
  private int maxMessagesPerChunk = MAX_MESSAGES_PER_CHUNK;
  private int messagesInChunk = 0;

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
      <subject></subject>
      <time></time>
      <sender></sender>
      <precedence></precedence>
      <peertopeer>False</peertopeer>
      <routingflag></routingflag>
      <source></source>
      <unread>False</unread>
      <flags>0</flags>
      <messageoptions></messageoptions>
      <mime>Message-ID: #MESSAGE_ID#
      Content-Transfer-Encoding: 8bit
      Content-Type: text/plain; charset=ISO-8859-1
      Date: #MIME_TIME#
      From: #SENDER#@winlink.org
      Reply-To: #SENDER#@winlink.org
      Subject: #SUBJECT#
      To: #TO#
      Type: Private
      X-Source: #SOURCE#

      #BODY#
      </mime>
      </message>
      """;

  public ChunkedWinlinkExpressOutboundMessageEngine(IConfigurationManager cm, String extraContent, String fileName) {
    super(cm, extraContent, fileName);
    now = LocalDateTime.now();

    maxMessagesPerChunk = cm.getAsInt(Key.OUTBOUND_MESSAGE_EXTRA_CONTEXT, MAX_MESSAGES_PER_CHUNK);
    chunkMap.put(chunkIndex, currentChunk);
  }

  @Override
  /**
   * generate text for a message
   */
  public String send(OutboundMessage m) {
    allOutput.append(m.to() + "\n" + m.body() + "\n\n");

    /*
     * body is user-generated content. It could contain characters that could interfere with the XML wrapping around
     * messages
     */
    var body = m.body();
    body = body.replaceAll("<", "&lt;");
    body = body.replaceAll("<=", "&lt;=3D");
    body = body.replaceAll(">", "&gt;");
    body = body.replaceAll(">=", "&gt;=3D");

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

    currentChunk.append(text);
    ++messagesInChunk;
    if (messagesInChunk == maxMessagesPerChunk) {
      chunkMap.put(chunkIndex, new StringBuilder(currentChunk));
      currentChunk = new StringBuilder();
      messagesInChunk = 0;
      ++chunkIndex;
      chunkMap.put(chunkIndex, currentChunk);
    }

    return messageId;
  }

  @Override
  public void finalizeSend() {
    super.finalizeSend();

    var nChunks = chunkMap.size() - ((messagesInChunk == 0) ? 1 : 0);
    for (var chunkIndex = 1; chunkIndex <= nChunks; ++chunkIndex) {
      var text = new String(messagesTemplate);
      text = text.replaceAll("#SOURCE#", source);
      text = text
          .replaceAll("#EXPORT_DATETIME#", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(UtcDateTime.ofNow()));
      text = text.replaceAll("#MESSAGES#", chunkMap.get(chunkIndex).toString());
      text = text.replaceAll("\n", "\r\n");

      var aFileName = fileName == null ? "all-winlinkExpressOutboundMessages.xml" : fileName;
      aFileName = makeChunkedFileName(aFileName, chunkIndex, nChunks);
      WriteProcessor.writeString(text, aFileName);
      logger.info("Oubound message file " + aFileName + " written; use Winlink Express to send!");
    }

  }

  private String makeChunkedFileName(String aFileName, int iChunk, int nChunks) {
    if (nChunks == 1) {
      return aFileName;
    }

    var chunkString = "-chunk-" + iChunk + "-of-" + nChunks;
    var indexOfLastDot = aFileName.lastIndexOf(".");
    if (indexOfLastDot >= 0) {
      var newFileName = aFileName.substring(0, indexOfLastDot);
      newFileName += chunkString;
      newFileName += aFileName.substring(indexOfLastDot);
      return newFileName;
    } else {
      return aFileName + "." + chunkString;
    }
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.WINLINK_EXPRESS;
  }

}
