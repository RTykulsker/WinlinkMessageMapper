/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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

package com.surftools.wimp.processors.std;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.MessageManager;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.message.PlainMessage;
import com.surftools.wimp.utils.config.impl.MemoryConfigurationManager;

public class DeduplicationProcessorTest {

  @Ignore
  @Test
  public void test_zeroMessages() {
    var cm = new MemoryConfigurationManager(Key.values());
    // TODO cm.putString(Key.PATH, "src/test/resources/");
    var mm = new MessageManager();
    mm.load(new ArrayList<ExportedMessage>());
    var p = new DeduplicationProcessor();
    p.initialize(cm, mm);
    p.process();
    var sender = "UNIT_TEST";
    var messages = mm.getAllMessagesForSender(sender);
    assertEquals(0, messages.size());

    messages = mm.getOriginalMessages();
    assertEquals(0, messages.size());
  }

  @Ignore
  @Test
  public void test_oneMessage() {
    var cm = new MemoryConfigurationManager(Key.values());
    // TODO cm.putString(Key.PATH, "src/test/resources/");
    cm.putString(Key.DEDUPLICATION_RULES, "{plain:0}");
    var mm = new MessageManager();
    var sender = "UNIT_TEST";
    var m = makePlainMessage("My MID", sender, "My Subject", LocalDateTime.now(), "foo.txt");
    List<ExportedMessage> messages = new ArrayList<ExportedMessage>();
    messages.add(m);
    mm.load(messages);
    var p = new DeduplicationProcessor();
    p.initialize(cm, mm);
    p.process();

    messages = mm.getAllMessagesForSender(sender);
    assertEquals(1, messages.size());

    messages = mm.getOriginalMessages();
    assertEquals(1, messages.size());
  }

  @Ignore
  @Test
  public void test_oneDuplicateMessage() {
    var cm = new MemoryConfigurationManager(Key.values());
    // TODO cm.putString(Key.PATH, "src/test/resources/");
    var mm = new MessageManager();

    List<ExportedMessage> messages = new ArrayList<ExportedMessage>();
    var sender = "UNIT_TEST";
    var m1 = makePlainMessage("My MID", sender, "My Subject", LocalDateTime.now(), "foo1.txt");
    messages.add(m1);
    var m2 = makePlainMessage("My MID", sender, "My Subject", LocalDateTime.now(), "foo2.txt");
    messages.add(m2);

    mm.load(messages);
    var p = new DeduplicationProcessor();
    p.initialize(cm, mm);
    p.process();

    messages = mm.getAllMessagesForSender(sender);
    assertEquals(1, messages.size());

    messages = mm.getOriginalMessages();
    assertEquals(2, messages.size());
  }

  @Ignore
  @Test
  public void test_oneSupercededMessage() {
    var cm = new MemoryConfigurationManager(Key.values());
    // TODO cm.putString(Key.PATH, "src/test/resources/");
    cm.putString(Key.DEDUPLICATION_RULES, "{plain:1}");
    var mm = new MessageManager();

    List<ExportedMessage> messages = new ArrayList<ExportedMessage>();
    var sender = "UNIT_TEST";
    var m1 = makePlainMessage("My MID-1", sender, "My Subject-1", LocalDateTime.of(LocalDate.now(), LocalTime.of(1, 1)),
        "foo1.txt");
    messages.add(m1);
    var m2 = makePlainMessage("My MID-2", sender, "My Subject-2", LocalDateTime.of(LocalDate.now(), LocalTime.of(2, 2)),
        "foo2.txt");
    messages.add(m2);

    mm.load(messages);
    var p = new DeduplicationProcessor();
    p.initialize(cm, mm);
    p.process();

    messages = mm.getAllMessagesForSender(sender);
    assertEquals(1, messages.size());

    messages = mm.getOriginalMessages();
    assertEquals(2, messages.size());
  }

  @Ignore
  @Test
  public void test_multipleSupercededMessage() {
    final var N = 3;
    var cm = new MemoryConfigurationManager(Key.values());
    // TODO cm.putString(Key.PATH, "src/test/resources/");
    cm.putString(Key.DEDUPLICATION_RULES, "{plain:" + N + "}");
    var mm = new MessageManager();

    List<ExportedMessage> messages = new ArrayList<ExportedMessage>();
    var sender = "UNIT_TEST";
    for (var i = 1; i <= 2 * N; ++i) {
      var m = makePlainMessage("My MID-" + i, sender, "My Subject-" + i,
          LocalDateTime.of(LocalDate.now(), LocalTime.of(i, i)), "foo" + i + ".txt");
      messages.add(m);
    }

    mm.load(messages);
    assertEquals(2 * N, mm.getAllMessagesForSender(sender).size());
    assertEquals(2 * N, mm.getOriginalMessages().size());

    var p = new DeduplicationProcessor();
    p.initialize(cm, mm);
    p.process();

    messages = mm.getAllMessagesForSender(sender);
    assertEquals(N, messages.size());

    messages = mm.getOriginalMessages();
    assertEquals(2 * N, messages.size());
  }

  public PlainMessage makePlainMessage(String messageId, String from, String subject, LocalDateTime dateTime,
      String fileName) {

    var exportedMessage = new ExportedMessage(messageId, from, null, null, null, null, subject, dateTime, null, null,
        null, null, null, false, fileName, null);
    var plainMessage = new PlainMessage(exportedMessage);
    return plainMessage;
  }
}
