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

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

public class EtoCheckInV2Message extends ExportedMessage {
  public final LatLongPair formLocation;
  public final String comments;
  public final String dateString;
  public final String timeString;
  public final String formName;
  public final String version;

  public EtoCheckInV2Message(ExportedMessage exportedMessage, LatLongPair formLocation, String comments,
      String dateString, String timeString, String formName, String version) {
    super(exportedMessage);
    this.formLocation = formLocation;
    this.comments = comments;
    this.dateString = dateString;
    this.timeString = timeString;
    this.formName = formName;
    this.version = version;

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", "Latitude", "Longitude", //
        "FormName", "Comments", "Version", };
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, dateString, timeString, latitude, longitude, //
        formName, comments, version };
  }

  public String getComments() {
    return comments;
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ETO_CHECK_IN_V2;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

}
