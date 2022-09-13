/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.tool.neighborTool;

import com.surftools.utils.config.IConfigurationKey;

public enum NeighborKey implements IConfigurationKey {

  PATH("path"), // path to message files
  KML_TEMPLATE_PATH("kmlTemplatePath"), // template file for KML
  TARGETS("targets"), // comma-delimited list of call sign targets to produce output for
  INPUT("input"), // semicolon-delimited list of comma-delimited params (filename,filetype,file data1, ...)
  DISTANCE_BOUNDS("distanceBounds"), // semicolon-delimited list of comma-delimited pair/triplet of min distance in
                                     // miles, max distance in miles, optional name
  MAP_NAME("mapName"), //
  MAP_DESCRIPTION("mapDescription"), ///
  ;

  private final String key;

  private NeighborKey(String key) {
    this.key = key;
  }

  public static NeighborKey fromString(String string) {
    for (NeighborKey key : NeighborKey.values()) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String toString() {
    return key;
  }
}