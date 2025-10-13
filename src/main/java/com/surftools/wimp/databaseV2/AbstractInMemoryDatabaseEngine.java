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

package com.surftools.wimp.databaseV2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.surftools.wimp.databaseV2.entity.ExerciseId;
import com.surftools.wimp.databaseV2.entity.ParticipantDetail;

/**
 * optimized for what I think will be most common operation, updating list of ParticipantDetail entries
 */
public abstract class AbstractInMemoryDatabaseEngine implements IDatabaseEngine {
  protected Logger logger;

  // our source of truth
  protected Map<ExerciseId, List<ParticipantDetail>> participantDetailMap = new HashMap<>();

  public AbstractInMemoryDatabaseEngine(Logger logger) {
    this.logger = logger;
  }

  /*
   * our abstract methods
   */
  @Override
  public abstract void load();

  @Override
  public abstract void store();

  /*
   * everything else we can do here
   */
  @Override
  public void update(ExerciseId exerciseId, List<ParticipantDetail> list) {
    var oldList = participantDetailMap.get(exerciseId);
    if (oldList == null) {
      logger.info("no existing details for exerciseId: " + exerciseId);
    } else {
      logger.info("replacing " + oldList.size() + " details with " + list.size() + " new details");
    }
    participantDetailMap.put(exerciseId, list);
  }

  @Override
  public List<ParticipantDetail> getAllParticipantDetails() {
    var list = new ArrayList<ParticipantDetail>();
    Collections.sort(list);
    return list;
  }

}
