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

import java.util.List;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.database.entity.ParticipantDetail;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class DatabaseManager {
  private IDatabaseEngine engine;

  public DatabaseManager(IConfigurationManager cm) {

    var engineTypeName = cm.getAsString(Key.DATABASE_ENGINE_TYPE, DatabaseEngineType.CSV.name());
    var engineType = DatabaseEngineType.valueOf(engineTypeName);
    if (engineType == null) {
      throw new RuntimeException("Could not find engineType for: " + engineTypeName);
    }

    switch (engineType) {
    case CSV:
      engine = new CsvDatabaseEngine(cm);
      break;

    default:
      throw new RuntimeException("Could not find database engine for " + engineType.name());
    }
  }

  public List<ParticipantDetail> getParticipantAllDetails() {
    return engine.getAllParticipantDetails();
  }

  public IDatabaseEngine getEngine() {
    return engine;
  }
}
