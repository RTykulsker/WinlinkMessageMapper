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

package com.surftools.wimp.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * delete or replace unwanted characters from a String
 *
 * useful before handing off to XML processor
 *
 * @author bobt
 *
 */
public class CharacterAssassinator {
  private List<String> deleteList;
  private Map<String, String> replaceMap;

  public CharacterAssassinator() {
    deleteList = new ArrayList<>();
    replaceMap = new HashMap<>();
  }

  public CharacterAssassinator(List<String> deleteList, Map<String, String> replaceMap) {
    if (deleteList == null) {
      this.deleteList = new ArrayList<>();
    } else {
      this.deleteList = deleteList;
    }

    if (replaceMap == null) {
      this.replaceMap = new HashMap<>();
    } else {
      this.replaceMap = replaceMap;
    }
  }

  /**
   * delete or replace unwanted characters from a String
   *
   * @param string
   * @return
   */
  public String assassinate(String string) {
    for (String s : deleteList) {
      string = string.replaceAll(s, "");
    }

    for (String key : replaceMap.keySet()) {
      string = string.replaceAll(key, replaceMap.get(key));
    }

    return string;
  }
}
