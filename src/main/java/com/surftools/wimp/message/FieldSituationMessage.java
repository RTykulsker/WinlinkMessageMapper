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

import java.util.LinkedHashMap;
import java.util.Map;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

/**
 * necessary because the WDT made FieldSituation, FieldSituation23, FieldSituation25, etc.
 *
 * @author bobt
 *
 */
public class FieldSituationMessage extends ExportedMessage {
  public enum ResourceType {
    POTS_LANDLINES("POTS landlines"), //
    VOIP_LANDLINES("VOIP landlines"), //
    CELL_VOICE("Cell phone voice calls"), //
    CELL_TEXT("Cell phone texts"), //
    AM_FM_BROADCAST("AM/FM Broadcast Stations"), //
    OTA_TV("OTA TV"), //
    SATELLITE_TV("Satellite TV"), //
    CABLE_TV("Cable TV"), //
    WATER_WORKS("Public Water Works"), //
    COMMERCIAL_POWER("Commercial Power"), //
    COMMERCIAL_POWER_STABLE("Commercial Power Stable"), //
    NATURAL_GAS_SUPPLY("Natural Gas Supply"), //
    INTERNET("Internet"), //
    NOAA_WEATHER_RADIO("NOAA Weather Radio"), //
    NOAA_DEGRADED("NOAA Weather Radio audio degraded"), //
    ;

    private final String key;

    private ResourceType(String key) {
      this.key = key;
    }

    @Override
    public String toString() {
      return key;
    }
  };

  public record Resource(ResourceType type, String status, String comments) {
  };

  public final String organization;
  public final LatLongPair formLocation;
  public final String precedence;
  public final String formDateTime;
  public final String task;
  public final String formTo;
  public final String formFrom;
  public final String isHelpNeeded;
  public final String neededHelp;
  public final String city;
  public final String county;
  public final String state;
  public final String territory;
  public final String landlineStatus;
  public final String landlineComments;
  public final String voipStatus;
  public final String voipComments;
  public final String cellPhoneStatus;
  public final String cellPhoneComments;
  public final String cellTextStatus;
  public final String cellTextComments;
  public final String radioStatus;
  public final String radioComments;
  public final String tvStatus;
  public final String tvComments;
  public final String satTvStatus;
  public final String satTvComments;
  public final String cableTvStatus;
  public final String cableTvComments;
  public final String waterStatus;
  public final String waterComments;
  public final String powerStatus;
  public final String powerComments;
  public final String powerStableStatus;
  public final String powerStableComments;
  public final String naturalGasStatus;
  public final String naturalGasComments;
  public final String internetStatus;
  public final String internetComments;
  public final String noaaStatus;
  public final String noaaComments;
  public final String noaaAudioDegraded;
  public final String noaaAudioDegradedComments;
  public final String additionalComments;
  public final String poc;
  public final String formVersion;

  public final Map<ResourceType, Resource> resourceMap;

  public FieldSituationMessage(ExportedMessage exportedMessage, String organization, LatLongPair formLocation, //
      String precedence, String formDateTime, String task, String formTo, String formFrom, //
      String isHelpNeeded, String neededHelp, //
      String city, String county, String state, String territory, //
      String landlineStatus, String landlineComments, //
      String voipStatus, String voipComments, //
      String cellPhoneStatus, String cellPhoneComments, //
      String cellTextStatus, String cellTextComments, //
      String radioStatus, String radioComments, //
      String tvStatus, String tvComments, //
      String satTvStatus, String satTvComments, //
      String cableTvStatus, String cableTvComments, //
      String waterStatus, String waterComments, //
      String powerStatus, String powerComments, //
      String powerStableStatus, String powerStableComments, //
      String naturalGasStatus, String naturalGasComments, //
      String internetStatus, String internetComments, //
      String noaaStatus, String noaaComments, //
      String noaaAudioDegraded, String noaaAudioDegradedComments, //
      String additionalComments, String poc, String formVersion) {
    super(exportedMessage);
    this.organization = organization;
    this.formLocation = formLocation;
    this.precedence = precedence;
    this.formDateTime = formDateTime;
    this.task = task;
    this.formTo = formTo;
    this.formFrom = formFrom;
    this.isHelpNeeded = isHelpNeeded;
    this.neededHelp = neededHelp;
    this.city = city;
    this.county = county;
    this.state = state;
    this.territory = territory;
    this.landlineStatus = landlineStatus;
    this.landlineComments = landlineComments;
    this.voipStatus = voipStatus;
    this.voipComments = voipComments;
    this.cellPhoneStatus = cellPhoneStatus;
    this.cellPhoneComments = cellPhoneComments;
    this.cellTextStatus = cellTextStatus;
    this.cellTextComments = cellTextComments;
    this.radioStatus = radioStatus;
    this.radioComments = radioComments;
    this.tvStatus = tvStatus;
    this.tvComments = tvComments;
    this.satTvStatus = satTvStatus;
    this.satTvComments = satTvComments;
    this.cableTvStatus = cableTvStatus;
    this.cableTvComments = cableTvComments;
    this.waterStatus = waterStatus;
    this.waterComments = waterComments;
    this.powerStatus = powerStatus;
    this.powerComments = powerComments;
    this.powerStableStatus = powerStableStatus;
    this.powerStableComments = powerStableComments;
    this.naturalGasStatus = naturalGasStatus;
    this.naturalGasComments = naturalGasComments;
    this.internetStatus = internetStatus;
    this.internetComments = internetComments;
    this.noaaStatus = noaaStatus;
    this.noaaComments = noaaComments;
    this.additionalComments = additionalComments;
    this.noaaAudioDegraded = noaaAudioDegraded;
    this.noaaAudioDegradedComments = noaaAudioDegradedComments;
    this.poc = poc;
    this.formVersion = formVersion;

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }

    resourceMap = new LinkedHashMap<>();
    add(ResourceType.POTS_LANDLINES, landlineStatus, landlineComments);
    add(ResourceType.VOIP_LANDLINES, voipStatus, voipComments);
    add(ResourceType.CELL_VOICE, cellPhoneStatus, cellPhoneComments);
    add(ResourceType.CELL_TEXT, cellTextStatus, cellTextComments);
    add(ResourceType.AM_FM_BROADCAST, radioStatus, radioComments);
    add(ResourceType.OTA_TV, tvStatus, tvComments);
    add(ResourceType.SATELLITE_TV, satTvStatus, satTvComments);
    add(ResourceType.CABLE_TV, cableTvStatus, cableTvComments);
    add(ResourceType.WATER_WORKS, waterStatus, waterComments);
    add(ResourceType.COMMERCIAL_POWER, powerStatus, powerComments);
    add(ResourceType.COMMERCIAL_POWER_STABLE, powerStableStatus, powerStableComments);
    add(ResourceType.NATURAL_GAS_SUPPLY, naturalGasStatus, naturalGasComments);
    add(ResourceType.INTERNET, internetStatus, internetComments);
    add(ResourceType.NOAA_WEATHER_RADIO, noaaStatus, noaaComments);
    add(ResourceType.NOAA_DEGRADED, noaaAudioDegraded, noaaAudioDegradedComments);
  }

  private void add(ResourceType type, String status, String comments) {
    var resource = new Resource(type, status, comments);
    resourceMap.put(type, resource);
  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", "Date/Time", "Latitude", "Longitude", //
        "Precedence", "Task", "FormTo", "FormFrom", "IsHelpNeeded", "NeededHelp", //
        "Organization", "City", "County", "State", "Territory", //
        "POTS Status", "POTS Comments", "VOIP Status", "VOIP Comments", //
        "Cell Voice Status", "Cell Voice Comments", "Cell Text Status", "Cell Text Comments", //
        "AM/FM Radio Status", "AM/FM Radio Comments", //
        "OTA TV Status", "OTA TV Comments", "Satellite TV Status", "Satellite TV Comments", "Cable TV Status",
        "Cable TV Comments", //
        "Water Status", "Water Comments", //
        "Power Status", "Power Comments", //
        "Power Stable", "Power Stable Comments", //
        "Natural Gas Status", "Natural Gas Comments", //
        "Internet Status", "Internet Comments", //
        "NOAA Status", "NOAA Comments", "NOAA audio degraded", "NOAA audio degraded Comments", //
        "Additional Comments", "POC", "FormVersion", "File Name" };
  }

  @Override
  public String[] getValues() {
    var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, formDateTime, latitude, longitude, //
        precedence, task, formTo, formFrom, isHelpNeeded, neededHelp, organization, city, county, state, territory, //
        landlineStatus, landlineComments, voipStatus, voipComments, //
        cellPhoneStatus, cellPhoneComments, cellTextStatus, cellTextComments, //
        radioStatus, radioComments, //
        tvStatus, tvComments, satTvStatus, satTvComments, cableTvStatus, cableTvComments, //
        waterStatus, waterComments, powerStatus, powerComments, powerStableStatus, powerStableComments, //
        naturalGasStatus, naturalGasComments, //
        internetStatus, internetComments, //
        noaaStatus, noaaComments, noaaAudioDegraded, noaaAudioDegradedComments, //
        additionalComments, poc, formVersion, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.FIELD_SITUATION;
  }

  @Override
  public String getMultiMessageComment() {
    return additionalComments;
  }

  @Override
  public String toString() {
    var headers = getHeaders();
    var values = getValues();
    var sb = new StringBuilder();
    sb.append(" + {");
    var n = Math.min(headers.length, values.length);
    for (var i = 0; i < n; ++i) {
      sb.append(headers[i] + ": " + values[i] + ", ");
    }
    sb.append("}");
    return super.toString() + sb.toString();
  }
}
