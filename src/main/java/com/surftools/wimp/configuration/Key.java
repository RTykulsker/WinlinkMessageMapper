/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.wimp.configuration;

import com.surftools.wimp.utils.config.IConfigurationKey;

/**
 * the @{IConfigurationKey} for the WinlinkMessageMapper
 *
 * @author bobt
 *
 */
public enum Key implements IConfigurationKey {

  PATH("path"), // path to message files
  DATABASE_PATH("databasePath"), // path to input database summary files
  DUMP_IDS("dumpIds"), // comma-delimited list of messageIds or call signs to dump message contents for

  DEDUPLICATION_RULES("deduplication.rules"), // json string: {messageTypeName:rule,...}

  IMAGE_MAX_SIZE("image.maxSize"), // of attached images

  MULIIPLE_CHOICE_MESSAGE_TYPE("multipleChoice.messageType"), // type of message for MultipleChoice grading
  MULTIPLE_CHOICE_VALID_RESPONSES("multipleChoice.validResponses"), // comma-delimited list;
  MULTIPLE_CHOICE_CORRECT_RESPONSES("multipleChoice.correctResponses"), // comma-delimitedList
  MULTIPLE_CHOICE_ALLOW_STOP_CHARS("multipleChoice.allowStopChars"), // boolean
  MULTIPLE_CHOICE_FORCE_CASE("multipleChoice.forceCase"), // boolean, all comparisons in upper-case
  MULITPLE_CHOICE_ALLOW_QUOTES("multipleChoice.allowQuotes"), // boolean, allow quotes around responses
  MULTIPLE_CHOICE_ALLOW_TRIM("multipleChoice.allowTrim"), // boolean allow trim around responses

  OVERRIDE_LOCATION_PATH("overrideLocation.path"), // path to file with override locations

  P2P_TARGET_PATH("p2p.targetPath"), // for P2P processing, target (destination) definitions
  P2P_FIELD_PATH("p2p.fieldPath"), // for P2P processing, field (source) definitions
  P2P_DISTANCE_THRESHOLD_METERS("p2p.distanceThreshold.meters"), // for jittering
  P2P_BEGIN_PATH("p2p.beginPath"), // for Pickup before
  P2P_END_PATH("p2p.endPath"), // for Pickup after
  P2P_KML_TEMPLATE_PATH("p2p.kmlTemplatePath"), // for Pickup after

  PIPELINE_STDIN("pipeline.stdin"), // list of input processors
  PIPELINE_STDOUT("pipeline.stdout"), // list of output processors
  PIPELINE_MAIN("pipeline.main"), // list of main processors

  EXERCISE_DATE("exerciseDate"), // for Summarizer
  EXERCISE_NAME("exerciseName"), // for Summarizer
  EXERCISE_DESCRIPTION("exerciseDescription"), // for Summarizer
  EXERCISE_WINDOW_OPEN("exerciseWindowOpen"), //
  EXERCISE_WINDOW_CLOSE("exerciseWindowClose"), //

  FEEDBACK_PATH("feedbackPath"), // for feedback file

  EXPECTED_DESTINATIONS("expectedDestinations"), // comma-delimited list, like ETO-01,ETO-02
  SECONDARY_DESTINATIONS("secondaryDestinations"), // comma-delimited list, list ETO-BK

  MAX_DAYS_BEFORE_LATE("maxDaysBeforeLate"), // for warning about late messages

  RMS_KML_MESSAGE_TYPES("rms.kml.messageTypes"), // for supported MessageTypes in the RmsKmlProcessor
  RMS_KML_SHOW_MESSAGE_TYPES("rms.kml.show.messageTypes"), // show message types in descriptions
  RMS_KML_SHOW_DATES("rms.kml.show.dates"), // for dates as well as times in descriptions

  RMS_HF_GATEWAYS_FILE_NAME("rms.hf_gateways.fileName"), // for getting RMS location
  RMS_VHF_GATEWAYS_FILE_NAME("rms.vhf_gateways.fileName"), // for getting RMS location

  OUTBOUND_MESSAGE_ENGINE_TYPE("outboundMessage.engineType"), // PAT, WINLINK_CMS, etc
  OUTBOUND_MESSAGE_SOURCE("outboundMessage.source"), // mbo address
  OUTBOUND_MESSAGE_SENDER("outboundMessage.sender"), // from address
  OUTBOUND_MESSAGE_SUBJECT("outboundMessage.subject"), // message subject
  OUTBOUND_MESSAGE_PAT_EXEC_PATH("outboundMessage.pat.execPath"), // where to find PAT executable

  CMS_AUTHORIZATION_KEY("cms.authorizationKey"), // for CMS access
  CMS_MOCK_SENDER_PATH("cms.mock.sender.path"), // for keeping mock data out of git
  CMS_MOCK_SOURCE_PATH("cms.mock.source.path"), // for keeping mock data out of git
  CMS_MOCK_CHANNEL_PATH("cms.mock.channel.path"), // for keeping mock data out of git
  CMS_MOCK_GATEWAY_PATH("cms.mock.gateway.path"), // for keeping mock data out of git
  ;

  private final String key;

  private Key(String key) {
    this.key = key;
  }

  public static Key fromString(String string) {
    for (Key key : Key.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return key;
  }
}