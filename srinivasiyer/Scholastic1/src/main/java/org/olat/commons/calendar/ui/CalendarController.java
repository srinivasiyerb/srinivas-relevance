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
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.commons.calendar.ui;

import java.util.Date;
import java.util.List;

import org.olat.core.gui.control.Controller;

public interface CalendarController extends Controller {

	public static final String ACTION_CALENDAR_COURSE = "action.calendar.course";
	public static final String ACTION_CALENDAR_GROUP = "action.calendar.group";
	public static final String CALLER_COURSE = "course";
	public static final String CALLER_COLLAB = "collab";
	public static final String CALLER_PROFILE = "profile";
	public static final String CALLER_HOME = "home";

	/**
	 * Set the focus for this calendar.
	 * 
	 * @param Date
	 */
	public void setFocus(Date date);

	/**
	 * Set the focus for this calendar.
	 * 
	 * @param Date
	 */
	public void setFocusOnEvent(String eventId);

	/**
	 * Sets the list of KalendarRenderWrappers for this calendar controller.
	 * 
	 * @param calendars
	 */
	public void setCalendars(List calendars);

	/**
	 * Sets the list of KalendarRenderWrappers for this calendar controller.
	 * 
	 * @param calendars
	 */
	public void setCalendars(List calendars, List importedCalendars);

	/**
	 * Sets the calendar dirty. The calendar controller should issue an KalendarModfied event upon next user click.
	 */
	public void setDirty();
}
