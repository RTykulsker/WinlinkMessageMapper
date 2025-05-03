/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.processors.lineBased;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Merge all messages of expected types into a single output file. This facilitates message import into Winlink Express
 */
public class MergeMessageProcessor extends AbstractBaseProcessor {
  private Set<MessageType> expectededMessageTypes = new LinkedHashSet<MessageType>();

  private List<String> mergedMessageStrings = new ArrayList<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm);
    var expectedMessageTypesString = cm.getAsString(Key.EXPECTED_MESSAGE_TYPES, "");
    if (expectedMessageTypesString.equals("")) {
      expectededMessageTypes.addAll(Arrays.asList(MessageType.values()));
    } else {
      var expectedMessageTypesStrings = expectedMessageTypesString.split(",");
      for (var typeString : expectedMessageTypesStrings) {
        var messageType = MessageType.fromString(typeString);
        if (messageType == null) {
          logger
              .error("unknown messageType: " + messageType + " from configuration: "
                  + Key.EXPECTED_MESSAGE_TYPES.toString() + ": " + expectedMessageTypesString);
        } else {
          expectededMessageTypes.add(messageType);
        }
      }
    }
    logger.info("Expected messageTypes: " + expectedMessageTypesString);
  }

  @Override
  public void process() {
    var senderIterator = mm.getSenderIterator();
    while (senderIterator.hasNext()) {
      var sender = senderIterator.next();
      var map = mm.getMessagesForSender(sender);
      for (var messageType : map.keySet()) {
        if (!expectededMessageTypes.contains(messageType)) {
          logger.warn("### UNSUPPORTED messageType: " + messageType.toString() + " from: " + sender);
          continue;
        }
        var messages = map.get(messageType);
        for (var m : messages) {
          mergedMessageStrings.add(String.join("\n", m.lines));
        } // end for over messages of a given type
      } // end for over messageTypes
    } // end for over senders
  }

  @Override
  public void postProcess() {
    final String messagesTemplate = """
        <?xml version="1.0"?>
        <Winlink_Express_message_export>
          <export_parameters>
          </export_parameters>
          <message_list>
            #MESSAGES#
          </message_list>
        </Winlink_Express_message_export>
              """;

    FileUtils.makeDirIfNeeded(Path.of(outputPathName));
    var content = messagesTemplate.replace("#MESSAGES", String.join("\n", mergedMessageStrings));
    var fileOutputPath = Path.of(outputPath.toString(), "mergedMessages.xml");
    try {
      Files.writeString(fileOutputPath, content);
    } catch (IOException e) {
      logger.error("Could not write " + fileOutputPath.toString() + ", " + e.getLocalizedMessage());
    }
    logger.info("wrote " + mergedMessageStrings.size() + " messages to file: " + fileOutputPath.toString());

  }

}
