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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private String pathString;

  private IProcessor pipeline;

  @Option(name = "--configurationFile", usage = "path to configuration file", required = false)
  private String configurationFileName = "configuration.txt";

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
      pathString = cm.getAsString(Key.PATH);

      // TODO see if we can instantiate and initialize once, then call pipeline.process() once for each web request
      pipeline = new PipelineProcessor();
      pipeline.initialize(cm, mm);
      // pipeline.process();
      // pipeline.postProcess();

      final int port = cm.getAsInt(Key.WEB_SERVER_PORT, 3200);
      if (!WebUtils.isPortAvailable(port)) {
        logger.error("Web server port: " + port + " in use, exiting!");
        System.exit(1);
      }

      final String ipAddress = WebUtils.getLocalIPv4Address();
      final String serverUrl = "http://" + ipAddress + ":" + port;
      logger.info("listening on port: " + serverUrl);

      var uploadHandler = new UploadHandler();
      var app = Javalin.create();
      app.get("/", new InitHandler());
      app.get("/status", new StatusHandler());
      app.post("/upload", uploadHandler);
      app.post("/uploadXHR", uploadHandler);
      app.start(port);
      logger.info("started on port: " + port);
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  class InitHandler implements Handler {
    private String pageHtml = "";

    InitHandler() {
      try {
        var htmlPath = Path.of(pathString, "index.html");
        var rawHtml = Files.readString(htmlPath);

        // TODO fixup page
        pageHtml = rawHtml;
      } catch (IOException e) {
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

  class StatusHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      ctx.result("alive");
      ctx.status(HttpStatus.OK);
    }
  }

  class UploadHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
      System.err.println("upload!");
      // TODO get export sender from line 6: <callsign>KM6SO</callsign>
      // TODO write to common log

      // https://javalin.io/tutorials/html-forms-example

      var req = ctx.req();
      var requestPath = req.getPathInfo();
      var fileContent = "";

      String fileName = req.getHeader("X_FILENAME");
      logger.info("receivedfilename: " + fileName);

      if (requestPath.equals("/uploadXHR")) {
        var reader = req.getReader();
        String text = reader.lines().collect(Collectors.joining("\n"));
        fileContent = text;
      } else {
        var map = ctx.uploadedFileMap();
        for (var fileNameX : map.keySet()) {
          var list = map.get(fileNameX);
          var uploadedFile = list.get(0);
          logger.info(uploadedFile.toString());
        }
      }

      mm.putContextObject("webReqestMessages", fileContent);
      pipeline.process();
      pipeline.postProcess();

      @SuppressWarnings("unchecked")
      var webMessageMap = (Map<String, String>) mm.getContextObject("webOutboundMessage");
      if (webMessageMap == null) {
        ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
        ctx.result("null map");
      } else {
        var stringOutput = String.join("\n", webMessageMap.values());
        ctx.result(stringOutput);
      }
    }
  }

  // private String commonLogFormat(Request request, String displayFormName, FormResults results) {
  // // [10/Oct/2000:13:55:36 -0700]
  // DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss xxxx");
  // String timeString = formatter.format(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()));
  //
  // StringBuilder sb = new StringBuilder();
  // sb.append(request.ip());
  // sb.append(" - - [");
  // sb.append(timeString);
  // sb.append("] ");
  // sb.append("\"POST" + request.pathInfo() + "/" + displayFormName + "\"");
  // sb.append(" " + results.responseCode + " ");
  // sb.append(results.resultString.length());
  // return sb.toString();
  // }
}