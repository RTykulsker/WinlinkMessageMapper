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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class MapService implements IMapService {

  public static final MapGeometry DEFAULT_MAP_GEOMETRY = new MapGeometry("40", "-100", "4", "19");

  private IMapService engine;

  public MapService() {
  }

  public MapService(IConfigurationManager cm, IMessageManager mm) {
    // TODO configuration
    var engineType = MapEngineType.LEAFLET;
    switch (engineType) {
    case LEAFLET:
      engine = new LeafletMapEngine(cm, mm);
      break;

    default:
      break;
    }
  }

  @Override
  public Set<String> getValidIconColors() {
    return engine.getValidIconColors();
  }

  @Override
  public String getInvalidIconColor() {
    return engine.getInvalidIconColor();
  }

  private int[] hsvToRgb(double h, double s, double v) {
    double c = v * s;
    double x = c * (1 - Math.abs((h / 60.0) % 2 - 1));
    double m = v - c;

    double rPrime = 0, gPrime = 0, bPrime = 0;

    if (h < 60) {
      rPrime = c;
      gPrime = x;
      bPrime = 0;
    } else if (h < 120) {
      rPrime = x;
      gPrime = c;
      bPrime = 0;
    } else if (h < 180) {
      rPrime = 0;
      gPrime = c;
      bPrime = x;
    } else if (h < 240) {
      rPrime = 0;
      gPrime = x;
      bPrime = c;
    } else if (h < 300) {
      rPrime = x;
      gPrime = 0;
      bPrime = c;
    } else {
      rPrime = c;
      gPrime = 0;
      bPrime = x;
    }

    int r = (int) Math.round((rPrime + m) * 255);
    int g = (int) Math.round((gPrime + m) * 255);
    int b = (int) Math.round((bPrime + m) * 255);

    return new int[] { r, g, b };
  }

  protected String rgbToHex(int[] rgb) {
    if (rgb == null || rgb.length != 3) {
      throw new IllegalArgumentException("rgb must have exactly 3 values");
    }

    return String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
  }

  @Override
  public Map<Integer, String> makeGradientMap(double startHue, double endHue, int nSteps) {
    var map = new LinkedHashMap<Integer, String>();
    if (nSteps <= 1) {
      var rgb = hsvToRgb(startHue, 1, 1);
      map.put(0, rgbToHex(rgb));
      return map;
    }

    for (var i = 0; i < nSteps; ++i) {
      var t = (double) i / (double) (nSteps - 1); // 0 to 1 inclusive
      var hue = startHue + (endHue - startHue) * t;
      var rgb = hsvToRgb(hue, 1, 1);
      map.put(i, rgbToHex(rgb));
    }

    return map;
  }

  @Override
  public void makeMap(MapContext mapContext) {
    engine.makeMap(mapContext);
  }

}
