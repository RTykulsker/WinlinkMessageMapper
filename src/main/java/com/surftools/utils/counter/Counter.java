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

package com.surftools.utils.counter;

import java.io.FileWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.opencsv.CSVWriter;

@SuppressWarnings("rawtypes")
public class Counter implements ICounter, Comparable {
  protected Map<Comparable, Integer> map = new HashMap<>();
  protected String name;

  public Counter() {
  }

  @Override
  public String toString() {
    return "name: " + name + ", map: "
        + map.keySet().stream().map(key -> key + "->" + map.get(key)).collect(Collectors.joining(",", "{", "}"));
  }

  public Counter(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public void increment(Comparable key) {
    var value = map.getOrDefault(key, Integer.valueOf(0));
    ++value;
    map.put(key, value);
  }

  @Override
  public void incrementNullSafe(Comparable key) {
    key = key == null ? "(null)" : key;
    var value = map.getOrDefault(key, Integer.valueOf(0));
    ++value;
    map.put(key, value);
  }

  @Override
  public void increment(Comparable key, int amount) {
    var value = map.getOrDefault(key, Integer.valueOf(0));
    value += amount;
    map.put(key, value);
  }

  @Override
  public Integer getCount(Comparable key) {
    var value = map.get(key);
    return value;
  }

  @Override
  public Iterator<Entry<Comparable, Integer>> getDescendingCountIterator() {
    var list = new ArrayList<>(map.entrySet());
    list.sort(java.util.Map.Entry.comparingByValue());
    Collections.reverse(list);
    return list.iterator();
  }

  @Override
  public Iterator<Entry<Comparable, Integer>> getAscendingCountIterator() {
    var list = new ArrayList<>(map.entrySet());
    list.sort(java.util.Map.Entry.comparingByValue());
    return list.iterator();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<Entry<Comparable, Integer>> getDescendingKeyIterator() {
    var list = new ArrayList<>(map.entrySet());
    list.sort(java.util.Map.Entry.comparingByKey());
    Collections.reverse(list);
    return list.iterator();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<Entry<Comparable, Integer>> getAscendingKeyIterator() {
    var list = new ArrayList<>(map.entrySet());
    list.sort(java.util.Map.Entry.comparingByKey());
    return list.iterator();
  }

  @Override
  public int getKeyCount() {
    return map.size();
  }

  @Override
  public int getValueTotal() {
    return map.values().stream().reduce(0, Integer::sum);
  }

  @Override
  public void merge(Counter subCounter) {
    if (subCounter == null) {
      return;
    }

    for (var entry : subCounter.map.entrySet()) {
      var key = entry.getKey();
      var value = entry.getValue();
      increment(key, value);
    }

  }

  @Override
  public Iterator<Entry<Comparable, Integer>> getIterator(CounterType type) {
    switch (type) {
    case ASCENDING_COUNT:
      return getAscendingCountIterator();

    case ASCENDING_KEY:
      return getAscendingKeyIterator();

    case DESCENDING_COUNT:
      return getDescendingCountIterator();

    case DESCENDING_KEY:
      return getDescendingKeyIterator();

    default:
      return null;
    }
  }

  @Override
  public void write(Path path, CounterType counterType) {
    try (var csvWriter = new CSVWriter(new FileWriter(path.toFile()))) {
      csvWriter.writeNext(new String[] { "Name", "Count" });

      var it = getIterator(counterType);
      while (it.hasNext()) {
        var next = it.next();
        csvWriter.writeNext(new String[] { next.getKey().toString(), String.valueOf(next.getValue()) });
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void write(Path path) {
    write(path, CounterType.DESCENDING_COUNT);
  }

  @Override
  public int compareTo(Object other) {
    var o = (Counter) other;
    return name.compareTo(o.name);
  }

}
