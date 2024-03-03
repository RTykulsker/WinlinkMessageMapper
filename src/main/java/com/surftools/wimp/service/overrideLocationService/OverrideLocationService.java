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

package com.surftools.wimp.service.overrideLocationService;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.location.LatLongPair;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.service.IService;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class OverrideLocationService implements IService {
  private static Logger logger = LoggerFactory.getLogger(OverrideLocationService.class);

  protected IConfigurationManager cm;
  protected Map<String, LatLongPair> callLocationMap;

  public OverrideLocationService(IConfigurationManager cm) {
    this.cm = cm;

    var overrideLocationPathName = cm.getAsString(Key.OVERRIDE_LOCATION_PATH);
    var overrideLocationPath = Path.of(overrideLocationPathName);
    callLocationMap = new HashMap<>();
    var fieldsList = ReadProcessor.readCsvFileIntoFieldsArray(overrideLocationPath);
    for (var fields : fieldsList) {
      if (fields != null && fields.length == 3) {
        var call = fields[0];
        var latitude = fields[1];
        var longitude = fields[2];
        var pair = new LatLongPair(latitude, longitude);
        var oldPair = callLocationMap.get(call);
        if (oldPair != null) {
          logger.warn("### oldPair: " + oldPair + ", newPair: " + pair + ", for call: " + call);
        }
        callLocationMap.put(call, pair);
      } // end if fields != null
    } // end for over fields
    logger.info("read " + callLocationMap.size() + " call/locations from file: " + overrideLocationPathName);
  }

  public LatLongPair getLocation(String call) {
    return callLocationMap.get(call);
  }

  @Override
  public String getName() {
    return "OverrideLocationService";
  }

}
