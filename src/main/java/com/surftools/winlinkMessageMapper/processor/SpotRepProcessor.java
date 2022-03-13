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

package com.surftools.winlinkMessageMapper.processor;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.MessageType;
import com.surftools.winlinkMessageMapper.dto.SpotRepMessage;
import com.surftools.winlinkMessageMapper.reject.MessageOrRejectionResult;
import com.surftools.winlinkMessageMapper.reject.RejectType;

public class SpotRepProcessor extends AbstractBaseProcessor {
  private static final String[] OVERRIDE_LAT_LON_TAG_NAMES = new String[] {};
  private static final String MERGED_LAT_LON_TAG_NAMES;

  static {
    var set = new LinkedHashSet<String>();
    set.addAll(Arrays.asList(DEFAULT_LATLON_TAGS));
    set.addAll(Arrays.asList(OVERRIDE_LAT_LON_TAG_NAMES));
    MERGED_LAT_LON_TAG_NAMES = "couldn't find lat/long within tags: " + set.toString();
  }

  @Override
  public MessageOrRejectionResult process(ExportedMessage message) {

    try {
      String xmlString = new String(message.attachments.get(MessageType.SPOTREP.attachmentName()));

      var latLong = getLatLongFromXml(xmlString, null);
      if (latLong == null) {
        return new MessageOrRejectionResult(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String location = getStringFromXml(xmlString, "city");

      String landlineStatus = getStringFromXml(xmlString, "land");
      String landlineComments = getStringFromXml(xmlString, "comm1");

      String cellPhoneComments = getStringFromXml(xmlString, "cell");
      String cellPhoneStatus = getStringFromXml(xmlString, "comm2");

      String radioStatus = getStringFromXml(xmlString, "amfm");
      String radioComments = getStringFromXml(xmlString, "comm3");

      String tvStatus = getStringFromXml(xmlString, "tvstatus");
      String tvComments = getStringFromXml(xmlString, "comm4");

      String waterStatus = getStringFromXml(xmlString, "waterworks");
      String waterComments = getStringFromXml(xmlString, "comm5");

      String powerStatus = getStringFromXml(xmlString, "powerworks");
      String powerComments = getStringFromXml(xmlString, "comm6");

      String internetComments = getStringFromXml(xmlString, "inter");
      String internetStatus = getStringFromXml(xmlString, "comm7");

      String additionalComments = getStringFromXml(xmlString, "message");
      String poc = getStringFromXml(xmlString, "poc");

      SpotRepMessage m = new SpotRepMessage(message, latLong.latitude(), latLong.longitude(), //
          location, landlineStatus, landlineComments, cellPhoneStatus, cellPhoneComments, radioStatus, radioComments,
          tvStatus, tvComments, waterStatus, waterComments, powerStatus, powerComments, internetStatus,
          internetComments, additionalComments, poc);

      return new MessageOrRejectionResult(m, null);
    } catch (Exception e) {
      return new MessageOrRejectionResult(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
