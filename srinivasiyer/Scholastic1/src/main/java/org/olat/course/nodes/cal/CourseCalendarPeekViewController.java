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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.olat.commons.calendar.model.Kalendar;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.nodes.CalCourseNode;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;

/**
 * <h3>Description:</h3> A PeekViewController which show the next three events of the calendar. Next to the current date if the course node is setup to show the actual
 * date or next to the date defined in the course node.
 * <p>
 * Initial Date: 9 nov. 2009 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com, www.frentix.com
 */
public class CourseCalendarPeekViewController extends BasicController {
	private TableController tableController;

	public CourseCalendarPeekViewController(final UserRequest ureq, final WindowControl wControl, final UserCourseEnvironment userCourseEnv,
			final CalCourseNode courseNode, final NodeEvaluation ne) {
		super(ureq, wControl);

		init(ureq, courseNode, userCourseEnv, ne);

		putInitialPanel(tableController.getInitialComponent());
	}

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void event(final UserRequest ureq, final Component source, final Event event) {
		//
	}

	private void init(final UserRequest ureq, final CalCourseNode courseNode, final UserCourseEnvironment courseEnv, final NodeEvaluation ne) {
		final ICourse course = CourseFactory.loadCourse(courseEnv.getCourseEnvironment().getCourseResourceableId());
		final CourseCalendars myCal = CourseCalendars.createCourseCalendarsWrapper(ureq, getWindowControl(), course, ne);

		Date refDate;
		final ModuleConfiguration config = courseNode.getModuleConfiguration();
		if (CalEditController.getAutoDate(config)) {
			refDate = new Date();
		} else {
			refDate = CalEditController.getStartDate(config);
			if (refDate == null) {
				refDate = new Date();
			}
		}

		final List<KalendarEvent> nextEvents = new ArrayList<KalendarEvent>();
		for (final KalendarRenderWrapper calendar : myCal.getCalendars()) {
			final Kalendar cal = calendar.getKalendar();
			final Collection<KalendarEvent> events = cal.getEvents();
			for (final KalendarEvent event : events) {
				if (refDate.compareTo(event.getBegin()) <= 0) {
					nextEvents.add(event);
				}
			}
		}
		Collections.sort(nextEvents, new KalendarEventComparator());
		final List<KalendarEvent> nextThreeEvents = nextEvents.subList(0, Math.min(3, nextEvents.size()));

		final TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("calendar.noEvents"));
		tableConfig.setDisplayTableHeader(false);
		tableConfig.setCustomCssClass("b_portlet_table");
		tableConfig.setDisplayRowCount(false);
		tableConfig.setPageingEnabled(false);
		tableConfig.setDownloadOffered(false);
		tableConfig.setSortingEnabled(false);

		removeAsListenerAndDispose(tableController);
		tableController = new TableController(tableConfig, ureq, getWindowControl(), getTranslator());
		listenTo(tableController);

		// dummy header key, won't be used since setDisplayTableHeader is set to
		// false
		tableController.addColumnDescriptor(new DefaultColumnDescriptor("calendar.date", 0, null, ureq.getLocale()));
		tableController.addColumnDescriptor(new DefaultColumnDescriptor("calendar.subject", 1, null, ureq.getLocale(), ColumnDescriptor.ALIGNMENT_LEFT));
		tableController.setTableDataModel(new CourseCalendarPeekViewModel(nextThreeEvents, getTranslator()));
	}

	public class KalendarEventComparator implements Comparator<KalendarEvent> {
		@Override
		public int compare(final KalendarEvent o1, final KalendarEvent o2) {
			final Date b1 = o1.getBegin();
			final Date b2 = o2.getBegin();
			if (b1 == null) { return -1; }
			if (b2 == null) { return 1; }
			return b1.compareTo(b2);
		}
	}
}
