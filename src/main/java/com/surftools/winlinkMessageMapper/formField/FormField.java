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

package com.surftools.winlinkMessageMapper.formField;

public class FormField {
  // public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  public final String label;
  public final String placeholderValue;
  public final FormFieldType type;
  public final int points;
  public int count;

  public FormField(FormFieldType type, String label, String placeholderValue, int points) {
    this.type = type;
    this.label = label;
    this.placeholderValue = placeholderValue;
    this.points = points;
  }

  // public int test(String value, ArrayList<String> explanations) {
  // if (!ENABLE_NON_ACTIONABLE_FIELDS && points == NON_SCORABLE_FIELD_POINTS) {
  // return NON_SCORABLE_FIELD_POINTS;
  // }
  //
  // var isOk = false;
  // var explanation = "";
  //
  // switch (type) {
  // case DATE_TIME:
  // if (value == null) {
  // explanation = label + " must be supplied";
  // } else {
  // try {
  // LocalDateTime.parse(value, FORMATTER);
  // isOk = true;
  // } catch (Exception e) {
  // explanation = label + "(" + value + ") is not a valid Date/Time";
  // }
  // }
  // break;
  //
  // case DATE_TIME_NOT:
  // if (value == null) {
  // explanation = label + " must be supplied";
  // } else if (value.equalsIgnoreCase(placeholderValue)) {
  // explanation = label + "(" + value + ") must not be " + placeholderValue;
  // } else {
  // try {
  // LocalDateTime.parse(value, FORMATTER);
  // isOk = true;
  // } catch (Exception e) {
  // explanation = label + "(" + value + ") is not a valid Date/Time";
  // }
  // }
  // break;
  //
  // case EMPTY:
  // isOk = value == null || value.isBlank();
  // if (!isOk) {
  // explanation = label + "(" + value + ") must be blank";
  // }
  // break;
  //
  // case OPTIONAL:
  // isOk = true;
  // break;
  //
  // case OPTIONAL_NOT:
  // if (value != null && value.equalsIgnoreCase(placeholderValue)) {
  // explanation = label + "(" + value + ") must not be " + placeholderValue;
  // } else {
  // isOk = true;
  // }
  // break;
  //
  // case REQUIRED:
  // if (value == null) {
  // explanation = label + " must be supplied";
  // } else if (value.isBlank()) {
  // explanation = label + "(" + value + ") must not be blank";
  // } else {
  // isOk = true;
  // }
  // break;
  //
  // case REQUIRED_NOT:
  // if (value == null) {
  // explanation = label + " must be supplied";
  // } else if (value.isBlank()) {
  // explanation = label + "(" + value + ") must not be blank";
  // } else if (value.equalsIgnoreCase(placeholderValue)) {
  // isOk = false;
  // explanation = label + "(" + value + ") must not be " + placeholderValue;
  // } else {
  // isOk = true;
  // }
  //
  // break;
  //
  // case SPECIFIED:
  // if (value == null) {
  // explanation = label + " must be " + placeholderValue;
  // } else if (!value.equalsIgnoreCase(placeholderValue)) {
  // explanation = label + "(" + value + ") must be " + placeholderValue;
  // } else {
  // isOk = true;
  // }
  // break;
  //
  // default:
  // throw new RuntimeException("unhandled type: " + type.toString());
  // }
  //
  // var returnPoints = 0;
  // if (isOk) {
  // ++count;
  // returnPoints = points;
  // } else {
  // explanations.add(explanation);
  // }
  //
  // return returnPoints;
  // }
};
