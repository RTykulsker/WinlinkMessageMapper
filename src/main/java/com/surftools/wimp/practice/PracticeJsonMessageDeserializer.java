/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.practice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.FieldSituationMessage;
import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Hics259Message.CasualtyEntry;
import com.surftools.wimp.message.Ics205Message;
import com.surftools.wimp.message.Ics205Message.RadioEntry;
import com.surftools.wimp.message.Ics213Message;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics213RRMessage.LineItem;

/**
 * between LocalDates, LatLongPairs and so many final members, it'll be easier to deserialize by hand rather than using
 * Jackson
 */
public class PracticeJsonMessageDeserializer {
  private static final Logger logger = LoggerFactory.getLogger(PracticeJsonMessageDeserializer.class);

  private final ObjectMapper mapper;

  public PracticeJsonMessageDeserializer() {
    mapper = new ObjectMapper();
  }

  public ExportedMessage deserialize(String jsonString, MessageType messageType) {
    try {
      switch (messageType) {
      case ICS_213:
        return deserialize_Ics213Message(jsonString);

      case ICS_213_RR:
        return deserialize_Ics213RRMessage(jsonString);

      case ICS_205:
        return deserialize_Ics205Message(jsonString);

      case FIELD_SITUATION:
        return deserialize_FsrMessage(jsonString);

      case HICS_259:
        return deserialize_Hics259Message(jsonString);

      default:
        throw new RuntimeException("unsupported messageType: " + messageType.toString());
      }
    } catch (Exception e) {
      logger
          .error("Exception deserializing messageType: " + messageType.toString() + ", json: " + jsonString
              + ", error: " + e.getMessage());
      throw new RuntimeException(e);
    }
  }

  private ExportedMessage deserialize_ExportedMessage(JsonNode json) {
    var messageId = json.get("messageId").asText();
    var from = json.get("from").asText();
    var source = json.get("source").asText();
    var to = json.get("to").asText();
    var toList = json.get("toList").asText();
    var ccList = json.get("ccList").asText();
    var subject = json.get("subject").asText();
    var msgDateTime = deserialize_LocalDateTime(json.get("msgDateTime"));
    var msgLocation = deserialize_LatLongPair(json.get("msgLocation"));
    var locationSource = json.get("msgLocationSource").asText();
    var mime = json.get("mime").asText();
    var plainContent = json.get("plainContent").asText();
    var attachments = new HashMap<String, byte[]>();
    var isP2p = json.get("from").asBoolean();
    var fileName = json.get("fileName").asText();
    List<String> lines = null;

    var exportedMessage = new ExportedMessage(messageId, from, source, to, toList, ccList, //
        subject, msgDateTime, //
        msgLocation, locationSource, //
        mime, plainContent, attachments, isP2p, fileName, lines);
    return exportedMessage;
  }

  private LocalDateTime deserialize_LocalDateTime(JsonNode json) {
    var year = json.get(0).asInt();
    var month = json.get(1).asInt();
    var day = json.get(2).asInt();
    var hour = json.get(3).asInt();
    var min = json.get(4).asInt();
    var dateTime = LocalDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, min));
    return dateTime;
  }

  private LatLongPair deserialize_LatLongPair(JsonNode json) {
    var latitude = json.get("latitude").asText();
    var longitude = json.get("longitude").asText();
    var pair = new LatLongPair(latitude, longitude);
    return pair;
  }

  private Ics213Message deserialize_Ics213Message(String jsonString)
      throws JsonMappingException, JsonProcessingException {
    var json = mapper.readTree(jsonString);
    var exportedMessage = deserialize_ExportedMessage(json);
    var organization = json.get("organization").asText();
    var incidentName = json.get("incidentName").asText();
    var formFrom = json.get("formFrom").asText();
    var formTo = json.get("formTo").asText();
    var formSubject = json.get("formSubject").asText();
    var formDate = json.get("formDate").asText();
    var formTime = json.get("formTime").asText();
    var formMessage = json.get("formMessage").asText();
    var approvedBy = json.get("approvedBy").asText();
    var position = json.get("position").asText();
    var isExercise = json.get("isExercise").asBoolean();
    var formLocation = deserialize_LatLongPair(json.get("formLocation"));
    var version = json.get("version").asText();
    var dataSource = json.get("dataSource").asText();

    var m = new Ics213Message(exportedMessage, organization, incidentName, //
        formFrom, formTo, formSubject, formDate, formTime, //
        formMessage, approvedBy, position, //
        isExercise, formLocation, version, dataSource);
    return m;
  }

  private Ics213RRMessage deserialize_Ics213RRMessage(String jsonString)
      throws JsonMappingException, JsonProcessingException {
    var json = mapper.readTree(jsonString);
    var exportedMessage = deserialize_ExportedMessage(json);

    var organization = json.get("organization").asText();
    var incidentName = json.get("incidentName").asText();
    var activityDateTime = json.get("activityDateTime").asText();
    var requestNumber = json.get("requestNumber").asText();

    var lineItems = new ArrayList<LineItem>();
    var jsonLineItems = json.get("lineItems");
    var it = jsonLineItems.iterator();
    while (it.hasNext()) {
      var jsonLineItem = it.next();
      var quantity = jsonLineItem.get("quantity").asText();
      var kind = jsonLineItem.get("kind").asText();
      var type = jsonLineItem.get("type").asText();
      var item = jsonLineItem.get("item").asText();
      var requestedDateTime = jsonLineItem.get("requestedDateTime").asText();
      var estimatedDateTime = jsonLineItem.get("estimatedDateTime").asText();
      var cost = jsonLineItem.get("cost").asText();
      var lineItem = new LineItem(quantity, kind, type, item, requestedDateTime, estimatedDateTime, cost);
      lineItems.add(lineItem);
    }

    var delivery = json.get("delivery").asText();
    var substitutes = json.get("substitutes").asText();
    var requestedBy = json.get("requestedBy").asText();
    var priority = json.get("priority").asText();
    var approvedBy = json.get("approvedBy").asText();

    var logisticsOrderNumber = json.get("logisticsOrderNumber").asText();
    var supplierInfo = json.get("supplierInfo").asText();
    var supplierName = json.get("supplierName").asText();
    var supplierPointOfContact = json.get("supplierPointOfContact").asText();
    var supplyNotes = json.get("supplyNotes").asText();
    var logisticsAuthorizer = json.get("logisticsAuthorizer").asText();
    var logisticsDateTime = json.get("logisticsDateTime").asText();
    var orderedBy = json.get("orderedBy").asText();

    var financeComments = json.get("financeComments").asText();
    var financeName = json.get("financeName").asText();
    var financeDateTime = json.get("financeDateTime").asText();

    var m = new Ics213RRMessage(exportedMessage, organization, incidentName, activityDateTime, requestNumber, //
        lineItems, //
        delivery, substitutes, requestedBy, priority, approvedBy, //
        logisticsOrderNumber, supplierInfo, supplierName, //
        supplierPointOfContact, supplyNotes, logisticsAuthorizer, //
        logisticsDateTime, orderedBy, //
        financeComments, financeName, financeDateTime);

    return m;
  }

  private Ics205Message deserialize_Ics205Message(String jsonString)
      throws JsonMappingException, JsonProcessingException {
    var json = mapper.readTree(jsonString);
    var exportedMessage = deserialize_ExportedMessage(json);

    var organization = json.get("organization").asText();
    var incidentName = json.get("incidentName").asText();
    var dateTimePreparedString = json.get("dateTimePrepared").asText();

    var dateFrom = json.get("dateFrom").asText();
    var dateTo = json.get("dateTo").asText();
    var timeFrom = json.get("timeFrom").asText();
    var timeTo = json.get("timeTo").asText();

    var specialInstructions = json.get("specialInstructions").asText();
    var approvedBy = json.get("approvedBy").asText();
    var dateTimeApprovedString = json.get("approvedDateTime").asText();
    var iapPageString = json.get("iapPage").asText();
    var version = json.get("version").asText();

    var radioEntries = new ArrayList<RadioEntry>();
    var jsonRadioEntries = json.get("radioEntries");
    var it = jsonRadioEntries.iterator();
    while (it.hasNext()) {
      var jsonRadioEntry = it.next();
      var rowNumber = jsonRadioEntry.get("rowNumber").asInt();
      var zoneGroup = jsonRadioEntry.get("zoneGroup").asText();
      var channelNumber = jsonRadioEntry.get("channelNumber").asText();
      var function = jsonRadioEntry.get("function").asText();
      var channelName = jsonRadioEntry.get("channelName").asText();
      var assignment = jsonRadioEntry.get("assignment").asText();
      var rxFrequency = jsonRadioEntry.get("rxFrequency").asText();
      var rxNarrowWide = jsonRadioEntry.get("rxNarrowWide").asText();
      var rxTone = jsonRadioEntry.get("rxTone").asText();
      var txFrequency = jsonRadioEntry.get("txFrequency").asText();
      var txNarrowWide = jsonRadioEntry.get("txNarrowWide").asText();
      var txTone = jsonRadioEntry.get("txTone").asText();
      var mode = jsonRadioEntry.get("mode").asText();
      var remarks = jsonRadioEntry.get("remarks").asText();
      var radioEntry = new RadioEntry(rowNumber, //
          zoneGroup, channelNumber, function, channelName, assignment, //
          rxFrequency, rxNarrowWide, rxTone, //
          txFrequency, txNarrowWide, txTone, //
          mode, remarks);
      radioEntries.add(radioEntry);
    }

    var m = new Ics205Message(exportedMessage, organization, incidentName, //
        dateTimePreparedString, dateFrom, dateTo, timeFrom, timeTo, //
        specialInstructions, approvedBy, dateTimeApprovedString, iapPageString, //
        radioEntries, version);

    return m;
  }

  private FieldSituationMessage deserialize_FsrMessage(String jsonString)
      throws JsonMappingException, JsonProcessingException {
    var json = mapper.readTree(jsonString);
    var message = deserialize_ExportedMessage(json);

    var organization = json.get("organization").asText();
    var formLocation = deserialize_LatLongPair(json.get("formLocation"));
    var precedence = json.get("precedence").asText();
    var formDateTime = json.get("formDateTime").asText();
    var task = json.get("task").asText();
    var formTo = json.get("formTo").asText();
    var formFrom = json.get("formFrom").asText();
    var isHelpNeeded = json.get("isHelpNeeded").asText();
    var neededHelp = json.get("neededHelp").asText();

    var city = json.get("city").asText();
    var county = json.get("county").asText();
    var state = json.get("state").asText();
    var territory = json.get("territory").asText();

    var landlineStatus = json.get("landlineStatus").asText();
    var landlineComments = json.get("landlineComments").asText();

    var voipStatus = json.get("voipStatus").asText();
    var voipComments = json.get("voipComments").asText();

    var cellPhoneStatus = json.get("cellPhoneStatus").asText();
    var cellPhoneComments = json.get("cellPhoneComments").asText();

    var cellTextStatus = json.get("cellTextStatus").asText();
    var cellTextComments = json.get("cellTextComments").asText();

    var radioStatus = json.get("radioStatus").asText();
    var radioComments = json.get("radioComments").asText();

    var tvStatus = json.get("tvStatus").asText();
    var tvComments = json.get("tvComments").asText();

    var satTvStatus = json.get("satTvStatus").asText();
    var satTvComments = json.get("satTvComments").asText();

    var cableTvStatus = json.get("cableTvStatus").asText();
    var cableTvComments = json.get("cableTvComments").asText();

    var waterStatus = json.get("waterStatus").asText();
    var waterComments = json.get("waterComments").asText();

    var powerStatus = json.get("powerStatus").asText();
    var powerComments = json.get("powerComments").asText();

    var powerStable = json.get("powerStableStatus").asText();
    var powerStableComments = json.get("powerStableComments").asText();

    var naturalGasStatus = json.get("naturalGasStatus").asText();
    var naturalGasComments = json.get("naturalGasComments").asText();

    var internetStatus = json.get("internetStatus").asText();
    var internetComments = json.get("internetComments").asText();

    var noaaStatus = json.get("noaaStatus").asText();
    var noaaComments = json.get("noaaComments").asText();

    var noaaAudioDegraded = json.get("noaaAudioDegraded").asText();
    var noaaAudioDegradedComments = json.get("noaaAudioDegradedComments").asText();

    var additionalComments = json.get("additionalComments").asText();
    var poc = json.get("poc").asText();
    var formVersion = json.get("formVersion").asText();

    var m = new FieldSituationMessage(//
        message, organization, formLocation, //
        precedence, formDateTime, task, formTo, formFrom, isHelpNeeded, neededHelp, //
        city, county, state, territory, //
        landlineStatus, landlineComments, voipStatus, voipComments, //
        cellPhoneStatus, cellPhoneComments, cellTextStatus, cellTextComments, //
        radioStatus, radioComments, //
        tvStatus, tvComments, satTvStatus, satTvComments, cableTvStatus, cableTvComments, //
        waterStatus, waterComments, //
        powerStatus, powerComments, powerStable, powerStableComments, //
        naturalGasStatus, naturalGasComments, //
        internetStatus, internetComments, //
        noaaStatus, noaaComments, noaaAudioDegraded, noaaAudioDegradedComments, //
        additionalComments, poc, formVersion);

    return m;
  }

  private ExportedMessage deserialize_Hics259Message(String jsonString)
      throws JsonMappingException, JsonProcessingException {
    var json = mapper.readTree(jsonString);
    var message = deserialize_ExportedMessage(json);

    var incidentName = json.get("incidentName").asText();
    var formDate = json.get("formDate").asText();
    var formTime = json.get("formTime").asText();
    ;
    var operationalPeriod = json.get("operationalPeriod").asText();
    var opFromDate = json.get("opFromDate").asText();
    var opFromTime = json.get("opFromTime").asText();
    var opToDate = json.get("opToDate").asText();
    var opToTime = json.get("opToTime").asText();
    var patientTrackingManager = json.get("patientTrackingManager").asText();
    var facilityName = json.get("facilityName").asText();
    var version = json.get("version").asText();

    var casualtyMap = new HashMap<String, CasualtyEntry>();
    var jsonCasualtyMap = json.get("casualtyMap");
    for (var key : Hics259Message.CASUALTY_KEYS) {
      var jsonEntry = jsonCasualtyMap.get(key);
      var adultCount = jsonEntry.get("adultCount").asText();
      var childCount = jsonEntry.get("childCount").asText();
      var comment = jsonEntry.get("comment").asText();
      var casualEntry = new CasualtyEntry(adultCount, childCount, comment);
      casualtyMap.put(key, casualEntry);
    }

    var m = new Hics259Message(message, //
        incidentName, formDate, formTime, //
        operationalPeriod, opFromDate, opFromTime, opToDate, opToTime, //
        casualtyMap, //
        patientTrackingManager, facilityName, version);

    return m;
  }
}
