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

package com.surftools.winlinkMessageMapper.dto.message;

import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

/**
 * message created when a message doesn't follow the happy path:
 *
 * an internal server error -- bad parsing, null pointer, etc. MUST be researched explict rejection -- bad location,
 * etc.
 *
 * @author bobt
 *
 */
public class RejectionMessage extends ExportedMessage {
  public final RejectType reason;
  public final String context;

  public RejectionMessage(ExportedMessage message, RejectType reason, String context) {
    super(message);
    this.reason = reason;
    this.context = context;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Date", "Time", "Reason", "Context" };
  }

  @Override
  public String[] getValues() {
    return new String[] { messageId, from, to, date, time, reason.toString(), context };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.REJECTS;
  }

}
