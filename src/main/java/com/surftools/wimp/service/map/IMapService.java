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

package com.surftools.wimp.service.map;

import java.util.Map;
import java.util.Set;

public interface IMapService {
  public void makeMap(MapContext mapContext);

  public Set<String> getValidIconColors();

  public String getInvalidIconColor();

  public Map<Integer, String> makeGradientMap(double startHue, double endHue, int nSteps);

  public static final Map<String, String> etoColorMap = Map
      .ofEntries(//
          Map.entry("ETO-01", "#cac428"), //
          Map.entry("ETO-02", "#2a81cb"), //
          Map.entry("ETO-03", "#cb2b32"), //
          Map.entry("ETO-04", "#2aad27"), //
          Map.entry("ETO-05", "#cb8427"), //
          Map.entry("ETO-06", "#9c2bcb"), //
          Map.entry("ETO-07", "#cac428"), //
          Map.entry("ETO-08", "#2aad27"), //
          Map.entry("ETO-09", "#cb2b32"), //
          Map.entry("ETO-10", "#2a81cb"), //
          Map.entry("ETO-CAN", "#9c2bcb"), //
          Map.entry("ETO-DX", "#ffd326"), //
          Map.entry("unknown", "#7b7b7b"));

  public static final Map<String, String> rgbMap = Map
      .ofEntries( //
          Map.entry("blue", "#2a81cb"), //
          Map.entry("gold", "#ffd326"), //
          Map.entry("red", "#cb2b32"), //
          Map.entry("green", "#2aad27"), //
          Map.entry("orange", "#cb8427"), //
          Map.entry("yellow", "#cac428"), //
          Map.entry("violet", "#9c2bcb"), //
          Map.entry("grey", "#7b7b7b"), //
          Map.entry("black", "#3d3d3d") //
      );

  public static final Set<String> ALL_ICON_COLORS = Set
      .of("blue", "gold", "red", "green", "orange", "yellow", "violet", "grey", "black");

  public static final Set<String> VALID_ICON_COLORS = Set // no grey
      .of("blue", "gold", "red", "green", "orange", "yellow", "violet", "black");
}
