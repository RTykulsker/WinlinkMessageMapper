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

package com.surftools.winlinkMessageMapper.reply;

public class AshfallReplyParser implements IReplyParser {

  private final int N_FIELDS = 8;

  private int goodParseCount = 0;
  private int badParseCount = 0;

  @Override
  public void parse(ParsedReply r) {
    var reply = r.reply;
    var lines = reply.split("\n");

    r.values = new String[N_FIELDS];
    if (lines.length == N_FIELDS) {
      ++goodParseCount;
      for (var i = 0; i < N_FIELDS; ++i) {
        var line = lines[i];
        var strippedLine = stripNumber(line);
        r.values[i] = strippedLine;
      }
      r.values = lines;
      r.isParseOk = true;
    } else {
      ++badParseCount;
      r.values = new String[N_FIELDS];
      r.isParseOk = false;
      r.parseErrors = "found " + lines.length + " lines in reply";
    }
  }

  private String stripNumber(String line) {
    if (!Character.isDigit(line.charAt(0))) {
      return line;
    }

    for (int i = 0; i < line.length(); ++i) {
      var c = line.charAt(i);
      if (Character.isDigit(c) || !Character.isAlphabetic(c)) {
        continue;
      }
      var s = line.substring(i);
      return s;
    }
    return "";
  }

  @Override
  public String[] getHeaders() {
    return new String[] { //
        "Call", //
        "Latitude", //
        "Longitude", //
        "Reply", //
        "ParseOk", //
        "ParseErrors", //
        "ToClearinghouse", //
        "ToTarget", //
        "WereInstructionsClear", //
        "WasGradingReasonable", //
        "WhatWorkedWell", //
        "ETOImprovments", //
        "OpImprovememnts", //
        "OtherComments", //
    };
  }

  @Override
  public int getGoodParseCount() {
    return goodParseCount;
  }

  @Override
  public int getBadParseCount() {
    return badParseCount;
  }
}
