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

import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;

public class KalendarConfig {

	private transient String displayName;
	private String css;
	private boolean vis;
	private Long resId;

	public KalendarConfig() {
		this("Calendar", KalendarRenderWrapper.CALENDAR_COLOR_BLUE, true);
	}

	public KalendarConfig(final String displayName, final String calendarCSS, final boolean visible) {
		this.displayName = displayName;
		this.css = calendarCSS;
		this.vis = visible;
		this.resId = null;
	}

	public Long getResId() {
		return this.resId;
	}

	public void setResId(final Long resId) {
		this.resId = resId;
	}

	public String getCss() {
		return css;
	}

	public void setCss(final String css) {
		this.css = css;
	}

	public boolean isVis() {
		return vis;
	}

	public void setVis(final boolean vis) {
		this.vis = vis;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}
}
