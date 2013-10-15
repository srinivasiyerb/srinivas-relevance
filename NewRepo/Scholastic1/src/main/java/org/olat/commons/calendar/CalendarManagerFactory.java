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

import java.io.File;

import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.WebappHelper;

public class CalendarManagerFactory {

	private static CalendarManagerFactory INSTANCE;
	private static CalendarManager DEFAULT_MANAGER;

	private CalendarManagerFactory(final File fBaseDirectory) {
		// singleton
		final File calendarBase = new File(fBaseDirectory, "/calendars");
		if (!calendarBase.exists()) {
			if (!calendarBase.mkdirs()) { throw new OLATRuntimeException("Error creating calendar base directory at: " + calendarBase.getAbsolutePath(), null); }
		}
		DEFAULT_MANAGER = new ICalFileCalendarManager(calendarBase);
	}

	public static final CalendarManagerFactory getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new CalendarManagerFactory(new File(WebappHelper.getUserDataRoot()));
		}
		return INSTANCE;
	}

	public static final CalendarManagerFactory getJUnitInstance() {
		final String tmpDir = System.getProperty("java.io.tmpdir");
		System.out.println("CalendarManagerFactory initialized with temp directory at " + tmpDir);
		return new CalendarManagerFactory(new File(tmpDir));
	}

	public CalendarManager getCalendarManager() {
		return DEFAULT_MANAGER;
	}
}
