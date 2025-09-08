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

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;

public class WA_FieldSituationMessage extends ExportedMessage {
  public static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public final String formTo;
  public final String formCc;

  public final String reportingParty; // formFrom
  public final String reportingLocation;
  public final String provideUpdate;

  public final String winlinkAddress;
  public final String servedAgency;

  public final String county;
  public final String town;
  public final LatLongPair formLocation;
  public final String formLocationSource;

  public final String frequenciesMonitored;
  public final String radioService;
  public final String precedence;
  public final LocalDateTime formDateTime;
  public final String formDateTimeString;

  public final boolean transportHighway; // true for damage
  public final boolean transportMassTransit;
  public final boolean transportRailroad;
  public final boolean transportAirport;
  public final boolean transportSeaport;
  public final boolean transportArterial;
  public final boolean transportPipeline;
  public final String transportComments;

  public final boolean utilGas; // true for damage
  public final String utilGasCo;
  public final boolean utilWater;
  public final String utilWaterCo;
  public final boolean utilSewer;
  public final String utilSewerCo;
  public final boolean utilElectric;
  public final String utilElectricCo;
  public final String utilElectricStable;
  public final String utilComments;

  public final boolean envAir; // true if hazard observed
  public final boolean envWater;
  public final boolean envLandslide;
  public final boolean envAvalanche;
  public final boolean envHazmat;
  public final boolean envFlood;
  public final String envComments;

  public final boolean healthNeeds; // true if needed
  public final String healthComments;

  public final boolean commLandline; // true if non-functional
  public final String commLandlineProvider;
  public final boolean commVoip;
  public final String commVoipProvider;
  public final boolean commCellVoice;
  public final String commCellVoiceProvider;
  public final boolean commCellText;
  public final String commCellTextProvider;
  public final boolean commCellData;
  public final String commCellDataProvider;

  public final String commCableInternet; // functionality: YES, YES but degraded, NO, Not observed
  public final String commCableInternetNotes;
  public final String commCableTelevison;
  public final String commCableTelevisionNotes;
  public final String commSatInternet;
  public final String commSatInternetNotes;
  public final String commSatTelevision;
  public final String commSatTelevisionNotes;
  public final String commOtaTelevision;
  public final String commOtaTelevisionNotes;
  public final String commAmFmRadio;
  public final String commAmFmRadioNotes;
  public final String commNoaaRadio;
  public final String commNoaaRadioNotes;

  public final String approvedBy;
  public final String approvedByTitle;
  public final LocalDateTime approvedDateTime;
  public final String approvedDateTimeString;

  public final String version;

  public WA_FieldSituationMessage(ExportedMessage exportedMessage, //
      String formTo, String formCc, //
      String reportingParty, String reportingLocation, String provideUpdate, //
      String winlinkAddress, String servedAgency, //
      String county, String town, LatLongPair formLocation, String formLocationSource, //
      String frequenciesMonitored, String radioService, String precedence, //
      LocalDateTime formDateTime, String formDateTimeString, //

      boolean transportHighway, boolean transportMassTransit, boolean transportRailroad, boolean transportAirport,
      boolean transportSeaport, boolean transportArterial, boolean transportPipeline, String transportComments, //

      boolean utilGas, String utilGasCo, //
      boolean utilWater, String utilWaterCo, //
      boolean utilSewer, String utilSewerCo, //
      boolean utilElectric, String utilElectricCo, String utilElectricStable, String utilComments, //

      boolean envAir, boolean envWater, boolean envLandslide, boolean envAvalanche, boolean envHazmat, boolean envFlood,
      String envComments, //

      boolean healthNeeds, String healthComments, //

      boolean commLandline, String commLandlineProvider, //
      boolean commVoip, String commVoipProvider, //
      boolean commCellVoice, String commCellVoiceProvider, //
      boolean commCellText, String commCellTextProvider, //
      boolean commCellData, String commCellDataProvider, //

      String commCableInternet, String commCableInternetNotes, //
      String commCableTelevison, String commCableTelevisionNotes, //
      String commSatInternet, String commSatInternetNotes, //
      String commSatTelevision, String commSatTelevisionNotes, //
      String commOtaTelevision, String commOtaTelevisionNotes, //
      String commAmFmRadio, String commAmFmRadioNotes, //
      String commNoaaRadio, String commNoaaRadioNotes, //

      String approvedBy, String approvedByTitle, LocalDateTime approvedDateTime, String approvedDateTimeString, //
      String version) {
    super(exportedMessage);

    this.formTo = formTo;
    this.formCc = formCc;

    this.reportingParty = reportingParty; // formFrom
    this.reportingLocation = reportingLocation;
    this.provideUpdate = provideUpdate;

    this.winlinkAddress = winlinkAddress;
    this.servedAgency = servedAgency;

    this.county = county;
    this.town = town;
    this.formLocation = formLocation;
    this.formLocationSource = formLocationSource;

    this.frequenciesMonitored = frequenciesMonitored;
    this.radioService = radioService;
    this.precedence = precedence;
    this.formDateTime = formDateTime;
    this.formDateTimeString = formDateTimeString;

    this.transportHighway = transportHighway; // true for damage
    this.transportMassTransit = transportMassTransit;
    this.transportRailroad = transportRailroad;
    this.transportAirport = transportAirport;
    this.transportSeaport = transportSeaport;
    this.transportArterial = transportArterial;
    this.transportPipeline = transportPipeline;
    this.transportComments = transportComments;

    this.utilGas = utilGas; // true for damage
    this.utilGasCo = utilGasCo;
    this.utilWater = utilWater;
    this.utilWaterCo = utilWaterCo;
    this.utilSewer = utilSewer;
    this.utilSewerCo = utilSewerCo;
    this.utilElectric = utilElectric;
    this.utilElectricCo = utilElectricCo;
    this.utilElectricStable = utilElectricStable;
    this.utilComments = utilComments;

    this.envAir = envAir; // true if hazard observed
    this.envWater = envWater;
    this.envLandslide = envLandslide;
    this.envAvalanche = envAvalanche;
    this.envHazmat = envHazmat;
    this.envFlood = envFlood;
    this.envComments = envComments;

    this.healthNeeds = healthNeeds; // true if needed
    this.healthComments = healthComments;

    this.commLandline = commLandline; // true if non-functional
    this.commLandlineProvider = commLandlineProvider;
    this.commVoip = commVoip;
    this.commVoipProvider = commVoipProvider;
    this.commCellVoice = commCellVoice;
    this.commCellVoiceProvider = commCellVoiceProvider;
    this.commCellText = commCellText;
    this.commCellTextProvider = commCellTextProvider;
    this.commCellData = commCellData;
    this.commCellDataProvider = commCellDataProvider;

    this.commCableInternet = commCableInternet; // functionality: YES, YES but degraded, NO, Not observed
    this.commCableInternetNotes = commCableInternetNotes;
    this.commCableTelevison = commCableTelevison;
    this.commCableTelevisionNotes = commCableTelevisionNotes;
    this.commSatInternet = commSatInternet;
    this.commSatInternetNotes = commSatInternetNotes;
    this.commSatTelevision = commSatTelevision;
    this.commSatTelevisionNotes = commSatTelevisionNotes;
    this.commOtaTelevision = commOtaTelevision;
    this.commOtaTelevisionNotes = commOtaTelevisionNotes;
    this.commAmFmRadio = commAmFmRadio;
    this.commAmFmRadioNotes = commAmFmRadioNotes;
    this.commNoaaRadio = commNoaaRadio;
    this.commNoaaRadioNotes = commNoaaRadioNotes;

    this.approvedBy = approvedBy;
    this.approvedByTitle = approvedByTitle;
    this.approvedDateTime = approvedDateTime;
    this.approvedDateTimeString = approvedDateTimeString;

    this.version = version;

    if (formLocation.isValid()) {
      setMapLocation(formLocation);
    }

  }

  @Override
  public String[] getHeaders() {
    return new String[] { "MessageId", "From", "To", "Subject", //
        "Msg Date/Time", "Msg Latitude", "Msg Longitude", //
        "Form To", "Form Cc", "Reporting Party", "Reporting Location", //
        "Can Provide Updates", //
        "Winlink Address", "Served Agency", //
        "County", "Town", //
        "Form Latitude", "Form Longitude", "Form Location Source", //
        "HF Frequencies", "Radio Service", //
        "Precedence", "Form Date/Time", //

        "Highway", "Mass Transit", "Railroad", "Airport", "Seaport", "Arterial", "Fuel", "Transport Damage Comments", //

        "Gas", "Gas Co", "Water", "Water Co", "Sewer", "Sewer Co", "Electric", "Electric Co", "Electric Stable",
        "Utility Damage Comments", //

        "Air Quality", "Water Quality", "Landslide", "Avalance", "Hazmat", "Flood/Dam", "Environment Damage Comments", //

        "Public Health/Medical", "Public Health/Medical Comments", //

        "Landline Phone", "Landline Phone Provider", "Voip Phone", "Voip Phone Provider", "Cell Voice",
        "Cell Voice Provider", "Cell Text", "Cell Text Provider", "Cell Data", "Cell Data Provider", //

        "Cable Internet", "Cable Internet Notes", "Cable Television", "Cable Television Notes", //
        "Sat Internet", "Sat Internet Notes", "Sat Television", "Sat Television Notes", "OTA Television",
        "OTA Television Notes", "AM/FM Broadcast", "AM/FM Broadcast Notes", "NOAA Radio", "NOAA Radio Notes", //

        "Approved by", "Approved Title", "Approved Date/Time", //
        "FormVersion", "File Name" };
  }

  private String b(boolean b) {
    return b ? "YES" : "NO";
  }

  private String formatDT(LocalDateTime dateTime, String source) {
    if (dateTime == null) {
      return source;
    }

    return DTF.format(dateTime);
  }

  @Override
  public String[] getValues() {
    // var latitude = mapLocation == null ? "" : mapLocation.getLatitude();
    // var longitude = mapLocation == null ? "" : mapLocation.getLongitude();

    return new String[] { messageId, from, to, subject, //
        DTF.format(msgDateTime), msgLocation.getLatitude(), msgLocation.getLongitude(), //
        formTo, formCc, //
        reportingParty, reportingLocation, //
        provideUpdate, //
        winlinkAddress, servedAgency, //
        county, town, //
        formLocation.getLatitude(), formLocation.getLongitude(), formLocationSource, //
        frequenciesMonitored, radioService, //
        precedence, formatDT(formDateTime, formDateTimeString), //

        b(transportHighway), b(transportMassTransit), b(transportRailroad), b(transportAirport), b(transportSeaport),
        b(transportArterial), b(transportPipeline), transportComments, //

        b(utilGas), utilGasCo, b(utilWater), utilWaterCo, b(utilSewer), utilSewerCo, b(utilElectric), utilElectricCo,
        utilElectricStable, utilComments, //

        b(envAir), b(envWater), b(envLandslide), b(envAvalanche), b(envHazmat), b(envFlood), envComments, //

        b(healthNeeds), healthComments, //

        b(commLandline), commLandlineProvider, b(commVoip), commVoipProvider, b(commCellVoice), commCellVoiceProvider,
        b(commCellText), commCellTextProvider, b(commCellData), commCellDataProvider, //

        commCableInternet, commCableInternetNotes, commCableTelevison, commCableTelevisionNotes, //
        commSatInternet, commSatInternetNotes, commSatTelevision, commSatTelevisionNotes, //
        commOtaTelevision, commOtaTelevisionNotes, commAmFmRadio, commAmFmRadioNotes, commNoaaRadio, commNoaaRadioNotes, //

        approvedBy, approvedByTitle, formatDT(approvedDateTime, approvedDateTimeString), //
        version, fileName };
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.WA_FIELD_SITUATION;
  }

  @Override
  public String getMultiMessageComment() {
    return "";
  }

}
