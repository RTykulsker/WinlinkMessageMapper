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

package com.surftools.wimp.processors.named;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.AbstractBaseP2PProcessor;
import com.surftools.wimp.processors.std.WriteProcessor;

/**
 * processor for the P2P component of the 2023-11-11 semi-annual P2P drill
 *
 * @author bobt
 *
 */
public class ETO_2023_11_11_P2P extends AbstractBaseP2PProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2023_11_11_P2P.class);

  private static class Target extends BaseTarget {
    public String channel;
    public String region;
    public String locationName;
    public int ics213RrCount;

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Band", "Center Freq. (KHz)", "Dial Freq (KHz)", "ETO Region", //
          "Station", "Location", "Latitude", "Longitude", "Ics213RR Count" };
    }

    @Override
    public String[] getValues() {
      return new String[] { band, centerFreq, dialFreq, region, call, //
          locationName, location.getLatitude(), location.getLongitude(), String.valueOf(ics213RrCount) };
    }
  }

  private static class Field extends BaseField {
    public String to;
    public int ics213rrCount;

    @Override
    public String toString() {
      return "{call: " + call + "}";
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "To", "Latitude", "Longitude", //
          "Ics213RR Count" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, to, location.getLatitude(), location.getLongitude(), //
          String.valueOf(ics213rrCount), };
    }
  }

  private List<IWritableTable> entryList;

  private int ppIcs213RrMessageCount;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {

    super.initialize(cm, mm, logger);

    p2p_initialize();

    requiredMessageTypeSet.add(MessageType.ICS_213_RR);

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
      for (var m : fromList) {
        var message = (Ics213RRMessage) m;
        var fieldCall = message.from;
        var field = (Field) fieldMap.get(fieldCall);
        ++ppIcs213RrMessageCount;
        ++field.ics213rrCount;
        ++target.ics213RrCount;

        var entry = new P2PEntry(fieldCall, targetCall, message.messageId, message.sortDateTime,
            message.getMessageType(), false);
        entryList.add(entry);
      } // end loop over messages in toList
    } // end loop over targets

    summarize();
  }

  private void makeP2PFavorites() {
    var lines = new ArrayList<String>();
    for (var baseTarget : targetMap.values()) {
      var target = (Target) baseTarget;
      lines.add(target.call + "|" + target.centerFreq + "/500");
    }

    var favoritesPath = Path.of(cm.getAsString(Key.PATH), "output", "Vara P2P Favorites.dat");
    try {
      Files.writeString(favoritesPath, String.join("\n", lines));
      logger.info("wrote " + lines.size() + " favorites to: " + favoritesPath);
    } catch (IOException e) {
      logger.error("Exception writing " + favoritesPath + ", " + e.getMessage());
    }
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
    sb.append("total ICS 213 RR messages received: " + ppIcs213RrMessageCount + "\n");
    logger.info(sb.toString());

    makeP2PFavorites();
  }

  @Override
  protected BaseTarget makeTarget(String[] fields) {
    Target target = new Target();

    // Call Latitude Longitude Location First Name Last Name Email Team Channel FEMA Center Freq Band

    target.isActive = fields[7].equals("A");
    target.channel = fields[8];
    target.band = fields[11];
    target.centerFreq = fields[10];
    target.dialFreq = centerStringToDialString(fields[10]);
    target.region = fields[9];
    target.call = fields[0];
    target.locationName = fields[3];
    var latitude = fields[1];
    var longitude = fields[2];
    target.location = new LatLongPair(latitude, longitude);
    target.fromList = new ArrayList<>();

    return target;
  }

  private String centerStringToDialString(String centerString) {
    var centerFreq = Double.parseDouble(centerString);
    var dialFreq = centerFreq - 1.5;
    var dialString = String.format("%.1f", dialFreq);
    return dialString;
  }

  @Override
  protected BaseField makeField(String[] fields) {
    // use ics_213_rr.csv as the basis
    Field field = new Field();
    field.call = fields[1];
    field.to = fields[2];
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
    sb.append(DASHES);
    for (var m : toList) {
      var time = m.sortDateTime.toLocalTime();
      var to = m.to;
      var target = (Target) targetMap.get(to);
      var band = target.band;
      var location = target.location;
      var distanceMiles = LocationUtils.computeDistanceMiles(field.location, location);
      sb.append(time + ", " + to + //
          " (" + band + ", " + distanceMiles + " miles)\n");
    }

    return sb.toString();
  }

  @Override
  protected String makeTargetDescription(BaseTarget baseTarget, Map<String, BaseField> fieldMap) {
    Target target = (Target) baseTarget;
    var fromList = target.fromList;
    var sb = new StringBuilder();
    sb.append(target.dialFreq + " KHz dial, " + target.locationName + "\n");
    sb.append("band: " + target.band + ", channel: " + target.channel + ", region: " + target.region + "\n");
    sb.append(DASHES);
    sb.append("Inbound messages: " + fromList.size() + "\n");
    sb.append(DASHES);
    for (var m : fromList) {
      var time = m.sortDateTime.toLocalTime();
      var from = m.from;
      var location = m.mapLocation;
      var distanceMiles = location == null ? "unknown" : LocationUtils.computeDistanceMiles(target.location, location);
      sb.append(time + ", " + from + //
          " (" + distanceMiles + " miles)\n");

    }
    return sb.toString();
  }

}
