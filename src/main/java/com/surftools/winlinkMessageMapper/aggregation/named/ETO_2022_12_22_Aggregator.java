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

  public static final int DEFAULT_AF_POINTS = 1;
  public static final int DEFAULT_NAF_POINTS = 0;
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

  static enum AFType {
    REQUIRED, REQUIRED_NOT, OPTIONAL, OPTIONAL_NOT, DATE_TIME, DATE_TIME_NOT, EMPTY, SPECIFIED
  };

  static class ActionableField {
    public final String explanation;
    public final String summaryText;
    public final String placeholderValue;
    public final AFType type;
    public int points;
    public int count;

    public ActionableField(AFType type, String explanation, String summaryText, String placeholderValue) {
      this.type = type;
      this.explanation = explanation;
      this.summaryText = summaryText;
      this.placeholderValue = placeholderValue;
      this.points = DEFAULT_AF_POINTS;
    }

    public ActionableField(AFType type, String explanation, String summaryText, String placeholderValue, int points) {
      this.type = type;
      this.explanation = explanation;
      this.summaryText = summaryText;
      this.placeholderValue = placeholderValue;
      this.points = points;
    }

    public int test(String value, ArrayList<String> explanations) {
      if (!ENABLE_NON_ACTIONABLE_FIELDS && points == DEFAULT_NAF_POINTS) {
        return DEFAULT_NAF_POINTS;
      }

      var isOk = false;
      var explanationString = explanation;
      var returnPoints = 0;

      switch (type) {
      case DATE_TIME:
        try {
          LocalDateTime.parse(value, FORMATTER);
          isOk = true;
        } catch (Exception e) {
          ;
        }
        break;

      case DATE_TIME_NOT:
        try {
          var valueDT = LocalDateTime.parse(value, FORMATTER);
          var defaultDT = LocalDateTime.parse(placeholderValue, FORMATTER);
          if (valueDT.compareTo(defaultDT) != 0) {
            isOk = true;
          }
        } catch (Exception e) {
          ;
        }
        break;

      case EMPTY:
        isOk = value == null || value.isBlank();
        break;

      case OPTIONAL:
        isOk = true;
        break;

      case OPTIONAL_NOT:
        isOk = true;
        if (value != null) {
          isOk = !value.equals(placeholderValue);
        } else {
          isOk = true;
        }
        break;

      case REQUIRED:
        isOk = value != null && !value.isBlank();
        break;

      case REQUIRED_NOT:
        isOk = value != null && !value.isBlank() && !value.equals(placeholderValue);
        break;

      case SPECIFIED:
        isOk = value != null && value.equalsIgnoreCase(placeholderValue);
        break;

      default:
        throw new RuntimeException("unhandled type: " + type.toString());
      }

      if (isOk) {
        ++count;
        returnPoints = points;
      } else {
        if (value != null) {
          explanationString = explanationString.replaceAll("VALUE", value);
        }
        if (placeholderValue != null) {
          explanationString = explanationString.replaceAll("DEFAULT", placeholderValue);
        }
        explanations.add(explanationString);
      }

      return returnPoints;
    }
  };

  private Map<String, ActionableField> afMap = new LinkedHashMap<>(); // scoreable
  private Map<String, ActionableField> nafMap = new LinkedHashMap<>(); // non-scorable

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
    nafMap
        .put("organization", new ActionableField(AFType.SPECIFIED, "agency/group name (VALUE) not (DEFAULT)",
            "Agency/Group name", "ETO ICS-213RR Santa Purchase Request", DEFAULT_NAF_POINTS));

    afMap
        .put("activityDateTime", new ActionableField(AFType.DATE_TIME_NOT, "Box 2 (VALUE) not a valid date/time",
            "Box 2 Date/Time", "2022-12-22 06:00"));

    nafMap
        .put("requestNumber", new ActionableField(AFType.SPECIFIED, "Resource Request Number (VALUE) not (DEFAULT)",
            "Resource Request Number", "12-22-22-ETO-SR1", DEFAULT_NAF_POINTS));

    // line 1
    afMap
        .put("quantity1",
            new ActionableField(AFType.REQUIRED_NOT, "Item Quantity#1 not provided", "Item Quantity#1", "REQ"));

    nafMap
        .put("type1",
            new ActionableField(AFType.EMPTY, "Item Type#1 should be blank", "Item Type#1", null, DEFAULT_NAF_POINTS));

    nafMap
        .put("kind1",
            new ActionableField(AFType.EMPTY, "Item Kind#1 should be blank", "Item Kind#1", null, DEFAULT_NAF_POINTS));

    afMap
        .put("item1", new ActionableField(AFType.REQUIRED_NOT, "Item Description#1 not provided", "Item Description#1",
            "REQUIRED"));

    afMap
        .put("requestedDateTime1", new ActionableField(AFType.DATE_TIME_NOT,
            "Requested Date/Time#1 not a valid date/time", "Requested Date/Time#1", "2022-12-22 06:00"));

    nafMap
        .put("estimatedDateTime1", new ActionableField(AFType.EMPTY, "Estimated Date/Time#1 should be blank",
            "Estimated Date/Time#1", null, DEFAULT_NAF_POINTS));

    afMap.put("cost1", new ActionableField(AFType.REQUIRED_NOT, "Item Cost#1 not provided", "Item Cost#1", "REQUIRED"));

    // line 2-8
    for (int i = 2; i <= 8; ++i) {
      nafMap
          .put("quantity" + i, new ActionableField(AFType.EMPTY, "Item Quantity#" + i + " should be blank",
              "Item Quantity#" + i, null, DEFAULT_NAF_POINTS));

      nafMap
          .put("type" + i, new ActionableField(AFType.EMPTY, "Item Type#" + i + " should be blank", "Item Type#" + i,
              null, DEFAULT_NAF_POINTS));

      nafMap
          .put("kind" + i, new ActionableField(AFType.EMPTY, "Item Kind#" + i + " should be blank", "Item Kind#" + i,
              null, DEFAULT_NAF_POINTS));

      nafMap
          .put("item" + i, new ActionableField(AFType.EMPTY, "Item Description#" + i + " should be blank",
              "Item Description#" + i, null, DEFAULT_NAF_POINTS));

      nafMap
          .put("requestedDateTime" + i, new ActionableField(AFType.EMPTY,
              "Requested Date/Time#" + i + " should be blank", "Requested Date/Time#" + i, null, DEFAULT_NAF_POINTS));

      nafMap
          .put("estimatedDateTime" + i, new ActionableField(AFType.EMPTY,
              "Estimated Date/Time#" + i + " should be blank", "Estimated Date/Time#" + i, null, DEFAULT_NAF_POINTS));

      nafMap
          .put("cost" + i, new ActionableField(AFType.EMPTY, "Item Cost#" + i + " should be blank", "Item Cost#" + i,
              null, DEFAULT_NAF_POINTS));
    }

    // rest of request
    afMap
        .put("delivery", new ActionableField(AFType.REQUIRED, "Deliver/Reporting location not provided",
            "Delivery/Reporting Location", null));

    afMap
        .put("substitutes", new ActionableField(AFType.OPTIONAL_NOT, "Substitutes not provided", "Substitutes",
            "OPTIONAL - MAKE BLANK IF NO ENTRY"));

    afMap
        .put("requestedBy", new ActionableField(AFType.REQUIRED_NOT, "Requested By not provided", "Requested By",
            "Your Name & Call Sign - then select PRIORITY----->"));

    afMap.put("priority", new ActionableField(AFType.REQUIRED, "Priority not provided", "Priority", null));

    nafMap
        .put("approvedBy", new ActionableField(AFType.SPECIFIED, "Section Chief (VALUE) not (DEFAULT)", "Section Chief",
            "David Rudolph Rednose", DEFAULT_NAF_POINTS));

    // logistics
    afMap
        .put("logisticsOrderNumber", new ActionableField(AFType.REQUIRED_NOT, "Log Order Number not provided",
            "Log Order Number", "MAKE ONE UP", DEFAULT_AF_POINTS));

    afMap
        .put("supplierInfo", new ActionableField(AFType.REQUIRED_NOT, "Supplier Info not provided", "Supplier Info",
            "MAKE UP or USE A REAL PHONE NUMBER FOR YOUR FAVORITE AMATEUR RADIO SUPPLIER", DEFAULT_AF_POINTS));

    afMap
        .put("supplierName", new ActionableField(AFType.REQUIRED_NOT, "Supplier Name not provided", "Supplier Name",
            "NAME OF FAVORITE AMATEUR RADIO SUPPLIER", DEFAULT_AF_POINTS));

    afMap
        .put("supplierPointOfContact", new ActionableField(AFType.REQUIRED_NOT, "Point of Contact not provided",
            "Point of Contact", "MAKE ONE UP", DEFAULT_AF_POINTS));

    afMap
        .put("supplyNotes", new ActionableField(AFType.REQUIRED_NOT, "Notes not provided", "Notes",
            "OPTIONAL - ADD ACCESSORIES OR MAKE BLANK IF NO ENTRY", DEFAULT_AF_POINTS));

    afMap
        .put("logisticsAuthorizer", new ActionableField(AFType.REQUIRED_NOT, "Logistics Rep not provided",
            "Logistics Rep", "MAKE ONE UP              UPDATE DATE/TIME -->", DEFAULT_AF_POINTS));

    afMap
        .put("logisticsDateTime", new ActionableField(AFType.DATE_TIME_NOT, "Logististics Date/Time not provided",
            "Logistics Date/Time", "2022-12-22 06:00", DEFAULT_AF_POINTS));

    afMap
        .put("orderedBy", new ActionableField(AFType.REQUIRED_NOT, "Ordered By not provided", "Ordered By",
            "YOUR NAME AND CALL SIGN", DEFAULT_AF_POINTS));

    // finance
    nafMap
        .put("financeComments", new ActionableField(AFType.SPECIFIED, "Finance Comments (VALUE) not (DEFAULT)",
            "Finance Comments", "HAPPY HOLIDAYS !!", DEFAULT_NAF_POINTS));

    afMap
        .put("financeName", new ActionableField(AFType.REQUIRED_NOT, "Finance Chief not provided", "Finance Chief",
            "MAKE ONE UP OR SPOUSE", DEFAULT_AF_POINTS));

    afMap
        .put("financeDateTime", new ActionableField(AFType.DATE_TIME_NOT, "Finance Date/Time not provided",
            "Finance Date/Time By", "2022-12-22 06:00", DEFAULT_AF_POINTS));

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

      // "actionable fields"
      var afPoints = 0;
      var nafPoints = 0;
      nafPoints += nafMap.get("organization").test(message.organization, explanations);
      afPoints += afMap.get("activityDateTime").test(message.activityDateTime, explanations);
      nafPoints += nafMap.get("requestNumber").test(message.requestNumber, explanations);

      var lineItem = message.lineItems.get(0);
      afPoints += afMap.get("quantity1").test(lineItem.quantity(), explanations);
      nafPoints += nafMap.get("kind1").test(lineItem.kind(), explanations);
      nafPoints += nafMap.get("type1").test(lineItem.type(), explanations);
      afPoints += afMap.get("item1").test(lineItem.item(), explanations);
      afPoints += afMap.get("requestedDateTime1").test(lineItem.requestedDateTime(), explanations);
      nafPoints += nafMap.get("estimatedDateTime1").test(lineItem.estimatedDateTime(), explanations);
      afPoints += afMap.get("cost1").test(lineItem.cost(), explanations);

      for (int i = 2; i <= 8; ++i) {
        lineItem = message.lineItems.get(i - 1);
        try {
          nafPoints += nafMap.get("quantity" + String.valueOf(i)).test(lineItem.quantity(), explanations);
          nafPoints += nafMap.get("kind" + String.valueOf(i)).test(lineItem.kind(), explanations);
          nafPoints += nafMap.get("type" + String.valueOf(i)).test(lineItem.type(), explanations);
          nafPoints += nafMap.get("item" + String.valueOf(i)).test(lineItem.item(), explanations);
          nafPoints += nafMap
              .get("requestedDateTime" + String.valueOf(i))
                .test(lineItem.requestedDateTime(), explanations);
          nafPoints += nafMap
              .get("estimatedDateTime" + String.valueOf(i))
                .test(lineItem.estimatedDateTime(), explanations);
          nafPoints += nafMap.get("cost" + String.valueOf(i)).test(lineItem.cost(), explanations);
        } catch (Exception e) {
          logger.error("expection processing for call: " + m.from() + ", line: " + i + ", " + e.getLocalizedMessage());
        }
      }

      afPoints += afMap.get("delivery").test(message.delivery, explanations);
      afPoints += afMap.get("substitutes").test(message.substitutes, explanations);
      afPoints += afMap.get("requestedBy").test(message.requestedBy, explanations);
      afPoints += afMap.get("priority").test(message.priority, explanations);
      nafPoints += nafMap.get("approvedBy").test(message.approvedBy, explanations);

      // logistics
      afPoints += afMap.get("logisticsOrderNumber").test(message.logisticsOrderNumber, explanations);
      afPoints += afMap.get("supplierInfo").test(message.supplierInfo, explanations);
      afPoints += afMap.get("supplierName").test(message.supplierName, explanations);
      afPoints += afMap.get("supplierPointOfContact").test(message.supplierPointOfContact, explanations);
      afPoints += afMap.get("supplyNotes").test(message.supplyNotes, explanations);
      afPoints += afMap.get("logisticsAuthorizer").test(message.logisticsAuthorizer, explanations);
      afPoints += afMap.get("logisticsDateTime").test(message.logisticsDateTime, explanations);
      afPoints += afMap.get("orderedBy").test(message.orderedBy, explanations);

      // finance
      nafPoints += nafMap.get("financeComments").test(message.financeComments, explanations);
      afPoints += afMap.get("financeName").test(message.financeName, explanations);
      afPoints += afMap.get("financeDateTime").test(message.financeDateTime, explanations);

      points += 25 * (afPoints / (double) afMap.size());
      points += 0 * (nafPoints / (double) nafMap.size());

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
    for (var key : afMap.keySet()) {
      var af = afMap.get(key);
      sb.append("  " + formatPP(af.summaryText, af.count, ppCount));
    }

    if (ENABLE_NON_ACTIONABLE_FIELDS) {
      sb.append("\nNon-Scorable Actionable Fields\n");
      for (var key : nafMap.keySet()) {
        var af = nafMap.get(key);
        sb.append(" " + formatPP(af.summaryText, af.count, ppCount));
      }
    }

    sb.append("\nscores: \n");
    var it = ppScoreCounter.getDescendingKeyIterator();
    while (it.hasNext()) {
      @SuppressWarnings("unchecked")
      var entry = (Entry<Integer, Integer>) it.next();
      sb.append(" score: " + entry.getKey() + ", count: " + entry.getValue() + "\n");
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
