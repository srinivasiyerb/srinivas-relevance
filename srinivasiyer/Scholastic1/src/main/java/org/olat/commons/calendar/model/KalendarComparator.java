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

import java.util.Comparator;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;

public class KalendarComparator implements Comparator {

	private static final KalendarComparator INSTANCE = new KalendarComparator();

	public static final KalendarComparator getInstance() {
		return INSTANCE;
	}

	@Override
	public int compare(final Object arg0, final Object arg1) {
		final KalendarRenderWrapper calendar0 = (KalendarRenderWrapper) arg0;
		final KalendarRenderWrapper calendar1 = (KalendarRenderWrapper) arg1;
		// if of the same type, order by display name
		if (calendar0.getKalendar().getType() == calendar1.getKalendar().getType()) { return calendar0.getKalendarConfig().getDisplayName()
				.compareTo(calendar1.getKalendarConfig().getDisplayName()); }
		// if of different type, order by type
		if (calendar0.getKalendar().getType() == CalendarManager.TYPE_USER) { return -1; // TYPE_USER is displayed first
		}
		if (calendar0.getKalendar().getType() == CalendarManager.TYPE_GROUP) { return +1; // TYPE GROUP is displayed last
		}
		if (calendar1.getKalendar().getType() == CalendarManager.TYPE_USER) {
			return +1;
		} else {
			return -1;
		}
	}

}
