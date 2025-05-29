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

package com.surftools.wimp.database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.database.entity.ParticipantDetail;
import com.surftools.wimp.service.IService;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class DatabaseService implements IDatabaseService, IService {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

  private DatabaseManager dm;

  public DatabaseService(IConfigurationManager cm) {
    dm = new DatabaseManager(cm);
  }

  @Override
  public List<String> getActiveParticipants(int minExercises, int maxLookbackDays) {
    var nowDate = LocalDate.now();
    var maxLookbackDate = nowDate.minusDays(maxLookbackDays);
    var participantSummaryMap = new HashMap<String, List<ParticipantDetail>>();
    for (var pd : dm.getParticipantAllDetails()) {
      var call = pd.call();
      var list = participantSummaryMap.getOrDefault(call, new ArrayList<ParticipantDetail>());
      list.add(pd);
      participantSummaryMap.put(call, list);
    }

    var returnList = new ArrayList<String>();
    for (var call : participantSummaryMap.keySet()) {
      var list = participantSummaryMap.get(call);
      if (list.size() < minExercises) {
        logger.debug("skipping call: " + call + " because only " + list.size() + " exercises");
        continue;
      }
      Collections.sort(list, (d1, d2) -> d1.exerciseId().compareTo(d2));
      var nDatesInRange = 0;
      for (var pd : list) {
        var exDate = pd.exerciseId().date();
        if (exDate.isBefore(maxLookbackDate)) {
          ++nDatesInRange;
        }
      } // end loop over details
      if (nDatesInRange >= minExercises) {
        returnList.add(call);
      } else {
        logger
            .debug(
                "skipping call: " + call + " because only " + nDatesInRange + " exercises in last " + maxLookbackDays);
      }
    } // end loop over calls

    return returnList;
  }

  @Override
  public String getName() {
    return "DatabaseService";
  }

}
