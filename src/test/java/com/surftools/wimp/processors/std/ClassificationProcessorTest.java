package com.surftools.wimp.processors.std;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClassificationProcessorTest {

  @Test
  public void test_makeParserName() {

    var mismatchCount = 0;
    for (var type : com.surftools.wimp.core.MessageType.values()) {
      if (ClassifierProcessor.IGNORED_TYPES.contains(type)) {
        System.out.println("Skipping type: " + type);
        continue;
      }

      var parserName = type.makeParserName();
      var className = "com.surftools.wimp.parser." + parserName + "Parser";
      try {
        Class.forName(className);
      } catch (Exception e) {
        ++mismatchCount;
        System.err.println("Couldn't create parser for: " + type.toString() + ", " + e.getLocalizedMessage());
      }
    }
    assertEquals("ParserName mismatchs (" + mismatchCount + ")", 0, mismatchCount);
  }

}
