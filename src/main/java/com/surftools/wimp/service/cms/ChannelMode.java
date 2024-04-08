/**

The MIT License (MIT)

Copyright (c) 2024, Robert Tykulsker

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

package com.surftools.wimp.service.cms;

public enum ChannelMode {
  AX_25_1200(0, "AX.25 1200", "Packet"), //
  AX_25_2400(1, "AX.25 2400", "Packet"), //
  AX_25_4800(2, "AX.25 4800", "Packet"), //
  AX_25_9600(3, "AX.25 9600", "Packet"), //
  AX_25_19200(4, "AX.25 19200", "Packet"), //
  AX_25_38400(5, "AX.25 38400", "Packet"), //
  // 6-10,"Future packet reserved
  PACTOR_P1(11, "P1 only", "Pactor"), //
  PACTOR_P1_2(12, "P1,2 only", "Pactor"), //
  PACTOR_P1_2_3(13, "P1,2,3", "Pactor"), //
  PACTOR_P2(14, "P2 only", "Pactor"), //
  PACTOR_P2_3(15, "P2,3 only", "Pactor"), //
  PACTOR_P3(16, "P3 only", "Pactor"), //
  PACTOR_P1_2_3_4(17, "P1,2,3,4", "Pactor"), //
  PACTOR_P2_2_4(18, "P2,3,4 only", "Pactor"), //
  PACTOR_P2_4(19, "P3,4 only", "Pactor"), //
  PACTOR_P4(20, "P4 only", "Pactor"), //

  WINMOR_500(21, "WINMOR 500", "Winmor"), //
  WINMOR_1600(22, "WINMOR 1600", "Winmor"), //

  ROBUST_PACKET(30, "SCS Robust Packet", "Robust Packet"), //

  ARDOP_200(40, "ARDOP 200", "Ardop"), //
  ARDOP_500(41, "ARDOP 500", "Ardop"), //
  ARDOP_1000(42, "ARDOP 1000", "Ardop"), //
  ARDOP_2000(43, "ARDOP 2000", "Ardop"), //
  ARDOP_2000_FM(44, "ARDOP 2000 FM", "Ardop"), //

  VARA_HF_2300(50, "VARA HF 2300", "VARA HF"), //
  VARA_FM_NARROW(51, "VARA FM narrow", "VARA FM"), //
  VARA_FM_WIDE(52, "VARA FM wide", "VARA FM"), //
  VARA_HF_500(53, "VARA HF 500", "VARA HF"), //
  VARA_HF_2700(54, "VARA HF 2700", "VARA HF"), //

  UNKNOWN(-1, "Unknown", "Unknown") //
  ;

  private final int mode;
  private final String label;
  private final String family;

  private ChannelMode(int mode, String label, String family) {
    this.mode = mode;
    this.label = label;
    this.family = family;
  }

  @Override
  public String toString() {
    return label;
  }

  public int mode() {
    return mode;
  }

  public String family() {
    return family;
  }

  public static ChannelMode of(int mode) {
    for (var e : values()) {
      if (e.mode == mode) {
        return e;
      }
    }
    return ChannelMode.UNKNOWN;
  }
}
