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

package com.surftools.winlinkMessageMapper.tool.p2p.pickup;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.surftools.utils.config.IConfigurationManager;
import com.surftools.winlinkMessageMapper.dao.ExportedMessageReader;
import com.surftools.winlinkMessageMapper.dto.other.MessageType;
import com.surftools.winlinkMessageMapper.tool.WinlinkMessageMapper;

public class Dao {
  private static final Logger logger = LoggerFactory.getLogger(Dao.class);

  private final IConfigurationManager cm;
  private Map<String, Target> targetMap;
  private Map<String, Field> fieldMap;

  private final Set<String> dumpIds;
  private final Set<String> excludedFieldSet;
  private final Set<String> excludedTargetSet;

  public Dao(IConfigurationManager cm) {
    this.cm = cm;

    dumpIds = new TreeSet<>();
    var dumpIdsString = cm.getAsString(PickupKey.DUMP_IDS, "");
    dumpIds.addAll(Arrays.asList(dumpIdsString.split(",")));

    excludedFieldSet = new TreeSet<>();
    var excludedFieldString = cm.getAsString(PickupKey.EXCLUDED_FIELDS, "");
    excludedFieldSet.addAll(Arrays.asList(excludedFieldString.split(",")));

    excludedTargetSet = new TreeSet<>();
    var excludedTargetString = cm.getAsString(PickupKey.EXCLUDED_TARGETS, "");
    excludedTargetSet.addAll(Arrays.asList(excludedTargetString.split(",")));

    logger.info("dumpIds: " + String.join(",", dumpIds));
    logger.info("excludedFieldCalls: " + String.join(",", excludedFieldSet));
    logger.info("excludedTargetCalls: " + String.join(",", excludedTargetSet));
  }

  public Map<String, Target> getTargetMap() {
    var map = new TreeMap<String, Target>();
    var list = readTargets();
    for (var target : list) {
      var call = target.call;
      if (call == null || call.isBlank()) {
        continue;
      }
      map.put(target.call, target);
    }
    logger.info("created targetMap with " + map.size() + " target entries");

    this.targetMap = map;
    return map;
  }

  public List<Target> readTargets() {
    var targets = new ArrayList<Target>();
    String targetFilePathName = cm.getAsString(PickupKey.TARGET_PATH);

    try {
      Reader reader = new FileReader(targetFilePathName);
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      String[] fields;
      while ((fields = csvReader.readNext()) != null) {
        var target = new Target(fields);
        if (excludedTargetSet.contains(target.call)) {
          logger.info("*** skipping excludedTarget: " + target);
          continue;
        }

        if (target.ignore) {
          logger.warn("skipping target: " + target.toString() + " because of explicit ignore");
          continue;
        }

        logger.debug(target.toString());
        targets.add(target);

      }
    } catch (Exception e) {
      logger.error("exception processing targets: " + targetFilePathName + ", " + e.getLocalizedMessage());
    }

    logger.info("read " + targets.size() + " target entries from " + targetFilePathName);
    return targets;
  }

  public Map<String, Field> getFieldMap() {
    var map = new HashMap<String, Field>();
    var list = readFields();
    for (var field : list) {
      var call = field.call;
      if (call == null || call.isBlank()) {
        continue;
      }
      map.put(field.call, field);
    }
    logger.info("created fieldMap with " + map.size() + " field entries");

    this.fieldMap = map;
    return map;
  }

  public List<Field> readFields() {
    var list = new ArrayList<Field>();
    String fieldFilePathName = cm.getAsString(PickupKey.FIELD_PATH);

    try {
      Reader reader = new FileReader(fieldFilePathName);
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(',') //
            .withIgnoreQuotations(false) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(1)//
            .withCSVParser(parser)//
            .build();
      String[] fields;
      while ((fields = csvReader.readNext()) != null) {
        var field = new Field(fields);
        if (excludedFieldSet.contains(field.call)) {
          logger.info("*** skipping excludedField: " + field);
          continue;
        }

        logger.debug("row: " + list.size() + ", " + field.toString());
        list.add(field);

        if (dumpIds.contains(field.call)) {
          logger.debug("field: " + field);
        }
      }
    } catch (Exception e) {
      logger.error("exception processing fields: " + fieldFilePathName + ", " + e.getLocalizedMessage());
    }

    logger.info("read " + list.size() + " field entries from " + fieldFilePathName);
    return list;
  }

  /**
   * get Map by reading all the files in the appropriate directory
   *
   * @param string
   * @return
   */
  public Map<String, Set<String>> getFieldSetsFromTargets(String pathName, MessageType expectedMessageType,
      boolean shouldMessageBeP2P) {
    var map = new HashMap<String, Set<String>>();
    var path = Path.of(pathName);

    // read all Exported Messages from files
    var files = path.toFile().listFiles();
    if (files == null) {
      return map;
    }

    /*
     * In a perfect world, all the normal ETO clearinghouses will report promptly and the fieldMap contains all
     * potential Field stations.
     *
     * In reality, some clearinghouses take days to report, so that the fieldMap is not complete until then and hence,
     * can't really be used to warn about targets having non-P2P messages to Field stations in their out boxes
     */
    var requireFieldCallInFieldMap = cm.getAsBoolean(PickupKey.REQUIRE_FIELD_CALL_IN_FIELD_MAP);

    var reader = new ExportedMessageReader();
    for (File file : path.toFile().listFiles()) {
      if (file.isFile()) {
        if (!file.getName().toLowerCase().endsWith(".xml")) {
          continue;
        }

        var targetCall = getTargetCallFromFileName(file.getName());
        if (targetCall == null) {
          logger.error("could not find target name from file name: " + file.getName());
          continue;
        }
        var fileExportedMessages = reader.readAll(file.toPath());
        var set = map.getOrDefault(targetCall, new TreeSet<String>());
        for (var message : fileExportedMessages) {

          // begin vs end? let's autosense it!
          var fieldCall = message.from;
          if (fieldCall.equalsIgnoreCase(targetCall)) {
            fieldCall = message.to;
          }
          // set.add(fieldCall);

          // check if right message type
          var messageTypeName = "";
          boolean fireWrongType = false;
          if (expectedMessageType != null) {
            var messageType = WinlinkMessageMapper.getMessageType(message);
            if (messageType != expectedMessageType) {
              fireWrongType = true;
              messageTypeName = messageType.toString();
            }
          }

          // check if message is P2P if it should be P2P
          boolean fireP2P = false;
          if (shouldMessageBeP2P) {
            if (!message.isP2P) {
              fireP2P = true;
            }
          }

          // check if field in fieldMap
          boolean fireFieldNotInMap = false;

          if (fieldCall != null) {
            set.add(fieldCall);
            if (requireFieldCallInFieldMap) {
              var field = fieldMap.get(fieldCall);
              if (field == null) {
                fireFieldNotInMap = true;
              }
            }
          } // end fieldCall != null

          if (fireFieldNotInMap || fireP2P || fireWrongType) {
            var sb = new StringBuilder();
            sb.append("### to target: " + targetCall + " from field: " + fieldCall + ": ");
            if (fireFieldNotInMap) {
              sb.append(" field not in fieldMap, ");
            }
            if (fireP2P) {
              sb.append(" message not P2P, ");
            }
            if (fireWrongType) {
              sb.append(" wrong message type " + messageTypeName + ", ");
            }
            var s = sb.toString();
            s = s.substring(0, s.length() - 1);

            logger.warn(s);
          }

        } // end loop over messages
        map.put(targetCall, set);
      } // end file
    } // end file in directory

    logger.info("returning map with " + map.size() + " entries from directory:" + pathName);
    return map;

  }

  private String getTargetCallFromFileName(String fileName) {
    var candidates = new HashSet<String>();
    for (var call : targetMap.keySet()) {
      if (fileName.toUpperCase().contains(call)) {
        candidates.add(call);
      }
    }

    if (candidates.size() == 0) {
      logger.info("no candidate targets for file: " + fileName);
      return null;
    } else if (candidates.size() > 1) {
      logger.info(("multiple candidate targets for file: " + fileName + ", " + String.join(",", candidates)));
      return null;
    } else {
      return candidates.iterator().next();
    }
  }

  public void writeFields(Map<String, Field> fieldMap) {
    Path outputPath = Path.of(cm.getAsString(PickupKey.OUTPUT_PATH), "fields.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(Field.getHeaders());
      int rowCount = 0;
      for (var field : fieldMap.values()) {
        if (field.reservedCalls.size() == 0) {
          continue;
        }
        ++rowCount;
        writer.writeNext(field.getValues());
      }
      writer.close();
      logger.info("wrote " + rowCount + " Field Station summaries to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }

  public void writeTargets(Map<String, Target> targetMap) {
    Path outputPath = Path.of(cm.getAsString(PickupKey.OUTPUT_PATH), "targets.csv");

    try {
      File outputDirectory = new File(outputPath.toFile().getParent());
      if (!outputDirectory.exists()) {
        outputDirectory.mkdir();
      }

      CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toString()));
      writer.writeNext(Target.getHeaders());
      int rowCount = 0;
      for (var target : targetMap.values()) {
        ++rowCount;
        writer.writeNext(target.getValues());
      }
      writer.close();
      logger.info("wrote " + rowCount + " Target Station summaries to file: " + outputPath);
    } catch (Exception e) {
      logger.error("Exception writing file: " + outputPath + ", " + e.getLocalizedMessage());
    }
  }
}
