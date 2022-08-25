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

package com.surftools.winlinkMessageMapper.tool.p2p.pickup;

import com.surftools.utils.config.IConfigurationKey;

public enum PickupKey implements IConfigurationKey {

  OUTPUT_PATH("outputPath"), // path to output files
  FIELD_PATH("fieldPath"), // path to input Field stations, just for lat/long
  TARGET_PATH("targetPath"), // path to input Target stations, just for lat/log
  KML_TEMPLATE_PATH("kmlTemplatePath"), // template file for KML

  BEGIN_PATH("beginPath"), // path to BEGIN files from targets
  END_PATH("endPath"), // path to END files from targets

  MAP_NAME("mapName"), // name of KML map
  MAP_DESCRIPTION("mapDescription"), // description of KML MAP
  REQUIRE_FIELD_CALL_IN_FIELD_MAP("requireFieldCallInFieldMap"), // for testing Target messages for isP2P

  DUMP_IDS("dumpIds"), // comma-delimited list of messageIds or call signs to dump message contents for

  EXCLUDED_FIELDS("excludedFields"), // comma-delimited list of Field station calls to exclude
  EXCLUDED_TARGETS("excludedTargets"), // comma-delimited list of Target stations calls to be excluded
  ;

  private final String key;

  private PickupKey(String key) {
    this.key = key;
  }

  public static PickupKey fromString(String string) {
    for (PickupKey key : PickupKey.values()) {
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