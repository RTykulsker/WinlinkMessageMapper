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

package com.surftools.winlinkMessageMapper.tool;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.processor.CharacterAssassinator;

public class ExportedMessageReader {
  private static final Logger logger = LoggerFactory.getLogger(ExportedMessageReader.class);

  private static final List<String> DEFAULT_DELETE_LIST = Arrays.asList(new String[] { "&#21" });
  private final List<String> deleteList;

  private Set<String> preferredToPrefixSet = new HashSet<>();
  private Set<String> preferredToSuffixSet = new HashSet<>();
  private Set<String> notPreferredToPrefixSet = new HashSet<>();
  private Set<String> notPreferredToSuffixSet = new HashSet<>();

  private Set<String> dumpIds = new HashSet<>();

  public ExportedMessageReader() {
    this(DEFAULT_DELETE_LIST);
  }

  public ExportedMessageReader(List<String> deleteList) {
    this.deleteList = deleteList;
  }

  private InputStream getInputStream(Path filePath) throws Exception {
    String content = Files.readString(filePath);

    CharacterAssassinator assassinator = new CharacterAssassinator(deleteList, null);
    content = assassinator.assassinate(content);

    return new ByteArrayInputStream(content.getBytes());
  }

  public List<ExportedMessage> extractAll(Path filePath) {
    logger.info("Processing file: " + filePath.getFileName());
    List<ExportedMessage> messages = new ArrayList<>();

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(getInputStream(filePath));
      doc.getDocumentElement().normalize();
      NodeList nodeList = doc.getElementsByTagName("message");
      int nNodes = nodeList.getLength();
      for (int i = 0; i < nNodes; ++i) {
        Node node = nodeList.item(i);
        if (node.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) node;
          ExportedMessage exportedMessage = extractMessage(element);
          if (exportedMessage != null) {
            messages.add(exportedMessage);
          }
        } // end if XML Message Node
      } // end for over messages
    } catch (Exception e) {
      logger
          .error("Exception processing file: " + filePath.getFileName()
              + " (maybe not exported Winlink Messages XML file) : " + e.getLocalizedMessage());
    }

    logger.info("extracted " + messages.size() + " exported messages from file: " + filePath.getFileName());
    return messages;
  }

  private ExportedMessage extractMessage(Element element) {
    var messageId = element.getElementsByTagName("id").item(0).getTextContent();
    var subject = element.getElementsByTagName("subject").item(0).getTextContent();
    var dtString = element.getElementsByTagName("time").item(0).getTextContent();
    var sender = element.getElementsByTagName("sender").item(0).getTextContent();
    var mime = element.getElementsByTagName("mime").item(0).getTextContent();

    String[] dtFields = dtString.split(" ");
    var dateString = dtFields[0];
    var timeString = dtFields[1];
    String[] mimeLines = mime.split("\\n");

    if (dumpIds.contains(messageId) || dumpIds.contains(sender)) {
      logger.info("exportedMessage: " + "{messageId: " + messageId + ", sender: " + sender + "}");
    }

    String recipient = getRecipient(mimeLines);

    ExportedMessage message = new ExportedMessage(messageId, sender, recipient, subject, dateString, timeString, mime);
    return message;
  }

  // Subject: DYFI Automatic Entry - Winlink EXERCISE
  // To: SMTP:dyfi_reports_automated@usgs.gov,
  // ETO-02@winlink.org
  // Cc: KC3DOW@winlink.org,
  // W3IHP@winlink.org,
  // SMTP:kc3dow@kanidor.com,
  // SMTP:w3ihp@jmbventures.com
  // Message-ID: Z4VVKSSM1GII

  public String getRecipient(String[] mimeLines) {

    List<String> addresses = new ArrayList<>();

    for (String keyWord : new String[] { "To: ", "Cc: " }) {
      boolean isFound = false;
      for (String line : mimeLines) {
        if (line.startsWith("Message-ID: ")) {
          break;
        }
        if (line.startsWith(keyWord)) {
          isFound = true;
        }
        if (isFound) {
          line = pre_fixAddress(line);
          if (line != null) {
            addresses.add(pre_fixAddress(line));
          }
        } else {
          continue;
        }
      } // end for over lines
    } // end for over keyWords

    if (addresses.size() == 0) {
      return null;
    }

    logger.debug("found " + addresses.size() + " addresses: " + String.join(", ", addresses));

    String address = chooseBestAddress(addresses);
    address = post_fix(address);
    return address;
  } // end getRecipient

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
    String address = null;

    // 1st pass: find first address that is in preferred prefix AND suffix AND not in both not sets
    for (String test : addresses) {
      var lc = test.toLowerCase();
      if ((startsWith(lc, preferredToPrefixSet) //
          && endsWith(lc, preferredToSuffixSet)) //
          && (!startsWith(lc, notPreferredToPrefixSet) //
              && !endsWith(lc, notPreferredToSuffixSet))) {
        address = test;
        return address;
      }
    }

    // 2nd pass: find first address that is in preferred prefix OR suffix AND not in both not sets
    for (String test : addresses) {
      var lc = test.toLowerCase();
      if ((startsWith(lc, preferredToPrefixSet) //
          || endsWith(lc, preferredToSuffixSet)) //
          && (!startsWith(lc, notPreferredToPrefixSet) //
              && !endsWith(lc, notPreferredToSuffixSet))) {
        address = test;
        return address;
      }
    }

    // 3rd pass: find first address that is in preferred prefix AND suffix
    for (String test : addresses) {
      var lc = test.toLowerCase();
      if (startsWith(lc, preferredToPrefixSet) //
          && endsWith(lc, preferredToSuffixSet)) {//
        address = test;
        return address;
      }
    }

    // 4th pass: find first address that is in preferred prefix OR suffix
    for (String test : addresses) {
      var lc = test.toLowerCase();
      if (startsWith(lc, preferredToPrefixSet) //
          || endsWith(lc, preferredToSuffixSet)) {//
        address = test;
        return address;
      }
    }

    // 5th pass: find first address that is not in both not sets
    for (String test : addresses) {
      var lc = test.toLowerCase();
      if (!startsWith(lc, notPreferredToPrefixSet) //
          && !endsWith(lc, notPreferredToSuffixSet)) {//
        address = test;
        return address;
      }
    }

    // 6th pass: find first address that is not in either not sets
    for (String test : addresses) {
      var lc = test.toLowerCase();
      if (!startsWith(lc, notPreferredToPrefixSet) //
          || !endsWith(lc, notPreferredToSuffixSet)) {//
        address = test;
        return address;
      }
    }

    // last ditch
    return addresses.get(0);
  }

  private boolean startsWith(String needle, Set<String> hayStack) {
    for (String s : hayStack) {
      if (needle.toLowerCase().startsWith(s)) {
        return true;
      }
    }
    return false;
  }

  private boolean endsWith(String needle, Set<String> hayStack) {
    for (String s : hayStack) {
      if (needle.toLowerCase().endsWith(s)) {
        return true;
      }
    }
    return false;
  }

  public void setPreferredPrefixes(String string) {
    if (string != null) {
      preferredToPrefixSet.addAll(Arrays.asList(string.toLowerCase().split(",")));
    }
  }

  public void setPreferredSuffixes(String string) {
    if (string != null) {
      preferredToSuffixSet.addAll(Arrays.asList(string.toLowerCase().split(",")));
    }
  }

  public void setNotPreferredPrefixes(String string) {
    if (string != null) {
      notPreferredToPrefixSet.addAll(Arrays.asList(string.toLowerCase().split(",")));
    }
  }

  public void setNotPreferredSuffixes(String string) {
    if (string != null) {
      notPreferredToSuffixSet.addAll(Arrays.asList(string.toLowerCase().split(",")));
    }
  }

  public void setDumpIds(Set<String> dumpIds) {
    if (dumpIds != null) {
      this.dumpIds = dumpIds;
    }
  }
}
