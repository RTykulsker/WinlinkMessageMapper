import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ParserNameTest {

  @Test
  public void test_mismatch_name_key() {
    var mismatchCount = 0;
    for (var type : com.surftools.wimp.core.MessageType.values()) {
      var key = type.toString();
      var name = type.name();
      var match = key.toLowerCase().equals(name.toLowerCase());
      if (!match) {
        ++mismatchCount;
        System.out.println("key: " + key + ", name: " + name + ", match: " + match);
      }
    }
    assertEquals("name/key mismatch", 0, mismatchCount);
  }
}
