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

package com.surftools.wimp.processors.exercise.eto_2027;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.message.PositionMessage;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor;
import com.surftools.wimp.service.map.IMapService;
import com.surftools.wimp.service.map.MapContext;
import com.surftools.wimp.service.map.MapEntry;
import com.surftools.wimp.service.map.MapLayer;
import com.surftools.wimp.service.map.MapService;
import com.surftools.wimp.utils.config.IConfigurationManager;

/**
 * Processor for 2027-01-21: Position, Catalog and Quick messages, centered around Winter Field Day (WFD)
 *
 * We don't have an explict Catalog Request message, so we'll just test the plain subject and content
 *
 * Yikes; the Quick message that used to have an XML form viewer attachment is now just a plain text message too
 *
 *
 * @author bobt
 *
 */
public class ETO_2027_01_21 extends MultiMessageFeedbackProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ETO_2027_01_21.class);

  /**
   * #MM just the necessary fields for a (multi-message) Summary
   */
  private class Summary extends BaseSummary {

    PositionMessage positionMessage;
    PlainMessage catalogMessage;
    PlainMessage quickMessage;

    int attachedImageSize;
    boolean hasAttachedTextFile;
    int reportedNeighbors;
    int actualNeighbors;
    String neighbors;

    public Summary(String from) {
      this.from = from;
      this.explanations = new ArrayList<String>();
    }

    @Override
    public String[] getHeaders() {
      var list = new ArrayList<String>();
      list.addAll(Arrays.asList(super.getHeaders()));
      list
          .addAll(List
              .of("Position mId", "Nearby mId", "Quick mId", //
                  "AttachedImageSize", "AttachedText", //
                  "Quick Nearby", "File Nearby") //
          );
      return list.toArray(new String[0]);
    }

    @Override
    public String[] getValues() {
      var list = new ArrayList<>();
      list.addAll(Arrays.asList(super.getValues()));
      list
          .addAll(List
              .of(mId(positionMessage), mId(catalogMessage), mId(quickMessage), //
                  s(attachedImageSize), Boolean.toString(hasAttachedTextFile), //
                  s(reportedNeighbors), s(actualNeighbors) //
              ));

      return list.toArray(new String[0]);
    };

    public int getMessageCount() {
      var messageCount = positionMessage != null ? 1 : 0;
      messageCount += catalogMessage != null ? 1 : 0;
      messageCount += quickMessage != null ? 1 : 0;

      return messageCount;
    }

    public String getMessageCountSummary() {
      if (getMessageCount() == 3) {
        return "<b>" + from + "</b><hr>\n" + "All messages received!";
      } else {
        var messages = new ArrayList<ExportedMessage>();
        messages.add(positionMessage);
        messages.add(catalogMessage);
        messages.add(quickMessage);

        final var labels = List.of("position", "catalog", "quick");

        var rList = new ArrayList<String>();
        var mList = new ArrayList<String>();
        for (var i = 0; i < messages.size(); ++i) {
          var message = messages.get(i);
          var label = labels.get(i);
          if (message == null) {
            mList.add(label);
          } else {
            rList.add(label);
          }
        }

        var sb = new StringBuilder();
        sb.append("<b>" + from + "</b><hr>\n");
        sb.append("received: " + String.join(",", rList) + "\n");
        sb.append("missing:  " + String.join(",", mList));
        var ret = sb.toString();
        return ret;
      }
    }

    public String getNeighborCountSummary() {
      if (quickMessage == null) {
        return "<b>" + from + "</b><hr>\n" + "No quick message received!";
      } else {
        var sb = new StringBuilder();
        sb.append("<b>" + from + "</b><hr>\n");
        sb.append("actual   neighbors: " + actualNeighbors + "\n");
        sb.append("reported neighbors: " + reportedNeighbors + "\n");
        sb.append("neighbors (actual): " + neighbors);
        var ret = sb.toString();
        return ret;
      }
    }
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    acceptableMessageTypesSet = new LinkedHashSet<MessageType>(List.of(MessageType.POSITION, MessageType.PLAIN));
    super.initialize(cm, mm, logger);

    var extraOutboundMessageText = getNextExerciseInstructions();
    outboundMessageExtraContent = OB_DISCLAIMER + extraOutboundMessageText;
  }

  @Override
  protected void beforeProcessingForSender(String sender) {
    super.beforeProcessingForSender(sender);

    // #MM must instantiate a derived Summary object
    iSummary = summaryMap.getOrDefault(sender, new Summary(sender));
    summaryMap.put(sender, iSummary);
  }

  @Override
  protected void specificProcessing(ExportedMessage message) {
    var summary = (Summary) iSummary;

    var type = message.getMessageType();
    if (type == MessageType.POSITION) {
      handle_PositionMessage(summary, (PositionMessage) message);
    } else if (type == MessageType.PLAIN) {
      route_PlainMessage(summary, (PlainMessage) message);
    }

    summaryMap.put(sender, iSummary);
  }

  private void handle_PositionMessage(Summary summary, PositionMessage m) {
    sts.setExplanationPrefix("(Position) (" + m.messageId + "): ");

    //
    // ETO Winlink Thursday Exercise 1/15/2026
    // WFD2026 Participation: (Yes or No for plans for participation)
    //
    var comments = m.comments;
    count(sts.testIfPresent("Comments should be present", comments));

    if (comments != null) {
      count(sts
          .testStartsWith("Comments line should start with #EV", "ETO Winlink Thursday Exercise 1/15/2026", comments));

      var words = comments.split(" ");
      var lastWord = words[words.length - 1];
      getCounter("WFD Participation").increment(lastWord.toUpperCase());
      final var list = List.of("YES", "NO");
      count(sts.testList("WFD participation should be #EV", list, lastWord.toUpperCase()));

    }

    // #MM update summary
    summary.positionMessage = m;
    summary.messageIds.add(m.messageId);
  }

  /**
   * act as a router to either CatalogMessage or QuickMessage
   *
   * @param summary
   * @param plainMessage
   */
  private void route_PlainMessage(Summary summary, PlainMessage m) {
    sts.setExplanationPrefix("(Plain) (" + m.messageId + "): ");
    // are we full up?
    if (summary.catalogMessage != null && summary.quickMessage != null) {
      logger.info("skipping message: " + m.messageId + ", from: " + m.from + ", subject: " + m.subject + "; unneeded");
      breakLoopForMessageType = true;
      return;
    }
    var addresses = m.toList + "," + m.ccList;
    var addressesSet = new HashSet<String>(Arrays.asList(addresses.split(",")));

    if (m.subject.equals("REQUEST") //
        && addressesSet.contains("INQUIRY@winlink.org") //
        && m.plainContent.startsWith("WL2K_NEARBY")) {
      handle_CatalogMessage(summary, m);
    } else if (m.subject.toUpperCase().startsWith("ETO Winlink Thursday".toUpperCase())) {
      handle_QuickMessage(summary, m);
    } else {
      logger
          .info(
              "skipping message: " + m.messageId + ", from: " + m.from + ", subject: " + m.subject + "; unknown type");
    }
  }

  private void handle_CatalogMessage(Summary summary, PlainMessage m) {
    sts.setExplanationPrefix("(Catalog) (" + m.messageId + "): ");

    if (summary.catalogMessage != null) {
      logger.info("skipping message: " + m.messageId + ", from: " + m.from + ", subject: " + m.subject + "; unneeded");
      return;
    }

    // and that's it!

    // #MM update summary
    summary.catalogMessage = m;
    summary.messageIds.add(m.messageId);
  }

  private void handle_QuickMessage(Summary summary, PlainMessage m) {
    sts.setExplanationPrefix("(Quick) (" + m.messageId + "): ");
    var attachments = m.attachments;
    count(sts.test("Number of attachments should be #EV", "2", String.valueOf(attachments.size())));
    getCounter("Number of attachments").increment(attachments.size());

    int attachedImageSize = -1;
    String imageFileName = getFirstImageFile(m);
    count(sts.testNotNull("Image file attachment should be present", imageFileName));
    if (imageFileName != null) {
      var bytes = attachments.get(imageFileName);
      count(sts
          .test("Image files size should be less that 5 kb", bytes.length <= (6 * 1024), String.valueOf(bytes.length)));
      attachedImageSize = bytes.length;
    }

    var reportedNeighbors = 0;
    var messageLines = m.plainContent.split("\n");
    var countLine = messageLines[3].replaceAll("\"", "'");
    getCounter("reported neighbors").increment(countLine);
    try {
      reportedNeighbors = Integer.parseInt(countLine);
      count(sts.test("Message should be a number", true, String.valueOf(reportedNeighbors)));
    } catch (Exception e) {
      count(sts.test("Message should be a number", false, countLine));
    }

    var actualNeighbors = 0;
    var neighborList = new ArrayList<String>();
    boolean hasAttachedTextFile = false;
    for (var name : attachments.keySet()) {
      var ucName = name.toUpperCase();
      if (ucName.startsWith(m.from) && ucName.endsWith(".TXT")) {

        var content = new String(attachments.get(name));
        var fileLines = content.split("\n");
        for (var fileLine : fileLines) {
          if (fileLine.toUpperCase().contains("ETO WINLINK THURSDAY EXERCISE")) {
            var fields = fileLine.split("\\s+");
            var neighbor = fields[0];
            neighborList.add(neighbor);
            ++actualNeighbors;
          } // end if ETO WLT line
        } // end loop over lines
        hasAttachedTextFile = true;
      } // end if text file
    } // end loop over attachments
    count(sts.testNotNull("Text file attachment should be present", hasAttachedTextFile));

    count(sts
        .test("Reported participants in Quick message should match nearby file", reportedNeighbors == actualNeighbors,
            "in message: " + reportedNeighbors + ", in file: " + actualNeighbors));

    // #MM update summary
    summary.quickMessage = m;
    summary.attachedImageSize = attachedImageSize;
    summary.hasAttachedTextFile = hasAttachedTextFile;
    summary.actualNeighbors = actualNeighbors;
    summary.reportedNeighbors = reportedNeighbors;
    summary.neighbors = String.join("\n", neighborList);
    summary.messageIds.add(m.messageId);
  }

  @Override
  protected void endProcessingForSender(String sender) {
    sts.setExplanationPrefix("");

    var summary = (Summary) summaryMap.get(sender); // #MM
    if (summary.positionMessage == null) {
      summary.explanations.add("No Position Report message received.");
      getCounter("Position Report message received").increment(false);
    } else {
      getCounter("Position Report message received").increment(true);
    }

    if (summary.catalogMessage == null) {
      summary.explanations.add("No Catalog Request message received.");
      getCounter("Catalog Request message received").increment(false);
    } else {
      getCounter("Catalog Request message received").increment(true);
    }

    if (summary.quickMessage == null) {
      summary.explanations.add("No Quick message received.");
      getCounter("Quick message received").increment(false);
    } else {
      getCounter("Quick message received").increment(true);
    }

    summaryMap.put(sender, summary); // #MM
  }

  @Override
  public void postProcess() {
    super.postProcess();

    makeMessageCountMap();
    makeNeighborCountMap();
  }

  record Legend(String label, String color, Predicate<Summary> predicate, Function<Summary, String> popupGenerator) {
  };

  private void makeMapViaLegends(Collection<BaseSummary> summaries, String legendTitle, String fileName, Path path,
      List<Legend> legends) {
    var colorCountMap = new HashMap<String, Integer>();

    var mapEntries = new ArrayList<MapEntry>(summaries.size());
    for (var bs : summaries) {
      var s = (Summary) bs;
      var found = false;
      for (var legend : legends) {
        if (legend.predicate.test(s)) {
          var count = colorCountMap.getOrDefault(legend.color, Integer.valueOf(0));
          ++count;
          colorCountMap.put(legend.color, count);
          var mapEntry = new MapEntry(s.from, s.to, s.location, legend.popupGenerator.apply(s), legend.color);
          mapEntries.add(mapEntry);
          found = true;
          break;
        } // endif predicate matches
      } // end loop over legend entries
      if (!found) {
        logger.debug("not found");
      }
    } // end loop of mapEntries

    var layers = new ArrayList<MapLayer>();
    for (var legend : legends) {
      var color = legend.color;
      var label = legend.label;
      var count = colorCountMap.getOrDefault(color, Integer.valueOf(0));
      layers.add(new MapLayer(label + ", count: " + count, color));
    }

    legendTitle = "WFD 2027 Prep " + legendTitle + " (" + summaries.size() + " total)";
    var context = new MapContext(path, //
        dateString + "-map-" + fileName, // file name
        dateString + legendTitle, // map title
        null, legendTitle, layers, mapEntries);
    var mapService = new MapService(cm, mm);
    mapService.makeMap(context);
  }

  private void makeMessageCountMap() {
    final var green = IMapService.rgbMap.get("green");
    final var yellow = IMapService.rgbMap.get("yellow");
    final var red = IMapService.rgbMap.get("red");

    Function<Summary, String> popup = (s -> s.getMessageCountSummary());

    var summaries = summaryMap.values();
    makeMapViaLegends(summaries, "Message Counts", "neighborCounts", outputPath, List
        .of(//
            new Legend("messages: 3", green, (s -> s.getMessageCount() == 3), popup), //
            new Legend("messages: 2", yellow, (s -> s.getMessageCount() == 2), popup), //
            new Legend("messages: 1", red, (s -> s.getMessageCount() == 1), popup) //
        ));
  }

  private void makeNeighborCountMap() {
    final var green = IMapService.rgbMap.get("green");
    final var yellow = IMapService.rgbMap.get("yellow");
    final var red = IMapService.rgbMap.get("red");
    final var black = IMapService.rgbMap.get("black");

    Function<Summary, String> popup = (s -> s.getNeighborCountSummary());

    var summaries = summaryMap.values();
    makeMapViaLegends(summaries, "Neighbor Counts from Quick Message", "neighborCounts", outputPath, List
        .of(//
            new Legend("More than 5 Actual neighbors", green, (s -> s.actualNeighbors > 5), popup), //
            new Legend("At least 1 Actual neighbor", yellow, (s -> s.actualNeighbors > 0 && s.actualNeighbors <= 5),
                popup), //
            new Legend("No Actual neighbors", red, (s -> s.actualNeighbors == 0 && s.quickMessage != null), popup), //
            new Legend("No Quick message", black, (s -> s.quickMessage == null), popup) //
        ));

  }

}
