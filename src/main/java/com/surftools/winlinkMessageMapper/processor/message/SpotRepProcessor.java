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

package com.surftools.winlinkMessageMapper.processor.message;

import java.util.Arrays;
import java.util.LinkedHashSet;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.SpotRepMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.dto.other.RejectType;

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
  public ExportedMessage process(ExportedMessage message) {

    try {
      String xmlString = new String(message.attachments.get(MessageType.SPOTREP.attachmentName()));
      makeDocument(message.messageId, xmlString);

      var latLong = getLatLongFromXml(null);
      if (latLong == null) {
        return reject(message, RejectType.CANT_PARSE_LATLONG, MERGED_LAT_LON_TAG_NAMES);
      }

      String location = getStringFromXml("city");

      String landlineStatus = getStringFromXml("land");
      String landlineComments = getStringFromXml("comm1");

      String cellPhoneComments = getStringFromXml("cell");
      String cellPhoneStatus = getStringFromXml("comm2");

      String radioStatus = getStringFromXml("amfm");
      String radioComments = getStringFromXml("comm3");

      String tvStatus = getStringFromXml("tvstatus");
      String tvComments = getStringFromXml("comm4");

      String waterStatus = getStringFromXml("waterworks");
      String waterComments = getStringFromXml("comm5");

      String powerStatus = getStringFromXml("powerworks");
      String powerComments = getStringFromXml("comm6");

      String internetStatus = getStringFromXml("inter");
      String internetComments = getStringFromXml("comm7");

      String additionalComments = getStringFromXml("message");
      String poc = getStringFromXml("poc");

      SpotRepMessage m = new SpotRepMessage(message, latLong.getLatitude(), latLong.getLongitude(), //
          location, landlineStatus, landlineComments, cellPhoneStatus, cellPhoneComments, radioStatus, radioComments,
          tvStatus, tvComments, waterStatus, waterComments, powerStatus, powerComments, internetStatus,
          internetComments, additionalComments, poc);

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

}
