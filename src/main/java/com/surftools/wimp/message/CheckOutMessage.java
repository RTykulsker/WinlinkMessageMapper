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

package com.surftools.wimp.message;

import java.time.LocalDateTime;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

public class CheckOutMessage extends CheckInMessage {

  public CheckOutMessage(ExportedMessage exportedMessage, String organization, //
      LocalDateTime formDateTime, String contactName, String initialOperators, //
      String status, String service, String band, String mode, //
      String locationString, LatLongPair formLocation, String mgrs, String gridSquare, //
      String comments, String version, String dataSource) {
    super(exportedMessage, organization, //
        formDateTime, contactName, initialOperators, //
        status, service, band, mode, //
        locationString, formLocation, mgrs, gridSquare, //
        comments, version, dataSource);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.CHECK_OUT;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }
}
