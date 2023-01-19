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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.counter.Counter;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;

/**
 * because we might be expecting messages only to certain destinations
 *
 * @author bobt
 *
 */
public class MissingDestinationProcessor extends AbstractBaseProcessor {
  private final Logger logger = LoggerFactory.getLogger(MissingDestinationProcessor.class);

  private Set<String> expectedDestinations = new LinkedHashSet<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var expectedDestinationsString = cm.getAsString(Key.EXPECTED_DESTINATIONS);
    if (expectedDestinationsString != null) {
      var fields = expectedDestinationsString.split(",");
      for (var field : fields) {
        expectedDestinations.add(field);
      }

      logger.info("Expected Destinations: " + expectedDestinations.toString());
    }

  }

  @SuppressWarnings("unchecked")
  @Override
  public void process() {
    var missingExpectedDestinations = new TreeSet<String>(expectedDestinations);
    var it = mm.getMessageTypeIteror();
    var expectedDestinationCounter = new Counter();
    var unexpectedDestinationCounter = new Counter();
    while (it.hasNext()) {
      var messageType = it.next();
      var messages = mm.getMessagesForType(messageType);
      for (var message : messages) {
        var destination = message.to;
        if (expectedDestinations.contains(destination)) {
          expectedDestinationCounter.increment(destination);
          missingExpectedDestinations.remove(destination);
        } else {
          unexpectedDestinationCounter.increment(destination);
        }
      } // end loop over messages
    } // end loop over messageType

    var sb = new StringBuilder();

    sb.append("\n\nMissing Expected Destinations: " + missingExpectedDestinations.toString() + "\n");
    sb
        .append("\nExpected Destinations:\n"
            + formatCounter(expectedDestinationCounter.getAscendingKeyIterator(), "destination", "count"));

    sb
        .append("\nUnexpected Destinations:\n"
            + formatCounter(unexpectedDestinationCounter.getAscendingKeyIterator(), "destination", "count"));

    logger.info(sb.toString());
  } // end process()

}
