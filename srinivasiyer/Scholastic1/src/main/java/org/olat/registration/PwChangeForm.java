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

package org.olat.registration;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.login.auth.OLATAuthManager;
import org.olat.user.UserManager;

/**
 * Description:
 * 
 * @author Sabina Jeger
 */
public class PwChangeForm extends FormBasicController {

	private TextElement newpass1;
	private TextElement newpass2; // confirm

	/**
	 * Password change form.
	 * 
	 * @param name
	 */
	public PwChangeForm(final UserRequest ureq, final WindowControl wControl) {
		super(ureq, wControl);
		initForm(ureq);
	}

	@Override
	public boolean validateFormLogic(final UserRequest ureq) {

		final boolean newIsValid = UserManager.getInstance().syntaxCheckOlatPassword(newpass1.getValue());
		if (!newIsValid) {
			newpass1.setErrorKey("form.password.error.characters", null);
		}
		// validate that both passwords are the same
		final boolean newDoesMatch = newpass1.getValue().equals(newpass2.getValue());
		if (!newDoesMatch) {
			newpass2.setErrorKey("form.password.error.nomatch", null);
		}
		return newIsValid && newDoesMatch;
	}

	/**
	 * Saves the form data in the user object and the database
	 * 
	 * @param doer The current identity.
	 * @param s The identity to change the password.
	 */
	public boolean saveFormData(final Identity s) {
		return OLATAuthManager.changePasswordByPasswordForgottenLink(s, newpass1.getValue());
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormTitle("form.password.enter.new");
		newpass1 = uifactory.addPasswordElement("newpass1", "form.password.new1", 128, "", formLayout);
		newpass2 = uifactory.addPasswordElement("newpass2", "form.password.new2", 128, "", formLayout);
		uifactory.addFormSubmitButton("submit", formLayout);
	}

	@Override
	protected void doDispose() {
		//
	}
}