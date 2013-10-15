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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.StaticTextElement;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nManager;
import org.olat.user.UserManager;
import org.olat.user.UserModule;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Description:
 * 
 * @author Sabina Jeger
 */
public class RegistrationForm2 extends FormBasicController {
	static final String USERPROPERTIES_FORM_IDENTIFIER = RegistrationForm2.class.getCanonicalName();
	private final String languageKey;
	private List<UserPropertyHandler> userPropertyHandlers;
	private final Map<String, FormItem> propFormItems;

	private SingleSelection lang;
	private TextElement username;
	private StaticTextElement usernameStatic;
	private TextElement newpass1;
	private TextElement newpass2; // confirm

	private final String proposedUsername;
	private final boolean userInUse;
	private final boolean usernameReadonly;

	private FormLayoutContainer buttonLayout;

	/**
	 * @param name
	 * @param languageKey
	 */

	public RegistrationForm2(final UserRequest ureq, final WindowControl wControl, final String languageKey, final String proposedUsername, final boolean userInUse,
			final boolean usernameReadonly) {
		super(ureq, wControl);

		this.languageKey = languageKey;
		this.proposedUsername = proposedUsername;
		this.userInUse = userInUse;
		this.usernameReadonly = usernameReadonly;

		propFormItems = new HashMap<String, FormItem>();
		initForm(ureq);
	}

	protected void freeze() {
		setFormTitle("step5.reg.yourdata", null);
		flc.setEnabled(false);
	}

	protected String getLangKey() {
		return lang.getSelectedKey();
	}

	protected String getFirstName() {
		final FormItem fi = propFormItems.get("firstName");
		final TextElement fn = (TextElement) fi;
		return fn.getValue().trim();
	}

	protected String getLastName() {
		final FormItem fi = propFormItems.get("lastName");
		final TextElement fn = (TextElement) fi;
		return fn.getValue().trim();
	}

	protected String getLogin() {
		if (username != null) {
			return username.getValue().trim();
		} else if (usernameStatic != null) { return usernameStatic.getValue().trim(); }
		return null;
	}

	private void setLogin(final String login) {
		if (username != null) {
			username.setValue(login);
		} else if (usernameStatic != null) {
			usernameStatic.setValue(login);
		}
	}

	private void setLoginErrorKey(final String errorKey) {
		if (username != null) {
			username.setErrorKey(errorKey, new String[0]);
		} else if (usernameStatic != null) {
			usernameStatic.setErrorKey(errorKey, new String[0]);
		}
	}

	protected String getPassword() {
		return newpass1.getValue().trim();
	}

	protected FormItem getPropFormItem(final String k) {
		return propFormItems.get(k);
	}

	/**
	 * Initialize the form
	 */

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		setFormTitle("title.register");
		// first the configured user properties
		final UserManager um = UserManager.getInstance();
		userPropertyHandlers = um.getUserPropertyHandlersFor(USERPROPERTIES_FORM_IDENTIFIER, false);

		final Translator tr = Util.createPackageTranslator(UserPropertyHandler.class, getLocale(), getTranslator());

		// Add all available user fields to this form
		for (final UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) {
				continue;
			}

			final FormItem fi = userPropertyHandler.addFormItem(getLocale(), null, USERPROPERTIES_FORM_IDENTIFIER, false, formLayout);
			fi.setTranslator(tr);
			propFormItems.put(userPropertyHandler.getName(), fi);
		}

		uifactory.addSpacerElement("lang", formLayout, true);
		// second the user language
		final Map<String, String> languages = I18nManager.getInstance().getEnabledLanguagesTranslated();
		lang = uifactory.addDropdownSingleselect("user.language", formLayout, StringHelper.getMapKeysAsStringArray(languages),
				StringHelper.getMapValuesAsStringArray(languages), null);
		lang.select(languageKey, true);

		uifactory.addSpacerElement("loginstuff", formLayout, true);
		if (usernameReadonly) {
			usernameStatic = uifactory.addStaticTextElement("username", "user.login", proposedUsername, formLayout);
		} else {
			username = uifactory.addTextElement("username", "user.login", 128, "", formLayout);
			username.setMandatory(true);
		}

		if (proposedUsername != null) {
			setLogin(proposedUsername);
		}
		if (userInUse) {
			setLoginErrorKey("form.check6");
		}

		newpass1 = uifactory.addPasswordElement("newpass1", "form.password.new1", 128, "", formLayout);
		newpass2 = uifactory.addPasswordElement("newpass2", "form.password.new2", 128, "", formLayout);

		newpass1.setMandatory(true);
		newpass2.setMandatory(true);

		// Button layout
		buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton("submit.speichernUndweiter", buttonLayout);

	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		// validate each user field
		for (final UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			final FormItem fi = propFormItems.get(userPropertyHandler.getName());
			if (!userPropertyHandler.isValid(fi, null)) { return false; }
		}

		if (!UserManager.getInstance().syntaxCheckOlatLogin(getLogin())) {
			setLoginErrorKey("form.check3");
			return false;
		}

		final Identity s = BaseSecurityManager.getInstance().findIdentityByName(getLogin());
		if (s != null || UserModule.isLoginOnBlacklist(getLogin())) {
			setLoginErrorKey("form.check6");
			return false;
		}

		if (newpass1.getValue().equals("")) {
			newpass1.setErrorKey("form.check4", null);
			return false;
		}

		if (newpass2.getValue().equals("")) {
			newpass2.setErrorKey("form.check4", null);
			return false;
		}

		if (!UserManager.getInstance().syntaxCheckOlatPassword(newpass1.getValue())) {
			newpass1.setErrorKey("form.checkregex", null);
			return false;
		}
		if (!newpass1.getValue().equals(newpass2.getValue())) {
			newpass2.setErrorKey("form.check5", null);
		}
		return true;
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		fireEvent(ureq, Event.DONE_EVENT);
	}

	@Override
	protected void doDispose() {
		//
	}
}