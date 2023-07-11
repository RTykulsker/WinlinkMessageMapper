/**

The MIT License (MIT)

Copyright (c) 2023, Robert Tykulsker

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

package com.surftools.wimp.message;

public record DamageEntry(String description, String affected, String minor, String major, String destroyed,
    String total, String lossString, String lossAmount) {

  private static String[] names = { "n/a", "Houses", "Apt Complexes", "Mobile Homes", //
      "Residential High Rise", "Commercial High Rise", "Public Blgs", "Small Businesses", //
      "Factories/Industrial Complexes", "Roads", "Bridges", "Electrical Distribution", //
      "Schools"//
  };

  public static String getName(int index) {
    if (index < 0 || index >= names.length) {
      throw new IllegalArgumentException("index: " + index + " out of range");
    }
    return names[index];
  }
}