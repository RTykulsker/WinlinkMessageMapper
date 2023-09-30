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
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RejectionMessage;
import com.surftools.wimp.parser.AbstractBaseParser;
import com.surftools.wimp.parser.CharacterAssassinator;

/**
 * Reads an "exported message" file, produced by Winlink, creates @{ExportedMessage} records
 *
 * @author bobt
 *
 */
public class ReadProcessor extends AbstractBaseProcessor {
  private static final Logger logger = LoggerFactory.getLogger(ReadProcessor.class);
  DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

  static record LocationResult(LatLongPair location, String source) {
  };

  private static final List<String> DEFAULT_DELETE_LIST = Arrays.asList(new String[] { "&#21" });
  private final List<String> deleteList;

  private Set<String> expectedDestinations = new LinkedHashSet<>();

  public ReadProcessor() {
    this(DEFAULT_DELETE_LIST);
  }

  public ReadProcessor(List<String> deleteList) {
    this.deleteList = deleteList;
  }

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    var expectedDestinationsString = cm.getAsString(Key.EXPECTED_DESTINATIONS);
    if (expectedDestinationsString != null) {
      var fields = expectedDestinationsString.split(",");
      for (var field : fields) {
        expectedDestinations.add(field);
      }

      logger.info("Expected Destinations: " + expectedDestinations.toString());
    }

  }

  @Override
  public void process() {
    Path path = Paths.get(pathName);

    // read all Exported Messages from files
    List<ExportedMessage> exportedMessages = new ArrayList<>();
    for (File file : path.toFile().listFiles()) {
      if (file.isFile()) {
        if (!file.getName().toLowerCase().endsWith(".xml")) {
          continue;
        }
        var fileExportedMessages = readAll(file.toPath());
        exportedMessages.addAll(fileExportedMessages);
      }
    }
    logger.info("read " + exportedMessages.size() + " exported messages from all files");

    mm.load(exportedMessages);
  }

  private List<ExportedMessage> parseExportedMessages(InputStream inputStream) {
    List<ExportedMessage> messages = new ArrayList<>();

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
          ExportedMessage message = readMessage(element);
          messages.add(message);
        } // end if XML Message Node
      } // end for over messages
    } catch (Exception e) {
      logger
          .error("Exception processing imput stream, message " + iNode + " of " + nNodes
              + " (maybe not exported Winlink Messages XML file) : " + e.getLocalizedMessage());
    }

    return messages;
  }

  /**
   * reads a single file (from a clearinghouse), returns a list of ExportedMessage records
   *
   * @param filePath
   * @return
   */
  public List<ExportedMessage> readAll(Path filePath) {
    logger.debug("Processing file: " + filePath.getFileName());

    try {
      var messages = parseExportedMessages(getInputStream(filePath));
      logger.info("extracted " + messages.size() + " exported messages from file: " + filePath.getFileName());
      return messages;
    } catch (Exception e) {
      logger.error("Exception processing file: " + filePath + ", " + e.getLocalizedMessage());
      return new ArrayList<ExportedMessage>();
    }

  }

  private ExportedMessage readMessage(Element element) {

    var messageId = element.getElementsByTagName("id").item(0).getTextContent();
    var subject = element.getElementsByTagName("subject").item(0).getTextContent();
    var dtString = element.getElementsByTagName("time").item(0).getTextContent();
    var sender = element.getElementsByTagName("sender").item(0).getTextContent();
    var mime = element.getElementsByTagName("mime").item(0).getTextContent();

    if (dumpIds.contains(messageId) || dumpIds.contains(sender)) {
      logger.debug("messageId: " + messageId + ", sender: " + sender);
    }

    var locationResult = parseLocation(element);

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

    var parser = AbstractBaseParser.makeMimeMessageParser(mime);
    if (parser == null) {
      message = new ExportedMessage(messageId, sender, recipient, toList, ccList, subject, //
          localDateTime, locationResult.location, locationResult.source, //
          mime, plainContent, attachments);
      return new RejectionMessage(message, RejectType.CANT_PARSE_MIME, message.mime);
    }

    plainContent = parser.getPlainContent();
    attachments = AbstractBaseParser.getAttachments(parser);

    message = new ExportedMessage(messageId, sender, recipient, toList, ccList, subject, //
        localDateTime, locationResult.location, locationResult.source, //
        mime, plainContent, attachments);

    return message;
  }

  /**
   * location may be missing, present as "40.187500N, 92.541667W", or even "40.187500N, 92.541667W (GRID SQUARE)"
   *
   * @param element
   * @return
   */
  private LocationResult parseLocation(Element element) {
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
        }
      }
    } catch (Exception e) {
      ;
    }

    var locationResult = new LocationResult(location, source);
    return locationResult;
  }

  private InputStream getInputStream(Path filePath) throws Exception {
    String content = Files.readString(filePath);

    CharacterAssassinator assassinator = new CharacterAssassinator(deleteList, null);
    content = assassinator.assassinate(content);

    return new ByteArrayInputStream(content.getBytes());
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

    // last ditch
    return addresses.get(0);
  }

  /**
   * semi-generic method to read a CSV file into a list of array of String fields
   *
   * @param inputPath
   * @return
   */
  public static List<String[]> readCsvFileIntoFieldsArray(Path inputPath) {
    var list = new ArrayList<String[]>();

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        list.add(fields);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " records from: " + inputPath.toString());
    return list;
  }
}
