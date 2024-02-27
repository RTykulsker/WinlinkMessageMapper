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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IParser;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.parser.AckParser;
import com.surftools.wimp.parser.CheckInParser;
import com.surftools.wimp.parser.DyfiParser;
import com.surftools.wimp.parser.EtoCheckInParser;
import com.surftools.wimp.parser.EtoCheckInV2Parser;
import com.surftools.wimp.parser.EtoResumeParser;
import com.surftools.wimp.parser.FieldSituationParser;
import com.surftools.wimp.parser.HospitalBedParser;
import com.surftools.wimp.parser.HospitalStatusParser;
import com.surftools.wimp.parser.HumanitarianNeedsParser;
import com.surftools.wimp.parser.Ics205Parser;
import com.surftools.wimp.parser.Ics213Parser;
import com.surftools.wimp.parser.Ics213RRParser;
import com.surftools.wimp.parser.Ics213ReplyParser;
import com.surftools.wimp.parser.Ics214Parser;
import com.surftools.wimp.parser.Ics309Parser;
import com.surftools.wimp.parser.MiroCheckInParser;
import com.surftools.wimp.parser.PlainParser;
import com.surftools.wimp.parser.PositionParser;
import com.surftools.wimp.parser.QuickParser;
import com.surftools.wimp.parser.SpotRepParser;
import com.surftools.wimp.parser.WindshieldDamageParser;
import com.surftools.wimp.parser.WxHurricaneParser;
import com.surftools.wimp.parser.WxLocalParser;
import com.surftools.wimp.parser.WxSevereParser;

/**
 * classify message by type
 *
 * @author bobt
 *
 */
public class ClassifierProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ClassifierProcessor.class);

  private Map<MessageType, IParser> parserMap;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    parserMap = makeParserMap();
  }

  @Override
  public void process() {
    var messages = mm.getOriginalMessages();

    if (messages != null) {
      var tmpMessageMap = new HashMap<MessageType, List<ExportedMessage>>();
      for (var message : messages) {
        if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
          logger.debug("messageId: " + message.messageId + ", from: " + message.from);
        }

        var messageType = getMessageType(message);
        var parser = parserMap.get(messageType);
        ExportedMessage parsedMessage = message;
        if (parser != null) {
          parsedMessage = parser.parse(message);
        }

        var parsedMessageType = parsedMessage.getMessageType();
        var list = tmpMessageMap.getOrDefault(parsedMessageType, new ArrayList<ExportedMessage>());
        list.add(parsedMessage);
        tmpMessageMap.put(parsedMessageType, list);
      } // end loop over messages

      mm.load(tmpMessageMap);

    }
  }

  public MessageType getMessageType(ExportedMessage message) {
    var subject = message.subject;

    /**
     * mime based
     */
    var attachments = message.attachments;
    if (attachments != null && attachments.size() > 0) {
      var attachmentNames = attachments.keySet();

      // because there is/was FSR, FSR_23, FSR_25, FSR_26 ...
      for (var name : attachmentNames) {
        if (name.startsWith(MessageType.FIELD_SITUATION.attachmentName())) {
          return MessageType.FIELD_SITUATION;
        }
      }

      if (attachmentNames.contains(MessageType.CHECK_IN.attachmentName())) {
        return MessageType.CHECK_IN;
      } else if (attachmentNames.contains(MessageType.CHECK_OUT.attachmentName())) {
        return MessageType.CHECK_OUT;
      } else if (attachmentNames.contains(MessageType.HOSPITAL_BED.attachmentName())) {
        return MessageType.HOSPITAL_BED;
      } else if (attachmentNames.contains(MessageType.SPOTREP.attachmentName())) {
        return MessageType.SPOTREP;
      } else if (attachmentNames.contains(MessageType.WX_LOCAL.attachmentName())) {
        return MessageType.WX_LOCAL;
      } else if (attachmentNames.contains(MessageType.WX_SEVERE.attachmentName())) {
        return MessageType.WX_SEVERE;
      } else if (attachmentNames.contains(MessageType.ICS_213.attachmentName())) {
        return MessageType.ICS_213;
      } else if (attachmentNames.contains(MessageType.ICS_213_REPLY.attachmentName())) {
        return MessageType.ICS_213_REPLY;
      } else if (attachmentNames.contains(MessageType.ICS_213_RR.attachmentName())) {
        return MessageType.ICS_213_RR;
      } else if (attachmentNames.contains(MessageType.ICS_214.attachmentName())) {
        return MessageType.ICS_214;
      } else if (attachmentNames.contains(MessageType.ICS_309.attachmentName())) {
        return MessageType.ICS_309;
      } else if (attachmentNames.contains(MessageType.DAMAGE_ASSESSMENT.attachmentName())) {
        return MessageType.DAMAGE_ASSESSMENT;
      } else if (attachmentNames.contains(MessageType.QUICK.attachmentName())) {
        return MessageType.QUICK;
      } else if (attachmentNames.contains(MessageType.ICS_205_RADIO_PLAN.attachmentName())) {
        return MessageType.ICS_205_RADIO_PLAN;
      } else if (attachmentNames.contains(MessageType.HUMANITARIAN_NEEDS.attachmentName())) {
        return MessageType.HUMANITARIAN_NEEDS;
      } else if (attachmentNames.contains(MessageType.HOSPITAL_STATUS.attachmentName())) {
        return MessageType.HOSPITAL_STATUS;
      }
    }
    /**
     * subject-based
     */
    if (subject.startsWith("DYFI Automatic Entry")) {
      return MessageType.DYFI;
    } else if (subject.startsWith("Hurricane Report")) {
      return MessageType.WX_HURRICANE;
    } else if (subject.startsWith("Winlink Thursday Net Check-In")
        || subject.startsWith("Re: Winlink Thursday Net Check-In")) {
      return MessageType.ETO_CHECK_IN;
    } else if (subject.startsWith("ETO Winlink Thursday Check-In")
        || subject.startsWith("Re: ETO Winlink Thursday Check-In")) {
      return MessageType.ETO_CHECK_IN_V2;
    } else if (subject.equals("MIRO Check In") || subject.startsWith("MIRO Winlink Check In")
        || subject.startsWith("MIRO After Action")) {
      return MessageType.MIRO_CHECK_IN;
    } else if (subject.equals("Position Report")) {
      return MessageType.POSITION;
    } else if (subject.startsWith("ETO Participant resume")) {
      return MessageType.ETO_RESUME;
    } else if (subject.startsWith("ACK:")) {
      return MessageType.ACK;
    } else {
      return MessageType.PLAIN;
    }
  }

  /**
   * instantiate all IProcessors, put into a map
   *
   * @return
   */
  private Map<MessageType, IParser> makeParserMap() {
    var parserMap = new HashMap<MessageType, IParser>();

    parserMap.put(MessageType.PLAIN, new PlainParser());
    parserMap.put(MessageType.ACK, new AckParser());
    parserMap.put(MessageType.POSITION, new PositionParser());

    parserMap.put(MessageType.CHECK_IN, new CheckInParser(true));
    parserMap.put(MessageType.CHECK_OUT, new CheckInParser(false));
    parserMap.put(MessageType.SPOTREP, new SpotRepParser());
    parserMap.put(MessageType.FIELD_SITUATION, new FieldSituationParser());
    parserMap.put(MessageType.DYFI, new DyfiParser());
    //
    parserMap.put(MessageType.WX_LOCAL, new WxLocalParser());
    parserMap.put(MessageType.WX_SEVERE, new WxSevereParser());
    parserMap.put(MessageType.WX_HURRICANE, new WxHurricaneParser());
    //
    parserMap.put(MessageType.HOSPITAL_BED, new HospitalBedParser());

    parserMap.put(MessageType.ICS_213, new Ics213Parser());
    parserMap.put(MessageType.ICS_213_REPLY, new Ics213ReplyParser());
    parserMap.put(MessageType.ICS_213_RR, new Ics213RRParser());
    parserMap.put(MessageType.ICS_214, new Ics214Parser());
    parserMap.put(MessageType.ICS_309, new Ics309Parser());

    parserMap.put(MessageType.ETO_CHECK_IN, new EtoCheckInParser());
    parserMap.put(MessageType.ETO_CHECK_IN_V2, new EtoCheckInV2Parser());
    parserMap.put(MessageType.MIRO_CHECK_IN, new MiroCheckInParser());

    parserMap.put(MessageType.DAMAGE_ASSESSMENT, new WindshieldDamageParser());
    parserMap.put(MessageType.QUICK, new QuickParser());
    parserMap.put(MessageType.ICS_205_RADIO_PLAN, new Ics205Parser());
    parserMap.put(MessageType.HUMANITARIAN_NEEDS, new HumanitarianNeedsParser());
    parserMap.put(MessageType.ETO_RESUME, new EtoResumeParser());
    parserMap.put(MessageType.HOSPITAL_STATUS, new HospitalStatusParser());
    for (IParser parser : parserMap.values()) {
      parser.initialize(cm, mm);
    }

    return parserMap;
  }
}
