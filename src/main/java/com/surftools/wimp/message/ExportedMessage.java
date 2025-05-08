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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IMessage;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;

/**
 * base class of all parsed Winlink Messages
 *
 * all fields, but "to" may or may not be present, and are parsed with no interpretation
 *
 * @author bobt
 *
 */
public class ExportedMessage implements IMessage, IWritableTable {
  public record ExportedKey(String from, String messageId) {
  };

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

  public final String messageId;
  public final String from; // aka sender, which can be a tactical address
  public final String source; // "owner" of the Winlink Express instance, license holder, with password
  public final String to;
  public final String toList;
  public final String ccList;
  public final String subject;

  public final LocalDateTime msgDateTime; // date/time (UTC) from message meta-data
  public final LatLongPair msgLocation; // location from message meta-data; may or may not be present
  public final String msgLocationSource;

  public final String mime;
  public final String plainContent;
  public final Map<String, byte[]> attachments;

  public LocalDateTime sortDateTime; // date/time used for sorting
  public LatLongPair mapLocation; // location used for mapping

  public final boolean isP2p;
  public final String fileName;
  public final List<String> lines;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((messageId == null) ? 0 : messageId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ExportedMessage other = (ExportedMessage) obj;
    if (messageId == null) {
      if (other.messageId != null) {
        return false;
      }
    } else if (!messageId.equals(other.messageId)) {
      return false;
    }
    return true;
  }

  public ExportedMessage(String messageId, String from, String source, String to, String toList, String ccList, //
      String subject, LocalDateTime dateTime, //
      LatLongPair location, String locationSource, //
      String mime, String plainContent, Map<String, byte[]> attachments, boolean isP2p, String fileName,
      List<String> lines) {
    this.messageId = messageId;
    this.from = from;
    this.source = source;
    this.to = to;
    this.toList = toList;
    this.ccList = ccList;
    this.subject = subject;

    this.msgDateTime = dateTime;
    this.msgLocation = location;
    this.msgLocationSource = locationSource;

    this.mime = mime;
    this.plainContent = plainContent;
    this.attachments = attachments;

    this.sortDateTime = dateTime;
    this.mapLocation = location;

    this.isP2p = isP2p;
    this.fileName = fileName;
    this.lines = lines;
  }

  public void setSortDateTime(LocalDateTime dateTime) {
    this.sortDateTime = dateTime;
  }

  public void setMapLocation(LatLongPair location) {
    this.mapLocation = location;
  }

  public ExportedMessage(ExportedMessage exportedMessage) {
    this.messageId = exportedMessage.messageId;
    this.from = exportedMessage.from;
    this.source = exportedMessage.source;
    this.to = exportedMessage.to;
    this.toList = exportedMessage.toList;
    this.ccList = exportedMessage.ccList;
    this.subject = exportedMessage.subject;

    this.msgDateTime = exportedMessage.msgDateTime;
    this.msgLocation = exportedMessage.msgLocation;
    this.msgLocationSource = exportedMessage.msgLocationSource;

    this.mime = exportedMessage.mime;
    this.plainContent = exportedMessage.plainContent;
    this.attachments = exportedMessage.attachments;

    this.sortDateTime = exportedMessage.sortDateTime;
    this.mapLocation = exportedMessage.mapLocation;
    this.isP2p = exportedMessage.isP2p;
    this.fileName = exportedMessage.fileName;
    this.lines = exportedMessage.lines;
  }

  @Override
  public String toString() {
    var nAttachments = attachments == null ? 0 : attachments.size();
    var attachmentNames = nAttachments == 0 ? "" : String.join(",", attachments.keySet());
    String attachmentsString = "\n" + nAttachments + " attachments(" + attachmentNames + ")\n";
    return "ExportedMessage {messageId: " + messageId + ", from: " + from + ", to: " + to + ", subject: " + subject
        + ", date: " + msgDateTime.toLocalDate() + ", time: " + msgDateTime.toLocalTime() + ", plainContent: \n"
        + plainContent + attachmentsString + ", fileName: " + fileName + "}";
  }

  public String[] getMimeLines() {
    return mime.split("\n");
  }

  public String getPlainContent() {
    return plainContent;
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "ToList", "CcList", "Subject", //
        "Date", "Time", "Latitude", "Longitude", "LocSource", //
        "Plain Content", "#Attachments", "FileName"//
    };
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var lat = mapLocation == null ? "" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "" : mapLocation.getLongitude();
    var nAttachments = attachments == null ? "" : String.valueOf(attachments.size());
    return new String[] { messageId, from, to, toList, ccList, subject, //
        date, time, lat, lon, msgLocationSource, //
        plainContent, nAttachments, fileName };
  }

  public MessageType getMessageType() {
    return MessageType.EXPORTED;
  }

  /**
   * default behavior will be to return empty string
   *
   * @return
   */
  @Override
  public String getMultiMessageComment() {
    return "";
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (ExportedMessage) other;
    var cmp = this.sortDateTime.compareTo(o.sortDateTime);
    if (cmp != 0) {
      return cmp;
    }
    cmp = this.from.compareTo(o.from);
    return cmp;
  }

}
