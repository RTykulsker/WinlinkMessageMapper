/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.wimp.web;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.surftools.utils.FileUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IProcessor;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.processors.std.PipelineProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;
import com.surftools.wimp.utils.config.impl.PropertyFileConfigurationManager;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;

/**
 * This is the *main* class for the Web front-end
 *
 * receive a single Winlink "Exported Message" file of messages via upload, return result of running pipeline
 *
 * @author bobt
 *
 */

public class WimpWebServer {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(WimpWebServer.class);

  private IConfigurationManager cm;
  private IMessageManager mm;
  private String pathName;
  private String outputPathName;
  private IProcessor pipeline;

  @Option(name = "--configurationFile", usage = "path to configuration file, default: webConfig.txt", required = false)
  private String configurationFileName = "webConfig.txt";

  public static void main(String[] args) {
    WimpWebServer app = new WimpWebServer();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  /**
   * main processing function
   */
  private void run() {
    try {
      logger.info("begin");

      cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
      mm = new MessageManager();
      pathName = cm.getAsString(Key.PATH);
      outputPathName = cm.getAsString(Key.OUTPUT_PATH);
      if (outputPathName == null) {
        outputPathName = Path.of(pathName, "webOutput").toString();
      }
      FileUtils.makeDirIfNeeded(Path.of(outputPathName));

      final int port = cm.getAsInt(Key.WEB_SERVER_PORT, 3200);
      if (!WebUtils.isPortAvailable(port)) {
        logger.error("Web server port: " + port + " in use, exiting!");
        System.exit(1);
      }

      var app = Javalin.create();
      app.get("/", new InitHandler());
      app.post("/upload", new UploadHandler());
      app.start(port);
      logger.info("Web server started on: " + "http://" + WebUtils.getLocalIPv4Address() + ":" + port);
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  class InitHandler implements Handler {
    private String pageHtml = "";

    InitHandler() {
      try {
        var getHtmlFromFile = false;
        var rawHtml = "";
        if (getHtmlFromFile) {
          var htmlPath = Path.of(pathName, "index.html");
          rawHtml = Files.readString(htmlPath);
        } else {
          rawHtml = WebUtils.makeInitialPageHtml();
        }

        rawHtml = rawHtml.replaceAll("#EN", cm.getAsString(Key.EXERCISE_NAME));
        rawHtml = rawHtml.replaceAll("#ED", cm.getAsString(Key.EXERCISE_DESCRIPTION));
        rawHtml = rawHtml.replaceAll("#OPEN", cm.getAsString(Key.EXERCISE_WINDOW_OPEN));
        rawHtml = rawHtml.replaceAll("#CLOSE", cm.getAsString(Key.EXERCISE_WINDOW_CLOSE));
        pageHtml = rawHtml;
      } catch (Exception e) {
        logger.error("Exception initializing InitHandler: " + e.getLocalizedMessage());
        System.exit(1);
      }

    }

    @Override
    public void handle(Context ctx) throws Exception {
      ctx.html(pageHtml);
      ctx.status(HttpStatus.OK);
    }
  }

  class UploadHandler implements Handler {
    ThreadLocal<Path> senderPath = new ThreadLocal<>();

    @Override
    public void handle(Context ctx) throws Exception {
      var responseText = "";
      var responseStatus = HttpStatus.OK;
      var fileName = ctx.req().getHeader("X_FILENAME");
      var fileContent = ctx.req().getReader().lines().collect(Collectors.joining("\n"));
      var callsign = getExportCallsign(fileContent);
      logger.info("received file: " + fileName + ", from call: " + callsign);
      log_request(ctx, fileContent, fileName, callsign);

      mm = new MessageManager();
      mm.putContextObject("webReqestMessages", fileContent);
      mm.putContextObject("webFileName", fileName);
      pipeline = new PipelineProcessor();
      pipeline.initialize(cm, mm);
      pipeline.process();
      pipeline.postProcess();

      @SuppressWarnings("unchecked")
      var feedbackMap = (Map<String, String>) mm.getContextObject("webOutboundMessage");
      if (feedbackMap == null) {
        responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        responseText = "Server Error";
      } else if (feedbackMap.size() == 0) {
        responseStatus = HttpStatus.BAD_REQUEST;
        responseText = "No exported messages in file: " + fileName + ", perhaps not an exported message file";
      } else {
        var sb = new StringBuilder()
            .append("<div>Received " + mm.getOriginalMessages().size() + " messages exported from " + callsign
                + "<p/></div>\n");

        sb.append("<pre>\n");
        var senders = new ArrayList<String>(feedbackMap.keySet());
        Collections.sort(senders);
        for (var sender : senders) {
          var feedback = feedbackMap.get(sender);
          sb.append("sender: " + sender + "\n");
          sb.append(feedback + "\n\n");
        }
        sb.append("</pre>\n");
        responseText = sb.toString();
      }

      ctx.status(responseStatus);
      ctx.result(responseText);

      log_response(ctx, fileContent, fileName, responseStatus, responseText, callsign);
    }

    private String getExportCallsign(String fileContent) {
      try {
        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var db = dbf.newDocumentBuilder();
        var doc = db.parse(new ByteArrayInputStream(fileContent.getBytes()));
        doc.getDocumentElement().normalize();

        var nodeList = doc.getElementsByTagName("callsign");
        var nNodes = nodeList.getLength();
        for (var iNode = 0; iNode < nNodes; ++iNode) {
          var node = nodeList.item(iNode);
          if (node.getNodeType() == Node.ELEMENT_NODE) {
            var element = (Element) node;
            return element.getTextContent();
          } // end if
        } // end for
      } catch (Exception e) {
        logger.error("Exception processing exported messages : " + e.getLocalizedMessage());
      }
      return "unknown callsign";
    }

    private void log_request(Context ctx, String fileContent, String fileName, String callsign) {
      // outputPath/
      // ____commonLog.txt //
      // ________/<call-sign>/ //
      // ____________<time-stamp> //
      // ________________<request-file-name> //
      // ________________response.txt

      FileUtils.makeDirIfNeeded(Path.of(outputPathName, callsign));

      var senderTimestamp = WebUtils.makeTimestamp();
      var path = FileUtils.makeDirIfNeeded(Path.of(outputPathName, callsign, senderTimestamp));
      senderPath.set(path);
      try {
        Files.writeString(Path.of(path.toString(), fileName), fileContent);
      } catch (Exception e) {
        logger.error("Error writing request from " + callsign + ", " + e.getLocalizedMessage());
      }

    }

    private void log_response(Context ctx, String fileContent, String fileName, HttpStatus responseStatus,
        String responseText, String callsign) {

      try {
        Files.writeString(Path.of(senderPath.get().toString(), "response.txt"), responseText);
      } catch (IOException e) {
        logger.error("Error writing response from " + callsign + ", " + e.getLocalizedMessage());
      }

      var commonLogDTFormatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss xxxx");
      var clTimestamp = commonLogDTFormatter.format(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()));

      var clString = ctx.req().getRemoteHost() + " " + callsign + " - [" + clTimestamp + "] \"POST /upload/" + fileName
          + "\" " + responseStatus.getCode() + " " + responseText.length() + "\n";

      logger.info("Common log: " + clString);

      var clName = Path.of(outputPathName, "commonLog.txt").toString();
      try (var writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(clName, true), "UTF-8"))) {
        writer.write(clString);
      } catch (IOException e) {
        logger.error("Error writing common log from " + callsign + ", " + e.getLocalizedMessage());
      }
    }
  }

}