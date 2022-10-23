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

package com.surftools.winlinkMessageMapper.aggregation.p2p.named;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.aggregation.p2p.AbstractBaseP2PAggregator;
import com.surftools.winlinkMessageMapper.aggregation.p2p.BaseField;
import com.surftools.winlinkMessageMapper.aggregation.p2p.BaseTarget;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.Ics213Message;
import com.surftools.winlinkMessageMapper.dto.message.UnifiedFieldSituationMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * aggregator for the P2P component of the 2022-11-12 Dril
 *
 * @author bobt
 *
 */
public class ETO_2022_11_12_P2P_Aggregator extends AbstractBaseP2PAggregator {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_11_12_P2P_Aggregator.class);

  // one entry for every P2P message received, either FSR or ICS
  private static record Entry(//
      String from, //
      String to, //
      String messageId, //
      LocalDateTime dateTime, //
      String messageType, //
      boolean hasImageAttachment) {

    public static String[] getHeaders() {
      return new String[] { "From", "To", "MiD", "DateTime", "Type", "Attachments?" };
    }

    public String[] getValues() {
      return new String[] { from, to, messageId, dateTime.toString(), messageType, String.valueOf(hasImageAttachment) };
    }
  }

  private static class Target extends BaseTarget {
    public String channel;
    public String region;
    public String locationName;
    public int fsrCount;
    public int icsCount;
    public int imageCount;

    @Override
    public String[] getHeaders() {
      return new String[] { "Band", "Center Freq. (KHz)", "Dial Freq (KHz)", "ETO Region", //
          "Station", "Location", "Latitude", "Longitude", "Fsr Count", "ICS Count", "Image Count", "Total Messages" };
    }

    @Override
    public String[] getValues() {
      return new String[] { band, centerFreq, dialFreq, region, call, //
          locationName, location.getLatitude(), location.getLongitude(), //
          String.valueOf(fsrCount), String.valueOf(icsCount), String.valueOf(imageCount), //
          String.valueOf(fromList.size()) };
    }
  }

  private static class Field extends BaseField {
    public int fsrCount;
    public int icsCount;
    public int imageCount;

    @Override
    public String[] getHeaders() {
      return new String[] { "Call", "Latitude", "Longitude", //
          "Fsr Count", "ICS Count", "Image Count", "Total Messages" };
    }

    @Override
    public String[] getValues() {
      return new String[] { call, location.getLatitude(), location.getLongitude(), //
          String.valueOf(fsrCount), String.valueOf(icsCount), String.valueOf(imageCount), //
          String.valueOf(toList.size()) };
    }
  }

  private List<Entry> entryList;

  public ETO_2022_11_12_P2P_Aggregator() {
    super(logger);
  }

  @Override
  public void initialize() {
    super.initialize();

    requiredMessageTypeSet.add(MessageType.UNIFIED_FIELD_SITUATION);
    requiredMessageTypeSet.add(MessageType.ICS_213);

    entryList = new ArrayList<>();
  }

  @Override
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    initialize();

    super.aggregate(messageMap);

    // update extended Field and Targets before generating KML
    // loop over targets, because there are fewer of them
    for (var baseTarget : targetMap.values()) {
      var target = (Target) baseTarget;
      var targetCall = target.call;
      var fromList = target.fromList;
      for (var message : fromList) {
        var fieldCall = message.from;
        var field = (Field) fieldMap.get(fieldCall);
        var messageType = "unknown";
        var hasImageAttachment = false;
        if (message instanceof Ics213Message) {
          messageType = "ics";
          ++field.icsCount;
          ++target.icsCount;

          hasImageAttachment = hasImageAttachment(message);
          if (hasImageAttachment) {
            ++field.imageCount;
            ++target.imageCount;
          }
        } else if (message instanceof UnifiedFieldSituationMessage) {
          ++field.fsrCount;
          ++target.fsrCount;
          messageType = "fsr";
        } else {
          // this shouldn't happen!
          logger.warn("unexpected messageType for messageId: " + message.messageId);
        }

        var dateTime = makeDateTime(message.date, message.time);
        var entry = new Entry(fieldCall, targetCall, message.messageId, //
            dateTime, messageType, hasImageAttachment);
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

    writeEntries(cm, entryList);
    writeUpdatedTargets();
    writeUpdatedFields();
  }

  public void writeEntries(IConfigurationManager cm, List<Entry> entryList) {
    Path outputPath = Path.of(cm.getAsString(Key.PATH), "output", "p2p-entries.csv");

    var messageCount = 0;
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(Entry.getHeaders());

      if (entryList.size() > 0) {
        entryList.sort(new EntryComparator());
        for (Entry e : entryList) {
          if (e != null) {
            var values = e.getValues();
            if (values != null) {
              writer.writeNext(e.getValues());
              ++messageCount;
            }
          } else {
            continue;
          }
        }
      }

      writer.close();
      logger.info("wrote " + messageCount + " P2P entry messages to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath.toString() + ", " + e.getLocalizedMessage());
    }

  }

  static class EntryComparator implements Comparator<Entry> {

    @Override
    public int compare(Entry o1, Entry o2) {
      var cmp = o1.dateTime.compareTo(o2.dateTime);
      if (cmp != 0) {
        return cmp;
      }

      cmp = o1.from.compareTo(o2.from);
      if (cmp != 0) {
        return cmp;
      }

      cmp = o1.to.compareTo(o2.to);
      if (cmp != 0) {
        return cmp;
      }

      cmp = o1.messageType.compareTo(o2.messageType);
      if (cmp != 0) {
        return cmp;
      }

      return 0;
    }

  }

  private boolean hasImageAttachment(ExportedMessage m) {
    for (String key : m.attachments.keySet()) {
      try {
        var bytes = m.attachments.get(key);
        var bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
          continue;
        }
        logger.debug("image found for call: " + m.from + ", attachment: " + key + ", size:" + bytes.length);
        return true;
      } catch (Exception e) {
        ;
      }
    }
    return false;
  }

  @Override
  protected BaseTarget makeTarget(String[] fields) {
    Target target = new Target();

    var index = 0;
    target.channel = fields[index++];
    target.band = fields[index++];
    target.centerFreq = fields[index++];
    target.dialFreq = fields[index++];
    target.region = fields[index++];
    target.call = fields[index++];
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
     * "Call", "Latitude", "Longitude", // "FsrTo", "FsrMiD", "FsrComment", // "IcsTo", "IcsMiD", "IcsMessage",
     * "IcsImageBytes", // // "Grade", "Explanation"
     */

    var fsrMid = fields[4];
    var icsMid = fields[7];

    /**
     * we REQUIRE both ICS and FSR messages to qualify Field for P2P exercise
     */
    if (fsrMid == null || fsrMid.trim().isEmpty() || icsMid == null || icsMid.trim().isEmpty()) {
      return null;
    }

    Field field = new Field();

    var index = 0;
    field.call = fields[index++];
    var latitude = fields[index++];
    var longitude = fields[index++];
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
    sb.append("  FSR messages: " + field.fsrCount + "(with " + field.imageCount + " images)\n");
    sb.append("  ICS messages: " + field.icsCount + "\n");
    sb.append(DASHES);
    for (var m : toList) {
      var date = m.date;
      var time = m.time;
      var to = m.to;
      var target = (Target) targetMap.get(to);
      var band = target.band;
      var location = target.location;
      var type = m.getMessageType();
      var typeString = (type == MessageType.ICS_213) ? "ics" : "fsr";
      var hasImage = (type == MessageType.ICS_213 && hasImageAttachment(m));
      var imageString = (hasImage) ? ", with image" : "";
      var distanceMiles = LocationUtils.computeDistanceMiles(field.location, location);
      sb.append(date + " " + time + ", " + to + //
          " (" + band + ", " + distanceMiles + " miles)" + ", [" + typeString + imageString + "]\n");
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
    sb.append("  FSR messages: " + target.fsrCount + "\n");
    sb.append("  ICS messages: " + target.icsCount + "(with " + target.imageCount + " images)\n");
    sb.append(DASHES);
    for (var m : fromList) {
      var date = m.date;
      var time = m.time;
      var from = m.from;
      var location = m.location;
      var type = m.getMessageType();
      var typeString = (type == MessageType.ICS_213) ? "ics" : "fsr";
      var hasImage = (type == MessageType.ICS_213 && hasImageAttachment(m));
      var imageString = (hasImage) ? ", with image" : "";
      var distanceMiles = LocationUtils.computeDistanceMiles(target.location, location);
      sb.append(date + " " + time + ", " + from + //
          " (" + distanceMiles + " miles)" + ", [" + typeString + imageString + "]\n");
    }
    return sb.toString();
  }

  @Override
  public String[] getHeaders() {
    return null;
  }

  @Override
  public String[] getValues(AggregateMessage message) {
    return null;
  }

}
