/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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

package com.surftools.utils.pdf;

import java.util.Map;

public interface IPdfUtils {

  /**
   * determine if byte array represents a PDF "file" or not
   *
   * @param bytes
   * @return true if it does, false if not
   */
  public boolean isPdf(byte[] bytes);

  /**
   * via "magic" header
   *
   * @param bytes
   * @return null if not pdf
   */
  public String getPdfVersion(byte[] bytes);

  /**
   * return number of "pages" in PDF document
   *
   * @param bytes
   *
   * @return 0 if no pages, etc.
   */
  public int getNumberOfPages(byte[] bytes);

  /**
   * return a single String representing all "extractable" text
   *
   * @param bytes
   * @return null if not PDF, empty or non-empty String otherwise
   */
  public String getTextFromAllPages(byte[] bytes);

  /**
   * return a single String representing all "extractable" text
   *
   * @param bytes
   * @param pageNumber,
   *          starting from page 1
   * @return null if not PDF, empty or non-empty String otherwise
   */
  public String getTextFromPage(byte[] bytes, int pageNumber);

  /**
   * return a Map of form field names and values
   *
   * @param bytes
   * @return null if not PDF
   */
  public Map<String, String> getFieldMap(byte[] bytes);

}
