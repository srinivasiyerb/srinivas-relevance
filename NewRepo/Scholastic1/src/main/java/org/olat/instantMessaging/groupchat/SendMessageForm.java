/**
 * OLAT - Online Learning and Training<br />
 * http://www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br />
 * you may not use this file except in compliance with the License.<br />
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br />
 * software distributed under the License is distributed on an "AS IS" BASIS, <br />
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br />
 * See the License for the specific language governing permissions and <br />
 * limitations under the License.
 * <p>
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br />
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.instantMessaging.groupchat;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;

/**
 * <P>
 * Initial Date: 26.03.2007 <br />
 * 
 * @author guido
 */
public class SendMessageForm extends FormBasicController {

	private TextElement msg;
	private FormLink submit;

	public SendMessageForm(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl, "sendMessageForm");
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		//
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.impl.FormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		msg = uifactory.addTextElement("msg", null, 1024, null, formLayout);
		msg.setFocus(true);// always focus to the message field
		msg.setDisplaySize(40);
		submit = uifactory.addFormLink("subm", "msg.send", "msg.send", formLayout, Link.BUTTON);
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (source == submit) {
			flc.getRootForm().submit(ureq);
		}
	}

	public String getMessage() {
		return msg.getValue();
	}

	public void resetTextField() {
		msg.setValue("");
	}
}
