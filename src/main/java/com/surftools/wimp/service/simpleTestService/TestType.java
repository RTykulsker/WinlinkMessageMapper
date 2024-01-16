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

package com.surftools.wimp.service.simpleTestService;

@Deprecated
public enum TestType {
  STRING_EMPTY, //
  STRING_PRESENT, //

  STRING_EQUALS, //
  STRING_EQUALS_CI, // case-independent
  STRING_EQUALS_CI_AN, // case-independent and alpha-numeric only

  STRING_CONTAINS, // value contains expected value
  STRING_CONTAINS_CI, // case-independent
  STRING_CONTAINS_CI_AN, // case-independent and alpha-numeric only

  STRING_CONTAINED_BY, // value contained by expected value
  STRING_CONTAINED_BY_CI, // case-independent
  STRING_CONTAINED_BY_CI_AN, // case-independent and alpha-numeric only

  STRING_STARTS_WITH, // value starts with expected value
  STRING_STARTS_WITH_CI, // case-independent
  STRING_STARTS_WITH_CI_AN, // case-independent and alpha-numeric only

  STRING_ENDS_WITH, // value ends with expected value
  STRING_ENDS_WITH_CI, // case-independent
  STRING_ENDS_WITH_CI_AN, // case-independent and alpha-numeric only

  DATE_TIME_EQUALS, //
  DATE_TIME_GE, // on or after
  DATE_TIME_LE, // on or before

  BOOLEAN, // no data comparisons, predicate passed to test()
}
