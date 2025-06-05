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

    var OUT_5 = "1,%s,%s,%s,0,0,3,0,0,6,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,0,0,0,0,1,%s,%s,0,0,0,2,1,1,0,0,0,0,0,0,0,0";// repeater
    var OUT_3 = "1,%s,%s,%s,0,0,3,0,0,6,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,0,0,0,0,1,\"None\",\"None\",0,0,0,2,1,1,0,0,0,0,0,0,0,0"; // simplex
    var OUT_2 = "1,%s,%s,0,0,0,3,0,0,6,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,1,1,1,0,0,0,0,1,\"None\",\"None\",0,0,0,2,1,1,0,0,0,0,0,0,0,0"; // beacon
    var formatMap = Map.of(5, OUT_5, 3, OUT_3, 2, OUT_2);

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
      // TODO; add "zones" for local, WX, local WX, local GMRS repeaters, data/winlink, unassigned
    var locals = List
        .of(new Object[] { "MIRO VHF", "147.160", "147.760", "146.2", "146.2" },
            new Object[] { "MIRO UHF", "440.150", "445.150", "103.5", "103.5" },
            new Object[] { "KM6SO-10", "145.530", "145.530" } //
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
