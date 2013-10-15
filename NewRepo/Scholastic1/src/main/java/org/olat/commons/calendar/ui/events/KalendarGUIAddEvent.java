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

package org.olat.commons.calendar.ui.events;

import java.util.Date;

import org.olat.core.gui.control.Event;

public class KalendarGUIAddEvent extends Event {

	public static final String CMD_ADD = "acalevent";
	private final String calendarID;
	private final Date startDate;
	private boolean allDayEvent = false;

	public KalendarGUIAddEvent(final String calendarID, final Date startDate) {
		super(CMD_ADD);
		this.calendarID = calendarID;
		this.startDate = startDate;
	}

	/**
	 * @param calendarID
	 * @param startDate
	 * @param allDayEvent When true, the new event should be an all-day event
	 */
	public KalendarGUIAddEvent(final String calendarID, final Date startDate, final boolean allDayEvent) {
		super(CMD_ADD);
		this.calendarID = calendarID;
		this.startDate = startDate;
		this.allDayEvent = allDayEvent;
	}

	public String getCalendarID() {
		return calendarID;
	}

	public Date getStartDate() {
		return startDate;
	}

	public boolean isAllDayEvent() {
		return allDayEvent;
	}
}
