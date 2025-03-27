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

package com.surftools.wimp.parser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.activation.DataSource;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.mail.util.MimeMessageParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IParser;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.RejectionMessage;
import com.surftools.wimp.utils.config.IConfigurationManager;

public abstract class AbstractBaseParser implements IParser {
  private static final Logger logger = LoggerFactory.getLogger(AbstractBaseParser.class);
  public static final String[] DEFAULT_LATLON_TAGS = new String[] { "maplat", "gps2", "GPS2", "gpslat" };

  public static final String DATA_SOURCE_RMS_VIEWER = "RmsViewer.xml";
  public static final String DATA_SOURCE_FORM_DATA = "FormData.txt";

  protected Set<String> filterIds = new HashSet<>();
  protected IConfigurationManager cm;
  protected IMessageManager mm;

  protected Document currentDocument = null;
  protected String currentMessageId = null;

  @SuppressWarnings("unchecked")
  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    this.cm = cm;
    this.mm = mm;

    filterIds = (Set<String>) mm.getContextObject("filterIds");
  }

  /**
   * convenience method for handling rejections
   *
   * @param message
   * @param reason
   * @param context
   * @return
   */
  protected ExportedMessage reject(ExportedMessage message, RejectType reason, String context) {
    return new RejectionMessage(message, reason, context);
  }

  public String getValueFromMime(String[] mimeLines, String key) {
    for (String line : mimeLines) {
      if (line.startsWith(key)) {
        var value = line.substring(key.length());
        return value.trim();
      }
    }
    return "";
  }

  public static MimeMessageParser makeMimeMessageParser(String messageId, String mimeContent) {
    try {

      // ETO_2024_09_19: some people send in messages with this:
      // Content-Disposition: attachment; name=""ETO Welfare Message //
      // Exercise".pdf"; filename=""ETO Welfare Message Exercise".pdf"//
      // Content-Type: text/plain; name=""ETO Welfare Message Exercise".pdf"//
      // Content-Transfer-Encoding: base64//
      var doHack_ETO_2024_09_19 = true;
      if (doHack_ETO_2024_09_19) {
        if (mimeContent == null) {
          return null;
        }
        var lines = mimeContent.split("\n");
        var newLines = new ArrayList<String>();
        var inBadArea = false;
        for (var line : lines) {
          if (!inBadArea && line.startsWith("Content-Disposition: attachment;") && line.contains("\"\"")) {
            inBadArea = true;
          }
          if (inBadArea && line.isBlank()) {
            inBadArea = false;
          }
          if (inBadArea) {
            line = line.replaceAll("\"\"", "\"");
            line = line.replaceAll("\".pdf", ".pdf");
          }
          newLines.add(line);
        } // end for over lines
        mimeContent = String.join("\n", newLines);
      }

      InputStream inputStream = new ByteArrayInputStream(mimeContent.getBytes());
      Session session = Session.getDefaultInstance(new Properties(), null);
      MimeMessage mimeMessage = new MimeMessage(session, inputStream);

      MimeMessageParser parser = new MimeMessageParser(mimeMessage);
      parser.parse();

      return parser;
    } catch (Exception e) {
      logger.error("could not parse mime: " + e.getLocalizedMessage() + ", for messageId: " + messageId);
      return null;
    }
  }

  public static Map<String, byte[]> getAttachments(MimeMessageParser parser) {
    Map<String, byte[]> attachments = new LinkedHashMap<>();
    List<DataSource> dataSources = parser.getAttachmentList();
    int attachmentIndex = -1;
    for (DataSource ds : dataSources) {
      ++attachmentIndex;
      var bads = (ByteArrayDataSource) ds;
      var attachmentName = bads.getName();
      if (attachmentName == null || attachmentName.length() == 0) {
        attachmentName = "attachment-" + attachmentIndex;
      }
      try {
        var bytes = bads.getInputStream().readAllBytes();
        attachments.put(attachmentName, bytes);
      } catch (Exception e) {
        logger
            .error("could not read attachment #: " + attachmentIndex + ", name: " + attachmentName + ", "
                + e.getLocalizedMessage());
      }
    }
    return attachments;
  }

  public LatLongPair getLatLongFromXml(String[] overrideTags) {
    for (String[] tags : new String[][] { DEFAULT_LATLON_TAGS, overrideTags }) {
      if (tags == null) {
        continue;
      }

      for (String tagName : tags) {
        String s = getStringFromXml(tagName);
        if (s == null || s.length() == 0) {
          continue;
        }

        if (s.indexOf(",") >= 0) {
          String[] fields = s.split(",");
          if (fields.length >= 2) {
            LatLongPair pair = new LatLongPair(fields[0].trim(), fields[1].trim());
            if (pair.isValid()) {
              return pair;
            } // end if pair is valid
          } // end if at least two fields after split
        } else {
          if (tagName.endsWith("lat")) {
            String latString = s;
            String newTagName = tagName.replaceAll("lat", "lon");
            String lonString = getStringFromXml(newTagName);
            LatLongPair pair = new LatLongPair(latString, lonString);
            if (pair.isValid()) {
              return pair;
            } // end if a valid pair
          } // end if tagName ends with lat
        } // end if doesn't contain comma, ie lat,lon
      } // end for over set of tagNames
    } // end for over sets of tagNames, default and overrides

    return null;
  }

  public String getStringFromFormLines(String[] formLines, String tagName) {
    return getStringFromFormLines(formLines, "=", tagName);
  }

  public String getStringFromFormLines(String[] formLines, String delimiter, String tagName) {
    for (String line : formLines) {
      line = line.trim();
      if (line.startsWith(tagName)) {
        int index = line.indexOf(delimiter);
        if (index == -1) {
          return null;
        }
        return line.substring(index + delimiter.length());
      }
    }
    return null;
  }

  public void makeDocument(String messageId, String xmlString) {
    makeDocument(messageId, xmlString, true);
  }

  public void makeDocument(String messageId, String xmlString, boolean removeParseme) {
    xmlString = xmlString.trim().replaceFirst("^([\\W]+)<", "<");

    // I don't remember why I generally DON'T want parsme, //
    // but I NEED it for EyeWarnMessage
    if (removeParseme) {
      xmlString = removeBetween(xmlString, "<parseme>", "</parseme>");
    }

    if (!xmlString.endsWith(">")) {
      xmlString += ">";
    }

    // [Fatal Error] :16:31: Character reference "&#21" is an invalid XML character.
    xmlString = xmlString.replaceAll("&#21", "_");

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      InputSource inputSource = new InputSource(new StringReader(xmlString));
      Document doc = db.parse(inputSource);
      doc.getDocumentElement().normalize();
      currentDocument = doc;
      currentMessageId = messageId;
    } catch (Exception e) {
      logger.error("can't parse xml: " + xmlString + ", " + e.getLocalizedMessage());
      throw new RuntimeException("can't parse xml: " + e.getLocalizedMessage());
    }
  }

  /**
   * return string with content between start of beginString and end of endString removed
   *
   * @param source
   * @param beginString
   * @param endString
   * @return
   */
  protected String removeBetween(String source, String beginString, String endString) {
    if (source == null || beginString == null || endString == null) {
      return source;
    }

    var beginIndex = source.indexOf(beginString);
    var endIndex = source.indexOf(endString);
    if (beginIndex == -1 || endIndex == -1) {
      return source;
    }

    var startString = source.substring(0, beginIndex);
    var finishString = source.substring(endIndex + endString.length());
    var ret = startString + finishString;

    return ret;
  }

  public String getStringFromXml(String tagName) {
    String s = null;
    NodeList list = currentDocument.getElementsByTagName(tagName);
    if (list != null && list.getLength() > 0) {
      NodeList subList = list.item(0).getChildNodes();
      if (subList != null && subList.getLength() > 0) {
        s = subList.item(0).getNodeValue();
      }
    }

    if (s == null && Character.isLowerCase(tagName.charAt(0))) {
      tagName = tagName.substring(0, 1).toUpperCase() + tagName.substring(1);
      list = currentDocument.getElementsByTagName(tagName);
      if (list != null && list.getLength() > 0) {
        NodeList subList = list.item(0).getChildNodes();
        if (subList != null && subList.getLength() > 0) {
          s = subList.item(0).getNodeValue();
        }
      }
    }

    if (s != null) {
      s = s.trim();
    } else {
      s = "";
    }

    return s;
  }

  /**
   * get first line from a multi-line string
   *
   * @param first
   *          (possibly empty) line
   */
  public String getFirstLineOf(String multiLine) {
    if (multiLine == null) {
      return "";
    }

    var firstLine = "";
    if (multiLine != null) {
      String[] lines = multiLine.split("\n");
      if (lines != null && lines.length > 0) {
        firstLine = lines[0];
      }
    }

    return firstLine;
  }

  protected String dumpXmlMap() {
    var map = getXmlValueMap();
    var sb = new StringBuilder();

    for (var name : map.keySet()) {
      var value = getStringFromXml(name);
      sb.append("name: " + name + " => value: " + value + "\n");
    }

    return sb.toString();
  }

  protected Map<String, String> getXmlValueMap() {
    var map = new LinkedHashMap<String, String>();
    var variableNames = new ArrayList<String>();
    var level1Node = currentDocument.getChildNodes().item(0);
    var level1List = level1Node.getChildNodes();
    var nLevel1 = level1List.getLength();
    for (int iLevel1 = 0; iLevel1 < nLevel1; ++iLevel1) {
      var level2Node = level1List.item(iLevel1);
      var level2List = level2Node.getChildNodes();
      var nLevel2 = level2List.getLength();
      for (int iLevel2 = 0; iLevel2 < nLevel2; ++iLevel2) {
        var level3Node = level2List.item(iLevel2);
        var level3List = level3Node.getChildNodes();
        var nLevel3 = level3List.getLength();
        for (int iLevel3 = 0; iLevel3 < nLevel3; ++iLevel3) {

          var name = level2List.item(iLevel2).getNodeName();
          if (name != null) {
            variableNames.add(name);
          }
        }
      }
    }

    for (var name : variableNames) {
      map.put(name, getStringFromXml(name));
    }
    return map;
  }

  protected String getVersion(String tagName) {
    var version = "";
    var templateVersion = getStringFromXml(tagName);
    if (templateVersion != null) {
      var fields = templateVersion.split(" ");
      version = fields[fields.length - 1]; // last field
    }

    return version;
  }
}
