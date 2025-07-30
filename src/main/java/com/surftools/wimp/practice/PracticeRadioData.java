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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.surftools.wimp.message.Ics205Message.RadioEntry;

public class PracticeRadioData {
  private Random rng;

  private enum BONUS {
    WX, GMRS, TAC, MARINE
  }

  private static List<BONUS> bonusBucket = new ArrayList<>();

  public PracticeRadioData() {
    this.rng = new Random();
  }

  public PracticeRadioData(Random rng) {
    if (rng == null) {
      rng = new Random();
    }
    this.rng = rng;
  }

  enum BAND {
    VHF, UHF
  }

  enum OFFSET {
    POSITIVE, NEGATIVE
  }

  enum WIDTH {
    WIDE, NARROW
  }

  enum SQUELCH {
    TONE, TSQL
  }

  public List<RadioEntry> makeRadioEntries(int desiredCount) {
    List<RadioEntry> list = new ArrayList<>();

    if (desiredCount == 0) {
      return list;
    }

    // first entry will be a repeater, either vhf or uhf,
    var line1Band = rng.nextBoolean() ? BAND.VHF : BAND.UHF;
    var line1Offset = rng.nextBoolean() ? OFFSET.POSITIVE : OFFSET.NEGATIVE;
    var line1Width = rng.nextBoolean() ? WIDTH.WIDE : WIDTH.NARROW;
    var line1Squelch = rng.nextBoolean() ? SQUELCH.TONE : SQUELCH.TSQL;
    var entry = makeRepeater(1, line1Band, line1Offset, line1Width, line1Squelch);
    list.add(entry);

    if (desiredCount == 1) {
      return list;
    }

    var line2Band = line1Band == BAND.VHF ? BAND.UHF : BAND.VHF;
    var line2Width = line1Width == WIDTH.WIDE ? WIDTH.NARROW : WIDTH.WIDE;
    entry = makeSimplex(2, line2Band, line2Width);
    list.add(entry);
    if (desiredCount == 2) {
      return list;
    }

    if (bonusBucket.size() == 0) {
      bonusBucket.addAll(Arrays.asList(BONUS.values()));
      Collections.shuffle(bonusBucket, rng);
    }
    var bonus = bonusBucket.remove(0);

    var lineNumber = 3;
    switch (bonus) {
    case BONUS.WX:
      entry = makeWx(lineNumber);
      break;

    case BONUS.GMRS:
      entry = makeGmrs(lineNumber);
      break;

    case BONUS.TAC:
      entry = makeTac(lineNumber);
      break;

    case BONUS.MARINE:
      entry = makeMarine(lineNumber);
      break;

    default:
      break;
    }

    list.add(entry);
    if (desiredCount == 3) {
      return list;
    }

    var line4Offset = line1Offset == OFFSET.POSITIVE ? OFFSET.NEGATIVE : OFFSET.POSITIVE;
    var line4Squelch = line1Squelch == SQUELCH.TONE ? SQUELCH.TSQL : SQUELCH.TONE;
    entry = makeRepeater(4, line2Band, line4Offset, line2Width, line4Squelch);
    list.add(entry);
    if (desiredCount == 4) {
      return list;
    }

    // just make random repeaters and/or simplex channels
    for (var count = 5; count <= desiredCount; ++count) {
      var doRepeater = rng.nextBoolean();
      var band = rng.nextBoolean() ? BAND.VHF : BAND.UHF;
      var width = rng.nextBoolean() ? WIDTH.WIDE : WIDTH.NARROW;
      if (doRepeater) {
        var offset = rng.nextBoolean() ? OFFSET.POSITIVE : OFFSET.NEGATIVE;
        var squelch = rng.nextBoolean() ? SQUELCH.TONE : SQUELCH.TSQL;
        entry = makeRepeater(count, band, offset, width, squelch);
      } else {
        entry = makeSimplex(count, band, width);
      }
      list.add(entry);
    }

    return list;
  }

  private RadioEntry makeRepeater(int rowNumber, BAND band, OFFSET offset, WIDTH width, SQUELCH squelch) {
    var widthName = getWidthName(width);
    var rxFrequency = makeRxFrequency(band, offset);
    var txFrequency = makeTxFrequency(rxFrequency, band, offset);
    var txTone = getTone();
    var rxTone = getTone(txTone, squelch);
    var function = "Coordination";
    var channelName = "Repeater";
    var assignment = "amateur";
    var remarks = "";
    if (rowNumber == 1) {
      remarks = "Primary repeater";
    } else if (rowNumber == 4) {
      remarks = "Secondary repeater";
    } else {
      remarks = "Backup repeater";
    }
    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        function, channelName, assignment, //
        rxFrequency, widthName, rxTone, //
        txFrequency, widthName, txTone, //
        "A", remarks);

    return entry;
  }

  private String makeRxFrequency(BAND band, OFFSET offset) {
    double rxFreq = 0;
    if (band == BAND.VHF) {
      rxFreq = 147_000;
      // Repeaters above 147 typically use positive offsets, while those below 147 often use negative offsets.
      if (offset == OFFSET.POSITIVE) {
        rxFreq = rxFreq + (rng.nextInt(50) * 10);
      } else {
        rxFreq = rxFreq - (rng.nextInt(100) * 10);
      }
    } else {
      rxFreq = 442_000;
      // Repeaters above 442 generally use +5 offset, while those below 445 may use –5 offset
      if (offset == OFFSET.POSITIVE) {
        rxFreq = rxFreq + (rng.nextInt(60) * 50);
      } else {
        rxFreq = rxFreq - (rng.nextInt(60) * 50);
      }
    }
    // convert from Hz to double, to 3 digits as String;
    var d = rxFreq / 1000d;
    var rxFreqString = String.format("%.3f", d);
    return rxFreqString;
  }

  private String makeTxFrequency(String rxFreqString, BAND band, OFFSET offset) {
    var rxFreqDouble = Double.parseDouble(rxFreqString);
    var offsetDouble = band == BAND.VHF ? 0.6d : 5.0d;
    var multDouble = offset == OFFSET.POSITIVE ? +1 : -1;
    var txFreqDouble = rxFreqDouble + (multDouble * offsetDouble);
    var txFreqString = String.format("%.3f", txFreqDouble);
    return txFreqString;
  }

  private String getTone() {
    final var list = List
        .of( //
            "67.0", "69.3", "71.9", "74.4", "77.0", "79.7", "82.5", "85.4", "88.5", "91.5", "94.8", "97.4", "100.0",
            "103.5", "107.2", "110.9", "114.8", "118.8", "123.0", "127.3", "131.8", "136.5", "141.3", "146.2", "151.4",
            "156.7", "159.8", "162.2", "165.5", "167.9", "171.3", "173.8", "177.3", "179.9", "183.5", "186.2", "189.9",
            "192.8", "196.6", "199.5", "203.5", "206.5", "210.7", "218.1", "225.7", "229.1", "233.6", "241.8", "250.3",
            "254.1");
    var freq = list.get(rng.nextInt(list.size()));

    return freq;
  }

  private String getTone(String txTone, SQUELCH squelch) {
    return squelch == SQUELCH.TONE ? "" : txTone;
  }

  private String getWidthName(WIDTH width) {
    return width == WIDTH.WIDE ? "W" : "N";
  }

  private RadioEntry makeSimplex(int rowNumber, BAND band, WIDTH width) {

    final var twoMeterSimplex = List
        .of("146.400", "146.415", "146.430", "146.445", "146.460", "146.475", "146.490", "146.505", "146.520",
            "146.535", "146.550", "146.565", "146.580", "146.595", "147.405", "147.420", "147.435", "147.450",
            "147.465", "147.480", "147.495", "147.510", "147.525", "147.540", "147.555", "147.570", "147.585");

    final var seventySimplex = List
        .of("445.925", "445.950", "445.975", "446.000", "446.025", "446.050", "446.075", "446.100", "446.125",
            "446.150", "446.175");

    var list = band == BAND.VHF ? twoMeterSimplex : seventySimplex;
    var rxFreq = list.get(rng.nextInt(list.size()));

    var widthName = getWidthName(width);
    var function = "Tactical";
    var channelName = "Simplex";
    var assignment = "amateur";
    var remarks = "";
    if (rowNumber == 2) {
      remarks = "Primary simplex";
    } else {
      remarks = "Secondary simplex";
    }

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        function, channelName, assignment, //
        rxFreq, widthName, "", //
        rxFreq, widthName, "", //
        "A", remarks);
    return entry;
  }

  private RadioEntry makeWx(int rowNumber) {
    final var list = List
        .of( //
            "WX1 - 162.400", "WX2 - 162.425", "WX3 - 162.450", "WX4 - 162.475", "WX5 - 162.500", "WX6 - 162.525",
            "WX7 - 162.550");
    var data = list.get(rng.nextInt(list.size()));
    var fields = data.split(" - ");
    var name = fields[0];
    var rxFreq = fields[1];

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        "Information", name, "weather", //
        rxFreq, "W", "", //
        "", "", "", //
        "A", "Receive only. Do not Transmit!");
    return entry;
  }

  private RadioEntry makeGmrs(int rowNumber) {
    final var list = List
        .of(//
            "GMRS 1 - 462.5625", "GMRS 2 - 462.5875", "GMRS 3 - 462.6125", "GMRS 4 - 462.6375", "GMRS 5 - 462.6625",
            "GMRS 6 - 462.6875", "GMRS 7 - 462.7125", "GMRS 8 - 467.5625", "GMRS 9 - 467.5875", "GMRS 10 - 467.6125",
            "GMRS 11 - 467.6375", "GMRS 12 - 467.6625", "GMRS 13 - 467.6875", "GMRS 14 - 467.7125",
            "GMRS 15 - 462.5500", "GMRS 16 - 462.5750", "GMRS 17 - 462.6000", "GMRS 18 - 462.6250",
            "GMRS 19 - 462.6500", "GMRS 20 - 462.6750", "GMRS 21 - 462.7000", "GMRS 22 - 462.7250");
    var data = list.get(rng.nextInt(list.size()));
    var fields = data.split(" - ");
    var name = fields[0];
    var rxFreq = fields[1];

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        "Information", name, "GMRS", //
        rxFreq, "N", "", //
        rxFreq, "N", "", //
        "A", "Do not Transmit without GMRS license!");
    return entry;
  }

  private RadioEntry makeTac(int rowNumber) {
    // CTCSS tone 156.7 Hz is commonly used for analog FM operation
    final var list = List
        .of("VCALL10 - 155.7525", "VTAC11 - 151.1375", "VTAC12 - 154.4525", "VTAC13 - 158.7375", "VTAC14 - 159.4725",
            "VTAC17 - 161.8500", "UCALL40 - 453.2125", "UTAC41 - 453.4625", "UTAC42 - 453.7125", "UTAC43 - 453.8625");
    var data = list.get(rng.nextInt(list.size()));
    var fields = data.split(" - ");

    var name = fields[0];
    var rxFreq = fields[1];

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        "InterOp", name, "public safety", //
        rxFreq, "N", "156.7", //
        "", "N", "", //
        "A", "Receive only. Do not Transmit!");
    return entry;
  }

  private RadioEntry makeMarine(int rowNumber) {
    final var list = List
        .of(//
            "Channel 06 - 156.300 - Intership Safety",
            "Channel 09 - 156.450 - Boater Calling (Commercial & Non-Commercial)",
            "Channel 13 - 156.650 - Bridge-to-Bridge Navigation Safety",
            "Channel 16 - 156.800 - Distress, Safety, and Calling",
            "Channel 22A - 157.100 - Coast Guard Liaison & Safety Broadcasts",
            "Channel 68 - 156.425 - Non-Commercial Working Channel",
            "Channel 69 - 156.475 - Non-Commercial Working Channel",
            "Channel 71 - 156.575 - Non-Commercial Working Channel",
            "Channel 72 - 156.625 - Non-Commercial (Intership Only)", //
            "Channel 73 - 156.675 - Port Operations", //
            "Channel 77 - 156.875 - Port Operations (Intership Only)");
    var data = list.get(rng.nextInt(list.size()));
    var fields = data.split(" - ");

    var name = fields[0];
    var rxFreq = fields[1];
    var function = fields[2];

    var entry = new RadioEntry(rowNumber, "", String.valueOf(rowNumber), //
        "Marine", name, function, //
        rxFreq, "W", "", //
        "", "W", "", //
        "A", "Receive only. Do not Transmit!");
    return entry;
  }

  // public static void main(String[] args) {
  // var app = new PracticeRadioData(null);
  // for (var i = 0; i < 10; ++i) {
  // var list = app.makeRadioEntries(3);
  // list.stream().forEach(System.out::println);
  // System.out.println();
  // }
  // }

}
