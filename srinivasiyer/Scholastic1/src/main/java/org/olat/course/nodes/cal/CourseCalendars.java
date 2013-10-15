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
 * frentix GmbH, Switzerland, http://www.frentix.com
 * <p>
 */
package org.olat.course.nodes.cal;

import java.util.ArrayList;
import java.util.List;

import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarConfig;
import org.olat.commons.calendar.ui.LinkProvider;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.CourseRights;
import org.olat.course.nodes.CalCourseNode;
import org.olat.course.run.calendar.CourseCalendarSubscription;
import org.olat.course.run.calendar.CourseLinkProviderController;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.group.BusinessGroup;
import org.olat.repository.RepositoryManager;

public class CourseCalendars {

	private KalendarRenderWrapper courseKalendarWrapper;
	private List<KalendarRenderWrapper> calendars;

	public CourseCalendars(final KalendarRenderWrapper courseKalendarWrapper, final List<KalendarRenderWrapper> calendars) {
		this.courseKalendarWrapper = courseKalendarWrapper;
		this.calendars = calendars;
	}

	public List<KalendarRenderWrapper> getCalendars() {
		return calendars;
	}

	public void setCalendars(final List<KalendarRenderWrapper> calendars) {
		this.calendars = calendars;
	}

	public KalendarRenderWrapper getCourseKalendarWrapper() {
		return courseKalendarWrapper;
	}

	public void setCourseKalendarWrapper(final KalendarRenderWrapper courseKalendarWrapper) {
		this.courseKalendarWrapper = courseKalendarWrapper;
	}

	public Kalendar getKalendar() {
		return courseKalendarWrapper.getKalendar();
	}

	public CourseCalendarSubscription createSubscription(final UserRequest ureq) {
		final CourseCalendarSubscription calSubscription = new CourseCalendarSubscription(getKalendar(), ureq.getUserSession().getGuiPreferences());
		return calSubscription;
	}

	public static CourseCalendars createCourseCalendarsWrapper(final UserRequest ureq, final WindowControl wControl, final OLATResourceable ores, final NodeEvaluation ne) {
		final List<KalendarRenderWrapper> calendars = new ArrayList<KalendarRenderWrapper>();
		final CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
		// add course calendar
		final ICourse course = CourseFactory.loadCourse(ores);
		final KalendarRenderWrapper courseKalendarWrapper = calendarManager.getCourseCalendar(course);
		final CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
		final Identity identity = ureq.getIdentity();
		final boolean isPrivileged = cgm.isIdentityCourseAdministrator(identity) || ne.isCapabilityAccessible(CalCourseNode.EDIT_CONDITION_ID)
				|| RepositoryManager.getInstance().isInstitutionalRessourceManagerFor(RepositoryManager.getInstance().lookupRepositoryEntry(course, false), identity)
				|| ureq.getUserSession().getRoles().isOLATAdmin();
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
		final CourseLinkProviderController clpc = new CourseLinkProviderController(course, ureq, wControl);
		courseKalendarWrapper.setLinkProvider(clpc);
		calendars.add(courseKalendarWrapper);

		// add course group calendars
		final boolean isGroupManager = cgm.isIdentityCourseAdministrator(identity) || cgm.hasRight(identity, CourseRights.RIGHT_GROUPMANAGEMENT);
		if (isGroupManager) {
			// learning groups
			List<BusinessGroup> allGroups = cgm.getAllLearningGroupsFromAllContexts();
			addCalendars(ureq, allGroups, true, clpc, calendars);
			// right groups
			allGroups = cgm.getAllRightGroupsFromAllContexts();
			addCalendars(ureq, allGroups, true, clpc, calendars);
		} else {
			// learning groups
			final List<BusinessGroup> ownerGroups = cgm.getOwnedLearningGroupsFromAllContexts(identity);
			addCalendars(ureq, ownerGroups, true, clpc, calendars);
			final List<BusinessGroup> attendedGroups = cgm.getParticipatingLearningGroupsFromAllContexts(identity);
			for (final BusinessGroup ownerGroup : ownerGroups) {
				if (attendedGroups.contains(ownerGroup)) {
					attendedGroups.remove(ownerGroup);
				}
			}
			addCalendars(ureq, attendedGroups, false, clpc, calendars);

			// right groups
			final List<BusinessGroup> rightGroups = cgm.getParticipatingRightGroupsFromAllContexts(identity);
			addCalendars(ureq, rightGroups, false, clpc, calendars);
		}
		return new CourseCalendars(courseKalendarWrapper, calendars);
	}

	private static void addCalendars(final UserRequest ureq, final List<BusinessGroup> groups, final boolean isOwner, final LinkProvider linkProvider,
			final List<KalendarRenderWrapper> calendars) {
		final CollaborationToolsFactory collabFactory = CollaborationToolsFactory.getInstance();
		final CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
		for (final BusinessGroup bGroup : groups) {
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
}
