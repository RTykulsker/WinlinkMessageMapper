/**

The MIT License (MIT)

Copyright (c) 2019, Robert Tykulsker

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

package com.surftools.winlinkMessageMapper.tool.neighborTool;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.config.impl.PropertyFileConfigurationManager;
import com.surftools.utils.location.LocationUtils;

/**
 * produce information about "neighbors" for selected targets
 *
 * @author bobt
 *
 */

public class NeighborTool {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }

  private static final Logger logger = LoggerFactory.getLogger(NeighborTool.class);

  private IConfigurationManager cm;

  @Option(name = "--configurationFile", usage = "path to configuration file", required = true)
  private String configurationFileName;

  public static void main(String[] args) {
    NeighborTool app = new NeighborTool();
    CmdLineParser parser = new CmdLineParser(app);
    try {
      parser.parseArgument(args);
      app.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  private void run() {
    try {
      cm = new PropertyFileConfigurationManager(configurationFileName, NeighborKey.values());

      var pathName = cm.getAsString(NeighborKey.PATH);
      logger.info("path: " + pathName);
      Path path = Paths.get(pathName);
      if (!Files.exists(path)) {
        logger.error("specified path: " + pathName + " does not exist");
        System.exit(1);
      } else {
        logger.debug("path: " + path);
      }

      var targetString = cm.getAsString(NeighborKey.TARGETS);
      var targetSet = new TreeSet<String>(Arrays.asList(targetString.split(",")));
      logger.info("targets: " + String.join(", ", targetSet));

      var dao = new Dao(cm);
      var positionMap = dao.makePositionMap();
      var boundsList = makeBounds(cm);

      // the money shot
      for (var call : targetSet) {
        var targetPosition = positionMap.get(call);
        logger.info("targetCall: " + call + ", position: " + targetPosition);

        // set up the bins in "definition" order
        var bins = new LinkedHashMap<DistanceBound, Set<RangedPosition>>();
        for (var bound : boundsList) {
          bins.put(bound, new TreeSet<RangedPosition>());
        }

        for (var position : positionMap.values()) {
          var distance = LocationUtils.computeDistanceMiles(targetPosition.location(), position.location());
          var bearing = LocationUtils.computBearing(targetPosition.location(), position.location());
          var rangedPosition = new RangedPosition(position, distance, bearing);

          for (var bound : boundsList) {
            if (bound.contains(distance)) {
              var set = bins.get(bound);
              set.add(rangedPosition);
              bins.put(bound, set);
            } // end if bound contains
          } // end loop over bounds
        } // end loop over positions
        dao.output(targetPosition, bins);
      } // end loop over targets

      logger.info("exiting");
    } catch (

    Exception e) {
      logger.error("Exception running, " + e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * bins can be overlapping and/or not-contiguous. Definately not mutually exclusive and completely exhaustive
   *
   * @param cm
   * @return
   */
  private List<DistanceBound> makeBounds(IConfigurationManager cm) {
    List<DistanceBound> list = new ArrayList<>();
    var distanceBoundsString = cm.getAsString(NeighborKey.DISTANCE_BOUNDS);
    var distanceBoundsArray = distanceBoundsString.split(";");
    for (var distanceBoundString : distanceBoundsArray) {
      try {
        var fields = distanceBoundString.split(",");
        var lower = Integer.valueOf(fields[0]);
        var upper = Integer.valueOf(fields[1]);
        var label = fields.length >= 3 ? fields[2] : null;
        var bound = new DistanceBound(lower, upper, label);
        logger.info("bound: " + bound);
        list.add(bound);
      } catch (Exception e) {
        logger.error("Exception parsing distance bound: " + distanceBoundsString + ", " + e.getLocalizedMessage());
        System.exit(1);
      }
    }
    return list;
  }
}
