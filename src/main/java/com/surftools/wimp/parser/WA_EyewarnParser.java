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

import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.core.RejectType;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.WA_EyewarnMessage;

public class WA_EyewarnParser extends AbstractBaseParser {

  @Override
  public ExportedMessage parse(ExportedMessage message) {

    try {
      String xmlString = new String(message.attachments.get(MessageType.WA_EYEWARN.rmsViewerName()));
      makeDocument(message.messageId, xmlString);

      var precedence = gx("prec");
      var isExercise = gx("excer");

      var ncs = gx("ncs");
      var location = gx("loc");

      var formDateTime = gx("datetime");
      var reportType = gx("reptype");
      var activationType = gx("acttype");
      var missionNumber = gx("missnum");

      var incidentType = gx("inctype");

      var numberOfZips = gx("zip");
      var totalCheckIns = gx("chkins");

      var questions = gx("questions");

      var bridges = gx("bridges");
      var cellTowers = gx("cells");
      var hospitals = gx("hospitals");
      var powerLinesTowers = gx("power");
      var roads = gx("roads");
      var schools = gx("schools");
      var otherLocalDamage = gx("other");

      var relayOperator = gx("relayop");
      var relayReceived = gx("rctime1");
      var relaySent = gx("senttime1");

      var radioOperator = gx("radioop");
      var radioReceived = gx("rctime2");

      var m = new WA_EyewarnMessage(message, //
          precedence, isExercise, //

          ncs, location, //

          formDateTime, reportType, activationType, missionNumber, //

          incidentType, //

          numberOfZips, totalCheckIns, //

          questions,

          bridges, //
          cellTowers, //
          hospitals, //
          powerLinesTowers, //
          roads, //
          schools, //
          otherLocalDamage, //

          relayOperator, //
          relayReceived, //
          relaySent, //

          radioOperator, //
          radioReceived //

      );

      return m;
    } catch (Exception e) {
      return reject(message, RejectType.PROCESSING_ERROR, e.getMessage());
    }
  }

  private String gx(String tagName) {
    return getStringFromXml(tagName);
  }

}