/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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

package com.surftools.wimp.processors.std.baseExercise;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.ReadProcessor;

/**
 * some messages can attach a two line CSV file ...
 */
public abstract class CsvSingleMessageFeedbackProcessor extends SingleMessageFeedbackProcessor {
  private static Logger logger = LoggerFactory.getLogger(CsvSingleMessageFeedbackProcessor.class);

  public enum TestType {
    TEST_EQUALS, // default case
    TEST_IGNORE, // no testing
    TEST_IF_PRESENT
  }

  private List<String> referenceKeys;
  private List<String> referenceValues;
  private Map<String, String> referenceMap;

  private Map<String, TestType> keyTypeMap;

  /**
   * read reference file, etc.
   *
   * @param messageType
   *
   * @param referencePath
   */
  public void initialize(Path referencePath, Map<TestType, List<String>> testTypeKeyListMap) {
    var listOfStringArrarys = ReadProcessor.readCsvFileIntoFieldsArray(referencePath, ',', true, 0);
    if (listOfStringArrarys == null || listOfStringArrarys.size() != 2) {
      logger.error("FATAL: expected exactly 2 lines from file: " + referencePath.toString());
      System.exit(1);
    }

    referenceKeys = Arrays.asList(listOfStringArrarys.get(0));
    referenceValues = Arrays.asList(listOfStringArrarys.get(1));

    if (referenceKeys.size() != referenceValues.size()) {
      logger
          .error("FATAL: csv file: " + referencePath.toString() + ", header line has " + referenceKeys.size()
              + ", data line has " + referenceValues.size());
      System.exit(1);
    }

    referenceMap = new LinkedHashMap<>();
    for (var i = 0; i < referenceKeys.size(); ++i) {
      referenceMap.put(referenceKeys.get(i), referenceValues.get(i));
    }

    // set test types
    keyTypeMap = new HashMap<>();
    if (testTypeKeyListMap != null) {
      for (var testType : testTypeKeyListMap.keySet()) {
        var keyList = testTypeKeyListMap.get(testType);
        for (var key : keyList) {
          keyTypeMap.put(key, testType);
        }
      }
    }

  }

  protected void csvTestAll(ExportedMessage message) {
    var candidates = getCandidateCsvAttachmentsByName(message.attachments);
    if (candidates.size() == 1) {
      count(sts.test(messageType.toString() + " message: valid csv attachment found", true));
      csv_test(candidates.get(0).values());
    } else {
      count(sts.test(messageType.toString() + " message: no valid csv attachment found", false));
    }
  }

  public void csv_test(List<String> values) {
    for (var i = 0; i < referenceKeys.size(); ++i) {
      var key = referenceKeys.get(i);
      var refValue = referenceValues.get(i).strip();
      var value = values.get(i).strip();
      var testType = keyTypeMap.get(key);
      if (testType == null) {
        testType = TestType.TEST_EQUALS;
      }

      if (testType == TestType.TEST_EQUALS) {
        if (refValue.strip().length() > 0) {
          count(sts.test_2line(key + " should be #EV", refValue.strip(), value));
        } else {
          count(sts.testIfEmpty(key + " should be emptry", value));
        }
      } else if (testType == TestType.TEST_IGNORE) {
        ; // ignored!
      } else if (testType == TestType.TEST_IF_PRESENT) {

        count(sts.testIfPresent(key + " should be present", value));
      } else {
        logger.error("Unsupported TestTest: " + testType.toString());
        System.exit(1);

      } // end if over TestTypes
    } // end for over values
  } // end test

  public record CsvAttachment(String fileName, List<String> values) {
  };

  protected List<CsvAttachment> getCandidateCsvAttachmentsByName(Map<String, byte[]> attachments) {
    var returnList = new ArrayList<CsvAttachment>();

    for (var fileName : attachments.keySet()) {
      var lcFileName = fileName.toLowerCase();
      if (lcFileName.endsWith(".csv")) {
        var content = new String(attachments.get(fileName));
        var listOfStringArrays = ReadProcessor.readCsvStringIntoFieldsArray(content, ',', true, 0);
        if (listOfStringArrays == null || listOfStringArrays.size() != 2) {
          continue;
        }

        var header = Arrays.asList(listOfStringArrays.get(0));
        var values = Arrays.asList(listOfStringArrays.get(1));

        if (header.size() != referenceKeys.size()) {
          continue;
        }

        // if (header.equals(referenceKeys)) {
        // continue;
        // }

        returnList.add(new CsvAttachment(fileName, values));
      }
    }

    return returnList;
  }

  protected List<String> getAttachmentValues(Map<String, byte[]> attachments, String fileName) {
    var content = new String(attachments.get(fileName));
    var listOfStringArrays = ReadProcessor.readCsvStringIntoFieldsArray(content, ',', true, 0);

    var values = Arrays.asList(listOfStringArrays.get(1));

    return values;
  }
}
