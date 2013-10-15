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

package org.olat.user;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;

/**
 * Initial Date: Jul 14, 2003
 * 
 * @author gnaegi Comment: Form for changing the password. It asks for the old and the new password
 */
public class ChangePasswordForm extends FormBasicController {

	private TextElement oldpass;
	private TextElement newpass1;
	private TextElement newpass2; // confirm

	private String _oldpass = "";
	private String _newpass = "";

	/**
	 * @param name
	 */
	public ChangePasswordForm(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		initForm(ureq);
	}

	/**
	 * @return Old password field value.
	 */
	public String getOldPasswordValue() {
		return _oldpass;
	}

	/**
	 * @return New password field value.
	 */
	public String getNewPasswordValue() {
		return _newpass;
	}

	@Override
	protected void formOK(final UserRequest ureq) {

		_oldpass = oldpass.getValue();
		_newpass = newpass1.getValue();

		oldpass.setValue("");
		newpass1.setValue("");
		newpass2.setValue("");

		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void formCancelled(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		if (!UserManager.getInstance().syntaxCheckOlatPassword(newpass1.getValue())) {
			newpass1.setErrorKey("error.password.characters", null);
			return false;
		}
		if (!newpass1.getValue().equals(newpass2.getValue())) {
			newpass1.setValue("");
			newpass2.setValue("");
			newpass2.setErrorKey("error.password.nomatch", null);
			return false;
		}
		return true;
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {

		oldpass = uifactory.addPasswordElement("oldpass", "form.password.old", 128, "", formLayout);
		newpass1 = uifactory.addPasswordElement("newpass1", "form.password.new1", 128, "", formLayout);
		newpass2 = uifactory.addPasswordElement("newpass2", "form.password.new2", 128, "", formLayout);

		// Button layout
		final FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton("submit", buttonLayout);
		uifactory.addFormCancelButton("cancel", buttonLayout, ureq, getWindowControl());

		oldpass.setMandatory(true);
		newpass1.setMandatory(true);
		newpass2.setMandatory(true);

		oldpass.setNotEmptyCheck("form.please.enter.old");
		newpass1.setNotEmptyCheck("form.please.enter.new");
		newpass2.setNotEmptyCheck("form.please.enter.new");
	}

	@Override
	protected void doDispose() {
		//
	}
}