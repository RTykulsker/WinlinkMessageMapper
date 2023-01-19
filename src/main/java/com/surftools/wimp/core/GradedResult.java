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

package com.surftools.wimp.core;

import java.util.ArrayList;
import java.util.Collections;

import com.surftools.wimp.message.ExportedMessage;

public record GradedResult(ExportedMessage message, String grade, String explanation) implements IWritableTable {
  @Override
  public String[] getHeaders() {
    var resultList = new ArrayList<String>(message.getHeaders().length + 2);
    Collections.addAll(resultList, message.getHeaders());
    Collections.addAll(resultList, new String[] { "Grade", "Explanation" });
    return resultList.toArray(new String[resultList.size()]);
  }

  @Override
  public String[] getValues() {
    var resultList = new ArrayList<String>(message.getHeaders().length + 2);
    Collections.addAll(resultList, message.getValues());
    Collections.addAll(resultList, new String[] { grade, explanation });
    return resultList.toArray(new String[resultList.size()]);
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (GradedResult) other;
    return this.message.compareTo(o.message);
  }

}
