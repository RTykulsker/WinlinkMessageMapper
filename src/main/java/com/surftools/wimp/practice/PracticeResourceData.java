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

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.surftools.wimp.message.Ics213RRMessage.LineItem;

public class PracticeResourceData {
  private Map<ResourceType, List<ResourceEntry>> resourceMap = new HashMap<>();
  private Random rng;

  public record ResourceEntry(Integer qty, String kind, String type, String description) {
  };

  private final boolean CHRISTMAS_IN_JULY = false; // or any other month

  private final List<ResourceEntry> SANTA_WISH_LIST = List
      .of(//
          new ResourceEntry(1, "n/a", "n/a", "Wolf River Silver Bullet 1000"), //
          new ResourceEntry(1, "n/a", "n/a", "LDG Electronics AT-1000ProII Automatic Antenna Tuner"), //
          new ResourceEntry(1, "n/a", "n/a", "Heil Sound PRO 7 Headset"), //
          new ResourceEntry(2, "n/a", "n/a", "Bioenno Power BLF-1220A LiFePO4 Battery"), //
          new ResourceEntry(1, "n/a", "n/a", "RigExpert Antenna Analyzer AA-55ZOOM"), //
          new ResourceEntry(1, "n/a", "n/a", "Kenwood TS-990S HF/6 Meter Base Transceiver"), //
          new ResourceEntry(null, "n/a", "n/a", "DX Engineering Hat DXE-HAT") //
      );

  private enum ResourceType {
    DRUGS;
  }

  PracticeResourceData(Random _rng) {
    rng = _rng;

    var drugsList = List
        .of(//
            new ResourceEntry(null, "Bottle", "n/a", "Metformin (1000mg) Bottle of 60 tablets extended-release"), //
            new ResourceEntry(null, "Bottle", "n/a", "Glimepiride (4mg) Bottle of 60 tablets"), //
            new ResourceEntry(null, "Carton", "n/a", "Glargine (Insulin) (3ml) 5 - 100 unit pens"), //
            new ResourceEntry(null, "Box", "n/a", "Empagliflozin (12.5mg) 10 X 10 Tablets"), //
            new ResourceEntry(null, "Bottle", "n/a", "Pioglitazone (30mg) Bottle of 30 tablets."), //
            new ResourceEntry(null, "Bottle", "n/a", "Lisinopril (30mg) Bottle of 90 tablets"), //
            new ResourceEntry(null, "Bottle", "n/a", "Amlodipin (10mg Bottle of 30 tablets"), //
            new ResourceEntry(null, "Bottle", "n/a", "Atorvastatin (40mg) Bottle of 30 tablets") //
        );

    resourceMap.put(ResourceType.DRUGS, drugsList);
  }

  private List<ResourceEntry> getResources(LocalDate date) {
    if (date.getMonth() == Month.DECEMBER || CHRISTMAS_IN_JULY) {
      var resources = new ArrayList<ResourceEntry>(SANTA_WISH_LIST);
      Collections.shuffle(resources, rng);
      return resources;
    }

    var keys = new ArrayList<ResourceType>(resourceMap.keySet());
    Collections.shuffle(keys, rng);
    var key = keys.get(0);
    var resources = new ArrayList<ResourceEntry>(resourceMap.get(key));
    Collections.shuffle(resources, rng);

    return resources;
  }

  public List<LineItem> getRandomResources(LocalDate date, int desiredCount, Integer minInt, Integer maxInt) {
    var lineItems = new ArrayList<LineItem>(desiredCount);
    if (desiredCount == 0) {
      return lineItems;
    }
    var resources = getResources(date);

    var minQty = minInt == null ? 1 : minInt.intValue();
    var maxQty = maxInt == null ? 100 : maxInt.intValue();

    var timeString = rng.nextInt(10, 18) + ":00";

    var index = -1;
    do {
      index = (index + 1) % resources.size();
      var entry = resources.get(index);
      if (entry.qty == null) {
        var qty = rng.nextInt(minQty, maxQty);
        entry = new ResourceEntry(qty, entry.kind, entry.type, entry.description);
      }
      var lineItem = new LineItem(String.valueOf(entry.qty), entry.kind, entry.type, entry.description, //
          timeString, "", "");
      lineItems.add(lineItem);
    } while (lineItems.size() < desiredCount);
    return lineItems;
  }

  // public static void main(String[] args) {
  // var app = new PracticeResourceData(null);
  // var list = app.getRandomResources(LocalDateTime.now(), 3, null, null);
  // list.stream().forEach(System.out::println);
  // }

}
