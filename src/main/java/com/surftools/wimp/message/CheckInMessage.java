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

/**
 * the favorite message type for ETO WLT exercises
 *
 * @author bobt
 *
 */
public class CheckInMessage extends ExportedMessage {
  public final String organization;
  public final LocalDateTime formDateTime;
  public final String contactName;
  public final String initialOperators;

  public final String status; // Exercise vs real event
  public final String service; // AMATEUR or SHARES
  public final String band; // NA, Telnet, HF, VHF, UHF, SHF
  public final String mode; // Telnet, Packet, Pactor, Robust Packet, Ardop, VARA HF, VARA FM, Iridium Go, Mesh

  public final String locationString;
  public final LatLongPair formLocation;
  public final String mgrs;
  public final String gridSquare;

  public final String comments;
  public final String version;
  public final String dataSource;

  public CheckInMessage(ExportedMessage exportedMessage, String organization, //
      LocalDateTime formDateTime, String contactName, String initialOperators, //
      String status, String service, String band, String mode, //
      String locationString, LatLongPair formLocation, String mgrs, String gridSquare, //
      String comments, String version, String dataSource) {
    super(exportedMessage);
    this.organization = organization;

    this.formDateTime = formDateTime;
    this.contactName = contactName;
    this.initialOperators = initialOperators;

    this.status = status;
    this.service = service;
    this.band = band;
    this.mode = mode;

    this.locationString = locationString;
    this.formLocation = formLocation;
    this.mgrs = mgrs;
    this.gridSquare = gridSquare;

    this.comments = comments;
    this.version = version;
    this.dataSource = dataSource;

    if (formDateTime != null) {
      setSortDateTime(formDateTime);
    }

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }
  }

  public CheckInMessage(ExportedMessage exportedMessage, String organization, LatLongPair formLocation,
      LocalDateTime formDateTime, String status, String band, String mode, String comments, String version,
      String dataSource) {

    super(exportedMessage);
    this.organization = organization;

    this.formDateTime = formDateTime;
    this.contactName = null;
    this.initialOperators = null;

    this.status = status;
    this.service = null;
    this.band = band;
    this.mode = mode;

    this.locationString = null;
    this.formLocation = formLocation;
    this.mgrs = null;
    this.gridSquare = null;

    this.comments = comments;
    this.version = version;
    this.dataSource = dataSource;

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

        organization, formDateTime == null ? "" : formDateTime.toString(), //
        toList, from, contactName, initialOperators, //
        status, service, band, mode, //
        locationString, formLocation == null ? "" : formLocation.toString(), mgrs, gridSquare, //
        comments, version, dataSource, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.CHECK_IN;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  }

  public static String[] getStaticHeaders() {
    return new String[] { "MessageId", "From", "Latitude", "Longitude", "To", "Subject", //
        "Msg Date/Time", "Msg Lat/Long", //
        "Organization", "Form Date/Time", "To List", "Form From", "Station Contact", "Initial Operators", //
        "Status", "Service", "Band", "Mode", //
        "Location", "Form Lat/Long", "MGRS", "Grid Square", //
        "Comments", "Version", "Data Source", "File Name" };
  }

}
