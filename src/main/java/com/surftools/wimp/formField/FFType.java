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

public enum FFType {
  REQUIRED, // field value must be present, but any value
  REQUIRED_NOT, // field value must be present, and NOT value held by placeholder
  OPTIONAL, // field may or may not (null/empty) be presnet
  OPTIONAL_NOT, // field may or may not be present, but if present, must NOT be value held by placeholder value
  CONTAINS, // field must be present and contain the placeholder value
  CONTAINED_BY, // field must be present and within the placeholder value
  DATE_TIME, // field must be parsable as a LocalDateTime
  DATE_TIME_NOT, // field must be parsable as a LocalDateTime, but NOT value held by placeholder value
  EMPTY, // field must be null or empty
  SPECIFIED, // field must be exactly as specified by placeholder value (case-independent)
  EQUALS, // same as specified
  EQUALS_IGNORE_CASE, // field must case-independently equal to placeholder value
  LIST, // field must be present in list
  DOUBLE // field must be present, and parsable as a double
}
