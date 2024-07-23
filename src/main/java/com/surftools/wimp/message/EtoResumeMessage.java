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

import java.util.List;

import com.surftools.wimp.core.MessageType;

/**
 * a simple set of training requirements
 *
 * @author bobt
 *
 */
public class EtoResumeMessage extends ExportedMessage {
  public final String sentBy;
  public final String formDateTime; // truly local, not utc

  public final String hasIs100;
  public final String hasIs200;
  public final String hasIs700;
  public final String hasIs800;
  public final String hasIs2200;
  public final String hasAces;
  public final String hasEc001;
  public final String hasEc016;
  public final String hasSkywarn;
  public final String hasAuxComm;
  public final String hasComT;
  public final String hasComL;
  public final List<String> agencies;

  public final String comments;
  public final String version;

  public EtoResumeMessage(ExportedMessage exportedMessage, //
      String sentBy, String formDateTime, //
      String hasIs100, String hasIs200, String hasIs700, String hasIs800, String hasIs2200, //
      String hasAces, String hasEc001, String hasEc016, String hasSkywarn, //
      String hasAuxComm, String hasComT, String hasComL, //
      List<String> agencies, String comments, String version) {
    super(exportedMessage);

    this.sentBy = sentBy;
    this.formDateTime = formDateTime;

    this.hasIs100 = hasIs100;
    this.hasIs200 = hasIs200;
    this.hasIs700 = hasIs700;
    this.hasIs800 = hasIs800;
    this.hasIs2200 = hasIs2200;

    this.hasAces = hasAces;
    this.hasEc001 = hasEc001;
    this.hasEc016 = hasEc016;
    this.hasSkywarn = hasSkywarn;

    this.hasAuxComm = hasAuxComm;
    this.hasComT = hasComT;
    this.hasComL = hasComL;
    this.agencies = agencies;

    this.comments = comments;
    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    return getStaticHeaders();
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "Latitude", "Longitude", "To", "Subject", //
        "Date", "Time", //
        "Sent By", "Form Date/Time", "Training Count", "Agency Count", //
        "IS-100", "IS-200", "IS-700", "IS-800", "IS-2200", //
        "ACES", "EC-001", "EC-016", "Skywarn", //
        "AuxComm", "ComT", "ComL", "Agencies", //
        "Comments", "Version" };
  }

  @Override
  public String[] getValues() {
    // these are what we use to map: form over message
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    return new String[] { messageId, from, latitude, longitude, to, subject, //
        date, time, //
        sentBy, formDateTime, //
        trainingCount(), //
        String.valueOf(agencies.size()), //

        hasIs100, //
        hasIs200, //
        hasIs700, //
        hasIs800, //
        hasIs2200, //

        hasAces, //
        hasEc001, //
        hasEc016, //
        hasSkywarn, //

        hasAuxComm, //
        hasComT, //
        hasComL, //

        String.join(",", agencies), //
        comments, version };
  }

  private String trainingCount() {
    var sum = 0;
    sum += hasIs100.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasIs200.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasIs700.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasIs800.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasIs2200.equalsIgnoreCase("Not Yet!") ? 0 : 1;

    sum += hasAces.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasEc001.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasEc016.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasSkywarn.equalsIgnoreCase("Not Yet!") ? 0 : 1;

    sum += hasAuxComm.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasComT.equalsIgnoreCase("Not Yet!") ? 0 : 1;
    sum += hasComL.equalsIgnoreCase("Not Yet!") ? 0 : 1;

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

}
