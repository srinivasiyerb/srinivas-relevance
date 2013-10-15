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

package org.olat.admin.user;

import java.util.List;
import java.util.Map;

import org.olat.basesecurity.AuthHelper;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.events.SingleIdentityChosenEvent;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SelectionElement;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.OLATSecurityException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.ArrayHelper;
import org.olat.core.util.StringHelper;
import org.olat.core.util.i18n.I18nManager;
import org.olat.core.util.i18n.I18nModule;
import org.olat.core.util.resource.OresHelper;
import org.olat.user.UserManager;
import org.olat.user.propertyhandlers.UserPropertyHandler;

/**
 * Initial Date: Jul 31, 2003
 * 
 * @author gnaegi Comment: Displays a form to create a new user on the OLAT plattform
 */
public class UserCreateController extends BasicController {

	private final NewUserForm createUserForm;

	/**
	 * @param ureq
	 * @param wControl
	 */
	public UserCreateController(final UserRequest ureq, final WindowControl wControl, final boolean canCreateOLATPassword) {
		super(ureq, wControl);
		final BaseSecurity mgr = BaseSecurityManager.getInstance();
		if (!mgr.isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, OresHelper.lookupType(this.getClass()))) { throw new OLATSecurityException(
				"Insufficient permissions to access UserCreateController"); }

		final Translator pT = UserManager.getInstance().getPropertyHandlerTranslator(getTranslator());
		createUserForm = new NewUserForm(ureq, wControl, canCreateOLATPassword, pT);
		this.listenTo(createUserForm);

		final VelocityContainer newUserVC = this.createVelocityContainer("newuser");
		newUserVC.put("createUserForm", createUserForm.getInitialComponent());
		this.putInitialPanel(newUserVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(final UserRequest ureq, final Component source, final Event event) {
		// empty
	}

	@Override
	public void event(final UserRequest ureq, final Controller source, final Event event) {
		if (source == createUserForm) {
			if (event instanceof SingleIdentityChosenEvent) {
				showInfo("new.user.successful");
				fireEvent(ureq, event);
			} else if (event == Event.FAILED_EVENT) {
				fireEvent(ureq, event);
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	@Override
	protected void doDispose() {
		// nothing to do
	}
}

/**
 * <pre>
 * 
 *  Initial Date:  Jul 31, 2003
 * 
 *  @author gnaegi
 *  
 *  Comment:  
 *  Form for creating new a new user as administrator
 * 
 * </pre>
 */

class NewUserForm extends FormBasicController {

	private final OLog log = Tracing.createLoggerFor(this.getClass());

	private static final String formIdentifyer = NewUserForm.class.getCanonicalName();
	private static final String PASSWORD_NEW1 = "passwordnew1";
	private static final String PASSWORD_NEW2 = "passwordnew2";
	private static final String LOGINNAME = "loginname";
	private static final String USER_CREATE_SUCCESS = "user successfully created: ";
	private List<UserPropertyHandler> userPropertyHandlers;
	private boolean showPasswordFields = false;

	private TextElement emailTextElement;
	private TextElement usernameTextElement;
	private TextElement psw1TextElement;
	private TextElement psw2TextElement;
	private SingleSelection languageSingleSelection;
	private SelectionElement authCheckbox;

	/**
	 * @param ureq
	 * @param wControl
	 * @param showPasswordFields: true the password fields are used, the user can enter a password for the new user; false: the passwort is not used at all
	 */
	public NewUserForm(final UserRequest ureq, final WindowControl wControl, final boolean showPasswordFields, final Translator translator) {
		super(ureq, wControl);
		this.showPasswordFields = showPasswordFields;
		this.setTranslator(translator);
		initForm(ureq);
	}

	@Override
	protected void initForm(final FormItemContainer formLayout, final Controller listener, final UserRequest ureq) {
		uifactory.addStaticTextElement("heading1", null, translate("new.form.please.enter"), formLayout);
		usernameTextElement = uifactory.addTextElement(LOGINNAME, "username", 128, "", formLayout);
		usernameTextElement.setMandatory(true);
		usernameTextElement.setDisplaySize(30);

		final UserManager um = UserManager.getInstance();
		userPropertyHandlers = um.getUserPropertyHandlersFor(formIdentifyer, true);
		// Add all available user fields to this form
		for (final UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			if (userPropertyHandler == null) {
				continue;
			}
			final FormItem formItem = userPropertyHandler.addFormItem(ureq.getLocale(), null, formIdentifyer, true, formLayout);
			// special case to handle email field
			if (userPropertyHandler.getName().equals(UserConstants.EMAIL)) {
				emailTextElement = (TextElement) formItem;
			}
		}

		final Map<String, String> languages = I18nManager.getInstance().getEnabledLanguagesTranslated();
		final String[] langKeys = StringHelper.getMapKeysAsStringArray(languages);
		final String[] langValues = StringHelper.getMapValuesAsStringArray(languages);
		ArrayHelper.sort(langKeys, langValues, false, true, false);
		// Build css classes for reference languages
		final String[] langCssClasses = I18nManager.getInstance().createLanguageFlagsCssClasses(langKeys, "b_with_small_icon_left");
		languageSingleSelection = uifactory.addDropdownSingleselect("new.form.language", formLayout, langKeys, langValues, langCssClasses);
		// select default language in form
		languageSingleSelection.select(I18nModule.getDefaultLocale().toString(), true);

		// add password fields!!!
		if (showPasswordFields) {
			uifactory.addStaticTextElement("heading2", null, translate("new.form.please.enter.pwd"), formLayout);

			// checkBox: generate user with OLAT authentication or not
			final String[] authKeys = { "xx" };
			final String[] authValues = { translate("new.form.auth.true") };
			authCheckbox = uifactory.addCheckboxesHorizontal("new.form.auth", formLayout, authKeys, authValues, null);
			authCheckbox.select("xx", showPasswordFields);
			authCheckbox.addActionListener(this, FormEvent.ONCLICK);

			// if OLAT authentication is used, use the pwd below
			psw1TextElement = uifactory.addPasswordElement(PASSWORD_NEW1, "new.form.password.new1", 255, "", formLayout);
			psw1TextElement.setMandatory(true);
			psw1TextElement.setDisplaySize(30);
			psw1TextElement.setVisible(showPasswordFields);

			psw2TextElement = uifactory.addPasswordElement(PASSWORD_NEW2, "new.form.password.new2", 255, "", formLayout);
			psw2TextElement.setMandatory(true);
			psw2TextElement.setDisplaySize(30);
			psw2TextElement.setVisible(showPasswordFields);
		}

		uifactory.addFormSubmitButton("save", "submit.save", formLayout);
	}

	@Override
	protected void formInnerEvent(final UserRequest ureq, final FormItem source, final FormEvent event) {
		if (showPasswordFields && source == authCheckbox) {
			psw1TextElement.setVisible(authCheckbox.isSelected(0));
			psw2TextElement.setVisible(authCheckbox.isSelected(0));
		}
	}

	@Override
	protected boolean validateFormLogic(final UserRequest ureq) {
		// validate if username does match the syntactical login requirements
		final String loginName = usernameTextElement.getValue();
		if (usernameTextElement.isEmpty() || !UserManager.getInstance().syntaxCheckOlatLogin(loginName)) {
			usernameTextElement.setErrorKey("new.error.loginname.empty", new String[] {});
			return false;
		}
		// Check if login is still available
		final Identity identity = BaseSecurityManager.getInstance().findIdentityByName(loginName);
		if (identity != null) {
			usernameTextElement.setErrorKey("new.error.loginname.choosen", new String[] {});
			return false;
		}
		usernameTextElement.clearError();

		// validate special rules for each user property
		for (final UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			// we assume here that there are only textElements for the user properties
			final FormItem formItem = this.flc.getFormComponent(userPropertyHandler.getName());
			if (!userPropertyHandler.isValid(formItem, null) || formItem.hasError()) { return false; }
			formItem.clearError();
		}
		// special test on email address: validate if email is already used
		if (emailTextElement != null) {
			final String email = emailTextElement.getValue();
			// Check if email is not already taken
			final UserManager um = UserManager.getInstance();

			// TODO:fj offer a method in basesecurity to threadsafely generate a new
			// user!!!

			final Identity exists = um.findIdentityByEmail(email);
			if (exists != null) {
				// Oups, email already taken, display error
				emailTextElement.setErrorKey("new.error.email.choosen", new String[] {});
				return false;
			}
		}

		// validate if new password does match the syntactical password requirements

		// password fields depend on form configuration
		if (showPasswordFields && psw1TextElement != null && psw2TextElement != null && authCheckbox.isSelected(0)) {
			final String pwd = psw1TextElement.getValue();
			if (psw1TextElement.isEmpty("new.form.mandatory") || psw1TextElement.hasError()) { return false; }
			if (!UserManager.getInstance().syntaxCheckOlatPassword(pwd)) {
				psw1TextElement.setErrorKey("new.error.password.characters", new String[] {});
				return false;
			}
			psw1TextElement.clearError();
			if (psw2TextElement.isEmpty("new.form.mandatory") || psw2TextElement.hasError()) { return false; }
			// validate that both passwords are the same
			if (!pwd.equals(psw2TextElement.getValue())) {
				psw2TextElement.setErrorKey("new.error.password.nomatch", new String[] {});
				return false;
			}
			psw2TextElement.clearError();
		}
		// all checks passed
		return true;
	}

	@Override
	protected void formOK(final UserRequest ureq) {
		// Create user on database
		final Identity s = doCreateAndPersistIdentity();
		if (s != null) {
			log.audit(USER_CREATE_SUCCESS + s.getName());
			fireEvent(ureq, new SingleIdentityChosenEvent(s));
		} else {
			// Could not save form, display error
			getWindowControl().setError(translate("new.user.unsuccessful"));
			fireEvent(ureq, Event.FAILED_EVENT);
		}
	}

	@Override
	protected void formResetted(final UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	private Identity doCreateAndPersistIdentity() {
		final String lang = languageSingleSelection.getSelectedKey();
		final String username = usernameTextElement.getValue();
		String pwd = null;
		// use password only when configured to do so
		if (showPasswordFields && authCheckbox.isSelected(0)) {
			pwd = psw1TextElement.getValue();
			if (!StringHelper.containsNonWhitespace(pwd)) {
				// treat white-space passwords as no-password. This is fine, a password can be set later on
				pwd = null;
			}
		}
		// Create new user and identity and put user to users group
		// Create transient user without firstName,lastName, email
		final UserManager um = UserManager.getInstance();
		final User newUser = um.createUser(null, null, null);
		// Now add data from user fields (firstName,lastName and email are mandatory)
		for (final UserPropertyHandler userPropertyHandler : userPropertyHandlers) {
			final FormItem propertyItem = this.flc.getFormComponent(userPropertyHandler.getName());
			userPropertyHandler.updateUserFromFormItem(newUser, propertyItem);
		}
		// Init preferences
		newUser.getPreferences().setLanguage(lang);
		newUser.getPreferences().setInformSessionTimeout(true);
		// Save everything in database
		final Identity ident = AuthHelper.createAndPersistIdentityAndUserWithUserGroup(username, pwd, newUser);
		return ident;
	}

	@Override
	protected void doDispose() {
		// empty
	}
}