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

package com.surftools.wimp.processors.std;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RejectionMessage;
import com.surftools.wimp.parser.AbstractBaseParser;
import com.surftools.wimp.parser.CharacterAssassinator;
import com.surftools.wimp.processors.std.baseExercise.AbstractBaseProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class BaseReadProcessor extends AbstractBaseProcessor {
  protected DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

  private Set<String> expectedDestinations = new LinkedHashSet<>();
  private Set<String> secondaryDestinations = new LinkedHashSet<>();

  private boolean isReadFilteringEnabled = false;
  private int readFilterIncludeCount = 0;
  private int readFilterExcludeCount = 0;
  private Set<String> includeSenderSet;
  private Set<String> excludeSenderSet;

  private static final List<String> DELETE_LIST = Arrays.asList(new String[] { "&#21" });

  static record LocationResult(LatLongPair location, String source) {
  };

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    baseInitialize(cm, mm);
  }

  public void baseInitialize(IConfigurationManager cm, IMessageManager mm) {
    isReadFilteringEnabled = cm.getAsBoolean(Key.READ_FILTER_ENABLED, false);

    var expectedDestinationsString = cm.getAsString(Key.EXPECTED_DESTINATIONS);
    if (expectedDestinationsString != null) {
      var fields = expectedDestinationsString.split(",");
      for (var field : fields) {
        expectedDestinations.add(field);
      }

      logger.info("Expected Destinations: " + expectedDestinations.toString());
    }

    var secondaryDestinationsString = cm.getAsString(Key.SECONDARY_DESTINATIONS);
    if (secondaryDestinationsString != null) {
      var fields = secondaryDestinationsString.split(",");
      for (var field : fields) {
        secondaryDestinations.add(field);
      }

      logger.info("Secondary Destinations: " + secondaryDestinations.toString());
    }
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm, Logger _logger) {
    super.initialize(cm, mm, _logger);
    baseInitialize(cm, mm);
  }

  private InputStream fixInputString(String content) {
    CharacterAssassinator assassinator = new CharacterAssassinator(DELETE_LIST, null);
    content = assassinator.assassinate(content);
    return new ByteArrayInputStream(content.getBytes());
  }

  protected List<ExportedMessage> parseExportedMessages(List<String> fileLines, String fileName) {
    List<ExportedMessage> messages = new ArrayList<>();

    /**
     * I want to have the message lines in very rare circumstances. Here's the best place to get those lines
     */
    var singleMessageLines = new ArrayList<String>(); // lines for a single message
    var messageLines = new ArrayList<List<String>>(); // one list entry per message in file
    var inMessage = false;
    for (var line : fileLines) {
      if (line.trim().equals("<message>")) {
        inMessage = true;
        singleMessageLines = new ArrayList<>();
      }
      if (inMessage) {
        singleMessageLines.add(line);
      }
      if (line.trim().equals("</message>")) {
        inMessage = false;
        messageLines.add(singleMessageLines);
      }
    }

    for (var lines : messageLines) {
      // var inputStream = new ByteArrayInputStream(String.join("\n", lines).getBytes());
      var inputStream = fixInputString(String.join("\n", lines));
      var iNode = 0;
      var nNodes = 0;
      try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(inputStream);
        doc.getDocumentElement().normalize();
        NodeList nodeList = doc.getElementsByTagName("message");
        nNodes = nodeList.getLength();
        for (iNode = 0; iNode < nNodes; ++iNode) {
          Node node = nodeList.item(iNode);
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            ExportedMessage message = readMessage(element, fileName, lines);
            var isSelected = readFilter(message);
            if (isSelected) {
              messages.add(message);
            }
          } // end if XML Message Node
        } // end for over messages
      } catch (Exception e) {
        logger
            .error("Exception processing imput stream, message " + iNode + " of " + nNodes
                + " (maybe not exported Winlink Messages XML file) : " + e.getLocalizedMessage());
      }
    }

    return messages;
  }

  private boolean readFilter(ExportedMessage message) {
    if (!isReadFilteringEnabled) {
      return true;
    }

    var sender = message.from;

    if (includeSenderSet.size() > 0) {
      if (includeSenderSet.contains(sender)) {
        ++readFilterIncludeCount;
        return true;
      } else {
        return false;
      }
    }

    if (excludeSenderSet.size() > 0) {
      if (excludeSenderSet.contains(sender)) {
        ++readFilterExcludeCount;
        return false;
      } else {
        return true;
      }
    }
    return true;
  }

  private ExportedMessage readMessage(Element element, String fileName, List<String> lines) {

    var messageId = element.getElementsByTagName("id").item(0).getTextContent();
    var subject = element.getElementsByTagName("subject").item(0).getTextContent();
    var dtString = element.getElementsByTagName("time").item(0).getTextContent();
    var sender = element.getElementsByTagName("sender").item(0).getTextContent();
    var source = element.getElementsByTagName("source").item(0).getTextContent();
    var mime = element.getElementsByTagName("mime").item(0).getTextContent();

    var isP2p = false;
    var v = element.getElementsByTagName("peertopeer");
    if (v != null && v.item(0) != null && v.item(0).getTextContent() != null) {
      isP2p = Boolean.parseBoolean(v.item(0).getTextContent());
    }

    var locationResult = parseLocation(element, mime, sender, messageId);
    var localDateTime = LocalDateTime.parse(dtString, DT_FORMATTER);

    String[] mimeLines = mime.split("\\n");

    var recipients = getRecipients(mimeLines);
    if (recipients == null) {
      logger.error("null recipients: messageId: " + messageId + ", from " + sender);
    }
    String recipient = recipients[0];
    String toList = recipients[1];
    String ccList = recipients[2];

    ExportedMessage message = null;
    String plainContent = null;
    Map<String, byte[]> attachments = null;

    var parser = AbstractBaseParser.makeMimeMessageParser(messageId, mime);
    if (parser == null) {
      message = new ExportedMessage(messageId, sender, source, recipient, toList, ccList, subject, //
          localDateTime, locationResult.location, locationResult.source, //
          mime, plainContent, attachments, isP2p, fileName, lines);
      return new RejectionMessage(message, RejectType.CANT_PARSE_MIME, message.mime);
    }

    plainContent = parser.getPlainContent();
    attachments = AbstractBaseParser.getAttachments(parser);

    message = new ExportedMessage(messageId, sender, source, recipient, toList, ccList, subject, //
        localDateTime, locationResult.location, locationResult.source, //
        mime, plainContent, attachments, isP2p, fileName, lines);

    return message;
  }

  /**
   * location may be missing, present as "40.187500N, 92.541667W", or even "40.187500N, 92.541667W (GRID SQUARE)"
   *
   * @param element
   * @return
   */
  private LocationResult parseLocation(Element element, String mime, String sender, String messageId) {
    LatLongPair location = null;
    String source = null;

    try {
      var locationString = element.getElementsByTagName("location").item(0).getTextContent();
      if (locationString != null) {
        var fields = locationString.split(",");
        if (fields.length >= 2) {
          var latString = fields[0].substring(0, fields[0].length() - 1);
          if (fields[0].endsWith("S")) {
            latString = "-" + latString.trim();
          }

          var subfields = fields[1].trim().split(" ");
          var lonString = subfields[0].substring(0, subfields[0].length() - 1);
          if (subfields[0].endsWith("W")) {
            lonString = "-" + lonString.trim();
          }

          location = new LatLongPair(latString, lonString);

          var leftParenIndex = fields[1].indexOf("(");
          var rightParenIndex = fields[1].indexOf(")");
          if (leftParenIndex >= 0 && rightParenIndex >= 0 && leftParenIndex < rightParenIndex) {
            source = fields[1].substring(leftParenIndex + 1, rightParenIndex);
          }
        } else {
          if (mime != null) {
            // looking for something like: X-Location: 38.660000N, 122.870667W (SPECIFIED)
            var mimeLines = mime.split("\n");
            for (var line : mimeLines) {
              line = line.toUpperCase();
              if (line.startsWith("X-LOCATION:")) {
                fields = line.split(" ");
                if (fields.length >= 4) {
                  var latString = fields[1].substring(0, fields[0].length() - 2);
                  if (fields[0].endsWith("S")) {
                    latString = "-" + latString.trim();
                  }

                  var lonString = fields[2].substring(0, fields[2].length() - 1);
                  if (fields[2].endsWith("W")) {
                    lonString = "-" + lonString.trim();
                  }

                  location = new LatLongPair(latString, lonString);

                  source = "OTHER";
                  if (line.contains("GRID SQUARE")) {
                    source = "GRID SQUARE";
                  } else if (line.contains("SPECIFIED")) {
                    source = "SPECIFIED";
                  } else if (line.contains("GPS")) {
                    source = "GPS";
                  } else {
                    source = "UNKNOWN";
                  }

                  break;
                } // end if 4 fields in X-Location
              } // end if X-Location line
            } // end loop over mimeLines
          } // end if mime != null
        } // end if <location> tag has 2 fields
      } // end if location string
    } catch (Exception e) {
      ;
    }

    var locationResult = new LocationResult(location, source);
    return locationResult;
  }

  // Subject: DYFI Automatic Entry - Winlink EXERCISE
  // To: SMTP:dyfi_reports_automated@usgs.gov,
  // ETO-02@winlink.org
  // Cc: KC3DOW@winlink.org,
  // W3IHP@winlink.org,
  // SMTP:kc3dow@kanidor.com,
  // SMTP:w3ihp@jmbventures.com
  // Message-ID: Z4VVKSSM1GII

  /**
   *
   * @param mimeLines
   * @return array of Strings
   *
   *         0 -- the "recipient"
   *
   *         1 -- the toList
   *
   *         2 -- the ccList
   */
  public String[] getRecipients(String[] mimeLines) {
    List<String> toList = new ArrayList<String>();
    List<String> ccList = new ArrayList<String>();
    List<String> theList = null;

    boolean isFound = false;
    for (String line : mimeLines) {
      if (line.startsWith("Message-ID: ")) {
        break;
      }

      if (line.startsWith("To: ")) {
        isFound = true;
        theList = toList;
      }

      if (line.startsWith("Cc: ")) {
        isFound = true;
        theList = ccList;
      }

      if (isFound) {
        line = pre_fixAddress(line);
        if (line != null && !line.isBlank()) {
          theList.add(line);
        }
      } else {
        continue;
      }
    } // end for over lines

    List<String> addresses = new ArrayList<>();
    addresses.addAll(toList);
    addresses.addAll(ccList);

    if (addresses.size() == 0) {
      return null;
    }

    logger.debug("found " + addresses.size() + " addresses: " + String.join(", ", addresses));

    String address = chooseBestAddress(addresses);
    address = post_fix(address);

    var strings = new String[] { address, String.join(",", toList), String.join(",", ccList) };
    return strings;
  } // end getRecipients

  /**
   * clean up line, strip leading To: Cc: and space, strip trailing ,
   *
   * @param line
   * @return
   */
  public String pre_fixAddress(String line) {
    if (line == null) {
      return null;
    }

    if (line.startsWith("To: ") || line.startsWith("Cc: ")) {
      String[] fields = line.split(" ");
      line = fields[1];
    }

    if (line.endsWith(",")) {
      line = line.substring(0, line.length() - 1);
    }
    return line.trim();
  }

  /**
   * remove leading "SMTP:", trailing "@..."
   *
   * @param address
   * @return
   */
  private String post_fix(String address) {
    final String smtp = "SMTP:";
    if (address.startsWith(smtp)) {
      address = address.substring(smtp.length());
    }

    int atIndex = address.indexOf("@");
    if (atIndex >= 0) {
      address = address.substring(0, atIndex);
    }
    return address;
  }

  /**
   * choose the best out of potentially many addresses
   *
   * @param addresses
   * @return
   */
  private String chooseBestAddress(List<String> addresses) {

    for (var address : addresses) {
      if (expectedDestinations.contains(post_fix(address))) {
        return address;
      }
    }

    for (var address : addresses) {
      if (secondaryDestinations.contains(post_fix(address))) {
        return address;
      }
    }

    // last ditch
    return addresses.get(0);

  }

  @Override
  public void process() {
    // must wait until FilterProcessor.initialize() has executed
    this.includeSenderSet = FilterProcessor.includeSenderSet;
    this.excludeSenderSet = FilterProcessor.excludeSenderSet;
  }

  @Override
  public void postProcess() {
    if (isReadFilteringEnabled && readFilterIncludeCount > 0) {
      logger.warn("### Read Filter: " + readFilterIncludeCount + " messages included");
    }
    if (isReadFilteringEnabled && readFilterExcludeCount > 0) {
      logger.warn("### Read Filter: " + readFilterExcludeCount + " messages excluded");
    }
  }
}
