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

package com.surftools.winlinkMessageMapper.configuration;

import com.surftools.utils.config.IConfigurationKey;

public enum Key implements IConfigurationKey {

  PATH("path"), // path to message files
  DATABASE_PATH("databasePath"), // path to input database summary files
  REQUIRED_MESSAGE_TYPE("requiredMessageType"), // to ONLY process messages of a given type
  DUMP_IDS("dumpIds"), // comma-delimited list of messageIds or call signs to dump message contents for

  PREFERRED_PREFIXES("preferredPrefixes"), // comma-delimited preferred prefixes for To: addresses:
  PREFERRED_SUFFEXES("preferredSuffixes"), // comma-delimited preferred suffixes for To: addresses:
  NOT_PREFERRED_PREFIXES("notPreferredPrefixes"), // comma-delimited NOT preferred prefixes for To: addresses:
  NOT_PREFERRED_SUFFIXES("notPreferredSuffixes"), // comma-delimited NOT preferred suffixes for To: addresses

  DEDUPLICATION_THRESHOLD_Meters("deduplicationThresholdMeters"), // threshold distance in meters to avoid being
                                                                  // considered a duplicate location, negative to skip",

  SAVE_ATTACHMENTS("saveAttachments"), // save ALL attachments in exported message

  GRADE_KEY("gradeKey"), // exercise/processor-specific key to identify grading method, if any

  AGGREGATOR_NAME("aggregatorName"), // exercise-specific name of multi-message aggregator, if any

  MM_COMMENT_KEY("mmCommentKey"), // MultiMessageComment key, if any

  ;

  private final String key;

  private Key(String key) {
    this.key = key;
  }

  public static Key fromString(String string) {
    for (Key key : Key.values()) {
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