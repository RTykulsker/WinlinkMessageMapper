/**

The MIT License (MIT)

Copyright (c) 2026, Robert Tykulsker

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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Draw from a bag of items.
 *
 * When bag is empty, refill.
 *
 * @param <T>
 */
public class RenewableBag<T> implements Iterator<T> {
  private final List<T> list;
  private final Random rng;
  private int index;
  private int N;

  public RenewableBag(Collection<T> collection) {
    this(collection, null);
  }

  public RenewableBag(Collection<T> collection, Random rng) {
    list = new ArrayList<T>(collection);
    this.rng = rng;
    index = 0;
    N = list.size();
    if (rng != null) {
      Collections.shuffle(list, rng);
    }
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public T next() {
    var t = list.get(index);
    ++index;
    if (index == N) {
      index = 0;
      if (rng != null) {
        Collections.shuffle(list, rng);
      }
    }
    return t;
  }

  // public static void main(String[] args) {
  // var list = List.of("a", "b", "c");
  // var bag = new RenewableBag<>(list, new Random());
  // var result = new ArrayList<String>();
  // for (int i = 0; i < 20; ++i) {
  // result.add(bag.next());
  // }
  // System.out.println(String.join(", ", result));
  // }

}
