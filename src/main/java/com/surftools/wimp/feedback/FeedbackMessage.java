/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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

package com.surftools.wimp.feedback;

import java.util.ArrayList;
import java.util.Collections;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.message.ExportedMessage;

/**
 * record to combine a FeedbackResult and an ExportedMessage
 */
public record FeedbackMessage(FeedbackResult feedbackResult, ExportedMessage message) implements IWritableTable {

  @Override
  public String[] getHeaders() {
    var list = new ArrayList<String>(feedbackResult.getHeaders().length + message.getHeaders().length);
    Collections.addAll(list, feedbackResult.getHeaders());
    Collections.addAll(list, message.getHeaders());
    return list.toArray(new String[list.size()]);
  }

  @Override
  public String[] getValues() {
    var list = new ArrayList<String>(feedbackResult.getHeaders().length + message.getHeaders().length);
    Collections.addAll(list, feedbackResult.getValues());
    Collections.addAll(list, message.getValues());
    return list.toArray(new String[list.size()]);
  }

  @Override
  public int compareTo(IWritableTable o) {
    var otherFeedback = (FeedbackMessage) o;
    return this.message.compareTo(otherFeedback.message);
  }

  public FeedbackMessage updateLocation(LatLongPair newLocation) {
    var newFeedbackResult = feedbackResult.updateLocation(newLocation);
    return new FeedbackMessage(newFeedbackResult, this.message);
  }

}
