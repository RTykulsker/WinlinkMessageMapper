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

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;

import com.surftools.utils.counter.Counter;

public class FormField {

  public final String label;
  public final String placeholderValue;
  public final FFType type;
  public final int points;
  public final int importance;
  public int count;
  public Counter counter;
  public final Object data;

  private static final FFType DEFAULT_TYPE = FFType.ALPHANUMERIC;

  public FormField(String label) {
    this(FFType.EMPTY, label, null, 0, 0);
  }

  public FormField(String label, String placeholderValue) {
    this(DEFAULT_TYPE, label, placeholderValue, 0, 0);
  }

  public FormField(FFType type, String label) {
    this(type, label, null, 0, 0);
  }

  public FormField(FFType type, String label, String placeholderValue) {
    this(type, label, placeholderValue, 0, 0);
  }

  public FormField(FFType type, String label, String placeholderValue, int points) {
    this(type, label, placeholderValue, points, 0);
  }

  public FormField(FFType type, String label, String placeholderValue, int points, int importance) {
    if (type == null) {
      type = DEFAULT_TYPE;
    }

    this.type = type;
    this.label = label;
    this.placeholderValue = placeholderValue;
    this.points = points;
    this.importance = importance;
    this.counter = new Counter();

    switch (type) {
    case DATE_TIME:
    case DATE_TIME_NOT:
      data = DateTimeFormatter.ofPattern(placeholderValue);
      break;

    case DATE_TIME_ON_OR_BEFORE:
    case DATE_TIME_ON_OR_AFTER:
      data = FormFieldManager.FORMATTER.parse(placeholderValue);
      break;

    case LIST:
      data = new HashSet<String>(Arrays.asList(placeholderValue.split(",")));
      break;

    case ALPHANUMERIC:
      data = placeholderValue.replaceAll("[^A-Za-z0-9]", "");
      break;

    case IGNORE_WHITESPACE:
      data = placeholderValue.replaceAll("\\s+", "");
      break;

    default:
      data = null;
    }
  }

};
