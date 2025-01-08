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

import java.util.ArrayList;
import java.util.List;

import com.surftools.wimp.core.IDetailableMessage;
import com.surftools.wimp.core.MessageType;

/**
 * custom SnoVArc message, with details!
 *
 * @author bobt
 *
 */

public class EyeWarnMessage extends ExportedMessage implements IDetailableMessage {
  public record EyeWarnDetail(String color, String date, String time, String text) {

    public String toShortString() {
      return date + " " + time + " " + text;
    }

    public String toLongString() {
      return color + " " + toShortString();
    }
  };

  public final String exerciseOrIncident;
  public final String formDate;
  public final String formTime;
  public final String ncs;
  public final String incidentName;
  public final String totalCheckIns;

  public final List<EyeWarnDetail> redDetails;
  public final List<EyeWarnDetail> yellowDetails;
  public final List<EyeWarnDetail> greenDetails;

  public final String version;

  public EyeWarnMessage(ExportedMessage exportedMessage, //
      String exerciseOrIncident, //
      String formDate, String formTime, String ncs, //
      String incidentName, String totalCheckins, //
      List<EyeWarnDetail> redDetails, List<EyeWarnDetail> yellowDetails, List<EyeWarnDetail> greenDetails, //
      String version) {
    super(exportedMessage);

    this.exerciseOrIncident = exerciseOrIncident;

    this.formDate = formDate;
    this.formTime = formTime;
    this.ncs = ncs;

    this.incidentName = incidentName;
    this.totalCheckIns = totalCheckins;

    this.redDetails = redDetails;
    this.yellowDetails = yellowDetails;
    this.greenDetails = greenDetails;

    this.version = version;
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
        "# Red", "Red Reports", //
        "# Yellow", "Yellow Reports", //
        "# Green", "Green Reports", //
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

        exerciseOrIncident, //
        formDate, formTime, ncs, //
        incidentName, totalCheckIns, //

        String.valueOf(redDetails.size()), //
        String.join("\n", redDetails.stream().map(s -> s.toShortString()).toList()), //

        String.valueOf(yellowDetails.size()),
        String.join("\n", yellowDetails.stream().map(s -> s.toShortString()).toList()), //

        String.valueOf(greenDetails.size()),
        String.join("\n", greenDetails.stream().map(s -> s.toShortString()).toList()), //

        version };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.EYEWARN;
  }

  @Override
  public String getMultiMessageComment() {
    return "Red: " + redDetails.size() + ", Yellow: " + yellowDetails.size() + ", Green: " + greenDetails.size();
  }

  @Override
  public List<ExportedMessage> getDetailMessages() {
    var returnList = new ArrayList<ExportedMessage>();
    for (var detailList : List.of(redDetails, yellowDetails, greenDetails)) {
      for (var detail : detailList) {
        var detailMessage = new EyeWarnDetailMessage(this, detail);
        returnList.add(detailMessage);
      }
    }
    return returnList;
  }

  @Override
  public MessageType getDetailMessageType() {
    return MessageType.EYEWARN_DETAIL;
  }

}
