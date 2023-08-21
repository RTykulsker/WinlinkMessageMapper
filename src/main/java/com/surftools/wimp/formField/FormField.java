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

package com.surftools.wimp.formField;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.surftools.utils.counter.Counter;

public class FormField {
  // public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public final String label;
  public final String placeholderValue;
  public final FFType type;
  public final int points;
  public int count;
  public Counter counter;
  public final Set<String> set;

  public FormField(FFType type, String label, String placeholderValue, int points) {
    this.type = type;
    this.label = label;
    this.placeholderValue = placeholderValue;
    this.points = points;
    this.counter = new Counter();

    if (placeholderValue == null) {
      set = new HashSet<String>();
    } else {
      var array = placeholderValue.split(",");
      set = new HashSet<String>(Arrays.asList(array));
    }
  }

};
