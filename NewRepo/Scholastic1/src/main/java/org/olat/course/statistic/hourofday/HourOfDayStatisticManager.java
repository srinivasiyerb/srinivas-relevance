/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.course.statistic.hourofday;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.course.ICourse;
import org.olat.course.statistic.IStatisticManager;
import org.olat.course.statistic.StatisticDisplayController;
import org.olat.course.statistic.StatisticResult;
import org.olat.course.statistic.TotalAwareColumnDescriptor;

/**
 * Implementation of the IStatisticManager for 'hourofday' statistic
 * <P>
 * Initial Date: 12.02.2010 <br>
 * 
 * @author Stefan
 */
public class HourOfDayStatisticManager extends BasicManager implements IStatisticManager {

	/** the logging object used in this class **/
	private static final OLog log_ = Tracing.createLoggerFor(HourOfDayStatisticManager.class);

	@Override
	public StatisticResult generateStatisticResult(final UserRequest ureq, final ICourse course, final long courseRepositoryEntryKey) {
		final DBQuery dbQuery = DBFactory.getInstance().createQuery(
				"select businessPath,hour,value from org.olat.course.statistic.hourofday.HourOfDayStat sv " + "where sv.resId=:resId");
		dbQuery.setLong("resId", courseRepositoryEntryKey);

		final StatisticResult statisticResult = new StatisticResult(course, dbQuery.list());
		final List<String> columnHeaders = statisticResult.getColumnHeaders();
		if (columnHeaders != null && columnHeaders.size() > 1) {
			try {
				final int start = Integer.parseInt(columnHeaders.get(0));
				final int end = Integer.parseInt(columnHeaders.get(columnHeaders.size() - 1));
				final List<String> resultingColumnHeaders = new ArrayList<String>((end - start) + 1);
				for (int hour = start; hour <= end; hour++) {
					resultingColumnHeaders.add(String.valueOf(hour));
				}
				statisticResult.setColumnHeaders(resultingColumnHeaders);
			} catch (final NumberFormatException nfe) {
				log_.warn("generateStatisticResult: Got a NumberFormatException: " + nfe, nfe);
			}
		}
		return statisticResult;
	}

	@Override
	public ColumnDescriptor createColumnDescriptor(final UserRequest ureq, final int column, final String headerId) {
		if (column == 0) { return new DefaultColumnDescriptor("stat.table.header.node", 0, null, ureq.getLocale()); }
		String hourOfDayLocaled = headerId;

		try {
			final Calendar c = Calendar.getInstance(ureq.getLocale());
			c.setTime(new Date());
			c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(headerId));
			c.set(Calendar.MINUTE, 0);
			final DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, ureq.getLocale());
			hourOfDayLocaled = df.format(c.getTime());
		} catch (final RuntimeException re) {
			re.printStackTrace(System.out);
		}

		final TotalAwareColumnDescriptor cd = new TotalAwareColumnDescriptor(hourOfDayLocaled, column, StatisticDisplayController.CLICK_TOTAL_ACTION + column,
				ureq.getLocale(), ColumnDescriptor.ALIGNMENT_RIGHT);
		cd.setTranslateHeaderKey(false);
		return cd;
	}

	@Override
	public StatisticResult generateStatisticResult(final UserRequest ureq, final ICourse course, final long courseRepositoryEntryKey, final Date fromDate,
			final Date toDate) {
		return generateStatisticResult(ureq, course, courseRepositoryEntryKey);
	}

}
