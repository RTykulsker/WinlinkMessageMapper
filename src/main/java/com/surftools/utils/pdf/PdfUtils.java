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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

public class PdfUtils implements IPdfUtils {
  private static final Logger logger = LoggerFactory.getLogger(PdfUtils.class);

  private String getHeader(byte[] bytes) {
    if (bytes == null) {
      return null;
    }

    var charList = new ArrayList<Character>();
    for (var i = 0; i < bytes.length; ++i) {
      var b = bytes[i];
      if (b == '\n') {
        break;
      }
      if (i == 16) {
        return null;
      }
      charList.add(Character.valueOf((char) b));
    }

    var s = charList.stream().map(String::valueOf).collect(Collectors.joining());
    return s;
  }

  @Override
  public boolean isPdf(byte[] bytes) {
    var header = getHeader(bytes);
    if (header == null) {
      return false;
    }

    var fields = header.split("-");
    if (fields.length == 2 && fields[0].equalsIgnoreCase("%PDF")) {
      return true;
    }

    return false;
  }

  @Override
  public String getPdfVersion(byte[] bytes) {
    var header = getHeader(bytes);

    if (header == null) {
      return null;
    }

    var fields = header.split("-");
    if (fields.length == 2) {
      return fields[1];
    }

    return null;
  }

  @Override
  public int getNumberOfPages(byte[] bytes) {
    int pages = 0;
    if (!isPdf(bytes)) {
      return pages;
    }

    try {
      var reader = new PdfReader(bytes);
      pages = reader.getNumberOfPages();
      reader.close();
      return pages;
    } catch (IOException e) {
      logger.error("Exception in getNumberOfPages: " + e.getLocalizedMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getTextFromAllPages(byte[] bytes) {
    if (!isPdf(bytes)) {
      return null;
    }

    var nPages = getNumberOfPages(bytes);
    var text = new StringBuilder();

    for (var pageNumber = 1; pageNumber <= nPages; ++pageNumber) {
      text.append(getTextFromPage(bytes, pageNumber));
    }

    return text.toString();
  }

  @Override
  public String getTextFromPage(byte[] bytes, int pageNumber) {
    if (!isPdf(bytes)) {
      return null;
    }

    String text = null;
    try {
      var reader = new PdfReader(bytes);
      text = PdfTextExtractor.getTextFromPage(reader, pageNumber);
      reader.close();
      return text;
    } catch (IOException e) {
      logger.error("Exception in getTextFromPage[" + pageNumber + "]: " + e.getLocalizedMessage());
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, String> getFieldMap(byte[] bytes) {
    if (!isPdf(bytes)) {
      return null;
    }

    try {
      var reader = new PdfReader(bytes);
      var map = new HashMap<String, String>();
      var fields = reader.getAcroFields();
      var fieldMap = fields.getFields();
      for (var key : fieldMap.keySet()) {
        var value = fieldMap.get(key);
        map.put(key, value.toString());
      }
      reader.close();
      return map;
    } catch (IOException e) {
      logger.error("Exception in getNumberOfPages: " + e.getLocalizedMessage());
      throw new RuntimeException(e);
    }

  }

}
