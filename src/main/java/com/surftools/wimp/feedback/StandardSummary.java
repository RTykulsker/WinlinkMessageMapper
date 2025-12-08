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

package com.surftools.wimp.feedback;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.processors.std.baseExercise.MultiMessageFeedbackProcessor.BaseSummary;

public class StandardSummary implements IWritableTable {
  public String from;
  public String to;
  public LatLongPair location;
  public LocalDateTime dateTime;
  public String feedbackCount;
  public String feedback;
  public String messageId;

  public StandardSummary(String from, String to, LatLongPair location, LocalDateTime dateTime, String feedbackCount,
      String feedback, String messageId) {
    this.from = from;
    this.to = to;
    this.location = location;
    this.dateTime = dateTime;
    this.feedbackCount = feedbackCount;
    this.feedback = feedback;
    this.messageId = messageId;
  }

  @Override
  public int compareTo(IWritableTable o) {
    var other = (StandardSummary) o;
    return from.compareTo(other.from);
  }

  @Override
  public String[] getHeaders() {
    var mIdHeader = messageId.contains(",") ? "MessageIds" : "MessageId";
    var list = new ArrayList<String>(
        List.of("From", "To", "Latitude", "Longitude", "Date", "Time", "Feedback Count", "Feedback", mIdHeader));
    return list.toArray(new String[list.size()]);
  }

  @Override
  public String[] getValues() {
    var latitude = location == null ? "0.0" : location.getLatitude();
    var longitude = location == null ? "0.0" : location.getLongitude();
    var date = dateTime == null ? "" : dateTime.toLocalDate().toString();
    var time = dateTime == null ? "" : dateTime.toLocalTime().toString();

    var nsTo = to == null ? "(null)" : to;

    var list = new ArrayList<String>(
        List.of(from, nsTo, latitude, longitude, date, time, feedbackCount, feedback, messageId));
    return list.toArray(new String[list.size()]);
  }

  public static StandardSummary fromMultiMessageFeedback(BaseSummary s) {
    return new StandardSummary(s.from, s.to, //
        s.location, s.dateTime, //
        s.getFeedbackCountString(), s.getFeedback(), s.messageIds);
  }

  public static StandardSummary fromSingleMessageFeedback(IWritableTable s) {
    var feedbackMessage = (FeedbackMessage) s;
    var m = feedbackMessage.message();
    var r = feedbackMessage.feedbackResult();
    return new StandardSummary(m.from, m.to, //
        m.mapLocation, m.msgDateTime, //
        String.valueOf(r.feedbackCount()), r.feedback(), m.messageId);
  }

}
