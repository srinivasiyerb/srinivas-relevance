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

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ImportCalendarManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;

public class CalendarImportNameForm extends FormBasicController {

	private static final String SUBMIT_SINGLE = "submit";

	private TextElement calendarName;
	private final Identity identity;

	/**
	 * Display an event for modification or to add a new event.
	 * 
	 * @param name
	 */
	public CalendarImportNameForm(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		setBasePackage(CalendarManager.class);

		this.identity = ureq.getIdentity();

		initForm(ureq);
	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		if (calendarName.isEmpty()) {
			calendarName.setErrorKey("cal.import.calname.empty.error", null);
			return false;
		} else {
			final CalendarManager calManager = CalendarManagerFactory.getInstance().getCalendarManager();
			final String calID = ImportCalendarManager.getImportedCalendarID(identity, calendarName.getValue());
			if (calManager.calendarExists(CalendarManager.TYPE_USER, calID)) {
				calendarName.setErrorKey("cal.import.calname.exists.error", null);
				return false;
			}
		}
		return true;
	}

	public String getCalendarName() {
		return calendarName.getValue();
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		// title
		// addFormElement("cal.import.calname.title", new TitleElement("cal.import.calname.title"));

		// prompt for the calendar name
		final int identityLen = ureq.getIdentity().getName().length();

		// 41=OresHelper.ORES_TYPE_LENGTH - 2 - 7
		// 2 because: 1 for the '_' which is added between identity and calendar name,
		// and 1 for fuzzy counting which TextElement seems to do...
		// 7 because: the CalendarManager.TYPE is prepended to the whole thing adding a _
		// and the max length of TYPE is 6 - hence 7
		// @see jira OLAT-4202

		calendarName = uifactory.addTextElement("calname", "cal.import.calname.prompt", 41 - identityLen, "", formLayout);

		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton(SUBMIT_SINGLE, "cal.import.calname.submit", buttonLayout);
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());
	}

	@Override
	protected void doDispose() {
		//
	}
}
