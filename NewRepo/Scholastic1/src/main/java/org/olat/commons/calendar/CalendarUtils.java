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

package org.olat.commons.calendar;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.RRule;

import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.model.KalendarRecurEvent;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;

public class CalendarUtils {
	static OLog log = Tracing.createLoggerFor(CalendarUtils.class);

	public static String getTimeAsString(final Date date, final Locale locale) {
		return DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(date);
	}

	public static String getDateTimeAsString(final Date date, final Locale locale) {
		return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(date);
	}

	/**
	 * Create a calendar instance that uses mondays or sundays as the first day of the week depending on the given locale and sets the week number 1 to the first week in
	 * the year that has four days of january.
	 * 
	 * @param local the locale to define if a week starts on sunday or monday
	 * @return a calendar instance
	 */
	public static Calendar createCalendarInstance(final Locale locale) {
		// use Calendar.getInstance(locale) that sets first day of week
		// according to locale or let user decide in GUI
		final Calendar cal = Calendar.getInstance(locale);
		// manually set min days to 4 as we are used to have it
		cal.setMinimalDaysInFirstWeek(4);
		return cal;
	}

	public static Calendar getStartOfWeekCalendar(final int year, final int weekOfYear, final Locale locale) {
		final Calendar cal = createCalendarInstance(locale);
		cal.clear();
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.WEEK_OF_YEAR, weekOfYear);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}

	/**
	 * Find all events in this calendar with the given subject.
	 * 
	 * @param calendar
	 * @param subject
	 * @return
	 */
	public static List findEvents(final Kalendar calendar, final String subject, final String location, final Date beginPeriod, final Date endPeriod,
			final boolean publicOnly) {
		final List results = new ArrayList();
		final Collection events = calendar.getEvents();
		String regExSubject = subject.replace("*", ".*");
		String regExLocation = location.replace("*", ".*");
		regExSubject = ".*" + regExSubject + ".*";
		regExLocation = ".*" + regExLocation + ".*";
		for (final Iterator iter = events.iterator(); iter.hasNext();) {
			final KalendarEvent event = (KalendarEvent) iter.next();
			if (publicOnly && event.getClassification() != KalendarEvent.CLASS_PUBLIC) {
				continue;
			}
			if (beginPeriod != null && event.getBegin().before(beginPeriod)) {
				continue;
			}
			if (endPeriod != null && event.getEnd().after(endPeriod)) {
				continue;
			}
			String eventSubject = event.getSubject().toLowerCase();
			eventSubject = eventSubject.replace("\n", " ");
			eventSubject = eventSubject.replace("\r", " ");
			if ((subject != null) && !subject.trim().isEmpty() && !eventSubject.matches(regExSubject.toLowerCase())) {
				log.debug("Does not add event because subject did not match eventSubject=" + eventSubject + "  regExSubject=" + regExSubject);
				continue;
			}
			if ((location != null) && !location.trim().isEmpty() && (event.getLocation() != null)) {
				String eventLocation = event.getLocation().toLowerCase();
				eventLocation = eventLocation.replace("\n", " ");
				eventLocation = eventLocation.replace("\r", " ");
				if (!eventLocation.matches(regExLocation.toLowerCase())) {
					log.debug("Does not add event because subject did not match eventLocation=" + eventLocation + "  regExLocation=" + regExLocation);
					continue;
				}
			}
			log.debug("add to results event.Location=" + event.getLocation() + " ,Subject=" + event.getSubject());
			results.add(event);
			final CalendarManager cm = CalendarManagerFactory.getInstance().getCalendarManager();
			final Date periodStart = beginPeriod == null ? event.getBegin() : beginPeriod;
			final long year = (long) 60 * (long) 60 * 24 * 365 * 1000;
			final Date periodEnd = endPeriod == null ? new Date(periodStart.getTime() + year) : endPeriod;
			final List<KalendarRecurEvent> lstEvnt = cm.getRecurringDatesInPeriod(periodStart, periodEnd, event);
			for (final KalendarRecurEvent recurEvent : lstEvnt) {
				if (publicOnly && event.getClassification() != KalendarEvent.CLASS_PUBLIC) {
					continue;
				}
				if (beginPeriod != null && event.getBegin().before(beginPeriod)) {
					continue;
				}
				if (endPeriod != null && event.getEnd().after(endPeriod)) {
					continue;
				}
				if (subject != null && !event.getSubject().matches(regExSubject)) {
					continue;
				}
				if (location != null && !event.getLocation().matches(regExLocation)) {
					continue;
				}
				if ((subject != null) && !subject.trim().isEmpty() && !event.getSubject().toLowerCase().matches(regExSubject.toLowerCase())) {
					continue;
				}
				if ((location != null) && !location.trim().isEmpty() && !event.getLocation().toLowerCase().matches(regExLocation.toLowerCase())) {
					continue;
				}
				results.add(recurEvent);
			}
		}
		Collections.sort(results);
		return results;
	}

	public static List listEventsForPeriod(final Kalendar calendar, final Date periodStart, final Date periodEnd) {
		final List periodEvents = new ArrayList();
		final Collection events = calendar.getEvents();
		for (final Iterator iter = events.iterator(); iter.hasNext();) {
			final KalendarEvent event = (KalendarEvent) iter.next();
			final CalendarManager cm = CalendarManagerFactory.getInstance().getCalendarManager();
			final List<KalendarRecurEvent> lstEvnt = cm.getRecurringDatesInPeriod(periodStart, periodEnd, event);
			for (final KalendarRecurEvent recurEvent : lstEvnt) {
				periodEvents.add(recurEvent);
			}
			if (event.getEnd().before(periodStart) || event.getBegin().after(periodEnd)) {
				continue;
			}
			periodEvents.add(event);
		}
		return periodEvents;
	}

	public static String getRecurrence(final String rule) {
		if (rule != null) {
			try {
				final Recur recur = new Recur(rule);
				final String frequency = recur.getFrequency();
				final WeekDayList wdl = recur.getDayList();
				final Integer interval = recur.getInterval();
				if ((wdl != null && wdl.size() > 0)) {
					// we only support one rule with daylist
					return KalendarEvent.WORKDAILY;
				} else if (interval != null && interval == 2) {
					// we only support one rule with interval
					return KalendarEvent.BIWEEKLY;
				} else {
					// native supportet rule
					return frequency;
				}
			} catch (final ParseException e) {
				Tracing.createLoggerFor(CalendarUtils.class).error("cannot restore recurrence rule", e);
			}
		}

		return null;
	}

	/**
	 * @param rule
	 * @return date of recurrence end
	 */
	public static Date getRecurrenceEndDate(final String rule) {
		final TimeZone tz = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone(java.util.Calendar.getInstance().getTimeZone().getID());

		if (rule != null) {
			try {
				final Recur recur = new Recur(rule);
				final Date dUntil = recur.getUntil();
				final DateTime dtUntil = dUntil == null ? null : new DateTime(dUntil.getTime());
				if (dtUntil != null) {
					dtUntil.setTimeZone(tz);
					return dtUntil;
				}
			} catch (final ParseException e) {
				Tracing.createLoggerFor(CalendarUtils.class).error("cannot restore recurrence rule", e);
			}
		}

		return null;
	}

	/**
	 * Build iCalendar-compliant recurrence rule
	 * 
	 * @param recurrence
	 * @param recurrenceEnd
	 * @return rrule
	 */
	public static String getRecurrenceRule(final String recurrence, final Date recurrenceEnd) {
		final TimeZone tz = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone(java.util.Calendar.getInstance().getTimeZone().getID());

		if (recurrence != null) { // recurrence available
			// create recurrence rule
			final StringBuilder sb = new StringBuilder();
			sb.append("FREQ=");
			if (recurrence.equals(KalendarEvent.WORKDAILY)) {
				// build rule for monday to friday
				sb.append(KalendarEvent.DAILY);
				sb.append(";");
				sb.append("BYDAY=MO,TU,WE,TH,FR");
			} else if (recurrence.equals(KalendarEvent.BIWEEKLY)) {
				// build rule for biweekly
				sb.append(KalendarEvent.WEEKLY);
				sb.append(";");
				sb.append("INTERVAL=2");
			} else {
				// normal supported recurrence
				sb.append(recurrence);
			}
			if (recurrenceEnd != null) {
				final DateTime recurEndDT = new DateTime(recurrenceEnd.getTime());
				recurEndDT.setTimeZone(tz);
				sb.append(";");
				sb.append(KalendarEvent.UNTIL);
				sb.append("=");
				sb.append(recurEndDT.toString());
			}
			try {
				final Recur recur = new Recur(sb.toString());
				final RRule rrule = new RRule(recur);
				return rrule.getValue();
			} catch (final ParseException e) {
				Tracing.createLoggerFor(CalendarUtils.class).error("cannot create recurrence rule: " + recurrence.toString(), e);
			}
		}

		return null;
	}

	/**
	 * Create list with excluded dates based on the exclusion rule.
	 * 
	 * @param recurrenceExc
	 * @return list with excluded dates
	 */
	public static List<Date> getRecurrenceExcludeDates(final String recurrenceExc) {
		final List<Date> recurExcDates = new ArrayList<Date>();
		if (recurrenceExc != null && !recurrenceExc.equals("")) {
			try {
				final net.fortuna.ical4j.model.ParameterList pl = new net.fortuna.ical4j.model.ParameterList();
				final ExDate exdate = new ExDate(pl, recurrenceExc);
				final DateList dl = exdate.getDates();
				for (final Object date : dl) {
					final Date excDate = (Date) date;
					recurExcDates.add(excDate);
				}
			} catch (final ParseException e) {
				Tracing.createLoggerFor(CalendarUtils.class).error("cannot restore recurrence exceptions", e);
			}
		}

		return recurExcDates;
	}

	/**
	 * Create exclusion rule based on list with dates.
	 * 
	 * @param dates
	 * @return string with exclude rule
	 */
	public static String getRecurrenceExcludeRule(final List<Date> dates) {
		if (dates != null && dates.size() > 0) {
			final DateList dl = new DateList();
			for (final Date date : dates) {
				final net.fortuna.ical4j.model.Date dd = new net.fortuna.ical4j.model.Date(date);
				dl.add(dd);
			}
			final ExDate exdate = new ExDate(dl);
			return exdate.getValue();
		}

		return null;
	}

}