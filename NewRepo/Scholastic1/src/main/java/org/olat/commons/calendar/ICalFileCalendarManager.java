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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Clazz;
import net.fortuna.ical4j.model.property.Contact;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.ExDate;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.model.property.XProperty;

import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarConfig;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.model.KalendarEventLink;
import org.olat.commons.calendar.model.KalendarRecurEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.commons.calendar.ui.events.KalendarModifiedEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.FileUtils;
import org.olat.core.util.cache.n.CacheWrapper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.prefs.Preferences;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.ICourse;
import org.olat.group.BusinessGroup;

public class ICalFileCalendarManager extends BasicManager implements CalendarManager {

	private static final OLog log = Tracing.createLoggerFor(ICalFileCalendarManager.class);

	private final File fStorageBase;
	// o_clusterOK by:cg
	private final CacheWrapper calendarCache;

	private static final Clazz ICAL_CLASS_PRIVATE = new Clazz("PRIVATE");
	private static final Clazz ICAL_CLASS_PUBLIC = new Clazz("PUBLIC");
	private static final Clazz ICAL_CLASS_X_FREEBUSY = new Clazz("X-FREEBUSY");

	private static final String ICAL_X_OLAT_LINK = "X-OLAT-LINK";

	private static final String ICAL_X_OLAT_COMMENT = "X-OLAT-COMMENT";
	private static final String ICAL_X_OLAT_NUMPARTICIPANTS = "X-OLAT-NUMPARTICIPANTS";
	private static final String ICAL_X_OLAT_PARTICIPANTS = "X-OLAT-PARTICIPANTS";
	private static final String ICAL_X_OLAT_SOURCENODEID = "X-OLAT-SOURCENODEID";

	/** rule for recurring events */
	private static final String ICAL_RRULE = "RRULE";
	/** property to exclude events from recurrence */
	private static final String ICAL_EXDATE = "EXDATE";

	private final TimeZone tz;

	protected ICalFileCalendarManager(final File fStorageBase) {
		this.fStorageBase = fStorageBase;
		if (!fStorageBase.exists()) {
			if (!fStorageBase.mkdirs()) { throw new OLATRuntimeException("Error creating calendar base directory at: " + fStorageBase.getAbsolutePath(), null); }
		}
		createCalendarFileDirectories();
		// set parser to relax (needed for allday events
		// see http://sourceforge.net/forum/forum.php?thread_id=1253735&forum_id=368291
		System.setProperty("ical4j.unfolding.relaxed", "true");
		// initialize tiemzone
		tz = TimeZoneRegistryFactory.getInstance().createRegistry().getTimeZone(java.util.Calendar.getInstance().getTimeZone().getID());
		calendarCache = CoordinatorManager.getInstance().getCoordinator().getCacher().getOrCreateCache(this.getClass(), "calendar");
		UserDeletionManager.getInstance().registerDeletableUserData(this);
	}

	/**
	 * Check if a calendar already exists for the given id.
	 * 
	 * @param calendarID
	 * @param type
	 * @return
	 */
	@Override
	public boolean calendarExists(final String calendarType, final String calendarID) {
		return getCalendarFile(calendarType, calendarID).exists();
	}

	/**
	 * @see org.olat.calendar.CalendarManager#createClaendar(java.lang.String)
	 */
	@Override
	public Kalendar createCalendar(final String type, final String calendarID) {
		return new Kalendar(calendarID, type);
	}

	@Override
	public Kalendar getCalendar(final String type, final String calendarID) {
		// o_clusterOK by:cg
		final OLATResourceable calOres = OresHelper.createOLATResourceableType(getKeyFor(type, calendarID));
		final String callType = type;
		final String callCalendarID = calendarID;
		final Kalendar cal = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(calOres, new SyncerCallback<Kalendar>() {
			@Override
			public Kalendar execute() {
				return getCalendarFromCache(callType, callCalendarID);
			}
		});
		return cal;
	}

	protected Kalendar getCalendarFromCache(final String callType, final String callCalendarID) {
		final OLATResourceable calOres = OresHelper.createOLATResourceableType(getKeyFor(callType, callCalendarID));
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(calOres);

		final String key = getKeyFor(callType, callCalendarID);
		Kalendar cal = (Kalendar) calendarCache.get(key);
		if (cal == null) {
			cal = loadOrCreateCalendar(callType, callCalendarID);
			calendarCache.put(key, cal);
		}
		return cal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public OLATResourceable getOresHelperFor(final Kalendar cal) {
		return OresHelper.createOLATResourceableType(getKeyFor(cal.getType(), cal.getCalendarID()));
	}

	private String getKeyFor(final String type, final String calendarID) {
		return type + "_" + calendarID;
	}

	/**
	 * Internal load calendar file from filesystem.
	 */
	// o_clusterOK by:cg This must not be synchronized because the caller already synchronized
	private Kalendar loadCalendarFromFile(final String type, final String calendarID) {
		final Calendar calendar = readCalendar(type, calendarID);
		final Kalendar kalendar = createKalendar(type, calendarID, calendar);
		return kalendar;
	}

	private Kalendar createKalendar(final String type, final String calendarID, final Calendar calendar) {
		final Kalendar cal = new Kalendar(calendarID, type);
		for (final Iterator iter = calendar.getComponents().iterator(); iter.hasNext();) {
			final Component comp = (Component) iter.next();
			if (comp instanceof VEvent) {
				final VEvent vevent = (VEvent) comp;
				final KalendarEvent calEvent = getKalendarEvent(vevent);
				cal.addEvent(calEvent);
			} else if (comp instanceof VTimeZone) {
				log.info("createKalendar: VTimeZone Component is not supported and will not be added to calender");
				log.debug("createKalendar: VTimeZone=" + comp);
			} else {
				log.warn("createKalendar: unknown Component=" + comp);
			}
		}
		return cal;
	}

	/**
	 * Internal read calendar file from filesystem
	 */
	@Override
	public Calendar readCalendar(final String type, final String calendarID) {
		log.debug("readCalendar from file, type=" + type + "  calendarID=" + calendarID);
		final File calendarFile = getCalendarFile(type, calendarID);

		InputStream in = null;
		try {
			in = new BufferedInputStream(new FileInputStream(calendarFile));
		} catch (final FileNotFoundException fne) {
			throw new OLATRuntimeException("Not found: " + calendarFile, fne);
		}

		final CalendarBuilder builder = new CalendarBuilder();
		Calendar calendar = null;
		try {
			calendar = builder.build(in);
		} catch (final Exception e) {
			throw new OLATRuntimeException("Error parsing calendar file.", e);
		} finally {
			if (in != null) {
				FileUtils.closeSafely(in);
			}
		}
		return calendar;
	}

	@Override
	public Kalendar buildKalendarFrom(final String calendarContent, final String calType, final String calId) {
		Kalendar kalendar = null;
		final BufferedReader reader = new BufferedReader(new StringReader(calendarContent));
		final CalendarBuilder builder = new CalendarBuilder();
		try {
			final Calendar calendar = builder.build(reader);
			kalendar = createKalendar(calType, calId, calendar);
		} catch (final Exception e) {
			throw new OLATRuntimeException("Error parsing calendar file.", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					throw new OLATRuntimeException("Could not close reader after build calendar file.", e);
				}
			}
		}
		return kalendar;
	}

	/**
	 * Save a calendar. This method is not thread-safe. Must be called from a synchronized block. Be sure to have the newest calendar (reload calendar in synchronized
	 * block before safe it).
	 * 
	 * @param calendar
	 */
	// o_clusterOK by:cg only called by Junit-test
	@Override
	public boolean persistCalendar(final Kalendar kalendar) {
		final Calendar calendar = buildCalendar(kalendar);
		final boolean success = writeCalendarFile(calendar, kalendar.getType(), kalendar.getCalendarID());
		calendarCache.update(getKeyFor(kalendar.getType(), kalendar.getCalendarID()), kalendar);
		return success;
	}

	private boolean writeCalendarFile(final Calendar calendar, final String calType, final String calId) {
		final File fKalendarFile = getCalendarFile(calType, calId);
		OutputStream os = null;
		try {
			os = new BufferedOutputStream(new FileOutputStream(fKalendarFile, false));
			final CalendarOutputter calOut = new CalendarOutputter(false);
			calOut.output(calendar, os);
		} catch (final Exception e) {
			return false;
		} finally {
			FileUtils.closeSafely(os);
		}
		return true;
	}

	/**
	 * Delete calendar by type and id.
	 */
	@Override
	public boolean deleteCalendar(final String type, final String calendarID) {
		calendarCache.remove(getKeyFor(type, calendarID));
		final File fKalendarFile = getCalendarFile(type, calendarID);
		return fKalendarFile.delete();
	}

	@Override
	public File getCalendarICalFile(final String type, final String calendarID) {
		final File fCalendarICalFile = getCalendarFile(type, calendarID);
		if (fCalendarICalFile.exists()) {
			return fCalendarICalFile;
		} else {
			return null;
		}
	}

	/**
	 * @see org.olat.calendar.CalendarManager#findKalendarConfigForIdentity(org.olat.calendar.model.Kalendar, org.olat.core.gui.UserRequest)
	 */
	@Override
	public KalendarConfig findKalendarConfigForIdentity(final Kalendar kalendar, final UserRequest ureq) {
		final Preferences guiPreferences = ureq.getUserSession().getGuiPreferences();
		return (KalendarConfig) guiPreferences.get(KalendarConfig.class, kalendar.getCalendarID());
	}

	/**
	 * @see org.olat.calendar.CalendarManager#saveKalendarConfigForIdentity(org.olat.calendar.model.KalendarConfig, org.olat.calendar.model.Kalendar,
	 *      org.olat.core.gui.UserRequest)
	 */
	@Override
	public void saveKalendarConfigForIdentity(final KalendarConfig config, final Kalendar kalendar, final UserRequest ureq) {
		final Preferences guiPreferences = ureq.getUserSession().getGuiPreferences();
		guiPreferences.putAndSave(KalendarConfig.class, kalendar.getCalendarID(), config);
	}

	private Calendar buildCalendar(final Kalendar kalendar) {
		final Calendar calendar = new Calendar();
		// add standard propeties
		calendar.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 1.0//EN"));
		calendar.getProperties().add(Version.VERSION_2_0);
		calendar.getProperties().add(CalScale.GREGORIAN);
		for (final Iterator<KalendarEvent> iter = kalendar.getEvents().iterator(); iter.hasNext();) {
			final KalendarEvent kEvent = iter.next();
			final VEvent vEvent = getVEvent(kEvent);
			calendar.getComponents().add(vEvent);
		}
		return calendar;
	}

	private VEvent getVEvent(final KalendarEvent kEvent) {
		VEvent vEvent = new VEvent();
		if (!kEvent.isAllDayEvent()) {
			// regular VEvent
			final DateTime dtBegin = new DateTime(kEvent.getBegin());
			dtBegin.setTimeZone(tz);
			final DateTime dtEnd = new DateTime(kEvent.getEnd());
			dtEnd.setTimeZone(tz);
			vEvent = new VEvent(dtBegin, dtEnd, kEvent.getSubject());
		} else {
			// AllDay VEvent
			final net.fortuna.ical4j.model.Date dtBegin = new net.fortuna.ical4j.model.Date(kEvent.getBegin());
			// adjust end date: ICal end dates for all day events are on the next day
			final Date adjustedEndDate = new Date(kEvent.getEnd().getTime() + (1000 * 60 * 60 * 24));
			final net.fortuna.ical4j.model.Date dtEnd = new net.fortuna.ical4j.model.Date(adjustedEndDate);
			vEvent = new VEvent(dtBegin, dtEnd, kEvent.getSubject());
			vEvent.getProperties().getProperty(Property.DTSTART).getParameters().add(Value.DATE);
			vEvent.getProperties().getProperty(Property.DTEND).getParameters().add(Value.DATE);
		}

		if (kEvent.getCreated() > 0) {
			final Created created = new Created(new DateTime(kEvent.getCreated()));
			vEvent.getProperties().add(created);
		}

		if ((kEvent.getCreatedBy() != null) && !kEvent.getCreatedBy().trim().isEmpty()) {
			final Contact contact = new Contact();
			contact.setValue(kEvent.getCreatedBy());
			vEvent.getProperties().add(contact);
		}

		if (kEvent.getLastModified() > 0) {
			final LastModified lastMod = new LastModified(new DateTime(kEvent.getLastModified()));
			vEvent.getProperties().add(lastMod);
		}

		// Uid
		final PropertyList vEventProperties = vEvent.getProperties();
		vEventProperties.add(new Uid(kEvent.getID()));

		// clazz
		switch (kEvent.getClassification()) {
			case KalendarEvent.CLASS_PRIVATE:
				vEventProperties.add(ICAL_CLASS_PRIVATE);
				break;
			case KalendarEvent.CLASS_PUBLIC:
				vEventProperties.add(ICAL_CLASS_PUBLIC);
				break;
			case KalendarEvent.CLASS_X_FREEBUSY:
				vEventProperties.add(ICAL_CLASS_X_FREEBUSY);
				break;
			default:
				vEventProperties.add(ICAL_CLASS_PRIVATE);
				break;
		}

		// location
		if (kEvent.getLocation() != null) {
			vEventProperties.add(new Location(kEvent.getLocation()));
		}

		// event links
		final List kalendarEventLinks = kEvent.getKalendarEventLinks();
		if ((kalendarEventLinks != null) && !kalendarEventLinks.isEmpty()) {
			for (final Iterator iter = kalendarEventLinks.iterator(); iter.hasNext();) {
				final KalendarEventLink link = (KalendarEventLink) iter.next();
				final StringBuilder linkEncoded = new StringBuilder(200);
				linkEncoded.append(link.getProvider());
				linkEncoded.append("§");
				linkEncoded.append(link.getId());
				linkEncoded.append("§");
				linkEncoded.append(link.getDisplayName());
				linkEncoded.append("§");
				linkEncoded.append(link.getURI());
				linkEncoded.append("§");
				linkEncoded.append(link.getIconCssClass());
				final XProperty linkProperty = new XProperty(ICAL_X_OLAT_LINK, linkEncoded.toString());
				vEventProperties.add(linkProperty);
			}
		}

		if (kEvent.getComment() != null) {
			vEventProperties.add(new XProperty(ICAL_X_OLAT_COMMENT, kEvent.getComment()));
		}
		if (kEvent.getNumParticipants() != null) {
			vEventProperties.add(new XProperty(ICAL_X_OLAT_NUMPARTICIPANTS, Integer.toString(kEvent.getNumParticipants())));
		}
		if (kEvent.getParticipants() != null) {
			final StringBuffer strBuf = new StringBuffer();
			final String[] participants = kEvent.getParticipants();
			for (final String participant : participants) {
				strBuf.append(participant);
				strBuf.append("§");
			}
			vEventProperties.add(new XProperty(ICAL_X_OLAT_PARTICIPANTS, strBuf.toString()));
		}
		if (kEvent.getSourceNodeId() != null) {
			vEventProperties.add(new XProperty(ICAL_X_OLAT_SOURCENODEID, kEvent.getSourceNodeId()));
		}

		// recurrence
		final String recurrence = kEvent.getRecurrenceRule();
		if (recurrence != null && !recurrence.equals("")) {
			try {
				final Recur recur = new Recur(recurrence);
				final RRule rrule = new RRule(recur);
				vEventProperties.add(rrule);
			} catch (final ParseException e) {
				Tracing.createLoggerFor(getClass()).error("cannot create recurrence rule: " + recurrence.toString(), e);
			}
		}
		// recurrence exclusions
		final String recurrenceExc = kEvent.getRecurrenceExc();
		if (recurrenceExc != null && !recurrenceExc.equals("")) {
			final ExDate exdate = new ExDate();
			try {
				exdate.setValue(recurrenceExc);
				vEventProperties.add(exdate);
			} catch (final ParseException e) {
				e.printStackTrace();
			}
		}

		return vEvent;
	}

	/**
	 * Build a KalendarEvent out of a source VEvent.
	 * 
	 * @param event
	 * @return
	 */
	private KalendarEvent getKalendarEvent(final VEvent event) {
		// subject
		final String subject = event.getSummary().getValue();
		// start
		final Date start = event.getStartDate().getDate();
		final Duration dur = event.getDuration();
		// end
		Date end = null;
		if (dur != null) {
			end = dur.getDuration().getTime(event.getStartDate().getDate());
		} else {
			end = event.getEndDate().getDate();
		}

		// check all day event first
		boolean isAllDay = false;
		final Parameter dateParameter = event.getProperties().getProperty(Property.DTSTART).getParameters().getParameter(Value.DATE.getName());
		if (dateParameter != null) {
			isAllDay = true;
		}

		if (isAllDay) {
			// adjust end date: ICal sets end dates to the next day
			end = new Date(end.getTime() - (1000 * 60 * 60 * 24));
		}

		final KalendarEvent calEvent = new KalendarEvent(event.getUid().getValue(), subject, start, end);
		calEvent.setAllDayEvent(isAllDay);

		// classification
		final Clazz classification = event.getClassification();
		if (classification != null) {
			final String sClass = classification.getValue();
			int iClassification = KalendarEvent.CLASS_PRIVATE;
			if (sClass.equals(ICAL_CLASS_PRIVATE.getValue())) {
				iClassification = KalendarEvent.CLASS_PRIVATE;
			} else if (sClass.equals(ICAL_CLASS_X_FREEBUSY.getValue())) {
				iClassification = KalendarEvent.CLASS_X_FREEBUSY;
			} else if (sClass.equals(ICAL_CLASS_PUBLIC.getValue())) {
				iClassification = KalendarEvent.CLASS_PUBLIC;
			}
			calEvent.setClassification(iClassification);
		}
		// created/last modified
		final Created created = event.getCreated();
		if (created != null) {
			calEvent.setCreated(created.getDate().getTime());
		}
		// created/last modified
		final Contact contact = (Contact) event.getProperty(Property.CONTACT);
		if (contact != null) {
			calEvent.setCreatedBy(contact.getValue());
		}

		final LastModified lastModified = event.getLastModified();
		if (lastModified != null) {
			calEvent.setLastModified(lastModified.getDate().getTime());
		}

		// location
		final Location location = event.getLocation();
		if (location != null) {
			calEvent.setLocation(location.getValue());
		}

		// links if any
		final List linkProperties = event.getProperties(ICAL_X_OLAT_LINK);
		final List kalendarEventLinks = new ArrayList();
		for (final Iterator iter = linkProperties.iterator(); iter.hasNext();) {
			final XProperty linkProperty = (XProperty) iter.next();
			if (linkProperty != null) {
				final String encodedLink = linkProperty.getValue();
				final StringTokenizer st = new StringTokenizer(encodedLink, "§", false);
				if (st.countTokens() == 4) {
					final String provider = st.nextToken();
					final String id = st.nextToken();
					final String displayName = st.nextToken();
					final String uri = st.nextToken();
					String iconCss = "";
					// migration: iconCss has been added later, check if available first
					if (st.hasMoreElements()) {
						iconCss = st.nextToken();
					}
					final KalendarEventLink eventLink = new KalendarEventLink(provider, id, displayName, uri, iconCss);
					kalendarEventLinks.add(eventLink);
				}
			}
		}
		calEvent.setKalendarEventLinks(kalendarEventLinks);

		final Property comment = event.getProperty(ICAL_X_OLAT_COMMENT);
		if (comment != null) {
			calEvent.setComment(comment.getValue());
		}

		final Property numParticipants = event.getProperty(ICAL_X_OLAT_NUMPARTICIPANTS);
		if (numParticipants != null) {
			calEvent.setNumParticipants(Integer.parseInt(numParticipants.getValue()));
		}

		final Property participants = event.getProperty(ICAL_X_OLAT_PARTICIPANTS);
		if (participants != null) {
			final StringTokenizer strTok = new StringTokenizer(participants.getValue(), "§", false);
			final String[] parts = new String[strTok.countTokens()];
			for (int i = 0; strTok.hasMoreTokens(); i++) {
				parts[i] = strTok.nextToken();
			}
			calEvent.setParticipants(parts);
		}

		final Property sourceNodId = event.getProperty(ICAL_X_OLAT_SOURCENODEID);
		if (sourceNodId != null) {
			calEvent.setSourceNodeId(sourceNodId.getValue());
		}

		// recurrence
		if (event.getProperty(ICAL_RRULE) != null) {
			calEvent.setRecurrenceRule(event.getProperty(ICAL_RRULE).getValue());
		}

		// recurrence exclusions
		if (event.getProperty(ICAL_EXDATE) != null) {
			calEvent.setRecurrenceExc(event.getProperty(ICAL_EXDATE).getValue());
		}

		return calEvent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public KalendarEvent getRecurringInPeriod(final Date periodStart, final Date periodEnd, final KalendarEvent kEvent) {
		final boolean isRecurring = isRecurringInPeriod(periodStart, periodEnd, kEvent);
		KalendarEvent recurEvent = null;

		if (isRecurring) {
			final java.util.Calendar periodStartCal = java.util.Calendar.getInstance();
			final java.util.Calendar eventBeginCal = java.util.Calendar.getInstance();

			periodStartCal.setTime(periodStart);
			eventBeginCal.setTime(kEvent.getBegin());

			final Long duration = kEvent.getEnd().getTime() - kEvent.getBegin().getTime();

			final java.util.Calendar beginCal = java.util.Calendar.getInstance();
			beginCal.setTime(kEvent.getBegin());
			beginCal.set(java.util.Calendar.YEAR, periodStartCal.get(java.util.Calendar.YEAR));
			beginCal.set(java.util.Calendar.MONTH, periodStartCal.get(java.util.Calendar.MONTH));
			beginCal.set(java.util.Calendar.DAY_OF_MONTH, periodStartCal.get(java.util.Calendar.DAY_OF_MONTH));

			recurEvent = kEvent.clone();
			recurEvent.setBegin(beginCal.getTime());
			recurEvent.setEnd(new Date(beginCal.getTime().getTime() + duration));
		}

		return recurEvent;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isRecurringInPeriod(final Date periodStart, final Date periodEnd, final KalendarEvent kEvent) {
		final DateList recurDates = getRecurringsInPeriod(periodStart, periodEnd, kEvent);
		return (recurDates != null && !recurDates.isEmpty());
	}

	private DateList getRecurringsInPeriod(final Date periodStart, final Date periodEnd, final KalendarEvent kEvent) {
		DateList recurDates = null;
		final String recurrenceRule = kEvent.getRecurrenceRule();
		if (recurrenceRule != null && !recurrenceRule.equals("")) {
			try {
				final Recur recur = new Recur(recurrenceRule);
				final net.fortuna.ical4j.model.Date periodStartDate = new net.fortuna.ical4j.model.Date(periodStart);
				final net.fortuna.ical4j.model.Date periodEndDate = new net.fortuna.ical4j.model.Date(periodEnd);
				final net.fortuna.ical4j.model.Date eventStartDate = new net.fortuna.ical4j.model.Date(kEvent.getBegin());
				recurDates = recur.getDates(eventStartDate, periodStartDate, periodEndDate, Value.DATE);
			} catch (final ParseException e) {
				Tracing.createLoggerFor(getClass()).error("cannot restore recurrence rule: " + recurrenceRule, e);
			}

			final String recurrenceExc = kEvent.getRecurrenceExc();
			if (recurrenceExc != null && !recurrenceExc.equals("")) {
				try {
					final ExDate exdate = new ExDate();
					// expected date+time format:
					// 20100730T100000
					// unexpected all-day format:
					// 20100730
					// see OLAT-5645
					if (recurrenceExc.length() > 8) {
						exdate.setValue(recurrenceExc);
					} else {
						exdate.getParameters().replace(Value.DATE);
						exdate.setValue(recurrenceExc);
					}
					for (final Object date : exdate.getDates()) {
						if (recurDates.contains(date)) {
							recurDates.remove(date);
						}
					}
				} catch (final ParseException e) {
					Tracing.createLoggerFor(getClass()).error("cannot restore excluded dates for this recurrence: " + recurrenceExc, e);
				}
			}
		}

		return recurDates;
	}

	/**
	 * @see org.olat.commons.calendar.CalendarManager#getRecurringDatesInPeriod(java.util.Date, java.util.Date, org.olat.commons.calendar.model.KalendarEvent)
	 */
	@Override
	public List<KalendarRecurEvent> getRecurringDatesInPeriod(final Date periodStart, final Date periodEnd, final KalendarEvent kEvent) {
		final List<KalendarRecurEvent> lstDates = new ArrayList<KalendarRecurEvent>();
		final DateList recurDates = getRecurringsInPeriod(periodStart, periodEnd, kEvent);
		if (recurDates == null) { return lstDates; }

		for (final Object obj : recurDates) {
			final net.fortuna.ical4j.model.Date date = (net.fortuna.ical4j.model.Date) obj;

			KalendarRecurEvent recurEvent;

			final java.util.Calendar eventStartCal = java.util.Calendar.getInstance();
			eventStartCal.clear();
			eventStartCal.setTime(kEvent.getBegin());

			final java.util.Calendar eventEndCal = java.util.Calendar.getInstance();
			eventEndCal.clear();
			eventEndCal.setTime(kEvent.getEnd());

			final java.util.Calendar recurStartCal = java.util.Calendar.getInstance();
			recurStartCal.clear();
			recurStartCal.setTimeInMillis(date.getTime());

			final long duration = kEvent.getEnd().getTime() - kEvent.getBegin().getTime();

			final java.util.Calendar beginCal = java.util.Calendar.getInstance();
			beginCal.clear();
			beginCal.set(recurStartCal.get(java.util.Calendar.YEAR), recurStartCal.get(java.util.Calendar.MONTH), recurStartCal.get(java.util.Calendar.DATE),
					eventStartCal.get(java.util.Calendar.HOUR_OF_DAY), eventStartCal.get(java.util.Calendar.MINUTE), eventStartCal.get(java.util.Calendar.SECOND));

			final java.util.Calendar endCal = java.util.Calendar.getInstance();
			endCal.clear();
			endCal.setTimeInMillis(beginCal.getTimeInMillis() + duration);
			if (kEvent.getBegin().compareTo(beginCal.getTime()) == 0) {
				continue; // prevent doubled events
			}
			final Date recurrenceEnd = CalendarUtils.getRecurrenceEndDate(kEvent.getRecurrenceRule());
			if (kEvent.isAllDayEvent() && recurrenceEnd != null && recurStartCal.getTime().after(recurrenceEnd)) {
				continue; // workaround for ical4j-bug in all day events
			}
			recurEvent = new KalendarRecurEvent(kEvent.getID(), kEvent.getSubject(), new Date(beginCal.getTimeInMillis()), new Date(endCal.getTimeInMillis()));
			recurEvent.setSourceEvent(kEvent);
			lstDates.add(recurEvent);
		}
		return lstDates;
	}

	@Override
	public File getCalendarFile(final String type, final String calendarID) {
		return new File(fStorageBase, "/" + type + "/" + calendarID + ".ics");
	}

	private void createCalendarFileDirectories() {
		File fDirectory = new File(fStorageBase, "/" + TYPE_USER);
		fDirectory.mkdirs();
		fDirectory = new File(fStorageBase, "/" + TYPE_GROUP);
		fDirectory.mkdirs();
		fDirectory = new File(fStorageBase, "/" + TYPE_COURSE);
		fDirectory.mkdirs();
	}

	@Override
	public KalendarRenderWrapper getPersonalCalendar(final Identity identity) {
		final Kalendar cal = getCalendar(CalendarManager.TYPE_USER, identity.getName());
		final KalendarRenderWrapper calendarWrapper = new KalendarRenderWrapper(cal);
		final KalendarConfig config = new KalendarConfig(identity.getName(), KalendarRenderWrapper.CALENDAR_COLOR_BLUE, true);
		calendarWrapper.setKalendarConfig(config);
		return calendarWrapper;
	}

	@Override
	public KalendarRenderWrapper getImportedCalendar(final Identity identity, final String calendarName) {
		final Kalendar cal = getCalendar(CalendarManager.TYPE_USER, ImportCalendarManager.getImportedCalendarID(identity, calendarName));
		final KalendarRenderWrapper calendarWrapper = new KalendarRenderWrapper(cal);
		final KalendarConfig config = new KalendarConfig(calendarName, KalendarRenderWrapper.CALENDAR_COLOR_BLUE, true);
		calendarWrapper.setKalendarConfig(config);
		return calendarWrapper;
	}

	@Override
	public KalendarRenderWrapper getGroupCalendar(final BusinessGroup businessGroup) {
		final Kalendar cal = getCalendar(CalendarManager.TYPE_GROUP, businessGroup.getResourceableId().toString());
		final KalendarRenderWrapper calendarWrapper = new KalendarRenderWrapper(cal);
		final KalendarConfig config = new KalendarConfig(businessGroup.getName(), KalendarRenderWrapper.CALENDAR_COLOR_ORANGE, true);
		calendarWrapper.setKalendarConfig(config);
		return calendarWrapper;
	}

	@Override
	public KalendarRenderWrapper getCourseCalendar(final ICourse course) {
		final Kalendar cal = getCalendar(CalendarManager.TYPE_COURSE, course.getResourceableId().toString());
		final KalendarRenderWrapper calendarWrapper = new KalendarRenderWrapper(cal);
		final KalendarConfig config = new KalendarConfig(course.getCourseTitle(), KalendarRenderWrapper.CALENDAR_COLOR_GREEN, true);
		calendarWrapper.setKalendarConfig(config);
		return calendarWrapper;
	}

	@Override
	public void deletePersonalCalendar(final Identity identity) {
		deleteCalendar(CalendarManager.TYPE_USER, identity.getName());
	}

	@Override
	public void deleteGroupCalendar(final BusinessGroup businessGroup) {
		deleteCalendar(CalendarManager.TYPE_GROUP, businessGroup.getResourceableId().toString());
	}

	@Override
	public void deleteCourseCalendar(final ICourse course) {
		deleteCalendar(CalendarManager.TYPE_COURSE, course.getResourceableId().toString());
	}

	@Override
	public void deleteUserData(final Identity identity, final String newDeletedUserName) {
		deletePersonalCalendar(identity);
		Tracing.logDebug("Personal calendar deleted for identity=" + identity, this.getClass());
	}

	/**
	 * @see org.olat.commons.calendar.CalendarManager#addEventTo(org.olat.commons.calendar.model.Kalendar, org.olat.commons.calendar.model.KalendarEvent)
	 */
	@Override
	public boolean addEventTo(final Kalendar cal, final KalendarEvent kalendarEvent) {
		final OLATResourceable calOres = getOresHelperFor(cal);
		final Boolean persistSuccessful = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(calOres, new SyncerCallback<Boolean>() {
			@Override
			public Boolean execute() {
				final Kalendar loadedCal = getCalendarFromCache(cal.getType(), cal.getCalendarID());
				loadedCal.addEvent(kalendarEvent);
				final boolean successfullyPersist = persistCalendar(loadedCal);
				return new Boolean(successfullyPersist);
			}
		});
		// inform all controller about calendar change for reload
		CoordinatorManager.getInstance().getCoordinator().getEventBus()
				.fireEventToListenersOf(new KalendarModifiedEvent(cal), OresHelper.lookupType(CalendarManager.class));
		return persistSuccessful.booleanValue();
	}

	/**
	 * @see org.olat.commons.calendar.CalendarManager#removeEventFrom(org.olat.commons.calendar.model.Kalendar, org.olat.commons.calendar.model.KalendarEvent)
	 */
	@Override
	public boolean removeEventFrom(final Kalendar cal, final KalendarEvent kalendarEvent) {
		final OLATResourceable calOres = getOresHelperFor(cal);
		final Boolean removeSuccessful = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(calOres, new SyncerCallback<Boolean>() {
			@Override
			public Boolean execute() {
				final Kalendar loadedCal = getCalendarFromCache(cal.getType(), cal.getCalendarID());
				loadedCal.removeEvent(kalendarEvent);
				final boolean successfullyPersist = persistCalendar(loadedCal);
				return new Boolean(successfullyPersist);
			}
		});
		// inform all controller about calendar change for reload
		CoordinatorManager.getInstance().getCoordinator().getEventBus()
				.fireEventToListenersOf(new KalendarModifiedEvent(cal), OresHelper.lookupType(CalendarManager.class));
		return removeSuccessful.booleanValue();
	}

	/**
	 * @see org.olat.commons.calendar.CalendarManager#updateEventFrom(org.olat.commons.calendar.model.Kalendar, org.olat.commons.calendar.model.KalendarEvent)
	 */
	@Override
	public boolean updateEventFrom(final Kalendar cal, final KalendarEvent kalendarEvent) {
		final OLATResourceable calOres = getOresHelperFor(cal);
		final Boolean updatedSuccessful = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(calOres, new SyncerCallback<Boolean>() {
			@Override
			public Boolean execute() {
				return updateEventAlreadyInSync(cal, kalendarEvent);
			}
		});
		return updatedSuccessful.booleanValue();
	}

	/**
	 * @see org.olat.commons.calendar.CalendarManager#updateEventFrom(org.olat.commons.calendar.model.Kalendar, org.olat.commons.calendar.model.KalendarEvent)
	 */
	@Override
	public boolean updateEventAlreadyInSync(final Kalendar cal, final KalendarEvent kalendarEvent) {
		final OLATResourceable calOres = getOresHelperFor(cal);
		CoordinatorManager.getInstance().getCoordinator().getSyncer().assertAlreadyDoInSyncFor(calOres);
		final Kalendar loadedCal = getCalendarFromCache(cal.getType(), cal.getCalendarID());
		loadedCal.removeEvent(kalendarEvent); // remove old event
		loadedCal.addEvent(kalendarEvent); // add changed event
		final boolean successfullyPersist = persistCalendar(loadedCal);
		// inform all controller about calendar change for reload
		CoordinatorManager.getInstance().getCoordinator().getEventBus()
				.fireEventToListenersOf(new KalendarModifiedEvent(cal), OresHelper.lookupType(CalendarManager.class));
		return successfullyPersist;
	}

	/**
	 * Load a calendar when a calendar exists or create a new one. This method is not thread-safe. Must be called from synchronized block!
	 * 
	 * @param callType
	 * @param callCalendarID
	 * @return
	 */
	protected Kalendar loadOrCreateCalendar(final String callType, final String callCalendarID) {
		if (!calendarExists(callType, callCalendarID)) {
			return createCalendar(callType, callCalendarID);
		} else {
			return loadCalendarFromFile(callType, callCalendarID);
		}
	}

}
