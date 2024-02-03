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
import java.util.List;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

/**
 * a simple set of training requirements
 *
 * @author bobt
 *
 */
public class EtoResumeMessage extends ExportedMessage {
  public final String sentBy;
  public final LocalDateTime formDateTime; // truly local, not utc
  public final LatLongPair formLocation;

  public final boolean hasIs100;
  public final boolean hasIs200;
  public final boolean hasIs700;
  public final boolean hasIs800;
  public final boolean hasAces;
  public final boolean hasEc001;
  public final boolean hasEc016;
  public final boolean hasSkywarn;
  public final boolean hasAuxComm;
  public final boolean hasComT;
  public final boolean hasComL;
  public final List<String> agencies;

  public final String comments;
  public final String version;

  public EtoResumeMessage(ExportedMessage exportedMessage, //
      String sentBy, LocalDateTime formDateTime, LatLongPair formLocation, //
      boolean hasIs100, boolean hasIs200, boolean hasIs700, boolean hasIs800, //
      boolean hasAces, boolean hasEc001, boolean hasEc016, boolean hasSkywarn, //
      boolean hasAuxComm, boolean hasComT, boolean hasComL, //
      List<String> agencies, String comments, String version) {
    super(exportedMessage);

    this.sentBy = sentBy;
    this.formDateTime = formDateTime;
    this.formLocation = formLocation;

    this.hasIs100 = hasIs100;
    this.hasIs200 = hasIs200;
    this.hasIs700 = hasIs700;
    this.hasIs800 = hasIs800;

    this.hasAces = hasAces;
    this.hasEc001 = hasEc001;
    this.hasEc016 = hasEc016;
    this.hasSkywarn = hasSkywarn;

    this.hasAuxComm = hasAuxComm;
    this.hasComT = hasComT;
    this.hasComL = hasComL;
    this.agencies = agencies == null ? List.of("") : agencies;

    this.comments = comments;
    this.version = version;

    if (formDateTime != null) {
      setSortDateTime(formDateTime);
    }

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  @Override
  public String[] getValues() {
    // these are what we use to map: form over message
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, latitude, longitude, to, subject, //
        msgDateTime == null ? "" : msgDateTime.toString(), //
        msgLocation == null ? "" : msgLocation.toString(), //

        sentBy, //
        formDateTime == null ? "" : formDateTime.toString(), //
        formLocation == null ? "" : formLocation.toString(), //
        trainingCount(), //
        String.valueOf(agencies.size()), //

        String.valueOf(hasIs100), //
        String.valueOf(hasIs200), //
        String.valueOf(hasIs700), //
        String.valueOf(hasIs800), //

        String.valueOf(hasAces), //
        String.valueOf(hasEc001), //
        String.valueOf(hasEc016), //
        String.valueOf(hasSkywarn), //

        String.valueOf(hasAuxComm), //
        String.valueOf(hasComT), //
        String.valueOf(hasComL), //

        String.join(",", agencies), //
        comments, version };
  }

  private String trainingCount() {
    var sum = 0;
    sum += hasIs100 ? 1 : 0;
    sum += hasIs200 ? 1 : 0;
    sum += hasIs700 ? 1 : 0;
    sum += hasIs800 ? 1 : 0;

    sum += hasAces ? 1 : 0;
    sum += hasEc001 ? 1 : 0;
    sum += hasEc016 ? 1 : 0;
    sum += hasSkywarn ? 1 : 0;

    sum += hasAuxComm ? 1 : 0;
    sum += hasComT ? 1 : 0;
    sum += hasComL ? 1 : 0;

    return String.valueOf(sum);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.ETO_RESUME;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "Latitude", "Longitude", "To", "Subject", //
        "Msg Date/Time", "Msg Lat/Long", //
        "Sent By", "Form Date/Time", "Form Location", "Training Count", "Agency Count", //
        "IS-100", "IS-200", "IS-700", "IS-800", //
        "ACES", "EC-001", "EC-016", "Skywarn", //
        "AuxComm", "ComT", "ComL", "Agencies", //
        "Comments", "Version" };
  }

}
