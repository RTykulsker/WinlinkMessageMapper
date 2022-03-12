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

package com.surftools.winlinkMessageMapper.processor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
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

import com.surftools.winlinkMessageMapper.dto.ExportedMessage;
import com.surftools.winlinkMessageMapper.dto.LatLongPair;

public abstract class AbstractBaseProcessor implements IProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AbstractBaseProcessor.class);
  public static final String[] DEFAULT_LATLON_TAGS = new String[] { "maplat", "gps2", "GPS2" };
  protected static Set<String> dumpIds = new HashSet<>();
  protected static boolean saveAttachments = false;
  protected static Path path = null;

  /**
   * convert strings representing lat/lon like 47-32.23N or 122-14.33W into decimal degree representation
   *
   * @param ddmm
   * @return
   */
  public String convertToDecimalDegrees(String ddmm) {
    char direction = ddmm.charAt(ddmm.length() - 1);
    ddmm = ddmm.substring(0, ddmm.length() - 1);
    String[] fields = ddmm.split("-");
    double degrees = Double.parseDouble(fields[0]);
    double minutes = Double.parseDouble(fields[1]);
    double decimalDegrees = degrees + (minutes / 60d);
    DecimalFormat df = new DecimalFormat("#.#####");
    df.setRoundingMode(RoundingMode.CEILING);
    String result = ((direction == 'S' || direction == 'W') ? "-" : "") + df.format(decimalDegrees);
    return result;
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

  protected MimeMessageParser makeMimeMessageParser(String mimeContent) throws Exception {
    InputStream inputStream = new ByteArrayInputStream(mimeContent.getBytes());
    Session session = Session.getDefaultInstance(new Properties(), null);
    MimeMessage mimeMessage = new MimeMessage(session, inputStream);

    MimeMessageParser parser = new MimeMessageParser(mimeMessage);
    parser.parse();

    return parser;
  }

  /**
   * save ALL attachments for message in output/attachments/<messageId>/<call>-<real-file-name><
   *
   * @param message
   */
  public void saveAttachments(ExportedMessage message) {
    var messageId = message.messageId;
    var call = message.from;
    Path outputPath = Path.of(path.toString(), "output", "attachments", messageId);
    try {
      if (dumpIds.contains(message.messageId) || dumpIds.contains(message.from)) {
        logger.info("saveAttachments: " + message);
      }

      // remove output directory and all contents
      if (Files.exists(outputPath)) {
        Files //
            .walk(outputPath) //
              .map(Path::toFile) //
              .sorted((o1, o2) -> -o1.compareTo(o2)) //
              .forEach(File::delete);
      }

      // create output directory
      File outputDirectory = new File(outputPath.toString());
      outputDirectory.mkdirs();

      // write message body to outputDir
      var parser = makeMimeMessageParser(message.mime);
      var plainContent = parser.getPlainContent();
      var mimePath = Path.of(outputPath.toString(), call + "-plainContent.txt");
      var out = new PrintWriter(mimePath.toString());
      out.println(plainContent);
      out.close();

      // write all attachments to outputDir
      List<DataSource> attachments = parser.getAttachmentList();
      int attachmentIndex = -1;
      for (DataSource ds : attachments) {
        ++attachmentIndex;
        var bads = (ByteArrayDataSource) ds;
        var attachmentName = bads.getName();
        if (attachmentName == null) {
          mimePath = Path.of(outputPath.toString(), call + "-attachment-" + attachmentIndex);
        } else {
          mimePath = Path.of(outputPath.toString(), call + "-" + attachmentName.toLowerCase());
        }
        FileOutputStream fos = new FileOutputStream(mimePath.toString());
        fos.write(bads.getInputStream().readAllBytes());
        fos.close();
      } // end for over attachments
    } catch (

    Exception e) {
      logger.error("Exception saving attachments for messageId: " + messageId + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * find base64-encoded attachment, if any, decode
   *
   * @param mime
   * @return
   */
  public String decodeAttachment(String mime, String key, String from) {
    if (dumpIds.contains(from)) {
      logger.info("decodeAttachment: " + from);
    }

    String s = null;
    try {
      Properties props = new Properties();
      InputStream inputStream = new ByteArrayInputStream(mime.getBytes());
      Session session = Session.getDefaultInstance(props, null);
      MimeMessage mimeMessage = new MimeMessage(session, inputStream);

      // apache
      MimeMessageParser parser = new MimeMessageParser(mimeMessage);
      parser.parse();
      List<DataSource> attachments = parser.getAttachmentList();

      for (DataSource ds : attachments) {
        ByteArrayDataSource bads = (ByteArrayDataSource) ds;
        var attachmentName = bads.getName();
        if (attachmentName != null && attachmentName.contains(key)) {
          s = new String(bads.getInputStream().readAllBytes());
          break;
        }
      }
    } catch (Exception e) {
      logger.error("could not base64 decode string from: " + from + ", " + e.getLocalizedMessage());
    }
    return s;
  }

  public LatLongPair getLatLongFromXml(String xmlString, String[] overrideTags) {
    for (String[] tags : new String[][] { DEFAULT_LATLON_TAGS, overrideTags }) {
      if (tags == null) {
        continue;
      }

      for (String tagName : tags) {
        String s = getStringFromXml(xmlString, tagName);
        if (s == null || s.length() == 0) {
          continue;
        }

        if (s.indexOf(",") >= 0) {
          String[] fields = s.split(",");
          if (fields.length >= 2) {
            LatLongPair pair = new LatLongPair(fields[0], fields[1]);
            if (pair.isValid()) {
              return pair;
            } // end if pair is valid
          } // end if at least two fields after split
        } else {
          if (tagName.endsWith("lat")) {
            String latString = s;
            String newTagName = tagName.replaceAll("lat", "lon");
            String lonString = getStringFromXml(xmlString, newTagName);
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

  public String getStringFromXml(String xmlString, String tagName) {
    xmlString = xmlString.trim().replaceFirst("^([\\W]+)<", "<");

    if (!xmlString.endsWith(">")) {
      xmlString += ">";
    }

    boolean doXmlProcessing = true;
    String s = null;
    if (doXmlProcessing) {
      try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        InputSource inputSource = new InputSource(new StringReader(xmlString));
        Document doc = db.parse(inputSource);
        doc.getDocumentElement().normalize();
        NodeList list = doc.getElementsByTagName(tagName);
        if (list != null && list.getLength() > 0) {
          NodeList subList = list.item(0).getChildNodes();
          if (subList != null && subList.getLength() > 0) {
            s = subList.item(0).getNodeValue();
          }
        }
      } catch (Exception e) {
        logger.error("can't parse xml: " + xmlString + ", " + e.getLocalizedMessage());
        return null;
      }
    } else {
      String beginTag = "<" + tagName + ">";
      int beginIndex = xmlString.indexOf(beginTag);
      if (beginIndex == -1) {
        throw new RuntimeException("couldn't find begin tag: " + beginTag);
      }

      String endTag = "</" + tagName + ">";
      int endIndex = xmlString.indexOf(endTag);
      if (endIndex == -1) {
        throw new RuntimeException("couldn't find end tag: " + endTag);
      }

      s = xmlString.substring(beginIndex + beginTag.length(), endIndex);
      s = s.replaceAll("\r", "");
    }
    return s;
  }

  public static void setDumpIds(Set<String> _dumpIds) {
    if (dumpIds != null) {
      dumpIds = _dumpIds;
    }
  }

  public static void setSaveAttachments(boolean _saveAttachments) {
    saveAttachments = _saveAttachments;
  }

  public static void setPath(Path _path) {
    path = _path;
  }
}
