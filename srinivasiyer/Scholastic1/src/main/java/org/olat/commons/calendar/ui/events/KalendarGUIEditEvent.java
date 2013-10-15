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

import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.gui.control.Event;

public class KalendarGUIEditEvent extends Event {

	public static final String CMD_EDIT = "ecalevent";
	private final KalendarEvent event;
	private final KalendarRenderWrapper calendarWrapper;

	public KalendarGUIEditEvent(final KalendarEvent event, final KalendarRenderWrapper calendarWrapper) {
		super(CMD_EDIT);
		this.event = event;
		this.calendarWrapper = calendarWrapper;
	}

	public KalendarEvent getKalendarEvent() {
		return event;
	}

	public KalendarRenderWrapper getKalendarRenderWrapper() {
		return calendarWrapper;
	}
}
