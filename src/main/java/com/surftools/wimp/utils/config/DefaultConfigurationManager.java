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

package com.surftools.wimp.utils.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * generic implementation for an IConfigurationManager
 *
 * specific implementations will populate the map!
 *
 * @author bobt
 *
 */
public class DefaultConfigurationManager implements IWritableConfigurationManager {
  protected Map<IConfigurationKey, String> map;
  protected IConfigurationKey[] values = null;

  public DefaultConfigurationManager() {
    map = new LinkedHashMap<>();
  }

  public void setValues(IConfigurationKey[] values) {
    this.values = values;
  }

  protected IConfigurationKey fromString(String string) {
    if (values == null) {
      throw new RuntimeException("Values not specified");
    }

    for (IConfigurationKey key : values) {
      if (key.toString().equals(string)) {
        return key;
      }
    }
    return null;
  }

  @Override
  public String getAsString(IConfigurationKey key) {
    return map.get(key);
  }

  @Override
  public String getAsString(IConfigurationKey key, String defaultValue) {
    String stringValue = map.get(key);
    if (stringValue == null) {
      stringValue = defaultValue.toString();
    }

    return stringValue;
  }

  @Override
  public int getAsInt(IConfigurationKey key) {
    return getAsInt(key, null);
  }

  @Override
  public int getAsInt(IConfigurationKey key, Integer defaultValue) {
    String stringValue = map.get(key);
    if (stringValue == null) {
      stringValue = defaultValue.toString();
    }

    return Double.valueOf(stringValue).intValue();
  }

  @Override
  public boolean getAsBoolean(IConfigurationKey key) {
    return getAsBoolean(key, null);
  }

  @Override
  public boolean getAsBoolean(IConfigurationKey key, Boolean defaultValue) {
    String stringValue = map.get(key);
    if (stringValue == null) {
      if (defaultValue == null) {
        return false;
      }
      stringValue = defaultValue.toString();
    }

    if (stringValue.equals("1") || stringValue.equals("1.0")) {
      stringValue = Boolean.toString(true);
    } else if (stringValue.equals("0") || stringValue.equals("0.0")) {
      stringValue = Boolean.toString(false);
    }

    return Boolean.valueOf(stringValue);
  }

  @Override
  public void putString(IConfigurationKey key, String value) {
    map.put(key, value);
  }

  @Override
  public void putInt(IConfigurationKey key, Integer value) {
    map.put(key, String.valueOf(value));
  }

  @Override
  public void putBoolean(IConfigurationKey key, Boolean value) {
    map.put(key, String.valueOf(value));
  }
}
