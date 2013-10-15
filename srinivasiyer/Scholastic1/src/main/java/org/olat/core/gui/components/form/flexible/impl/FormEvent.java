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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.gui.components.form.flexible.impl;

import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.control.Event;

/**
 * Description:<br>
 * TODO: patrickb Class Description for FormEvent
 * <P>
 * Initial Date: 11.12.2006 <br>
 * 
 * @author patrickb
 */
public class FormEvent extends Event {

	private FormItem source;
	public static final int ONDBLCLICK = 1;
	public static final int ONCLICK = 2;
	// Don't use onchange events for radiobuttons / checkboxes, this does not work
	// in IE <= 8. Use onclick instead (OLAT-5753). Note that when activating a
	// checkbox / radio button the onclick will also be fired although nothing has
	// been clicked.
	public static final int ONCHANGE = 4;
	// FIXME:BP: add support for onkeydown event. Also add support for multiple
	// events, e.g. onclick and onkeydown. onchange is buggy, (IE does not fire it
	// correctly)

	// sorted x0 > x1 > x2 .. > xn
	public static final int[] ON_DOTDOTDOT = new int[] { ONDBLCLICK, ONCLICK, ONCHANGE };
	public static final FormEvent RESET = new FormEvent("reset", null);
	private int trigger = -1;

	public FormEvent(String command, FormItem source, int action) {
		super(command);
		this.source = source;
		this.trigger = action;
	}

	public FormEvent(String command, FormItem source) {
		this(command, source, ONCLICK);
	}

	public FormEvent(Event event, FormItem source, int action) {
		this(event.getCommand(), source, action);
	}

	public FormItem getFormItemSource() {
		return source;
	}

	public boolean wasTriggerdBy(int events) {
		// FIXME:pb: make it multiple event capable
		return trigger - events == 0;
	}
}
