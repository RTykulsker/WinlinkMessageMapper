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

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.formField.FFType;
import com.surftools.wimp.formField.FormField;
import com.surftools.wimp.formField.FormFieldManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.processors.std.WriteProcessor;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2022-12-22 Exercise: one ICS-213-RR with image
 *
 * @author bobt
 *
 */
public class ETO_2022_12_22 extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_12_22.class);
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  public static final String DATE_TIME_NOT = "2022-12-31 06:00";

  public static final boolean ENABLE_NON_ACTIONABLE_FIELDS = false;

  public static String outputPathName;

  public static final String[] gradeBands = new String[] { //
      "0-9%", "10-19%", "20-29%", "30-39%", "40-49%", "50-59%", "60-69%", "70-79%", "80-89%", "90-99%", "100%" };

  static record Result(Ics213RRMessage message, LatLongPair location, String grade, String gradeBand,
      String explanation) implements IWritableTable {

    @Override
    public String[] getHeaders() {
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 4);
      Collections.addAll(resultList, Ics213RRMessage.getStaticHeaders());
      Collections
          .addAll(resultList, new String[] { "FormLatitude", "FormLongitude", "Grade", "GradeBand", "Explanation" });
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public String[] getValues() {
      var loc = location != null ? location : LatLongPair.ZERO_ZERO;
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 4);
      Collections.addAll(resultList, message.getValues());
      Collections
          .addAll(resultList, new String[] { loc.getLatitude(), loc.getLongitude(), grade, gradeBand, explanation });
      return resultList.toArray(new String[resultList.size()]);
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      return this.message.compareTo(o.message);
    }
  };

  private FormFieldManager sfMgr = new FormFieldManager();
  private FormFieldManager nsfMgr = new FormFieldManager();

  private Path imageAllPath;
  private List<Path> imagePassPaths;
  private List<Path> imageBadPaths;
  private int maxImageSize = -1;

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var imageOutputPath = Path.of(outputPathName.toString(), "image");
    FileUtils.deleteDirectory(imageOutputPath);

    imageAllPath = FileUtils.createDirectory(Path.of(imageOutputPath.toString(), "all"));
    imagePassPaths = List.of(FileUtils.createDirectory(Path.of(imageOutputPath.toString(), "pass")));
    imageBadPaths = List.of(FileUtils.createDirectory(Path.of(imageOutputPath.toString(), "badSize")));

    maxImageSize = cm.getAsInt(Key.IMAGE_MAX_SIZE, 5500);

    // header
    nsfMgr
        .add("organization",
            new FormField(FFType.SPECIFIED, "Agency/Group name", "ETO ICS-213RR Santa Purchase Request", 0));

    sfMgr.add("activityDateTime", new FormField(FFType.DATE_TIME_NOT, "Box 2 Date/Time", DATE_TIME_NOT, 1));

    nsfMgr.add("requestNumber", new FormField(FFType.SPECIFIED, "Resource Request Number", "12-22-22-ETO-SR1", 0));

    // line 1
    sfMgr.add("quantity1", new FormField(FFType.REQUIRED_NOT, "Item Quantity#1", "REQ", 1));

    nsfMgr.add("type1", new FormField(FFType.EMPTY, "Item Type#1", null, 0));

    nsfMgr.add("kind1", new FormField(FFType.EMPTY, "Item Kind#1", null, 0));

    sfMgr.add("item1", new FormField(FFType.REQUIRED_NOT, "Item Description#1", "REQUIRED", 1));

    sfMgr.add("requestedDateTime1", new FormField(FFType.DATE_TIME_NOT, "Requested Date/Time#1", DATE_TIME_NOT, 1));

    nsfMgr.add("estimatedDateTime1", new FormField(FFType.EMPTY, "Estimated Date/Time#1", null, 0));

    sfMgr.add("cost1", new FormField(FFType.REQUIRED_NOT, "Item Cost#1", "REQUIRED", 1));

    // line 2-8
    for (int i = 2; i <= 8; ++i) {
      nsfMgr.add("quantity" + i, new FormField(FFType.EMPTY, "Item Quantity#" + i, null, 0));
      nsfMgr.add("type" + i, new FormField(FFType.EMPTY, "Item Type#" + i, null, 0));
      nsfMgr.add("kind" + i, new FormField(FFType.EMPTY, "Item Kind#" + i, null, 0));
      nsfMgr.add("item" + i, new FormField(FFType.EMPTY, "Item Description#" + i, null, 0));
      nsfMgr.add("requestedDateTime" + i, new FormField(FFType.EMPTY, "Requested Date/Time#" + i, null, 0));
      nsfMgr.add("estimatedDateTime" + i, new FormField(FFType.EMPTY, "Estimated Date/Time#" + i, null, 0));
      nsfMgr.add("cost" + i, new FormField(FFType.EMPTY, "Item Cost#" + i, null, 0));
    }

    // rest of request
    sfMgr
        .add("delivery", new FormField(FFType.REQUIRED_NOT, "Delivery/Reporting Location",
            "YOUR GPS COORDINATES - in decimal degrees - REQUIRED", 1));

    sfMgr.add("substitutes", new FormField(FFType.OPTIONAL_NOT, "Substitutes", "OPTIONAL - MAKE BLANK IF NO ENTRY", 1));

    sfMgr
        .add("requestedBy", new FormField(FFType.REQUIRED_NOT, "Requested By",
            "Your Name & Call Sign - then select PRIORITY----->", 1));

    sfMgr.add("priority", new FormField(FFType.REQUIRED, "Priority", null, 1));

    nsfMgr.add("approvedBy", new FormField(FFType.SPECIFIED, "Section Chief", "David Rudolph Rednose", 0));

    // logistics
    sfMgr.add("logisticsOrderNumber", new FormField(FFType.REQUIRED_NOT, "Log Order Number", "MAKE ONE UP", 1));

    sfMgr
        .add("supplierInfo", new FormField(FFType.REQUIRED_NOT, "Supplier Info",
            "MAKE UP or USE A REAL PHONE NUMBER FOR YOUR FAVORITE AMATEUR RADIO SUPPLIER", 1));

    sfMgr
        .add("supplierName",
            new FormField(FFType.REQUIRED_NOT, "Supplier Name", "NAME OF FAVORITE AMATEUR RADIO SUPPLIER", 1));

    sfMgr.add("supplierPointOfContact", new FormField(FFType.REQUIRED_NOT, "Point of Contact", "MAKE ONE UP", 1));

    sfMgr
        .add("supplyNotes",
            new FormField(FFType.OPTIONAL_NOT, "Notes", "OPTIONAL - ADD ACCESSORIES OR MAKE BLANK IF NO ENTRY", 1));

    sfMgr
        .add("logisticsAuthorizer",
            new FormField(FFType.REQUIRED_NOT, "Logistics Rep", "MAKE ONE UP              UPDATE DATE/TIME -->", 1));

    sfMgr.add("logisticsDateTime", new FormField(FFType.DATE_TIME_NOT, "Logistics Date/Time", DATE_TIME_NOT, 1));

    sfMgr.add("orderedBy", new FormField(FFType.REQUIRED_NOT, "Ordered By", "YOUR NAME AND CALL SIGN", 1));

    // finance
    nsfMgr.add("financeComments", new FormField(FFType.SPECIFIED, "Finance Comments", "HAPPY HOLIDAYS !!", 0));

    sfMgr.add("financeName", new FormField(FFType.REQUIRED_NOT, "Finance Chief", "MAKE ONE UP OR SPOUSE", 1));

    sfMgr.add("financeDateTime", new FormField(FFType.DATE_TIME_NOT, "Finance Date/Time By", DATE_TIME_NOT, 1));

    nsfMgr.setIsEnabled(ENABLE_NON_ACTIONABLE_FIELDS);
  }

  @Override
  public void process() {

    var ppAllParticipantCount = 0;
    var ppCount = 0;
    var ppImageAttachedOk = 0;
    var ppImageSizeOk = 0;
    var ppLatLongOk = 0;

    var ppScoreCounter = new Counter();
    var ppGradeBandCounter = new Counter();
    var ppItemCounter = new Counter();
    var ppCostCounter = new Counter();
    var ppPriorityCounter = new Counter();

    var results = new ArrayList<IWritableTable>();
    for (var m : mm.getMessagesForType(MessageType.ICS_213_RR)) {
      var message = (Ics213RRMessage) m;
      var sender = message.from;

      var points = 0;
      var explanations = new ArrayList<String>();

      ++ppAllParticipantCount;
      ++ppCount;

      var imageFileName = getImageFile(message);
      if (imageFileName != null) {
        var bytes = message.attachments.get(imageFileName);
        if (bytes != null) {
          ++ppImageAttachedOk;
          points += 25;

          List<Path> linkPaths = null;
          if (bytes.length <= maxImageSize) {
            ++ppImageSizeOk;
            points += 25;
            linkPaths = imagePassPaths;
          } else {
            explanations.add("image size (" + bytes.length + ") > max(" + maxImageSize + ")");
            linkPaths = imageBadPaths;
          }

          var newImageFileName = sender;
          var lastIndex = imageFileName.toLowerCase().lastIndexOf(".");
          if (lastIndex >= 0) {
            newImageFileName = sender + imageFileName.toLowerCase().substring(lastIndex);
          }

          writeContent(bytes, newImageFileName, imageAllPath, linkPaths);
        }
      } else {
        explanations.add("no image attachment found");
      }

      var resultArray = getLocation(message);
      LatLongPair location = (LatLongPair) resultArray[0];
      String explanation = (String) resultArray[1];
      if (explanation != null) {
        explanations.add(explanation);
      } else {
        points += 25;
        ++ppLatLongOk;
      }

      // scorable and non-scorable fields
      sfMgr.reset(explanations);
      nsfMgr.reset(explanations);

      nsfMgr.test("organization", message.organization);
      sfMgr.test("activityDateTime", message.activityDateTime);
      nsfMgr.test("requestNumber", message.requestNumber);

      var lineItem = message.lineItems.get(0);
      sfMgr.test("quantity1", lineItem.quantity());
      nsfMgr.test("kind1", lineItem.kind());
      nsfMgr.test("type1", lineItem.type());
      sfMgr.test("item1", lineItem.item());
      sfMgr.test("requestedDateTime1", lineItem.requestedDateTime());
      nsfMgr.test("estimatedDateTime1", lineItem.estimatedDateTime());
      sfMgr.test("cost1", lineItem.cost());

      ppItemCounter.incrementNullSafe(lineItem.item());
      ppCostCounter.incrementNullSafe(lineItem.cost());
      ppPriorityCounter.increment(message.priority);

      for (int i = 2; i <= 8; ++i) {
        lineItem = message.lineItems.get(i - 1);
        try {
          nsfMgr.test("quantity" + String.valueOf(i), lineItem.quantity());
          nsfMgr.test("kind" + String.valueOf(i), lineItem.kind());
          nsfMgr.test("type" + String.valueOf(i), lineItem.type());
          nsfMgr.test("item" + String.valueOf(i), lineItem.item());
          nsfMgr.test("requestedDateTime" + String.valueOf(i), lineItem.requestedDateTime());
          nsfMgr.test("estimatedDateTime" + String.valueOf(i), lineItem.estimatedDateTime());
          nsfMgr.test("cost" + String.valueOf(i), lineItem.cost());
        } catch (Exception e) {
          logger.error("expection processing for call: " + sender + ", line: " + i + ", " + e.getLocalizedMessage());
        }
      }

      sfMgr.test("delivery", message.delivery);
      sfMgr.test("substitutes", message.substitutes);
      sfMgr.test("requestedBy", message.requestedBy);
      sfMgr.test("priority", message.priority);
      nsfMgr.test("approvedBy", message.approvedBy);

      // logistics
      sfMgr.test("logisticsOrderNumber", message.logisticsOrderNumber);
      sfMgr.test("supplierInfo", message.supplierInfo);
      sfMgr.test("supplierName", message.supplierName);
      sfMgr.test("supplierPointOfContact", message.supplierPointOfContact);
      sfMgr.test("supplyNotes", message.supplyNotes);
      sfMgr.test("logisticsAuthorizer", message.logisticsAuthorizer);
      sfMgr.test("logisticsDateTime", message.logisticsDateTime);
      sfMgr.test("orderedBy", message.orderedBy);

      // finance
      nsfMgr.test("financeComments", message.financeComments);
      sfMgr.test("financeName", message.financeName);
      sfMgr.test("financeDateTime", message.financeDateTime);

      points += 25 * (sfMgr.getPoints() / sfMgr.size());
      points += 0 * (nsfMgr.getPoints() / nsfMgr.size());

      points = Math.min(100, points);
      points = Math.max(0, points);

      var grade = String.valueOf(points);
      explanation = (points == 100 && explanations.size() == 0) //
          ? "Perfect Score!"
          : String.join("\n", explanations);

      ppScoreCounter.increment(points);

      var gradeIndex = points / 10;
      var gradeBand = gradeBands[gradeIndex];
      ppGradeBandCounter.increment(gradeBand);

      var result = new Result(message, location, grade, gradeBand, explanation);
      results.add(result);
    } // end loop over for

    var sb = new StringBuilder();
    sb.append("\n\nETO 2022-12-22 aggregate results:\n");
    sb.append("all participants: " + ppAllParticipantCount + "\n");
    sb.append("ICS-213-RR participants: " + ppCount + "\n");

    sb.append(formatPP("  Image file present", ppImageAttachedOk, ppCount));
    sb.append(formatPP("  Image size present", ppImageSizeOk, ppCount));
    sb.append(formatPP("  Delivery location", ppLatLongOk, ppCount));

    sb.append("\nScorable Actionable Fields\n");
    for (var key : sfMgr.keySet()) {
      var af = sfMgr.get(key);
      sb.append("  " + formatPP(af.label, af.count, ppCount));
    }

    if (ENABLE_NON_ACTIONABLE_FIELDS) {
      sb.append("\nNon-Scorable Actionable Fields\n");
      for (var key : nsfMgr.keySet()) {
        var af = nsfMgr.get(key);
        sb.append(" " + formatPP(af.label, af.count, ppCount));
      }
    }

    sb.append("\nScores: \n" + formatCounter(ppScoreCounter.getDescendingKeyIterator(), "score", "count"));

    sb.append("\nScores: \n" + formatCounter(ppGradeBandCounter.getDescendingKeyIterator(), "band", "count"));

    var topItemCount = 20;
    sb
        .append("\nTop " + topItemCount + " requested Items: \n"
            + formatCounter(ppItemCounter.getDescendingCountIterator(), "item", "count", topItemCount));

    var topCostCount = 20;
    sb
        .append("\nTop " + topCostCount + " requested Costs: \n"
            + formatCounter(ppCostCounter.getDescendingCountIterator(), "cost", "count", topCostCount));

    sb.append("\nPriorities: \n" + formatCounter(ppPriorityCounter.getDescendingKeyIterator(), "priority", "count"));

    logger.info(sb.toString());

    WriteProcessor.writeTable(results, Path.of(outputPathName, "aggregated-ics-213-rr.csv"));
    WriteProcessor.writeCounter(ppItemCounter, Path.of(outputPathName, "counted-items.csv"));
    WriteProcessor.writeCounter(ppCostCounter, Path.of(outputPathName, "counted-costs.csv"));
    WriteProcessor.writeCounter(ppPriorityCounter, Path.of(outputPathName, "counted-priorities.csv"));
  }

  /**
   * try to get location from the delivery string.
   *
   * Alas, the instructions were ambiguous about the delimiter between latitude and longitude
   *
   * If we can't get from the delivery string, return an error explanation, but try to get from the ExportedMessage for
   * the purposes of plotting.
   *
   * @param message
   * @return an array of [LatLongPair, String(explanation if any)
   */
  private Object[] getLocation(Ics213RRMessage message) {
    LatLongPair location = null;
    String explanation = null;

    // var debug = false;
    // var dumpList = new String[] { "NK8B" };
    // var dumpSet = new HashSet<String>();
    // dumpSet.addAll(Arrays.asList(dumpList));
    // if (dumpSet.contains(message.from)) {
    // debug = true;
    // }

    var locationString = (message.delivery == null) ? null : new String(message.delivery);
    if (locationString != null && !locationString.isBlank()) {
      var stopWords = new String[] { // longest strings first!
          "Latitude : ", "Latitude:", "LATITUDE.", "Latitude", "latitude", "LAT:", "Lat:", "lat:", "LAT", "lat", "Lat", //
          "Longitude : ", "Longitude:", "LAT:", "LAT", "LONGITUDE", "Longitude", "longitude", "long.", "long", "Long",
          "LON:", "Lon:", "lon:", "LON", "lon", "Lon", //
          "N", "S", "E", "W", //
          "Â", "°", "'" };

      for (var stopWord : stopWords) {
        locationString = locationString.replaceAll(stopWord, "");
      }

      // trim multiple embedded spaces
      locationString = locationString.replaceAll("\\s{2,}", " ").trim();

      // trim trailing period
      if (locationString.endsWith(".")) {
        locationString = locationString.substring(0, locationString.length() - 1);
      }

      var delimiters = new String[] { ",", " ", ";", "/", "x", "|" };
      for (var delimiter : delimiters) {
        var llFields = locationString.split(delimiter);
        if (llFields.length == 2) {
          location = new LatLongPair(llFields[0].trim(), llFields[1].trim());
          if (location != null && location.isValid()) {

            // 42-50.10N 078-45.40E and his message location is stuffed up too
            if (message.from.equals("KD2MIC")) {
              return new Object[] { location, explanation };
            }

            // last-ditch effort to prevent bad parsing of DD/MM/SS type data
            var lat = location.getLatitudeAsDouble();
            if (lat > 0 && lat <= 4) {
              location = message.mapLocation;
              explanation = "can not parse Delivery/Reporting Location: " + message.delivery;
              return new Object[] { location, explanation };
            }

            // last ditch effort to correct for losing the sign of longitude
            var lon = location.getLongitudeAsDouble();
            if (lon > 0 && message.delivery.endsWith("W")) {
              return new Object[] { new LatLongPair(location.getLatitude(), "-" + location.getLongitude()), null };
            }

            var messageLocation = message.mapLocation;
            if (messageLocation != null && messageLocation.isValid()) {
              var distanceMiles = LocationUtils.computeDistanceMiles(location, messageLocation);
              if (distanceMiles >= 10
                  && (location.getLongitudeAsDouble() > 0 && messageLocation.getLongitudeAsDouble() < 0)) {
                return new Object[] { messageLocation, null };
              }
            }

            return new Object[] { location, null };
          } // end if valid location from locationString
        } // end if at least 2 fields from delimited-splitting
      } // end loop over delimiters
      explanation = "can not parse Delivery/Reporting Location: " + message.delivery;
    } else {
      explanation = "no Delivery/Reporting Location";
    } // end if locationString present

    // try to get a location from the message itself
    location = message.mapLocation;
    if (location == null) {
      location = LatLongPair.ZERO_ZERO;
    }
    return new Object[] { location, explanation };
  }

  private String getImageFile(ExportedMessage m) {
    for (String key : m.attachments.keySet()) {
      try {
        var bytes = m.attachments.get(key);
        var bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
        if (bufferedImage == null) {
          continue;
        }
        logger.debug("image found for call: " + m.from + ", attachment: " + key + ", size:" + bytes.length);
        return key;
      } catch (Exception e) {
        ;
      }
    }
    return null;
  }
}
