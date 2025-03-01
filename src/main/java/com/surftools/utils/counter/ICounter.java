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

import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * generic Counter, inspired by Python's Counter
 *
 * @author bobt
 *
 */
public interface ICounter<K extends Comparable<K>> {

  /**
   * increment the count by 1
   *
   * @param key
   */
  public void increment(K key);

  /**
   * increment the count by 1
   *
   * @param key
   */
  public void incrementNullSafe(K key);

  /**
   * increment the count by the amount
   *
   * @param key
   * @param amount
   */
  public void increment(K key, int amount);

  /**
   * get value for key, if it exists
   *
   * @param key
   * @return Integer value or null
   */
  public Integer getCount(K key);

  /**
   * get number of distinct keys
   *
   * @return
   */
  public int getKeyCount();

  /**
   * get sum of amounts
   *
   * @return
   */
  public int getValueTotal();

  /**
   * return an iterator in descending-count order
   *
   * @return
   */
  public Iterator<Entry<K, Integer>> getDescendingCountIterator();

  /**
   * return an iterator in ascending-count order
   *
   * @return
   */
  public Iterator<Entry<K, Integer>> getAscendingCountIterator();

  /**
   * return an iterator in descending-key order
   *
   * @return
   */
  public Iterator<Entry<K, Integer>> getDescendingKeyIterator();

  /**
   * return an iterator in ascending-key order
   *
   * @return
   */
  public Iterator<Entry<K, Integer>> getAscendingKeyIterator();

  public Iterator<Entry<K, Integer>> getIterator(CounterType type);

  /**
   * merge in all value from subCounter
   *
   * @param subCounter
   */
  public void merge(Counter subCounter);

  /**
   * write the Counter keys and values with a default ordering
   *
   * @param path
   */
  public void write(Path path);

  /**
   * write the Counter keys and values with a specified ordering
   *
   * @param path
   * @param counterType
   */
  public void write(Path path, CounterType counterType);
}
