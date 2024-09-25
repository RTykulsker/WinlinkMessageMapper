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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.surftools.utils.counter.Counter;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.service.simpleTestService.SimpleTestService;

/**
 * more common processing, exercise processors must implement
 * specificProcessing(...)
 * 
 * explicitly for multiple messages; no TypeEntry, or typeEntryMap
 *
 * support multiple message types
 */
public abstract class CommonFeedbackProcessor extends AbstractBaseProcessor {
  protected static final String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  protected static final DateTimeFormatter ALT_DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING.replaceAll("-", "/"));
  protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  protected static Logger logger;

  protected LocalDateTime windowOpenDT = null;
  protected LocalDateTime windowCloseDT = null;

  protected boolean doStsFieldValidation = true;

  protected List<String> excludedPieChartCounterLabels = new ArrayList<>();
  public Map<String, Counter> summaryCounterMap = new LinkedHashMap<String, Counter>();

	protected Set<MessageType> acceptableMessageTypesSet = new LinkedHashSet<>(); // order matters

	protected SimpleTestService sts = new SimpleTestService();
	protected ExportedMessage message;
	protected String sender;
	protected Set<MessageType> messageTypesRequiringSecondaryAddress = new HashSet<>();
	protected Set<String> secondaryDestinations = new LinkedHashSet<>();

  protected static final String OB_DISCLAIMER = """


      =====================================================================================================

      DISCLAIMER: This feedback is provided for your consideration. We use the results to improve future
      exercises. Differences in spelling, numbers or omitting whitespace  will trigger this automated message.
      Differences in capitalization, punctuation and extra whitespace are generally ignored. You may
      think that some of our feedback is "nit picking" and that your responses would be understood by any
      reasonable person -- and you'd be correct! You're welcome to disagree with any or all of our feedback.
      You're also welcome to reply via Winlink to this message or send an email to
      ETO.Technical.Team@emcomm-training.groups.io. In any event, thank you for participating
      in this exercise. We look forward to seeing you at our next Winlink Thursday Exercise!
      """;

  protected static final String OB_NAG = """

       =====================================================================================================

       ETO needs sponsors to be able to renew our groups.io subscription for 2024.
       By sponsoring this group, you are helping pay the Groups.io hosting fees.
       Here is the link to sponsor our group:  https://emcomm-training.groups.io/g/main/sponsor
       Any amount you sponsor will be held by Groups.io and used to pay hosting fees as needed.
       The minimum sponsorship is $5.00.
       Thank you for your support!
      """;

  protected static final String OB_REQUEST_FEEDBACK = """

      =====================================================================================================

      ETO would love to hear from you! Would you please take a few minutes to answer the following questions:
      1. Were the exercise instructions clear? If not, where did they need improvement?
      2. Did you find the exercise useful?
      3. Did you find the above feedback useful?
      4. What did you dislike about the exercise?
      5. Any additional comments?

      Please reply to this Winlink message or to ETO.Technical.Team@EmComm-Training.groups.io. Thank you!
      """;

}
