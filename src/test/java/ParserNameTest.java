import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.surftools.wimp.core.MessageType;

public class ParserNameTest {

  @Test
  public void test_makeParserName() {

    final var ignoreTypes = List.of(MessageType.EXPORTED, MessageType.REJECTS, MessageType.EYEWARN_DETAIL);
    var mismatchCount = 0;
    for (var type : com.surftools.wimp.core.MessageType.values()) {
      if (ignoreTypes.contains(type)) {
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
