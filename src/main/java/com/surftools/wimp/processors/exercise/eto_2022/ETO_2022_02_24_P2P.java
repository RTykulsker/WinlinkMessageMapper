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

package com.surftools.wimp.processors.exercise.eto_2022;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.processors.std.AbstractBaseP2PProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * processor for the P2P component of the 2022-02-24 exercise
 *
 * @author bobt
 *
 */
public class ETO_2022_02_24_P2P extends AbstractBaseP2PProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_02_24_P2P.class);

  private static class Target extends BaseTarget {
    public String channel;
    public String region;
    public String locationName;
    public int checkInCount;

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Band", "Center Freq. (KHz)", "Dial Freq (KHz)", "ETO Region", //
          "Station", "Location", "Latitude", "Longitude", "Check In Count", "Total Messages" };
    }

    @Override
    public String[] getValues() {
      return new String[] { band, centerFreq, dialFreq, region, call, //
          locationName, location.getLatitude(), location.getLongitude(), //
          String.valueOf(checkInCount), String.valueOf(fromList.size()) };
    }
  }

  private static class Field extends BaseField {
    public int checkInCount;

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", //
          "CheckIn Count", "Total Messages" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, location.getLatitude(), location.getLongitude(), //
          String.valueOf(checkInCount), String.valueOf(toList.size()) };
    }
  }

  private List<IWritableTable> entryList;

  private int ppCheckInMessageCount;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    p2p_initialize();

    requiredMessageTypeSet.add(MessageType.CHECK_IN);

    entryList = new ArrayList<>();
  }

  @Override
  public void process() {
    p2p_process();

    // update extended Field and Targets before generating KML
    // loop over targets, because there are fewer of them
    for (var baseTarget : targetMap.values()) {
      var target = (Target) baseTarget;
      var targetCall = target.call;
      var fromList = target.fromList;
      for (var message : fromList) {
        var fieldCall = message.from;
        var field = (Field) fieldMap.get(fieldCall);
        var messageType = message.getMessageType();
        if (messageType.equals(MessageType.CHECK_IN)) {
          ++ppCheckInMessageCount;
          ++field.checkInCount;
          ++target.checkInCount;

        } else {
          // this shouldn't happen, but it does, rejects, etc
          logger
              .debug("unexpected messageType for messageId: " + message.messageId + ", from: " + message.from + ",to: "
                  + message.to);
        }

        var hasAttachments = getFirstImageFile(message) != null;
        var entry = new P2PEntry(fieldCall, targetCall, message.messageId, //
            message.sortDateTime, messageType, hasAttachments);
        entryList.add(entry);

      } // end loop over messages in toList
    } // end loop over targets

    summarize();
  }

  /**
   * produce needed (and un-needed) summary output
   */
  public void summarize() {
    var kmlText = makeKmlText();
    writeKml(kmlText);

    WriteProcessor.writeTable(entryList, Path.of(outputPathName, "p2p-entries.csv"));
    writeUpdatedTargets();
    writeUpdatedFields();

    var sb = new StringBuilder();
    sb.append("\n\nSummary\n");
    sb.append("total Check In messages received: " + ppCheckInMessageCount + "\n");
    logger.info(sb.toString());
  }

  @Override
  protected BaseTarget makeTarget(String[] fields) {
    Target target = new Target();

    var index = 0;
    target.isActive = Boolean.parseBoolean(fields[index++]);
    target.channel = fields[index++];
    target.band = fields[index++];
    target.centerFreq = fields[index++];
    target.dialFreq = fields[index++];
    target.region = fields[index++];
    target.call = fields[index++].trim();
    target.locationName = fields[index++];
    var latitude = fields[index++];
    var longitude = fields[index++];
    target.location = new LatLongPair(latitude, longitude);
    target.fromList = new ArrayList<>();

    return target;
  }

  @Override
  protected BaseField makeField(String[] fields) {
    /*
     * return new String[] { "MessageId", "From", "To", "Subject", "Date", "Time", // "Latitude", "Longitude",
     * "Organization", // "Status", "Band", "Mode", "Comments", "Version" };
     */

    Field field = new Field();

    field.call = fields[1];
    var latitude = fields[6];
    var longitude = fields[7];
    field.location = new LatLongPair(latitude, longitude);
    field.toList = new ArrayList<>();

    return field;
  }

  @Override
  protected String makeFieldDescription(BaseField baseField, Map<String, BaseTarget> targetMap) {
    Field field = (Field) baseField;
    var sb = new StringBuilder();
    var toList = field.toList;

    sb.append("Outbound messages: " + toList.size() + "\n");
    sb.append("  Check In messages: " + field.checkInCount + "\n");
    sb.append(DASHES);
    for (var m : toList) {
      var time = m.sortDateTime.toString();
      var to = m.to;
      var target = (Target) targetMap.get(to);
      var band = target.band;
      var location = target.location;
      var distanceMiles = LocationUtils.computeDistanceMiles(field.location, location);
      sb.append(time + ", " + to + //
          " (" + band + "m, " + distanceMiles + " miles)" + "\n");
    }

    return sb.toString();
  }

  @Override
  protected String makeTargetDescription(BaseTarget baseTarget, Map<String, BaseField> fieldMap) {
    Target target = (Target) baseTarget;
    var fromList = target.fromList;
    var sb = new StringBuilder();
    sb.append(target.dialFreq + " KHz dial, " + target.locationName + "\n");
    sb.append("band: " + target.band + "m, channel: " + target.channel + ", region: " + target.region + "\n");
    sb.append(DASHES);
    sb.append("Inbound messages: " + fromList.size() + "\n");
    sb.append(DASHES);
    for (var m : fromList) {
      var time = m.sortDateTime.toString();
      var from = m.from;
      var location = m.mapLocation;
      var distanceMiles = location == null ? "unknown" : LocationUtils.computeDistanceMiles(target.location, location);
      sb.append(time + ", " + from + //
          " (" + distanceMiles + " miles)" + "\n");

    }
    return sb.toString();
  }

}
