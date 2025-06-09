package com.surftools.wimp.tool.adhoc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * convert a codeplug for Powerwerx DB-750X to TYT 390 UV Plus
 *
 */
public class TytTool {

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      throw new RuntimeException("usage: TytTool <inputFileName> <outputFileName");
    }
    var inputFileName = args[0];
    var outputFileName = args[1];

	var OUT_5 = "1,%s,%s,%s,0,0,3,0,0,6,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,0,0,0,0,1,%s,%s,0,0,0,2,1,1,0,0,0,0,0,0,0,0";
	var OUT_4 = "1,%s,%s,%s,0,0,3,0,0,6,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,0,0,0,0,1,%s,\"None\",0,0,0,2,1,1,0,0,0,0,0,0,0,0";
    var OUT_3 = "1,%s,%s,%s,0,0,3,0,0,6,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,0,0,0,0,1,\"None\",\"None\",0,0,0,2,1,1,0,0,0,0,0,0,0,0"; // simplex
    var OUT_2 = "1,%s,%s,0,0,0,3,0,0,6,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,0,0,0,0,1,\"None\",\"None\",0,0,0,2,1,1,0,0,0,0,0,0,0,0"; // beacon
	var formatMap = Map.of(5, OUT_5, 4, OUT_4, 3, OUT_3, 2, OUT_2);

    var sb = new StringBuilder();
    var lines = Files.readAllLines(Path.of(inputFileName));
    System.out.println("read " + lines.size() + " records from " + inputFileName);
    var lineNumber = 0;
    var outputRecords = 0;
    var format = formatMap.get(5);
    for (var inputLine : lines) {
      try {
        ++lineNumber;
        if (lineNumber == 1) {
          final var HEADER = """
              "Channel Mode","Channel Name","RX Frequency(MHz)","TX Frequency(MHz)","Band Width","Scan List","Squelch","RX Ref Frequency","TX Ref Frequency","TOT[s]","TOT Rekey Delay[s]","Power","Admit Criteria","Auto Scan","Rx Only","Lone Worker","VOX","Allow Talkaround","Send GPS Info","Receive GPS Info","Private Call Confirmed","Emergency Alarm Ack","Data Call Confirmed","Allow Interrupt","DCDM Switch","Leader/MS","Emergency System","Contact Name","Group List","Color Code","Repeater Slot","In Call Criteria","Privacy","Privacy No.","GPS System","CTCSS/DCS Dec","CTCSS/DCS Enc","Rx Signaling System","Tx Signaling System","QT Reverse","Non-QT/DQT Turn-off Freq","Display PTT ID","Reverse Burst/Turn-off Code","Decode 1","Decode 2","Decode 3","Decode 4","Decode 5","Decode 6","Decode 7","Decode 8"
              """;
          sb.append(HEADER);
          continue;
        }
        if (inputLine.contains(",,,,")) {
          continue;
        }
        var fields = inputLine.split(",");
        var channelName = fields[1].replaceAll("\"", "");
        var rxFreq = fields[2];
        var txFreq = fields[3];
        var toneMode = new String(fields[8]).replaceAll("\"", "");
        var txCtcss = fields[9].replaceAll("\"", "");
        var rxCtcss = fields[10].replaceAll("\"", "");
        // Tone; transmit tone, no rx tone, T Sql: both
        var rxTone = toneMode.equals("T Sql") ? rxCtcss.split(" ")[0] : "\"None\"";
        var txTone = toneMode.startsWith("T") ? txCtcss.split(" ")[0] : "\"None\"";
        var outputLine = String.format(format, channelName, rxFreq, txFreq, rxTone, txTone);
        sb.append(outputLine + "\n");
        ++outputRecords;
      } catch (Exception e) {
        System.err.println("Execption on line: " + lineNumber + ", " + inputLine + ", " + e.getMessage());
      }
    } // end loop over lines in input file
		// TODO; add "zones" for  GMRS/FRS with tone, local GMRS repeaters,
		// data/winlink, unassigned
    var locals = List
			.of(//
					new Object[] { "MIRO VHF", "147.160", "147.760", "146.2", "146.2" }, //
					new Object[] { "MIRO UHF", "440.150", "445.150", "103.5", "103.5" }, //
					new Object[] { "MI 2m D1", "147.440", "147.440"}, //
					new Object[] { "MI 2m D2", "146.600", "146.600"}, //
					new Object[] { "MI 70 D1", "445.900", "445.900" }, //
					new Object[] { "MI 70 D2", "445.925", "44525" }, //
					new Object[] { "CQ 2 m", "146.520", "146.520" }, //
					new Object[] { "CQ 70 cm", "446.000", "446.000" }, //
					new Object[] { "KC ARES", "147.080", "147.680", "103.5" }, //
					new Object[] { "RC KING", "441.550", "446.550", "103.5" }, //
					new Object[] { "PSRG", "146.960", "146.360", "103.5" }, //
					new Object[] { "COL CTR", "444.550", "449.550", "103.5" }, //
					new Object[] { "NOAA SEA", "162.550" }, //
					new Object[] { "NOAA PS", "162.425" }, //
					new Object[] { "W7MIR-10", "145.030", "145.030" }, //
					new Object[] { "W7MIR-11", "430.825", "430.825" }, //
					new Object[] { "APRS 2M", "144.390", "144.390" }, //
					new Object[] { "KM6SO-10", "145.530", "145.530" }, //
					new Object[] { "W7EFR-10", "144.950", "144.950" }, //
					new Object[] { "KCARES-9", "147.080", "147.680", "110.9" }, //
					new Object[] { "KCARES-BU", "147.000", "146.400", "103.5" }, //
					//
					new Object[] { "WX-01", "162.550000" }, //
					new Object[] { "WX-02", "162.400000" }, //
					new Object[] { "WX-03", "162.475000" }, //
					new Object[] { "WX-04", "162.425000" }, //
					new Object[] { "WX-05", "162.450000" }, //
					new Object[] { "WX-06", "162.500000" }, //
					new Object[] { "WX-07", "162.525000" }, //
					new Object[] { "WX-08", "161.650000" }, //
					new Object[] { "WX-09", "161.775000" }, //
					new Object[] { "WX-10", "161.750000" }, //
					new Object[] { "WX-11", "162.000000" }, //
					// GMRS repeaters, might need to tweak for narrow-band
					new Object[] { "E Tiger", "462.625", "467.625", "141.3", "141.3" }, //
					new Object[] { "Cap Hill", "462.600", "467.600", "141.3", "141.3" }, //
					new Object[] { "NSeattle", "462.675", "467.675", "141.3", "141.3" }, //
					new Object[] { "NBothell", "462.550", "467.550" }, //
					new Object[] { "PeacockH", "462.675", "467.675", "173.8", "173.8" }, //
					new Object[] { "Trilogy", "462.600", "467.600", "123.0", "23.0" } //
        );
    for (var vars : locals) {
      var outputLine = String.format(formatMap.get(vars.length), vars);
      sb.append(outputLine + "\n");
      ++outputRecords;
    }

    Files.write(Path.of(outputFileName), sb.toString().getBytes());
    System.out.println("wrote " + outputRecords + " TYT records to " + outputFileName);
    System.out.println("end tyt");
  }

}
