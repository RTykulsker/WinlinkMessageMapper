/**

The MIT License (MIT)

Copyright (c) 2021, Robert Tykulsker

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

package com.surftools.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * choose a random object, based on weights
 *
 * if weight associated with an object is negative, throw IllegalArguementException during construction
 *
 * if weight associated with an object is zero, the object (for that weight) won't EVER be chosen
 *
 * @author bobt
 *
 */
public class WeightedRandomChooser {
  private static final Logger logger = LoggerFactory.getLogger(WeightedRandomChooser.class);

  private double sum;
  private List<WeightEntry> entryList;
  private Random rng;

  /**
   * don't allow public to construct without arguments
   */
  @SuppressWarnings("unused")
  private WeightedRandomChooser() {
  }

  /**
   * recommended constructor
   *
   * @param map
   * @param rng
   */
  public WeightedRandomChooser(Map<Object, Double> map, Random rng) {
    this.rng = rng == null ? new Random() : rng;
    init(map);
  }

  /**
   * equal-weighted constructor from list of objects
   *
   * @param objects
   */
  public WeightedRandomChooser(List<Object> objects, Random rng) {
    this.rng = rng == null ? new Random() : rng;

    Map<Object, Double> map = new HashMap<>(objects.size());
    for (Object object : objects) {
      Double count = map.getOrDefault(object, Double.valueOf(0));
      map.put(object, count + 1);
    }

    init(map);
  }

  /**
   * equal-weighted constructor from array of objects
   *
   * @param objects
   */
  public WeightedRandomChooser(Object[] objects, Random rng) {
    if (objects == null) {
      throw new IllegalArgumentException("null objects");
    }

    if (objects.length == 0) {
      throw new IllegalArgumentException("zero-length objects");
    }

    this.rng = rng == null ? new Random() : rng;
    Map<Object, Double> map = new HashMap<>(objects.length);
    for (Object object : objects) {
      Double count = map.getOrDefault(object, Double.valueOf(0));
      map.put(object, count + 1);
    }

    init(map);
  }

  /**
   * common initialization method
   *
   * @param map
   */
  private void init(Map<Object, Double> map) {
    if (map == null) {
      throw new IllegalArgumentException("null map");
    }

    entryList = new ArrayList<>(map.size());
    sum = 0;
    for (Map.Entry<Object, Double> mapEntry : map.entrySet()) {
      Object object = mapEntry.getKey();
      Double weight = mapEntry.getValue();

      if (weight < 0) {
        throw new IllegalArgumentException("negative weight: " + weight + " not allowed for object: " + object);
      }

      if (weight == 0) {
        logger.debug("skipping object: " + object + " because weight is zero");
        continue;
      }

      sum += weight;
      WeightEntry weightEntry = new WeightEntry(object, sum);
      entryList.add(weightEntry);
      logger.debug("added object: " + object + ", with weight: " + weight);
    }

    if (entryList.size() == 0) {
      throw new IllegalArgumentException("no objects available to choose");
    }

    logger.debug("initialized with " + entryList.size() + " objects");
  }

  public Object next() {
    double r = sum * rng.nextDouble();
    for (WeightEntry entry : entryList) {

      if (entry.sum() > r) {
        return entry.object();
      }
    }
    throw new RuntimeException("couldn't find an object");
  }

  record WeightEntry( //
      Object object, //
      double sum) {
  }
}
