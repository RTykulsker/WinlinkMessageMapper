/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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
import java.util.Collections;
import java.util.List;

import com.surftools.wimp.core.MessageType;

public class AfterActionMessage extends ExportedMessage {

  private static int nDisplayAdditionalResouces = 8;

  public final String organization;

  public final String incidentName;
  public final String page;

  public final String opFrom;
  public final String opTo;

  public final Resource selfResource;

  public final List<Resource> assignedResources;

  public final String whatWasPlanned;
  public final String whatActuallyHappened;
  public final String whyDidItHappen;
  public final String whatAreWeGoingToDoNext;

  public final String preparedBy;
  public final String preparedTime;

  public final String version;

  public static record Resource(String name, String icsPosition, String homeAgency) {

    public boolean isEmpty() {
      if (name.isEmpty() && icsPosition.isEmpty() && homeAgency.isEmpty()) {
        return true;
      }

      return false;
    }
  }

  public AfterActionMessage(ExportedMessage exportedMessage, String organization, String incidentName, String page,
      String opFrom, String opTo, Resource selfResource, List<Resource> assignedResources, //
      String whatWasPlanned, String whatActuallyHappened, String whyDidItHappen, String whatAreWeGoingToDoNext, //
      String preparedBy, String preparedTime, String version) {
    super(exportedMessage);
    this.organization = organization;

    this.incidentName = incidentName;
    this.page = page;

    this.opFrom = opFrom;
    this.opTo = opTo;

    this.selfResource = selfResource;

    this.assignedResources = assignedResources;

    this.whatWasPlanned = whatWasPlanned;
    this.whatActuallyHappened = whatActuallyHappened;
    this.whyDidItHappen = whyDidItHappen;
    this.whatAreWeGoingToDoNext = whatAreWeGoingToDoNext;

    this.preparedBy = preparedBy;
    this.preparedTime = preparedTime;

    this.version = version;
  }

  public static int getNDisplayAdditionalResouces() {
    return nDisplayAdditionalResouces;
  }

  public static void setNDisplayAdditionalResouces(int nDisplayAdditionalResouces) {
    AfterActionMessage.nDisplayAdditionalResouces = nDisplayAdditionalResouces;
  }

  @Override
  public String[] getHeaders() {
    final var fixedHeaders = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "Organization", "IncidentName", "Page", "OP From", "OP To", //
        "Name", "ICS Position", "Home Agency", "Prepared By", "Prepared Time", //
        "What Was Planned", "What Actually Happened", "Why Did It Happen", "What Are We Going To Do Next", //
        "Version", "File Name" };
    final var resourceHeaders = new String[] { "Name", "Position", "Agency" };

    var resultList = new ArrayList<String>(fixedHeaders.length + (nDisplayAdditionalResouces * 3));

    Collections.addAll(resultList, fixedHeaders);
    for (var i = 1; i <= nDisplayAdditionalResouces; ++i) {
      for (var header : resourceHeaders) {
        resultList.add(header + String.valueOf(i));
      }
    }

    return resultList.toArray(new String[resultList.size()]);
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();

    var lat = mapLocation == null ? "" : mapLocation.getLatitude();
    var lon = mapLocation == null ? "" : mapLocation.getLongitude();

    var fixedValues = new String[] { messageId, from, to, subject, date, time, //
        lat, lon, organization, incidentName, page, opFrom, opTo, //
        selfResource.name, selfResource.icsPosition, selfResource.homeAgency, preparedBy, preparedTime, //
        whatWasPlanned, whatActuallyHappened, whyDidItHappen, whatAreWeGoingToDoNext, //
        version, fileName };

    var resultList = new ArrayList<String>(fixedValues.length + (nDisplayAdditionalResouces * 3));

    Collections.addAll(resultList, fixedValues);

    for (var i = 0; i < nDisplayAdditionalResouces; ++i) {
      if (i < assignedResources.size()) {
        var r = assignedResources.get(i);
        if (r != null) {
          resultList.add(r.name == null ? "" : r.name);
          resultList.add(r.icsPosition == null ? "" : r.icsPosition);
          resultList.add(r.homeAgency == null ? "" : r.homeAgency);
        } else { // r is null
          resultList.add("");
          resultList.add("");
          resultList.add("");
        }
      } else {
        resultList.add("");
        resultList.add("");
        resultList.add("");
      }
    }

    return resultList.toArray(new String[resultList.size()]);

  }

  @Override
  public MessageType getMessageType() {
    return MessageType.AFTER_ACTION;
  }

  @Override
  public String getMultiMessageComment() {
    var s = selfResource;
    var sb = new StringBuilder();
    sb.append("name: " + s.name + ", ics: " + s.icsPosition + ", agency: " + s.homeAgency);
    sb
        .append("what was planned: " + whatWasPlanned + ", what happened; " + whatActuallyHappened + ", why: "
            + whyDidItHappen + ", next: " + whatAreWeGoingToDoNext);
    sb.append(", assigned resources: " + assignedResources.size());
    return sb.toString();
  }

}
