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
 * Description:<br>
 * The MailNotificationEditController allows the user to enter a mailtext based on the MailTemplate. It will be surrounded by some comments which variables that can be
 * used in this context.
 * <p>
 * Events:
 * <ul>
 * <li>Event.DONE_EVENT</li>
 * <li>Event.CANCEL_EVENT</li>
 * </ul>
 * <p>
 * Initial Date: 23.11.2006 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH<br>
 *         http://www.frentix.com
 */

package org.olat.core.util.mail;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;

public class MailNotificationEditController extends BasicController {

	// components
	private VelocityContainer mainVC;
	private MailTemplateForm mailForm;

	// data model
	private MailTemplate mailTemplate;
	private String orgMailSubject;
	private String orgMailBody;
	private Boolean cpFrom;

	/**
	 * Constructor for the email
	 * 
	 * @param wControl
	 * @param ureq
	 * @param mailTemplate
	 * @param useCancel
	 */
	public MailNotificationEditController(WindowControl wControl, UserRequest ureq, MailTemplate mailTemplate, boolean useCancel) {
		super(ureq, wControl);
		this.mailTemplate = mailTemplate;
		orgMailSubject = mailTemplate.getSubjectTemplate();
		orgMailBody = mailTemplate.getBodyTemplate();
		cpFrom = mailTemplate.getCpfrom();

		mainVC = createVelocityContainer("mailnotification");
		mailForm = new MailTemplateForm(ureq, wControl, mailTemplate, useCancel);
		listenTo(mailForm);

		mainVC.put("mailForm", mailForm.getInitialComponent());

		putInitialPanel(mainVC);
	}

	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		//
	}

	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == mailForm) {
			if (event == Event.DONE_EVENT) {
				mailForm.updateTemplateFromForm(mailTemplate);
				fireEvent(ureq, event);
			} else if (event == Event.CANCELLED_EVENT) {
				mailTemplate = null;
				fireEvent(ureq, event);
			}
		}
	}

	/**
	 * @return The mail template containing the configured mail or null if user decided to not send a mail
	 */
	public MailTemplate getMailTemplate() {
		if (mailForm.sendMailSwitchEnabled()) {
			return mailTemplate;
		} else {
			return null;
		}
	}

	/**
	 * @return Boolean
	 */
	public boolean isTemplateChanged() {
		return !orgMailSubject.equals(mailTemplate.getSubjectTemplate()) || !orgMailBody.equals(mailTemplate.getBodyTemplate())
				|| !cpFrom.equals(mailTemplate.getCpfrom());
	}

	@Override
	protected void doDispose() {
		//
	}

}
