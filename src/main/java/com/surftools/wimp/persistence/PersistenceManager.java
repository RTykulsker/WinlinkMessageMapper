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

package com.surftools.wimp.persistence;

import java.util.Set;

import com.surftools.wimp.persistence.dto.BulkInsertEntry;
import com.surftools.wimp.persistence.dto.Exercise;
import com.surftools.wimp.persistence.dto.ReturnRecord;
import com.surftools.wimp.persistence.dto.ReturnStatus;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class PersistenceManager implements IPersistenceManager {
  protected IConfigurationManager cm;
  protected IPersistenceEngine engine;
  protected EngineType engineType;

  public PersistenceManager(IConfigurationManager cm) {
    this.cm = cm;
    this.engineType = EngineType.SQLITE_NATIVE;
    this.engine = new SQLIteNativeEngine(cm);
  }

  @Override
  public ReturnRecord getAllUsers() {
    return engine.getAllUsers();
  }

  @Override
  public ReturnRecord getAllExercises() {
    return engine.getAllExercises();
  }

  @Override
  public ReturnRecord getAllEvents() {
    return engine.getAllEvents();
  }

  @Override
  public ReturnRecord bulkInsert(BulkInsertEntry input) {
    return engine.bulkInsert(input);
  }

  @Override
  public ReturnRecord updateDateJoined() {
    return engine.updateDateJoined();
  }

  @Override
  public ReturnRecord getUsersMissingExercises(Set<String> requiredExerciseTypes, Exercise fromExercise,
      int missLimit) {

    return engine.getUsersMissingExercises(requiredExerciseTypes, fromExercise, missLimit);
  }

  @Override
  public ReturnRecord getUsersHistory(Set<String> requiredExerciseTypes, Exercise fromExercise, boolean doPartition) {
    return engine.getUsersHistory(requiredExerciseTypes, fromExercise, doPartition);
  }

  @Override
  public ReturnRecord getHealth() {
    var ret = engine.getHealth();
    if (ret == null) {
      throw new RuntimeException("null return from getHealth()");
    }
    if (ret.status() == ReturnStatus.ERROR) {
      throw new RuntimeException("Database health error: " + ret.content());
    }
    return ret;
  }

}
