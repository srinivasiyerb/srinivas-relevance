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

package org.olat.course.run.calendar;

import java.util.ArrayList;
import java.util.List;

import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.ui.CalendarSubscription;
import org.olat.core.gui.control.Controller;
import org.olat.core.util.prefs.Preferences;

public class CourseCalendarSubscription implements CalendarSubscription {

	private static final String KEY_SUBSCRIPTION = "subs";
	private static final String KEY_UN_SUBSCRIPTION = "notdesired";

	private final Kalendar kalendar;
	private final Preferences preferences;

	public CourseCalendarSubscription(final Kalendar kalendar, final Preferences preferences) {
		this.kalendar = kalendar;
		this.preferences = preferences;
	}

	/**
	 * @see org.olat.calendar.ui.CalendarSubscription#isSubscribed()
	 */
	@Override
	public boolean isSubscribed() {
		return getSubscribedCourseCalendarIDs(preferences).contains(kalendar.getCalendarID());
	}

	/**
	 * @see org.olat.calendar.ui.CalendarSubscription#triggerSubscribeAction()
	 */
	@Override
	public Controller triggerSubscribeAction() {
		final List<String> courseSubscriptions = getSubscribedCourseCalendarIDs(preferences);
		final List<String> courseUnSubscriptions = getUnsubscribedCourseCalendarIDs(preferences);
		// check if already subscribed
		if (courseSubscriptions.contains(kalendar.getCalendarID())) {
			// do an unsubscribe of the actual calendar
			if (courseSubscriptions.remove(kalendar.getCalendarID())) {
				courseUnSubscriptions.add(kalendar.getCalendarID());
				persistAllSubscribptionInfos(courseSubscriptions, courseUnSubscriptions, preferences);
			}
		} else {
			// subscribe to the actual calendar
			courseSubscriptions.add(kalendar.getCalendarID());
			courseUnSubscriptions.remove(kalendar.getCalendarID());
			persistAllSubscribptionInfos(courseSubscriptions, courseUnSubscriptions, preferences);
		}
		return null;
	}

	@Override
	public void subscribe(final boolean force) {
		// check if already subscribed
		if (!isSubscribed()) {
			// subscribe to the actual calendar
			final List<String> courseSubscriptions = getSubscribedCourseCalendarIDs(preferences);
			final List<String> courseUnSubscriptions = getUnsubscribedCourseCalendarIDs(preferences);
			if (!courseUnSubscriptions.contains(kalendar.getCalendarID()) || force) {
				courseSubscriptions.add(kalendar.getCalendarID());
				courseUnSubscriptions.remove(kalendar.getCalendarID());
				persistAllSubscribptionInfos(courseSubscriptions, courseUnSubscriptions, preferences);
			}
		}
	}

	@Override
	public void unsubscribe() {
		// unsubscribe to the actual calendar
		final List<String> courseSubscriptions = getSubscribedCourseCalendarIDs(preferences);
		final List<String> courseUnSubscriptions = getUnsubscribedCourseCalendarIDs(preferences);
		courseSubscriptions.remove(kalendar.getCalendarID());
		courseUnSubscriptions.add(kalendar.getCalendarID());
		persistAllSubscribptionInfos(courseSubscriptions, courseUnSubscriptions, preferences);
	}

	public static List<String> getSubscribedCourseCalendarIDs(final Preferences preferences) {
		List<String> courseSubscriptions = (List<String>) preferences.get(CourseCalendarSubscription.class, KEY_SUBSCRIPTION);
		if (courseSubscriptions == null) {
			courseSubscriptions = new ArrayList<String>();
		}
		return courseSubscriptions;
	}

	public static List<String> getUnsubscribedCourseCalendarIDs(final Preferences preferences) {
		List<String> courseSubscriptions = (List<String>) preferences.get(CourseCalendarSubscription.class, KEY_UN_SUBSCRIPTION);
		if (courseSubscriptions == null) {
			courseSubscriptions = new ArrayList<String>();
		}
		return courseSubscriptions;
	}

	public static void persistSubscribedCalendarIDs(final List<String> subscribedCalendarIDs, final Preferences preferences) {
		preferences.put(CourseCalendarSubscription.class, KEY_SUBSCRIPTION, subscribedCalendarIDs);
		preferences.save();
	}

	private static void persistAllSubscribptionInfos(final List<String> subscribedCalendarIDs, final List<String> unSubscribedCalendarIDs, final Preferences preferences) {
		preferences.put(CourseCalendarSubscription.class, KEY_SUBSCRIPTION, subscribedCalendarIDs);
		preferences.put(CourseCalendarSubscription.class, KEY_UN_SUBSCRIPTION, unSubscribedCalendarIDs);
		preferences.save();
	}
}