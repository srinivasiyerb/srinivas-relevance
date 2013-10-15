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

package org.olat.commons.calendar.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ImportCalendarManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.logging.OLATRuntimeException;

/**
 * Description:<BR>
 * <P>
 * Initial Date: August 22, 2008
 * 
 * @author Udit Sajjanhar
 */
public class CalendarImportByUrlController extends BasicController {

	private final VelocityContainer importVC;
	private final CalendarImportUrlForm importUrlForm;
	private final Link cancelButton;
	private final Panel panel;

	CalendarImportByUrlController(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);

		setBasePackage(CalendarManager.class);

		importVC = createVelocityContainer("calImportByUrl");

		importUrlForm = new CalendarImportUrlForm(ureq, wControl);
		listenTo(importUrlForm);

		panel = new Panel("panel");
		panel.setContent(importUrlForm.getInitialComponent());

		importVC.put("urlinput", panel);
		cancelButton = LinkFactory.createButton("cancel", importVC, this);
		putInitialPanel(importVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == cancelButton) {
			fireEvent(ureq, Event.CANCELLED_EVENT);
		}
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == importUrlForm) {
			if (event == Event.DONE_EVENT) {
				try {
					final String calendarContent = ImportCalendarManager.getContentFromUrl(importUrlForm.getCalendarUrl());
					processCalendarUrl(ureq, calendarContent);
				} catch (final IOException e) {
					getWindowControl().setError(translate("cal.import.url.invalid"));
					return;
				}
			} else if (event == Event.CANCELLED_EVENT) {
				fireEvent(ureq, Event.CANCELLED_EVENT);
			}
		}
	}

	private void processCalendarUrl(final UserRequest ureq, final String content) {
		try {
			// store the content of the url in a file by a temporary name
			final CalendarManager calManager = CalendarManagerFactory.getInstance().getCalendarManager();
			final String calID = ImportCalendarManager.getTempCalendarIDForUpload(ureq);
			final File tmpFile = calManager.getCalendarFile(CalendarManager.TYPE_USER, calID);
			final BufferedWriter output = new BufferedWriter(new FileWriter(tmpFile));
			output.write(content);
			output.close();

			// try to parse the tmp file
			final Object calendar = calManager.readCalendar(CalendarManager.TYPE_USER, calID);
			if (calendar != null) {
				fireEvent(ureq, Event.DONE_EVENT);
			} else {
				getWindowControl().setError(translate("cal.import.url.content.invalid"));
			}
		} catch (final IOException e) {
			getWindowControl().setError(translate("cal.import.url.file.write.error"));
		} catch (final OLATRuntimeException e) {
			getWindowControl().setError(translate("cal.import.url.content.invalid"));
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// do nothing here yet
	}

	public String getImportUrl() {
		return importUrlForm.getCalendarUrl();
	}

}
