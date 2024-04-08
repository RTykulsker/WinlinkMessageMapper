/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

package com.surftools.wimp.service.cms;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.surftools.wimp.core.IWritableTable;

/**
 * response for TrafficLogsSenderGet and TrafficLogsSourceGet
 *
 * for reasons unknown, the TrafficLogsXXX documentation has disappeared from the web site and the result does not
 * include mode (aka session type)
 */
public record TrafficRecord( //
    LocalDateTime dateTime, //
    String site, //
    String event, //
    String messageId, //
    int clientType, //
    String callsign, //
    String gateway, //
    String source, //
    String sender, //
    String subject, //
    int size, //
    int attachments, //
    int frequency) implements IWritableTable {

  final static String DT_FORMAT_STRING = "yyyy-MM-dd HH:mm";
  final static DateTimeFormatter DTF = DateTimeFormatter.ofPattern(DT_FORMAT_STRING);
  @Override
  public String[] getHeaders() {
    return new String[] { //
        "DateTime", //
        "Site", //
        "Event", //
        "MessageId", //
        "ClientType", //
        "Callsign", //
        "Gateway", //
        "Source", //
        "Sender", //
        "Subject", //
        "Size", //
        "Attachments", //
        "Frequency" };
  };

  @Override
  public String[] getValues() {
    return new String[] { //
        DTF.format(dateTime), //
        site, //
        event, //
        messageId, //
        String.valueOf(clientType), //
        callsign, //
        gateway, //
        source, //
        sender, //
        subject, //
        String.valueOf(size), //
        String.valueOf(attachments), //
        String.valueOf(frequency) };
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (TrafficRecord) other;
    return dateTime.compareTo(o.dateTime);
  }

  public static TrafficRecord fromFields(String[] f) {
    return new TrafficRecord( //
        LocalDateTime.parse(f[0], DTF), //
        f[1], //
        f[2], //
        f[3], //
        Integer.parseInt(f[4]), //
        f[5], //
        f[6], //
        f[7], //
        f[8], //
        f[9], //
        Integer.valueOf(f[10]), //
        Integer.valueOf(f[11]), //
        Integer.valueOf(f[12]));
  }

}
