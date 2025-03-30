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
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * remove all messages that are or are not from a given sender
 *
 * @author bobt
 *
 */
public class FilterProcessor extends AbstractBaseProcessor {
  private final Logger logger = LoggerFactory.getLogger(FilterProcessor.class);

  public static Set<String> includeSenderSet = new HashSet<>();
  public static Set<String> excludeSenderSet = new HashSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var includeSenderString = cm.getAsString(Key.FILTER_INCLUDE_SENDERS);
    if (includeSenderString != null) {
      var fields = includeSenderString.split(",");
      for (var field : fields) {
        includeSenderSet.add(field);
      }
      logger.info("includeSenders: " + String.join(",", includeSenderSet));
    }

    var excludeSenderString = cm.getAsString(Key.FILTER_EXCLUDE_SENDERS);
    if (excludeSenderString != null) {
      var fields = includeSenderString.split(",");
      for (var field : fields) {
        excludeSenderSet.add(field);
      }
      logger.info("excludeSenders: " + String.join(",", excludeSenderSet));
    }

    if (includeSenderSet.size() == 0 && excludeSenderSet.size() == 0) {
      logger.info("nothing to filter");
      return;
    }

    for (var sender : includeSenderSet) {
      if (excludeSenderSet.contains(sender)) {
        throw new RuntimeException(
            "excludedSenderSet (" + String.join(",", excludeSenderSet) + ") contains included sender: " + sender);
      }
    }

    for (var sender : excludeSenderSet) {
      if (includeSenderSet.contains(sender)) {
        throw new RuntimeException(
            "includedSenderSet (" + String.join(",", includeSenderSet) + ") contains excluded sender: " + sender);
      }
    }
  }

  @Override
  public void process() {
    var includedSenderCount = 0;
    var excludedSenderCount = 0;

    if (includeSenderSet.size() == 0 && excludeSenderSet.size() == 0) {
      logger.info("nothing to filter");
      return;
    }

    var removeList = new ArrayList<String>();
    var it = mm.getSenderIterator();
    while (it.hasNext()) {
      var sender = it.next();

      if (includeSenderSet.size() > 0 && includeSenderSet.contains(sender)) {
        ++includedSenderCount;
      } else {
        removeList.add(sender);
      }

      if (excludeSenderSet.size() > 0 && excludeSenderSet.contains(sender)) {
        ++excludedSenderCount;
        removeList.add(sender);
      }

    } // end loop over sender

    for (var sender : removeList) {
      mm.removeMessagesForSender(sender);
    }
    mm.removeMesseagesForSenders(removeList);

    logger
        .warn("\n### included: " + includedSenderCount + " senders\n\n### excluded: " + excludedSenderCount
            + " senders\n");
  } // end process()

}
