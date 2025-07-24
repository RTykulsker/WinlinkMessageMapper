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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.apache.commons.codec.binary.Base32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.textEditor.ITextEditor;
import com.surftools.utils.textEditor.TextEditorManager;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBaseOutboundMessageEngine implements IOutboundMessageEngine {
  private static final Logger logger = LoggerFactory.getLogger(AbstractBaseOutboundMessageEngine.class);

  protected IConfigurationManager cm;
  protected final String extraContent;

  protected StringBuilder allOutput = new StringBuilder();

  protected boolean isReady;

  protected Path outboxPath;

  protected String sender;
  protected String source;
  protected String fileName;

  protected ITextEditor allFeedbackTextEditor;
  protected ITextEditor bodyTextEditor;

  public AbstractBaseOutboundMessageEngine(IConfigurationManager cm, String extraContent, String fileName) {
    this.cm = cm;

    this.extraContent = extraContent;

    sender = cm.getAsString(Key.OUTBOUND_MESSAGE_SENDER);
    if (sender == null) {
      logger.warn("Configuration key: " + Key.OUTBOUND_MESSAGE_SENDER.name() + " not defined");
      return;
    }

    source = cm.getAsString(Key.OUTBOUND_MESSAGE_SOURCE);
    if (source == null) {
      logger.warn("Configuration key: " + Key.OUTBOUND_MESSAGE_SOURCE.name() + " not defined");
      return;
    }

    this.fileName = fileName;

    var tem = new TextEditorManager();
    allFeedbackTextEditor = tem.getTextEditor(cm.getAsString(Key.PRACTICE_ALL_FEEDBACK_TEXT_EDITOR));
    bodyTextEditor = tem.getTextEditor(cm.getAsString(Key.PRACTICE_BODY_TEXT_EDITOR));

    isReady = true;
  }

  @Override
  /**
   * generate a b2f file representing message for PAT to send later
   */
  public String send(OutboundMessage m) {
    if (!isReady) {
      logger.warn("NO outbound message sent for: " + m);
      return null;
    }

    // see https://winlink.org/B2F
    String messageId = m.messageId();
    if (m.messageId() == null || m.messageId().isEmpty()) {
      messageId = generateMid(m.toString());
    }

    final String SEP = "\r\n";

    final LocalDateTime nowUTC = LocalDateTime.now(Clock.systemUTC());
    final String dateString = nowUTC.format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));

    var body = m.body();

    // translate non-ascii characters
    body = body.replaceAll("[^\\x00-\\x7F]", ".");

    if (extraContent != null) {
      body = body + "\n" + extraContent;
    }
    body = body.replaceAll("\n", SEP);
    body = body.replaceAll("\\u009d", "");

    if (bodyTextEditor != null) {
      body = bodyTextEditor.edit(body);
    }

    var to = expandToAddresses(m.to());

    var doDebugToAddress = false;
    if (doDebugToAddress) {
      to = expandToAddresses("RTykulsker@gmail.com");
    }

    var sb = new StringBuilder();
    sb.append("Mid: " + messageId + SEP);
    sb.append("Body: " + (body.length() + SEP.length()) + SEP);
    sb.append("Content-Transfer-Encoding: 8bit" + SEP);
    sb.append("Content-Type: text/plain; charset=ISO-8859-1" + SEP);
    sb.append("Date: " + dateString + SEP);
    sb.append("From: " + sender + SEP);
    sb.append("Mbo: " + source + SEP);
    sb.append("Subject: " + m.subject() + SEP);
    sb.append("To: " + to + SEP);
    sb.append("Type: Private" + SEP);
    sb.append(SEP);
    sb.append(body + SEP);

    var outputPath = Path.of(outboxPath.toString(), messageId + ".b2f");
    try {
      Files.writeString(outputPath, sb.toString());
      logger.debug("wrote b2f file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing b2f file: " + outputPath + ", " + e.getLocalizedMessage());
    }

    var text = m.body();
    if (allFeedbackTextEditor != null) {
      text = allFeedbackTextEditor.edit(text);
    }

    allOutput.append(m.to() + "\n" + text + "\n\n");
    return messageId;
  }

  @Override
  public void finalizeSend() {
    if (!isReady) {
      return;
    }

    try {
      var path = cm.getAsString(Key.PATH);
      Files.writeString(Path.of(path, "allFeedback.txt"), allOutput.toString());
    } catch (Exception e) {
      logger.error("error writing allFeedback.txt: " + e.getLocalizedMessage());
    }

  }

  /**
   * fix addresses: add "SMTP:" to non-Winlink addresses
   *
   * @param toAddressString
   * @return
   */
  protected String expandToAddresses(String toAddressString) {
    var list = new ArrayList<String>();
    var addresses = toAddressString.split(",");
    for (var address : addresses) {
      if (address.contains("@") && !address.toLowerCase().endsWith("@winlink.org")) {
        address = "SMTP:" + address;
      }
      list.add(address);
    }
    return String.join(",", list);
  }

  public static String generateMid(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String stringToHash = string + System.nanoTime();
      md.update(stringToHash.getBytes());
      byte[] digest = md.digest();
      Base32 base32 = new Base32();
      String encodedString = base32.encodeToString(digest);
      String subString = encodedString.substring(0, 12);
      return subString;
    } catch (Exception e) {
      throw new RuntimeException("could not generate messageId: " + e.getMessage());
    }
  }

  public static String generateBoundaryId(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String stringToHash = string + System.nanoTime();
      md.update(stringToHash.getBytes());
      byte[] digest = md.digest();
      Base32 base32 = new Base32();
      String encodedString = base32.encodeToString(digest);
      String subString = encodedString.substring(0, 6);
      return "boundary" + subString;
    } catch (Exception e) {
      throw new RuntimeException("could not generate boundaryId: " + e.getMessage());
    }
  }

  @Override
  public boolean isReady() {
    return isReady;
  }
}
