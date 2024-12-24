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
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Map;

public class WebUtils {

  public static boolean isPortAvailable(int port) {
    try (@SuppressWarnings("unused")
    Socket ignored = new Socket("localhost", port)) {
      return false;
    } catch (IOException ignored) {
      return true;
    }
  }

  public static String getLocalIPv4Address() {
    String ip = "127.0.0.1";
    try {
      Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface iface = interfaces.nextElement();
        // filters out 127.0.0.1 and inactive interfaces
        if (iface.isLoopback() || !iface.isUp()) {
          continue;
        }

        Enumeration<InetAddress> addresses = iface.getInetAddresses();
        while (addresses.hasMoreElements()) {
          InetAddress addr = addresses.nextElement();

          if (addr instanceof Inet6Address) {
            continue;
          }

          ip = addr.getHostAddress();
        }
      }
    } catch (SocketException e) {
      throw new RuntimeException(e);
    }
    return ip;
  }

  /**
   * return a String representing a LocalDateTime string
   *
   * @return
   */
  public static String makeTimestamp() {
    final LocalDateTime now = LocalDateTime.now();
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    final String timestamp = now.format(formatter);
    return timestamp;
  }

  /**
   * convert map of key->value to a single String key1=value1[&key2=value2...]
   *
   * @param formData
   * @return
   */
  public static String getFormDataAsString(Map<String, String> formData) {
    var sb = new StringBuilder();
    for (var entry : formData.entrySet()) {
      if (sb.length() > 0) {
        sb.append("&");
      }
      sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
      sb.append("=");
      sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
    }
    return sb.toString();
  }

  public static String makeInitialPageHtml() {
    var s = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
        <meta charset="UTF-8" />
        <title>Winlink Exported Message Feedback Preview</title>

        <style>

        body {
          font-family: "Segoe UI", Tahoma, Helvetica, freesans, sans-serif;
          font-size: 100%;
          margin: 10px;
          color: #333;
          background-color: #fff;
        }

        h1, h2 {
          font-size: 1.5em;
          font-weight: normal;
        }

        h2 {
          font-size: 1.3em;
        }

        legend {
          font-weight: bold;
          color: #333;
        }

        #filedrag {
          display: none;
          font-weight: bold;
          text-align: center;
          padding: 1em 0;
          margin: 1em 0;
          color: #555;
          border: 2px dashed #555;
          border-radius: 7px;
          cursor: default;
          flex: 1;
        }

        #filedrag.hover {
          color: #f00;
          border-color: #f00;
          border-style: solid;
          box-shadow: inset 0 3px 4px #888;
        }

        .contentContainer {
          margin: 0 auto;
        }

        .flex-container {
          display: flex;
          align-items: center;
        }

        .flex-left {
          flex: 1;
        }

        .flex-right {
          flex: 1;
        }

        </style>

        </head>
        <body>

        <div id="headBlock">
        <h1>Winlink Exported Message Processor Feedback Preview (FP)</h1>

        <div>
        <h2>Purpose</h2>
        FP provides <em>feedback</em> on one or more Winlink messages. The feeback is based on a specific exercise plan. This is the <bold>same</bold> feedback that is provided after the exercise via maps and Winlink feedback messages, but this service previews the feedback as the exercise is running.
        <p/>

        </div>

        <div>
        <h2>Warnings and Caveats</h2>
        This is beta software. It may not work. The web site may not be available. The feedback may change. Blah, blah, blah!
        This feedback is <bold>only</bold> valid for the #EN exercise, #ED, with a messages submission window from #OPEN to #CLOSE.
        </div>

        <div>
        <h2>Usage</h2>
        To use, export one or more messages from your Outbox, Inbox, Sent Items, etc to a file.
        Then upload that Exported Messages file on this page, using either the [Choose File] button or just
        "drag and drop".
        </div>

        <p/>
        <form id="upload" action="upload" method="POST" enctype="multipart/form-data">

        <fieldset>
        <legend>Exported Message File Upload</legend>

        <input type="hidden" id="MAX_FILE_SIZE" name="MAX_FILE_SIZE" value="300000" />

        <div class="flex-container">
          <div class="flex-left">
            <label for="fileselect">View file to upload:</label>
            <input type="file" id="fileselect" name="fileselect[]" />
          </div>
          <div id="filedrag" class="flex-right">or drop a single file here</div>
        </div>

        <div id="submitbutton">
          <button type="submit">Upload Files</button>
        </div>

        </fieldset>

        </form>

        <hr>
        </div>

        <div id="viewContent" class="contentContainer"/>

        <script>
        (function() {
          // getElementById
          function $id(id) {
            return document.getElementById(id);
          }

          // file drag hover
          function FileDragHover(e) {
            e.stopPropagation();
            e.preventDefault();
            e.target.className = (e.type == "dragover" ? "hover" : "");
          }

          // file selection
          function FileSelectHandler(e) {
            // cancel event and hover styling
            FileDragHover(e);

            // fetch FileList object
            var files = e.target.files || e.dataTransfer.files;

            if (files.length > 1) {
              alert("Please only drop one Winlink \\"exported messages\\" XML file");
              return;
            }

            // process all File objects
            for (var i = 0, f; f = files[i]; i++) {
              UploadFile(f);
            }
          }

          // upload files
          function UploadFile(file) {
              var maxFileSize = 30000000;
              if (file.size > maxFileSize) {
                  alert("File too big, max size: " + maxFileSize + " bytes");
                  return;
              }
            var xhr = new XMLHttpRequest();
            if (xhr.upload) {
              xhr.onreadystatechange = function(e) {
                if (xhr.readyState == 4) {
                  // document.write(xhr.response);
                  $id("viewContent").innerHTML=xhr.response;
                }
                if (xhr.status != 200) {
                  $id("viewContent").style.color = "red";
                } else {
                  $id("viewContent").style.color = "black";
                }
              };

              // start upload
              xhr.open("POST", $id("upload").action, true);
              xhr.setRequestHeader("X_FILENAME", file.name);
              xhr.send(file);
            }
          }

          function Init() {
            var fileselect = $id("fileselect"),
              filedrag = $id("filedrag"),
              submitbutton = $id("submitbutton");

            // file select
            fileselect.addEventListener("change", FileSelectHandler, false);

            // is XHR2 available?
            var xhr = new XMLHttpRequest();
            if (xhr.upload) {
              // file drop
              filedrag.addEventListener("dragover", FileDragHover, false);
              filedrag.addEventListener("dragleave", FileDragHover, false);
              filedrag.addEventListener("drop", FileSelectHandler, false);
              filedrag.style.display = "block";

              // remove submit button
              submitbutton.style.display = "none";
            }
          }

          // call initialization file
          if (window.File && window.FileList && window.FileReader) {
            Init();
          }

        })();
        </script>
        </body>
        </html>
                """;
    return s;
  }
}
