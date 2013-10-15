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

package org.olat.login.auth;

import org.olat.core.gui.GUIInterna;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;

/**
 * Initial Date: 08.07.2003
 * 
 * @author Mike Stock
 */
public class OLATAuthentcationForm extends FormBasicController {

	private TextElement login;
	private TextElement pass;

	/**
	 * Login form used by the OLAT Authentication Provider
	 * 
	 * @param name
	 */
	public OLATAuthentcationForm(final UserRequest ureq, final WindowControl wControl, final Translator translator) {
		super(ureq, wControl);
		setTranslator(translator);
		initForm(ureq);
	}

	/**
	 * @see org.olat.core.gui.components.Form#validate(org.olat.core.gui.UserRequest)
	 */
	@Override
	public boolean validateFormLogic(final UserRequest ureq) {
		boolean valid = true;
		valid = valid && !login.isEmpty("lf.error.loginempty");
		valid = valid && !pass.isEmpty("lf.error.passempty");
		return valid;
	}

	/**
	 * @return Login field value.
	 */
	public String getLogin() {
		return login.getValue();
	}

	/**
	 * @return Password filed value.
	 */
	public String getPass() {
		return pass.getValue();
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormTitle("login.form");
		setFormDescription("login.intro");

		if (GUIInterna.isLoadPerformanceMode()) {
			setFormWarning("loadtest.warn");
		}

		login = uifactory.addTextElement("lf_login", "lf.login", 128, "", formLayout);
		pass = uifactory.addPasswordElement("lf_pass", "lf.pass", 128, "", formLayout);

		login.setDisplaySize(20);
		pass.setDisplaySize(20);

		uifactory.addFormSubmitButton("login.button", formLayout);
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (!login.isEmpty() && !pass.isEmpty()) {
			flc.getRootForm().submit(ureq);
		}
	}

	@Override
	protected void doDispose() {
		//
	}
}
