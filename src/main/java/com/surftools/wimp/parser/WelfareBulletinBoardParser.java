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

package com.surftools.wimp.parser;

import java.util.LinkedHashMap;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.WelfareBulletinBoardMessage;
import com.surftools.wimp.message.WelfareBulletinBoardMessage.DataType;

public class WelfareBulletinBoardParser extends AbstractBaseParser {

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {
      makeDocument(message.messageId,
          new String(message.attachments.get(MessageType.WELFARE_BULLETIN_BOARD.attachmentName())));

      var dataMap = new LinkedHashMap<DataType, String>();
      for (var dt : DataType.values()) {
        dataMap.put(dt, getStringFromXml(dt.getFieldName()));
      }

      if (dataMap.get(DataType.VERSION) != null) {
        var fields = dataMap.get(DataType.VERSION).split(" ");
        dataMap.put(DataType.VERSION, fields != null && fields.length >= 4 ? fields[4] : "unknown");
      }

      var m = new WelfareBulletinBoardMessage(message, dataMap);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WELFARE_BULLETIN_BOARD;
  }

}