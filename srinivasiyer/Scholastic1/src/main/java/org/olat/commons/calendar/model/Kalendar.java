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

package org.olat.commons.calendar.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Kalendar implements Serializable {

	private final String calendarID;
	private final String type;
	private final Map<String, KalendarEvent> events;

	public Kalendar(final String calendarID, final String type) {
		this.calendarID = calendarID;
		this.type = type;
		events = new HashMap<String, KalendarEvent>();
	}

	/**
	 * Return this calendar's ID.
	 * 
	 * @return
	 */
	public String getCalendarID() {
		return calendarID;
	}

	/**
	 * Add a new event.
	 * 
	 * @param event
	 */
	public void addEvent(final KalendarEvent event) {
		event.setKalendar(this);
		events.put(event.getID(), event);
	}

	/**
	 * Remove an event from this calendar.
	 * 
	 * @param event
	 */
	public void removeEvent(final KalendarEvent event) {
		events.remove(event.getID());
	}

	/**
	 * Get a specific event.
	 * 
	 * @param eventID
	 * @return
	 */
	public KalendarEvent getEvent(final String eventID) {
		return events.get(eventID);
	}

	/**
	 * Return all events associated with this calendar.
	 * 
	 * @return
	 */
	public Collection<KalendarEvent> getEvents() {
		return events.values();
	}

	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return "Kalendar[type=" + getType() + ", id=" + getCalendarID() + "]";
	}

}
