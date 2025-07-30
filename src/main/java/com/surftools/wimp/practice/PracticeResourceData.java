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

package com.surftools.wimp.practice;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.surftools.wimp.message.Ics213RRMessage;
import com.surftools.wimp.message.Ics213RRMessage.LineItem;
import com.surftools.wimp.processors.std.ReadProcessor;

public class PracticeResourceData {

  private Random rng;

  public record ResourceEntry(String key, String qty, String kind, String type, String description) {
    public static ResourceEntry fromArray(String[] array) {
      return new ResourceEntry(array[0], array[1], array[2], array[3], array[4]);
    }
  };

  private static final String SANTA_KEY = "SANTA";
  private final List<ResourceEntry> SANTA_WISH_LIST = new ArrayList<>(); // must not be static
  private final boolean CHRISTMAS_IN_JULY = false; // or any other month

  private final Map<String, List<ResourceEntry>> resourceMap = new LinkedHashMap<>();

  // static to persist across multiple ctors/months
  private static final List<List<ResourceEntry>> resourceBucket = new ArrayList<>();

  PracticeResourceData(Random _rng, String dirName) {
    rng = _rng;

    var dataFilePath = Path.of(dirName, "practice-resources.csv");

    try {
      var listOfStringArrays = ReadProcessor.readCsvFileIntoFieldsArray(dataFilePath);
      for (var array : listOfStringArrays) {
        var key = array[0];
        if (key.equals("Key")) {
          continue;
        }

        var resource = ResourceEntry.fromArray(array);
        if (key.equals(SANTA_KEY)) {
          SANTA_WISH_LIST.add(resource);
        } else {
          var list = resourceMap.getOrDefault(key, new ArrayList<ResourceEntry>());
          list.add(resource);
          resourceMap.put(key, list);
        }
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private List<ResourceEntry> getResources(LocalDate date) {
    if (date.getMonth() == Month.DECEMBER || CHRISTMAS_IN_JULY) {
      var resources = new ArrayList<ResourceEntry>(SANTA_WISH_LIST);
      Collections.shuffle(resources, rng);
      return resources;
    }

    if (resourceBucket.size() == 0) {
      resourceBucket.addAll(new ArrayList<List<ResourceEntry>>(resourceMap.values()));
      Collections.shuffle(resourceBucket, rng);
    }

    var resources = resourceBucket.remove(0);
    Collections.shuffle(resources, rng);
    return resources;
  }

  public List<LineItem> getRandomResources(LocalDate date, int desiredCount, Integer minInt, Integer maxInt) {
    var lineItems = new ArrayList<LineItem>(desiredCount);
    if (desiredCount == 0) {
      return lineItems;
    }
    var resources = getResources(date);
    var key = resources.get(0).key;

    var minQty = minInt == null ? 1 : minInt.intValue();
    var maxQty = maxInt == null ? 100 : maxInt.intValue();

    var timeString = rng.nextInt(10, 18) + ":00";

    for (var i = 0; i < Ics213RRMessage.MAX_LINE_ITEMS; ++i) {
      if (i < desiredCount) {
        var index = i % resources.size();
        var entry = resources.get(index);
        if (entry.qty.equals("0")) {
          var qty = String.valueOf(rng.nextInt(minQty, maxQty));
          entry = new ResourceEntry(key, qty, entry.kind, entry.type, entry.description);
        }
        var lineItem = new LineItem(entry.qty, entry.kind, entry.type, entry.description, //
            timeString, "", "");
        lineItems.add(lineItem);
      } else {
        lineItems.add(LineItem.EMPTY);
      }
    }
    return lineItems;
  }

}
