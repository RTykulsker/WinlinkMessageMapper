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

package com.surftools.winlinkMessageMapper.aggregation.named;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;
import com.surftools.utils.FileUtils;
import com.surftools.utils.counter.Counter;
import com.surftools.utils.location.LatLongPair;
import com.surftools.winlinkMessageMapper.aggregation.AbstractBaseAggregator;
import com.surftools.winlinkMessageMapper.aggregation.AggregateMessage;
import com.surftools.winlinkMessageMapper.configuration.Key;
import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.message.Ics213RRMessage;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;

/**
 * Aggregator for 2022-12-22 Exercise: one ICS-213-RR with image
 *
 * @author bobt
 *
 */
public class ETO_2022_12_22_Aggregator extends AbstractBaseAggregator {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2022_12_22_Aggregator.class);
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  public static final String DATE_TIME_NOT = "2022-12-31 06:00";

  public static final int SCORABLE_FIELD_POINTS = 1;
  public static final int NON_SCORABLE_FIELD_POINTS = 0;
  public static final int FP1 = SCORABLE_FIELD_POINTS;
  public static final int FP0 = NON_SCORABLE_FIELD_POINTS;
  public static final boolean ENABLE_NON_ACTIONABLE_FIELDS = false;

  static record Result(Ics213RRMessage message, LatLongPair location, String grade, String explanation) {

    public static String[] getHeaders() {
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 4);
      Collections.addAll(resultList, Ics213RRMessage.getStaticHeaders());
      Collections.addAll(resultList, new String[] { "FormLatitude", "FormLongitude", "Grade", "Explanation" });
      return resultList.toArray(new String[resultList.size()]);
    }

    public String[] getValues() {
      var loc = location != null ? location : LatLongPair.ZERO_ZERO;
      var resultList = new ArrayList<String>(Ics213RRMessage.getStaticHeaders().length + 4);
      Collections.addAll(resultList, message.getValues());
      Collections.addAll(resultList, new String[] { loc.getLatitude(), loc.getLongitude(), grade, explanation });
      return resultList.toArray(new String[resultList.size()]);
    }
  };

  enum FormFieldType {
    REQUIRED, REQUIRED_NOT, OPTIONAL, OPTIONAL_NOT, DATE_TIME, DATE_TIME_NOT, EMPTY, SPECIFIED
  };

  static class FormField {
    public final String label;
    public final String placeholderValue;
    public final FormFieldType type;
    public final int points;
    public int count;

    public FormField(FormFieldType type, String label, String placeholderValue, int points) {
      this.type = type;
      this.label = label;
      this.placeholderValue = placeholderValue;
      this.points = points;
    }

    public int test(String value, ArrayList<String> explanations) {
      if (!ENABLE_NON_ACTIONABLE_FIELDS && points == NON_SCORABLE_FIELD_POINTS) {
        return NON_SCORABLE_FIELD_POINTS;
      }

      var isOk = false;
      var explanation = "";

      switch (type) {
      case DATE_TIME:
        if (value == null) {
          explanation = label + " must be supplied";
        } else {
          try {
            LocalDateTime.parse(value, FORMATTER);
            isOk = true;
          } catch (Exception e) {
            explanation = label + "(" + value + ") is not a valid Date/Time";
          }
        }
        break;

      case DATE_TIME_NOT:
        if (value == null) {
          explanation = label + " must be supplied";
        } else if (value.equalsIgnoreCase(placeholderValue)) {
          explanation = label + "(" + value + ") must not be " + placeholderValue;
        } else {
          try {
            LocalDateTime.parse(value, FORMATTER);
            isOk = true;
          } catch (Exception e) {
            explanation = label + "(" + value + ") is not a valid Date/Time";
          }
        }
        break;

      case EMPTY:
        isOk = value == null || value.isBlank();
        if (!isOk) {
          explanation = label + "(" + value + ") must be blank";
        }
        break;

      case OPTIONAL:
        isOk = true;
        break;

      case OPTIONAL_NOT:
        if (value != null && value.equalsIgnoreCase(placeholderValue)) {
          explanation = label + "(" + value + ") must not be " + placeholderValue;
        } else {
          isOk = true;
        }
        break;

      case REQUIRED:
        if (value == null) {
          explanation = label + " must be supplied";
        } else if (value.isBlank()) {
          explanation = label + "(" + value + ") must not be blank";
        } else {
          isOk = true;
        }
        break;

      case REQUIRED_NOT:
        if (value == null) {
          explanation = label + " must be supplied";
        } else if (value.isBlank()) {
          explanation = label + "(" + value + ") must not be blank";
        } else if (value.equalsIgnoreCase(placeholderValue)) {
          isOk = false;
          explanation = label + "(" + value + ") must not be " + placeholderValue;
        } else {
          isOk = true;
        }

        break;

      case SPECIFIED:
        if (value == null) {
          explanation = label + " must be " + placeholderValue;
        } else if (!value.equalsIgnoreCase(placeholderValue)) {
          explanation = label + "(" + value + ") must be " + placeholderValue;
        } else {
          isOk = true;
        }
        break;

      default:
        throw new RuntimeException("unhandled type: " + type.toString());
      }

      var returnPoints = 0;
      if (isOk) {
        ++count;
        returnPoints = points;
      } else {
        explanations.add(explanation);
      }

      return returnPoints;
    }
  };

  private Map<String, FormField> sfMap = new LinkedHashMap<>(); // scoreableFieldMap
  private Map<String, FormField> nsfMap = new LinkedHashMap<>(); // nonScorableFieldMap

  private List<Result> results = new ArrayList<Result>();

  private Path imageAllPath;
  private Path imagePassPath;
  private Path imageBadSizePath;
  private int maxImageSize = -1;

  public ETO_2022_12_22_Aggregator() {
    super(logger);
  }

  public void initialize() {
    var outputPath = Path.of(cm.getAsString(Key.PATH), "output", "image");
    FileUtils.deleteDirectory(outputPath);

    imageAllPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "all"));
    imagePassPath = FileUtils.createDirectory(Path.of(outputPath.toString(), "pass"));
    imageBadSizePath = FileUtils.createDirectory(Path.of(outputPath.toString(), "badSize"));

    maxImageSize = cm.getAsInt(Key.MAX_IMAGE_SIZE, 5500);

    // header
    nsfMap
        .put("organization",
            new FormField(FormFieldType.SPECIFIED, "Agency/Group name", "ETO ICS-213RR Santa Purchase Request", FP1));

    sfMap.put("activityDateTime", new FormField(FormFieldType.DATE_TIME_NOT, "Box 2 Date/Time", DATE_TIME_NOT, FP1));

    nsfMap
        .put("requestNumber",
            new FormField(FormFieldType.SPECIFIED, "Resource Request Number", "12-22-22-ETO-SR1", FP0));

    // line 1
    sfMap.put("quantity1", new FormField(FormFieldType.REQUIRED_NOT, "Item Quantity#1", "REQ", FP1));

    nsfMap.put("type1", new FormField(FormFieldType.EMPTY, "Item Type#1", null, FP0));

    nsfMap.put("kind1", new FormField(FormFieldType.EMPTY, "Item Kind#1", null, FP0));

    sfMap.put("item1", new FormField(FormFieldType.REQUIRED_NOT, "Item Description#1", "REQUIRED", FP1));

    sfMap
        .put("requestedDateTime1",
            new FormField(FormFieldType.DATE_TIME_NOT, "Requested Date/Time#1", DATE_TIME_NOT, FP1));

    nsfMap.put("estimatedDateTime1", new FormField(FormFieldType.EMPTY, "Estimated Date/Time#1", null, FP0));

    sfMap.put("cost1", new FormField(FormFieldType.REQUIRED_NOT, "Item Cost#1", "REQUIRED", FP1));

    // line 2-8
    for (int i = 2; i <= 8; ++i) {
      nsfMap.put("quantity" + i, new FormField(FormFieldType.EMPTY, "Item Quantity#" + i, null, FP0));
      nsfMap.put("type" + i, new FormField(FormFieldType.EMPTY, "Item Type#" + i, null, FP0));
      nsfMap.put("kind" + i, new FormField(FormFieldType.EMPTY, "Item Kind#" + i, null, FP0));
      nsfMap.put("item" + i, new FormField(FormFieldType.EMPTY, "Item Description#" + i, null, FP0));
      nsfMap.put("requestedDateTime" + i, new FormField(FormFieldType.EMPTY, "Requested Date/Time#" + i, null, FP0));
      nsfMap.put("estimatedDateTime" + i, new FormField(FormFieldType.EMPTY, "Estimated Date/Time#" + i, null, FP0));
      nsfMap.put("cost" + i, new FormField(FormFieldType.EMPTY, "Item Cost#" + i, null, FP0));
    }

    // rest of request
    sfMap
        .put("delivery", new FormField(FormFieldType.REQUIRED_NOT, "Delivery/Reporting Location",
            "YOUR GPS COORDINATES - in decimal degrees - REQUIRED", FP1));

    sfMap
        .put("substitutes",
            new FormField(FormFieldType.OPTIONAL_NOT, "Substitutes", "OPTIONAL - MAKE BLANK IF NO ENTRY", FP1));

    sfMap
        .put("requestedBy", new FormField(FormFieldType.REQUIRED_NOT, "Requested By",
            "Your Name & Call Sign - then select PRIORITY----->", FP1));

    sfMap.put("priority", new FormField(FormFieldType.REQUIRED, "Priority", null, FP1));

    nsfMap.put("approvedBy", new FormField(FormFieldType.SPECIFIED, "Section Chief", "David Rudolph Rednose", FP0));

    // logistics
    sfMap
        .put("logisticsOrderNumber", new FormField(FormFieldType.REQUIRED_NOT, "Log Order Number", "MAKE ONE UP", FP1));

    sfMap
        .put("supplierInfo", new FormField(FormFieldType.REQUIRED_NOT, "Supplier Info",
            "MAKE UP or USE A REAL PHONE NUMBER FOR YOUR FAVORITE AMATEUR RADIO SUPPLIER", FP1));

    sfMap
        .put("supplierName",
            new FormField(FormFieldType.REQUIRED_NOT, "Supplier Name", "NAME OF FAVORITE AMATEUR RADIO SUPPLIER", FP1));

    sfMap
        .put("supplierPointOfContact",
            new FormField(FormFieldType.REQUIRED_NOT, "Point of Contact", "MAKE ONE UP", FP1));

    sfMap
        .put("supplyNotes", new FormField(FormFieldType.OPTIONAL_NOT, "Notes",
            "OPTIONAL - ADD ACCESSORIES OR MAKE BLANK IF NO ENTRY", FP1));

    sfMap
        .put("logisticsAuthorizer", new FormField(FormFieldType.REQUIRED_NOT, "Logistics Rep",
            "MAKE ONE UP              UPDATE DATE/TIME -->", FP1));

    sfMap
        .put("logisticsDateTime",
            new FormField(FormFieldType.DATE_TIME_NOT, "Logistics Date/Time", DATE_TIME_NOT, FP1));

    sfMap.put("orderedBy", new FormField(FormFieldType.REQUIRED_NOT, "Ordered By", "YOUR NAME AND CALL SIGN", FP1));

    // finance
    nsfMap.put("financeComments", new FormField(FormFieldType.SPECIFIED, "Finance Comments", "HAPPY HOLIDAYS !!", FP0));

    sfMap.put("financeName", new FormField(FormFieldType.REQUIRED_NOT, "Finance Chief", "MAKE ONE UP OR SPOUSE", FP1));

    sfMap
        .put("financeDateTime", new FormField(FormFieldType.DATE_TIME_NOT, "Finance Date/Time By", DATE_TIME_NOT, FP1));

  }

  @Override
  public void aggregate(Map<MessageType, List<ExportedMessage>> messageMap) {
    initialize();

    super.aggregate(messageMap);

    var points = 0;
    var explanations = new ArrayList<String>();

    var ppAllParticipantCount = 0;
    var ppCount = 0;
    var ppImageAttachedOk = 0;
    var ppImageSizeOk = 0;
    var ppLatLongOk = 0;

    var ppScoreCounter = new Counter();
    var ppItemCounter = new Counter();
    var ppCostCounter = new Counter();

    for (var m : aggregateMessages) {
      ++ppAllParticipantCount;
      var from = m.from();
      if (dumpIds.contains(from)) {
        logger.info("dump: " + from);
      }

      var map = fromMessageMap.get(from);
      var message = getIcs213RRMessage(map);
      if (message == null) {
        logger.info("skipping messages from " + from + ", since no ICS213RR message");
        continue;
      }

      ++ppCount;

      var imageFileName = getImageFile(message);
      if (imageFileName != null) {
        var bytes = message.attachments.get(imageFileName);
        if (bytes != null) {
          ++ppImageAttachedOk;
          points += 25;

          var isImageSizeOk = false;
          if (bytes.length <= maxImageSize) {
            ++ppImageSizeOk;
            points += 25;
            isImageSizeOk = true;
          } else {
            explanations.add("image size (" + bytes.length + ") > max(" + maxImageSize + ")");
          }
          writeImage(message, imageFileName, bytes, isImageSizeOk);
        }
      } else {
        explanations.add("no image attachment found");
      }

      LatLongPair location = null;
      var locationString = message.delivery;
      if (locationString == null || locationString.isBlank()) {
        explanations.add("no Delivery/Reporting Location");
      } else {
        var llFields = locationString.split(",");
        if (llFields.length != 2) {
          explanations.add("can not parse Delivery/Reporting Location: " + locationString);
        } else {
          location = new LatLongPair(llFields[0], llFields[1]);
          if (location == null || !location.isValid()) {
            explanations.add("can not parse Delivery/Reporting Location: " + locationString);
          } else {
            points += 25;
            ++ppLatLongOk;
          }
        }
      }

      // scorable and non-scorable fields
      var sfPoints = 0;
      var nsfPoints = 0;
      nsfPoints += nsfMap.get("organization").test(message.organization, explanations);
      sfPoints += sfMap.get("activityDateTime").test(message.activityDateTime, explanations);
      nsfPoints += nsfMap.get("requestNumber").test(message.requestNumber, explanations);

      var lineItem = message.lineItems.get(0);
      sfPoints += sfMap.get("quantity1").test(lineItem.quantity(), explanations);
      nsfPoints += nsfMap.get("kind1").test(lineItem.kind(), explanations);
      nsfPoints += nsfMap.get("type1").test(lineItem.type(), explanations);
      sfPoints += sfMap.get("item1").test(lineItem.item(), explanations);
      sfPoints += sfMap.get("requestedDateTime1").test(lineItem.requestedDateTime(), explanations);
      nsfPoints += nsfMap.get("estimatedDateTime1").test(lineItem.estimatedDateTime(), explanations);
      sfPoints += sfMap.get("cost1").test(lineItem.cost(), explanations);

      ppItemCounter.increment(lineItem.item());
      ppCostCounter.increment(lineItem.cost());

      for (int i = 2; i <= 8; ++i) {
        lineItem = message.lineItems.get(i - 1);
        try {
          nsfPoints += nsfMap.get("quantity" + String.valueOf(i)).test(lineItem.quantity(), explanations);
          nsfPoints += nsfMap.get("kind" + String.valueOf(i)).test(lineItem.kind(), explanations);
          nsfPoints += nsfMap.get("type" + String.valueOf(i)).test(lineItem.type(), explanations);
          nsfPoints += nsfMap.get("item" + String.valueOf(i)).test(lineItem.item(), explanations);
          nsfPoints += nsfMap
              .get("requestedDateTime" + String.valueOf(i))
                .test(lineItem.requestedDateTime(), explanations);
          nsfPoints += nsfMap
              .get("estimatedDateTime" + String.valueOf(i))
                .test(lineItem.estimatedDateTime(), explanations);
          nsfPoints += nsfMap.get("cost" + String.valueOf(i)).test(lineItem.cost(), explanations);
        } catch (Exception e) {
          logger.error("expection processing for call: " + m.from() + ", line: " + i + ", " + e.getLocalizedMessage());
        }
      }

      sfPoints += sfMap.get("delivery").test(message.delivery, explanations);
      sfPoints += sfMap.get("substitutes").test(message.substitutes, explanations);
      sfPoints += sfMap.get("requestedBy").test(message.requestedBy, explanations);
      sfPoints += sfMap.get("priority").test(message.priority, explanations);
      nsfPoints += nsfMap.get("approvedBy").test(message.approvedBy, explanations);

      // logistics
      sfPoints += sfMap.get("logisticsOrderNumber").test(message.logisticsOrderNumber, explanations);
      sfPoints += sfMap.get("supplierInfo").test(message.supplierInfo, explanations);
      sfPoints += sfMap.get("supplierName").test(message.supplierName, explanations);
      sfPoints += sfMap.get("supplierPointOfContact").test(message.supplierPointOfContact, explanations);
      sfPoints += sfMap.get("supplyNotes").test(message.supplyNotes, explanations);
      sfPoints += sfMap.get("logisticsAuthorizer").test(message.logisticsAuthorizer, explanations);
      sfPoints += sfMap.get("logisticsDateTime").test(message.logisticsDateTime, explanations);
      sfPoints += sfMap.get("orderedBy").test(message.orderedBy, explanations);

      // finance
      nsfPoints += nsfMap.get("financeComments").test(message.financeComments, explanations);
      sfPoints += sfMap.get("financeName").test(message.financeName, explanations);
      sfPoints += sfMap.get("financeDateTime").test(message.financeDateTime, explanations);

      points += 25 * (sfPoints / (double) sfMap.size());
      points += 0 * (nsfPoints / (double) nsfMap.size());

      points = Math.min(100, points);
      points = Math.max(0, points);

      var grade = String.valueOf(points);
      var explanation = (points == 100 && explanations.size() == 0) //
          ? "Perfect Score!"
          : String.join("\n", explanations);

      ppScoreCounter.increment(points);

      var result = new Result(message, location, grade, explanation);
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
    for (var key : sfMap.keySet()) {
      var af = sfMap.get(key);
      sb.append("  " + formatPP(af.label, af.count, ppCount));
    }

    if (ENABLE_NON_ACTIONABLE_FIELDS) {
      sb.append("\nNon-Scorable Actionable Fields\n");
      for (var key : nsfMap.keySet()) {
        var af = nsfMap.get(key);
        sb.append(" " + formatPP(af.label, af.count, ppCount));
      }
    }

    sb.append("\nscores: \n");
    var it = ppScoreCounter.getDescendingKeyIterator();
    while (it.hasNext()) {
      @SuppressWarnings("unchecked")
      var entry = (Entry<Integer, Integer>) it.next();
      sb.append(" score: " + entry.getKey() + ", count: " + entry.getValue() + "\n");
    }

    sb.append("\nMost requested Items: \n");
    it = ppItemCounter.getDescendingKeyIterator();
    while (it.hasNext()) {
      @SuppressWarnings("unchecked")
      var entry = (Entry<Integer, Integer>) it.next();
      sb.append(" item: " + entry.getKey() + ", count: " + entry.getValue() + "\n");
    }

    sb.append("\nMost common Costs: \n");
    it = ppCostCounter.getDescendingKeyIterator();
    while (it.hasNext()) {
      @SuppressWarnings("unchecked")
      var entry = (Entry<Integer, Integer>) it.next();
      sb.append(" item: " + entry.getKey() + ", count: " + entry.getValue() + "\n");
    }

    logger.info(sb.toString());
  }

  private Ics213RRMessage getIcs213RRMessage(Map<MessageType, List<ExportedMessage>> map) {
    var list = map.get(MessageType.ICS_213_RR);
    if (list != null) {
      Collections.reverse(list);
      var message = (Ics213RRMessage) list.iterator().next();
      return message;
    } else {
      return null;
    }
  }

  protected String formatPercent(Double d) {
    if (d == null) {
      return "";
    }

    return String.format("%.2f", 100d * d) + "%";
  }

  protected String formatPP(String label, int okCount, int ppCount) {
    var notOkCount = ppCount - okCount;
    var okPercent = okCount / (double) ppCount;
    var notOkPercent = 1d - okPercent;
    return "  " + label + ": " //
        + okCount + "(" + formatPercent(okPercent) + ") ok, " //
        + notOkCount + "(" + formatPercent(notOkPercent) + ") not ok" //
        + "\n";
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

  private void writeImage(ExportedMessage m, String imageFileName, byte[] bytes, boolean isImageSizeOk) {

    imageFileName = m.from + "-" + imageFileName;
    try {
      // write the file
      var allImagePath = Path.of(imageAllPath.toString(), imageFileName);
      Files.write(allImagePath, bytes);

      // create the link to pass or fail
      if (isImageSizeOk) {
        var passImagePath = Path.of(imagePassPath.toString(), imageFileName);
        Files.createLink(passImagePath, allImagePath);
      } else {
        var badSizePath = Path.of(imageBadSizePath.toString(), imageFileName);
        Files.createLink(badSizePath, allImagePath);
      }
    } catch (Exception e) {
      logger
          .error("Exception writing image file for call: " + m.from + ", messageId: " + m.messageId + ", "
              + e.getLocalizedMessage());
    }
  }

  @Override
  public void output(String pathName) {
    output(pathName, "aggregated-ics-213-rr.csv", results);
  }

  private void output(String pathName, String fileName, List<Result> entries) {
    Path outputPath = Path.of(pathName, "output", fileName);
    FileUtils.makeDirIfNeeded(outputPath.toString());

    var messageCount = 0;
    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(Result.getHeaders());

      if (entries.size() > 0) {
        entries.sort((Result s1, Result s2) -> s1.message.from.compareTo(s2.message.from));
        for (Result e : entries) {
          if (e != null) {
            var values = e.getValues();
            if (values != null) {
              writer.writeNext(values);
              ++messageCount;
            }
          } else {
            continue;
          }
        }
      }

      writer.close();
      logger.info("wrote " + messageCount + " aggregate messages to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
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
