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

package com.surftools.wimp.schedule;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.surftools.wimp.configuration.Key;
import com.surftools.wimp.processors.std.ReadProcessor;
import com.surftools.wimp.utils.config.IConfigurationManager;

public class ScheduleManager {
	private static final Logger logger = LoggerFactory.getLogger(ScheduleManager.class);

	protected TreeMap<LocalDate, ScheduleRecord> dateScheduleMap;
	protected List<ScheduleRecord> schedules;

	public ScheduleManager(IConfigurationManager cm) {

		dateScheduleMap = new TreeMap<LocalDate, ScheduleRecord>();
		schedules = readSchedule(cm);
		for (var schedule : schedules) {
			dateScheduleMap.put(schedule.date(), schedule);
		}
	}

	/**
	 * return list of ScheduleRecord
	 *
	 * @param cm
	 * @return
	 */
	protected List<ScheduleRecord> readSchedule(IConfigurationManager cm) {
		var list = new ArrayList<ScheduleRecord>();

		var schedulePathName = cm.getAsString(Key.PATH_SCHEDULE);
		var schedulePath = Path.of(schedulePathName);
		var fieldsList = ReadProcessor.readCsvFileIntoFieldsArray(schedulePath, ',', true, 1);
		for (var fields : fieldsList) {
			var scheduleRecord = ScheduleRecord.from(fields);
			list.add(scheduleRecord);
		}

		logger.info("read: " + list.size() + " entries from: " + schedulePathName);
		Collections.sort(list);

		return list;
	}

	/**
	 * return a list of previously read ScheduleRecords
	 *
	 * @return
	 */
	public List<ScheduleRecord> getSchedules() {
		return schedules;
	}

	/**
	 * check if given date has a valid schedule
	 *
	 * @param date
	 * @return
	 */
	public ScheduleCheckResult check(LocalDate date) {
		var thisOutput = dateScheduleMap.get(date);

		var lowerEntry = dateScheduleMap.lowerEntry(date);
		var lastOutput = lowerEntry == null ? null : dateScheduleMap.lowerEntry(date).getValue();

		var higherEntry = dateScheduleMap.higherEntry(date);
		var nextOutput = higherEntry == null ? null : dateScheduleMap.higherEntry(date).getValue();

		ScheduleRecord nextNextOutput = null;
		if (nextOutput != null) {
			var nextHigherEntry = dateScheduleMap.higherEntry(nextOutput.date());
			nextNextOutput = nextHigherEntry == null ? null : dateScheduleMap.higherEntry(nextOutput.date()).getValue();
		}
		return new ScheduleCheckResult(lastOutput, thisOutput, nextOutput, nextNextOutput);
	}
}
