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

package com.surftools.wimp.service.map;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.feedback.FeedbackMessage;
import com.surftools.wimp.practice.PracticeProcessor.Summary;

public record MapEntry(String label, LatLongPair location, String message) {

  public static MapEntry fromSummary(Summary summary) {
    var content = "MessageId: " + summary.messageId + "\n" + "Feedback Count: " + summary.getFeedbackCount() + "\n"
        + "Feedback: " + summary.getFeedback();
    return new MapEntry(summary.from, summary.location, content);
  }

  public static MapEntry fromSingleMessageFeedback(IWritableTable s) {
    var feedbackMessage = (FeedbackMessage) s;
    var feedbackResult = feedbackMessage.feedbackResult();
    var location = new LatLongPair(feedbackResult.latitude(), feedbackResult.longitude());
    var messageId = feedbackMessage.message().messageId;
    var content = "MessageId: " + messageId + "\n" + "Feedback Count: " + feedbackResult.feedbackCount() + "\n"
        + "Feedback: " + feedbackResult.feedback();
    return new MapEntry(feedbackResult.call(), location, content);
  }

}
