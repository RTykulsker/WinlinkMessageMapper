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

package com.surftools.wimp.tool.adhoc;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.surftools.wimp.core.IWritableTable;

/**
 * convert a codeplug for Powerwerx DB-750X to TYT 390 UV Plus
 */
public class TytTool_Old {
  static {
    System.setProperty("logback.configurationFile", "src/main/resources/logback.xml");
  }
  private static final Logger logger = LoggerFactory.getLogger(TytTool_Old.class);

  @Option(name = "--inputFileName", usage = "path to input file name", required = true)
  private String inputFileName = "tyt_input.csv";

  @Option(name = "--outputFileName", usage = "path to output file name", required = true)
  private String outputFileName = "tyt_output.csv";

  public static void main(String[] args) {
    var tool = new TytTool_Old();
    CmdLineParser parser = new CmdLineParser(tool);
    try {
      parser.parseArgument(args);
      tool.run();
    } catch (Exception e) {
      System.err.println(e.getMessage());
      parser.printUsage(System.err);
    }
  }

  public void run() throws Exception {
    logger.info("begin tyt");

    var lines = readCsvFileIntoFieldsArray(Path.of(inputFileName), ',', false, 1);
    logger.info("read " + lines.size() + " lines from: " + inputFileName);

    var list = new ArrayList<Entry>(lines.size());
    for (var line : lines) {
      var entry = Entry.fromLine(line);
      if (entry.name().isEmpty()) {
        continue;
      }
      list.add(entry);
    }

    writeTable(outputFileName, list);
    logger.info("end tyt");
  }

  public static List<String[]> readCsvFileIntoFieldsArray(Path inputPath, char separator, boolean ignoreQuotes,
      int skipLines) {
    var list = new ArrayList<String[]>();

    if (!inputPath.toFile().exists()) {
      logger.warn("file: " + inputPath.toString() + " not found");
      return list;
    }

    var rowCount = -1;
    try {
      Reader reader = new FileReader(inputPath.toString());
      CSVParser parser = new CSVParserBuilder() //
          .withSeparator(separator) //
            .withIgnoreQuotations(ignoreQuotes) //
            .build();
      CSVReader csvReader = new CSVReaderBuilder(reader) //
          .withSkipLines(skipLines)//
            .withCSVParser(parser)//
            .build();
      rowCount = 1;
      String[] fields = null;
      while ((fields = csvReader.readNext()) != null) {
        ++rowCount;
        list.add(fields);
      }
    } catch (Exception e) {
      logger.error("Exception reading " + inputPath.toString() + ", row " + rowCount + ", " + e.getLocalizedMessage());
    }

    logger.info("returning: " + list.size() + " records from: " + inputPath.toString());
    return list;
  }

  public static void writeTable(String fileName, List<Entry> entries) {
    var path = Path.of(fileName);
    var messageCount = 0;
    // TODO Collections.sort(entries);
    try {
      CSVWriter writer = new CSVWriter(new FileWriter(path.toString()), ',', '\u0000', '"', "\r\n");
      // CSVWriter writer = new CSVWriter(new FileWriter(path.toString()));

      if (entries.size() > 0) {
        writer.writeNext(entries.get(0).getHeaders());
        for (IWritableTable e : entries) {
          if (e != null) {
            var values = e.getValues();
            if (values != null) {
              writer.writeNext(values);
              ++messageCount;
            }
          } else {
            continue;
          }
        }
      } else {
        writer.writeNext(new String[] { "No Data" });
      }

      writer.close();
      logger.info("wrote " + messageCount + " results to file: " + path.toString());
    } catch (Exception e) {
      logger.error("Exception writing file: " + path.toString() + ", " + e.getLocalizedMessage());
    }
  }

  record Entry(String channel, String name, String rxFreq, String txFreq, String toneMode, String txCtcss,
      String rxCtcss, String power) implements IWritableTable {

    public static Entry fromLine(String[] fields) {
      var channel = fields[0];
      var name = fields[1];
      var rxFreq = fields[2];
      var txFreq = fields[3];
      var toneMode = fields[8];
      var txCtcss = fields[9];
      var rxCtcss = fields[10];
      var power = fields[23];
      return new Entry(channel, name, rxFreq, txFreq, toneMode, txCtcss, rxCtcss, power);
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Entry) other;
      return Integer.valueOf(channel).compareTo(Integer.valueOf(o.channel));
    }

    @Override
    public String[] getHeaders() {
      return new String[] { "Channel Mode", "Channel Name", "RX Frequency(MHz)", "TX Frequency(MHz)", "Band Width",
          "Scan List", "Squelch", "RX Ref Frequency", "TX Ref Frequency", "TOT[s]", "TOT Rekey Delay[s]", "Power",
          "Admit Criteria", "Auto Scan", "Rx Only", "Lone Worker", "VOX", "Allow Talkaround", "Send GPS Info",
          "Receive GPS Info", "Private Call Confirmed", "Emergency Alarm Ack", "Data Call Confirmed", "Allow Interrupt",
          "DCDM Switch", "Leader/MS", "Emergency System", "Contact Name", "Group List", "Color Code", "Repeater Slot",
          "In Call Criteria", "Privacy", "Privacy No.", "GPS System", "CTCSS/DCS Dec", "CTCSS/DCS Enc",
          "Rx Signaling System", "Tx Signaling System", "QT Reverse", "Non-QT/DQT Turn-off Freq", "Display PTT ID",
          "Reverse Burst/Turn-off Code", "Decode 1", "Decode 2", "Decode 3", "Decode 4", "Decode 5", "Decode 6",
          "Decode 7", "Decode 8" };
    }

    @Override
    public String[] getValues() {
      // Tone; transmit tone, no rx tone
      // T Sql: both
      var my_rxCtcss = toneMode.equals("T Sql") ? rxCtcss.split(" ")[0] : "\"None\"";
      var my_txCtcss = toneMode.startsWith("T") ? txCtcss.split(" ")[0] : "\"None\"";
      return new String[] { "1", name, rxFreq, txFreq, "0", "0", "3", "0", "0", "6", "0", "2", "0", "0", "0", "0", "0",
          "0", "0", "0", "0", "0", "0", "0", "0", "1", "0", "1", "1", "1", "0", "0", "0", "0", "1", //
          my_rxCtcss, my_txCtcss, "0", "0", "0", "2", "1", "1", "0", "0", "0", "0", "0", "0", "0", "0" };
    }

  };
}
