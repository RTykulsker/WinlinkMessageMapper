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

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.processors.std.PipelineProcessor;
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

      var cm = new PropertyFileConfigurationManager(configurationFileName, Key.values());
      var mm = new MessageManager();

      // TODO see if we can instantiate and initialize once, then call pipeline.process() once for each web request
      var pipeline = new PipelineProcessor();
      pipeline.initialize(cm, mm);
      pipeline.process();
      pipeline.postProcess();

      final int port = cm.getAsInt(Key.WEB_SERVER_PORT, 3200);
      if (!WebUtils.isPortAvailable(port)) {
        logger.error("Web server port: " + port + " in use, exiting!");
        System.exit(1);
      }

      final String ipAddress = WebUtils.getLocalIPv4Address();
      final String serverUrl = "http://" + ipAddress + ":" + port;
      logger.info("listening on port: " + serverUrl);

      var app = Javalin.create();
      app.get("/status", new StatusHandler());
      app.post("upload", new UploadHandler());
      app.start(port);
      logger.info("started on port: " + port);
    } catch (Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
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
      // https://javalin.io/tutorials/html-forms-example

      // String viewContent = null;
      // String requestPath = ctx.req().getPathInfo();
      // if (requestPath.equals(FILE_UPLOAD_URL)) {
      // logger.info("requestPath: " + requestPath);
      //
      // // handle form upload; gets placed into a multipart ...
      // MultipartConfigElement multipartConfigElement = new MultipartConfigElement("");
      // request.raw().setAttribute("org.eclipse.jetty.multipartConfig", multipartConfigElement);
      // Part file = request.raw().getPart("file"); // file is name of the upload form
      //
      // if (file == null) {
      // Utils.warn(cm, ConfigurationKey.EMSG_NO_UPLOAD_FILE, null);
      // response.status(401);
      // return cm.getAsString(ConfigurationKey.EMSG_NO_UPLOAD_FILE);
      // }
      //
      // InputStream is = file.getInputStream();
      // viewContent = new BufferedReader(new InputStreamReader(is)).lines().collect(Collectors.joining("\n"));
      // } else if (requestPath.equals(XHR_UPLOAD_URL)) {
      // viewContent = ctx.req().body();
      // }
      //
      // String fileName = request.headers("X_FILENAME");
      // logger.info("receivedfilename: " + fileName + ", " + viewContent.length() + " bytes");
      //
      // if (viewContent == null || viewContent.length() == 0) {
      // Utils.warn(cm, ConfigurationKey.EMSG_NO_UPLOAD_FILE, null);
      // response.status(401);
      // return cm.getAsString(ConfigurationKey.EMSG_NO_UPLOAD_FILE);
      // }
      //
      // FormResults results = generateResults(viewContent);
      // serverLogger.info(commonLogFormat(request, results.displayFormName, results));
      // response.status(results.responseCode);
      // return results.resultString;
      // }
    }
  }
}