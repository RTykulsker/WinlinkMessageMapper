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

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.EyeWarnMessage.EyeWarnDetail;

/**
 * synthetic (non-parsed, but generated) message from one EyewarnMessage detail
 *
 * @author bobt
 *
 */

public class EyeWarnDetailMessage extends ExportedMessage {

  public final EyeWarnMessage m;
  public final EyeWarnDetail detail;

  public EyeWarnDetailMessage(EyeWarnMessage exportedMessage, EyeWarnDetail detail) {
    super(exportedMessage);

    this.m = exportedMessage;
    this.detail = detail;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "Latitude", "Longitude", "To", "Subject", //
        "Msg Date/Time", "Msg Lat/Long", //

        "Type", //
        "Form Date", "Form Time", "NCS", //
        "Incident Name", "Total Check-ins", //
        "Color", "Form Date", "Form Time", "Text", //
        "Version" };
  }

  @Override
  public String[] getValues() {
    // these are what we use to map: form over message
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, latitude, longitude, to, subject, //
        msgDateTime == null ? "" : msgDateTime.toString(), //
        msgLocation == null ? "" : msgLocation.toString(), //

        m.exerciseOrIncident, //
        m.formDate, m.formTime, m.ncs, //
        m.incidentName, m.totalCheckIns, //
        detail.color(), detail.date(), detail.time(), detail.text(), //
        m.version };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.EYEWARN_DETAIL;
  }

  @Override
  public String getMultiMessageComment() {
    return detail.toLongString();
  }

}
