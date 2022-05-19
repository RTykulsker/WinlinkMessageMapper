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

public class ParsedReply implements Comparable<ParsedReply> {
  public String call;
  public String latitude;
  public String longitude;
  public String reply;
  public boolean isParseOk;
  public String parseErrors;
  public String[] values;

  public ParsedReply(String call, String reply) {
    this.call = call;
    this.reply = reply;
  }

  public String[] getValues() {
    if (values == null) {
      return new String[] { call, latitude, longitude, reply, String.valueOf(isParseOk), parseErrors };
    } else {
      var fixedCount = 6;
      var array = new String[fixedCount + values.length];
      array[0] = call;
      array[1] = latitude;
      array[2] = longitude;
      array[3] = reply;
      array[4] = String.valueOf(isParseOk);
      array[5] = parseErrors;
      for (var i = 0; i < values.length; ++i) {
        array[fixedCount + i] = values[i];
      }
      return array;
    }
  }

  @Override
  public int compareTo(ParsedReply o) {
    return this.call.compareTo(o.call);
  }
}
