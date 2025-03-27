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

import java.util.ArrayList;

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.PegelstandMessage;

public class PegelstandParser extends AbstractBaseParser {
  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {
      var mime = message.plainContent;
      var lines = mime.split("\n");

      var formDateTime = lineOf("Date Time ", lines);
      var reportStatus = lineOf("01. Report Status: ", lines);

      var sender = lineOf("03. Rufzeichen des Absenders: ", lines);
      var senderIsObserver = lineOf("02. Ist der Absender auch gleichzeitig der Beobachter? ", lines);

      var observerEmail = lineOf("04. Beobachter Email: ", lines);
      var observerPhone = lineOf("05. Beobachter Telefonnummer: ", lines);

      var city = lineOf("06. Stadt: ", lines);
      var region = lineOf("07. Region:", lines);
      var stateOfFederal = lineOf("08. Bundesland: ", lines);
      var state = lineOf("09. Staat: ", lines);

      var formLatitude = "";
      var formLongitude = "";
      var latLonLine = fieldOf("Coordinates: ", lines);

      if (latLonLine != null) {
        var fields = latLonLine.split(" ");
        if (fields.length >= 5) {
          formLatitude = fields[2];
          formLongitude = fields[5];
        }
      }

      var formMGRS = lineOf("MGRS ", lines);
      var formGrid = "";

      var measuredValues = lineOf("10. Messwerterhebung: ", lines);
      var measurementLocationNumber = lineOf("11. Standort der Messung: ", lines);

      var speed = fieldsOf("12. Geschwindigkeit: ", lines, 0);
      var speedUnits = fieldsOf("12. Geschwindigkeit: ", lines, 2);
      var volume = fieldsOf("13. Volumen/Zeit: ", lines, 0);
      var volumeUnits = fieldsOf("13. Volumen/Zeit: ", lines, 2);
      var trend = lineOf("14. Tendenz: ", lines);

      var water = fieldsOf("15. Pegel: ", lines, 0);
      var waterUnits = fieldsOf("15. Pegel: ", lines, 1);

      var comments = linesBetween("Event Comments:", "------------------------------------", lines);

      var formVersion = lineOf("Senders Template Version: ", lines);
      if (formVersion != null) {
        var fields = formVersion.split(" ");
        if (fields.length >= 3) {
          formVersion = fields[3];
        }
      }

      PegelstandMessage m = new PegelstandMessage(message, //
          formDateTime, reportStatus, //
          sender, senderIsObserver, //
          observerEmail, observerPhone, //
          city, region, stateOfFederal, //
          state, //
          formLatitude, formLongitude, formMGRS, formGrid, //
          measuredValues, measurementLocationNumber, //
          speed, speedUnits, volume, volumeUnits, trend, //
          water, waterUnits, //
          comments, //
          formVersion);
      return m;

    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  @Override
  public MessageType getMessageType() {
    return MessageType.PEGELSTAND;
  }

  private String fieldOf(String key, String[] lines) {
    for (var line : lines) {
      if (line.startsWith(key)) {
        return line;
      }
    }

    return null;
  }

  private String linesBetween(String startLine, String stopLine, String[] lines) {
    var list = new ArrayList<String>();
    var found = false;
    for (var line : lines) {
      if (!found) {
        if (line.equals(startLine)) {
          found = true;
          continue;
        }
      }

      if (found) {
        if (line.equals(stopLine)) {
          break;
        }

        list.add(line);
      }
    }

    if (list.size() > 0) {
      list.remove(list.size() - 1);
      var ret = String.join("\n", list);
      return ret;
    } else {
      return null;
    }
  }

  private String fieldsOf(String key, String[] lines, int index) {
    for (var line : lines) {
      if (line.startsWith(key)) {
        var fields = line.substring(key.length()).split(" ");
        if (fields.length >= index + 1) {
          return fields[index];
        }
        break;
      }
    }

    return null;
  }

  private String lineOf(String key, String[] lines) {
    for (var line : lines) {
      if (line.startsWith(key)) {
        return line.substring(key.length());
      }
    }
    return null;
  }

}
