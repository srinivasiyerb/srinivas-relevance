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
 * Copyright (c) 2007 frentix GmbH, Switzerland<br>
 * <p>
 */
package org.olat.user.propertyhandlers;

import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.formelements.FormElement;
import org.olat.core.gui.formelements.StaticSingleSelectionElement;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.User;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nModule;
import org.olat.user.AbstractUserPropertyHandler;
import org.olat.user.UserManager;

/**
 * <h3>Description:</h3> The gender user property implemente a user attribute to specify the users gender in a drop down.
 * <p>
 * Initial Date: 23.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

public class GenderPropertyHandler extends AbstractUserPropertyHandler {

	private static final String[] keys = new String[] { "male", "female", "-" };

	/**
	 * Helper method to create translated values that correspond with the static keys
	 * 
	 * @param locale
	 * @return
	 */

	private String[] getTranslatedValues(final Locale locale) {
		final Translator trans = Util.createPackageTranslator(this.getClass(), locale);
		final String[] values = new String[] { trans.translate(i18nFormElementLabelKey() + "." + keys[0]), trans.translate(i18nFormElementLabelKey() + "." + keys[1]),
				trans.translate(i18nFormElementLabelKey() + "." + keys[2]) };
		return values;
	}

	/**
	 * @see org.olat.user.AbstractUserPropertyHandler#getUserProperty(org.olat.core.id.User, java.util.Locale)
	 */
	@Override
	public String getUserProperty(final User user, final Locale locale) {
		Translator myTrans;
		if (locale == null) {
			myTrans = Util.createPackageTranslator(this.getClass(), I18nModule.getDefaultLocale());
		} else {
			myTrans = Util.createPackageTranslator(this.getClass(), locale);
		}
		final String internalValue = getInternalValue(user);
		final String displayValue = myTrans.translate("form.name.gender." + internalValue);
		return displayValue;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateUserFromFormElement(org.olat.core.id.User, org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public void updateUserFromFormElement(final User user, final FormElement ui) {
		final String internalValue = getStringValue(ui);
		setInternalValue(user, internalValue);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateUserFromFormItem(org.olat.core.id.User, org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public void updateUserFromFormItem(final User user, final FormItem formItem) {
		final String internalValue = getStringValue(formItem);
		setInternalValue(user, internalValue);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public String getStringValue(final FormElement ui) {
		return ((StaticSingleSelectionElement) ui).getSelectedKey();
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public String getStringValue(final FormItem formItem) {
		return ((org.olat.core.gui.components.form.flexible.elements.SingleSelection) formItem).getSelectedKey();
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(java.lang.String, java.util.Locale)
	 */
	@Override
	public String getStringValue(final String displayValue, final Locale locale) {
		// This should be refactored, but currently the bulk change does not work
		// otherwhise. When changing this here, the isValidValue must also to
		// changed to work with the real display value

		// use default: use key as value
		return displayValue;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getFormElement(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean)
	 */
	@Override
	public FormElement getFormElement(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser) {
		final UserManager um = UserManager.getInstance();
		final StaticSingleSelectionElement ui = new StaticSingleSelectionElement(i18nFormElementLabelKey(), keys, getTranslatedValues(locale));
		updateFormElementFromUser(ui, user);
		if (um.isUserViewReadOnly(usageIdentifyer, this) && !isAdministrativeUser) {
			ui.setReadOnly(true);
		}
		if (um.isMandatoryUserProperty(usageIdentifyer, this)) {
			ui.setMandatory(true);
		}
		return ui;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#addFormItem(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean,
	 *      org.olat.core.gui.components.form.flexible.FormItemContainer)
	 */
	@Override
	public FormItem addFormItem(final Locale locale, final User user, final String usageIdentifyer, final boolean isAdministrativeUser,
			final FormItemContainer formItemContainer) {
		org.olat.core.gui.components.form.flexible.elements.SingleSelection genderElem = null;
		// genderElem = FormUIFactory.getInstance().addDropdownSingleselect(getName(), i18nFormElementLabelKey(), formItemContainer, keys, getTranslatedValues(locale),
		// null);
		genderElem = FormUIFactory.getInstance().addRadiosVertical(getName(), i18nFormElementLabelKey(), formItemContainer, keys, getTranslatedValues(locale));

		genderElem.select(user == null ? "-" : this.getInternalValue(user), true);

		final UserManager um = UserManager.getInstance();
		if (um.isUserViewReadOnly(usageIdentifyer, this) && !isAdministrativeUser) {
			genderElem.setEnabled(false);
		}
		if (um.isMandatoryUserProperty(usageIdentifyer, this)) {
			genderElem.setMandatory(true);
		}
		return genderElem;
	}

	/**
	 * @see org.olat.user.AbstractUserPropertyHandler#getInternalValue(org.olat.core.id.User)
	 */
	@Override
	public String getInternalValue(final User user) {
		final String value = super.getInternalValue(user);
		return (value == null ? "-" : value); // default
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateFormElementFromUser(org.olat.core.gui.formelements.FormElement, org.olat.core.id.User)
	 */
	@Override
	public void updateFormElementFromUser(final FormElement ui, final User user) {
		final String key = getInternalValue(user);
		((StaticSingleSelectionElement) ui).select(key, true);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValid(org.olat.core.gui.formelements.FormElement)
	 */
	@Override
	public boolean isValid(final FormElement ui, final Map formContext) {
		if (ui.isMandatory()) {
			final StaticSingleSelectionElement sse = (StaticSingleSelectionElement) ui;
			// when mandatory, the - must not be selected
			if (sse.getSelectedKey().equals("-")) {
				sse.setErrorKey("gender.error");
				return false;
			}
		}
		return true;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValid(org.olat.core.gui.components.form.flexible.FormItem, java.util.Map)
	 */

	@Override
	public boolean isValid(final FormItem formItem, final Map formContext) {
		if (formItem.isMandatory()) {
			final org.olat.core.gui.components.form.flexible.elements.SingleSelection sse = (org.olat.core.gui.components.form.flexible.elements.SingleSelection) formItem;
			// when mandatory, the - must not be selected
			if (sse.getSelectedKey().equals("-")) {
				sse.setErrorKey("gender.error", null);
				return false;
			}
		}
		return true;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValidValue(java.lang.String, org.olat.core.gui.components.form.ValidationError, java.util.Locale)
	 */
	@Override
	public boolean isValidValue(final String value, final ValidationError validationError, final Locale locale) {
		if (value != null) {
			for (int i = 0; i < keys.length; i++) {
				final String key = keys[i];
				if (key.equals(value)) { return true; }
			}
			validationError.setErrorKey("gender.error");
			return false;
		}
		// null values are ok
		return true;
	}

}
