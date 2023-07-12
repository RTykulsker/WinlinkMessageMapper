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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.surftools.wimp.core.MessageType;

/**
 * the Initial Damage Assessment / Windshield Survey form
 *
 * @author bobt
 *
 */
public class WindshieldDamageAssessmentMessage extends ExportedMessage {
  public final String organization;
  public final String jurisdiction;
  public final String missionIncidentId;
  public final String formType;
  public final String eventType;
  public final String description;
  public final String surveyArea;
  public final String surveyTeam;
  public final String eventStartDate;
  public final String surveyDate;

  public final List<DamageEntry> damageEntries;

  public final String totalLossString;

  public final String comments;
  public final String version;

  public WindshieldDamageAssessmentMessage(ExportedMessage exportedMessage, String organization, String jurisdiction,
      String missionIncidentId, String formType, String eventType, String description, String surveyArea,
      String surveyTeam, String eventStartDate, String surveyDate, List<DamageEntry> damageEntries,
      String totalLossString, String comments, String version) {
    super(exportedMessage);

    this.organization = organization;
    this.jurisdiction = jurisdiction;
    this.missionIncidentId = missionIncidentId;
    this.formType = formType;
    this.eventType = eventType;
    this.description = description;
    this.surveyArea = surveyArea;
    this.surveyTeam = surveyTeam;
    this.eventStartDate = eventStartDate;
    this.surveyDate = surveyDate;
    this.damageEntries = damageEntries;
    this.totalLossString = totalLossString;
    this.comments = comments;
    this.version = version;
  }

  @Override
  public String[] getHeaders() {
    var headers = new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", //
        "Latitude", "Longitude", //
        "Organization", "Jurisdiction", "Mission/Incident", //
        "Type", "Event", "Description", //
        "Survey Area", "Survey Team", //
        "Start Event", "Survey Date", //
        "Total Loss", //
        "Comments", "Version" };

    var categoryHeaders = new ArrayList<String>();
    for (int i = 0; i < damageEntries.size(); i++) {
      categoryHeaders.add("Category");
      categoryHeaders.add("Affected (10%)");
      categoryHeaders.add("Minor (25%)");
      categoryHeaders.add("Major (50%)");
      categoryHeaders.add("Totaled (100%)");
      categoryHeaders.add("Total Number");
      categoryHeaders.add("$ Loss");
    }

    return Stream.concat(Arrays.stream(headers), categoryHeaders.stream()).toArray(String[]::new);
  }

  @Override
  public String[] getValues() {
    var date = sortDateTime == null ? "" : sortDateTime.toLocalDate().toString();
    var time = sortDateTime == null ? "" : sortDateTime.toLocalTime().toString();
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    var values = new String[] { messageId, from, to, subject, date, time, //
        latitude, longitude, //
        organization, jurisdiction, missionIncidentId, //
        formType, eventType, description, //
        surveyArea, surveyTeam, //
        eventStartDate, surveyDate, //
        totalLossString, comments, version };

    var categoryValues = new ArrayList<String>();
    for (var entry : damageEntries) {
      categoryValues.add(entry.description());
      categoryValues.add(entry.affected());
      categoryValues.add(entry.minor());
      categoryValues.add(entry.major());
      categoryValues.add(entry.destroyed());
      categoryValues.add(entry.total());
      categoryValues.add(entry.lossString());
    }

    return Stream.concat(Arrays.stream(values), categoryValues.stream()).toArray(String[]::new);
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.DAMAGE_ASSESSMENT;
  }

  @Override
  public String getMultiMessageComment() {
    return comments;
  };

  public record DamageEntry(String description, String affected, String minor, String major, String destroyed,
      String total, String lossString, String lossAmount) {

    private static String[] names = { "n/a", "Houses", "Apt Complexes", "Mobile Homes", //
        "Residential High Rise", "Commercial High Rise", "Public Blgs", "Small Businesses", //
        "Factories/Industrial Complexes", "Roads", "Bridges", "Electrical Distribution", //
        "Schools"//
    };

    public static String getName(int index) {
      if (index < 0 || index >= names.length) {
        throw new IllegalArgumentException("index: " + index + " out of range");
      }
      return names[index];
    }
  }
}
