/**

The MIT License (MIT)

Copyright (c) 2016, Robert Tykulsker

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

package com.surftools.wimp.utils.config.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.utils.config.DefaultConfigurationManager;
import com.surftools.wimp.utils.config.IConfigurationKey;

/**
 * implement IConfiguationManager, based on Properties file
 *
 * @author bobt
 *
 */
public class PropertyFileConfigurationManager extends DefaultConfigurationManager {

  private static final Logger logger = LoggerFactory.getLogger(PropertyFileConfigurationManager.class);

  public PropertyFileConfigurationManager(String configFileName, IConfigurationKey[] values) throws Exception {
    Properties properties = new Properties();
    try {
      logger.info("using configuration file: " + configFileName);
      properties.load(new FileInputStream(new File(configFileName)));

      var substitutionMap = new LinkedHashMap<String, String>();

      super.setValues(values);
      List<String> propertiesWithoutConfigurationKeys = new ArrayList<>();
      Enumeration<Object> propertyKeys = properties.keys();
      while (propertyKeys.hasMoreElements()) {
        String propertyKey = propertyKeys.nextElement().toString();

        if (propertyKey.startsWith("${") && propertyKey.endsWith("}")) {
          substitutionMap.put(propertyKey, properties.getProperty(propertyKey));
        } else {
          IConfigurationKey configurationKey = fromString(propertyKey);
          if (configurationKey == null) {
            propertiesWithoutConfigurationKeys.add(propertyKey);
          } else {
            String value = properties.getProperty(propertyKey);
            if (!value.isEmpty()) {
              map.put(configurationKey, value);
              logger.debug("config: key: " + configurationKey.toString() + " => " + value);
            } else {
              throw new RuntimeException("empty configuration parameter: " + configurationKey.toString());
            }
          }
        }
      }

      if (propertiesWithoutConfigurationKeys.size() > 0) {
        logger
            .warn("the following properties had no associated ConfigurationKey: "
                + String.join(", ", propertiesWithoutConfigurationKeys));
      }

      if (substitutionMap.size() > 0) {
        doSubstitutions(substitutionMap);
      }
    } catch (Exception e) {
      logger.error("Exception processing configuration file: " + configFileName + ": " + e.getMessage());
      throw e;
    }
  }

  /**
   * perform a substitution of ${values} within values
   */
  private void doSubstitutions(HashMap<String, String> substitutionMap) {
    final String regex = "\\$\\{([^}]++)\\}";
    final Pattern pattern = Pattern.compile(regex);

    // expand substitution map
    for (var entry : substitutionMap.entrySet()) {
      var oldValue = entry.getValue();
      var newValue = new String(oldValue);

      // https://stackoverflow.com/questions/17462146
      Matcher matcher = pattern.matcher(oldValue);

      while (matcher.find()) {
        String token = matcher.group(); // Ex: ${fizz}
        // String tokenKey = matcher.group(1); // Ex: fizz
        String replacementValue = null;

        if (substitutionMap.containsKey(token)) {
          replacementValue = substitutionMap.get(token);
          try {
            newValue = newValue.replaceFirst(Pattern.quote(token), replacementValue);
          } catch (Exception e) {
            logger.error("Exception for token: " + token + ", " + e.getMessage());
          }
        } else {
          logger.error("String contained an unsupported token: " + token);
        }
      }

      if (!newValue.equals(oldValue)) {
        substitutionMap.put(entry.getKey(), newValue);
      }

    }

    // expand main map
    for (var entry : map.entrySet()) {
      var oldValue = entry.getValue();
      var newValue = new String(oldValue);

      // https://stackoverflow.com/questions/17462146
      Matcher matcher = pattern.matcher(oldValue);

      while (matcher.find()) {
        String token = matcher.group(); // Ex: ${fizz}
        // String tokenKey = matcher.group(1); // Ex: fizz
        String replacementValue = null;

        if (substitutionMap.containsKey(token)) {
          replacementValue = substitutionMap.get(token);
          try {
            newValue = newValue.replaceFirst(Pattern.quote(token), replacementValue);
          } catch (Exception e) {
            logger.error("Exception for token: " + token + ", " + e.getMessage());
          }
        } else {
          logger.error("String contained an unsupported token: " + token);
        }
      }

      if (!newValue.equals(oldValue)) {
        map.put(entry.getKey(), newValue);
      }

    }
  }

}
