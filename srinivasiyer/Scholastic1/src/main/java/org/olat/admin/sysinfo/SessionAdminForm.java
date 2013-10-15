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

package org.olat.admin.sysinfo;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.Submit;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;

public class SessionAdminForm extends FormBasicController {
	private IntegerElement sessionTimeout;
	private IntegerElement maxSessions;
	private final int initialSessionTimeoutInSec;
	private final int initialMaxSessions;

	public SessionAdminForm(final UserRequest ureq, final WindowControl wControl, final Translator translator, final int initialSessionTimeoutInSec,
			final int initialMaxSessions) {
		super(ureq, wControl);
		setTranslator(translator);
		this.initialSessionTimeoutInSec = initialSessionTimeoutInSec;
		this.initialMaxSessions = initialMaxSessions;
		initForm(ureq);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		final FormLayoutContainer verticalL = FormLayoutContainer.createVerticalFormLayout("verticalL", getTranslator());
		formLayout.add(verticalL);
		sessionTimeout = uifactory.addIntegerElement("session.timeout", "session.timeout.label", initialSessionTimeoutInSec, verticalL);
		maxSessions = uifactory.addIntegerElement("max.sessions", "max.sessions.label", initialMaxSessions, verticalL);
		final Submit saveButton = new FormSubmit("save", "save");
		formLayout.add(saveButton);
	}

	@Override
	protected void doDispose() {
		// empty
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formResetted(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	/**
	 * sessiontimout in seconds
	 * 
	 * @return
	 */
	public int getSessionTimeout() {
		return sessionTimeout.getIntValue();
	}

	public int getMaxSessions() {
		return maxSessions.getIntValue();
	}

}
