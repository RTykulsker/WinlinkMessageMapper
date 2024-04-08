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

import com.surftools.wimp.core.IWritableTable;

public record OutboundMessage(String from, String to, String subject, String body, String messageId)
    implements IWritableTable {

  public OutboundMessage(OutboundMessage in, String messageId) {
    this(in.from, in.to, in.subject, in.body, messageId);
  }

  public static OutboundMessage fromFields(String[] fields) {
    return new OutboundMessage(fields[0], fields[1], fields[2], fields[3], fields[4]);
  }

  private static String[] getStaticHeaders() {
    return new String[] { "From", "To", "Subject", "Body", "MessageId", };
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  @Override
  public String[] getValues() {
    return new String[] { from, to, subject, body, messageId };
  }

  @Override
  public int compareTo(IWritableTable other) {
    return toString().compareTo(other.toString());
  }

}
