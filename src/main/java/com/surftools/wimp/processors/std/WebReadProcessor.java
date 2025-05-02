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

package com.surftools.wimp.processors.std;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Reads an "exported message" file, produced by Winlink, creates @{ExportedMessage} records
 *
 * @author bobt
 *
 */
public class WebReadProcessor extends BaseReadProcessor {
  private static final Logger logger = LoggerFactory.getLogger(WebReadProcessor.class);

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);
  }

  @Override
  public void process() {
    super.process();

    var exportedMessages = readAll();

    logger.info("read " + exportedMessages.size() + " exported messages from all files");

    mm.load(exportedMessages);
  }

  /**
   * reads a single exported message file (uploaded from web), returns a list of ExportedMessage records
   *
   * @return
   */
  public List<ExportedMessage> readAll() {

    try {
      var webExportedMessages = (String) mm.getContextObject("webReqestMessages");
      var fileName = (String) mm.getContextObject("webFileName");
      var messages = parseExportedMessages(new ArrayList<String>(Arrays.asList(webExportedMessages.split("\n"))),
          fileName);
      logger.info("extracted " + messages.size() + " exported messages from web: ");
      return messages;
    } catch (Exception e) {
      logger.error("Exception processing web content: " + e.getLocalizedMessage());
      return new ArrayList<ExportedMessage>();
    }

  }
}
