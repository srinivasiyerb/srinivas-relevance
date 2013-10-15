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
 * Copyright (c) 2005-2008 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * All rights reserved.
 * <p>
 */
package org.olat.commons.calendar;

import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.CalendarController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.LogDelegator;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.NotificationHelper;
import org.olat.core.util.notifications.NotificationsHandler;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.Publisher;
import org.olat.core.util.notifications.Subscriber;
import org.olat.core.util.notifications.SubscriptionInfo;
import org.olat.core.util.notifications.items.SubscriptionListItem;
import org.olat.core.util.notifications.items.TitleItem;
import org.olat.core.util.resource.OresHelper;
import org.olat.course.CourseModule;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.notifications.NotificationsUpgradeHelper;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * Description:<br>
 * Implementation for NotificationHandler of calendars. For more information see JIRA ticket OLAT-3861.
 * <P>
 * Initial Date: 22.12.2008 <br>
 * 
 * @author bja
 */
public class CalendarNotificationHandler extends LogDelegator implements NotificationsHandler {

	private static final String CSS_CLASS_CALENDAR_ICON = "o_calendar_icon";

	@Override
	public SubscriptionInfo createSubscriptionInfo(final Subscriber subscriber, final Locale locale, final Date compareDate) {
		SubscriptionInfo si = null;
		final Publisher p = subscriber.getPublisher();
		final Date latestNews = p.getLatestNewsDate();

		// do not try to create a subscription info if state is deleted - results in
		// exceptions, course
		// can't be loaded when already deleted
		if (NotificationsManager.getInstance().isPublisherValid(p) && compareDate.before(latestNews)) {
			final Long id = p.getResId();
			final String type = p.getSubidentifier();

			try {
				final Translator translator = Util.createPackageTranslator(this.getClass(), locale);

				String calType = null;
				String title = null;
				if (type.equals(CalendarController.ACTION_CALENDAR_COURSE)) {
					final String displayName = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(id);
					calType = CalendarManager.TYPE_COURSE;
					title = translator.translate("cal.notifications.header.course", new String[] { displayName });
				} else if (type.equals(CalendarController.ACTION_CALENDAR_GROUP)) {
					final BusinessGroup group = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(id, false);
					calType = CalendarManager.TYPE_GROUP;
					title = translator.translate("cal.notifications.header.group", new String[] { group.getName() });
				}

				if (calType != null) {
					final Formatter form = Formatter.getInstance(locale);
					si = new SubscriptionInfo(new TitleItem(title, CSS_CLASS_CALENDAR_ICON), null);

					String bPath;
					if (StringHelper.containsNonWhitespace(p.getBusinessPath())) {
						bPath = p.getBusinessPath();
					} else if ("CalendarManager.course".equals(p.getResName())) {
						try {
							final OLATResourceable ores = OresHelper.createOLATResourceableInstance(CourseModule.getCourseTypeName(), p.getResId());
							final RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(ores, true);
							bPath = "[RepositoryEntry:" + re.getKey() + "]";// Fallback
						} catch (final Exception e) {
							logError("Error processing calendar notifications of publisher:" + p.getKey(), e);
							return NotificationsManager.getInstance().getNoSubscriptionInfo();
						}
					} else {
						// cannot make link without business path
						return NotificationsManager.getInstance().getNoSubscriptionInfo();
					}

					final Kalendar cal = CalendarManagerFactory.getInstance().getCalendarManager().getCalendar(calType, id.toString());
					final Collection<KalendarEvent> calEvents = cal.getEvents();
					for (final KalendarEvent kalendarEvent : calEvents) {
						if (showEvent(compareDate, kalendarEvent)) {
							logDebug("found a KalendarEvent: " + kalendarEvent.getSubject() + " with time: " + kalendarEvent.getBegin() + " modified before: "
									+ compareDate.toString(), null);
							// found a modified event in this calendar
							Date modDate = null;
							if (kalendarEvent.getLastModified() > 0) {
								modDate = new Date(kalendarEvent.getLastModified());
							} else if (kalendarEvent.getCreated() > 0) {
								modDate = new Date(kalendarEvent.getCreated());
							} else if (kalendarEvent.getBegin() != null) {
								modDate = kalendarEvent.getBegin();
							}

							final String subject = kalendarEvent.getSubject();
							String author = kalendarEvent.getCreatedBy();
							if (author == null) {
								author = "";
							}

							String location = "";
							if (StringHelper.containsNonWhitespace(kalendarEvent.getLocation())) {
								location = kalendarEvent.getLocation() == null ? "" : translator.translate("cal.notifications.location",
										new String[] { kalendarEvent.getLocation() });
							}
							String dateStr;
							if (kalendarEvent.isAllDayEvent()) {
								dateStr = form.formatDate(kalendarEvent.getBegin());
							} else {
								dateStr = form.formatDate(kalendarEvent.getBegin()) + " - " + form.formatDate(kalendarEvent.getEnd());
							}
							final String desc = translator.translate("cal.notifications.entry", new String[] { subject, dateStr, location, author });
							final String businessPath = bPath + "[path=" + kalendarEvent.getID() + ":0]";
							final String urlToSend = NotificationHelper.getURLFromBusinessPathString(p, businessPath);
							final SubscriptionListItem subListItem = new SubscriptionListItem(desc, urlToSend, modDate, CSS_CLASS_CALENDAR_ICON);
							si.addSubscriptionListItem(subListItem);
						}
					}
				}
			} catch (final Exception e) {
				logError("Unexpected exception", e);
				checkPublisher(p);
				si = NotificationsManager.getInstance().getNoSubscriptionInfo();
			}
		} else {
			si = NotificationsManager.getInstance().getNoSubscriptionInfo();
		}
		return si;
	}

	private void checkPublisher(final Publisher p) {
		try {
			if (CalendarController.ACTION_CALENDAR_GROUP.equals(p.getSubidentifier())) {
				final BusinessGroup bg = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(p.getResId(), false);
				if (bg == null) {
					logInfo("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
				}
			} else if (CalendarController.ACTION_CALENDAR_COURSE.equals(p.getSubidentifier())) {
				if (!NotificationsUpgradeHelper.checkCourse(p)) {
					logInfo("deactivating publisher with key; " + p.getKey(), null);
					NotificationsManager.getInstance().deactivate(p);
				}
			}
		} catch (final Exception e) {
			logError("", e);
		}
	}

	private boolean showEvent(final Date compareDate, final KalendarEvent kalendarEvent) {
		if (kalendarEvent.getLastModified() > 0) { return compareDate.getTime() < kalendarEvent.getLastModified(); }
		if (kalendarEvent.getCreated() > 0) { return compareDate.getTime() < kalendarEvent.getCreated(); }
		return false;
	}

	@Override
	public String createTitleInfo(final Subscriber subscriber, final Locale locale) {
		try {
			final Translator translator = Util.createPackageTranslator(this.getClass(), locale);
			String title = null;
			final Long id = subscriber.getPublisher().getResId();
			final String type = subscriber.getPublisher().getSubidentifier();
			if (type.equals(CalendarController.ACTION_CALENDAR_COURSE)) {
				final String displayName = RepositoryManager.getInstance().lookupDisplayNameByOLATResourceableId(id);
				title = translator.translate("cal.notifications.header.course", new String[] { displayName });
			} else if (type.equals(CalendarController.ACTION_CALENDAR_GROUP)) {
				final BusinessGroup group = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(id, false);
				title = translator.translate("cal.notifications.header.group", new String[] { group.getName() });
			}
			return title;
		} catch (final Exception e) {
			logError("Error while creating calendar notifications for subscriber: " + subscriber.getKey(), e);
			checkPublisher(subscriber.getPublisher());
			return "-";
		}
	}

	@Override
	public String getType() {
		return "CalendarManager";
	}

}