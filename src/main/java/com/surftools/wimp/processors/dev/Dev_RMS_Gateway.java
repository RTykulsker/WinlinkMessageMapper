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

package com.surftools.wimp.processors.dev;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.utils.config.IConfigurationManager;
import com.surftools.utils.location.LatLongPair;
import com.surftools.utils.location.LocationUtils;
import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.core.IMessageManager;
import com.surftools.wimp.core.IWritableTable;
import com.surftools.wimp.core.MessageType;
import com.surftools.wimp.message.CheckInMessage;
import com.surftools.wimp.message.ExportedMessage;
import com.surftools.wimp.processors.std.AbstractBaseProcessor;
import com.surftools.wimp.service.rmsGateway.RmsGatewayService;

/**
 * development effort to see if I can get outbound RMS gateway
 *
 *
 * @author bobt
 *
 */
public class Dev_RMS_Gateway extends AbstractBaseProcessor {
  private static Logger logger = LoggerFactory.getLogger(Dev_RMS_Gateway.class);

  private int ppCount = 0;

  private Map<String, Integer> callCountMap = new HashMap<>();

  private RmsGatewayService rmsGatewayService;

  private List<String> expectedDestinations;

  record Result(LocalDateTime dateTime, String messageId, String sender, String gateway, String destination,
      LatLongPair senderLocation, LatLongPair gatewayLocation, int distanceMiles, int freqMHz)
      implements IWritableTable {

    public static String[] getStaticHeaders() {
      return new String[] { "Date", "Time", "MessageId", "Sender", "Gateway", "Destination", //
          "Sender Lat", "Sender Lon", "RMS Lat", "RMS Lon", "Distance(Mi)", "Freq MHz" };
    }

    @Override
    public String[] getHeaders() {
      return getStaticHeaders();
    }

    @Override
    public String[] getValues() {
      var doubleFreq = Double.valueOf(freqMHz) / 1_000_000d;
      var freqString = String.format("%.6f", doubleFreq);
      var gatewayLat = gatewayLocation == null ? "" : gatewayLocation.getLatitude();
      var gatewayLon = gatewayLocation == null ? "" : gatewayLocation.getLongitude();

      return new String[] { dateTime.toLocalDate().toString(), dateTime.toLocalTime().toString(), //
          messageId, sender, gateway, destination, senderLocation.getLatitude(), senderLocation.getLongitude(), //
          gatewayLat, gatewayLon, String.valueOf(distanceMiles), freqString };
    }

    @Override
    public int compareTo(IWritableTable other) {
      var o = (Result) other;
      var cmp = dateTime.compareTo(o.dateTime);

      if (cmp != 0) {
        return cmp;
      }

      cmp = sender.compareTo(o.sender);
      return cmp;
    }

  }

  private List<IWritableTable> results = new ArrayList<>();

  @Override
  public void initialize(IConfigurationManager cm, IMessageManager mm) {
    super.initialize(cm, mm, logger);

    rmsGatewayService = new RmsGatewayService(cm);

    expectedDestinations = Arrays.asList(cm.getAsString(Key.EXPECTED_DESTINATIONS).split(","));
  }

  @Override
  public void process() {
    var messages = mm.getMessagesForType(MessageType.CHECK_IN);

    if (messages != null) {
      for (var message : messages) {
        CheckInMessage m = (CheckInMessage) message;
        ++ppCount;

        var call = m.from;
        var count = callCountMap.getOrDefault(call, Integer.valueOf(0));
        ++count;
        callCountMap.put(call, count);

        var serviceResult = rmsGatewayService.getLocationOfRmsGateway(call, m.messageId);

        var destination = findDestination(m);
        var distanceMiles = 0;
        if (serviceResult.isFound() && serviceResult.location() != null && serviceResult.location().isValid()) {
          distanceMiles = LocationUtils.computeDistanceMiles(m.formLocation, serviceResult.location());
        }

        /**
         * record Result(LocalDateTime dateTime, String messageId, String sender, String gateway, String destination,
         * LatLongPair senderLocation, LatLongPair gatewayLocation, int distanceMiles, String freqMHz)
         */

        var result = new Result(m.msgDateTime, m.messageId, m.from, serviceResult.gatewayCallsign(), destination,
            m.formLocation, serviceResult.location(), distanceMiles, serviceResult.frequency());
        results.add(result);
      } // end loop over messages
    } // end messages not null
  }

  private String findDestination(ExportedMessage m) {
    for (var to : m.toList.split(",")) {
      if (expectedDestinations.contains(to)) {
        return to;
      }
    }

    for (var cc : m.ccList.split(",")) {
      if (expectedDestinations.contains(cc)) {
        return cc;
      }
    }

    return m.to;
  }

  @Override
  public void postProcess() {
    var sb = new StringBuilder();
    sb.append("\nCheck In messages: " + ppCount + "\n");

    sb.append("CallCount map has " + callCountMap.size() + " entries\n");
    logger.info(sb.toString());

    writeTable("sender-gateways.csv", results);
  }

}
