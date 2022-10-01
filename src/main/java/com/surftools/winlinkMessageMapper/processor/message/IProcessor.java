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

package com.surftools.winlinkMessageMapper.processor.message;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import com.surftools.winlinkMessageMapper.dto.message.ExportedMessage;

/**
 * interface that all message proccessors must conform to
 *
 * @author bobt
 *
 */
public interface IProcessor {

  /**
   * process the ExportedMessage
   *
   * @param message
   * @return either a typed message or a rejections
   */
  public ExportedMessage process(ExportedMessage message);

  /**
   * return a String that summarizes the processing, like grade totals:
   *
   * NOTE WELL: the processor should NOT accumulate statistics during the initial processing. Messages may be dropped
   * because of de-duplication or other explicit reasons
   *
   * @param messages
   *
   * @return
   */
  public String getPostProcessReport(List<ExportedMessage> messages);

  public void setDumpIds(Set<String> dumpIdsSet);

  public void setPath(Path path);

  public void setSaveAttachments(boolean asBoolean);

}
