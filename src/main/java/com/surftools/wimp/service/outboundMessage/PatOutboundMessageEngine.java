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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PatOutboundMessageEngine extends AbstractBaseOutboundMessageEngine {
  private static final Logger logger = LoggerFactory.getLogger(PatOutboundMessageEngine.class);

  private String execPath;
  private Path mailboxPath;

  public PatOutboundMessageEngine(IConfigurationManager cm, String extraContent, String fileName) {
    super(cm, extraContent, fileName);

    execPath = cm.getAsString(Key.OUTBOUND_MESSAGE_EXTRA_CONTEXT, "/usr/bin/pat");
    if (execPath == null || execPath.isEmpty()) {
      logger.warn("Configuration key: " + Key.OUTBOUND_MESSAGE_SENDER.name() + " not defined");
      return;
    }

    if (!new File(execPath).exists()) {
      logger.error("Pat exec: " + execPath + " not found");
      return;
    }

    var path = cm.getAsString(Key.PATH);
    mailboxPath = Path.of(path, "output", "mailbox");
    // FileUtils.deleteDirectory(mailboxPath);
    outboxPath = mailboxPath;

    for (var dir : List.of("archive", "in", "out", "sent")) {
      FileUtils.createDirectory(Path.of(mailboxPath.toString(), source, dir));
    }
    outboxPath = Path.of(mailboxPath.toString(), source, "out");

    isReady = true;
  }

  @Override
  public void finalizeSend() {
    super.finalizeSend();

    var args = new String[] { execPath, "--send-only", "--mbox", "\"" + mailboxPath.toString() + "\"", "connect",
        "telnet" };

    var reallyDoIt = false;
    if (reallyDoIt) {
      logger.info("ready to execute: " + String.join(" ", args));
      var processBuilder = new ProcessBuilder(args);
      try {
        var process = processBuilder.start();
        var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        var sb = new StringBuilder();
        var line = "";

        while ((line = reader.readLine()) != null) {
          sb.append(line + "\n");
        }

        int exitCode = process.waitFor();
        sb.append("\nExited with error code : " + exitCode);
        logger.info(sb.toString());
      } catch (Exception e) {
        logger.error("Exception executing pat command: " + String.join(" ", args) + ", " + e.getLocalizedMessage());
      }
    } else {
      logger
          .info(
              "oubound messages generated; enter the following\n\n" + String.join(" ", args) + "\n\nto send messages");
    }
  }

  @Override
  public EngineType getEngineType() {
    return EngineType.PAT;
  }

}
