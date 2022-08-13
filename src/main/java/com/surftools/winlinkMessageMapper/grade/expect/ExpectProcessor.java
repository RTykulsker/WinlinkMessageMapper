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

package com.surftools.winlinkMessageMapper.grade.expect;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.winlinkMessageMapper.grade.GradableMessage;
import com.surftools.winlinkMessageMapper.grade.GradeResult;
import com.surftools.winlinkMessageMapper.grade.GraderType;
import com.surftools.winlinkMessageMapper.grade.IGrader;

public class ExpectProcessor implements IGrader {
  private static final Logger logger = LoggerFactory.getLogger(ExpectProcessor.class);

  private final String name;
  private final Map<String, ExpectRecord> variableExpectRecordMap;
  private final Map<String, Object> variableCompiledObjectMap;
  private final Map<String, String> idMap;

  private double maxAvailablePoints = 0d;
  private List<String> explanations = new ArrayList<>();
  private double accumulatedPoints;

  public ExpectProcessor(String name, List<ExpectRecord> records) {
    this.name = name;
    variableExpectRecordMap = new HashMap<String, ExpectRecord>();
    variableCompiledObjectMap = new HashMap<String, Object>();
    idMap = new HashMap<String, String>();

    for (ExpectRecord record : records) {
      if (record.isId()) {
        idMap.put(record.variable(), record.value());
      }
      var variable = record.variable();
      var value = record.value();
      maxAvailablePoints += record.points();
      variableExpectRecordMap.put(variable, record);

      var expectType = record.expect();
      switch (expectType) {
      case RE:
        var pattern = Pattern.compile(value);
        variableCompiledObjectMap.put(variable, pattern);
        break;

      case LIST:
        var fields = value.split(",");
        var set = new HashSet<String>(Arrays.asList(fields));
        variableCompiledObjectMap.put(variable, set);
        break;

      case INTEGER:
        try {
          var i = Integer.parseInt(value);
          variableCompiledObjectMap.put(variable, i);
        } catch (Exception e) {
          logger.warn("*** name: " + name + ", var: " + record.variable() + ", could not parse int from: " + value);
          variableCompiledObjectMap.put(variable, null);
        }
        break;

      case INTEGER_RANGE:
        try {
          fields = value.split(",");
          if (fields.length == 2) {
            var lower = Integer.parseInt(fields[0]);
            var upper = Integer.parseInt(fields[1]);
            var array = new int[] { lower, upper };
            variableCompiledObjectMap.put(variable, array);
          }
        } catch (Exception e) {
          logger
              .warn("*** name: " + name + ", var: " + record.variable() + ", could not parse int pair from: " + value);
          variableCompiledObjectMap.put(variable, null);
        }
        break;

      case DATE_TIME:
        var dtf = DateTimeFormatter.ofPattern(value);
        variableCompiledObjectMap.put(variable, dtf);
        break;

      default:
        break;
      }
    } // end loop over ExpectRecord

    logger
        .info("expecting " + variableExpectRecordMap.size() + " values, total available points: " + maxAvailablePoints);
  }

  @Override
  public GradeResult grade(GradableMessage m) {
    var message = (ExpectGradableMessage) m;
    var map = message.getMap();

    // is this the right processor for the message?
    for (String variable : idMap.keySet()) {
      var idMapValue = idMap.get(variable);
      var messageValue = map.get(variable);
      if (idMapValue == null || messageValue == null || !idMapValue.equals(messageValue)) {
        return null;
      }
    }

    explanations.clear();
    accumulatedPoints = 0d;
    for (String variable : map.keySet()) {

      String value = map.get(variable);
      ExpectRecord record = variableExpectRecordMap.get(variable);
      if (record == null) {
        continue;
      }
      var expectType = record.expect();

      switch (expectType) {
      case ANY:
        accumulate(gradeAny(variable, value, record));
        break;

      case NULL:
        accumulate(gradeNull(variable, value, record));
        break;

      case NOT_NULL:
        accumulate(gradeNotNull(variable, value, record));
        break;

      case EXACT:
        accumulate(gradeExact(variable, value, record));
        break;

      case NOT_EXACT:
        accumulate(gradeNotExact(variable, value, record));
        break;

      case CI_EXACT:
        accumulate(gradeExactIgnoreCase(variable, value, record));
        break;

      case NOT_CI_EXACT:
        accumulate(gradeNotExactIgnoreCase(variable, value, record));
        break;

      case RE:
        accumulate(gradeRegularExpression(variable, value, record));
        break;

      case LIST:
        accumulate(gradeList(variable, value, record));
        break;

      case NOT_LIST:
        accumulate(gradeNotList(variable, value, record));
        break;

      case INTEGER:
        accumulate(gradeInteger(variable, value, record));
        break;

      case INTEGER_RANGE:
        accumulate(gradeIntegerRange(variable, value, record));
        break;

      case DATE_TIME:
        accumulate(gradeDateTime(variable, value, record));
        break;

      case CONTAINS:
        accumulate(gradeContains(variable, value, record));
        break;

      case STARTS_WITH:
        accumulate(gradeStartsWith(variable, value, record));
        break;
      }

    }

    var score = (int) (Math.round((100d * accumulatedPoints) / maxAvailablePoints));
    var explanation = (explanations.size() == 0) ? "" : String.join(",", explanations);
    var result = new GradeResult(String.valueOf(score), explanation);

    logger.info("processor: " + name + ", result: " + result);
    return result;
  }

  private void accumulate(GradeResult gradeResult) {
    var points = Double.parseDouble(gradeResult.grade());
    accumulatedPoints += points;
    var explanation = gradeResult.explanation();
    if (explanation != null) {
      explanations.add(explanation);
    }
  }

  private GradeResult gradeAny(String variable, String value, ExpectRecord record) {
    return new GradeResult(Double.toString(record.points()), null);
  }

  private GradeResult gradeNull(String variable, String value, ExpectRecord record) {
    if (value == null || value.length() == 0) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + " must be null");
    }
  }

  private GradeResult gradeNotNull(String variable, String value, ExpectRecord record) {
    if (value != null && value.length() >= 0) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + " must not be null");
    }
  }

  private GradeResult gradeExact(String variable, String value, ExpectRecord record) {
    if (value != null && value.equals(record.value())) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " must equal: " + record.value());
    }
  }

  private GradeResult gradeNotExact(String variable, String value, ExpectRecord record) {
    if (value != null && !value.equals(record.value())) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " must not equal: " + record.value());
    }
  }

  private GradeResult gradeExactIgnoreCase(String variable, String value, ExpectRecord record) {
    if (value != null && value.equalsIgnoreCase(record.value())) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d",
          "var: " + variable + ", val: " + value + " must equal (ignore case): " + record.value());
    }
  }

  private GradeResult gradeNotExactIgnoreCase(String variable, String value, ExpectRecord record) {
    if (value != null && !value.equalsIgnoreCase(record.value())) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " must not equal: " + record.value());
    }
  }

  private GradeResult gradeRegularExpression(String variable, String value, ExpectRecord record) {
    boolean isCorrect = false;
    if (value != null) {
      var pattern = ((Pattern) variableCompiledObjectMap.get(variable));
      isCorrect = pattern.matcher(value).matches();
    }

    if (isCorrect) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " must not equal: " + record.value());
    }
  }

  private GradeResult gradeList(String variable, String value, ExpectRecord record) {
    boolean isCorrect = false;
    if (value != null) {
      @SuppressWarnings("unchecked")
      var set = ((Set<String>) variableCompiledObjectMap.get(variable));
      isCorrect = set.contains(value);
    }

    if (isCorrect) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " must not equal: " + record.value());
    }
  }

  private GradeResult gradeNotList(String variable, String value, ExpectRecord record) {
    boolean isCorrect = false;
    if (value != null) {
      @SuppressWarnings("unchecked")
      var set = ((Set<String>) variableCompiledObjectMap.get(variable));
      isCorrect = !set.contains(value);
    }

    if (isCorrect) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " must not equal: " + record.value());
    }
  }

  private GradeResult gradeInteger(String variable, String value, ExpectRecord record) {
    boolean isCorrect = false;
    Integer compiled = (Integer) variableCompiledObjectMap.get(variable);
    if (compiled != null) {
      if (value != null) {
        try {
          Integer i = Integer.parseInt(value);
          isCorrect = i.equals(compiled);
        } catch (Exception e) {
          ; //
        }
      }
    }
    if (isCorrect) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " != " + record.value());
    }
  }

  private GradeResult gradeIntegerRange(String variable, String value, ExpectRecord record) {
    boolean isCorrect = false;
    var range = (int[]) variableCompiledObjectMap.get(variable);
    if (range != null) {
      if (value != null) {
        try {
          Integer i = Integer.parseInt(value);
          var lower = range[0];
          var upper = range[1];
          isCorrect = (lower <= i && i < upper);
        } catch (Exception e) {
          ; //
        }
      }
    }
    if (isCorrect) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " not in range " + record.value());
    }
  }

  private GradeResult gradeDateTime(String variable, String value, ExpectRecord record) {
    boolean isCorrect = false;

    var dtf = (DateTimeFormatter) variableCompiledObjectMap.get(variable);
    if (dtf != null) {
      try {
        dtf.parse(value);
        isCorrect = true;
      } catch (Exception e) {
        ; //
      }
    }

    if (isCorrect) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + "not a date/time for: " + record.value());
    }
  }

  private GradeResult gradeContains(String variable, String value, ExpectRecord record) {
    if (value != null && value.contains(record.value())) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " doesn't contain: " + record.value());
    }
  }

  private GradeResult gradeStartsWith(String variable, String value, ExpectRecord record) {
    if (value != null && value.startsWith(record.value())) {
      return new GradeResult(Double.toString(record.points()), null);
    } else {
      return new GradeResult("0d", "var: " + variable + ", val: " + value + " doesn't contain: " + record.value());
    }
  }

  @Override
  public GradeResult grade(String s) {
    return null;
  }

  @Override
  public String getPostProcessReport(List<GradableMessage> messages) {
    return null;
  }

  public String getName() {
    return name;
  }

  @Override
  public GraderType getGraderType() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void setDumpIds(Set<String> dumpIds) {
  }

  @Override
  public void setConfigurationManager(IConfigurationManager cm) {
  }

}
