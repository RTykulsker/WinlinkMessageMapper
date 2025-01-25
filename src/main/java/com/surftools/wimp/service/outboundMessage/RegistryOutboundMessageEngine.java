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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.UtcDateTime;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class RegistryOutboundMessageEngine extends AbstractBaseOutboundMessageEngine {
  private static final Logger logger = LoggerFactory.getLogger(RegistryOutboundMessageEngine.class);

  private String registryPathName;

  public RegistryOutboundMessageEngine(IConfigurationManager cm, String extraContent) {
    super(cm, extraContent);

    registryPathName = cm.getAsString(Key.OUTBOUND_MESSAGE_ENGINE_PATH);
    if (registryPathName == null || registryPathName.isEmpty()) {
      logger.warn("Configuration key: " + Key.OUTBOUND_MESSAGE_ENGINE_PATH.name() + " not defined");
      return;
    }

    if (!new File(registryPathName).exists()) {
      logger.error("Registry path: " + registryPathName + " not found");
      return;
    }

    var outboxPathName = cm.getAsString(Key.OUTBOUND_MESSAGE_MESSAGE_PATH);
    if (outboxPathName == null || outboxPathName.isEmpty()) {
      logger.warn("Configuration key: " + Key.OUTBOUND_MESSAGE_MESSAGE_PATH.name() + " not defined");
      return;
    }

    if (!new File(outboxPathName).exists()) {
      logger.error("Message path: " + outboxPathName + " not found");
      return;
    }

    outboxPath = Path.of(outboxPathName);

    isReady = true;
  }

  @Override
  /**
   * generate a b2f file representing message for PAT to send later
   */
  public String send(OutboundMessage m) {
    var messageId = super.send(m);

    // write a line to the registry
    final var latLonString = "47.520833N, 122.208333W (Grid square)";
    final var dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");
    final var timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    var utcDateTime = UtcDateTime.ofNow();
    var dateTimeString = dateTimeFormatter.format(utcDateTime);
    var timestampString = timestampFormatter.format(utcDateTime);

    var fields = new String[] { messageId, //
        dateTimeString, // yyyy/MM/dd HH:mm in UTC
        sender, source, "2", // unknown,
        "False", // unknown,
        "False|True||||True/", // unknown
        "256", // unknown, maybe messageSize
        m.subject(), //
        "Outbox", //
        "", // unknown
        "", // unknown
        "", // unknown
        "C", // unknown
        "False", // unknown
        "", // unknown
        "", // unknown
        "", // CC addresses ????
        "0", // unknown,
        timestampString, // yyyyMMddHHmmss
        "0", // unknown
        latLonString };

    var registryString = String.join("\1", fields);

    try (var writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(registryPathName, true), "UTF-8"))) {
      writer.write(registryString);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return messageId;
  }

  @Override
  public void finalizeSend() {
    super.finalizeSend();

    logger.info("Oubound messages generated; use Winlink Express to send!");
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.REGISTRY;
  }

}
