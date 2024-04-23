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

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.core.IWritableTable;

/**
 * record to hold results of computing feedback
 */
public record FeedbackResult(String call, String latitude, String longitude, int feedbackCount, String feedback)
    implements IWritableTable {

  @Override
  public String[] getHeaders() {
    return new String[] { "Call", "Feedback Latitude", "Feedback Longitude", "Feedback Count", "Feedback" };
  }

  @Override
  public String[] getValues() {
    return new String[] { call, latitude, longitude, String.valueOf(feedbackCount), feedback };
  }

  @Override
  public int compareTo(IWritableTable other) {
    var o = (FeedbackResult) other;
    return this.call.compareTo(o.call);
  }

  public FeedbackResult updateLocation(LatLongPair newLocation) {
    return new FeedbackResult(this.call, newLocation.getLatitude(), newLocation.getLongitude(), this.feedbackCount,
        this.feedback);
  }
}
