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
import java.util.Iterator;
import java.util.List;

import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.model.KalendarConfig;
import org.olat.commons.calendar.ui.CalendarController;
import org.olat.commons.calendar.ui.LinkProvider;
import org.olat.commons.calendar.ui.WeeklyCalendarController;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.commons.calendar.ui.events.KalendarModifiedEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.CourseRights;
import org.olat.group.BusinessGroup;
import org.olat.repository.RepositoryManager;

public class CourseCalendarController extends BasicController {

	private final CalendarController calendarController;
	private KalendarRenderWrapper courseKalendarWrapper;
	private final OLATResourceable ores;

	public CourseCalendarController(final UserRequest ureq, final WindowControl wControl, final OLATResourceable course) {
		super(ureq, wControl);
		this.ores = course;
		final List calendars = getListOfCalendarWrappers(ureq);
		final CourseCalendarSubscription calendarSubscription = new CourseCalendarSubscription(courseKalendarWrapper.getKalendar(), ureq.getUserSession()
				.getGuiPreferences());
		calendarController = new WeeklyCalendarController(ureq, wControl, calendars, WeeklyCalendarController.CALLER_COURSE, calendarSubscription, true);
		listenTo(calendarController);
		putInitialPanel(calendarController.getInitialComponent());
	}

	private List getListOfCalendarWrappers(final UserRequest ureq) {
		final List calendars = new ArrayList();
		final CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
		// add course calendar
		final ICourse course = CourseFactory.loadCourse(ores);
		courseKalendarWrapper = calendarManager.getCourseCalendar(course);
		final CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
		final Identity identity = ureq.getIdentity();
		final boolean isPrivileged = cgm.isIdentityCourseAdministrator(identity) || cgm.hasRight(identity, CourseRights.RIGHT_COURSEEDITOR)
				|| RepositoryManager.getInstance().isInstitutionalRessourceManagerFor(RepositoryManager.getInstance().lookupRepositoryEntry(course, false), identity);
		if (isPrivileged) {
			courseKalendarWrapper.setAccess(KalendarRenderWrapper.ACCESS_READ_WRITE);
		} else {
			courseKalendarWrapper.setAccess(KalendarRenderWrapper.ACCESS_READ_ONLY);
		}
		final KalendarConfig config = calendarManager.findKalendarConfigForIdentity(courseKalendarWrapper.getKalendar(), ureq);
		if (config != null) {
			courseKalendarWrapper.getKalendarConfig().setCss(config.getCss());
			courseKalendarWrapper.getKalendarConfig().setVis(config.isVis());
		}
		// add link provider
		final CourseLinkProviderController clpc = new CourseLinkProviderController(course, ureq, getWindowControl());
		courseKalendarWrapper.setLinkProvider(clpc);
		calendars.add(courseKalendarWrapper);

		// add course group calendars
		final boolean isGroupManager = cgm.isIdentityCourseAdministrator(identity) || cgm.hasRight(identity, CourseRights.RIGHT_GROUPMANAGEMENT);
		if (isGroupManager) {
			// learning groups
			List allGroups = cgm.getAllLearningGroupsFromAllContexts();
			addCalendars(ureq, allGroups, true, clpc, calendars);
			// right groups
			allGroups = cgm.getAllRightGroupsFromAllContexts();
			addCalendars(ureq, allGroups, true, clpc, calendars);
		} else {
			// learning groups
			final List ownerGroups = cgm.getOwnedLearningGroupsFromAllContexts(identity);
			addCalendars(ureq, ownerGroups, true, clpc, calendars);
			final List attendedGroups = cgm.getParticipatingLearningGroupsFromAllContexts(identity);
			for (final Iterator ownerGroupsIterator = ownerGroups.iterator(); ownerGroupsIterator.hasNext();) {
				final BusinessGroup ownerGroup = (BusinessGroup) ownerGroupsIterator.next();
				if (attendedGroups.contains(ownerGroup)) {
					attendedGroups.remove(ownerGroup);
				}
			}
			addCalendars(ureq, attendedGroups, false, clpc, calendars);

			// right groups
			final List rightGroups = cgm.getParticipatingRightGroupsFromAllContexts(identity);
			addCalendars(ureq, rightGroups, false, clpc, calendars);
		}
		return calendars;
	}

	private void addCalendars(final UserRequest ureq, final List groups, final boolean isOwner, final LinkProvider linkProvider, final List calendars) {
		final CollaborationToolsFactory collabFactory = CollaborationToolsFactory.getInstance();
		final CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
		for (final Iterator iter = groups.iterator(); iter.hasNext();) {
			final BusinessGroup bGroup = (BusinessGroup) iter.next();
			final CollaborationTools collabTools = collabFactory.getOrCreateCollaborationTools(bGroup);
			if (!collabTools.isToolEnabled(CollaborationTools.TOOL_CALENDAR)) {
				continue;
			}
			final KalendarRenderWrapper groupCalendarWrapper = calendarManager.getGroupCalendar(bGroup);
			// set calendar access
			int iCalAccess = CollaborationTools.CALENDAR_ACCESS_OWNERS;
			final Long lCalAccess = collabTools.lookupCalendarAccess();
			if (lCalAccess != null) {
				iCalAccess = lCalAccess.intValue();
			}
			if (iCalAccess == CollaborationTools.CALENDAR_ACCESS_OWNERS && !isOwner) {
				groupCalendarWrapper.setAccess(KalendarRenderWrapper.ACCESS_READ_ONLY);
			} else {
				groupCalendarWrapper.setAccess(KalendarRenderWrapper.ACCESS_READ_WRITE);
			}
			final KalendarConfig config = calendarManager.findKalendarConfigForIdentity(groupCalendarWrapper.getKalendar(), ureq);
			if (config != null) {
				groupCalendarWrapper.getKalendarConfig().setCss(config.getCss());
				groupCalendarWrapper.getKalendarConfig().setVis(config.isVis());
			}
			groupCalendarWrapper.setLinkProvider(linkProvider);
			calendars.add(groupCalendarWrapper);
		}
	}

	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// nothing to do
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (event instanceof KalendarModifiedEvent) {
			final List calendars = getListOfCalendarWrappers(ureq);
			calendarController.setCalendars(calendars);
		}
	}

	@Override
	protected void doDispose() {
		//
	}

}
